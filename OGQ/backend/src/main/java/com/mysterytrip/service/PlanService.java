package com.mysterytrip.service;

import com.mysterytrip.dto.PlanDtos.*;
import com.mysterytrip.entity.Place;
import com.mysterytrip.entity.Place.PlaceType;
import com.mysterytrip.entity.Wallet;
import com.mysterytrip.repository.WalletRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 예산 기반 플랜 생성.
 * 배분: 숙박 40% / 관광 30% / 식비 20% / 교통 10%
 *
 * 1차: LM Studio 로컬 AI에게 숙소·일자별 방문 순서를 짜게 한다 (AiPlanService).
 * 실패 시 휴리스틱 폴백:
 *   숙박 예산 내 최고 평점 숙소 → 관광 예산 내 하루 2~3곳(평점·근접도·입장료 점수화)
 *   → 식비 예산 내 하루 3끼 → 최근접 이웃 동선 최적화.
 * 공통: 마지막엔 숙소 복귀, 교통 예산(10%) 초과 시 대중교통 대신 도보.
 */
@Service
public class PlanService {

    // 직선 800m 이하만 도보 (도로를 따라 걸으면 대략 1km 이내가 되도록) — 그 이상은 대중교통
    private static final double WALK_THRESHOLD_M = 800;
    private static final java.time.format.DateTimeFormatter HHMM =
            java.time.format.DateTimeFormatter.ofPattern("HH:mm");
    private static final int MEAL_MIN = 50;          // 식사 시간
    private static final int ATTRACTION_MIN = 60;    // 관광지 이용 시간
    private static final int TRANSIT_WAIT_MIN = 7;   // 버스 대기 시간(평균 배차 가정)

    private final RouteService routeService;
    private final AiPlanService aiPlanService;
    private final WalletRepository walletRepository;
    private final PlaceProviderService placeProvider;

    public PlanService(RouteService routeService,
                       AiPlanService aiPlanService, WalletRepository walletRepository,
                       PlaceProviderService placeProvider) {
        this.routeService = routeService;
        this.aiPlanService = aiPlanService;
        this.walletRepository = walletRepository;
        this.placeProvider = placeProvider;
    }

