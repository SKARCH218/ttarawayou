// build_poster.js — 따라와유 본선 전시용 포스터 (A3 세로)
// 마감: 2026.5.15(금) 자정 / 파일명: 본선 포스터_아산스마트팩토리마이스터고등학교_김태훈
// 톤: 객관적 설명체 (전시장에서 발표자 없이 스스로 설명) — 대화체 금지

const pptxgen = require("pptxgenjs");

const pres = new pptxgen();
pres.defineLayout({ name: "A3_PORTRAIT", width: 11.69, height: 16.54 });
pres.layout = "A3_PORTRAIT";
pres.author = "Kims & Lee";
pres.company = "아산스마트팩토리마이스터고등학교";
pres.title = "따라와유 본선 전시 포스터";

// === 팔레트 (Sage Travel) ===
const SAGE       = "5C7A6E";
const SAGE_DARK  = "2D3F37";
const SAGE_LIGHT = "8FA89A";
const CREAM      = "F5E6D3";
const CORAL      = "E07856";
const CORAL_DARK = "C95A3F";
const WHITE      = "FFFFFF";
const NAVY_TEXT  = "1F2937";
const GREY       = "6B7280";
const LIGHT_GREY = "9CA3AF";
const BG_OFFWHITE = "FAF7F2";

const s = pres.addSlide();
s.background = { color: BG_OFFWHITE };

// 좌측 코랄 바
s.addShape(pres.shapes.RECTANGLE, {
  x: 0, y: 0, w: 0.25, h: 16.54,
  fill: { color: CORAL }, line: { color: CORAL, width: 0 }
});

// ============================================================
// [1] 헤더 (0 ~ 3.0")
// ============================================================
s.addShape(pres.shapes.RECTANGLE, {
  x: 0, y: 0, w: 11.69, h: 3.0,
  fill: { color: SAGE_DARK }, line: { color: SAGE_DARK, width: 0 }
});
// 좌측 코랄 바 (헤더 위)
s.addShape(pres.shapes.RECTANGLE, {
  x: 0, y: 0, w: 0.25, h: 16.54,
  fill: { color: CORAL }, line: { color: CORAL, width: 0 }
});

s.addText("TEAM  KIMS  &  LEE", {
  x: 0.6, y: 0.4, w: 10.5, h: 0.3,
  fontSize: 12, bold: true, color: CORAL, charSpacing: 5, fontFace: "Arial", margin: 0
});

// 따라와유 대형 로고
s.addText("따라와유", {
  x: 0.6, y: 0.78, w: 10.5, h: 1.3,
  fontSize: 88, bold: true, color: CORAL, fontFace: "Arial", margin: 0, charSpacing: -2
});

s.addText("Tarayou — AI 완전자동 서프라이즈 충남 여행 앱", {
  x: 0.6, y: 2.1, w: 10.5, h: 0.4,
  fontSize: 17, bold: true, color: WHITE, fontFace: "Arial", margin: 0
});
s.addText("End-to-End Automated Travel Service Powered by AI", {
  x: 0.6, y: 2.55, w: 10.5, h: 0.3,
  fontSize: 11, italic: true, color: CREAM, fontFace: "Arial", margin: 0
});

// ============================================================
// [2] 연구 배경 (3.2 ~ 4.7")
// ============================================================
s.addText("01   BACKGROUND", {
  x: 0.6, y: 3.2, w: 10.5, h: 0.28,
  fontSize: 11, bold: true, color: CORAL, charSpacing: 3, fontFace: "Arial", margin: 0
});
s.addText("연구 배경", {
  x: 0.6, y: 3.5, w: 10.5, h: 0.45,
  fontSize: 22, bold: true, color: SAGE_DARK, fontFace: "Arial", margin: 0
});
s.addText("한국 1인 가구는 전체의 36.1%(804만)에 달하지만, 1인 특화 여행 패키지는 부족함.\n충남은 백제 유산·해안·온천 등 풍부한 자원에도 불구하고 수도권 대비 인지도와 접근성에서 불리하며, 1인 여행객 90%가 수도권·부산·제주를 선택함. 또한 여행 중에도 SNS·업무 메시지 노출로 진정한 회복이 어려운 디지털 피로 문제가 심각함.", {
  x: 0.6, y: 4.0, w: 10.5, h: 0.7,
  fontSize: 11, color: NAVY_TEXT, fontFace: "Arial", margin: 0
});

