const puppeteer = require('puppeteer');
const path = require('path');
const SCDIR = path.join(__dirname, 'app_screens');
(async () => {
  const browser = await puppeteer.launch({ headless: 'new' });
  const page = await browser.newPage();
  await page.setViewport({ width: 512, height: 512, deviceScaleFactor: 2 });
  await page.goto('file://' + path.join(SCDIR, 'icon.html'), { waitUntil: 'networkidle0' });
  await page.evaluateHandle('document.fonts.ready');
  const el = await page.$('#icon');
  await el.screenshot({ path: path.join(SCDIR, 'icon.png') });
  await browser.close();
  console.log('✅ icon.png saved');
})();
