// build_ppt.js v4 — 따라와유 본선 발표 · Plog Style (흰 배경 + 어노테이션)
const pptxgen = require("pptxgenjs");
const path    = require("path");
const pres    = new pptxgen();
pres.layout   = "LAYOUT_16x9";
pres.title    = "따라와유";

// ── 팔레트 ──────────────────────────────────────────────────────
const SAGE_DARK  = "2D3F37";
const SAGE       = "5C7A6E";
const SAGE_LIGHT = "8FA89A";
const CREAM      = "F5E6D3";
const CORAL      = "E07856";
const WHITE      = "FFFFFF";
const NAVY       = "1F2937";
const GREY       = "6B7280";
const LGREY      = "D1D5DB";
const BG         = "FAF7F2";
const FRAME      = "1A1A1A";   // 폰 프레임
const SCDIR      = path.join(__dirname, "app_screens");

// ── 얇은 아이폰 프레임 ─────────────────────────────────────────
// 화면 이미지 경로를 fn으로 받아 내부 스크린 좌표 반환
function phone(s, x, y, w, h, imgPath) {
  const p  = 0.055;
  const sx = x+p, sy = y+p, sw = w-p*2, sh = h-p*2;
  // 바디
  s.addShape(pres.shapes.ROUNDED_RECTANGLE, {
    x,y,w,h, fill:{color:FRAME}, line:{color:FRAME,width:0}, rectRadius:0.18
  });
  // 스크린 배경
  s.addShape(pres.shapes.ROUNDED_RECTANGLE, {
    x:sx,y:sy,w:sw,h:sh, fill:{color:WHITE}, line:{color:WHITE,width:0}, rectRadius:0.14
  });
  // 다이나믹 아일랜드
  s.addShape(pres.shapes.ROUNDED_RECTANGLE, {
    x:x+w/2-0.18, y:sy+0.06, w:0.36, h:0.1,
    fill:{color:FRAME}, line:{color:FRAME,width:0}, rectRadius:0.05
  });
  // 스크린샷
  if(imgPath) {
    s.addImage({path:imgPath, x:sx, y:sy, w:sw, h:sh});
  }
  // 홈 인디케이터
  s.addShape(pres.shapes.ROUNDED_RECTANGLE, {
    x:x+w/2-0.28, y:y+h-0.1, w:0.56, h:0.04,
    fill:{color:"444444"}, line:{color:"444444",width:0}, rectRadius:0.02
  });
  return {sx,sy,sw,sh};
}

// ── Plog 스타일 헤더 ───────────────────────────────────────────
function featureHdr(s, title, hlW) {
  s.addText("주요 기능", {
    x:0.45, y:0.28, w:3, h:0.25,
    fontSize:11, color:GREY, fontFace:"Arial", charSpacing:2, margin:0
  });
  // 형광펜 하이라이트
  s.addShape(pres.shapes.RECTANGLE, {
    x:0.43, y:0.78, w:hlW||2.0, h:0.16,
    fill:{color:CORAL, transparency:55}, line:{color:CORAL,width:0}
  });
  s.addText(title, {
    x:0.45, y:0.50, w:7, h:0.55,
    fontSize:36, bold:true, color:NAVY, fontFace:"Arial", margin:0
  });
  // 우상단 브랜딩
  s.addText("따라와유 (Tarayou)", {
    x:7.8, y:0.28, w:2.0, h:0.25,
    fontSize:10, color:LGREY, fontFace:"Arial", align:"right", margin:0
  });
}

// ── 일반 슬라이드 우상단 브랜딩 ────────────────────────────────
function brand(s) {
  s.addText("따라와유 (Tarayou)", {
    x:7.8, y:0.28, w:2.0, h:0.25,
    fontSize:10, color:LGREY, fontFace:"Arial", align:"right", margin:0
  });
}

// ── 점선 어노테이션 ────────────────────────────────────────────
// dir: "left"→라벨이 폰 왼쪽 / "right"→라벨이 폰 오른쪽
function annotate(s, dotX, dotY, labelX, labelY, text) {
  // 점 (폰에 붙는 쪽)
  s.addShape(pres.shapes.OVAL, {
    x:dotX-0.06, y:dotY-0.06, w:0.12, h:0.12,
    fill:{color:CORAL}, line:{color:CORAL,width:0}
  });
  // 수평 점선
  const lx = Math.min(dotX, labelX+1.9);
  const rx = Math.max(dotX, labelX+1.9);
  s.addShape(pres.shapes.LINE, {
    x:lx, y:dotY, w:rx-lx, h:0,
    line:{color:SAGE_LIGHT, width:0.75, dashType:"dash"}
  });
  // 수직 점선 (필요할 때)
  if(Math.abs(dotY-labelY) > 0.05) {
    s.addShape(pres.shapes.LINE, {
      x:labelX+1.9, y:Math.min(dotY,labelY+0.12),
      w:0, h:Math.abs(dotY-labelY-0.12),
      line:{color:SAGE_LIGHT, width:0.75, dashType:"dash"}
    });
  }
  // 라벨 텍스트
  const textRight = labelX > dotX;
  s.addText(text, {
    x:labelX, y:labelY, w:1.85, h:0.4,
    fontSize:10.5, color:NAVY, fontFace:"Arial",
    align: textRight ? "left" : "right", margin:0
  });
}