// ============================================================
// [3] 핵심 아이디어 (4.85 ~ 6.35") — 오렌지 강조
// ============================================================
s.addShape(pres.shapes.RECTANGLE, {
  x: 0.6, y: 4.85, w: 10.5, h: 1.45,
  fill: { color: CORAL }, line: { color: CORAL, width: 0 }
});
s.addText("02   THE IDEA", {
  x: 0.6, y: 4.95, w: 10.5, h: 0.28,
  fontSize: 11, bold: true, color: SAGE_DARK, charSpacing: 3, align: "center", fontFace: "Arial", margin: 0
});
s.addText("예산만 입력하면, AI가 충남 여행을 대신 결정하고 실행함", {
  x: 0.6, y: 5.27, w: 10.5, h: 0.55,
  fontSize: 20, bold: true, color: WHITE, align: "center", fontFace: "Arial", margin: 0
});
s.addText("사용자는 결정 부담 없이 AI가 안내하는 대로 따라가며, 여행 중 다른 앱은 차단되어 진짜 휴식을 경험함.", {
  x: 0.6, y: 5.85, w: 10.5, h: 0.35,
  fontSize: 12, italic: true, color: WHITE, align: "center", fontFace: "Arial", margin: 0
});

// ============================================================
// [4] 5단계 흐름 (6.55 ~ 10.4") — 아이폰 목업 5개
// ============================================================
s.addText("03   USER FLOW", {
  x: 0.6, y: 6.55, w: 10.5, h: 0.28,
  fontSize: 11, bold: true, color: CORAL, charSpacing: 3, fontFace: "Arial", margin: 0
});
s.addText("이렇게 동작합니다 — 5단계", {
  x: 0.6, y: 6.85, w: 10.5, h: 0.45,
  fontSize: 22, bold: true, color: SAGE_DARK, fontFace: "Arial", margin: 0
});

// 5개 아이폰 목업 헬퍼
function drawPhone(x, y, w, h, drawScreen) {
  s.addShape(pres.shapes.ROUNDED_RECTANGLE, {
    x: x, y: y, w: w, h: h,
    fill: { color: SAGE_DARK }, line: { color: SAGE_DARK, width: 0 },
    rectRadius: 0.18
  });
  const padding = 0.07;
  const sx = x + padding, sy = y + padding;
  const sw = w - padding * 2, sh = h - padding * 2;
  s.addShape(pres.shapes.ROUNDED_RECTANGLE, {
    x: sx, y: sy, w: sw, h: sh,
    fill: { color: WHITE }, line: { color: WHITE, width: 0 },
    rectRadius: 0.13
  });
  s.addShape(pres.shapes.ROUNDED_RECTANGLE, {
    x: x + w / 2 - 0.27, y: sy + 0.04, w: 0.54, h: 0.1,
    fill: { color: SAGE_DARK }, line: { color: SAGE_DARK, width: 0 },
    rectRadius: 0.05
  });
  drawScreen(sx, sy + 0.2, sw, sh - 0.3);
  s.addShape(pres.shapes.ROUNDED_RECTANGLE, {
    x: x + w / 2 - 0.32, y: y + h - 0.13, w: 0.64, h: 0.04,
    fill: { color: SAGE_DARK }, line: { color: SAGE_DARK, width: 0 },
    rectRadius: 0.02
  });
}

const phStartY = 7.45;
const phW = 2.05, phH = 2.6;
const phStartX = 0.6;
const phGap = 0.16;

