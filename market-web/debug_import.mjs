import puppeteer from 'puppeteer';
import path from 'path';

const browser = await puppeteer.launch({ headless: true, args: ['--no-sandbox'] });
const page = await browser.newPage();
page.on('console', msg => console.log(`[BROWSER] ${msg.text()}`));

await page.goto('http://localhost:13034/market-web/lookup-classify', { waitUntil: 'networkidle2', timeout: 30000 });
await new Promise(r => setTimeout(r, 3000));

await page.evaluate(() => {
  const btns = document.querySelectorAll('button');
  for (const btn of btns) {
    if (btn.textContent.includes('导入')) {
      btn.click();
      break;
    }
  }
});

await new Promise(r => setTimeout(r, 2000));

const modalInfo = await page.evaluate(() => {
  const modals = document.querySelectorAll('.ant-modal');
  const fileInputs = document.querySelectorAll('input[type="file"]');
  const submitBtns = document.querySelectorAll('.ant-modal .ant-btn-primary');
  return {
    modalCount: modals.length,
    fileInputCount: fileInputs.length,
    submitBtnCount: submitBtns.length,
    modalVisible: modals.length > 0 ? window.getComputedStyle(modals[0]).display : 'none'
  };
});
console.log('Modal info:', JSON.stringify(modalInfo));

if (modalInfo.fileInputCount > 0) {
  const fileInput = await page.$('input[type="file"]');
  await fileInput.uploadFile(path.resolve('./test.xlsx'));
  await new Promise(r => setTimeout(r, 2000));
  
  await page.evaluate(() => {
    const submitBtn = document.querySelector('.ant-modal .ant-btn-primary');
    if (submitBtn) submitBtn.click();
  });
  
  await new Promise(r => setTimeout(r, 5000));
}

await browser.close();
console.log('Test completed');
