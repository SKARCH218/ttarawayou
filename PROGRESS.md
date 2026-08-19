# 진행 상황 (Progress Log)

> 세션 간 컨텍스트를 이어가기 위한 진행 기록. 새 대화를 시작할 때 이 파일을 참고시키면 됩니다.

## 저장소 정보
- GitHub: https://github.com/SKARCH218/ttarawayou.git
- 작업 브랜치: `OGQ` (기본 브랜치, origin/HEAD가 이 브랜치를 가리킴)
- 로컬 경로: `C:\Users\54463\OneDrive\Desktop\ttarawayou-OGQ\ttarawayou-OGQ`
- push 권한: 있음 (계정 `beuljag`)

## 2026-08-19
- 기존에 폴더가 GitHub 이력 없이 다운로드된 상태(.git 없음)였음을 확인
- 팀원 저장소를 새로 클론하여 기존 폴더를 교체 (`OGQ` 브랜치 체크아웃)
  - 교체 전 원본 폴더는 `ttarawayou-OGQ_backup_nogit`으로 백업 보관 (내용은 클론과 동일, 줄바꿈만 차이)
- `C:\Users\54463\.git` (홈 디렉터리에 실수로 생성된 빈 저장소) 삭제
- `CLAUDE.md` 없음 확인 — 아직 생성 안 함

- 웹 관련 제거 (Android 앱만 남김):
  - `code/frontend/` (Vanilla JS 웹 데모) 삭제
  - `code/app-kmp/composeApp/src/wasmJsMain/`, `kotlin-js-store/` (Compose 웹 빌드) 삭제, `composeApp`·`shared` build.gradle.kts에서 wasmJs 타겟 제거
  - `code/backend/src/main/resources/static/admin/` + `AdminPageController.kt` (관리자 웹페이지) 삭제
  - `run-app-web.*`, `run-app-web-dev.bat`, `run-debug.*`, `run-frontend.*`, `run.bat`, `run.sh` 삭제
  - `restart-servers.*`, `stop-servers.*` → 포트 3000/3030 제거, 8080(백엔드)만 남김
  - `backend/build.gradle.kts`, `backend/Dockerfile`의 wasm 정적 리소스 복사 단계 제거
  - **손대지 않은 것**: 최상위 `README.md`/`code/README.md` 문서 내용(여전히 웹 데모 언급)

## 2026-08-19 (계속) — 도메인·배포 인프라 정리
- 팀원이 도메인 `travit.p-e.kr` 확보. `deploy/nginx-trevit.conf`의 `server_name`을 채우고, 죽은 프론트 upstream/분기 제거 (이제 전부 백엔드로 프록시). HTTPS는 서버에서 `sudo certbot --nginx -d travit.p-e.kr` 실행하면 됨 (아직 미실행)
- 웹 삭제로 깨져 있던 운영 배포 스크립트 정리:
  - `deploy/node2-deploy.sh` — 더 이상 존재하지 않는 wasm 웹 빌드 단계 제거
  - `deploy/trevit-frontend.service` 삭제, `deploy/setup-node2.sh`·`deploy/build-bundle.sh`에서 프론트 관련 부분(포트 3000, 서비스 등록, 파일 복사) 제거
  - `deploy/.env.production.example` — `ADMIN_TOKEN` 주석을 "관리자 페이지" → "관리자 API"로 수정 (토큰 자체는 `AdminController.kt` API가 여전히 사용하므로 유지)
  - **API 키(TMAP/DATA_GO_KR/ODSAY/LMSTUDIO/ADMIN_TOKEN 등)는 전혀 건드리지 않음**

## 다음에 할 일
- (필요시) 운영 서버에서 `sudo certbot --nginx -d travit.p-e.kr` 실행해 HTTPS 적용
- 최상위 `README.md`/`code/README.md`에 남은 웹 데모 관련 서술 정리 여부 결정

