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
import org.slf4j.LoggerFactory
import kotlin.random.Random
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

    private val log = LoggerFactory.getLogger(PlanService::class.java)

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

        // 기준점(앵커) 결정 — 서비스 범위는 전국
        //   1) region 이름이 해석되면 그 지역 대표 좌표
        //   2) 없으면 사용자 위치 (국내일 때만)
        //   3) 둘 다 아니면 기본 지역(서울)
        val region = regionService.resolve(req.region)
        val userInArea = req.startLatitude != null && req.startLongitude != null &&
            regionService.inServiceArea(req.startLatitude, req.startLongitude)
        // 고른 지역 안에 있으면(지역 중심 15km 이내) 내 위치를 기준으로 장소를 찾는다 — 홍대에 있으면 홍대 주변부터
        val userInRegion = userInArea && region != null &&
            GeoUtil.distanceMeters(region.lat, region.lng, req.startLatitude!!, req.startLongitude!!) <= NEAR_REGION_M
        val anchorLat = if (userInRegion) req.startLatitude!!
            else region?.lat ?: if (userInArea) req.startLatitude!! else regionService.default.lat
        val anchorLng = if (userInRegion) req.startLongitude!!
            else region?.lng ?: if (userInArea) req.startLongitude!! else regionService.default.lng
        val regionName = region?.name ?: if (userInArea) "현재 위치" else regionService.default.name

        // 장소 후보: TMAP POI 실시간 조회 → 키 없음/429/부족 시 수도권 시드 폴백
        // + 취향 검색: 고른 취향마다 그에 맞는 장소를 따로 찾아 섞는다 (같은 지역도 취향에 따라 다른 곳이 나온다)
        val spotsPerDay = PlanDtos.spotsPerDay(req.pace)
        val prefPlaces = placeProvider.preferencePlaces(
            anchorLat, anchorLng, PreferenceKeywords.searches(req, PREF_SEARCH_LIMIT), PREF_RADIUS_M,
        )
        val rawPool = withPreferencePlaces(placeProvider.places(anchorLat, anchorLng), prefPlaces)
        // 취향 점수는 장소를 고를 때만 쓰고, 화면에는 원래 평점을 보여준다
        val shownRating = rawPool.all().mapNotNull { p -> p.id?.let { it to p.rating } }.toMap()

        // 동선 압축: 후보는 반경 15km라 그대로 쓰면 동선이 10km 넘게 흩어진다.
        // 장소가 가장 몰린 한 구역을 골라 그 근처 장소로만 일정을 짠다 (AI·알고리즘 공통).
        // 사용자가 같은 지역(10km 이내)에 있으면 현재 위치 주변에서 구역을 찾아 첫 이동도 짧게 한다.
        val userNearAnchor = userInArea && GeoUtil.distanceMeters(
            anchorLat, anchorLng, req.startLatitude!!, req.startLongitude!!,
        ) <= USER_NEAR_ANCHOR_M
        // 프로필·취향 반영: 걷기 기피 시 산·등산 장소 제외, 선호 키워드·음식 취향 가점
        val zone = focusZone(
            applyPreferences(rawPool, req),
            if (userNearAnchor) req.startLatitude!! else anchorLat,
            if (userNearAnchor) req.startLongitude!! else anchorLng,
            days,
            spotsPerDay,
        )
        val pool = zone.pool
        val areaName = zone.label?.let { "$regionName $it 일대" } ?: regionName
        log.info("구역 선택 [{}]: 반경 {}m, 숙박 {}, 식당 {}, 관광 {}", areaName, zone.radiusM.toInt(),
            pool.lodgings.size, pool.restaurants.size, pool.attractions.size)

        // 꼭 가고 싶은 곳 — 이름으로 실제 장소를 찾는다 (걷기 기피로 걸러지기 전 원본에서, 최대 5곳)
        val mustNames = req.mustVisit.orEmpty().map { it.trim() }.filter { it.isNotEmpty() }.distinct().take(5)
        val mustFound = mustNames.associateWith { placeProvider.findByName(it, anchorLat, anchorLng, rawPool) }
        val mustPlaces = mustFound.values.filterNotNull().distinctBy { it.id }
        val mustMissing = mustFound.filterValues { it == null }.keys.toList()

        // ---------- 1차: 로컬 AI(LM Studio)에게 프로필과 함께 플랜 요청 ----------
        val profile = PlanDtos.TravelProfile(
            regionName = areaName,
            gender = req.gender,
            ageGroup = req.ageGroup,
            mbti = req.mbti,
            purpose = req.purpose,
            foodPreference = req.foodPreference,
            avoidWalking = req.avoidWalking,
            keywords = req.keywords.orEmpty(),
            preferenceNote = req.preferenceNote,
            mustVisit = mustPlaces,
            language = req.language ?: "ko",
            companion = req.companion,
            moods = req.moods.orEmpty(),
            activities = req.activities.orEmpty(),
            spotsPerDay = spotsPerDay,
        )
        // ---------- 동선 최적화: 하루 = 동네 하나 ----------
        // 날마다 볼거리·맛집이 모인 동네 중심(허브)을 정하고, 그날 장소는 그 동네 안에서만 고른다.
        // 1일차는 현재 위치 근처 동네를, 꼭 가고 싶은 곳이 있으면 그곳을 동네로 삼는다.
        val start = if (userInArea) req.startLatitude!! to req.startLongitude!! else null
        val hubs = pickDayHubs(pool, days, mustPlaces, start, anchorLat to anchorLng)
        val zones = buildDayZones(pool, hubs, spotsPerDay)
        val zoneOf = HashMap<Long, Int>()
        zones.forEachIndexed { d, z -> (z.attractions + z.restaurants).forEach { p -> p.id?.let { zoneOf[it] = d + 1 } } }
        val hubCenter = hubs.map { it.first }.average() to hubs.map { it.second }.average()
        val mustIds = mustPlaces.mapNotNull { it.id }.toSet()

        val aiCatalog = ((if (dayTrip) emptyList() else nearbyLodgings(pool.lodgings, hubCenter)) +
            zones.flatMap { it.attractions + it.restaurants } + mustPlaces).distinctBy { it.id }
        // AI가 직접 장소를 더 찾을 때 쓰는 검색: 지금 후보 목록에서 이름·취향이 맞는 곳 + TMAP 키워드 검색
        val aiSearch = AiPlanService.PlaceSearch { query, type, lat, lng, radiusM ->
            val local = pool.all().filter { p ->
                p.type == type && GeoUtil.distanceMeters(lat, lng, p.latitude, p.longitude) <= radiusM &&
                    (p.name.contains(query) || p.description.orEmpty().contains(query) || query in p.tags)
            }
            (local + placeProvider.searchKeyword(query, type, lat, lng, radiusM)).distinctBy { it.name }
        }
        var ai = aiPlanService.plan(
            budget, days, people, nights, aiCatalog, profile, zoneOf,
            search = aiSearch, anchor = (start ?: (anchorLat to anchorLng)),
        )
        // 하루 장소가 한 동네(반경 DAY_SPREAD_M) 안에 모였는지 검증 — 꼭 가고 싶은 곳은 예외
        if (ai != null && !ai.days.all { day -> isCompact(day.filter { it.id !in mustIds }) }) {
            log.warn("AI 플랜의 하루 동선이 너무 넓게 흩어짐 → 휴리스틱 폴백")
            ai = null
        }

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
            lodging = if (dayTrip) null else pickLodging(nearbyLodgings(pool.lodgings, hubCenter), nights, lodgingBudget)

            // 이 지역 최저가 숙소마저 숙박 예산을 넘으면(예: 성수기 리조트 지역),
            // 초과분을 관광·식비 예산에서 비례 차감해 총액이 예산을 넘지 않게 한다.
            val lodgingOver = maxOf(0, (lodging?.price?.toLong() ?: 0L) * nights - lodgingBudget)
            val cut = if (lodgingOver > 0 && attractionBudget + foodBudget > 0) lodgingOver else 0L
            val attractionCut = cut * attractionBudget / maxOf(1, attractionBudget + foodBudget)
            val attractionBudgetAdj = maxOf(0, attractionBudget - attractionCut)
            val foodBudgetAdj = maxOf(0, foodBudget - (cut - attractionCut))

            // 그날 동네 안에서만 관광지 3곳 + 식당 3곳 (예산은 날짜별로 나눈다)
            perDay = zones.map { z ->
                pickPlaces(z.attractions, z.lat, z.lng, attractionBudgetAdj / days, people, spotsPerDay) +
                    pickPlaces(z.restaurants, z.lat, z.lng, foodBudgetAdj / days, people, 3)
            }
            aiOrdered = false
            plannedBy = "ALGORITHM"
        }

        val mustByDay = assignMustVisit(perDay, mustPlaces, days)
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
            val dayMust = mustByDay[d]
            // 휴리스틱은 하루 장소를 받아 스스로 순서를 짜므로 미리 넣어 두고, AI 순서는 아래에서 끼워 넣는다
            val dayPlaces = if (aiOrdered) perDay[d]
                else perDay[d] + dayMust.filter { m -> perDay[d].none { it.id == m.id } }

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
                // 위치·숙소가 없으면 구역 중심에서 출발 (첫 이동이 짧게)
                startLat = zone.lat
                startLng = zone.lng
                startName = "출발 지점"
                startType = "START"
            }

            // 식사는 08/12/17시 슬롯 근처에만 배치하고, 이미 지난 슬롯의 끼니는 생략한다.
            // 끼니 정리로 꼭 가고 싶은 곳이 빠졌으면 다시 넣는다
            val ordered = ensureIncluded(
                if (aiOrdered) {
                    trimMealsToSlots(fixConsecutiveMeals(ArrayList(dayPlaces)), dayStarts[d])
                } else {
                    buildDaySequenceTimed(startLat, startLng, dayPlaces, dayStarts[d])
                },
                dayMust,
            )
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
                    p.id?.let { shownRating[it] } ?: p.rating, p.description,
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
        AdminService.plansGenerated.incrementAndGet()

        return PlanResponse(
            budget, days, people, totalCost,
            budget - totalCost, breakdown, dayPlans, plannedBy, wallet.balance,
            aiReason = listOfNotNull(
                ai?.reason ?: describePlan(req, regionName, zone.label, days, usedPerDay),
                mustVisitNote(mustPlaces, mustMissing),
            ).joinToString(" "),
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

    /**
     * 취향 점수 (평점에 더해 장소 선택 순위에만 쓴다 — 화면에는 원래 평점이 나간다).
     * - 취향 검색으로 찾아온 장소(태그 = 취향): +1.2 — 가장 확실한 신호
     * - 장소 이름·설명에 취향 단어가 보이면: +0.6
     * - 누구와 가는지에 따라 가감 (아이·가족이면 술집 제외 수준으로 감점 등)
     * - 자유 메모의 단어가 보이면: +0.4
     */
    private fun preferenceBonus(p: Place, req: PlanRequest): Double {
        val text = "${p.name} ${p.description.orEmpty()} ${p.tags.joinToString(" ")}"
        var bonus = 0.0
        val foods = PreferenceKeywords.foods(req)
        for (label in PreferenceKeywords.others(req) + foods) {
            if (label in foods && p.type != PlaceType.RESTAURANT) continue
            val tagged = label in p.tags
            val matched = tagged || (PreferenceKeywords.of(label)?.match?.containsMatchIn(text)
                ?: (label.length >= 2 && text.contains(label)))  // 직접 입력한 취향
            if (matched) bonus += if (tagged) 1.2 else 0.6
        }
        bonus += companionBonus(p, text, req.companion)
        req.preferenceNote?.split(Regex("[,·\\s]+"))?.forEach { w ->
            val word = w.trim().trimEnd('요', '.', '!')
            if (word.length >= 2 && text.contains(word)) bonus += 0.4
        }
        return bonus.coerceIn(-3.0, 3.0)
    }

    /** 누구와 가는지에 따른 가감 */
    private fun companionBonus(p: Place, text: String, companion: String?): Double = when (companion) {
        "아이와 함께", "가족" -> when {
            p.type == PlaceType.RESTAURANT && DRINKING.containsMatchIn(text) -> -3.0
            Regex("체험|공원|동물원|아쿠아리움|박물관|과학관|목장|테마파크").containsMatchIn(text) -> 0.6
            else -> 0.0
        }
        "연인" -> if (Regex("야경|전망|카페|루프탑|정원|와인|해변|산책").containsMatchIn(text)) 0.5 else 0.0
        "친구" -> if (Regex("시장|핫플|체험|포차|펍|테마파크|거리").containsMatchIn(text)) 0.4 else 0.0
        "혼자" -> if (Regex("카페|서점|전시|미술관|산책|공원|박물관").containsMatchIn(text)) 0.4 else 0.0
        else -> 0.0
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
        val hasPreference = PreferenceKeywords.others(req).isNotEmpty() ||
            PreferenceKeywords.foods(req).isNotEmpty() || req.preferenceNote != null || req.companion != null

        fun refine(list: List<Place>, filterStrenuous: Boolean): List<Place> {
            val filtered = if (filterStrenuous) list.filterNot { looksStrenuous(it) } else list
            val base = if (filtered.size >= 5) filtered else list  // 너무 많이 걸러지면 원본 유지
            if (!hasPreference) return base
            return base.map { p ->
                val bonus = preferenceBonus(p, req)
                if (bonus == 0.0) p else withRating(p, (p.rating + bonus).coerceAtLeast(0.0))
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

    /** 하루 장소들이 모두 그 중심에서 DAY_SPREAD_M 안에 있는지 */
    private fun isCompact(places: List<Place>): Boolean {
        if (places.size < 2) return true
        val cLat = places.map { it.latitude }.average()
        val cLng = places.map { it.longitude }.average()
        return places.all { GeoUtil.distanceMeters(cLat, cLng, it.latitude, it.longitude) <= DAY_SPREAD_M }
    }

    /** 기본 후보에 취향 검색 장소를 합친다 (같은 이름이면 취향 태그가 붙은 쪽을 남긴다) */
    private fun withPreferencePlaces(
        base: PlaceProviderService.Pool, pref: List<Place>,
    ): PlaceProviderService.Pool {
        if (pref.isEmpty()) return base
        fun merge(list: List<Place>, type: PlaceType): List<Place> {
            val extra = pref.filter { it.type == type }.distinctBy { it.name }
            val names = extra.map { it.name }.toSet()
            return list.filter { it.name !in names } + extra
        }
        return PlaceProviderService.Pool(
            lodgings = base.lodgings,
            restaurants = merge(base.restaurants, PlaceType.RESTAURANT),
            attractions = merge(base.attractions, PlaceType.ATTRACTION),
            source = base.source + "+PREF",
        )
    }

    /**
     * 점수 1등만 고르면 같은 조건에서 늘 같은 곳이 나온다.
     * 상위 몇 개 중 1등과 점수 차가 크지 않은 후보 사이에서 무작위로 고른다.
     */
    private fun <T> pickVaried(items: List<T>, score: (T) -> Double): T {
        val ranked = items.map { it to score(it) }.sortedByDescending { it.second }
        val best = ranked.first().second
        return ranked.take(VARIETY_TOP).filter { it.second >= best - VARIETY_MARGIN }.random().first
    }

    /** 꼭 가고 싶은 곳을 일자에 배정 — 플래너가 이미 넣은 날이 있으면 그날, 없으면 그날 장소들과 가장 가까운 날 */
    private fun assignMustVisit(perDay: List<List<Place>>, must: List<Place>, days: Int): List<List<Place>> {
        val out = List(days) { ArrayList<Place>() }
        for (m in must) {
            val already = perDay.indexOfFirst { day -> day.any { it.id == m.id } }
            val d = if (already >= 0) already else (0 until days).minByOrNull { day ->
                perDay.getOrNull(day).orEmpty().minOfOrNull {
                    GeoUtil.distanceMeters(it.latitude, it.longitude, m.latitude, m.longitude)
                } ?: Double.MAX_VALUE
            } ?: 0
            out[d].add(m)
        }
        return out
    }

    /** 빠진 꼭 가고 싶은 곳을 그날 동선에서 가장 가까운 장소 바로 뒤에 끼워 넣는다 */
    private fun ensureIncluded(seq: List<Place>, must: List<Place>): List<Place> {
        if (must.isEmpty()) return seq
        val out = ArrayList(seq)
        for (m in must) {
            if (out.any { it.id == m.id }) continue
            val nearest = out.indices.minByOrNull {
                GeoUtil.distanceMeters(out[it].latitude, out[it].longitude, m.latitude, m.longitude)
            }
            if (nearest == null) out.add(m) else out.add(nearest + 1, m)
        }
        return out
    }

    private fun mustVisitNote(found: List<Place>, missing: List<String>): String? {
        val parts = ArrayList<String>()
        if (found.isNotEmpty()) parts += "꼭 가고 싶다고 하신 곳 ${found.size}곳을 일정에 넣었어요."
        if (missing.isNotEmpty()) {
            parts += "${missing.joinToString(", ") { "'$it'" }}은(는) 이 지역에서 찾지 못해 넣지 못했어요."
        }
        return parts.takeIf { it.isNotEmpty() }?.joinToString(" ")
    }

    // ---------- 동선 압축: 한 구역 집중 ----------

    /** 일정을 짤 구역 — 구역 안 장소만 담은 후보 풀 + 구역 중심 + 반경 + 동네 이름 */
    private data class Zone(
        val pool: PlaceProviderService.Pool,
        val lat: Double,
        val lng: Double,
        val radiusM: Double,
        val label: String?,
    )

    /**
     * 후보(기준점 반경 15km)가 넓게 퍼져 있어 동선이 10km 넘게 나오는 문제를 막는다.
     * 1) 검색 기준점 근처(4km)에서 볼거리·맛집이 가장 많이 몰린 지점(평점 가중)을 구역 중심으로 고르고
     * 2) 일정에 필요한 만큼 모일 때까지 반경을 1.2km → 5km 로 넓혀 그 안의 장소만 남긴다.
     * 구역 안에 장소가 모자라면 구역 중심에서 가까운 순으로 채운다.
     */
    private fun focusZone(
        pool: PlaceProviderService.Pool,
        searchLat: Double,
        searchLng: Double,
        days: Int,
        spotsPerDay: Int,
    ): Zone {
        val spots = pool.attractions + pool.restaurants
        if (spots.isEmpty()) return Zone(pool, searchLat, searchLng, 0.0, null)

        fun dist(aLat: Double, aLng: Double, p: Place) =
            GeoUtil.distanceMeters(aLat, aLng, p.latitude, p.longitude)

        // 1) 핫스팟: 반경 1.2km 안 장소들의 평점 합이 가장 큰 지점 (기준점에서 멀수록 km당 0.3점 감점)
        val candidates = spots.filter { dist(searchLat, searchLng, it) <= HOTSPOT_SEARCH_M }.ifEmpty { spots }
        val center = pickVaried(candidates) { c ->
            spots.sumOf { p -> if (dist(c.latitude, c.longitude, p) <= ZONE_RADII.first()) p.rating else 0.0 } -
                dist(searchLat, searchLng, c) / 1000.0 * 0.3
        }
        val cLat = center.latitude
        val cLng = center.longitude

        // 2) 필요한 만큼 모일 때까지 반경 확장
        val needAttr = days * spotsPerDay
        val needRest = days * 3
        fun within(list: List<Place>, r: Double) = list.filter { dist(cLat, cLng, it) <= r }
        val radius = ZONE_RADII.firstOrNull { r ->
            within(pool.attractions, r).size >= needAttr && within(pool.restaurants, r).size >= needRest
        } ?: ZONE_RADII.last()

        // 구역 안이 모자라면 구역 중심에서 가까운 순으로 채운다 (먼 곳이 섞이지 않게)
        fun pick(list: List<Place>, need: Int): List<Place> {
            val inZone = within(list, radius)
            return if (inZone.size >= need) inZone else list.sortedBy { dist(cLat, cLng, it) }.take(need)
        }
        val lodgings = within(pool.lodgings, maxOf(radius, LODGING_MIN_RADIUS_M))
            .ifEmpty { pool.lodgings.sortedBy { dist(cLat, cLng, it) }.take(5) }

        // 동네 이름 (예: "서울 중구 명동2가" → "명동2가")
        val tokens = center.address.split(" ").filter { it.isNotBlank() }
        val label = if (center.address.startsWith("주소")) null
        else tokens.getOrNull(2)?.takeIf { !it.first().isDigit() } ?: tokens.getOrNull(1)

        return Zone(
            PlaceProviderService.Pool(
                lodgings = lodgings,
                restaurants = pick(pool.restaurants, needRest),
                attractions = pick(pool.attractions, needAttr),
                source = pool.source,
            ),
            cLat, cLng, radius, label,
        )
    }

    /** 휴리스틱 플랜에 대한 규칙 기반 설명 (AI가 이유를 못 준 경우 사용) — 앱 화면 언어로 */
    private fun describePlan(
        req: PlanRequest, regionName: String, zoneLabel: String?, days: Int, usedPerDay: List<List<Place>>,
    ): String = PlanDescriber.describe(
        lang = req.language,
        regionName = regionName,
        zoneLabel = zoneLabel,
        days = days,
        spots = usedPerDay.flatten().count { it.type == PlaceType.ATTRACTION },
        meals = usedPerDay.flatten().count { it.type == PlaceType.RESTAURANT },
        purpose = req.purpose,
        avoidWalking = req.avoidWalking,
        keywords = req.keywords,
        foodPreference = req.foodPreference,
        mbti = req.mbti,
    )

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
                // 동네 안에서도 가까운 곳 우선 — 평점 0.5점 차이가 거리 1km와 맞먹는다
                p to (p.rating * 2.0 - minOf(distKm, 40.0) * 1.0 + Random.nextDouble(0.0, SCORE_JITTER))
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

    /** 하루 동선이 머무는 동네 — 중심 좌표와 그 안의 관광지·식당 후보 */
    private data class DayZone(
        val lat: Double, val lng: Double,
        val attractions: List<Place>, val restaurants: List<Place>,
    )

    private fun dist(lat: Double, lng: Double, p: Place) =
        GeoUtil.distanceMeters(lat, lng, p.latitude, p.longitude)

    /**
     * 날짜별 동네 중심(허브)을 고른다.
     * 1) 꼭 가고 싶은 곳은 그 자체가 그날의 동네 (서로 가까우면 같은 날)
     * 2) 나머지 날은 반경 안에 볼거리·맛집이 많이 모인 곳, 날마다 다른 동네가 되도록 서로 떨어뜨린다
     * 마지막으로 현재 위치(없으면 기준점)에서 가까운 순으로 날짜를 매긴다 — 1일차는 내 근처 동네.
     */
    private fun pickDayHubs(
        pool: PlaceProviderService.Pool, days: Int, must: List<Place>,
        start: Pair<Double, Double>?, anchor: Pair<Double, Double>,
    ): List<Pair<Double, Double>> {
        val spots = pool.attractions + pool.restaurants
        fun density(c: Place): Double {
            val near = spots.filter { dist(c.latitude, c.longitude, it) <= HUB_RADIUS_M }
            val a = near.count { it.type == PlaceType.ATTRACTION }
            val r = near.count { it.type == PlaceType.RESTAURANT }
            val balanced = if (a >= 2 && r >= 2) 3.0 else 0.0
            return minOf(a, 6) + minOf(r, 6) * 0.8 + balanced + c.rating * 0.3
        }
        val hubs = ArrayList<Pair<Double, Double>>()
        fun apart(lat: Double, lng: Double, gap: Double) =
            hubs.all { GeoUtil.distanceMeters(it.first, it.second, lat, lng) >= gap }

        for (m in must) {
            if (hubs.size >= days) break
            if (apart(m.latitude, m.longitude, HUB_RADIUS_M * 2)) hubs.add(m.latitude to m.longitude)
        }
        val origin = start ?: anchor
        val candidates = pool.attractions.map { it to density(it) }
        while (hubs.size < days) {
            // 1일차 후보는 현재 위치에서 멀수록 감점 (km당 1.5점)
            val nearStart = hubs.isEmpty() && start != null
            val options = candidates.filter { (p, _) -> apart(p.latitude, p.longitude, HUB_MIN_GAP_M) }
                .ifEmpty { candidates.filter { (p, _) -> apart(p.latitude, p.longitude, HUB_RADIUS_M) } }
            if (options.isEmpty()) break
            val pick = pickVaried(options) { (p, score) ->
                if (nearStart) score - dist(origin.first, origin.second, p) / 1000.0 * 1.5 else score
            }
            hubs.add(pick.first.latitude to pick.first.longitude)
        }
        while (hubs.size < days) hubs.add(hubs.lastOrNull() ?: anchor)

        // 현재 위치에서 시작해 가까운 동네부터 차례로 (최근접 이웃)
        val ordered = ArrayList<Pair<Double, Double>>()
        val left = ArrayList(hubs)
        var cur = origin
        while (left.isNotEmpty()) {
            val next = left.minByOrNull { GeoUtil.distanceMeters(cur.first, cur.second, it.first, it.second) }!!
            left.remove(next)
            ordered.add(next)
            cur = next
        }
        return ordered
    }

    /**
     * 동네마다 반경 안의 관광지·식당 후보를 모은다. 후보가 3곳씩 안 되면 반경을 조금씩 넓힌다.
     * 같은 장소가 두 날에 들어가지 않도록 앞선 날이 쓴 장소는 제외한다.
     */
    private fun buildDayZones(
        pool: PlaceProviderService.Pool, hubs: List<Pair<Double, Double>>, spotsPerDay: Int,
    ): List<DayZone> {
        val used = HashSet<Long>()
        return hubs.map { (lat, lng) ->
            var attractions = emptyList<Place>()
            var restaurants = emptyList<Place>()
            for (radius in ZONE_RADII_M) {
                attractions = pool.attractions.filter { it.id !in used && dist(lat, lng, it) <= radius }
                restaurants = pool.restaurants.filter { it.id !in used && dist(lat, lng, it) <= radius }
                if (attractions.size >= spotsPerDay && restaurants.size >= 3) break
            }
            fun top(list: List<Place>) =
                list.sortedByDescending { it.rating - dist(lat, lng, it) / 1000.0 }.take(ZONE_CANDIDATES)
            val zone = DayZone(lat, lng, top(attractions), top(restaurants))
            (zone.attractions + zone.restaurants).forEach { p -> p.id?.let(used::add) }
            zone
        }
    }

    /** 동네들 가까이의 숙소만 후보로 (멀리 있는 비싼 숙소 때문에 매일 먼 길을 오가지 않게) */
    private fun nearbyLodgings(lodgings: List<Place>, center: Pair<Double, Double>): List<Place> =
        lodgings.sortedBy { dist(center.first, center.second, it) }.take(NEARBY_LODGINGS)

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

        // 하루 동선 = 동네 하나: 허브 반경, 날마다 다른 동네가 되도록 허브 간 최소 거리
        private const val HUB_RADIUS_M = 1500.0
        private const val HUB_MIN_GAP_M = 3000.0
        private val ZONE_RADII_M = listOf(1500.0, 2500.0, 4000.0) // 후보가 모자라면 넓혀 본다
        private const val ZONE_CANDIDATES = 12   // 동네당 관광지·식당 후보 수 (AI 프롬프트 길이도 줄인다)
        private const val NEARBY_LODGINGS = 8
        private const val NEAR_REGION_M = 15_000.0  // 이 안이면 "그 지역에 와 있다"고 본다
        private const val PREF_SEARCH_LIMIT = 8       // 취향 검색어 수 (외부 API 호출 수)
        private const val DAY_SPREAD_M = 3_000.0      // AI 일정 검증: 하루 장소가 중심에서 이 안에 있어야 한다
        private const val PREF_RADIUS_M = 12_000.0    // 취향 검색 결과로 받을 거리
        private const val SCORE_JITTER = 0.8          // 장소 점수 무작위 폭 (평점 0.4점 정도)
        private const val VARIETY_TOP = 3             // 동네 중심을 고를 상위 후보 수
        private const val VARIETY_MARGIN = 2.0        // 1등과 이 점수 차 안의 후보만 무작위로 섞는다
        private val DRINKING = Regex("주점|술집|포차|이자카야|호프|펍|와인바|칵테일|bar", RegexOption.IGNORE_CASE)
        // 동선 압축 — 한 구역 집중
        private const val HOTSPOT_SEARCH_M = 4_000.0        // 구역 중심을 찾는 범위 (검색 기준점에서)
        private val ZONE_RADII = listOf(1_200.0, 1_800.0, 2_500.0, 3_500.0, 5_000.0) // 구역 반경 확장 단계
        private const val LODGING_MIN_RADIUS_M = 3_000.0    // 숙소는 구역보다 조금 넓게 찾는다
        private const val USER_NEAR_ANCHOR_M = 10_000.0     // 사용자가 이 거리 안이면 현재 위치 주변에서 구역 탐색
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
