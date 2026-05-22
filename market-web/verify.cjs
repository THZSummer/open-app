const puppeteer = require('puppeteer');

async function test() {
  const browser = await puppeteer.launch({
    headless: true,
    args: ['--no-sandbox', '--disable-setuid-sandbox']
  });
  
  const page = await browser.newPage();
  
  page.on('console', msg => {
    if (msg.type() === 'error') {
      console.log('BROWSER ERROR:', msg.text().substring(0, 300));
    }
  });
  
  page.on('pageerror', err => console.log('PAGE ERROR:', err.message.substring(0, 300)));
  
  console.log('Loading classify page...');
  
  try {
    await page.goto('http://localhost:13000/market-web/lookup/classify', { 
      waitUntil: 'networkidle2',
      timeout: 15000 
    });
    
    await new Promise(r => setTimeout(r, 5000));
    
    const rootContent = await page.evaluate(() => {
      const root = document.getElementById('root');
      return root ? root.innerHTML.substring(0, 1000) : 'root not found';
    });
    
    console.log('\nRoot content preview:');
    console.log(rootContent);
    
    const pageText = await page.evaluate(() => document.body.innerText);
    const hasData = pageText.includes('USER_TYPE') || pageText.includes('分类') || pageText.includes('ADMIN');
    console.log('\nHas actual data:', hasData);
    console.log('Page text preview:', pageText.substring(0, 500));
  } finally {
    await browser.close();
  }
}

test().catch(console.error);
