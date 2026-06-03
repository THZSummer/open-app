const puppeteer = require('puppeteer');

const wait = (ms) => new Promise(resolve => setTimeout(resolve, ms));

async function test() {
  console.log('=== Browser Test ===\n');
  
  const browser = await puppeteer.launch({
    headless: false,
    args: ['--no-sandbox', '--disable-setuid-sandbox']
  });
  
  const page = await browser.newPage();
  
  page.on('console', msg => console.log('CONSOLE:', msg.type(), msg.text().substring(0, 80)));
  page.on('pageerror', err => console.log('PAGE ERROR:', err.message));
  
  try {
    console.log('1. Loading classify page...');
    await page.goto('http://localhost:13000/market-web/lookup/classify', { waitUntil: 'domcontentloaded', timeout: 15000 });
    console.log('   Page loaded');
    
    await wait(2000);
    
    console.log('2. Looking for table rows...');
    const rows = await page.$$('tbody tr');
    console.log('   Found rows:', rows.length);
    
    if (rows.length > 0) {
      console.log('3. Clicking first row...');
      await rows[0].click();
      await wait(2000);
      console.log('   Clicked');
    }
    
    console.log('4. Taking screenshot...');
    await page.screenshot({ path: 'F:/workspace/open-app/test-result.png', fullPage: true });
    console.log('   Saved to test-result.png');
    
    const text = await page.evaluate(() => document.body.innerText);
    console.log('\nPage contains "项编码":', text.includes('项编码'));
    console.log('Page contains "ADMIN":', text.includes('ADMIN'));
    
    console.log('\n=== Done ===');
  } catch (error) {
    console.log('Error:', error.message);
  } finally {
    await browser.close();
  }
}

test().catch(console.error);