// ============================================================
// [01] 표지
// ============================================================
{
  const s = pres.addSlide();
  s.background = {color:WHITE};

  // 오른쪽 그라데이션 배경 박스 (Plog cover 스타일)
  s.addShape(pres.shapes.ROUNDED_RECTANGLE, {
    x:4.5, y:0, w:5.5, h:5.625,
    fill:{color:"F0F4F2"}, line:{color:"F0F4F2",width:0}, rectRadius:0
  });
  // 우측 CORAL 포인트
  s.addShape(pres.shapes.RECTANGLE, {
    x:9.82, y:0, w:0.18, h:5.625, fill:{color:CORAL}, line:{color:CORAL,width:0}
  });

  // 좌측: 앱 아이콘 + 브랜딩
  s.addImage({
    path:path.join(__dirname,"app_screens","icon.png"),
    x:0.5, y:0.6, w:1.8, h:1.8
  });
  s.addText("따라와유", {
    x:0.45, y:2.55, w:4.0, h:1.0,
    fontSize:58, bold:true, color:NAVY, fontFace:"Arial", margin:0, charSpacing:-2
  });
  s.addText("TARAYOU", {
    x:0.45, y:3.6, w:4.0, h:0.3,
    fontSize:12, color:CORAL, bold:true, charSpacing:6, fontFace:"Arial", margin:0
  });
  s.addText("예산만 넣으면, AI가 충남 여행을\n완전 자동으로 처리합니다", {
    x:0.45, y:4.05, w:4.0, h:0.7,
    fontSize:13, color:GREY, fontFace:"Arial", margin:0
  });

  // 우측: 3개 폰 — 실제 목업 이미지 (비율 2.11)
  const MW=1.62, MH=3.42;
  s.addImage({path:path.join(SCDIR,"mockup_phone1.png"), x:4.80, y:0.22, w:MW, h:MH});
  s.addImage({path:path.join(SCDIR,"mockup_phone3.png"), x:6.62, y:0.92, w:MW, h:MH});
  s.addImage({path:path.join(SCDIR,"mockup_phone5.png"), x:8.00, y:0.38, w:MW, h:MH});

  // 하단 팀 정보
  s.addShape(pres.shapes.LINE, {
    x:0.45, y:4.9, w:4.0, h:0, line:{color:LGREY,width:1}
  });
  s.addText("아산스마트팩토리마이스터고  ·  김태훈(대표) 김재원 김준서 이윤호  ·  지도교사 김영우", {
    x:0.45, y:5.04, w:9.2, h:0.25, fontSize:9.5, color:GREY, fontFace:"Arial", margin:0
  });
  s.addText("2026 충남권 고교-대학 창업아이디어 경진대회 본선", {
    x:0.45, y:5.3, w:9.2, h:0.22, fontSize:9, color:LGREY, fontFace:"Arial", margin:0
  });
}

// ============================================================
// [02] 아이디어 — 한 문장
// ============================================================
{
  const s = pres.addSlide();
  s.background = {color:WHITE};
  brand(s);

  // 상단 흐름 (3단계)
  const steps=[
    {icon:"💰", label:"예산 입력"},
    {icon:"🤖", label:"AI 자동 처리"},
    {icon:"🎁", label:"여행 출발"},
  ];
  steps.forEach((st,i)=>{
    const x=1.6+i*3.0;
    s.addShape(pres.shapes.OVAL, {
      x:x+0.35, y:0.55, w:0.8, h:0.8,
      fill:{color:i===1?CORAL:"F5F5F5"}, line:{color:i===1?CORAL:LGREY,width:1}
    });
    s.addText(st.icon, {x:x+0.35,y:0.55,w:0.8,h:0.8, fontSize:22,align:"center",valign:"middle",fontFace:"Arial",margin:0});
    s.addText(st.label, {x:x, y:1.42, w:1.5, h:0.28, fontSize:11,color:GREY,align:"center",fontFace:"Arial",margin:0});
    if(i<2) s.addText("→", {x:x+1.7,y:0.68,w:0.5,h:0.55, fontSize:18,color:LGREY,bold:true,align:"center",fontFace:"Arial",margin:0});
  });

  s.addShape(pres.shapes.LINE, {x:1.2,y:1.9,w:7.6,h:0, line:{color:LGREY,width:1}});

  // 핵심 메시지
  s.addText("사용자는", {
    x:0.5, y:2.1, w:9, h:0.65,
    fontSize:30, color:GREY, align:"center", fontFace:"Arial", margin:0
  });
  s.addText("따라가기만 하면 됩니다.", {
    x:0.5, y:2.75, w:9, h:1.05,
    fontSize:60, bold:true, color:NAVY, align:"center", fontFace:"Arial", margin:0, charSpacing:-1
  });
  // 형광펜
  s.addShape(pres.shapes.RECTANGLE, {
    x:1.6, y:3.62, w:6.8, h:0.14,
    fill:{color:CORAL,transparency:60}, line:{color:CORAL,width:0}
  });
  s.addText("코스·식당·숙소·이동·예약 모두 AI 결정  ·  목적지는 출발 당일 첫 공개", {
    x:0.5, y:4.05, w:9, h:0.3,
    fontSize:12, color:GREY, align:"center", fontFace:"Arial", margin:0
  });
}

