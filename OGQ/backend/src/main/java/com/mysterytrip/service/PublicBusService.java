package com.mysterytrip.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 대한민국 공공데이터포털 — 국토교통부(TAGO) 전국 버스 정보.
 *  - 버스정류소정보(BusSttnInfoInqireService): 좌표 → 근접 정류소·도시코드
 *  - 버스노선정보(BusRouteInfoInqireService): 노선번호 → 노선ID → 경유 정류소 시퀀스
 *  - 버스도착정보(ArvlInfoInqireService): 정류소별 실시간 도착 예정
 * 전국 어디서든 좌표로 도시코드를 자동 판별한다.
 * 키(DATA_GO_KR_API_KEY)가 없거나 호출이 실패하면 null/빈 결과로 폴백한다.
 */
@Service
public class PublicBusService {

    private static final Logger log = LoggerFactory.getLogger(PublicBusService.class);
    private static final String BASE = "https://apis.data.go.kr/1613000";
    private static final double STATION_MATCH_M = 700; // 승/하차 정류장 매칭 허용 거리

    /** 정류소 (근접 조회 결과) */
    public record Station(String nodeId, String name, String cityCode, double lat, double lng) {}

    /** 도착 예정 정보 */
    public record Arrival(String routeNo, String routeType, int arrTimeSec,
                          int prevStationCount, String vehicleType) {}

    private final RestClient http;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String encodedKey;
    private volatile boolean dead; // 키가 TAGO에 등록되지 않은 경우 재시도 중단

    /** "cityCode:busNo" → 노선 후보, routeId → 정류소 시퀀스, 좌표 → 근접 정류소 목록 */
    private final Map<String, List<JsonNode>> routeCache = new ConcurrentHashMap<>();
    private final Map<String, List<double[]>> stationCache = new ConcurrentHashMap<>();
    private final Map<String, List<Station>> nearCache = new ConcurrentHashMap<>();
    private volatile Map<String, String> cityNames; // citycode → cityname (예: 37020 → 경주시)

    public PublicBusService(@Value("${datago.api-key}") String apiKey) {
        String key = apiKey == null ? "" : apiKey.trim();
        this.encodedKey = key.contains("%") ? key : URLEncoder.encode(key, StandardCharsets.UTF_8);
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(3000);
        f.setReadTimeout(6000);
        this.http = RestClient.builder().requestFactory(f).build();
    }

    public boolean enabled() {
        return !encodedKey.isEmpty() && !dead;
    }

    // ---------- ① 좌표 → 근접 정류소 (도시코드 자동 판별) ----------

    /**
     * 좌표 주변 정류소 목록 (가까운 순).
     * 시 경계 근처에서는 인접 도시 소속 정류소가 섞여 나오므로 목록 전체를 반환한다.
     */
    public List<Station> nearestStations(double lat, double lng) {
        if (!enabled()) return List.of();
        String key = String.format(Locale.US, "%.3f,%.3f", lat, lng);
        List<Station> cached = nearCache.get(key);
        if (cached != null) return cached;
        List<Station> out = new ArrayList<>();
        try {
            JsonNode items = call("/BusSttnInfoInqireService/getCrdntPrxmtSttnList",
                    String.format(Locale.US, "&gpsLati=%f&gpsLong=%f&numOfRows=10", lat, lng));
            for (JsonNode it : asArray(items)) {
                Station st = new Station(
                        it.path("nodeid").asText(), it.path("nodenm").asText(),
                        it.path("citycode").asText(),
                        it.path("gpslati").asDouble(), it.path("gpslong").asDouble());
                if (!st.nodeId().isEmpty() && !st.cityCode().isEmpty()) out.add(st);
            }
            nearCache.put(key, out);
        } catch (Exception e) {
            handleFailure("근접 정류소 조회", e);
        }
        return out;
    }

    /** 좌표에서 가장 가까운 정류소. 실패 시 null */
    public Station nearestStation(double lat, double lng) {
        List<Station> list = nearestStations(lat, lng);
        return list.isEmpty() ? null : list.get(0);
    }