    public PlanResponse createPlan(PlanRequest req) {
        int days = Math.max(1, Math.min(req.days(), 7));
        int people = Math.max(1, req.people());
        long budget = Math.max(0, req.budget());
        boolean dayTrip = days == 1;         // 당일치기: 숙박 없음
        int nights = dayTrip ? 0 : days - 1;

        // 토큰 검증: 예산은 보유 토큰(1토큰 = 1원)을 넘을 수 없다
        Wallet wallet = walletRepository.findById(1L)
                .orElseGet(() -> walletRepository.save(new Wallet(1L, Wallet.INITIAL_BALANCE)));
        if (budget > wallet.getBalance()) {
            throw new IllegalArgumentException(
                    "보유 토큰이 부족합니다 (보유 " + wallet.getBalance() + "토큰, 요청 예산 " + budget + "토큰)");
        }

        // 예산 배분 — 당일치기는 숙박 몫을 관광·식비로 재배분
        long lodgingBudget = dayTrip ? 0 : budget * 40 / 100;
        long attractionBudget = dayTrip ? budget * 55 / 100 : budget * 30 / 100;
        long foodBudget = dayTrip ? budget * 35 / 100 : budget * 20 / 100;
        long transportBudget = budget * 10 / 100;

        // 기준점(앵커): 사용자 위치 → 없으면 기본 지역(서울 시청)
        double anchorLat = req.startLatitude() != null ? req.startLatitude() : 37.5665;
        double anchorLng = req.startLongitude() != null ? req.startLongitude() : 126.9780;

        // 장소 후보: 기준점 주변을 TMAP POI로 실시간 조회 (전국)
        PlaceProviderService.Pool pool = placeProvider.places(anchorLat, anchorLng);

        // ---------- 1차: 로컬 AI(LM Studio)에게 플랜 요청 ----------
        List<Place> aiCatalog = dayTrip
                ? pool.all().stream().filter(p -> p.getType() != PlaceType.LODGING).toList()
                : pool.all();
        AiPlanService.AiSelection ai = aiPlanService.plan(budget, days, people, nights, aiCatalog);

        Place lodging;              // 당일치기면 null
        List<List<Place>> perDay;   // 일자별 장소 (AI면 방문 순서 그대로, 휴리스틱이면 미정렬)
        boolean aiOrdered;
        String plannedBy;

        if (ai != null) {
            lodging = dayTrip ? null : ai.lodging();
            perDay = ai.days();
            aiOrdered = true;       // AI가 정한 방문 순서를 존중한다
            plannedBy = "AI";
        } else {
            // ---------- 폴백: 휴리스틱 ----------
            lodging = dayTrip ? null
                    : pickLodging(pool.lodgings(), nights, lodgingBudget);
            double cLat = lodging != null ? lodging.getLatitude() : anchorLat;
            double cLng = lodging != null ? lodging.getLongitude() : anchorLng;

            List<Place> attractions = pickPlaces(
                    pool.attractions(), cLat, cLng,
                    attractionBudget, people, days * 3);
            List<Place> restaurants = pickPlaces(
                    pool.restaurants(), cLat, cLng,
                    foodBudget, people, days * 3);

            List<List<Place>> attractionsByDay = splitByDay(
                    nearestNeighborOrder(cLat, cLng, attractions), days);
            List<List<Place>> restaurantsByDay = assignRestaurants(
                    restaurants, attractionsByDay, cLat, cLng, days);
            perDay = new ArrayList<>();
            for (int d = 0; d < days; d++) {
                List<Place> merged = new ArrayList<>();
                merged.addAll(attractionsByDay.get(d));
                merged.addAll(restaurantsByDay.get(d));
                perDay.add(merged);
            }
            aiOrdered = false;
            plannedBy = "ALGORITHM";
        }

        long lodgingSpent = lodging == null ? 0 : (long) lodging.getPrice() * nights;

        // 일자별 출발 시각: 1일차는 현재 시각(밤·새벽이면 09:00), 이후 날은 09:00
        List<java.time.LocalTime> dayStarts = new ArrayList<>();
        for (int d = 0; d < days; d++) {
            if (d == 0) {
                java.time.LocalTime now = java.time.LocalTime.now(java.time.ZoneId.of("Asia/Seoul"));
                now = now.withMinute(now.getMinute() / 5 * 5).withSecond(0).withNano(0);
                boolean sane = !now.isBefore(java.time.LocalTime.of(8, 0))
                        && !now.isAfter(java.time.LocalTime.of(19, 0));
                dayStarts.add(sane ? now : java.time.LocalTime.of(9, 0));
            } else {
                dayStarts.add(java.time.LocalTime.of(9, 0));
            }
        }

        // ---------- 일자별 동선·이동 구간 생성 ----------
        List<DayPlanDto> dayPlans = new ArrayList<>();
        List<List<Place>> usedPerDay = new ArrayList<>(); // 실제 일정에 들어간 장소(식사 슬롯 반영)
        long transportSpent = 0;
        for (int d = 0; d < days; d++) {
            List<Place> dayPlaces = perDay.get(d);

            // 출발점: 1일차는 사용자 현재 위치(있으면), 그 외/폴백은 숙소.
            // 당일치기에서 위치가 없으면 시내 기준점에서 출발한다
            double startLat, startLng;
            String startName, startType;
            boolean fromUserLocation = d == 0 && req.startLatitude() != null && req.startLongitude() != null;
            if (fromUserLocation) {
                startLat = req.startLatitude();
                startLng = req.startLongitude();
                startName = "현재 위치";
                startType = "START";
            } else if (lodging != null) {
                startLat = lodging.getLatitude();
                startLng = lodging.getLongitude();
                startName = lodging.getName();
                startType = "LODGING";
            } else {
                startLat = anchorLat;
                startLng = anchorLng;
                startName = "출발 지점";
                startType = "START";
            }

            // 식사는 08/12/17시 슬롯 근처에만 배치하고, 이미 지난 슬롯의 끼니는 생략한다.
            // AI 순서는 존중하되 연속 식당 보정 + 슬롯 수만큼만 식당 유지
            List<Place> ordered = aiOrdered
                    ? trimMealsToSlots(fixConsecutiveMeals(new ArrayList<>(dayPlaces)), dayStarts.get(d))
                    : buildDaySequenceTimed(startLat, startLng, dayPlaces, dayStarts.get(d));
            usedPerDay.add(ordered);

            List<StopDto> stops = new ArrayList<>();
            boolean startIsLodging = "LODGING".equals(startType);
            stops.add(new StopDto(startIsLodging ? lodging.getId() : null, startName, startType,
                    startIsLodging ? lodging.getAddress() : "출발 지점",
                    startLat, startLng, 0,
                    startIsLodging ? lodging.getRating() : 0,
                    startIsLodging ? lodging.getDescription() : "여행의 시작점"));
            for (Place p : ordered) {
                stops.add(new StopDto(p.getId(), p.getName(), p.getType().name(), p.getAddress(),
                        p.getLatitude(), p.getLongitude(), (long) p.getPrice() * people,
                        p.getRating(), p.getDescription()));
            }
            // 마지막엔 숙소 복귀 (당일치기는 숙소가 없으므로 마지막 장소에서 종료)
            if (lodging != null) {
                stops.add(new StopDto(lodging.getId(), lodging.getName(), "LODGING", lodging.getAddress(),
                        lodging.getLatitude(), lodging.getLongitude(), 0,
                        lodging.getRating(), lodging.getDescription()));
            }

            List<LegDto> legs = new ArrayList<>();
            for (int i = 0; i < stops.size() - 1; i++) {
                StopDto a = stops.get(i);
                StopDto b = stops.get(i + 1);
                double straight = GeoUtil.distanceMeters(
                        a.latitude(), a.longitude(), b.latitude(), b.longitude());
                LegDto leg;
                if (straight <= WALK_THRESHOLD_M) {
                    leg = routeService.walkLeg(a.latitude(), a.longitude(), b.latitude(), b.longitude());
                    // 직선은 짧아도 실제 도보 경로가 1km를 넘으면(호수·강 우회 등) 대중교통으로 전환
                    if (leg.distanceMeters() > 1000) {
                        LegDto transit = routeService.transitLeg(
                                a.latitude(), a.longitude(), b.latitude(), b.longitude());
                        long groupFare = transit.fare() * people;
                        transportSpent += groupFare;
                        leg = transit.withFare(groupFare);
                    }
                } else {
                    // 1km 이상 걷지 않는다: 장거리 구간은 교통 예산이 넘어도 대중교통 유지
                    LegDto transit = routeService.transitLeg(
                            a.latitude(), a.longitude(), b.latitude(), b.longitude());
                    long groupFare = transit.fare() * people;
                    transportSpent += groupFare;
                    leg = transit.withFare(groupFare);
                }
                legs.add(leg);
            }

            // 일정표 시각 계산 — 버스 구간은 평균 대기 7분 포함, 식당 50분·관광지 60분 체류 반영
            java.time.LocalTime clock = dayStarts.get(d);
            for (int i = 0; i < legs.size(); i++) {
                java.time.LocalTime depart = clock;
                int moveMin = legs.get(i).durationMinutes()
                        + ("TRANSIT".equals(legs.get(i).mode()) ? TRANSIT_WAIT_MIN : 0);
                clock = clock.plusMinutes(moveMin);
                legs.set(i, legs.get(i).withTimes(depart.format(HHMM), clock.format(HHMM)));
                String nextType = stops.get(i + 1).type();
                clock = clock.plusMinutes(
                        "RESTAURANT".equals(nextType) ? MEAL_MIN
                                : "ATTRACTION".equals(nextType) ? ATTRACTION_MIN : 0);
            }

            long dayCost = stops.stream().mapToLong(StopDto::cost).sum()
                    + legs.stream().mapToLong(LegDto::fare).sum();
            dayPlans.add(new DayPlanDto(d + 1, stops, legs, dayCost));
        }

        // 실제 일정에 들어간 장소 기준으로 비용 계산 (생략된 끼니는 비용에서 제외)
        long attractionSpent = spentOf(usedPerDay, PlaceType.ATTRACTION, people);
        long foodSpent = spentOf(usedPerDay, PlaceType.RESTAURANT, people);
        long totalCost = lodgingSpent + attractionSpent + foodSpent + transportSpent;
        BudgetBreakdown breakdown = new BudgetBreakdown(
                lodgingBudget, attractionBudget, foodBudget, transportBudget,
                lodgingSpent, attractionSpent, foodSpent, transportSpent);

        // 토큰 차감 (1토큰 = 1원, 예상 총비용만큼 / 음수 방지)
        wallet.setBalance(Math.max(0, wallet.getBalance() - totalCost));
        walletRepository.save(wallet);

        return new PlanResponse(budget, days, people, totalCost,
                budget - totalCost, breakdown, dayPlans, plannedBy, wallet.getBalance());
    }

