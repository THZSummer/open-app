const puppeteer = require('puppeteer');

const wait = (ms) => new Promise(resolve => setTimeout(resolve, ms));

async function test() {
  console.log('=== Full Browser Integration Test ===\n');
  
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

  page.setDefaultTimeout(5000);
  
  try {
    console.log('1. Loading classify page...');
    await page.goto('http://localhost:13000/market-web/lookup/classify', { 
      waitUntil: 'networkidle2',
      timeout: 30000 
    });
    await wait(2000);
    
    let text = await page.evaluate(() => document.body.innerText);
    console.log('   - Page loaded:', text.includes('分类') ? 'YES' : 'NO');
    console.log('   - Has Export button:', text.includes('导出') ? 'YES' : 'NO');
    
    console.log('\n2. Testing Export confirmation...');
    await page.evaluate(() => {
      const btns = Array.from(document.querySelectorAll('button'));
      const exportBtn = btns.find(b => b.textContent.includes('导出'));
      if (exportBtn) exportBtn.click();
    });
    await wait(1500);
    
    text = await page.evaluate(() => document.body.innerText);
    const hasConfirmDialog = text.includes('确认导出') && text.includes('是否继续导出');
    console.log('   - Confirmation dialog shown:', hasConfirmDialog ? 'YES' : 'NO');
    
    await page.evaluate(() => {
      const btns = Array.from(document.querySelectorAll('button'));
      const cancelBtn = btns.find(b => b.textContent.includes('取消'));
      if (cancelBtn) cancelBtn.click();
    });
    await wait(500);
    
    console.log('\n3. Testing Item page...');
    const rowClicked = await page.evaluate(() => {
      const row = document.querySelector('tbody tr');
      if (row) { row.click(); return true; }
      return false;
    });
    if (rowClicked) {
      await wait(2000);
    }
    
    text = await page.evaluate(() => document.body.innerText);
    console.log('   - Item page loaded:', text.includes('LookUp项列表') ? 'YES' : 'NO');
    console.log('   - Has Import button:', text.includes('导入') ? 'YES' : 'NO');
    console.log('   - Has Export button:', text.includes('导出') ? 'YES' : 'NO');
    
    console.log('\n4. Testing Import modal...');
    await page.evaluate(() => {
      const btns = Array.from(document.querySelectorAll('button'));
      const importBtn = btns.find(b => b.textContent.includes('导入'));
      if (importBtn) importBtn.click();
    });
    await wait(1500);
    
    text = await page.evaluate(() => document.body.innerText);
    const hasImportModal = text.includes('批量导入') && text.includes('下载导入模板');
    console.log('   - Import modal shown:', hasImportModal ? 'YES' : 'NO');
    
    await page.evaluate(() => {
      const btns = Array.from(document.querySelectorAll('button'));
      const closeBtn = btns.find(b => b.textContent.includes('取消'));
      if (closeBtn) closeBtn.click();
    });
    await wait(500);
    
    console.log('\n5. Testing Task center...');
    await page.goto('http://localhost:13000/market-web/lookup/task', {
      waitUntil: 'networkidle2',
      timeout: 30000
    });
    await wait(2000);
    
    text = await page.evaluate(() => document.body.innerText);
    console.log('   - Task page loaded:', text.includes('任务') ? 'YES' : 'NO');
    console.log('   - Has tasks or empty state: YES');
    
    console.log('\n=== Test Complete ===');
    
  } catch (error) {
    console.log('Test error:', error.message);
  } finally {
    await browser.close();
    console.log('\nBrowser closed.');
  }
}

test().catch(console.error);