// PHONE 1 — 예산 입력
drawPhone(phStartX, phStartY, phW, phH, (sx, sy, sw) => {
  s.addText("따라와유", { x: sx, y: sy, w: sw, h: 0.22, fontSize: 9, bold: true, color: CORAL, align: "center", fontFace: "Arial", margin: 0 });
  s.addText("예산을 입력하세요", { x: sx, y: sy + 0.28, w: sw, h: 0.18, fontSize: 7, color: GREY, align: "center", fontFace: "Arial", margin: 0 });
  s.addText("₩300,000", { x: sx, y: sy + 0.55, w: sw, h: 0.45, fontSize: 19, bold: true, color: SAGE_DARK, align: "center", fontFace: "Arial", margin: 0 });
  s.addShape(pres.shapes.LINE, { x: sx + 0.2, y: sy + 1.05, w: sw - 0.4, h: 0, line: { color: SAGE_LIGHT, width: 1 } });
  s.addText("📅 1박 2일", { x: sx, y: sy + 1.15, w: sw, h: 0.18, fontSize: 7, color: NAVY_TEXT, align: "center", fontFace: "Arial", margin: 0 });
  s.addText("👤 혼자", { x: sx, y: sy + 1.35, w: sw, h: 0.18, fontSize: 7, color: NAVY_TEXT, align: "center", fontFace: "Arial", margin: 0 });
  s.addShape(pres.shapes.ROUNDED_RECTANGLE, { x: sx + 0.2, y: sy + 1.65, w: sw - 0.4, h: 0.3, fill: { color: CORAL }, line: { color: CORAL, width: 0 }, rectRadius: 0.06 });
  s.addText("선결제", { x: sx + 0.2, y: sy + 1.65, w: sw - 0.4, h: 0.3, fontSize: 9, bold: true, color: WHITE, align: "center", valign: "middle", fontFace: "Arial", margin: 0 });
});

// PHONE 2 — AI 처리
drawPhone(phStartX + (phW + phGap), phStartY, phW, phH, (sx, sy, sw) => {
  s.addText("AI", { x: sx, y: sy, w: sw, h: 0.22, fontSize: 9, bold: true, color: SAGE, align: "center", fontFace: "Arial", margin: 0 });
  s.addShape(pres.shapes.OVAL, { x: sx + sw / 2 - 0.32, y: sy + 0.45, w: 0.64, h: 0.64, fill: { color: SAGE }, line: { color: SAGE, width: 0 } });
  s.addText("⏳", { x: sx + sw / 2 - 0.32, y: sy + 0.45, w: 0.64, h: 0.64, fontSize: 22, color: WHITE, align: "center", valign: "middle", fontFace: "Arial", margin: 0 });
  s.addText("AI가 충남 여행을\n준비하고 있어요", { x: sx, y: sy + 1.18, w: sw, h: 0.45, fontSize: 8, bold: true, color: SAGE_DARK, align: "center", fontFace: "Arial", margin: 0 });
  const items = ["✓ 코스 분석", "✓ 식당 예약", "○ 숙소 확정"];
  items.forEach((it, i) => {
    s.addText(it, { x: sx + 0.2, y: sy + 1.7 + i * 0.16, w: sw - 0.4, h: 0.14, fontSize: 7, color: i < 2 ? SAGE : LIGHT_GREY, fontFace: "Arial", margin: 0 });
  });
});

// PHONE 3 — 다음 행동
drawPhone(phStartX + (phW + phGap) * 2, phStartY, phW, phH, (sx, sy, sw) => {
  s.addText("NEXT STEP", { x: sx, y: sy, w: sw, h: 0.22, fontSize: 8, bold: true, color: CORAL, align: "center", charSpacing: 1, fontFace: "Arial", margin: 0 });
  s.addText("지금 바로", { x: sx, y: sy + 0.32, w: sw, h: 0.18, fontSize: 8, color: GREY, align: "center", fontFace: "Arial", margin: 0 });
  s.addText("고속버스\n터미널로", { x: sx, y: sy + 0.55, w: sw, h: 0.6, fontSize: 14, bold: true, color: SAGE_DARK, align: "center", fontFace: "Arial", margin: 0 });
  s.addText("→", { x: sx, y: sy + 1.2, w: sw, h: 0.4, fontSize: 32, bold: true, color: CORAL, align: "center", fontFace: "Arial", margin: 0 });
  s.addShape(pres.shapes.RECTANGLE, { x: sx + 0.2, y: sy + 1.7, w: sw - 0.4, h: 0.35, fill: { color: CREAM }, line: { color: CREAM, width: 0 } });
  s.addText("🎫 14:30 출발", { x: sx + 0.2, y: sy + 1.72, w: sw - 0.4, h: 0.16, fontSize: 7, bold: true, color: SAGE_DARK, align: "center", valign: "middle", fontFace: "Arial", margin: 0 });
  s.addText("목적지: ???", { x: sx + 0.2, y: sy + 1.88, w: sw - 0.4, h: 0.16, fontSize: 7, italic: true, color: GREY, align: "center", valign: "middle", fontFace: "Arial", margin: 0 });
});

