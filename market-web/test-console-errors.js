import puppeteer from 'puppeteer';

(async () => {
  const browser = await puppeteer.launch({
    headless: 'new',
    args: ['--no-sandbox', '--disable-setuid-sandbox']
  });
  
  const page = await browser.newPage();
  
  const errors = [];
  const warnings = [];
  const logs = [];
  
  page.on('console', msg => {
    const type = msg.type();
    const text = msg.text();
    
    if (type === 'error') {
      errors.push(text);
      console.error('❌ Console Error:', text);
    } else if (type === 'warning') {
      warnings.push(text);
      console.warn('⚠️  Console Warning:', text);
    } else {
      logs.push(`${type}: ${text}`);
    }
  });
  
  page.on('pageerror', error => {
    errors.push(error.message);
    console.error('❌ Page Error:', error.message);
  });
  
  page.on('requestfailed', request => {
    const failure = request.failure();
    if (failure) {
      console.error('❌ Request Failed:', request.url(), failure.errorText);
    }
  });
  
  try {
    console.log('Navigating to http://localhost:13000/market-web/dictionary ...\n');
    
    await page.goto('http://localhost:13000/market-web/dictionary', {
      waitUntil: 'networkidle2',
      timeout: 30000
    });
    
    await new Promise(resolve => setTimeout(resolve, 5000));
    
    const title = await page.title();
    console.log('\n📄 Page Title:', title);
    
    const content = await page.content();
    const hasErrorBoundary = content.includes('Something went wrong') || content.includes('Error');
    
    console.log('\n' + '='.repeat(60));
    console.log('SUMMARY');
    console.log('='.repeat(60));
    console.log(`Console Errors: ${errors.length}`);
    console.log(`Console Warnings: ${warnings.length}`);
    console.log(`Page Error Detected: ${hasErrorBoundary}`);
    
    if (errors.length > 0) {
      console.log('\n' + '='.repeat(60));
      console.log('ERRORS FOUND:');
      console.log('='.repeat(60));
      errors.forEach((err, i) => {
        console.log(`\n${i + 1}. ${err}`);
      });
    }
    
    if (warnings.length > 0) {
      console.log('\n' + '='.repeat(60));
      console.log('WARNINGS:');
      console.log('='.repeat(60));
      warnings.forEach((warn, i) => {
        console.log(`\n${i + 1}. ${warn}`);
      });
    }
    
  } catch (error) {
    console.error('Failed to load page:', error.message);
  } finally {
    await browser.close();
  }
})();