    private static long spentOf(List<List<Place>> perDay, PlaceType type, int people) {
        return perDay.stream().flatMap(List::stream)
                .filter(p -> p.getType() == type)
                .mapToLong(p -> (long) p.getPrice() * people).sum();
    }

    /** 숙박 예산을 최대한 활용: 예산 내 최고가 숙소 (동가면 평점 우선, 예산 내 없으면 최저가) */
    private Place pickLodging(List<Place> lodgings, int nights, long lodgingBudget) {
        return lodgings.stream()
                .filter(l -> (long) l.getPrice() * nights <= lodgingBudget)
                .max(Comparator.comparingInt(Place::getPrice)
                        .thenComparingDouble(Place::getRating))
                .orElseGet(() -> lodgings.stream()
                        .min(Comparator.comparingInt(Place::getPrice)).orElseThrow());
    }

    /**
     * 평점·숙소 근접도를 점수화해 예산 내에서 greedy 선택.
     * 예산을 최대한 활용하는 방향: 개수(target)를 먼저 보장한 뒤,
     * 남는 예산으로 저가 항목을 더 비싼(평점 좋은) 항목으로 업그레이드한다.
     */
    private List<Place> pickPlaces(List<Place> candidates, double centerLat, double centerLng,
                                   long categoryBudget, int people, int target) {
        record Scored(Place place, double score) {}
        List<Scored> scored = candidates.stream()
                .map(p -> {
                    double distKm = GeoUtil.distanceMeters(centerLat, centerLng,
                            p.getLatitude(), p.getLongitude()) / 1000.0;
                    double score = p.getRating() * 2.0 - Math.min(distKm, 40) * 0.22;
                    return new Scored(p, score);
                })
                .sorted(Comparator.comparingDouble(Scored::score).reversed())
                .toList();

        // 슬롯별 예산 캡: 남은 예산을 남은 슬롯 수로 나눠, 개수(하루 3끼 등)를
        // 먼저 보장하면서 예산을 고르게 쓴다. 캡 내 후보가 없으면 저렴한 것으로 채운다.
        List<Place> chosen = new ArrayList<>();
        long spent = 0;
        while (chosen.size() < target) {
            int slotsLeft = target - chosen.size();
            long remaining = categoryBudget - spent;
            long perSlotCap = Math.max(0, remaining / slotsLeft);
            final long fSpent = spent;
            Place pick = scored.stream().map(Scored::place)
                    .filter(p -> !chosen.contains(p))
                    .filter(p -> (long) p.getPrice() * people <= perSlotCap)
                    .findFirst()
                    .orElseGet(() -> scored.stream().map(Scored::place)
                            .filter(p -> !chosen.contains(p))
                            .filter(p -> (long) p.getPrice() * people <= categoryBudget - fSpent)
                            .min(Comparator.comparingInt(Place::getPrice))
                            .orElse(null));
            if (pick == null) break; // 더 이상 넣을 수 있는 후보가 없음
            chosen.add(pick);
            spent += (long) pick.getPrice() * people;
        }
        // 업그레이드 패스: 남은 예산으로 저가 항목을 더 비싼 항목과 교체해 예산 사용률을 끌어올린다
        List<Place> unchosen = new ArrayList<>(scored.stream().map(Scored::place)
                .filter(p -> !chosen.contains(p))
                .sorted(Comparator.comparingInt(Place::getPrice).reversed())
                .toList());
        boolean improved = true;
        while (improved) {
            improved = false;
            chosen.sort(Comparator.comparingInt(Place::getPrice));
            for (int i = 0; i < chosen.size() && !improved; i++) {
                Place cheap = chosen.get(i);
                for (Place cand : unchosen) {
                    if (cand.getPrice() <= cheap.getPrice()) break; // 이후는 전부 더 저렴
                    long newSpent = spent + (long) (cand.getPrice() - cheap.getPrice()) * people;
                    if (newSpent <= categoryBudget) {
                        chosen.set(i, cand);
                        unchosen.remove(cand);
                        unchosen.add(cheap);
                        unchosen.sort(Comparator.comparingInt(Place::getPrice).reversed());
                        spent = newSpent;
                        improved = true;
                        break;
                    }
                }
            }
        }
        return chosen;
    }

