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

    console.log('Clicking "LookUp管理"...');
    await page.click('text=LookUp管理');
    await new Promise(r => setTimeout(r, 2000));

    console.log('\n=== Classification Page Content ===');
    let pageText = await page.$eval('body', el => el.innerText);
    console.log('Page shows:', pageText.includes('ces1') ? 'ces1' : 'other data');
    
    console.log('\nClicking "ces1" row...');
    await page.click('text=ces1');
    await new Promise(r => setTimeout(r, 2000));

    console.log('\n=== Item Page Content (after clicking ces1) ===');
    pageText = await page.$eval('body', el => el.innerText);
    console.log('Page text (first 3000 chars):\n', pageText.substring(0, 3000));
    
    console.log('\nURL:', page.url());
    
    console.log('\n=== Console Messages ===');
    consoleMessages.forEach(m => console.log(m));
    
  } catch (error) {
    console.error('Test failed:', error.message);
  } finally {
    await browser.close();
  }
}

runTest();