// ============================================================
// [03] 동기 — 직접 겪은 이야기
// ============================================================
{
  const s = pres.addSlide();
  s.background = {color:WHITE};
  brand(s);

  // 큰 따옴표
  s.addText('"', {
    x:0.2, y:-0.4, w:2, h:2,
    fontSize:220, color:CORAL, bold:true, fontFace:"Arial", margin:0, transparency:85
  });
  s.addText("갈 데가 없어서가 아니라", {
    x:0.9, y:1.1, w:8.5, h:0.68,
    fontSize:36, color:GREY, fontFace:"Arial", margin:0
  });
  s.addText("정하기 귀찮아서 안 갑니다", {
    x:0.9, y:1.82, w:8.5, h:0.88,
    fontSize:46, bold:true, color:NAVY, fontFace:"Arial", margin:0
  });
  // 형광펜
  s.addShape(pres.shapes.RECTANGLE, {
    x:0.88, y:2.58, w:5.65, h:0.14,
    fill:{color:CORAL,transparency:60}, line:{color:CORAL,width:0}
  });
  s.addText("— 친구 30명 직접 인터뷰 / 팀원 가족들의 실제 경험", {
    x:0.9, y:2.82, w:8, h:0.28,
    fontSize:12, color:LGREY, fontFace:"Arial", margin:0
  });
  s.addShape(pres.shapes.LINE, {x:0.9,y:3.28,w:8.5,h:0, line:{color:LGREY,width:1}});
  const exps=[
    "충남에 살면서도 충남 여행 코스를 못 짜는 모순",
    "부모님: \"휴가가 있어도 어디 갈지 몰라 결국 집에\"",
    "기숙사 휴식 시간에도 SNS를 끊지 못하는 디지털 피로",
    "→ 개인 경험이 사회 문제임을 데이터로 확인",
  ];
  exps.forEach((e,i)=>{
    s.addText(e, {
      x:0.9, y:3.46+i*0.42, w:8.5, h:0.38,
      fontSize:13, color:i===3?CORAL:GREY, bold:i===3, fontFace:"Arial", margin:0
    });
  });
}

// ============================================================
// [04] 문제의식 — 숫자로 말한다
// ============================================================
{
  const s = pres.addSlide();
  s.background = {color:WHITE};
  brand(s);

  // 왼쪽 숫자
  s.addText("804만", {
    x:0.45, y:0.55, w:5.2, h:1.9,
    fontSize:100, bold:true, color:NAVY, fontFace:"Arial", margin:0, charSpacing:-3
  });
  s.addShape(pres.shapes.RECTANGLE, {
    x:0.43, y:2.32, w:2.6, h:0.14,
    fill:{color:CORAL,transparency:55}, line:{color:CORAL,width:0}
  });
  s.addText("1인 가구", {
    x:0.45, y:2.42, w:5.2, h:0.48,
    fontSize:22, bold:true, color:NAVY, fontFace:"Arial", margin:0
  });
  s.addText("여행 욕구는 있지만 특화 서비스가 없다", {
    x:0.45, y:2.94, w:5.2, h:0.32,
    fontSize:13, color:GREY, fontFace:"Arial", margin:0
  });

  s.addShape(pres.shapes.LINE, {x:5.7,y:0.6,w:0,h:3.5, line:{color:LGREY,width:1}});

  // 오른쪽 숫자
  s.addText("14시간", {
    x:6.0, y:0.55, w:3.8, h:1.7,
    fontSize:80, bold:true, color:NAVY, fontFace:"Arial", margin:0, charSpacing:-2
  });
  s.addShape(pres.shapes.RECTANGLE, {
    x:5.98, y:2.12, w:2.8, h:0.14,
    fill:{color:CORAL,transparency:55}, line:{color:CORAL,width:0}
  });
  s.addText("여행 계획 평균 소요 시간", {
    x:6.0, y:2.22, w:3.8, h:0.48,
    fontSize:18, bold:true, color:NAVY, fontFace:"Arial", margin:0
  });
  s.addText("결국 포기로 끝난다", {
    x:6.0, y:2.72, w:3.8, h:0.32,
    fontSize:13, color:GREY, fontFace:"Arial", margin:0
  });

  // 하단 배너
  s.addShape(pres.shapes.ROUNDED_RECTANGLE, {
    x:0.45, y:4.12, w:9.1, h:1.12,
    fill:{color:"FFF5F0"}, line:{color:CORAL,width:1.5}, rectRadius:0.08
  });
  s.addText("충남 1인 여행객의 90%는 수도권·부산·제주를 선택합니다", {
    x:0.45, y:4.12, w:9.1, h:1.12,
    fontSize:18, bold:true, color:CORAL, align:"center", valign:"middle", fontFace:"Arial", margin:0
  });
}