    /** 체인을 일수만큼 연속 구간으로 분할 (하루 2~3곳 균등 배분) */
    private List<List<Place>> splitByDay(List<Place> chain, int days) {
        List<List<Place>> byDay = new ArrayList<>();
        int total = chain.size();
        int base = total / days;
        int extra = total % days;
        int idx = 0;
        for (int d = 0; d < days; d++) {
            int size = base + (d < extra ? 1 : 0);
            byDay.add(new ArrayList<>(chain.subList(idx, Math.min(idx + size, total))));
            idx += size;
        }
        return byDay;
    }

    /** 각 날짜의 관광지 중심에 가까운 식당을 3곳씩 배정 */
    private List<List<Place>> assignRestaurants(List<Place> restaurants,
                                                List<List<Place>> attractionsByDay,
                                                double baseLat, double baseLng, int days) {
        List<Place> pool = new ArrayList<>(restaurants);
        List<List<Place>> byDay = new ArrayList<>();
        for (int d = 0; d < days; d++) {
            List<Place> dayAttractions = attractionsByDay.get(d);
            double cLat, cLng;
            if (dayAttractions.isEmpty()) {
                cLat = baseLat;
                cLng = baseLng;
            } else {
                cLat = dayAttractions.stream().mapToDouble(Place::getLatitude).average().orElse(0);
                cLng = dayAttractions.stream().mapToDouble(Place::getLongitude).average().orElse(0);
            }
            final double fLat = cLat, fLng = cLng;
            List<Place> picked = pool.stream()
                    .sorted(Comparator.comparingDouble(r ->
                            GeoUtil.distanceMeters(fLat, fLng, r.getLatitude(), r.getLongitude())))
                    .limit(3)
                    .collect(Collectors.toList());
            pool.removeAll(picked);
            byDay.add(picked);
        }
        return byDay;
    }

