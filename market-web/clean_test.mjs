import puppeteer from 'puppeteer';
import path from 'path';

const browser = await puppeteer.launch({ headless: true, args: ['--no-sandbox'] });
const page = await browser.newPage();
await page.setViewport({ width: 1400, height: 900 });
page.on('console', msg => console.log(`[CONSOLE] ${msg.text()}`));
page.on('pageerror', err => console.log(`[ERROR] ${err.message}`));

await page.goto('http://localhost:13036/market-web/lookup-classify', { waitUntil: 'networkidle2', timeout: 30000 });
await new Promise(r => setTimeout(r, 5000));

const content = await page.evaluate(() => document.body.innerText.substring(0, 400));
console.log('Page content:', content.substring(0, 200));

const [importBtn] = await page.$x("//button[contains(text(),'导入')]");
console.log('Import button:', importBtn ? 'Found' : 'Not found');

if (importBtn) {
  await importBtn.click();
  await new Promise(r => setTimeout(r, 2000));
  
  const fileInput = await page.$('input[type="file"]');
  console.log('File input:', fileInput ? 'Found' : 'Not found');
  if (fileInput) {
    await fileInput.uploadFile(path.resolve('./test99.xlsx'));
    await new Promise(r => setTimeout(r, 2000));
    
    const submitBtn = await page.$('.ant-modal .ant-btn-primary');
    console.log('Submit button:', submitBtn ? 'Found' : 'Not found');
    if (submitBtn) {
      console.log('Submitting import...');
      await submitBtn.click();
      await new Promise(r => setTimeout(r, 5000));
      
      const afterSubmit = await page.evaluate(() => document.body.innerText.substring(0, 500));
      console.log('After submit:', afterSubmit.substring(0, 200));
    }
  }
}

await browser.close();