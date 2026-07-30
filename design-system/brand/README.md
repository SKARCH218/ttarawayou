# 트레빗 브랜드 자산

| 파일 | 용도 |
|---|---|
| `travit-logo-full.svg` | 심볼 + 워드마크 (기본 로고) — README, 발표 자료, 인트로 화면 |
| `travit-symbol.svg` | 심볼만 (토끼 + 방향 화살표) — 앱 아이콘, 파비콘, 헤더 |
| `travit-wordmark.svg` | 워드마크만 (Travit) — 가로로 좁은 공간 |

## 컬러

| 이름 | 값 | 용도 |
|---|---|---|
| Travit Green | `#03B083` | 로고 면, 주요 액션 |
| Travit Green Dark | `#00966F` | 로고 외곽선, 눌린 상태 |

## 사용 규칙

- 로고 주변에 심볼 높이의 **0.25배 이상 여백**을 둔다.
- 단색 로고이므로 색을 바꾸지 않는다. 어두운 배경에서는 흰색 단색 버전을 쓰되, `fill`만 `#FFFFFF`로 교체한다.
- 심볼의 토끼는 **오른쪽을 향한다**(진행 방향). 좌우 반전 금지.
- PNG가 필요하면 SVG에서 내보낸다 — 벡터가 원본이므로 별도 PNG를 저장해 두지 않는다.
  (예: 브라우저에서 SVG를 열고 원하는 크기로 캡처, 또는 Figma/Illustrator에서 export)

## 적용 위치

- 웹: `code/frontend/public/brand/` (인트로·헤더·파비콘)
- 앱: `code/app-kmp/composeApp/src/androidMain/res/` (런처 아이콘, 인트로)
