package com.trevit.service

import com.trevit.dto.PlanDtos
import com.trevit.dto.PlanDtos.BudgetBreakdown
import com.trevit.dto.PlanDtos.DayPlanDto
import com.trevit.dto.PlanDtos.LegDto
import com.trevit.dto.PlanDtos.PlanRequest
import com.trevit.dto.PlanDtos.PlanResponse
import com.trevit.dto.PlanDtos.StopDto
import com.trevit.entity.Place
import com.trevit.entity.Place.PlaceType
import com.trevit.entity.Wallet
import com.trevit.repository.WalletRepository
import org.springframework.stereotype.Service
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 예산 기반 플랜 생성.
 * 배분: 숙박 40% / 관광 30% / 식비 20% / 교통 10%
 *
 * 1차: LM Studio 로컬 AI에게 숙소·일자별 방문 순서를 짜게 한다 (AiPlanService).
 * 실패 시 휴리스틱 폴백:
 *   숙박 예산 내 최고 평점 숙소 → 관광 예산 내 하루 2~3곳(평점·근접도·입장료 점수화)
 *   → 식비 예산 내 하루 3끼 → 최근접 이웃 동선 최적화.
 * 공통: 마지막엔 숙소 복귀, 800m 초과 구간은 대중교통.
 */
@Service
class PlanService(
    private val routeService: RouteService,
    private val aiPlanService: AiPlanService,
    private val walletRepository: WalletRepository,
    private val placeProvider: PlaceProviderService,
    private val regionService: RegionService,
) {

    fun createPlan(req: PlanRequest): PlanResponse {
        val days = req.days.coerceIn(1, 7)
        val people = maxOf(1, req.people)
        val budget = maxOf(0, req.budget)
        val dayTrip = days == 1         // 당일치기: 숙박 없음
        val nights = if (dayTrip) 0 else days - 1

        // 토큰 검증: 예산은 보유 토큰(1토큰 = 1원)을 넘을 수 없다
        val wallet = walletRepository.findById(1L)
            .orElseGet { walletRepository.save(Wallet(1L, Wallet.INITIAL_BALANCE)) }
        require(budget <= wallet.balance) {
            "보유 토큰이 부족합니다 (보유 ${wallet.balance}토큰, 요청 예산 ${budget}토큰)"
        }

        // 예산 배분 — 당일치기는 숙박 몫을 관광·식비로 재배분
        val lodgingBudget = if (dayTrip) 0 else budget * 40 / 100
        val attractionBudget = if (dayTrip) budget * 55 / 100 else budget * 30 / 100
        val foodBudget = if (dayTrip) budget * 35 / 100 else budget * 20 / 100
        val transportBudget = budget * 10 / 100

        // 기준점(앵커) 결정 — 서비스 범위는 수도권(서울·경기·인천)
        //   1) region 이름이 해석되면 그 지역 대표 좌표
        //   2) 없으면 사용자 위치 (수도권 안일 때만)
        //   3) 둘 다 아니면 기본 지역(서울)
        val region = regionService.resolve(req.region)
        val userInArea = req.startLatitude != null && req.startLongitude != null &&
            regionService.inServiceArea(req.startLatitude, req.startLongitude)
        val anchorLat = region?.lat ?: if (userInArea) req.startLatitude!! else regionService.default.lat
        val anchorLng = region?.lng ?: if (userInArea) req.startLongitude!! else regionService.default.lng
        val regionName = region?.name ?: if (userInArea) "현재 위치" else regionService.default.name

        // 장소 후보: TMAP POI 실시간 조회 → 키 없음/429/부족 시 수도권 시드 폴백
        val rawPool = placeProvider.places(anchorLat, anchorLng)
        // 프로필·취향 반영: 걷기 기피 시 산·등산 장소 제외, 선호 키워드·음식 취향 가점
        val pool = applyPreferences(rawPool, req)

        // ---------- 1차: 로컬 AI(LM Studio)에게 프로필과 함께 플랜 요청 ----------
        val profile = PlanDtos.TravelProfile(
            regionName = regionName,
            gender = req.gender,
            ageGroup = req.ageGroup,
            mbti = req.mbti,
            purpose = req.purpose,
            foodPreference = req.foodPreference,
            avoidWalking = req.avoidWalking,
            keywords = req.keywords.orEmpty(),
            preferenceNote = req.preferenceNote,
        )
        val aiCatalog = if (dayTrip) pool.all().filter { it.type != PlaceType.LODGING } else pool.all()
        val ai = aiPlanService.plan(budget, days, people, nights, aiCatalog, profile)

        val lodging: Place?              // 당일치기면 null
        val perDay: List<List<Place>>    // 일자별 장소 (AI면 방문 순서 그대로, 휴리스틱이면 미정렬)
        val aiOrdered: Boolean
        val plannedBy: String

        if (ai != null) {
            lodging = if (dayTrip) null else ai.lodging
            perDay = ai.days
            aiOrdered = true       // AI가 정한 방문 순서를 존중한다
            plannedBy = "AI"
        } else {
            // ---------- 폴백: 휴리스틱 ----------
            lodging = if (dayTrip) null else pickLodging(pool.lodgings, nights, lodgingBudget)
            val cLat = lodging?.latitude ?: anchorLat
            val cLng = lodging?.longitude ?: anchorLng

            // 이 지역 최저가 숙소마저 숙박 예산을 넘으면(예: 성수기 리조트 지역),
            // 초과분을 관광·식비 예산에서 비례 차감해 총액이 예산을 넘지 않게 한다.
            val lodgingOver = maxOf(0, (lodging?.price?.toLong() ?: 0L) * nights - lodgingBudget)
            val cut = if (lodgingOver > 0 && attractionBudget + foodBudget > 0) lodgingOver else 0L
            val attractionCut = cut * attractionBudget / maxOf(1, attractionBudget + foodBudget)
            val attractionBudgetAdj = maxOf(0, attractionBudget - attractionCut)
            val foodBudgetAdj = maxOf(0, foodBudget - (cut - attractionCut))

            val attractions = pickPlaces(pool.attractions, cLat, cLng, attractionBudgetAdj, people, days * 3)
            val restaurants = pickPlaces(pool.restaurants, cLat, cLng, foodBudgetAdj, people, days * 3)

            val attractionsByDay = splitByDay(nearestNeighborOrder(cLat, cLng, attractions), days)
            val restaurantsByDay = assignRestaurants(restaurants, attractionsByDay, cLat, cLng, days)
            perDay = (0 until days).map { d -> attractionsByDay[d] + restaurantsByDay[d] }
            aiOrdered = false
            plannedBy = "ALGORITHM"
        }

        val lodgingSpent = lodging?.let { it.price.toLong() * nights } ?: 0L

        // 일자별 출발 시각: 1일차는 현재 시각(밤·새벽이면 09:00), 이후 날은 09:00
        val dayStarts = (0 until days).map { d ->
            if (d == 0) {
                var now = LocalTime.now(ZoneId.of("Asia/Seoul"))
                now = now.withMinute(now.minute / 5 * 5).withSecond(0).withNano(0)
                val sane = !now.isBefore(LocalTime.of(8, 0)) && !now.isAfter(LocalTime.of(19, 0))
                if (sane) now else LocalTime.of(9, 0)
            } else {
                LocalTime.of(9, 0)
            }
        }

        // ---------- 일자별 동선·이동 구간 생성 ----------
        val dayPlans = ArrayList<DayPlanDto>()
        val usedPerDay = ArrayList<List<Place>>() // 실제 일정에 들어간 장소(식사 슬롯 반영)
        var transportSpent = 0L
        for (d in 0 until days) {
            val dayPlaces = perDay[d]

            // 출발점: 1일차는 사용자 현재 위치(있으면), 그 외/폴백은 숙소.
            val fromUserLocation = d == 0 && req.startLatitude != null && req.startLongitude != null
            val startLat: Double
            val startLng: Double
            val startName: String
            val startType: String
            if (fromUserLocation) {
                startLat = req.startLatitude!!
                startLng = req.startLongitude!!
                startName = "현재 위치"
                startType = "START"
            } else if (lodging != null) {
                startLat = lodging.latitude
                startLng = lodging.longitude
                startName = lodging.name
                startType = "LODGING"
            } else {
                startLat = anchorLat
                startLng = anchorLng
                startName = "출발 지점"
                startType = "START"
            }

            // 식사는 08/12/17시 슬롯 근처에만 배치하고, 이미 지난 슬롯의 끼니는 생략한다.
            val ordered = if (aiOrdered) {
                trimMealsToSlots(fixConsecutiveMeals(ArrayList(dayPlaces)), dayStarts[d])
            } else {
                buildDaySequenceTimed(startLat, startLng, dayPlaces, dayStarts[d])
            }
            usedPerDay.add(ordered)

            val stops = ArrayList<StopDto>()
            val startIsLodging = startType == "LODGING"
            stops.add(StopDto(
                if (startIsLodging) lodging!!.id else null, startName, startType,
                if (startIsLodging) lodging!!.address else "출발 지점",
                startLat, startLng, 0,
                if (startIsLodging) lodging!!.rating else 0.0,
                if (startIsLodging) lodging!!.description else "여행의 시작점",
            ))
            for (p in ordered) {
                stops.add(StopDto(
                    p.id, p.name, p.type.name, p.address,
                    p.latitude, p.longitude, p.price.toLong() * people,
                    p.rating, p.description,
                ))
            }
            // 마지막엔 숙소 복귀 (당일치기는 숙소가 없으므로 마지막 장소에서 종료)
            if (lodging != null) {
                stops.add(StopDto(
                    lodging.id, lodging.name, "LODGING", lodging.address,
                    lodging.latitude, lodging.longitude, 0,
                    lodging.rating, lodging.description,
                ))
            }

            val legs = ArrayList<LegDto>()
            for (i in 0 until stops.size - 1) {
                val a = stops[i]
                val b = stops[i + 1]
                val straight = GeoUtil.distanceMeters(a.latitude, a.longitude, b.latitude, b.longitude)
                var leg: LegDto
                if (straight <= WALK_THRESHOLD_M) {
                    leg = routeService.walkLeg(a.latitude, a.longitude, b.latitude, b.longitude)
                    // 직선은 짧아도 실제 도보 경로가 1km를 넘으면(호수·강 우회 등) 대중교통으로 전환
                    if (leg.distanceMeters > 1000) {
                        val transit = routeService.transitLeg(a.latitude, a.longitude, b.latitude, b.longitude)
                        val groupFare = transit.fare * people
                        transportSpent += groupFare
                        leg = transit.withFare(groupFare)
                    }
                } else {
                    // 1km 이상 걷지 않는다: 장거리 구간은 교통 예산이 넘어도 대중교통 유지
                    val transit = routeService.transitLeg(a.latitude, a.longitude, b.latitude, b.longitude)
                    val groupFare = transit.fare * people
                    transportSpent += groupFare
                    leg = transit.withFare(groupFare)
                }
                legs.add(leg)
            }

            // 일정표 시각 계산 — 버스 구간은 평균 대기 7분 포함, 식당 50분·관광지 60분 체류 반영
            var clock = dayStarts[d]
            for (i in legs.indices) {
                val depart = clock
                val moveMin = legs[i].durationMinutes + (if (legs[i].mode == "TRANSIT") TRANSIT_WAIT_MIN else 0)
                clock = clock.plusMinutes(moveMin.toLong())
                legs[i] = legs[i].withTimes(depart.format(HHMM), clock.format(HHMM))
                val nextType = stops[i + 1].type
                clock = clock.plusMinutes(when (nextType) {
                    "RESTAURANT" -> MEAL_MIN.toLong()
                    "ATTRACTION" -> ATTRACTION_MIN.toLong()
                    else -> 0L
                })
            }

            val dayCost = stops.sumOf { it.cost } + legs.sumOf { it.fare }
            dayPlans.add(DayPlanDto(d + 1, stops, legs, dayCost))
        }

        // 실제 일정에 들어간 장소 기준으로 비용 계산 (생략된 끼니는 비용에서 제외)
        val attractionSpent = spentOf(usedPerDay, PlaceType.ATTRACTION, people)
        val foodSpent = spentOf(usedPerDay, PlaceType.RESTAURANT, people)
        val totalCost = lodgingSpent + attractionSpent + foodSpent + transportSpent
        val breakdown = BudgetBreakdown(
            lodgingBudget, attractionBudget, foodBudget, transportBudget,
            lodgingSpent, attractionSpent, foodSpent, transportSpent,
        )

        // 토큰 차감 (1토큰 = 1원, 예상 총비용만큼 / 음수 방지)
        wallet.balance = maxOf(0, wallet.balance - totalCost)
        walletRepository.save(wallet)

        return PlanResponse(
            budget, days, people, totalCost,
            budget - totalCost, breakdown, dayPlans, plannedBy, wallet.balance,
            aiReason = ai?.reason ?: describePlan(req, regionName, days, usedPerDay),
        )
    }

    private fun spentOf(perDay: List<List<Place>>, type: PlaceType, people: Int): Long =
        perDay.flatten().filter { it.type == type }.sumOf { it.price.toLong() * people }

    // ---------- 프로필·취향 반영 ----------

    /** 산·등산 관련 장소로 보이는지 (걷기 기피 사용자에게서 제외) */
    private fun looksStrenuous(p: Place): Boolean {
        val t = "${p.name} ${p.description.orEmpty()} ${p.tags.joinToString(" ")}"
        return Regex("등산|트레킹|산성|둘레길|자연휴양림|(^|[^가-힣])산([^가-힣]|$)|봉우리|[가-힣]{1,3}산\\b")
            .containsMatchIn(t)
    }

    /** 선호 키워드·음식 취향·자연어 메모를 평점 가점으로 환산 (0.0 ~ 1.5) */
    private fun preferenceBonus(p: Place, req: PlanRequest): Double {
        val text = "${p.name} ${p.description.orEmpty()} ${p.tags.joinToString(" ")}"
        var bonus = 0.0
        req.keywords?.forEach { if (text.contains(it)) bonus += 0.5 }
        if (p.type == PlaceType.RESTAURANT) {
            when (req.foodPreference) {
                "한식" -> if (Regex("한식|국밥|백반|한정식|칼국수|비빔|삼겹|갈비|해물|찌개|분식|족발|보쌈|막국수")
                        .containsMatchIn(text)) bonus += 0.6
                "양식" -> if (Regex("파스타|스테이크|피자|브런치|버거|이탈리|프렌치").containsMatchIn(text)) bonus += 0.6
                "일식" -> if (Regex("일식|스시|초밥|라멘|돈카츠|우동|이자카야").containsMatchIn(text)) bonus += 0.6
                "중식" -> if (Regex("중식|짜장|짬뽕|중화|탕수육|마라").containsMatchIn(text)) bonus += 0.6
            }
        }
        // 자연어 취향 메모: 2글자 이상 단어가 장소 정보에 나타나면 가점
        req.preferenceNote?.split(Regex("[,·\\s]+"))?.forEach { w ->
            val word = w.trim().trimEnd('요', '.', '!')
            if (word.length >= 2 && text.contains(word)) bonus += 0.4
        }
        return minOf(bonus, 1.5)
    }

    /** 캐시된 원본을 건드리지 않도록 평점만 바꾼 사본을 만든다 */
    private fun withRating(src: Place, rating: Double): Place =
        Place(src.name, src.type, src.address, src.latitude, src.longitude,
            src.price, rating, src.description).also {
            it.id = src.id
            it.tags = src.tags
        }

    /**
     * 후보 풀에 프로필을 적용한다.
     * - 걷기 최소화 ON 이고 '산'을 선호하지 않으면 산·등산 장소 제외 (전멸하면 원본 유지)
     * - 취향 가점을 rating에 반영해 이후 스코어링에서 우선 선택되게 한다
     */
    private fun applyPreferences(
        pool: PlaceProviderService.Pool,
        req: PlanRequest,
    ): PlaceProviderService.Pool {
        val likesMountain = req.keywords?.contains("산") == true
        val hasPreference = req.keywords?.isNotEmpty() == true ||
            req.foodPreference != null || req.preferenceNote != null

        fun refine(list: List<Place>, filterStrenuous: Boolean): List<Place> {
            val filtered = if (filterStrenuous) list.filterNot { looksStrenuous(it) } else list
            val base = if (filtered.size >= 5) filtered else list  // 너무 많이 걸러지면 원본 유지
            if (!hasPreference) return base
            return base.map { p ->
                val bonus = preferenceBonus(p, req)
                if (bonus <= 0.0) p else withRating(p, minOf(5.0, p.rating + bonus))
            }
        }

        val avoidMountains = req.avoidWalking && !likesMountain
        return PlaceProviderService.Pool(
            lodgings = refine(pool.lodgings, false),
            restaurants = refine(pool.restaurants, false),
            attractions = refine(pool.attractions, avoidMountains),
            source = pool.source,
        )
    }

    /** 휴리스틱 플랜에 대한 규칙 기반 설명 (AI가 이유를 못 준 경우 사용) */
    private fun describePlan(
        req: PlanRequest, regionName: String, days: Int, usedPerDay: List<List<Place>>,
    ): String {
        val parts = ArrayList<String>()
        val spots = usedPerDay.flatten().count { it.type == PlaceType.ATTRACTION }
        val meals = usedPerDay.flatten().count { it.type == PlaceType.RESTAURANT }
        parts += "${regionName} 중심으로 ${days}일 일정에 볼거리 ${spots}곳과 식사 ${meals}번을 배치했어요."
        req.purpose?.let { parts += "'${it}' 목적에 맞게 이동 부담이 적은 순서로 묶었어요." }
        if (req.avoidWalking) parts += "걷기 최소화를 켜셨으니 산·등산 코스는 빼고 이동 거리를 줄였어요."
        req.keywords?.takeIf { it.isNotEmpty() }?.let { parts += "${it.joinToString("·")} 키워드에 맞는 장소를 우선 골랐어요." }
        req.foodPreference?.takeIf { it != "상관없음" }?.let { parts += "식사는 ${it} 위주로 찾았어요." }
        req.mbti?.let { parts += "${it} 성향도 참고했어요." }
        return parts.joinToString(" ")
    }

    /** 숙박 예산을 최대한 활용: 예산 내 최고가 숙소 (동가면 평점 우선, 예산 내 없으면 최저가) */
    private fun pickLodging(lodgings: List<Place>, nights: Int, lodgingBudget: Long): Place =
        lodgings.filter { it.price.toLong() * nights <= lodgingBudget }
            .maxWithOrNull(compareBy<Place> { it.price }.thenBy { it.rating })
            ?: lodgings.minByOrNull { it.price }
            ?: throw IllegalArgumentException("이용 가능한 숙소가 없습니다")

    /**
     * 평점·숙소 근접도를 점수화해 예산 내에서 greedy 선택.
     * 개수(target)를 먼저 보장한 뒤, 남는 예산으로 저가 항목을 더 비싼(평점 좋은) 항목으로 업그레이드한다.
     */
    private fun pickPlaces(
        candidates: List<Place>, centerLat: Double, centerLng: Double,
        categoryBudget: Long, people: Int, target: Int,
    ): List<Place> {
        val scored = candidates
            .map { p ->
                val distKm = GeoUtil.distanceMeters(centerLat, centerLng, p.latitude, p.longitude) / 1000.0
                p to (p.rating * 2.0 - minOf(distKm, 40.0) * 0.22)
            }
            .sortedByDescending { it.second }
            .map { it.first }

        // 슬롯별 예산 캡: 남은 예산을 남은 슬롯 수로 나눠 개수를 먼저 보장하면서 예산을 고르게 쓴다.
        val chosen = ArrayList<Place>()
        var spent = 0L
        while (chosen.size < target) {
            val slotsLeft = target - chosen.size
            val remaining = categoryBudget - spent
            val perSlotCap = maxOf(0, remaining / slotsLeft)
            val pick = scored.firstOrNull { it !in chosen && it.price.toLong() * people <= perSlotCap }
                ?: scored.filter { it !in chosen && it.price.toLong() * people <= categoryBudget - spent }
                    .minByOrNull { it.price }
                ?: break // 더 이상 넣을 수 있는 후보가 없음
            chosen.add(pick)
            spent += pick.price.toLong() * people
        }
        // 업그레이드 패스: 남은 예산으로 저가 항목을 더 비싼 항목과 교체해 예산 사용률을 끌어올린다
        val unchosen = scored.filter { it !in chosen }
            .sortedByDescending { it.price }
            .toMutableList()
        var improved = true
        while (improved) {
            improved = false
            chosen.sortBy { it.price }
            outer@ for (i in chosen.indices) {
                val cheap = chosen[i]
                for (cand in unchosen) {
                    if (cand.price <= cheap.price) break // 이후는 전부 더 저렴
                    val newSpent = spent + (cand.price - cheap.price).toLong() * people
                    if (newSpent <= categoryBudget) {
                        chosen[i] = cand
                        unchosen.remove(cand)
                        unchosen.add(cheap)
                        unchosen.sortByDescending { it.price }
                        spent = newSpent
                        improved = true
                        break@outer
                    }
                }
            }
        }
        return chosen
    }

    /** 체인을 일수만큼 연속 구간으로 분할 (하루 2~3곳 균등 배분) */
    private fun splitByDay(chain: List<Place>, days: Int): List<MutableList<Place>> {
        val byDay = ArrayList<MutableList<Place>>()
        val total = chain.size
        val base = total / days
        val extra = total % days
        var idx = 0
        for (d in 0 until days) {
            val size = base + (if (d < extra) 1 else 0)
            byDay.add(ArrayList(chain.subList(idx, minOf(idx + size, total))))
            idx += size
        }
        return byDay
    }

    /** 각 날짜의 관광지 중심에 가까운 식당을 3곳씩 배정 */
    private fun assignRestaurants(
        restaurants: List<Place>,
        attractionsByDay: List<List<Place>>,
        baseLat: Double, baseLng: Double, days: Int,
    ): List<List<Place>> {
        val pool = ArrayList(restaurants)
        val byDay = ArrayList<List<Place>>()
        for (d in 0 until days) {
            val dayAttractions = attractionsByDay[d]
            val cLat: Double
            val cLng: Double
            if (dayAttractions.isEmpty()) {
                cLat = baseLat
                cLng = baseLng
            } else {
                cLat = dayAttractions.map { it.latitude }.average()
                cLng = dayAttractions.map { it.longitude }.average()
            }
            val picked = pool
                .sortedBy { GeoUtil.distanceMeters(cLat, cLng, it.latitude, it.longitude) }
                .take(3)
            pool.removeAll(picked)
            byDay.add(picked)
        }
        return byDay
    }

    /** 출발 시각 기준으로 아직 챙길 수 있는 식사 슬롯 (1시간 이상 지난 슬롯은 생략) */
    private fun mealSlots(dayStart: LocalTime): MutableList<LocalTime> =
        MEAL_SLOTS.filterTo(ArrayList()) { !dayStart.isAfter(it.plusMinutes(60)) }

    /** AI가 정한 순서에서 식당을 슬롯 개수까지만 남긴다 (지나간 끼니 생략) */
    private fun trimMealsToSlots(seq: List<Place>, dayStart: LocalTime): List<Place> {
        val allowed = mealSlots(dayStart).size
        val out = ArrayList<Place>()
        var meals = 0
        for (p in seq) {
            if (isMeal(p)) {
                if (meals >= allowed) continue
                meals++
            }
            out.add(p)
        }
        return out
    }

    /**
     * 하루 시퀀스 구성 (시간 기반):
     * 시계를 굴리며 08/12/17시 슬롯이 다가오면 가까운 식당을, 아니면 다음 관광지를 넣는다.
     * 출발이 늦어 지난 슬롯의 끼니는 자동 생략된다.
     */
    private fun buildDaySequenceTimed(
        startLat: Double, startLng: Double,
        dayPlaces: List<Place>, dayStart: LocalTime,
    ): List<Place> {
        val meals = dayPlaces.filterTo(ArrayList()) { isMeal(it) }
        val sights = ArrayList(nearestNeighborOrder(startLat, startLng, dayPlaces.filter { !isMeal(it) }))
        val slots = mealSlots(dayStart)

        val seq = ArrayList<Place>()
        var clock = dayStart
        var curLat = startLat
        var curLng = startLng

        while (sights.isNotEmpty() || (slots.isNotEmpty() && meals.isNotEmpty())) {
            val mealDue = slots.isNotEmpty() && meals.isNotEmpty() &&
                !clock.isBefore(slots[0].minusMinutes(45))
            if (mealDue || sights.isEmpty()) {
                if (slots.isEmpty() || meals.isEmpty()) break
                // 슬롯보다 이르면 슬롯 시각까지 기다렸다 먹는 것으로 간주
                if (clock.isBefore(slots[0])) clock = slots[0]
                val m = meals.minByOrNull {
                    GeoUtil.distanceMeters(curLat, curLng, it.latitude, it.longitude)
                }!!
                meals.remove(m)
                slots.removeAt(0)
                clock = clock.plusMinutes((travelMinutes(curLat, curLng, m) + MEAL_MIN).toLong())
                seq.add(m)
                curLat = m.latitude
                curLng = m.longitude
            } else {
                val s = sights.removeAt(0)
                clock = clock.plusMinutes((travelMinutes(curLat, curLng, s) + ATTRACTION_MIN).toLong())
                seq.add(s)
                curLat = s.latitude
                curLng = s.longitude
            }
        }
        return fixConsecutiveMeals(seq)
    }

    /** 식당이 연속으로 붙어 있으면 뒤쪽의 비식당 장소를 사이에 끼워 넣는다 */
    private fun fixConsecutiveMeals(seq: MutableList<Place>): List<Place> {
        for (i in 1 until seq.size) {
            if (isMeal(seq[i]) && isMeal(seq[i - 1])) {
                var k = -1
                for (j in i + 1 until seq.size) {
                    if (!isMeal(seq[j])) {
                        k = j
                        break
                    }
                }
                if (k > 0) {
                    seq.add(i, seq.removeAt(k))
                } else {
                    // 뒤에 비식당이 없으면 앞쪽에서 끌어온다
                    for (j in i - 2 downTo 0) {
                        if (!isMeal(seq[j])) {
                            seq.add(i - 1, seq.removeAt(j))
                            break
                        }
                    }
                }
            }
        }
        return seq
    }

    /** 최근접 이웃 휴리스틱 정렬 */
    private fun nearestNeighborOrder(startLat: Double, startLng: Double, places: List<Place>): List<Place> {
        val remaining = ArrayList(places)
        val ordered = ArrayList<Place>()
        var curLat = startLat
        var curLng = startLng
        while (remaining.isNotEmpty()) {
            val next = remaining.minByOrNull {
                GeoUtil.distanceMeters(curLat, curLng, it.latitude, it.longitude)
            }!!
            remaining.remove(next)
            ordered.add(next)
            curLat = next.latitude
            curLng = next.longitude
        }
        return ordered
    }

    companion object {
        // 직선 800m 이하만 도보 (도로를 따라 걸으면 대략 1km 이내가 되도록) — 그 이상은 대중교통
        private const val WALK_THRESHOLD_M = 800.0
        private val HHMM = DateTimeFormatter.ofPattern("HH:mm")
        private const val MEAL_MIN = 50          // 식사 시간
        private const val ATTRACTION_MIN = 60    // 관광지 이용 시간
        private const val TRANSIT_WAIT_MIN = 7   // 버스 대기 시간(평균 배차 가정)

        /** 식사 슬롯 기준 시각: 아침 08:00 / 점심 12:00 / 저녁 17:00 */
        private val MEAL_SLOTS = listOf(
            LocalTime.of(8, 0),
            LocalTime.of(12, 0),
            LocalTime.of(17, 0),
        )

        private fun isMeal(p: Place): Boolean = p.type == PlaceType.RESTAURANT

        /** 시퀀스 계획용 이동 시간 추정 (800m 이하 도보, 그 외 대중교통 평균) */
        private fun travelMinutes(fromLat: Double, fromLng: Double, to: Place): Int {
            val d = GeoUtil.distanceMeters(fromLat, fromLng, to.latitude, to.longitude)
            return maxOf(3.0, if (d <= 800) d / 67 else d / 400 + 7).toInt()
        }
    }
}