// ============================================================
// [05] 타겟 & 시나리오
// ============================================================
{
  const s = pres.addSlide();
  s.background = {color:WHITE};
  brand(s);

  // 퍼소나 카드
  s.addShape(pres.shapes.ROUNDED_RECTANGLE, {
    x:0.45, y:0.52, w:4.05, h:4.68,
    fill:{color:"F9FAFB"}, line:{color:LGREY,width:1}, rectRadius:0.12
  });
  // 상단 CORAL 헤더
  s.addShape(pres.shapes.ROUNDED_RECTANGLE, {
    x:0.45, y:0.52, w:4.05, h:0.45,
    fill:{color:CORAL}, line:{color:CORAL,width:0}, rectRadius:0.12
  });
  s.addText("타겟 고객", {
    x:0.45, y:0.52, w:4.05, h:0.45,
    fontSize:12, bold:true, color:WHITE, align:"center", valign:"middle", fontFace:"Arial", margin:0
  });
  s.addText("👤", {x:0.45,y:1.05,w:4.05,h:0.9, fontSize:46,align:"center",fontFace:"Arial",margin:0});
  s.addText("김민준 / 32세", {
    x:0.45,y:2.0,w:4.05,h:0.42, fontSize:19,bold:true,color:NAVY,align:"center",fontFace:"Arial",margin:0
  });
  s.addText("IT 회사 3년차 직장인", {
    x:0.45,y:2.44,w:4.05,h:0.3, fontSize:12.5,color:GREY,align:"center",fontFace:"Arial",margin:0
  });
  s.addShape(pres.shapes.LINE, {x:0.75,y:2.88,w:3.45,h:0, line:{color:LGREY,width:1}});
  ["연차 쌓이는 중, 여행 욕구 높음","주말엔 핸드폰 놓고 싶음","계획 세우다 지쳐서 매번 포기"].forEach((t,i)=>{
    s.addText("·  "+t, {x:0.65,y:3.06+i*0.42,w:3.72,h:0.38, fontSize:12,color:GREY,fontFace:"Arial",margin:0});
  });

  // 5단계 시나리오
  s.addText("어떻게 사용하나요?", {
    x:4.85, y:0.52, w:5.0, h:0.44,
    fontSize:20, bold:true, color:NAVY, fontFace:"Arial", margin:0
  });
  const steps=[
    {n:"01", title:"예산 30만원 입력·결제", desc:"스마트폰 화면 하나로 완료"},
    {n:"02", title:"AI가 1분 내 자동 예약",  desc:"코스·식당·숙소·교통 전부"},
    {n:"03", title:"출발 당일 목적지 공개",  desc:"서프라이즈 — 충남 어딘가"},
    {n:"04", title:"실시간 동선 안내",       desc:"지도·알림·타이머로 안내"},
    {n:"05", title:"디지털 디톡스 ON",      desc:"여행 중 타 앱 자동 차단"},
  ];
  steps.forEach((st,i)=>{
    const y=1.12+i*0.82;
    s.addShape(pres.shapes.OVAL, {x:4.85,y:y+0.04,w:0.36,h:0.36, fill:{color:CORAL},line:{color:CORAL,width:0}});
    s.addText(st.n, {x:4.85,y:y+0.04,w:0.36,h:0.36, fontSize:9,bold:true,color:WHITE,align:"center",valign:"middle",fontFace:"Arial",margin:0});
    s.addText(st.title, {x:5.32,y:y,     w:4.5,h:0.28, fontSize:14,bold:true,color:NAVY,fontFace:"Arial",margin:0});
    s.addText(st.desc,  {x:5.32,y:y+0.28,w:4.5,h:0.24, fontSize:11,color:GREY,fontFace:"Arial",margin:0});
    if(i<4) s.addShape(pres.shapes.LINE,{x:5.02,y:y+0.44,w:0,h:0.38,line:{color:LGREY,width:1.5}});
  });
}

// ============================================================
// [06] 시장 규모
// ============================================================
{
  const s = pres.addSlide();
  s.background = {color:WHITE};
  brand(s);

  s.addText("시장은 충분히 큽니다", {
    x:0.45, y:0.42, w:9.1, h:0.65,
    fontSize:36, bold:true, color:NAVY, fontFace:"Arial", margin:0
  });
  s.addShape(pres.shapes.RECTANGLE, {
    x:0.43, y:0.94, w:4.2, h:0.14,
    fill:{color:CORAL,transparency:55}, line:{color:CORAL,width:0}
  });

  const tiers=[
    {key:"TAM", num:"₩36.8조", desc:"국내 여행 총 시장 (숙박+교통+식음)", off:0.0,  fill:"F9FAFB", brd:LGREY, nc:NAVY},
    {key:"SAM", num:"₩5조",    desc:"1인 여행·충남 특화 시장",           off:0.4,  fill:"F9FAFB", brd:LGREY, nc:NAVY},
    {key:"SOM", num:"₩1,500억",desc:"3년 목표 → 연매출 200억",           off:0.8,  fill:"FFF5F0", brd:CORAL, nc:CORAL},
  ];
  tiers.forEach((t,i)=>{
    const y=1.28+i*1.08;
    s.addShape(pres.shapes.ROUNDED_RECTANGLE, {
      x:0.45+t.off, y, w:9.1-t.off*2, h:0.88,
      fill:{color:t.fill}, line:{color:t.brd,width:i===2?1.5:1}, rectRadius:0.08
    });
    s.addText(t.key,  {x:0.55+t.off, y, w:1.2,         h:0.88, fontSize:11,bold:true,color:t.nc,charSpacing:2,valign:"middle",fontFace:"Arial",margin:0});
    s.addText(t.num,  {x:1.65+t.off, y, w:3.4,         h:0.88, fontSize:38,bold:true,color:t.nc,valign:"middle",fontFace:"Arial",margin:0});
    s.addText(t.desc, {x:5.0+t.off,  y, w:4.5-t.off*2, h:0.88, fontSize:12,color:GREY,valign:"middle",fontFace:"Arial",margin:0});
  });

  s.addShape(pres.shapes.ROUNDED_RECTANGLE, {
    x:0.45, y:4.6, w:9.1, h:0.68,
    fill:{color:"F9FAFB"}, line:{color:LGREY,width:1}, rectRadius:0.06
  });
  s.addText("디지털 디톡스 여행 시장  CAGR 24.5%  ·  $52.3B (2024) → $466.6B (2034)   (Polaris Market Research)", {
    x:0.45, y:4.6, w:9.1, h:0.68,
    fontSize:11.5, color:GREY, align:"center", valign:"middle", fontFace:"Arial", margin:0
  });
}

