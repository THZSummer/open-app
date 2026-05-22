const puppeteer = require('puppeteer');

async function test() {
  console.log('Starting browser test...');
  
  const browser = await puppeteer.launch({
    headless: true,
    args: ['--no-sandbox', '--disable-setuid-sandbox']
  });
  
  const page = await browser.newPage();
  
  // Capture console messages
  page.on('console', msg => {
    if (msg.type() === 'error') {
      console.log('Console ERROR:', msg.text());
    }
  });
  
  page.on('pageerror', err => {
    console.log('Page ERROR:', err.message);
  });
  
  try {
    console.log('Navigating to http://localhost:13000/market-web/...');
    await page.goto('http://localhost:13000/market-web/', { 
      waitUntil: 'networkidle0',
      timeout: 30000 
    });
    
    console.log('Page loaded. Title:', await page.title());
    
    // Wait for React to render
    await new Promise(resolve => setTimeout(resolve, 3000));
    
    // Check if main content exists
    const content = await page.content();
    console.log('Page has content length:', content.length);
    
    // Check for error indicators
    const hasError = content.includes('error') || content.includes('Error');
    console.log('Contains error text:', hasError);
    
    // Try to navigate to classify page
    console.log('Navigating to classify page...');
    await page.goto('http://localhost:13000/market-web/lookup/classify', {
      waitUntil: 'networkidle0',
      timeout: 30000
    });
    
    await new Promise(resolve => setTimeout(resolve, 3000));
    
    const classifyContent = await page.content();
    console.log('Classify page loaded, content length:', classifyContent.length);
    
    // Check for table or data
    const hasTable = classifyContent.includes('<table') || classifyContent.includes('table');
    console.log('Has table:', hasTable);
    
    // Check for any React error boundary
    const hasErrorBoundary = classifyContent.includes('Something went wrong') || 
                            classifyContent.includes('Error:') ||
                            classifyContent.includes('errorboundary');
    console.log('Has error boundary:', hasErrorBoundary);
    
    console.log('\n✅ Browser test completed');
    
  } catch (error) {
    console.log('Test failed:', error.message);
  } finally {
    await browser.close();
  }
}

test();
