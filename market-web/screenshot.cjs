const puppeteer = require('puppeteer');

async function test() {
  const browser = await puppeteer.launch({
    headless: false,
    args: ['--no-sandbox', '--disable-setuid-sandbox']
  });
  
  const page = await browser.newPage();
  
  try {
    // Navigate via classify page
    await page.goto('http://localhost:13000/market-web/lookup/classify', { waitUntil: 'networkidle2', timeout: 30000 });
    await new Promise(resolve => setTimeout(resolve, 3000));
    
    // Click first row
    await page.click('tbody tr');
    await new Promise(resolve => setTimeout(resolve, 3000));
    
    // Take screenshot
    await page.screenshot({ path: 'F:/workspace/open-app/item-page-style.png', fullPage: true });
    console.log('Screenshot saved to F:/workspace/open-app/item-page-style.png');
    
  } catch (error) {
    console.log('Error:', error.message);
  } finally {
    await browser.close();
  }
}

test().catch(console.error);