// ============================================================
// [07] 수익 모델
// ============================================================
{
  const s = pres.addSlide();
  s.background = {color:WHITE};
  brand(s);

  s.addText("3가지 수익 구조", {
    x:0.45, y:0.42, w:9.1, h:0.62,
    fontSize:36, bold:true, color:NAVY, fontFace:"Arial", margin:0
  });
  s.addShape(pres.shapes.RECTANGLE, {
    x:0.43, y:0.9, w:3.5, h:0.14,
    fill:{color:CORAL,transparency:55}, line:{color:CORAL,width:0}
  });

  const models=[
    {icon:"🔗", num:"12%",     label:"예약 수수료",  desc:"식당·숙소·체험\n예약 건당 정률 수수료", hi:true},
    {icon:"⭐", num:"₩9,900", label:"프리미엄 구독", desc:"월정액\n큐레이션 우선·디톡스 리포트", hi:false},
    {icon:"🏛️", num:"B2G",    label:"데이터 제공",  desc:"충남도청·관광공사\n여행 패턴 분석 리포트", hi:false},
  ];
  models.forEach((m,i)=>{
    const x=0.45+i*3.17;
    s.addShape(pres.shapes.ROUNDED_RECTANGLE, {
      x, y:1.22, w:3.02, h:3.95,
      fill:{color:m.hi?"FFF5F0":"F9FAFB"}, line:{color:m.hi?CORAL:LGREY,width:m.hi?1.5:1}, rectRadius:0.12
    });
    s.addText(m.icon, {x, y:1.52, w:3.02, h:0.75, fontSize:36, align:"center", fontFace:"Arial", margin:0});
    s.addText(m.num,  {x, y:2.38, w:3.02, h:0.75, fontSize:36, bold:true, color:m.hi?CORAL:NAVY, align:"center", fontFace:"Arial", margin:0});
    s.addText(m.label,{x, y:3.2,  w:3.02, h:0.38, fontSize:14, bold:true, color:NAVY, align:"center", fontFace:"Arial", margin:0});
    s.addShape(pres.shapes.LINE,{x:x+0.3, y:3.62, w:2.42, h:0, line:{color:LGREY,width:1}});
    s.addText(m.desc, {x, y:3.72, w:3.02, h:0.78, fontSize:11, color:GREY, align:"center", fontFace:"Arial", margin:0});
  });
}

// ============================================================
// [08] 차별성
// ============================================================
{
  const s = pres.addSlide();
  s.background = {color:WHITE};
  brand(s);

  s.addText("왜 따라와유인가?", {
    x:0.45, y:0.38, w:9.1, h:0.58,
    fontSize:36, bold:true, color:NAVY, fontFace:"Arial", margin:0
  });
  s.addShape(pres.shapes.RECTANGLE, {
    x:0.43, y:0.83, w:3.4, h:0.14,
    fill:{color:CORAL,transparency:55}, line:{color:CORAL,width:0}
  });

  const colX=[0.38,2.22,4.08,5.94,7.8];
  const colW=[1.8,1.82,1.82,1.82,1.82];
  const hdrs=["","따라와유","야놀자","트리플","네이버 여행"];
  hdrs.forEach((h,i)=>{
    if(!i) return;
    s.addShape(pres.shapes.ROUNDED_RECTANGLE, {
      x:colX[i], y:1.1, w:colW[i], h:0.48,
      fill:{color:i===1?CORAL:"F9FAFB"}, line:{color:i===1?CORAL:LGREY,width:1}, rectRadius:0.06
    });
    s.addText(h, {x:colX[i],y:1.1,w:colW[i],h:0.48, fontSize:11.5,bold:true,color:i===1?WHITE:GREY,align:"center",valign:"middle",fontFace:"Arial",margin:0});
  });

  const rows=[
    {label:"AI 완전 자동 결정",  vals:["✓","✗","✗","✗"]},
    {label:"서프라이즈 공개",    vals:["✓","✗","✗","✗"]},
    {label:"예산 선결제",       vals:["✓","△","✗","✗"]},
    {label:"디지털 디톡스",     vals:["✓","✗","✗","✗"]},
    {label:"충남 소상공인 특화", vals:["✓","✗","✗","△"]},
  ];
  rows.forEach((r,ri)=>{
    const y=1.72+ri*0.65;
    s.addShape(pres.shapes.RECTANGLE, {
      x:0.38, y, w:9.24, h:0.62,
      fill:{color:ri%2===0?WHITE:"F9FAFB"}, line:{color:LGREY,width:1}
    });
    s.addText(r.label, {x:0.5,y,w:colW[0]-0.12,h:0.62, fontSize:13,bold:true,color:NAVY,valign:"middle",fontFace:"Arial",margin:0});
    r.vals.forEach((v,vi)=>{
      const us=vi===0;
      s.addText(v, {
        x:colX[vi+1]+0.1, y, w:colW[vi+1]-0.2, h:0.62,
        fontSize:18, bold:us,
        color:v==="✓"?(us?CORAL:SAGE_LIGHT):v==="△"?LGREY:LGREY,
        align:"center", valign:"middle", fontFace:"Arial", margin:0
      });
    });
  });
}

