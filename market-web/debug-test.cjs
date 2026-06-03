const puppeteer = require('puppeteer');

async function test() {
  const browser = await puppeteer.launch({
    headless: true,
    args: ['--no-sandbox', '--disable-setuid-sandbox']
  });
  
  const page = await browser.newPage();
  
  page.on('console', msg => console.log('BROWSER:', msg.type(), msg.text()));
  page.on('pageerror', err => console.log('PAGE ERROR:', err.message));
  
  console.log('Loading page...');
  
  await page.goto('http://localhost:13000/market-web/', { 
    waitUntil: 'networkidle2',
    timeout: 30000 
  });
  
  await page.waitForTimeout(5000);
  
  const rootContent = await page.evaluate(() => {
    const root = document.getElementById('root');
    return root ? root.innerHTML.substring(0, 500) : 'root not found';
  });
  
  console.log('Root content:', rootContent);
  
  await browser.close();
}

test();
