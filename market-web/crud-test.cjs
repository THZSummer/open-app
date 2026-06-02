const puppeteer = require('puppeteer');

async function test() {
  console.log('=== Item Page CRUD Test ===\n');
  
  const browser = await puppeteer.launch({
    headless: false,
    args: ['--no-sandbox', '--disable-setuid-sandbox']
  });
  
  const page = await browser.newPage();
  
  page.on('console', msg => console.log('BROWSER:', msg.type(), msg.text().substring(0, 200)));
  page.on('pageerror', err => console.log('PAGE ERROR:', err.message));
  
  const delay = ms => new Promise(resolve => setTimeout(resolve, ms));
  
  try {
    console.log('1. Going to classify page...');
    await page.goto('http://localhost:13000/market-web/lookup/classify', { 
      waitUntil: 'domcontentloaded',
      timeout: 30000 
    });
    
    console.log('   Waiting for table to load...');
    await page.waitForSelector('tbody tr', { timeout: 10000 });
    console.log('   Table found');
    
    await delay(2000);
    
    const firstRow = await page.$('tbody tr');
    if (!firstRow) {
      console.log('   ERROR: No rows found in table');
      return;
    }
    await firstRow.click();
    console.log('   - Clicked first row');
    
    await delay(3000);
    console.log('   - Waited for navigation');
    
    // Check URL
    const url = page.url();
    console.log('   Current URL:', url);
    
    // 2. READ - Check current items
    console.log('\n2. READ - Current page content:');
    const text = await page.evaluate(() => document.body.innerText);
    const hasAdmin = text.includes('ADMIN');
    const hasUserType = text.includes('用户类型');
    const hasItemTable = text.includes('项编码') || text.includes('项名称');
    console.log('   - Has ADMIN:', hasAdmin ? 'YES' : 'NO');
    console.log('   - Has 用户类型:', hasUserType ? 'YES' : 'NO');
    console.log('   - Has item table fields:', hasItemTable ? 'YES' : 'NO');
    
    // 3. CREATE - Find and click add button
    console.log('\n3. CREATE - Testing add...');
    const addBtn = await page.$('button');
    const buttons = await page.$$('button');
    console.log('   Total buttons found:', buttons.length);
    
    // Print button texts
    for (let i = 0; i < Math.min(buttons.length, 10); i++) {
      const btnText = await buttons[i].evaluate(el => el.textContent.trim());
      console.log('   Button', i, ':', btnText.substring(0, 50));
    }
    
    // Click add button (index 6: "+ 新增LookUp项")
    const addButton = buttons[6];
    if (addButton) {
      await addButton.click();
      console.log('   - Clicked 新增LookUp项 button');
      await delay(2000);
      
      const formText = await page.evaluate(() => document.body.innerText);
      const hasForm = formText.includes('项编码') || formText.includes('新增') || formText.includes('保存');
      console.log('   - Form appeared:', hasForm ? 'YES' : 'NO');
      
      // Try to fill form
      const inputs = await page.$$('input');
      console.log('   - Found', inputs.length, 'inputs');
      
      for (let i = 0; i < inputs.length; i++) {
        const placeholder = await inputs[i].evaluate(el => el.placeholder || el.name || '');
        const value = await inputs[i].evaluate(el => el.value);
        console.log('   Input', i, '- placeholder:', placeholder, ', value:', value);
      }
    } else {
      console.log('   - No 新增 button found');
    }
    
    // 4. Take screenshot
    await page.screenshot({ path: 'F:/workspace/open-app/crud-test.png', fullPage: true });
    console.log('\n4. Screenshot saved');
    
    console.log('\n=== Test Complete ===');
    
  } catch (error) {
    console.log('Test error:', error.message);
    await page.screenshot({ path: 'F:/workspace/open-app/crud-test-error.png', fullPage: true });
    console.log('Error screenshot saved');
  } finally {
    await delay(5000);
    await browser.close();
    console.log('Browser closed');
  }
}

test().catch(console.error);