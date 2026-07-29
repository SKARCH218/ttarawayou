package com.mysterytrip.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysterytrip.entity.Place;
import com.mysterytrip.entity.Place.PlaceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.*;

/**
 * LM Studio(OpenAI 호환 API)의 로컬 LLM에게 여행 플랜을 짜게 한다.
 * 장소 카탈로그와 예산 규칙을 프롬프트로 주고, 숙소 + 일자별 방문 순서를 JSON으로 받는다.
 * 실패(서버 꺼짐·타임아웃·이상한 응답·예산 초과)하면 null을 반환해
 * PlanService가 휴리스틱 알고리즘으로 폴백하게 한다.
 */
@Service
public class AiPlanService {

    private static final Logger log = LoggerFactory.getLogger(AiPlanService.class);

    /** AI가 고른 숙소와 일자별(방문 순서대로) 장소 목록 */
    public record AiSelection(Place lodging, List<List<Place>> days) {}

    private final RestClient http;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String baseUrl;
    private final String model;
    private final boolean enabled;

    public AiPlanService(@Value("${lmstudio.base-url}") String baseUrl,
                         @Value("${lmstudio.model}") String model,
                         @Value("${lmstudio.enabled}") boolean enabled) {
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.model = model;
        this.enabled = enabled;
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(3000);
        f.setReadTimeout(300_000); // 로컬 추론 모델은 느릴 수 있어 최대 5분 대기
        this.http = RestClient.builder().requestFactory(f).build();
    }

    public AiSelection plan(long budget, int days, int people, int nights, List<Place> places) {
        if (!enabled) return null;
        try {
            String content = chat(buildPrompt(budget, days, people, nights, places));
            AiSelection sel = parse(content, days, places, nights > 0);
            if (sel == null) {
                log.warn("AI 응답 파싱/검증 실패 → 휴리스틱 폴백");
                return null;
            }
            // 교통비 몫(예산의 10%)을 남겨야 하므로 장소 비용은 90%까지만 허용.
            // 예산을 최대한 쓰는 것이 목표이므로 70% 미만으로 아끼면 휴리스틱이 낫다.
            long total = totalCost(sel, people, nights);
            long placeCap = budget * 90 / 100;
            long placeFloor = budget * 70 / 100;
            if (total > placeCap || total < placeFloor) {
                log.warn("AI 플랜 장소 비용 {}이 허용 범위({}~{}) 밖 → 휴리스틱 폴백",
                        total, placeFloor, placeCap);
                return null;
            }
            log.info("AI 플랜 채택: 숙소 '{}', 일자별 장소 수 {}",
                    sel.lodging() == null ? "(당일치기)" : sel.lodging().getName(),
                    sel.days().stream().map(List::size).toList());
            return sel;
        } catch (Exception e) {
            log.warn("LM Studio 호출 실패({}) → 휴리스틱 폴백", e.getMessage());
            return null;
        }
    }

    // ---------- 프롬프트 ----------

    private String buildPrompt(long budget, int days, int people, int nights, List<Place> places) {
        boolean dayTrip = nights <= 0;
        StringBuilder sb = new StringBuilder();
        sb.append("경주 여행 플랜을 짜라.\n")
          .append("총예산 ").append(budget).append("원, ").append(days).append("일, ")
          .append(people).append("명, ")
          .append(dayTrip ? "당일치기(숙박 없음)." : "숙박 " + nights + "박.").append("\n\n")
          .append("규칙:\n");
        if (dayTrip) {
            sb.append("- 당일치기라 숙박이 없다. lodgingId는 반드시 0으로 하라\n")
              .append("- 예산 배분 기준: 관광 55% / 식비 35% / 교통 10%\n");
        } else {
            sb.append("- 예산 배분 기준: 숙박 40% / 관광 30% / 식비 20% / 교통 10%\n")
              .append("- 숙소 1곳 선택: (1박요금 x ").append(nights).append("박)이 숙박 예산 이내. 평점 높을수록 좋다\n");
        }
        sb
          .append("- 매일 관광지 2~3곳 + 식당 3곳(아침/점심/저녁). 입장료와 식비는 ")
          .append(people).append("명 몫으로 계산된다\n")
          .append("- 같은 날의 장소들은 서로 가까운 곳으로 묶고, stopIds는 이동 동선이 자연스러운 방문 순서로 나열하라\n")
          .append("- 식당과 관광지를 번갈아 배치하라 (아침식사로 시작하면 자연스럽다). ")
          .append("식당 두 곳을 연속으로 배치하는 것은 절대 금지\n")
          .append("- 같은 장소를 두 번 넣지 마라\n")
          .append("- 중요: 예산을 최대한 다 써라. 장소 비용 합계(숙박+입장료x인원+식비x인원)가 ")
          .append("총예산의 75% 이상 88% 이하가 되도록 더 비싸고 평점 좋은 숙소·식당·관광지를 우선 선택하라. ")
          .append("남는 예산을 최소화하라. 나머지는 교통비로 자동 사용되므로 88%는 절대 초과하지 마라\n\n")
          .append("반드시 아래 형식의 JSON 하나만 출력하라. 설명·주석 금지.\n")
          .append("{\"lodgingId\": 숫자, \"days\": [{\"stopIds\": [숫자, ...]}");
        sb.append(", ...]}  (days 배열 길이는 정확히 ").append(days).append(")\n\n");
        sb.append("장소 목록 (id|종류|이름|1인가격원|평점|위도|경도):\n");
        for (Place p : places) {
            sb.append(p.getId()).append('|')
              .append(switch (p.getType()) { case LODGING -> "숙박"; case RESTAURANT -> "식당"; case ATTRACTION -> "관광지"; }).append('|')
              .append(p.getName()).append('|')
              .append(p.getPrice()).append('|')
              .append(p.getRating()).append('|')
              .append(String.format(Locale.US, "%.4f", p.getLatitude())).append('|')
              .append(String.format(Locale.US, "%.4f", p.getLongitude())).append('\n');
        }
        return sb.toString();
    }

