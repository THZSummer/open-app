const puppeteer = require('puppeteer');

const delay = ms => new Promise(r => setTimeout(r, ms));

async function test() {
  console.log('=== Full Integration Test ===\n');
  
  const browser = await puppeteer.launch({
    headless: false,
    args: ['--no-sandbox', '--disable-setuid-sandbox']
  });
  
  const page = await browser.newPage();
  
  page.on('console', msg => {
    if (msg.type() === 'error') {
      console.log('ERROR:', msg.text().substring(0, 200));
    }
  });
  
  try {
    console.log('1. Testing Classify List Page...');
    await page.goto('http://localhost:13000/market-web/lookup/classify', { 
      waitUntil: 'networkidle2',
      timeout: 30000 
    });
    await delay(3000);
    
    let text = await page.evaluate(() => document.body.innerText);
    console.log('   - Page loaded:', text.includes('分类') ? 'YES' : 'NO');
    console.log('   - Has table data:', text.includes('USER_TYPE') || text.includes('ADMIN') ? 'YES' : 'NO');
    console.log('   - Has Import button:', text.includes('导入') ? 'YES' : 'NO');
    console.log('   - Has Export button:', text.includes('导出') ? 'YES' : 'NO');
    
    console.log('\n2. Testing Item Page...');
    const firstDataRow = await page.$('tbody tr');
    if (firstDataRow) {
      await firstDataRow.click();
      await delay(2000);
      
      text = await page.evaluate(() => document.body.innerText);
      console.log('   - Navigated to Item page:', text.includes('LookUp项列表') ? 'YES' : 'NO');
      console.log('   - Has classify info:', text.includes('分类编码') ? 'YES' : 'NO');
      console.log('   - Has Add button:', text.includes('新增') ? 'YES' : 'NO');
    }
    
    console.log('\n3. Testing Task Center...');
    await page.goto('http://localhost:13000/market-web/lookup/task', {
      waitUntil: 'networkidle2',
      timeout: 30000
    });
    await delay(3000);
    
    text = await page.evaluate(() => document.body.innerText);
    console.log('   - Page loaded:', text.includes('任务') || text.includes('Task') ? 'YES' : 'NO');
    console.log('   - Has task data:', text.includes('导入') || text.includes('导出') ? 'YES' : 'NO');
    
    console.log('\n=== All Tests Complete ===');
    
  } catch (error) {
    console.log('Test error:', error.message);
  } finally {
    console.log('\nBrowser is still open for your review...');
  }
}

test().catch(console.error);