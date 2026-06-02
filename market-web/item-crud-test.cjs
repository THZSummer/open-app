const puppeteer = require('puppeteer');

const wait = (ms) => new Promise(resolve => setTimeout(resolve, ms));

async function test() {
  console.log('=== Item Page CRUD Browser Test ===\n');
  
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
    console.log('1. Loading item page...');
    await page.goto('http://localhost:13000/market-web/lookup/item', { 
      waitUntil: 'networkidle2',
      timeout: 30000 
    });
    await wait(3000);
    
    let text = await page.evaluate(() => document.body.innerText);
    console.log('   - Page title/body contains "LookUp":', text.includes('LookUp') ? 'YES' : 'NO');
    console.log('   - Page title/body contains "项列表":', text.includes('项列表') ? 'YES' : 'NO');
    console.log('   - Page title/body contains "项编码":', text.includes('项编码') ? 'YES' : 'NO');
    console.log('   - Page title/body contains "ADMIN":', text.includes('ADMIN') ? 'YES' : 'NO');
    
    console.log('\n2. Finding buttons...');
    const allButtons = await page.evaluate(() => {
      const btns = Array.from(document.querySelectorAll('button'));
      return btns.map(b => b.textContent.trim()).filter(t => t.length > 0);
    });
    console.log('   - All buttons found:', JSON.stringify(allButtons.slice(0, 10)));
    
    console.log('\n3. Testing CREATE...');
    const addBtn = await page.evaluate(() => {
      const btns = Array.from(document.querySelectorAll('button'));
      return btns.find(b => b.textContent.includes('新增分类'));
    });
    if (addBtn) {
      console.log('   - Add button found, clicking...');
      await addBtn.click();
      await wait(1000);
      text = await page.evaluate(() => document.body.innerText);
      console.log('   - Add modal opened:', text.includes('详情') || text.includes('新增') ? 'YES' : 'NO');
    } else {
      console.log('   - Add button NOT found (checked for "新增分类")');
    }
    
    console.log('\n4. Testing READ...');
    text = await page.evaluate(() => document.body.innerText);
    console.log('   - Table has data:', (text.includes('ADMIN') || text.includes('USER')) ? 'YES' : 'NO');
    
    console.log('\n5. Testing UPDATE...');
    const editBtn = await page.evaluate(() => {
      const btns = Array.from(document.querySelectorAll('button'));
      return btns.find(b => b.textContent.includes('编辑'));
    });
    if (editBtn) {
      console.log('   - Edit button found, clicking...');
      await editBtn.click();
      await wait(1000);
      text = await page.evaluate(() => document.body.innerText);
      console.log('   - Edit panel opened:', text.includes('编辑') ? 'YES' : 'NO');
    } else {
      console.log('   - Edit button NOT found');
    }
    
    console.log('\n6. Testing DELETE...');
    const deleteBtn = await page.evaluate(() => {
      const btns = Array.from(document.querySelectorAll('button'));
      return btns.find(b => b.textContent.includes('删除'));
    });
    console.log('   - Delete button exists:', deleteBtn ? 'YES' : 'NO');
    
    await page.screenshot({ path: 'F:/workspace/open-app/item-page-test.png', fullPage: true });
    console.log('\n7. Screenshot saved to F:/workspace/open-app/item-page-test.png');
    
    console.log('\n=== Test Complete ===');
    
  } catch (error) {
    console.log('Test error:', error.message);
  } finally {
    console.log('\nBrowser is still open for your review...');
  }
}

test().catch(console.error);