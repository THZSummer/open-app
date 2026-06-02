import puppeteer from 'puppeteer';
import path from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

const browser = await puppeteer.launch({ headless: true, args: ['--no-sandbox'] });
const page = await browser.newPage();
await page.setViewport({ width: 1400, height: 900 });

const logs = [];
page.on('console', msg => logs.push(`[${msg.type()}] ${msg.text()}`));
page.on('pageerror', err => logs.push(`[PAGE ERROR] ${err.message}`));

try {
  await page.goto('http://localhost:13022/market-web/lookup-classify', { waitUntil: 'networkidle2', timeout: 30000 });
  await new Promise(r => setTimeout(r, 4000));

  const rowLink = await page.$('.ant-table-row a');
  if (rowLink) {
    await rowLink.click();
    await new Promise(r => setTimeout(r, 5000));
    console.log('Navigated to:', page.url());
  }

  const importBtn = await page.$('button');
  const allBtns = await page.$$('button');
  let importBtnFound = null;
  for (const btn of allBtns) {
    const text = await btn.evaluate(b => b.textContent?.trim() || '');
    if (text.includes('导入')) {
      importBtnFound = btn;
      break;
    }
  }
  
  if (importBtnFound) {
    console.log('Clicking import button...');
    await importBtnFound.click();
    await new Promise(r => setTimeout(r, 2000));
    
    const fileInput = await page.$('input[type="file"]');
    if (fileInput) {
      await fileInput.uploadFile(path.resolve(__dirname, './e2e_test.xlsx'));
      console.log('File uploaded');
      await new Promise(r => setTimeout(r, 2000));
      
      const confirmBtn = await page.$('.ant-modal .ant-btn-primary');
      if (confirmBtn) {
        await confirmBtn.click();
        console.log('Clicked confirm');
        await new Promise(r => setTimeout(r, 3000));
      }
    }
  } else {
    console.log('Import button not found');
    const btns = await page.$$eval('button', b => b.map(x => x.textContent?.trim()));
    console.log('All buttons:', btns);
  }
} catch (e) {
  console.log('Error:', e.message);
}

const errors = logs.filter(l => l.includes('[error]') || l.includes('[PAGE ERROR]'));
console.log('\n=== Console output ===');
logs.slice(0, 20).forEach(l => console.log(l));
console.log('\n=== Errors ===');
errors.forEach(e => console.log(e));

await browser.close();