// ============================================================
// [09] AI·SW 기술 구현 (Plog 아키텍처 스타일)
// ============================================================
{
  const s = pres.addSlide();
  s.background = {color:WHITE};
  brand(s);

  s.addText("기술 아키텍처", {
    x:0.45, y:0.38, w:9.1, h:0.55,
    fontSize:36, bold:true, color:NAVY, fontFace:"Arial", margin:0
  });
  s.addShape(pres.shapes.RECTANGLE, {
    x:0.43, y:0.82, w:2.8, h:0.14,
    fill:{color:CORAL,transparency:55}, line:{color:CORAL,width:0}
  });

  // 아키텍처 박스 (ref7 스타일)
  s.addShape(pres.shapes.ROUNDED_RECTANGLE, {
    x:2.2, y:1.1, w:7.3, h:4.2,
    fill:{color:"F9FAFB"}, line:{color:LGREY,width:1}, rectRadius:0.1
  });
  // AI 엔진 헤더
  s.addShape(pres.shapes.ROUNDED_RECTANGLE, {
    x:2.2, y:1.1, w:7.3, h:0.48,
    fill:{color:SAGE_DARK}, line:{color:SAGE_DARK,width:0}, rectRadius:0.1
  });
  s.addText("AI 추천 엔진", {
    x:2.2, y:1.1, w:3.5, h:0.48,
    fontSize:13, bold:true, color:WHITE, valign:"middle", fontFace:"Arial", margin:8
  });
  s.addText("React Native  ·  FastAPI  ·  AWS Lambda", {
    x:5.7, y:1.1, w:3.8, h:0.48,
    fontSize:10, color:SAGE_LIGHT, align:"right", valign:"middle", fontFace:"Arial", margin:8
  });

  // 4 레이어
  const layers=[
    {tag:"자연어 이해",  tech:"LangChain + GPT-4",  desc:"예산·취향 → 여행 코스 생성", hi:true},
    {tag:"협업 필터링", tech:"ALS + TF-IDF",        desc:"유사 성향 여행자 패턴 학습", hi:false},
    {tag:"예산 최적화", tech:"XGBoost",             desc:"최고 만족도 조합 자동 예측", hi:false},
    {tag:"경로 최소화", tech:"OR-Tools TSP",        desc:"이동 시간 자동 최적화", hi:false},
  ];
  layers.forEach((l,i)=>{
    const y=1.72+i*0.8;
    s.addShape(pres.shapes.ROUNDED_RECTANGLE, {
      x:2.35, y, w:6.98, h:0.68,
      fill:{color:l.hi?"FFF5F0":WHITE}, line:{color:l.hi?CORAL:LGREY,width:1}, rectRadius:0.06
    });
    s.addShape(pres.shapes.ROUNDED_RECTANGLE, {
      x:2.35, y:y+0.1, w:1.5, h:0.48,
      fill:{color:l.hi?CORAL:SAGE_DARK}, line:{color:l.hi?CORAL:SAGE_DARK,width:0}, rectRadius:0.04
    });
    s.addText(l.tag, {x:2.35, y:y+0.1, w:1.5, h:0.48, fontSize:11,bold:true,color:WHITE,align:"center",valign:"middle",fontFace:"Arial",margin:0});
    s.addText(l.tech, {x:3.95, y, w:2.2, h:0.68, fontSize:12,bold:true,color:NAVY,valign:"middle",fontFace:"Arial",margin:0});
    s.addText(l.desc, {x:6.1,  y, w:3.1, h:0.68, fontSize:11,color:GREY,valign:"middle",fontFace:"Arial",margin:0});
  });

  // 왼쪽: 사용자
  s.addShape(pres.shapes.OVAL, {x:0.3,y:2.15,w:0.95,h:0.95, fill:{color:"F9FAFB"},line:{color:LGREY,width:1}});
  s.addText("👤", {x:0.3,y:2.15,w:0.95,h:0.95, fontSize:28,align:"center",valign:"middle",fontFace:"Arial",margin:0});
  s.addText("사용자", {x:0.3,y:3.15,w:0.95,h:0.28, fontSize:10,color:GREY,align:"center",fontFace:"Arial",margin:0});
  s.addShape(pres.shapes.LINE, {x:1.28,y:2.62,w:0.88,h:0, line:{color:LGREY,width:1}});
  s.addText("↔", {x:1.28,y:2.42,w:0.88,h:0.42, fontSize:18,color:LGREY,align:"center",fontFace:"Arial",margin:0});

  // 하단 DB
  s.addShape(pres.shapes.ROUNDED_RECTANGLE, {
    x:2.2, y:5.0, w:7.3, h:0.28,
    fill:{color:SAGE_DARK}, line:{color:SAGE_DARK,width:0}, rectRadius:0.04
  });
  s.addText("DB  PostgreSQL + Redis  ·  한국관광공사 TourAPI 4.0  ·  기상청 API  ·  교통 API", {
    x:2.2, y:5.0, w:7.3, h:0.28,
    fontSize:9.5, color:SAGE_LIGHT, align:"center", valign:"middle", fontFace:"Arial", margin:0
  });
}

// ============================================================
// [10] 주요 기능 — 예산 입력 + AI 자동 예약 (Plog 스타일)
// ============================================================
{
  const s = pres.addSlide();
  s.background = {color:WHITE};
  featureHdr(s, "예산 입력 · AI 자동 예약", 3.85);

  // 2개 폰 — 실제 목업 이미지 (비율 2.11)
  const PW=1.85, PH=3.90, PY=1.05;
  const P1X=2.05, P2X=6.3;

  s.addImage({path:path.join(SCDIR,"mockup_phone1.png"), x:P1X, y:PY, w:PW, h:PH});
  s.addImage({path:path.join(SCDIR,"mockup_phone2.png"), x:P2X, y:PY, w:PW, h:PH});

  // 폰 위 레이블
  s.addText("① 예산 입력", {
    x:P1X, y:PY-0.32, w:PW, h:0.28,
    fontSize:12, bold:true, color:CORAL, align:"center", fontFace:"Arial", margin:0
  });
  s.addText("② AI 자동 예약", {
    x:P2X, y:PY-0.32, w:PW, h:0.28,
    fontSize:12, bold:true, color:CORAL, align:"center", fontFace:"Arial", margin:0
  });

  // 왼쪽 폰 어노테이션 (왼쪽에 라벨)
  annotate(s, P1X,         PY+0.75,  0.05, PY+0.55,  "예산 슬라이더\n(5만~100만원)");
  annotate(s, P1X,         PY+1.45,  0.05, PY+1.28,  "기간·인원 선택");
  annotate(s, P1X+PW/2,    PY+PH-0.45, 0.05, PY+PH-0.6, "AI에게 맡기기\n선결제 CTA");

  // 오른쪽 폰 어노테이션 (오른쪽에 라벨)
  annotate(s, P2X+PW, PY+0.9,  P2X+PW+0.15, PY+0.72, "AI 처리 현황\n실시간 표시");
  annotate(s, P2X+PW, PY+1.8,  P2X+PW+0.15, PY+1.62, "코스·식당·숙소\n자동 예약 완료");
  annotate(s, P2X+PW, PY+2.8,  P2X+PW+0.15, PY+2.62, "평균 1분 이내\n처리 완료");

  // 중간 화살표
  s.addText("→", {
    x:P1X+PW+0.2, y:PY+PH/2-0.3, w:P2X-(P1X+PW)-0.4, h:0.6,
    fontSize:32, bold:true, color:LGREY, align:"center", valign:"middle", fontFace:"Arial", margin:0
  });
  s.addText("선결제 후\nAI 자동 실행", {
    x:P1X+PW+0.1, y:PY+PH/2+0.32, w:P2X-(P1X+PW)-0.2, h:0.52,
    fontSize:10, color:GREY, align:"center", fontFace:"Arial", margin:0
  });
}