// PHONE 4 — 동선 관리
drawPhone(phStartX + (phW + phGap) * 3, phStartY, phW, phH, (sx, sy, sw) => {
  s.addText("ROUTE", { x: sx, y: sy, w: sw, h: 0.22, fontSize: 8, bold: true, color: SAGE, align: "center", charSpacing: 1, fontFace: "Arial", margin: 0 });
  s.addShape(pres.shapes.RECTANGLE, { x: sx + 0.2, y: sy + 0.3, w: sw - 0.4, h: 0.85, fill: { color: SAGE_LIGHT }, line: { color: SAGE_LIGHT, width: 0 } });
  s.addText("📍", { x: sx + 0.2, y: sy + 0.5, w: sw - 0.4, h: 0.45, fontSize: 22, color: CORAL, align: "center", fontFace: "Arial", margin: 0 });
  s.addText("30분 후 점심", { x: sx, y: sy + 1.25, w: sw, h: 0.22, fontSize: 9, bold: true, color: SAGE_DARK, align: "center", fontFace: "Arial", margin: 0 });
  s.addText("부여 정림사지", { x: sx, y: sy + 1.5, w: sw, h: 0.18, fontSize: 8, color: GREY, italic: true, align: "center", fontFace: "Arial", margin: 0 });
  s.addShape(pres.shapes.RECTANGLE, { x: sx + 0.18, y: sy + 1.78, w: sw - 0.36, h: 0.32, fill: { color: CREAM }, line: { color: CORAL, width: 0.5 } });
  s.addText("🔔 잠시 후 안내", { x: sx + 0.18, y: sy + 1.78, w: sw - 0.36, h: 0.32, fontSize: 7, bold: true, color: CORAL_DARK, align: "center", valign: "middle", fontFace: "Arial", margin: 0 });
});

// PHONE 5 — 디지털 디톡스
drawPhone(phStartX + (phW + phGap) * 4, phStartY, phW, phH, (sx, sy, sw, sh) => {
  s.addShape(pres.shapes.RECTANGLE, { x: sx, y: sy - 0.2, w: sw, h: sh + 0.3, fill: { color: SAGE_DARK }, line: { color: SAGE_DARK, width: 0 } });
  s.addText("DETOX MODE", { x: sx, y: sy + 0.05, w: sw, h: 0.22, fontSize: 8, bold: true, color: CORAL, align: "center", charSpacing: 2, fontFace: "Arial", margin: 0 });
  s.addText("🔒", { x: sx, y: sy + 0.4, w: sw, h: 0.55, fontSize: 30, color: WHITE, align: "center", fontFace: "Arial", margin: 0 });
  s.addText("여행 중\n다른 앱은\n잠시 멈춤", { x: sx, y: sy + 1.05, w: sw, h: 0.65, fontSize: 10, bold: true, color: WHITE, align: "center", fontFace: "Arial", margin: 0 });
  s.addText("진짜 휴식의 시간", { x: sx, y: sy + 1.78, w: sw, h: 0.2, fontSize: 8, italic: true, color: CORAL, align: "center", fontFace: "Arial", margin: 0 });
});

