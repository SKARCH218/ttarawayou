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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 공공데이터포털 — 국토교통부(TAGO) 시외버스정보.
 * 시외 이동(도시 간 30km 이상) 구간에서 실제 시외버스 터미널·시간표·요금을 조회한다.
 *  - /GetSuberbsBusTrminlList        : 도시코드 → 터미널 목록
 *  - /GetStrtpntAlocFndSuberbsBusInfo: 출발/도착 터미널 → 당일 배차(출발·도착시각, 요금, 등급)
 * 키가 이 API에 활용신청되지 않았으면(403) 자동 비활성화되고 기존 방식으로 폴백한다.
 */
@Service
public class IntercityBusService {

    private static final Logger log = LoggerFactory.getLogger(IntercityBusService.class);
    private static final String BASE = "https://apis.data.go.kr/1613000/SuburbsBusInfo";

    /** 다음 시외버스 배차 정보 */
    public record Departure(String depTerminalNm, String arrTerminalNm,
                            String depTime, String arrTime, long charge, String grade) {}

    private record Terminal(String id, String name) {}

    private final RestClient http;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String encodedKey;
    private volatile boolean dead;

    private final Map<String, List<Terminal>> terminalCache = new ConcurrentHashMap<>();
    private final Map<String, List<JsonNode>> timetableCache = new ConcurrentHashMap<>();

    public IntercityBusService(@Value("${datago.api-key}") String apiKey) {
        String key = apiKey == null ? "" : apiKey.trim();
        this.encodedKey = key.contains("%") ? key : URLEncoder.encode(key, StandardCharsets.UTF_8);
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(3000);
        f.setReadTimeout(8000);
        this.http = RestClient.builder().requestFactory(f).build();
    }

    public boolean enabled() {
        return !encodedKey.isEmpty() && !dead;
    }

    /**
     * 두 도시 사이의 다음 시외버스 배차 (지금 이후 첫 출발).
     * 이 API는 광역시·도 단위 코드(11=서울, 37=경북…)를 쓰므로
     * 시내버스 도시코드(37020)의 앞 2자리로 변환하고, 도시명으로 터미널을 거른다.
     * 터미널 조합을 순서대로 시도하고, 배차가 없으면 null.
     */
    public Departure nextDeparture(String depCityCode, String depCityName,
                                   String arrCityCode, String arrCityName) {
        if (!enabled() || depCityCode == null || arrCityCode == null
                || depCityCode.equals(arrCityCode)) {
            return null;
        }
        try {
            List<Terminal> deps = terminalsForCity(depCityCode, depCityName);
            List<Terminal> arrs = terminalsForCity(arrCityCode, arrCityName);
            String today = LocalDate.now(ZoneId.of("Asia/Seoul"))
                    .format(DateTimeFormatter.BASIC_ISO_DATE);
            long nowStamp = Long.parseLong(LocalDateTime.now(ZoneId.of("Asia/Seoul"))
                    .format(DateTimeFormatter.ofPattern("yyyyMMddHHmm")));

            for (Terminal dep : deps.subList(0, Math.min(3, deps.size()))) {
                for (Terminal arr : arrs.subList(0, Math.min(3, arrs.size()))) {
                    for (JsonNode t : timetable(dep.id(), arr.id(), today)) {
                        long depPland = t.path("depplandtime").asLong(t.path("depPlandTime").asLong(0));
                        if (depPland < nowStamp) continue; // 이미 지난 배차
                        long arrPland = t.path("arrplandtime").asLong(t.path("arrPlandTime").asLong(0));
                        Departure d = new Departure(
                                dep.name(), arr.name(),
                                hhmm(depPland), hhmm(arrPland),
                                t.path("charge").asLong(0),
                                t.path("gradeNm").asText(t.path("gradenm").asText("일반")));
                        log.info("시외버스 배차 매칭: {} → {} · {} 출발 · {}원",
                                d.depTerminalNm(), d.arrTerminalNm(), d.depTime(), d.charge());
                        return d;
                    }
                }
            }
        } catch (Exception e) {
            handleFailure(e);
        }
        return null;
    }

