const puppeteer = require('puppeteer');

async function test() {
  console.log('=== Detailed Browser Test ===\n');
  
  const browser = await puppeteer.launch({
    headless: true,
    args: ['--no-sandbox', '--disable-setuid-sandbox']
  });
  
  const page = await browser.newPage();
  
  // Capture all console messages
  const consoleLogs = [];
  page.on('console', msg => {
    consoleLogs.push({ type: msg.type(), text: msg.text() });
  });
  
  page.on('pageerror', err => {
    console.log('PAGE ERROR:', err.message);
  });
  
  // Capture network requests
  const apiRequests = [];
  page.on('request', req => {
    if (req.url().includes('/api/')) {
      apiRequests.push(req.url());
    }
  });
  
  page.on('response', res => {
    if (res.url().includes('/api/')) {
      console.log('API Response:', res.status(), res.url().substring(0, 80));
    }
  });
  
  try {
    // 1. Test classify page
    console.log('1. Loading classify page...');
    await page.goto('http://localhost:13000/market-web/lookup/classify', { 
      waitUntil: 'networkidle2',
      timeout: 30000 
    });
    
    await new Promise(resolve => setTimeout(resolve, 5000));
    
    // Check for data in the page
    const pageText = await page.evaluate(() => document.body.innerText);
    
    // Debug: Show first 500 chars of page text
    console.log('\n   Page text preview:', pageText.substring(0, 500).replace(/\n/g, ' '));
    
    // Debug: Check React root
    const rootContent = await page.evaluate(() => {
      const root = document.getElementById('root');
      return root ? root.innerHTML.substring(0, 300) : 'NO ROOT ELEMENT';
    });
    console.log('   Root content:', rootContent);
    
    // Check for visible elements
    const bodyHtml = await page.evaluate(() => document.body.innerHTML.substring(0, 500));
    console.log('   Body HTML:', bodyHtml);
    
    // Check if we have actual data
    const hasUSER_TYPE = pageText.includes('USER_TYPE') || pageText.includes('用户类型');
    const hasClassifyList = pageText.includes('分类') || pageText.includes('Classify');
    const hasButtons = pageText.includes('导入') || pageText.includes('导出') || pageText.includes('新增');
    
    console.log('\n2. Page Content Check:');
    console.log('   - Has USER_TYPE/data:', hasUSER_TYPE);
    console.log('   - Has classify text:', hasClassifyList);
    console.log('   - Has action buttons:', hasButtons);
    
    // Count table rows
    const rowCount = await page.evaluate(() => {
      const rows = document.querySelectorAll('tbody tr');
      return rows.length;
    });
    console.log('   - Table rows:', rowCount);
    
    // 3. Check for API calls made
    console.log('\n3. API Requests made:', apiRequests.length);
    apiRequests.forEach(url => console.log('   -', url.substring(0, 100)));
    
    // 4. Check console errors (only actual errors, not warnings)
    const errors = consoleLogs.filter(l => l.type === 'error');
    console.log('\n4. Console Errors:', errors.length);
    errors.forEach(e => console.log('   -', e.text.substring(0, 200)));
    
    // 5. Try to click a button
    console.log('\n5. Testing button click...');
    const importBtn = await page.evaluate(() => {
      const btns = Array.from(document.querySelectorAll('button'));
      return btns.find(b => b.textContent.includes('导入'));
    });
    if (importBtn) {
      console.log('   - Import button found');
      await importBtn.click();
      await new Promise(resolve => setTimeout(resolve, 1000));
      
      // Check if modal opened
      const modalText = await page.evaluate(() => document.body.innerText);
      const hasModal = modalText.includes('选择文件') || modalText.includes('上传');
      console.log('   - Modal opened:', hasModal);
    } else {
      console.log('   - Import button NOT found');
    }
    
    console.log('\n=== Test Complete ===');
    
  } catch (error) {
    console.log('Test failed:', error.message);
  } finally {
    await browser.close();
  }
}

test();