// 5단계 라벨
const labels = ["① 예산 선결제", "② AI 자동 예약", "③ 서프라이즈", "④ 동선 관리", "⑤ 디지털 디톡스"];
labels.forEach((lb, i) => {
  s.addText(lb, {
    x: phStartX + i * (phW + phGap), y: phStartY + phH + 0.1, w: phW, h: 0.25,
    fontSize: 11, bold: true, color: SAGE_DARK, align: "center", fontFace: "Arial", margin: 0
  });
});

// ============================================================
// [5] 충남 산업 연계성 (10.55 ~ 12.85") — 평가 30점 항목
// ============================================================
s.addText("04   CHUNGNAM CONNECTION", {
  x: 0.6, y: 10.55, w: 10.5, h: 0.28,
  fontSize: 11, bold: true, color: CORAL, charSpacing: 3, fontFace: "Arial", margin: 0
});
s.addText("충남 지역특화산업 연계", {
  x: 0.6, y: 10.85, w: 10.5, h: 0.45,
  fontSize: 22, bold: true, color: SAGE_DARK, fontFace: "Arial", margin: 0
});

// 3개 카드 — 자산 / 소상공인 / 공공
const connections = [
  {
    tag: "ASSETS",
    title: "충남의 자산",
    items: ["유네스코 백제유산\n(공주·부여)", "태안 해안국립공원", "아산 온양온천", "독립기념관 (연 106만)"]
  },
  {
    tag: "MERCHANTS",
    title: "지역 소상공인 매칭",
    items: ["전통·로컬 식당", "한옥·게스트하우스·펜션", "도자기·온천·해양 체험", "AI 자동 송객·결제"]
  },
  {
    tag: "PUBLIC",
    title: "공공 협력 (B2G2C)",
    items: ["충청남도 + 시·군청", "관광공사 데이터 연동", "선문대 SW 사업단 멘토링", "충남 정주형 청년 창업 모델"]
  }
];
connections.forEach((c, i) => {
  const x = 0.6 + i * 3.61;
  const y = 11.45;
  s.addShape(pres.shapes.RECTANGLE, {
    x: x, y: y, w: 3.46, h: 1.4,
    fill: { color: WHITE }, line: { color: SAGE_LIGHT, width: 1 }
  });
  s.addShape(pres.shapes.RECTANGLE, {
    x: x, y: y, w: 3.46, h: 0.32,
    fill: { color: SAGE_DARK }, line: { color: SAGE_DARK, width: 0 }
  });
  s.addText(c.tag, {
    x: x, y: y, w: 3.46, h: 0.32,
    fontSize: 9, bold: true, color: CORAL, align: "center", valign: "middle", charSpacing: 3, fontFace: "Arial", margin: 0
  });
  s.addText(c.title, {
    x: x + 0.15, y: y + 0.4, w: 3.16, h: 0.3,
    fontSize: 13, bold: true, color: SAGE_DARK, fontFace: "Arial", margin: 0
  });
  c.items.forEach((it, j) => {
    s.addText("· " + it, {
      x: x + 0.15, y: y + 0.7 + j * 0.18, w: 3.16, h: 0.18,
      fontSize: 8, color: NAVY_TEXT, fontFace: "Arial", margin: 0
    });
  });
});

// ============================================================
// [6] 차별화 + 시장 (13.0 ~ 15.4")
// ============================================================
s.addText("05   DIFFERENTIATION & MARKET", {
  x: 0.6, y: 13.0, w: 10.5, h: 0.28,
  fontSize: 11, bold: true, color: CORAL, charSpacing: 3, fontFace: "Arial", margin: 0
});
s.addText("차별화와 시장 기회", {
  x: 0.6, y: 13.3, w: 10.5, h: 0.45,
  fontSize: 22, bold: true, color: SAGE_DARK, fontFace: "Arial", margin: 0
});

