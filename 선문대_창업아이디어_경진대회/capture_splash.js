const puppeteer = require('puppeteer');
const path = require('path');
const SCDIR = path.join(__dirname, 'app_screens');
(async () => {
  const browser = await puppeteer.launch({ headless: 'new' });
  const page = await browser.newPage();
  await page.setViewport({ width: 480, height: 920, deviceScaleFactor: 2 });
  await page.goto('file://' + path.join(SCDIR, 'splash.html'), { waitUntil: 'networkidle0' });
  await page.evaluateHandle('document.fonts.ready');
  const el = await page.$('.screen');
  await el.screenshot({ path: path.join(SCDIR, 'splash.png'), omitBackground: true });
  await browser.close();
  console.log('✅ splash.png saved');
})();
