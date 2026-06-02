import puppeteer from 'puppeteer';

const SCREENSHOTS_DIR = 'F:\\workspace\\open-app\\test_screenshots';
const BASE_URL = 'http://localhost:13000/market-web/';

async function runTest() {
  const browser = await puppeteer.launch({
    headless: true,
    args: ['--no-sandbox', '--disable-setuid-sandbox']
  });
  
  const page = await browser.newPage();
  await page.setViewport({ width: 1920, height: 1080 });
  
  const consoleErrors = [];
  
  page.on('console', msg => {
    if (msg.type() === 'error') {
      consoleErrors.push(msg.text());
    }
  });
  
  page.on('pageerror', err => {
    consoleErrors.push(`Page Error: ${err.message}`);
  });

  try {
    console.log('=== Step 1: Opening page ===');
    await page.goto(BASE_URL, { waitUntil: 'networkidle2', timeout: 30000 });
    console.log('Page loaded successfully');

    console.log('\n=== Step 2: Clicking "LookUp管理" ===');
    await page.click('text=LookUp管理');
    await new Promise(r => setTimeout(r, 2000));
    console.log('Clicked LookUp管理');

    console.log('\n=== Step 3: Screenshot lookup_classify.png ===');
    await page.screenshot({ path: `${SCREENSHOTS_DIR}\\lookup_classify.png`, fullPage: false });
    console.log('Screenshot saved');

    console.log('\n=== Step 4: Checking for classification data ===');
    const pageText = await page.$eval('body', el => el.innerText);
    const hasData = pageText.includes('共') && pageText.includes('条');
    const dataMatch = pageText.match(/共\s*(\d+)\s*条/);
    const recordCount = dataMatch ? dataMatch[1] : 0;
    console.log(`Classification data present: ${hasData} (${recordCount} records)`);

    console.log('\n=== Step 5: Clicking first data row with items ===');
    await page.click('text=LOOKUP_TEST');
    await new Promise(r => setTimeout(r, 2000));
    console.log('Clicked "LOOKUP_TEST"');

    console.log('\n=== Step 6: Screenshot lookup_item.png ===');
    await page.screenshot({ path: `${SCREENSHOTS_DIR}\\lookup_item.png`, fullPage: false });
    console.log('Screenshot saved');

    console.log('\n=== Step 7: Checking Item page data ===');
    const itemPageText = await page.$eval('body', el => el.innerText);
    const itemHasData = itemPageText.includes('暂无数据') === false && itemPageText.includes('共') && itemPageText.includes('条');
    const itemMatch = itemPageText.match(/共\s*(\d+)\s*条/);
    const itemCount = itemMatch ? itemMatch[1] : 0;
    console.log(`Item data present: ${itemHasData} (${itemCount} records)`);
    console.log(`Item page shows: ${itemPageText.includes('暂无数据') ? '暂无数据 (No data)' : 'Has data'}`);

    console.log('\n=== Step 8: Console Errors ===');
    if (consoleErrors.length > 0) {
      console.log('ERRORS found:');
      consoleErrors.forEach(err => console.log(`  - ${err}`));
    } else {
      console.log('No console errors detected');
    }

    console.log('\n=== Summary ===');
    console.log(`- Classification page screenshot: ${SCREENSHOTS_DIR}\\lookup_classify.png`);
    console.log(`- Classification page records: ${recordCount}`);
    console.log(`- Item page screenshot: ${SCREENSHOTS_DIR}\\lookup_item.png`);
    console.log(`- Item page records: ${itemCount}`);
    console.log(`- Console errors: ${consoleErrors.length}`);
    
  } catch (error) {
    console.error('Test failed:', error.message);
  } finally {
    await browser.close();
  }
}

runTest();