// 좌측: 5가지 차별점
s.addShape(pres.shapes.RECTANGLE, {
  x: 0.6, y: 13.9, w: 6.6, h: 1.6,
  fill: { color: WHITE }, line: { color: SAGE_LIGHT, width: 1 }
});
s.addText("기존 여행 앱과 다른 5가지", {
  x: 0.75, y: 14.0, w: 6.4, h: 0.3,
  fontSize: 12, bold: true, color: SAGE_DARK, fontFace: "Arial", margin: 0
});
const diffs = [
  "AI 100% 자동 결정 (기존 앱 ≠ 사용자 직접 선택)",
  "도착 전까지 목적지 비공개 (서프라이즈)",
  "예산 선결제 → AI 일괄 처리",
  "여행 중 타 앱 차단 (디지털 디톡스)",
  "충남 지역 깊이 특화 (지자체·소상공인 연동)"
];
diffs.forEach((d, i) => {
  s.addText(`✓ ${d}`, {
    x: 0.85, y: 14.35 + i * 0.22, w: 6.3, h: 0.2,
    fontSize: 9, color: NAVY_TEXT, fontFace: "Arial", margin: 0
  });
});

// 우측: 시장 통계
s.addShape(pres.shapes.RECTANGLE, {
  x: 7.4, y: 13.9, w: 3.7, h: 1.6,
  fill: { color: SAGE_DARK }, line: { color: SAGE_DARK, width: 0 }
});
s.addText("MARKET SIZE", {
  x: 7.4, y: 14.0, w: 3.7, h: 0.26,
  fontSize: 9, bold: true, color: CORAL, charSpacing: 3, align: "center", fontFace: "Arial", margin: 0
});
s.addText("CAGR 24.5%", {
  x: 7.4, y: 14.32, w: 3.7, h: 0.45,
  fontSize: 22, bold: true, color: WHITE, align: "center", fontFace: "Arial", margin: 0
});
s.addText("디지털 디톡스 관광 시장", {
  x: 7.4, y: 14.78, w: 3.7, h: 0.2,
  fontSize: 9, color: CREAM, italic: true, align: "center", fontFace: "Arial", margin: 0
});
s.addShape(pres.shapes.LINE, {
  x: 7.7, y: 15.05, w: 3.1, h: 0, line: { color: CORAL, width: 1 }
});
s.addText("$65B (2025) → $467B (2034)", {
  x: 7.4, y: 15.13, w: 3.7, h: 0.3,
  fontSize: 11, bold: true, color: WHITE, align: "center", fontFace: "Arial", margin: 0
});

// 출처
s.addText("출처: Polaris Market Research, 통계청 1인가구 통계 (2024), 한국관광공사 데이터랩", {
  x: 0.6, y: 15.55, w: 10.5, h: 0.22,
  fontSize: 8, italic: true, color: LIGHT_GREY, align: "center", fontFace: "Arial", margin: 0
});

// ============================================================
// [7] 푸터 (16.05 ~ 16.54")
// ============================================================
s.addShape(pres.shapes.RECTANGLE, {
  x: 0, y: 16.05, w: 11.69, h: 0.49,
  fill: { color: SAGE_DARK }, line: { color: SAGE_DARK, width: 0 }
});
s.addText([
  { text: "TEAM  ", options: { color: CORAL, bold: true, charSpacing: 3 } },
  { text: "김태훈 (대표) · 김재원 · 김준서 · 이윤호", options: { color: WHITE, bold: true } },
  { text: "     |     ", options: { color: LIGHT_GREY } },
  { text: "지도교사  ", options: { color: CORAL, bold: true, charSpacing: 2 } },
  { text: "김영우", options: { color: WHITE } },
  { text: "     |     ", options: { color: LIGHT_GREY } },
  { text: "아산스마트팩토리마이스터고등학교", options: { color: CREAM } }
], {
  x: 0.6, y: 16.05, w: 11, h: 0.49,
  fontSize: 10, fontFace: "Arial", align: "center", valign: "middle", margin: 0
});

pres.writeFile({ fileName: "/Users/kimtaehun/Documents/2026프로젝트/인공지능_여행앱_따라와유_프로젝트/선문대_창업아이디어_경진대회/따라와유_본선_포스터_A3.pptx" })
  .then(fileName => { console.log(`포스터 생성 완료: ${fileName}`); });
