# 🔮 미스터리 트립 — 예산 기반 AI 여행 플래너 (경주)

예산만 입력하면 AI가 경주 여행(숙소·식당·관광지·동선)을 자동으로 설계합니다.
단, **어디로 가는지는 비밀** — 지도가 보여주는 길을 따라가다 보면 목적지에 도착하고,
도착한 순간에만 장소 이름이 공개됩니다.

## 구성

| 구분 | 기술 | 포트 |
|---|---|---|
| 프론트엔드 | HTML / CSS / JS (정적 파일) + Express 서빙 | 3000 |
| 백엔드 API | Spring Boot 3 + JPA + H2 인메모리 DB | 8080 |
| 지도 | Leaflet + OpenStreetMap 타일 (키 불필요) | — |
| 도보 경로 | OSRM 공개 라우팅 서버 (키 불필요) | — |
| 대중교통 | ODsay API (키 없으면 거리 기반 추정 자동 폴백) | — |

서버 시작 시 경주 **숙박업소 50곳 + 식당 50곳 + 관광지 50곳** 시드 데이터가 자동 삽입됩니다
(황리단길·교촌·보문단지·불국사·감포·양남 등 실제 지역대 좌표 사용).

## 실행 방법 (Windows)

가장 쉬운 방법 — 프로젝트 폴더의 배치 파일을 더블클릭:

1. **`run-backend.bat`** → Spring API 서버 (8080, ODsay 키 자동 설정)
2. **`run-frontend.bat`** → 웹 서버 (3000, 최초 실행 시 npm install 자동)
3. **`restart-servers.bat`** → 실행 중인 서버 전부 끄고 셋 다 새로 시작 (코드 수정 후 반영용)
4. **`stop-servers.bat`** → 실행 중인 서버 전부 종료
5. **`run-debug.bat`** → 디버깅 웹 (3030) — 폰 프레임 오른쪽에 전체 여정 패널 +
   경로를 따라 초당 10m 자동 이동. 일반 앱(3000)에는 디버깅 기능이 전혀 없다

> ⚠️ 명령을 직접 칠 때는 반드시 **프로젝트 폴더 안에서** 실행해야 합니다.
> `ODSAY_API_KEY=키 java -jar ...` 같은 한 줄 문법은 리눅스/맥 전용이라 Windows에서는 동작하지 않습니다.

수동 실행 (PowerShell, 프로젝트 폴더에서):

```bash
$env:ODSAY_API_KEY='YOUR_ODSAY_API_KEY'; java -jar backend/target/mystery-trip-api-0.0.1-SNAPSHOT.jar
```

수동 실행 (cmd, 프로젝트 폴더에서):

```bash
set ODSAY_API_KEY=YOUR_ODSAY_API_KEY&& java -jar backend\target\mystery-trip-api-0.0.1-SNAPSHOT.jar
```

키를 지정하지 않으면 거리 기반 추정으로 자동 폴백합니다.
다시 빌드하려면 (Maven 필요): `mvn -f backend/pom.xml -DskipTests package`

### 웹 서버 수동 실행 (3000)

```bash
cd frontend && npm install && npm start
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
