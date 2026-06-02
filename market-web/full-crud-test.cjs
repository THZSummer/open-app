const puppeteer = require('puppeteer');

const wait = (ms) => new Promise(resolve => setTimeout(resolve, ms));

async function test() {
  console.log('=== Full CRUD Browser Test ===\n');
  
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
    console.log('1. Navigating to classify page...');
    await page.goto('http://localhost:13000/market-web/lookup/classify', { 
      waitUntil: 'domcontentloaded',
      timeout: 10000 
    });
    console.log('   - Page loaded');
    await wait(2000);
    
    await page.evaluate(() => {
      const firstRow = document.querySelector('tbody tr');
      if (firstRow) firstRow.click();
    });
    console.log('   - Clicked first row');
    await wait(2000);
    
    console.log('\n2. Opening modal...');
    await page.evaluate(() => {
      const btn = Array.from(document.querySelectorAll('button')).find(b => b.textContent.includes('新增LookUp项'));
      if (btn) btn.click();
    });
    console.log('   - Clicked add button');
    
    await wait(2000);
    
    console.log('\n3. Filling form using keyboard input...');
    const code = 'CRUD_NEW_' + Date.now();
    
    const codeInputHandle = await page.evaluateHandle(() => {
      const inputs = document.querySelectorAll('input');
      return Array.from(inputs).find(i => i.placeholder.includes('项编码'));
    });
    
    if (codeInputHandle) {
      await codeInputHandle.click();
      await page.keyboard.type(code, { delay: 10 });
      console.log('   - Typed code');
    }
    await wait(500);
    
    const nameInputHandle = await page.evaluateHandle(() => {
      const inputs = document.querySelectorAll('input');
      return Array.from(inputs).find(i => i.placeholder.includes('项名称'));
    });
    
    if (nameInputHandle) {
      await nameInputHandle.click();
      await page.keyboard.type('CRUD测试项', { delay: 10 });
      console.log('   - Typed name');
    }
    await wait(500);
    
    const valInputHandle = await page.evaluateHandle(() => {
      const inputs = document.querySelectorAll('input');
      return Array.from(inputs).find(i => i.placeholder.includes('ADMIN'));
    });
    
    if (valInputHandle) {
      await valInputHandle.click();
      await page.keyboard.type('TEST_VALUE', { delay: 10 });
      console.log('   - Typed value');
    }
    await wait(500);
    
    const descInputHandle = await page.evaluateHandle(() => {
      const inputs = document.querySelectorAll('input');
      return Array.from(inputs).find(i => i.placeholder.includes('管理员'));
    });
    
    if (descInputHandle) {
      await descInputHandle.click();
      await page.keyboard.type('描述', { delay: 10 });
      console.log('   - Typed desc');
    }
    await wait(500);
    
    console.log('\n4. Trying to submit form...');
    
    const formSubmitted = await page.evaluate(() => {
      const form = document.querySelector('form');
      if (form) {
        form.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
        return 'form submit';
      }
      const btns = Array.from(document.querySelectorAll('button'));
      const saveBtn = btns.find(b => b.textContent.includes('保') && b.textContent.includes('存'));
      if (saveBtn) {
        saveBtn.dispatchEvent(new Event('click', { bubbles: true }));
        return 'button click';
      }
      return 'nothing';
    });
    console.log('   - Submitted via:', formSubmitted);
    
    await wait(3000);
    
    const modalVisible = await page.evaluate(() => {
      const modals = document.querySelectorAll('[class*="modal"], [class*="drawer"]');
      return Array.from(modals).some(m => window.getComputedStyle(m).display !== 'none');
    });
    console.log('   Modal visible after save:', modalVisible);
    
    const bodyText = await page.evaluate(() => document.body.innerText);
    console.log('   New item created:', bodyText.includes('CRUD测试项') ? 'YES' : 'NO');
    
    await page.screenshot({ path: 'F:/workspace/open-app/crud-result.png', fullPage: true });
    console.log('\n=== Screenshot saved ===');
    
  } catch (error) {
    console.log('Test error:', error.message);
  } finally {
    await wait(2000);
    await browser.close();
    console.log('Browser closed.');
  }
}

test().catch(console.error);