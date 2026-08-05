# 🔮 미스터리 트립 — 예산 기반 AI 여행 플래너 (경주)

예산만 입력하면 AI가 경주 여행(숙소·식당·관광지·동선)을 자동으로 설계합니다.
단, **어디로 가는지는 비밀** — 지도가 보여주는 길을 따라가다 보면 목적지에 도착하고,
도착한 순간에만 장소 이름이 공개됩니다.

## 구성

| 구분 | 기술 | 포트 |
|---|---|---|
| 프론트엔드 (웹) | Compose Multiplatform(wasm) — 안드로이드 앱과 같은 화면 코드 | 3000 |
| 앱 (Android) | Jetpack Compose (`app-kmp/composeApp`, 웹과 코드 공유) | — |
| 백엔드 API | Spring Boot 3 + JPA + H2 인메모리 DB | 8080 |
| 지도 | Leaflet + OpenStreetMap 타일 (키 불필요) | — |
| 도보 경로 | OSRM 공개 라우팅 서버 (키 불필요) | — |
| 대중교통 | ODsay API (키 없으면 거리 기반 추정 자동 폴백) | — |

서버 시작 시 경주 **숙박업소 50곳 + 식당 50곳 + 관광지 50곳** 시드 데이터가 자동 삽입됩니다
(황리단길·교촌·보문단지·불국사·감포·양남 등 실제 지역대 좌표 사용).

## 실행 방법 (Windows) — 새 PC에서도 키만 넣으면 끝

**준비물: Java 21+ 와 Node.js만 설치** (Maven 불필요 — Maven Wrapper가 자동 빌드)

1. `run-backend.bat`을 메모장으로 열어 **`TMAP_APP_KEY=`에 본인 SK open API 앱키 입력** (필수).
   ODsay·data.go.kr 키는 선택 — 없으면 해당 부가기능만 자동 폴백된다.
   지도 웹페이지는 이 키를 백엔드(`/api/config`)에서 받아 쓰므로 **다른 파일은 수정할 필요 없음**.
2. **`run-backend.bat`** 더블클릭 → 첫 실행이면 자동으로 빌드(몇 분) 후 8080 기동
3. **`run-app-web.bat`** 더블클릭 → Compose 웹 빌드 + 서빙 (3000)
4. 브라우저에서 `http://localhost:3000`

기타 배치 파일:
- **`run.bat`** → 백엔드 + 웹을 탭 2개로 한 번에 실행
- **`restart-servers.bat`** → 전부 끄고 재시작 (코드 수정 후 반영용)
- **`stop-servers.bat`** → 전부 종료
- **`run-app-web-dev.bat`** → 웹 개발 모드: 앱 화면 코드를 저장하면
  자동으로 다시 빌드되고 브라우저가 새로고침됨 (디자인 작업용)

TMAP 앱키 발급: [openapi.sk.com](https://openapi.sk.com) 가입 → 앱 생성 →
상품 이용신청(TMAP, TMAP 대중교통) → 앱키 복사.

수동 빌드가 필요하면 (프로젝트 폴더에서): `cd backend && .\mvnw.cmd -DskipTests package`

### 웹 서버 수동 실행 (3000)

```bash
cd app-kmp && ./gradlew :composeApp:wasmJsBrowserDistribution
npx --yes http-server app-kmp/composeApp/build/dist/wasmJs/productionExecutable -p 3000 -c-1
```

브라우저에서 `http://localhost:3000` 접속.
휴대폰에서 접속하려면 같은 Wi-Fi에서 `http://<PC의 IP>:3000` —
API 주소는 접속 호스트 기준으로 자동 연결됩니다.

## API

- `GET /api/places?type=LODGING|RESTAURANT|ATTRACTION` — 장소 목록
- `POST /api/plan` — 플랜 생성
  ```json
  { "budget": 600000, "days": 2, "people": 2,
    "startLatitude": 35.843, "startLongitude": 129.211 }
  ```
  출발 좌표가 있으면 1일차는 현재 위치에서 출발, 없으면(위치 권한 거부) 숙소에서 출발.

## 플랜 로직

1. 예산 배분: **숙박 40% / 관광 30% / 식비 20% / 교통 10%**
2. 숙박 예산 내 최고 평점 숙소 선택
3. 관광 예산 내 하루 2~3곳 — 평점·숙소 근접도·입장료 점수화
4. 식비 예산 내 하루 3끼 — 같은 방식
5. 하루 일정(식당+관광지)을 최근접 이웃 휴리스틱으로 동선 최적화 → 숙소 복귀
6. 1.6km 초과 구간은 대중교통(ODsay), 이하는 도보(OSRM 실도로 경로).
   교통 예산 10%를 초과하면 도보로 자동 대체

## 미스터리 지도 규칙

- 확대/축소 완전 잠금 (`minZoom = maxZoom`, 휠·더블클릭·핀치·드래그 전부 차단)
- 줌 레벨 19 고정 (OSM 타일 원본 최대 줌이라 선명함, 화면 가로 약 100m).
  `map.js`의 `ZOOM` 상수로 조절 — 19 초과 시 `maxNativeZoom: 19` 덕에 확대 렌더링됨
- GPS(`watchPosition`)가 지도를 움직이고, 화면은 항상 내 위치 중심
- 목적지 마커 없음 — 경로는 **현재 위치 앞 약 130m만** 공개
- 경로에서 40m 이상 벗어나면 "길에서 벗어났어요" 경고
- 목적지 반경 **20m 도착 순간에만** 장소 이름·평점·비용 공개
- **GPS 전용** — 여정 시작 지점은 지도를 연 순간의 현재 위치.
  계획된 출발점과 30m 이상 떨어져 있으면 현재 위치 → 첫 목적지 경로를 자동 재계산
- **이탈 자동 재탐색** — 길에서 40m 이상 벗어난 상태가 이어지면(연속 2회 감지)
  현재 위치 → 다음 목적지 경로를 새로 계산해 교체 (도보 구간만, 재탐색 간격 8초 제한)
- 참고: `http://IP:3000` 같은 비보안(HTTP) 접속에서는 브라우저가 GPS를 차단하므로
  localhost 접속 또는 크롬 플래그(`unsafely-treat-insecure-origin-as-secure`) 설정이 필요
