import puppeteer from 'puppeteer';

const BASE_URL = 'http://localhost:13000/market-web/';

async function runTest() {
  const browser = await puppeteer.launch({
    headless: true,
    args: ['--no-sandbox', '--disable-setuid-sandbox']
  });
  
  const page = await browser.newPage();
  await page.setViewport({ width: 1920, height: 1080 });
  
  const consoleMessages = [];
  
  page.on('console', msg => {
    consoleMessages.push(`[${msg.type()}] ${msg.text()}`);
  });

  try {
    console.log('Opening page...');
    await page.goto(BASE_URL, { waitUntil: 'networkidle2', timeout: 30000 });

    console.log('\nClicking "LookUp管理"...');
    await page.click('text=LookUp管理');
    await new Promise(r => setTimeout(r, 2000));

    console.log('\n=== Page Content Analysis ===');
    
    const bodyText = await page.$eval('body', el => el.innerText);
    console.log('Page text (first 2000 chars):\n', bodyText.substring(0, 2000));
    
    const antTables = await page.$$('.ant-table');
    console.log('\n.ant-table elements found:', antTables.length);
    
    const antTableBodies = await page.$$('.ant-table-tbody');
    console.log('.ant-table-tbody elements found:', antTableBodies.length);
    
    const tableRows = await page.$$('.ant-table-tbody tr');
    console.log('.ant-table-tbody tr rows found:', tableRows.length);
    
    const antEmptyText = await page.$$('.ant-empty-text');
    console.log('.ant-empty-text elements found:', antEmptyText.length);
    
    const antSpin = await page.$$('.ant-spin');
    console.log('.ant-spin elements found:', antSpin.length);
    
    const antTableWrapper = await page.$$('.ant-table-wrapper');
    console.log('.ant-table-wrapper elements found:', antTableWrapper.length);

    console.log('\n=== Console Messages ===');
    consoleMessages.forEach(m => console.log(m));
    
  } catch (error) {
    console.error('Test failed:', error.message);
  } finally {
    await browser.close();
  }
}

runTest();