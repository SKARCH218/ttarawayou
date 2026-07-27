package com.mysterytrip.service;

import com.mysterytrip.dto.PlanDtos.*;
import com.mysterytrip.entity.Place;
import com.mysterytrip.entity.Place.PlaceType;
import com.mysterytrip.repository.PlaceRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 예산 기반 플랜 생성.
 * 배분: 숙박 40% / 관광 30% / 식비 20% / 교통 10%
 * 1) 숙박 예산 내 최고 평점 숙소
 * 2) 관광 예산 내 하루 2~3곳 (평점·숙소 근접도·입장료 점수화)
 * 3) 식비 예산 내 하루 3끼 (같은 방식)
 * 4) 하루 일정을 최근접 이웃 휴리스틱으로 정렬, 마지막엔 숙소 복귀
 * 5) 교통 예산(10%)을 초과하면 대중교통 대신 도보로 대체
 */
@Service
public class PlanService {

    private static final double WALK_THRESHOLD_M = 1600; // 이 거리 이하는 기본 도보

    private final PlaceRepository repository;
    private final RouteService routeService;

    public PlanService(PlaceRepository repository, RouteService routeService) {
        this.repository = repository;
        this.routeService = routeService;
    }

    public PlanResponse createPlan(PlanRequest req) {
        int days = Math.max(1, Math.min(req.days(), 7));
        int people = Math.max(1, req.people());
        long budget = Math.max(0, req.budget());

        long lodgingBudget = budget * 40 / 100;
        long attractionBudget = budget * 30 / 100;
        long foodBudget = budget * 20 / 100;
        long transportBudget = budget * 10 / 100;

        // 1) 숙소 선택: 예산 내 최고 평점 (예산 내 없으면 최저가)
        int nights = Math.max(1, days - 1);
        List<Place> lodgings = repository.findByType(PlaceType.LODGING);
        Place lodging = lodgings.stream()
                .filter(l -> (long) l.getPrice() * nights <= lodgingBudget)
                .max(Comparator.comparingDouble(Place::getRating)
                        .thenComparing(Comparator.comparingInt(Place::getPrice).reversed()))
                .orElseGet(() -> lodgings.stream()
                        .min(Comparator.comparingInt(Place::getPrice)).orElseThrow());
        long lodgingSpent = (long) lodging.getPrice() * nights;

        // 2) 관광지 선택
        List<Place> attractions = pickPlaces(
                repository.findByType(PlaceType.ATTRACTION), lodging,
                attractionBudget, people, days * 3, days * 2, 50000.0);
        long attractionSpent = attractions.stream()
                .mapToLong(a -> (long) a.getPrice() * people).sum();

        // 3) 식당 선택 (하루 3끼)
        List<Place> restaurants = pickPlaces(
                repository.findByType(PlaceType.RESTAURANT), lodging,
                foodBudget, people, days * 3, days * 3, 30000.0);
        long foodSpent = restaurants.stream()
                .mapToLong(r -> (long) r.getPrice() * people).sum();

        // 4) 일자별 배정: 숙소 기준 최근접 체인으로 정렬 후 연속 구간을 나눠 지리적으로 묶는다
        List<List<Place>> attractionsByDay = splitByDay(chainFrom(lodging, attractions), days);
        List<List<Place>> restaurantsByDay = assignRestaurants(restaurants, attractionsByDay, lodging, days);

        // 5) 일자별 동선 생성
        List<DayPlanDto> dayPlans = new ArrayList<>();
        long transportSpent = 0;
        for (int d = 0; d < days; d++) {
            List<Place> dayPlaces = new ArrayList<>();
            dayPlaces.addAll(attractionsByDay.get(d));
            dayPlaces.addAll(restaurantsByDay.get(d));

            // 출발점: 1일차는 사용자 현재 위치(있으면), 그 외/폴백은 숙소
            double startLat, startLng;
            String startName, startType;
            boolean fromUserLocation = d == 0 && req.startLatitude() != null && req.startLongitude() != null;
            if (fromUserLocation) {
                startLat = req.startLatitude();
                startLng = req.startLongitude();
                startName = "현재 위치";
                startType = "START";
            } else {
                startLat = lodging.getLatitude();
                startLng = lodging.getLongitude();
                startName = lodging.getName();
                startType = "LODGING";
            }

            List<Place> ordered = nearestNeighborOrder(startLat, startLng, dayPlaces);

            List<StopDto> stops = new ArrayList<>();
            stops.add(new StopDto(fromUserLocation ? null : lodging.getId(), startName, startType,
                    fromUserLocation ? "출발 지점" : lodging.getAddress(),
                    startLat, startLng, 0,
                    fromUserLocation ? 0 : lodging.getRating(),
                    fromUserLocation ? "여행의 시작점" : lodging.getDescription()));
            for (Place p : ordered) {
                stops.add(new StopDto(p.getId(), p.getName(), p.getType().name(), p.getAddress(),
                        p.getLatitude(), p.getLongitude(), (long) p.getPrice() * people,
                        p.getRating(), p.getDescription()));
            }
            // 마지막엔 숙소 복귀
            stops.add(new StopDto(lodging.getId(), lodging.getName(), "LODGING", lodging.getAddress(),
                    lodging.getLatitude(), lodging.getLongitude(), 0,
                    lodging.getRating(), lodging.getDescription()));

            List<LegDto> legs = new ArrayList<>();
            for (int i = 0; i < stops.size() - 1; i++) {
                StopDto a = stops.get(i);
                StopDto b = stops.get(i + 1);
                double straight = GeoUtil.distanceMeters(
                        a.latitude(), a.longitude(), b.latitude(), b.longitude());
                LegDto leg;
                if (straight <= WALK_THRESHOLD_M) {
                    leg = routeService.walkLeg(a.latitude(), a.longitude(), b.latitude(), b.longitude());
                } else {
                    LegDto transit = routeService.transitLeg(
                            a.latitude(), a.longitude(), b.latitude(), b.longitude());
                    long groupFare = transit.fare() * people;
                    if (transportSpent + groupFare <= transportBudget) {
                        transportSpent += groupFare;
                        leg = new LegDto(transit.mode(), transit.distanceMeters(),
                                transit.durationMinutes(), groupFare, transit.summary(), transit.path());
                    } else {
                        // 교통 예산 초과 → 도보 대체
                        leg = routeService.walkLeg(a.latitude(), a.longitude(), b.latitude(), b.longitude());
                    }
                }
                legs.add(leg);
            }

            long dayCost = stops.stream().mapToLong(StopDto::cost).sum()
                    + legs.stream().mapToLong(LegDto::fare).sum();
            dayPlans.add(new DayPlanDto(d + 1, stops, legs, dayCost));
        }

        long totalCost = lodgingSpent + attractionSpent + foodSpent + transportSpent;
        BudgetBreakdown breakdown = new BudgetBreakdown(
                lodgingBudget, attractionBudget, foodBudget, transportBudget,
                lodgingSpent, attractionSpent, foodSpent, transportSpent);

        return new PlanResponse(budget, days, people, totalCost,
                budget - totalCost, breakdown, dayPlans);
    }

