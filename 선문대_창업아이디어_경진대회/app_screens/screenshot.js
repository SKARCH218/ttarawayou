// screenshot.js — 앱 스크린 캡처 (390×844px × 5장)
const puppeteer = require('puppeteer');
const path = require('path');
const fs = require('fs');

const HTML = 'file://' + path.resolve(__dirname, 'index.html');
const OUT  = __dirname;

(async () => {
  const browser = await puppeteer.launch({ headless: 'new' });
  const page = await browser.newPage();

  // Google Fonts 로드를 위해 넉넉하게 뷰포트 설정
  await page.setViewport({ width: 2400, height: 900, deviceScaleFactor: 2 });
  await page.goto(HTML, { waitUntil: 'networkidle0' });

  // 폰트 완전 로드 대기
  await page.evaluateHandle('document.fonts.ready');

  // 누끼: 라운드 코너·그림자 제거 → 딱 화면만
  await page.evaluate(() => {
    document.querySelectorAll('.screen').forEach(el => {
      el.style.borderRadius = '0';
      el.style.boxShadow = 'none';
    });
  });

  for (let i = 1; i <= 5; i++) {
    const el = await page.$(`#s${i}`);
    if (!el) { console.error(`#s${i} not found`); continue; }

    const out = path.join(OUT, `screen${i}.png`);
    await el.screenshot({ path: out });
    console.log(`✅ screen${i}.png saved`);
  }

  await browser.close();
  console.log('완료');
})();