    private static boolean isMeal(Place p) {
        return p.getType() == PlaceType.RESTAURANT;
    }

    /** 식사 슬롯 기준 시각: 아침 08:00 / 점심 12:00 / 저녁 17:00 */
    private static final java.time.LocalTime[] MEAL_SLOTS = {
            java.time.LocalTime.of(8, 0),
            java.time.LocalTime.of(12, 0),
            java.time.LocalTime.of(17, 0),
    };

    /** 출발 시각 기준으로 아직 챙길 수 있는 식사 슬롯 (1시간 이상 지난 슬롯은 생략) */
    private static List<java.time.LocalTime> mealSlots(java.time.LocalTime dayStart) {
        List<java.time.LocalTime> out = new ArrayList<>();
        for (java.time.LocalTime slot : MEAL_SLOTS) {
            if (!dayStart.isAfter(slot.plusMinutes(60))) out.add(slot);
        }
        return out;
    }

    /** AI가 정한 순서에서 식당을 슬롯 개수까지만 남긴다 (지나간 끼니 생략) */
    private List<Place> trimMealsToSlots(List<Place> seq, java.time.LocalTime dayStart) {
        int allowed = mealSlots(dayStart).size();
        List<Place> out = new ArrayList<>();
        int meals = 0;
        for (Place p : seq) {
            if (isMeal(p)) {
                if (meals >= allowed) continue;
                meals++;
            }
            out.add(p);
        }
        return out;
    }

