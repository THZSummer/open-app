const puppeteer = require('puppeteer');

(async () => {
  const browser = await puppeteer.launch({
    headless: 'new',
    devtools: true,
    args: ['--no-sandbox', '--disable-setuid-sandbox', '--disable-gpu']
  });
  
  const page = await browser.newPage();
  
  const consoleMessages = [];
  const consoleErrors = [];
  
  page.on('console', msg => {
    const text = msg.text();
    consoleMessages.push(`[${msg.type()}] ${text}`);
    if (msg.type() === 'error') {
      consoleErrors.push(text);
    }
  });
  
  page.on('pageerror', err => {
    consoleErrors.push(`PageError: ${err.message}`);
  });

  try {
    console.log('Opening http://localhost:13000/market-web/');
    await page.goto('http://localhost:13000/market-web/', { waitUntil: 'domcontentloaded', timeout: 30000 });
    
    await new Promise(resolve => setTimeout(resolve, 3000));
    
    console.log('Taking initial screenshot...');
    await page.screenshot({ path: 'F:/workspace/open-app/test_screenshots/initial_page.png', fullPage: true });
    
    const html = await page.content();
    console.log('Page HTML length:', html.length);
    
    console.log('Looking for "LookUp管理" menu...');
    const lookupMenu = await page.$('text=LookUp管理');
    if (lookupMenu) {
      await lookupMenu.click();
      console.log('Clicked on LookUp管理');
      await new Promise(resolve => setTimeout(resolve, 2000));
    } else {
      console.log('Looking for menu in page content...');
      const menuHtml = await page.$eval('body', el => el.innerHTML.substring(0, 2000));
      console.log('Body content preview:', menuHtml);
    }
    
    console.log('Taking screenshot after menu click...');
    await page.screenshot({ path: 'F:/workspace/open-app/test_screenshots/after_menu_click.png', fullPage: true });
    
    console.log('Looking for table rows...');
    const rows = await page.$$('tr');
    console.log(`Found ${rows.length} tr elements`);
    
    const tableRows = await page.$$('table tbody tr');
    console.log(`Found ${tableRows.length} table tbody tr elements`);
    
    if (tableRows.length > 0) {
      await tableRows[0].click();
      console.log('Clicked on first table row');
      await new Promise(resolve => setTimeout(resolve, 3000));
    }
    
    const screenshotPath = 'F:/workspace/open-app/test_screenshots/item_page_issue.png';
    await page.screenshot({ path: screenshotPath, fullPage: true });
    console.log(`Screenshot saved to ${screenshotPath}`);
    
    const title = await page.title();
    console.log(`Page title: ${title}`);
    
    const url = page.url();
    console.log(`Current URL: ${url}`);
    
    const bodyText = await page.$eval('body', el => el.innerText.substring(0, 1000));
    console.log(`Page content preview:\n${bodyText}`);
    
    console.log('\n=== Console Messages ===');
    consoleMessages.forEach(msg => console.log(msg));
    
    console.log('\n=== Console Errors ===');
    if (consoleErrors.length === 0) {
      console.log('No console errors found');
    } else {
      consoleErrors.forEach(err => console.log(`ERROR: ${err}`));
    }
    
  } catch (err) {
    console.error('Error:', err.message);
    await page.screenshot({ path: 'F:/workspace/open-app/test_screenshots/error_state.png', fullPage: true });
  } finally {
    await browser.close();
    console.log('Browser closed');
  }
})();
