import puppeteer from 'puppeteer';

const SCREENSHOTS_DIR = 'F:\\workspace\\open-app\\test_screenshots';
const BASE_URL = 'http://localhost:13000/market-web/';

async function runTest() {
  const browser = await puppeteer.launch({
    headless: true,
    args: ['--no-sandbox', '--disable-setuid-sandbox']
  });
  
  const page = await browser.newPage();
  await page.setViewport({ width: 1920, height: 1080 });
  
  const consoleErrors = [];
  const networkErrors = [];
  
  page.on('console', msg => {
    if (msg.type() === 'error') {
      consoleErrors.push(msg.text());
    }
  });
  
  page.on('pageerror', err => {
    consoleErrors.push(`Page Error: ${err.message}`);
  });

  page.on('response', response => {
    if (response.status() >= 400) {
      networkErrors.push(`${response.status()} ${response.url()}`);
    }
  });

  try {
    console.log('=== Step 1: Opening page ===');
    await page.goto(BASE_URL, { waitUntil: 'networkidle2', timeout: 30000 });
    console.log('Page loaded successfully');

    console.log('\n=== Step 2: Clicking "LookUp管理" ===');
    await page.evaluate(() => {
      const links = Array.from(document.querySelectorAll('*'));
      const link = links.find(el => el.textContent.trim() === 'LookUp管理');
      if (link) link.click();
    });
    await new Promise(r => setTimeout(r, 2000));
    console.log('Clicked LookUp管理');

    console.log('\n=== Step 3: Clicking on classify code to enter item list ===');
    await page.evaluate(() => {
      const cells = Array.from(document.querySelectorAll('table tbody td'));
      const codeCell = cells.find(c => c.textContent === 'TEST_NEW_001');
      if (codeCell) codeCell.click();
    });
    await new Promise(r => setTimeout(r, 3000));
    console.log('Navigated to item list page');

    await page.screenshot({ path: `${SCREENSHOTS_DIR}\\step1_item_list.png`, fullPage: false });

    const pageText = await page.evaluate(() => document.body.innerText);
    console.log('On item list page:', pageText.includes('LookUp项列表'));
    console.log('Current items:', pageText.match(/共\s*(\d+)\s*条/)?.[1] || '0');

    console.log('\n=== Step 4: Clicking "新增LookUp项" button ===');
    await page.evaluate(() => {
      const buttons = Array.from(document.querySelectorAll('button'));
      const btn = buttons.find(el => el.textContent.includes('新增') && el.textContent.includes('LookUp'));
      if (btn) btn.click();
    });
    await new Promise(r => setTimeout(r, 3000));
    console.log('Clicked add button');

    await page.screenshot({ path: `${SCREENSHOTS_DIR}\\step2_modal.png`, fullPage: false });

    console.log('\n=== Step 5: Checking modal ===');
    const modalExists = await page.evaluate(() => {
      const modals = document.querySelectorAll('[class*="modal"], [role="dialog"]');
      return modals.length > 0;
    });
    console.log('Modal exists:', modalExists);

    const modalContent = await page.evaluate(() => {
      const modal = document.querySelector('[class*="ant-modal"]');
      return modal ? modal.textContent?.substring(0, 300) : 'No modal';
    });
    console.log('Modal content:', modalContent);

    const inputs = await page.evaluate(() => {
      const inputs = Array.from(document.querySelectorAll('input'));
      return inputs.filter(i => i.offsetParent !== null).map((inp, i) => ({
        index: i,
        type: inp.type,
        placeholder: inp.placeholder,
        id: inp.id || inp.name
      }));
    });
    console.log('Visible inputs:', JSON.stringify(inputs, null, 2));

    console.log('\n=== Step 6: Filling form ===');
    const values = ['TEST_EXT_001', '测试扩展', 'test_value', 'attr1_value', 'attr2_value'];
    
    await page.evaluate((vals) => {
      const inputs = Array.from(document.querySelectorAll('input'));
      const visibleInputs = inputs.filter(i => i.offsetParent !== null && i.type === 'text' && !i.disabled);
      
      visibleInputs.forEach((inp, idx) => {
        if (idx < vals.length) {
          inp.focus();
          inp.value = vals[idx];
          inp.dispatchEvent(new Event('input', { bubbles: true }));
          inp.dispatchEvent(new Event('change', { bubbles: true }));
        }
      });
    }, values);
    console.log('Form filled');

    await new Promise(r => setTimeout(r, 1000));
    await page.screenshot({ path: `${SCREENSHOTS_DIR}\\step3_form_filled.png`, fullPage: false });

    console.log('\n=== Step 7: Clicking save button ===');
    await page.evaluate(() => {
      const buttons = Array.from(document.querySelectorAll('button'));
      const saveBtn = buttons.find(el => el.textContent.trim() === '保存');
      if (saveBtn) saveBtn.click();
    });
    await new Promise(r => setTimeout(r, 3000));
    console.log('Clicked save');

    console.log('\n=== Step 8: Screenshot and result ===');
    await page.screenshot({ path: `${SCREENSHOTS_DIR}\\item_save_result.png`, fullPage: false });

    const finalPageText = await page.evaluate(() => document.body.innerText);
    const success = finalPageText.includes('成功') && !finalPageText.includes('失败');
    const hasError = finalPageText.includes('失败') || finalPageText.includes('Error');
    
    console.log(`\n=== RESULT ===`);
    console.log(`Save result: ${success ? 'SUCCESS' : (hasError ? 'FAILED' : 'UNKNOWN')}`);
    
    if (networkErrors.length > 0) {
      console.log('\nNetwork Errors:');
      networkErrors.forEach(err => console.log(`  - ${err}`));
    }
    
    if (consoleErrors.length > 0) {
      console.log('\nConsole Errors:');
      consoleErrors.forEach(err => console.log(`  - ${err}`));
    }
    
    console.log(`\nScreenshot: ${SCREENSHOTS_DIR}\\item_save_result.png`);
    
  } catch (error) {
    console.error('Test failed:', error.message);
    await page.screenshot({ path: `${SCREENSHOTS_DIR}\\item_save_error.png`, fullPage: false });
  } finally {
    await browser.close();
  }
}

runTest();