    /**
     * 하루 시퀀스 구성 (시간 기반):
     * 시계를 굴리며 08/12/17시 슬롯이 다가오면 가까운 식당을, 아니면 다음 관광지를 넣는다.
     * 출발이 늦어 지난 슬롯의 끼니는 자동 생략된다.
     */
    private List<Place> buildDaySequenceTimed(double startLat, double startLng,
                                              List<Place> dayPlaces,
                                              java.time.LocalTime dayStart) {
        List<Place> meals = new ArrayList<>(dayPlaces.stream().filter(PlanService::isMeal).toList());
        List<Place> sights = new ArrayList<>(nearestNeighborOrder(startLat, startLng,
                dayPlaces.stream().filter(p -> !isMeal(p)).toList()));
        List<java.time.LocalTime> slots = new ArrayList<>(mealSlots(dayStart));

        List<Place> seq = new ArrayList<>();
        java.time.LocalTime clock = dayStart;
        double curLat = startLat, curLng = startLng;

        while (!sights.isEmpty() || (!slots.isEmpty() && !meals.isEmpty())) {
            boolean mealDue = !slots.isEmpty() && !meals.isEmpty()
                    && !clock.isBefore(slots.get(0).minusMinutes(45));
            if (mealDue || sights.isEmpty()) {
                if (slots.isEmpty() || meals.isEmpty()) break;
                // 슬롯보다 이르면 슬롯 시각까지 기다렸다 먹는 것으로 간주
                if (clock.isBefore(slots.get(0))) clock = slots.get(0);
                double fLat = curLat, fLng = curLng;
                Place m = meals.stream().min(Comparator.comparingDouble(p ->
                                GeoUtil.distanceMeters(fLat, fLng, p.getLatitude(), p.getLongitude())))
                        .orElseThrow();
                meals.remove(m);
                slots.remove(0);
                clock = clock.plusMinutes(travelMinutes(curLat, curLng, m) + MEAL_MIN);
                seq.add(m);
                curLat = m.getLatitude();
                curLng = m.getLongitude();
            } else {
                Place s = sights.remove(0);
                clock = clock.plusMinutes(travelMinutes(curLat, curLng, s) + ATTRACTION_MIN);
                seq.add(s);
                curLat = s.getLatitude();
                curLng = s.getLongitude();
            }
        }
        return fixConsecutiveMeals(seq);
    }

    /** 시퀀스 계획용 이동 시간 추정 (800m 이하 도보, 그 외 대중교통 평균) */
    private static int travelMinutes(double fromLat, double fromLng, Place to) {
        double d = GeoUtil.distanceMeters(fromLat, fromLng, to.getLatitude(), to.getLongitude());
        return (int) Math.max(3, d <= 800 ? d / 67 : d / 400 + 7);
    }

    /** 식당이 연속으로 붙어 있으면 뒤쪽의 비식당 장소를 사이에 끼워 넣는다 */
    private List<Place> fixConsecutiveMeals(List<Place> seq) {
        for (int i = 1; i < seq.size(); i++) {
            if (isMeal(seq.get(i)) && isMeal(seq.get(i - 1))) {
                int k = -1;
                for (int j = i + 1; j < seq.size(); j++) {
                    if (!isMeal(seq.get(j))) { k = j; break; }
                }
                if (k > 0) {
                    seq.add(i, seq.remove(k));
                } else {
                    // 뒤에 비식당이 없으면 앞쪽에서 끌어온다
                    for (int j = i - 2; j >= 0; j--) {
                        if (!isMeal(seq.get(j))) {
                            seq.add(i - 1, seq.remove(j));
                            break;
                        }
                    }
                }
            }
        }
        return seq;
    }

    /** 최근접 이웃 휴리스틱 정렬 */
    private List<Place> nearestNeighborOrder(double startLat, double startLng, List<Place> places) {
        List<Place> remaining = new ArrayList<>(places);
        List<Place> ordered = new ArrayList<>();
        double curLat = startLat, curLng = startLng;
        while (!remaining.isEmpty()) {
            final double lat = curLat, lng = curLng;
            Place next = remaining.stream()
                    .min(Comparator.comparingDouble(p ->
                            GeoUtil.distanceMeters(lat, lng, p.getLatitude(), p.getLongitude())))
                    .orElseThrow();
            remaining.remove(next);
            ordered.add(next);
            curLat = next.getLatitude();
            curLng = next.getLongitude();
        }
        return ordered;
    }
}
