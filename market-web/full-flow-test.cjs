const puppeteer = require('puppeteer');

const delay = ms => new Promise(resolve => setTimeout(resolve, ms));

async function test() {
  console.log('=== Full Flow Browser Test ===\n');
  
  const browser = await puppeteer.launch({
    headless: false,
    args: ['--no-sandbox', '--disable-setuid-sandbox']
  });
  
  const page = await browser.newPage();
  
  page.on('console', msg => {
    if (msg.type() === 'error') {
      console.log('BROWSER ERROR:', msg.text().substring(0, 200));
    }
  });
  
  try {
    console.log('1. Loading classify page...');
    await page.goto('http://localhost:13000/market-web/lookup/classify', { 
      waitUntil: 'networkidle2',
      timeout: 30000 
    });
    await delay(3000);
    
    let text = await page.evaluate(() => document.body.innerText);
    console.log('   - Classify page loaded:', text.includes('分类') ? 'YES' : 'NO');
    
    console.log('\n2. Clicking first classify row...');
    const firstRow = await page.$('tbody tr');
    if (firstRow) {
      await firstRow.click();
      await delay(3000);
      
      const url = page.url();
      console.log('   - Current URL:', url);
      
      text = await page.evaluate(() => document.body.innerText);
      console.log('   - Item page loaded:', text.includes('LookUp项列表') ? 'YES' : 'NO');
      console.log('   - Has item data:', text.includes('项编码') || text.includes('ADMIN') ? 'YES' : 'NO');
    } else {
      console.log('   - No rows found');
    }
    
    console.log('\n3. Testing ADD button...');
    const addBtn = await page.$('button');
    if (addBtn) {
      const btnText = await addBtn.evaluate(el => el.textContent);
      console.log('   - Button found:', btnText);
    } else {
      console.log('   - No button found');
    }
    
    await page.screenshot({ path: 'F:/workspace/open-app/test-item-page.png', fullPage: true });
    console.log('\n4. Screenshot saved');
    
    console.log('\n=== Test Complete ===');
    
  } catch (error) {
    console.log('Test error:', error.message);
  } finally {
    await browser.close();
    console.log('Browser closed');
  }
}

test().catch(console.error);