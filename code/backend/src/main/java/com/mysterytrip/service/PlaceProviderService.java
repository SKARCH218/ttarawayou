package com.mysterytrip.service;

import com.mysterytrip.entity.Place;
import com.mysterytrip.entity.Place.PlaceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 플랜에 쓸 장소 후보 공급자 — 전국 대응.
 * 기준 좌표 주변의 관광명소·숙박·음식점을 TMAP POI로 실시간 조회한다.
 * TMAP POI에는 가격·평점이 없어 이름 해시 기반의 결정적 추정값을 부여한다(테스트용).
 */
@Service
public class PlaceProviderService {

    private static final Logger log = LoggerFactory.getLogger(PlaceProviderService.class);
    private static final int FETCH_COUNT = 60; // 카테고리당 후보 수
    private static final int MIN_USABLE = 6;   // 이보다 적으면 시드 폴백

    public record Pool(List<Place> lodgings, List<Place> restaurants, List<Place> attractions,
                       String source) {
        public List<Place> all() {
            List<Place> out = new ArrayList<>(lodgings);
            out.addAll(restaurants);
            out.addAll(attractions);
            return out;
        }
    }

    private final TmapService tmapService;
    private final Map<String, Pool> cache = new ConcurrentHashMap<>();
    private final AtomicLong idSeq = new AtomicLong(1_000_000);

    public PlaceProviderService(TmapService tmapService) {
        this.tmapService = tmapService;
    }

    public Pool places(double lat, double lng) {
        if (!tmapService.enabled()) {
            throw new IllegalArgumentException(
                    "장소 검색을 사용할 수 없습니다 — TMAP 앱키(TMAP_APP_KEY)를 확인해 주세요.");
        }
        String key = String.format(Locale.US, "%.2f,%.2f", lat, lng);
        Pool cached = cache.get(key);
        if (cached != null) return cached;
        Pool pool = fetchFromTmap(lat, lng);
        cache.put(key, pool);
        return pool;
    }

    private Pool fetchFromTmap(double lat, double lng) {
        List<Place> lodgings = toPlaces(tmapService.poisAround(lat, lng, "숙박", FETCH_COUNT), PlaceType.LODGING);
        List<Place> restaurants = toPlaces(tmapService.poisAround(lat, lng, "음식점", FETCH_COUNT), PlaceType.RESTAURANT);
        List<Place> attractions = toPlaces(tmapService.poisAround(lat, lng, "관광명소", FETCH_COUNT), PlaceType.ATTRACTION);
        if (restaurants.size() < MIN_USABLE || attractions.size() < MIN_USABLE) {
            throw new IllegalArgumentException(
                    "이 지역 주변에서 여행 장소를 충분히 찾지 못했습니다. 잠시 후 다시 시도해 주세요.");
        }
        String region = restaurants.get(0).getAddress().split(" ").length > 1
                ? restaurants.get(0).getAddress() : "현재 지역";
        log.info("TMAP 지역 장소 조회 완료 [{}]: 숙박 {}, 식당 {}, 관광 {}",
                region, lodgings.size(), restaurants.size(), attractions.size());
        return new Pool(lodgings, restaurants, attractions, "TMAP:" + region);
    }

    private List<Place> toPlaces(List<TmapService.Poi> pois, PlaceType type) {
        List<Place> out = new ArrayList<>();
        for (TmapService.Poi poi : pois) {
            if (poi.name().isBlank()) continue;
            // 주차장·입구 등 부속 POI 제외
            String n = poi.name();
            if (n.contains("주차장") || n.endsWith("입구") || n.contains("화장실")) continue;
            int h = Math.abs(n.hashCode());
            int price = switch (type) {
                case LODGING -> 60_000 + (h % 12) * 10_000;          // 6만~17만/박
                case RESTAURANT -> 8_000 + (h % 5) * 2_000;          // 8천~1.6만/인
                case ATTRACTION -> (h % 3 == 0) ? 0 : 1_000 + (h % 5) * 1_000; // 무료~5천
            };
            double rating = Math.round((3.8 + (h % 12) * 0.1) * 10) / 10.0;   // 3.8~4.9
            Place p = new Place(n, type, poi.address().isBlank() ? "주소 정보 없음" : poi.address(),
                    poi.lat(), poi.lng(), price, rating, "TMAP 검색 결과 (가격은 추정)");
            p.setId(idSeq.incrementAndGet());
            out.add(p);
        }
        return out;
    }

}
