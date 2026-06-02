import puppeteer from 'puppeteer';

(async () => {
  const browser = await puppeteer.launch({
    headless: 'new',
    args: ['--no-sandbox', '--disable-setuid-sandbox']
  });
  
  const page = await browser.newPage();
  
  // Collect console errors
  const errors = [];
  page.on('console', msg => {
    if (msg.type() === 'error') {
      errors.push(msg.text());
    }
  });
  
  // Collect page errors
  page.on('pageerror', error => {
    errors.push(error.message);
  });
  
  try {
    console.log('Visiting: http://localhost:13016/market-web/dictionary');
    await page.goto('http://localhost:13016/market-web/dictionary', {
      waitUntil: 'networkidle2',
      timeout: 30000
    });
    
    // Wait a bit for the page to fully render
    await new Promise(resolve => setTimeout(resolve, 3000));
    
    // Take screenshot
    await page.screenshot({ path: 'dictionary-screenshot.png', fullPage: true });
    console.log('Screenshot saved: dictionary-screenshot.png');
    
    // Check for dictionary table
    const tableExists = await page.evaluate(() => {
      const table = document.querySelector('.ant-table, table, [class*="table"]');
      if (!table) return { exists: false, message: 'No table found' };
      
      const rows = table.querySelectorAll('tr, .ant-table-row');
      const cells = table.querySelectorAll('td, .ant-table-cell');
      
      return {
        exists: true,
        rows: rows.length,
        cells: cells.length,
        message: `Table found with ${rows.length} rows and ${cells.length} cells`
      };
    });
    
    console.log('\n=== TABLE STATUS ===');
    console.log(JSON.stringify(tableExists, null, 2));
    
    // Check if there's any "no data" message
    const noDataMessage = await page.evaluate(() => {
      const emptyText = document.querySelector('.ant-empty-description, .ant-table-empty');
      return emptyText ? emptyText.textContent : null;
    });
    
    if (noDataMessage) {
      console.log('\nNo data message found:', noDataMessage);
    }
    
    // Report errors
    console.log('\n=== CONSOLE ERRORS ===');
    if (errors.length === 0) {
      console.log('No errors found!');
    } else {
      console.log(`Found ${errors.length} error(s):`);
      errors.forEach((err, i) => console.log(`${i + 1}. ${err}`));
    }
    
    // Final status
    console.log('\n=== FINAL STATUS ===');
    if (tableExists.exists && tableExists.rows > 1 && errors.length === 0) {
      console.log('✅ Page is WORKING - Table has data and no errors');
    } else if (tableExists.exists && tableExists.rows <= 1) {
      console.log('⚠️  Table exists but appears to be empty');
    } else {
      console.log('❌ Page has issues');
    }
    
  } catch (error) {
    console.error('Error during test:', error.message);
  } finally {
    await browser.close();
  }
})();