// ============================================================
// [11] 주요 기능 — 서프라이즈 · 동선 안내 · 디지털 디톡스
// ============================================================
{
  const s = pres.addSlide();
  s.background = {color:WHITE};
  featureHdr(s, "서프라이즈 · 동선 · 디톡스", 4.35);

  // 3개 폰 — 실제 목업 이미지 (비율 2.11, 하단 텍스트 여백 확보)
  const PW=1.72, PH=3.63, PY=1.10, GAP=0.38;
  const PX=0.85;

  [3,4,5].forEach((idx,i)=>{
    s.addImage({path:path.join(SCDIR,`mockup_phone${idx}.png`), x:PX+i*(PW+GAP), y:PY, w:PW, h:PH});
  });

  // 폰 위 레이블
  const lbls=["③ 서프라이즈 공개","④ 실시간 동선 안내","⑤ 디지털 디톡스"];
  lbls.forEach((lb,i)=>{
    s.addText(lb, {
      x:PX+i*(PW+GAP), y:PY-0.32, w:PW, h:0.28,
      fontSize:11, bold:true, color:CORAL, align:"center", fontFace:"Arial", margin:0
    });
  });

  // 어노테이션 — 폰 1 (서프라이즈)
  annotate(s, PX,        PY+0.9,  0.05, PY+0.72, "출발 당일\n목적지 공개");
  annotate(s, PX,        PY+2.0,  0.05, PY+1.85, "교통편 자동\n안내");

  // 어노테이션 — 폰 3 (디톡스)
  const p3x = PX+2*(PW+GAP);
  annotate(s, p3x+PW, PY+1.1,  p3x+PW+0.12, PY+0.92, "타 앱 자동 차단\n(iOS/Android)");
  annotate(s, p3x+PW, PY+2.2,  p3x+PW+0.12, PY+2.05, "여행 타이머\n실시간 표시");

  // 하단 설명 3열
  const descs=[
    {title:"서프라이즈", body:"목적지는 출발 당일 처음\n공개됩니다"},
    {title:"동선 안내", body:"지도·알림·타이머로\n현장 실시간 안내"},
    {title:"디지털 디톡스", body:"iOS Focus / Android DND\n로 타 앱 자동 차단"},
  ];
  descs.forEach((d,i)=>{
    const x=PX+i*(PW+GAP);
    s.addText(d.title, {x, y:PY+PH+0.15, w:PW, h:0.28, fontSize:11.5,bold:true,color:NAVY,align:"center",fontFace:"Arial",margin:0});
    s.addText(d.body,  {x, y:PY+PH+0.44, w:PW, h:0.45, fontSize:10,color:GREY,align:"center",fontFace:"Arial",margin:0});
  });
}

