# W3 · MVP·아키텍처·AI 윤리 (PLAYLABS · 트레빗)

## 1. MVP 범위표

**핵심 행동 1문장**
예산과 취향을 입력하면 AI가 수도권 여행 일정을 짜고, 사용자는 미스터리 지도를 따라가다 도착하는 순간에만 장소를 확인한다.

**이번에 만든 것 (완료 — 안정화만 진행)**
- 지역·예산·기간 선택 + 취향 질문 8개
- AI 플랜 생성 (LM Studio, Google Gemma 3 26B)
- 미스터리 지도 (경로 순차 공개 → 도착 시 정보 공개)
- 시뮬레이션 모드 (1×/3×/10×, GPS 없이 여정 체험)

**이번엔 안 만드는 것**
- 실제 결제·예약 연동 (현재는 토큰 선결제 시뮬레이션)
- 경주 등 서비스 지역 확장 (현재 수도권 한정)
- 신규 부가기능 — 남은 5주는 기존 핵심 행동의 완성도에만 집중

## 2. 아키텍처 1장

```
[화면]
 Compose Multiplatform 앱 / Vanilla JS + Leaflet 웹
        │ HTTPS
        ▼
[서버] Kotlin + Spring Boot 3.5 (REST API, JPA)
        │                              │
        ▼                              ▼
[DB] PostgreSQL(운영)/H2(로컬)   [외부 API] TMAP POI·대중교통,
        │                        국토교통부 TAGO, ODsay, OSRM
        ▼
[AI] LM Studio (Google Gemma 3 26B, OpenAI 호환 API)
     실패·타임아웃 시 → 자체 휴리스틱(평점·근접도·예산 스코어링 +
     최근접 이웃 동선 최적화)으로 자동 폴백
```

## 3. AI·오픈소스 사용내역

- **LLM**: Google Gemma 3 26B — LM Studio로 로컬 구동, OpenAI 호환 API
- **개발 보조 AI**: Claude (Anthropic) — 코드 작성 보조
- **외부 API**: TMAP(SK open API), 국토교통부 TAGO, ODsay, OSRM(공개 라우팅 서버)
- **주요 오픈소스**: Spring Boot(Apache-2.0), Kotlin(Apache-2.0), Leaflet(BSD-2-Clause)
- **개인정보 처리**: 실사용자 이름·연락처를 프롬프트에 넣지 않음. 테스트는 가명 데이터로 진행.
- AI가 만든 코드와 팀원이 직접 작성·수정한 부분은 커밋 단위로 구분되어 GitHub 히스토리에서 확인 가능.

## 4. 보안 점검

코드(`.kt`/`.js`/`.yml`, 실행 스크립트) 전수 검색 결과 하드코딩된 API 키·비밀번호는 없음. 모든 민감값은 환경변수로 분리되어 있고 `.env`는 `.gitignore` 처리됨.

git 히스토리 점검 중 8/6 팀원 실수로 `code/.env`(SMTP 메일 계정 자격증명 포함)가 커밋된 이력을 확인했음. 같은 날 24분 만에 코드에서 삭제했으나, 해당 커밋 자체는 공개 저장소 히스토리에 남아 있어 그 시점부터 자격증명이 노출된 상태였음. TMAP·ODsay·data.go.kr·Google Client ID 등 다른 외부 API 키는 같은 커밋에서도 값이 비어 있어 유출되지 않음. 노출된 SMTP 자격증명은 무효화(비밀번호 재발급)가 필요한 사안으로 확인했고, 팀 내에서 조치를 진행 중.