    // ---------- API ----------

    /** 시내버스 도시코드 + 도시명 → 해당 도시의 시외버스 터미널 후보 */
    private List<Terminal> terminalsForCity(String cityCode, String cityName) throws Exception {
        String province = cityCode.length() >= 2 ? cityCode.substring(0, 2) : cityCode;
        List<Terminal> all = terminals(province);
        String nameKey = cityName == null ? "" : cityName
                .replaceAll("(특별자치시|특별자치도|특별시|광역시|시|군)$", "").trim();
        if (!nameKey.isEmpty()) {
            List<Terminal> filtered = all.stream()
                    .filter(t -> t.name().contains(nameKey)).toList();
            if (!filtered.isEmpty()) return filtered;
        }
        // 광역시(도시코드 2자리)는 광역 전체가 곧 그 도시라 전체 목록을 그대로 쓴다
        return cityCode.length() <= 2 ? all : List.of();
    }

    private List<Terminal> terminals(String provinceCode) throws Exception {
        List<Terminal> cached = terminalCache.get(provinceCode);
        if (cached != null) return cached;
        JsonNode items = call("/GetSuberbsBusTrminlList",
                "&cityCode=" + URLEncoder.encode(provinceCode, StandardCharsets.UTF_8) + "&numOfRows=300");
        List<Terminal> out = new ArrayList<>();
        for (JsonNode it : asArray(items)) {
            String id = it.path("terminalId").asText(it.path("terminalid").asText(""));
            String nm = it.path("terminalNm").asText(it.path("terminalnm").asText(""));
            if (!id.isEmpty()) out.add(new Terminal(id, nm));
        }
        // 대표 터미널(이름에 '시외'가 들어가거나 이름이 짧은 곳)을 앞으로
        out.sort((a, b) -> {
            int sa = (a.name().contains("시외") ? 0 : 1) * 100 + a.name().length();
            int sb = (b.name().contains("시외") ? 0 : 1) * 100 + b.name().length();
            return Integer.compare(sa, sb);
        });
        terminalCache.put(provinceCode, out);
        return out;
    }

    private List<JsonNode> timetable(String depId, String arrId, String date) throws Exception {
        String key = depId + ">" + arrId + "@" + date;
        List<JsonNode> cached = timetableCache.get(key);
        if (cached != null) return cached;
        JsonNode items = call("/GetStrtpntAlocFndSuberbsBusInfo",
                "&depTerminalId=" + URLEncoder.encode(depId, StandardCharsets.UTF_8)
                        + "&arrTerminalId=" + URLEncoder.encode(arrId, StandardCharsets.UTF_8)
                        + "&depPlandTime=" + date + "&numOfRows=50");
        List<JsonNode> out = asArray(items);
        out.sort((a, b) -> Long.compare(
                a.path("depplandtime").asLong(a.path("depPlandTime").asLong(0)),
                b.path("depplandtime").asLong(b.path("depPlandTime").asLong(0))));
        timetableCache.put(key, out);
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

    private void handleFailure(Exception e) {
        String msg = String.valueOf(e.getMessage());
        if (msg.contains("403")) {
            dead = true;
            log.warn("시외버스정보 API 인증 거부(403) — '국토교통부_(TAGO)_시외버스정보' 활용신청 필요. 이번 실행에서는 비활성화");
        } else {
            log.warn("시외버스정보 조회 실패: {}", msg);
        }
    }

    private static String hhmm(long plandTime) {
        String s = String.valueOf(plandTime);
        return s.length() >= 12 ? s.substring(8, 10) + ":" + s.substring(10, 12) : "";
    }

    private static List<JsonNode> asArray(JsonNode item) {
        List<JsonNode> out = new ArrayList<>();
        if (item == null || item.isMissingNode() || item.isNull()) return out;
        if (item.isArray()) item.forEach(out::add);
        else out.add(item);
        return out;
    }
}
