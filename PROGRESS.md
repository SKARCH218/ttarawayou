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

## 2026-09-16 — 팀 커밋 반영 + 회원가입 인증메일 미발송 원인 파악

### GitHub에 올라온 팀 커밋 pull (fast-forward, 5개)
- `fix: 웹(Compose wasm) 빌드 타겟 복원` — 8/19에 지웠던 wasmJs 타겟을 팀에서 다시 살림 (node2 자동배포가 여전히 웹 빌드를 먼저 시도해 3주간 배포 실패했다는 사유). `code/frontend`(Vanilla JS)와 관리자 페이지는 삭제된 채로 유지됨 — 완전히 되돌아간 건 아님
- LM Studio 관련 수정 3건: Bearer 인증 지원, Content-Type이 octet-stream이어도 파싱, max_tokens 상한(4000) 적용
- `feat(app): 실지도·현재위치 출발·대중교통 안내 개선 및 취향 질문 통일`

### 회원가입 인증메일이 안 오는 문제 — 원인 확정
node2(`playlabs@10.8.2.5`, 서비스명 `trevit`, user-level systemd)의
`journalctl --user -u trevit` 로그로 확인:
- 8/6~8/10: 정상 발송(`인증코드 메일 발송 완료`)
- **9/7부터 지금까지 전부 `Authentication failed`로 발송 실패** → 코드가 서버 로그에만 찍히고 사용자에게는 메일이 안 감 (MailService가 실패를 삼키고 로그 폴백하도록 설계돼 있어 에러가 겉으로 안 보임)
- 정황: 8/6에 유출된 Gmail 앱 비밀번호를 "폐기·재발급 진행 중"이라던 것이 9/7 즈음 실제 처리(구글 쪽 폐기)됐는데, 서버의 `~/trevit/trevit.env`(`EnvironmentFile=%h/trevit/trevit.env`)에는 새 비밀번호가 반영 안 된 것으로 보였음
- **추가로 확인된 사실**: 팀 공용 Gmail 계정(`travit.officiall@gmail.com`) 자체가 **봇 활동으로 오인되어 구글에 의해 계정이 삭제됨**. 즉 앱 비밀번호 재발급으로 해결 가능한 문제가 아니라, **발신 계정 자체가 없어진 상태**
- **영향**: 이메일 인증이 필요한 회원가입 전체가 막힘 (SMTP 미설정/실패 시 로그 폴백만 되고 실제 발송은 안 됨)

### 다음에 할 일 (추가)
- [ ] 회원가입용 이메일 발송 계정 새로 준비 (Gmail 재가입 또는 다른 SMTP 제공자로 교체 — 재발급이 아니라 계정 자체를 새로 만들어야 함)
- [ ] 새 계정으로 `~/trevit/trevit.env`의 `MAIL_HOST`/`MAIL_USERNAME`/`MAIL_PASSWORD`/`MAIL_FROM` 갱신 후 `systemctl --user restart trevit`
- [ ] (권장) 같은 사유로 반복될 수 있으니 Gmail보다 SendGrid·AWS SES 등 트랜잭션 메일 전용 서비스로 교체 검토