    /** 도시코드 → 도시명 (예: 37020 → 경주시). 실패/미확인 시 빈 문자열 */
    public String cityName(String cityCode) {
        if (!enabled() || cityCode == null) return "";
        try {
            if (cityNames == null) {
                Map<String, String> map = new ConcurrentHashMap<>();
                JsonNode items = call("/BusSttnInfoInqireService/getCtyCodeList", "");
                for (JsonNode it : asArray(items)) {
                    map.put(it.path("citycode").asText(), it.path("cityname").asText());
                }
                cityNames = map;
            }
            return cityNames.getOrDefault(cityCode, "");
        } catch (Exception e) {
            handleFailure("도시코드 목록 조회", e);
            return "";
        }
    }

    // ---------- ② 버스 경로 (노선의 경유 정류소 구간) ----------

    /**
     * 버스번호와 승차/하차 좌표로 실제 노선의 경유 정류소 구간을 찾는다.
     * 도시코드는 승차 좌표로 자동 판별한다. 반환: [lat, lng] 목록, 실패 시 null
     */
    public List<double[]> stationsBetween(String rawBusNo,
                                          double boardLat, double boardLng,
                                          double alightLat, double alightLng) {
        if (!enabled()) return null;
        try {
            String busNo = rawBusNo.replaceAll("\\(.*$", "").trim();
            if (busNo.isEmpty()) return null;
            // 시 경계에서는 인접 도시 정류소가 섞이므로 주변 정류소들의 도시코드를 모두 시도
            List<String> cityCodes = nearestStations(boardLat, boardLng).stream()
                    .map(Station::cityCode).distinct().toList();

            for (String cityCode : cityCodes) {
                for (JsonNode route : findRoutes(cityCode, busNo)) {
                    String routeId = route.path("routeid").asText();
                    if (routeId.isEmpty()) continue;
                    List<double[]> stations = findStations(cityCode, routeId);
                    if (stations.size() < 2) continue;
                    int bi = nearestIndex(stations, boardLat, boardLng);
                    int ai = nearestIndex(stations, alightLat, alightLng);
                    if (bi < 0 || ai < 0 || bi >= ai) continue; // 방향 불일치(반대 방향 노선 등)
                    if (GeoUtil.distanceMeters(stations.get(bi)[0], stations.get(bi)[1], boardLat, boardLng) > STATION_MATCH_M
                            || GeoUtil.distanceMeters(stations.get(ai)[0], stations.get(ai)[1], alightLat, alightLng) > STATION_MATCH_M) {
                        continue;
                    }
                    log.info("TAGO 버스 노선 매칭: {}번 (도시 {}, routeId {}, 정류소 {}→{})",
                            busNo, cityCode, routeId, bi, ai);
                    return new ArrayList<>(stations.subList(bi, ai + 1));
                }
            }
        } catch (Exception e) {
            handleFailure("버스노선 조회", e);
        }
        return null;
    }

    // ---------- ③ 실시간 도착 정보 ----------

    /**
     * 좌표에서 가장 가까운 정류소의 실시간 버스 도착 예정.
     * busNo가 주어지면 해당 노선만 필터링한다. 반환: (정류소, 도착목록), 실패 시 null
     */
    public Map.Entry<Station, List<Arrival>> arrivalsNear(double lat, double lng, String busNoFilter) {
        if (!enabled()) return null;
        Station st = nearestStation(lat, lng);
        if (st == null) return null;
        List<Arrival> out = new ArrayList<>();
        try {
            JsonNode items = call("/ArvlInfoInqireService/getSttnAcctoArvlPrearngeInfoList",
                    String.format(Locale.US, "&cityCode=%s&nodeId=%s&numOfRows=30",
                            st.cityCode(), URLEncoder.encode(st.nodeId(), StandardCharsets.UTF_8)));
            String filter = busNoFilter == null ? "" : busNoFilter.replaceAll("\\(.*$", "").trim();
            for (JsonNode it : asArray(items)) {
                String routeNo = it.path("routeno").asText();
                if (!filter.isEmpty() && !routeNo.equalsIgnoreCase(filter)) continue;
                out.add(new Arrival(routeNo, it.path("routetp").asText(),
                        it.path("arrtime").asInt(), it.path("arrprevstationcnt").asInt(),
                        it.path("vehicletp").asText()));
            }
            out.sort((a, b) -> Integer.compare(a.arrTimeSec(), b.arrTimeSec()));
        } catch (Exception e) {
            handleFailure("도착정보 조회", e);
        }
        return Map.entry(st, out);
    }