// ============================================================
// [12] PSST + 팀
// ============================================================
{
  const s = pres.addSlide();
  s.background = {color:WHITE};
  brand(s);

  s.addText("PSST 프레임워크", {
    x:0.45, y:0.35, w:9.1, h:0.48,
    fontSize:28, bold:true, color:NAVY, fontFace:"Arial", margin:0
  });
  s.addShape(pres.shapes.RECTANGLE, {
    x:0.43, y:0.72, w:3.2, h:0.14,
    fill:{color:CORAL,transparency:55}, line:{color:CORAL,width:0}
  });

  const psst=[
    {tag:"P", title:"Problem",  body:"1인 여행객 결정 피로\n충남 관광 접근성 부족",   dark:true},
    {tag:"S", title:"Solution", body:"AI 완전자동·서프라이즈\n디지털 디톡스 일체형",  dark:true},
    {tag:"S", title:"Scale-up", body:"전국 확장 → 글로벌\nB2G 데이터 사업 연계",    dark:false},
    {tag:"T", title:"Team",     body:"AI·SW·기획 4인\n아산스마트팩토리마이스터고", dark:false},
  ];
  psst.forEach((p,i)=>{
    const x=0.42+i*2.38;
    s.addShape(pres.shapes.ROUNDED_RECTANGLE, {
      x, y:1.0, w:2.25, h:2.2,
      fill:{color:p.dark?SAGE_DARK:"F9FAFB"}, line:{color:p.dark?SAGE_DARK:LGREY,width:1}, rectRadius:0.1
    });
    s.addShape(pres.shapes.OVAL, {x:x+0.1,y:1.08,w:0.48,h:0.48, fill:{color:CORAL},line:{color:CORAL,width:0}});
    s.addText(p.tag,   {x:x+0.1,y:1.08,w:0.48,h:0.48, fontSize:14,bold:true,color:WHITE,align:"center",valign:"middle",fontFace:"Arial",margin:0});
    s.addText(p.title, {x:x+0.12,y:1.65,w:2.0,h:0.36, fontSize:14,bold:true,color:p.dark?WHITE:NAVY,fontFace:"Arial",margin:0});
    s.addText(p.body,  {x:x+0.12,y:2.04,w:2.0,h:0.82, fontSize:11,color:p.dark?CREAM:GREY,fontFace:"Arial",margin:0});
  });

  s.addShape(pres.shapes.LINE, {x:0.42,y:3.34,w:9.16,h:0, line:{color:LGREY,width:1}});

  const members=[
    {name:"김태훈", role:"대표 / AI·기획", leader:true},
    {name:"김재원", role:"개발 / 백엔드",  leader:false},
    {name:"김준서", role:"개발 / 프론트",  leader:false},
    {name:"이윤호", role:"디자인 / UX",   leader:false},
  ];
  members.forEach((m,i)=>{
    const x=0.42+i*2.38;
    s.addShape(pres.shapes.OVAL, {x:x+0.68,y:3.52,w:0.88,h:0.88, fill:{color:m.leader?CORAL:"F9FAFB"},line:{color:m.leader?CORAL:LGREY,width:1}});
    s.addText("👤", {x:x+0.68,y:3.52,w:0.88,h:0.88, fontSize:24,align:"center",valign:"middle",fontFace:"Arial",margin:0});
    s.addText(m.name, {x, y:4.5,  w:2.25, h:0.3, fontSize:13,bold:true,color:NAVY,align:"center",fontFace:"Arial",margin:0});
    s.addText(m.role, {x, y:4.82, w:2.25, h:0.25, fontSize:10,color:GREY,align:"center",fontFace:"Arial",margin:0});
    if(m.leader){
      s.addShape(pres.shapes.ROUNDED_RECTANGLE, {x:x+0.62,y:4.22,w:1.0,h:0.22, fill:{color:CORAL},line:{color:CORAL,width:0},rectRadius:0.04});
      s.addText("LEADER", {x:x+0.62,y:4.22,w:1.0,h:0.22, fontSize:7,bold:true,color:WHITE,align:"center",valign:"middle",charSpacing:1,fontFace:"Arial",margin:0});
    }
  });
  s.addText("지도교사: 김영우", {x:0.42,y:5.2,w:9.16,h:0.22, fontSize:10,color:LGREY,align:"center",fontFace:"Arial",margin:0});
}

// ============================================================
// [13] 마무리
// ============================================================
{
  const s = pres.addSlide();
  s.background = {color:WHITE};

  // 우측 CORAL 포인트
  s.addShape(pres.shapes.RECTANGLE, {
    x:9.82, y:0, w:0.18, h:5.625, fill:{color:CORAL}, line:{color:CORAL,width:0}
  });

  s.addImage({
    path:path.join(__dirname,"app_screens","icon.png"),
    x:0.5, y:0.75, w:1.88, h:1.88
  });

  s.addText("따라가기만 하세요.", {
    x:2.65, y:0.7, w:7.1, h:1.1,
    fontSize:55, bold:true, color:NAVY, fontFace:"Arial", margin:0, charSpacing:-1
  });
  // 형광펜
  s.addShape(pres.shapes.RECTANGLE, {
    x:2.63, y:1.65, w:5.65, h:0.14,
    fill:{color:CORAL,transparency:55}, line:{color:CORAL,width:0}
  });
  s.addText("나머지는 AI가 합니다.", {
    x:2.65, y:1.88, w:7.1, h:0.65,
    fontSize:28, color:CORAL, fontFace:"Arial", margin:0
  });

  s.addShape(pres.shapes.LINE, {x:0.5, y:3.05, w:9.3, h:0, line:{color:LGREY,width:1}});

  s.addText([
    {text:"따라와유  ", options:{color:NAVY,bold:true,fontSize:14}},
    {text:"·  AI 완전자동 서프라이즈 충남 여행 앱  ·  ", options:{color:GREY,fontSize:12}},
    {text:"Kims & Lee Team", options:{color:CORAL,fontSize:12}},
  ], {x:0.5, y:3.25, w:9.3, h:0.35, fontFace:"Arial", margin:0});
  s.addText("아산스마트팩토리마이스터고등학교  ·  김태훈(대표) 김재원 김준서 이윤호  ·  지도교사 김영우", {
    x:0.5, y:3.65, w:9.3, h:0.28, fontSize:11, color:GREY, fontFace:"Arial", margin:0
  });

  const closing=[
    {k:"Problem",  v:"여행 결정 피로"},
    {k:"Solution", v:"AI 완전 자동화"},
    {k:"Scale-up", v:"충남→전국→글로벌"},
    {k:"Team",     v:"4인 마이스터고"},
  ];
  closing.forEach((c,i)=>{
    const x=0.5+i*2.38;
    s.addText(c.k, {x, y:4.32, w:2.22, h:0.28, fontSize:10,bold:true,color:CORAL,charSpacing:1,fontFace:"Arial",margin:0});
    s.addText(c.v, {x, y:4.62, w:2.22, h:0.28, fontSize:12,color:NAVY,fontFace:"Arial",margin:0});
  });
}

// ── 출력 ──────────────────────────────────────────────────────
const OUTPUT = path.join(__dirname, "따라와유_본선_발표.pptx");
pres.writeFile({fileName:OUTPUT}).then(()=>{
  console.log(`✅ PPT 저장 완료: ${OUTPUT}`);
});