    // ---------- LM Studio 호출 (OpenAI 호환 chat completions) ----------

    private String chat(String userPrompt) throws Exception {
        Map<String, Object> body = Map.of(
                "model", model,
                "temperature", 0.3,
                // 추론(reasoning) 모델은 '생각'에도 토큰을 쓰므로 제한하지 않는다 (LM Studio: -1 = 무제한)
                "max_tokens", -1,
                "messages", List.of(
                        Map.of("role", "system", "content",
                                "너는 대한민국 경주 여행 플래너다. 요청받은 JSON 형식으로만 답한다."),
                        Map.of("role", "user", "content", userPrompt)
                )
        );
        String res = http.post()
                .uri(URI.create(baseUrl + "/chat/completions"))
                .contentType(MediaType.APPLICATION_JSON)
                .body(mapper.writeValueAsString(body))
                .retrieve()
                .body(String.class);
        JsonNode root = mapper.readTree(res);
        return root.path("choices").path(0).path("message").path("content").asText("");
    }

    // ---------- 응답 파싱 · 검증 ----------

    private AiSelection parse(String content, int days, List<Place> places,
                              boolean requireLodging) throws Exception {
        // 코드펜스·잡담이 섞여도 첫 '{'부터 마지막 '}'까지만 취한다
        int s = content.indexOf('{');
        int e = content.lastIndexOf('}');
        if (s < 0 || e <= s) return null;
        JsonNode root = mapper.readTree(content.substring(s, e + 1));

        Map<Long, Place> byId = new HashMap<>();
        places.forEach(p -> byId.put(p.getId(), p));

        Place lodging = byId.get(root.path("lodgingId").asLong(-1));
        if (requireLodging && (lodging == null || lodging.getType() != PlaceType.LODGING)) return null;
        if (lodging != null && lodging.getType() != PlaceType.LODGING) lodging = null;

        JsonNode dayArr = root.path("days");
        if (!dayArr.isArray() || dayArr.size() != days) return null;

        Set<Long> used = new HashSet<>();
        List<List<Place>> perDay = new ArrayList<>();
        for (JsonNode d : dayArr) {
            List<Place> dayPlaces = new ArrayList<>();
            for (JsonNode idNode : d.path("stopIds")) {
                Place p = byId.get(idNode.asLong(-1));
                // 없는 id, 숙박 시설, 중복은 조용히 건너뛴다
                if (p == null || p.getType() == PlaceType.LODGING || !used.add(p.getId())) continue;
                dayPlaces.add(p);
                if (dayPlaces.size() >= 8) break;
            }
            if (dayPlaces.isEmpty()) return null;
            perDay.add(dayPlaces);
        }
        return new AiSelection(lodging, perDay);
    }

    private long totalCost(AiSelection sel, int people, int nights) {
        long total = sel.lodging() == null ? 0 : (long) sel.lodging().getPrice() * nights;
        for (List<Place> day : sel.days()) {
            for (Place p : day) total += (long) p.getPrice() * people;
        }
        return total;
    }
}