    // ---------- TAGO API 공통 ----------

    private List<JsonNode> findRoutes(String cityCode, String busNo) throws Exception {
        String cacheKey = cityCode + ":" + busNo;
        List<JsonNode> cached = routeCache.get(cacheKey);
        if (cached != null) return cached;
        JsonNode items = call("/BusRouteInfoInqireService/getRouteNoList",
                String.format(Locale.US, "&cityCode=%s&routeNo=%s&numOfRows=50",
                        cityCode, URLEncoder.encode(busNo, StandardCharsets.UTF_8)));
        // 조회가 부분일치라(예: '10' → 100, 710…) 번호가 정확히 같은 노선을 앞에 둔다
        List<JsonNode> exact = new ArrayList<>();
        List<JsonNode> others = new ArrayList<>();
        for (JsonNode it : asArray(items)) {
            if (it.path("routeno").asText().equalsIgnoreCase(busNo)) exact.add(it);
            else others.add(it);
        }
        exact.addAll(others);
        routeCache.put(cacheKey, exact);
        return exact;
    }

    private List<double[]> findStations(String cityCode, String routeId) throws Exception {
        String cacheKey = cityCode + ":" + routeId;
        List<double[]> cached = stationCache.get(cacheKey);
        if (cached != null) return cached;
        JsonNode items = call("/BusRouteInfoInqireService/getRouteAcctoThrghSttnList",
                String.format(Locale.US, "&cityCode=%s&routeId=%s&numOfRows=500",
                        cityCode, URLEncoder.encode(routeId, StandardCharsets.UTF_8)));
        List<JsonNode> list = new ArrayList<>(asArray(items));
        list.sort((a, b) -> Integer.compare(a.path("nodeord").asInt(), b.path("nodeord").asInt()));
        List<double[]> out = new ArrayList<>();
        for (JsonNode it : list) {
            double lat = it.path("gpslati").asDouble();
            double lng = it.path("gpslong").asDouble();
            if (lat != 0 && lng != 0) out.add(new double[]{lat, lng});
        }
        stationCache.put(cacheKey, out);
        return out;
    }

    private JsonNode call(String path, String params) throws Exception {
        String url = BASE + path + "?serviceKey=" + encodedKey + "&_type=json&pageNo=1" + params;
        String body = http.get().uri(URI.create(url)).retrieve().body(String.class);
        JsonNode root;
        try {
            root = mapper.readTree(body);
        } catch (Exception e) {
            throw new IllegalStateException("JSON 아님: " + body.substring(0, Math.min(160, body.length())));
        }
        String code = root.path("response").path("header").path("resultCode").asText();
        if (!"00".equals(code)) {
            throw new IllegalStateException("resultCode=" + code + " "
                    + root.path("response").path("header").path("resultMsg").asText());
        }
        return root.path("response").path("body").path("items").path("item");
    }

    private void handleFailure(String what, Exception e) {
        String msg = String.valueOf(e.getMessage());
        if (msg.contains("403")) {
            dead = true;
            log.warn("TAGO API 인증 거부(403) — 키 활용신청 상태 확인 필요. 이번 실행에서는 비활성화");
        } else {
            log.warn("TAGO {} 실패: {}", what, msg);
        }
    }

    /** items.item이 단건(객체)일 수도, 배열일 수도 있어 통일한다 */
    private static List<JsonNode> asArray(JsonNode item) {
        List<JsonNode> out = new ArrayList<>();
        if (item == null || item.isMissingNode() || item.isNull()) return out;
        if (item.isArray()) item.forEach(out::add);
        else out.add(item);
        return out;
    }

    private static int nearestIndex(List<double[]> stations, double lat, double lng) {
        int best = -1;
        double bestDist = Double.MAX_VALUE;
        for (int i = 0; i < stations.size(); i++) {
            double d = GeoUtil.distanceMeters(stations.get(i)[0], stations.get(i)[1], lat, lng);
            if (d < bestDist) {
                bestDist = d;
                best = i;
            }
        }
        return best;
    }
}
