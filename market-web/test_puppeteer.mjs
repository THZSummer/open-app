import puppeteer from 'puppeteer';

async function runTest() {
  const browser = await puppeteer.launch({
    headless: 'new',
    args: ['--no-sandbox', '--disable-setuid-sandbox']
  });
  
  const page = await browser.newPage();
  const consoleErrors = [];
  const failedRequests = [];
  
  page.on('console', msg => {
    if (msg.type() === 'error') {
      consoleErrors.push(msg.text());
    }
  });
  
  page.on('pageerror', err => {
    consoleErrors.push(`Page Error: ${err.message}`);
  });
  
  page.on('requestfailed', request => {
    failedRequests.push(`${request.url()} - ${request.failure().errorText}`);
  });
  
  page.on('response', response => {
    if (response.status() >= 400) {
      failedRequests.push(`${response.url()} - ${response.status()}`);
    }
  });
  
  const results = {
    categoryPageLoaded: false,
    firstRowClicked: false,
    itemPageLoaded: false,
    consoleErrors: [],
    failedRequests: [],
    error: null
  };
  
  try {
    console.log('Navigating to http://localhost:13000/market-web/...');
    await page.goto('http://localhost:13000/market-web/', { 
      waitUntil: 'networkidle2',
      timeout: 30000 
    });
    
    await new Promise(r => setTimeout(r, 2000));
    
    console.log('Taking screenshot of home page...');
    await page.screenshot({ 
      path: 'item_page_1.png',
      fullPage: true 
    });
    results.categoryPageLoaded = true;
    
    console.log('Page title:', await page.title());
    
    const menuItems = await page.$$('.ant-menu-item');
    console.log('Menu items found:', menuItems.length);
    
    for (let i = 0; i < menuItems.length; i++) {
      const text = await menuItems[i].evaluate(el => el.innerText);
      console.log(`  Menu ${i}: ${text}`);
    }
    
    const categoryMenu = await page.$('text=分类管理');
    if (categoryMenu) {
      console.log('\nClicking "分类管理" menu...');
      await categoryMenu.click();
      await new Promise(r => setTimeout(r, 3000));
      
      console.log('Taking screenshot of category page...');
      await page.screenshot({ 
        path: 'item_page_1.png',
        fullPage: true 
      });
      
      const antTableRows = await page.$$('.ant-table-row');
      console.log('Ant table rows found:', antTableRows.length);
      
      if (antTableRows.length > 0) {
        console.log('\nClicking first row...');
        await antTableRows[0].click();
        await new Promise(r => setTimeout(r, 3000));
        results.firstRowClicked = true;
        
        console.log('Taking screenshot of item page...');
        await page.screenshot({ 
          path: 'item_page_2.png',
          fullPage: true 
        });
        results.itemPageLoaded = true;
      } else {
        console.log('No rows found in category table');
      }
    } else {
      console.log('分类管理 menu not found');
    }
    
  } catch (err) {
    results.error = err.message;
  }
  
  results.consoleErrors = consoleErrors;
  results.failedRequests = failedRequests;
  
  await browser.close();
  
  console.log('\n========== TEST RESULTS ==========');
  console.log(`Category page loaded: ${results.categoryPageLoaded}`);
  console.log(`First row clicked: ${results.firstRowClicked}`);
  console.log(`Item page loaded: ${results.itemPageLoaded}`);
  console.log(`\nConsole errors (${results.consoleErrors.length}):`);
  results.consoleErrors.forEach((err, i) => console.log(`  ${i + 1}. ${err}`));
  console.log(`\nFailed requests (${results.failedRequests.length}):`);
  results.failedRequests.forEach((req, i) => console.log(`  ${i + 1}. ${req}`));
  if (results.error) {
    console.log(`\nError: ${results.error}`);
  }
  console.log('==================================');
  
  return results;
}

runTest();