    /**
     * 평점·숙소 근접도·가격을 점수화해 예산 내에서 greedy 선택.
     * target: 목표 개수, minimum: 최소 확보 개수(무료/저가로 보충)
     */
    private List<Place> pickPlaces(List<Place> candidates, Place lodging,
                                   long categoryBudget, int people,
                                   int target, int minimum, double priceScale) {
        record Scored(Place place, double score) {}
        List<Scored> scored = candidates.stream()
                .map(p -> {
                    double distKm = GeoUtil.distanceMeters(lodging.getLatitude(), lodging.getLongitude(),
                            p.getLatitude(), p.getLongitude()) / 1000.0;
                    double score = p.getRating() * 2.0
                            - Math.min(distKm, 40) * 0.22
                            - (p.getPrice() * (double) people) / priceScale;
                    return new Scored(p, score);
                })
                .sorted(Comparator.comparingDouble(Scored::score).reversed())
                .toList();

        List<Place> chosen = new ArrayList<>();
        long spent = 0;
        for (Scored s : scored) {
            if (chosen.size() >= target) break;
            long cost = (long) s.place().getPrice() * people;
            if (spent + cost <= categoryBudget) {
                chosen.add(s.place());
                spent += cost;
            }
        }
        // 최소 개수 미달이면 남은 후보 중 저렴한 순으로 예산 무시하지 않는 선에서 보충 (무료 위주)
        if (chosen.size() < minimum) {
            List<Place> rest = scored.stream().map(Scored::place)
                    .filter(p -> !chosen.contains(p))
                    .sorted(Comparator.comparingInt(Place::getPrice))
                    .toList();
            for (Place p : rest) {
                if (chosen.size() >= minimum) break;
                long cost = (long) p.getPrice() * people;
                if (spent + cost <= categoryBudget || cost == 0) {
                    chosen.add(p);
                    spent += cost;
                }
            }
        }
        return chosen;
    }

    /** 숙소에서 시작하는 최근접 이웃 체인 (지리적 연속성 확보용) */
    private List<Place> chainFrom(Place lodging, List<Place> places) {
        return nearestNeighborOrder(lodging.getLatitude(), lodging.getLongitude(), places);
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
                                                Place lodging, int days) {
        List<Place> pool = new ArrayList<>(restaurants);
        List<List<Place>> byDay = new ArrayList<>();
        for (int d = 0; d < days; d++) {
            List<Place> dayAttractions = attractionsByDay.get(d);
            double cLat, cLng;
            if (dayAttractions.isEmpty()) {
                cLat = lodging.getLatitude();
                cLng = lodging.getLongitude();
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
