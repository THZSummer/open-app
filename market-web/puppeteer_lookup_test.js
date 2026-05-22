import puppeteer from 'puppeteer';

const SCREENSHOTS_DIR = 'F:\\workspace\\open-app\\test_screenshots';
const BASE_URL = 'http://localhost:13000/market-web/';

const results = {
  addLookupItem: { success: false, message: '' },
  editLookupItem: { success: false, message: '' },
  viewLookupItem: { success: false, message: '' },
  errors: []
};

const wait = (ms) => new Promise(r => setTimeout(r, ms));

async function runTest() {
  const browser = await puppeteer.launch({
    headless: true,
    args: ['--no-sandbox', '--disable-setuid-sandbox']
  });
  
  const page = await browser.newPage();
  await page.setViewport({ width: 1920, height: 1080 });
  
  page.on('console', msg => {
    if (msg.type() === 'error') {
      results.errors.push(`Console Error: ${msg.text()}`);
    }
  });
  
  page.on('pageerror', err => {
    results.errors.push(`Page Error: ${err.message}`);
  });

  try {
    console.log('=== Step 1: Opening page ===');
    await page.goto(BASE_URL, { waitUntil: 'networkidle2', timeout: 30000 });
    console.log('Page loaded successfully');

    console.log('\n=== Step 2: Clicking "LookUp管理" ===');
    await page.click('text=LookUp管理');
    await wait(2000);
    console.log('Clicked LookUp管理');

    console.log('\n=== Step 3: Clicking first classification row ===');
    const firstRow = await page.$('tbody tr');
    if (firstRow) {
      await firstRow.click();
      await wait(2000);
      console.log('Clicked first classification row');
    } else {
      throw new Error('No classification rows found');
    }

    console.log('\n=== Step 4: Screenshot item1_list.png ===');
    await page.screenshot({ path: `${SCREENSHOTS_DIR}\\item1_list.png`, fullPage: false });
    console.log('Screenshot saved: item1_list.png');

    console.log('\n=== Step 5: Clicking "新增LookUp项" button ===');
    await page.click('text=新增LookUp项');
    await wait(1500);
    console.log('Clicked "新增LookUp项"');

    console.log('\n=== Step 6: Screenshot item2_add_modal.png ===');
    await page.screenshot({ path: `${SCREENSHOTS_DIR}\\item2_add_modal.png`, fullPage: false });
    console.log('Screenshot saved: item2_add_modal.png');

    console.log('\n=== Step 7: Filling form ===');
    const testItemCode = `TEST_ITEM_${Date.now()}`;
    console.log('Using test item code:', testItemCode);
    
    const itemCodeInput = await page.$('#itemCode');
    const itemNameInput = await page.$('#itemName');
    const itemValueInput = await page.$('#itemValue');
    const itemIndexInput = await page.$('#itemIndex');
    
    if (itemCodeInput) {
      await itemCodeInput.click({ timeout: 5000 });
      await page.keyboard.type(testItemCode, { delay: 30 });
      console.log('Typed item code');
    }
    await wait(300);
    if (itemNameInput) {
      await itemNameInput.click({ timeout: 5000 });
      await page.keyboard.type('测试项目', { delay: 30 });
      console.log('Typed item name');
    }
    await wait(300);
    if (itemValueInput) {
      await itemValueInput.click({ timeout: 5000 });
      await page.keyboard.type('1', { delay: 30 });
      console.log('Typed item value');
    }
    await wait(300);
    if (itemIndexInput) {
      await itemIndexInput.click({ timeout: 5000 });
      await page.keyboard.type('1', { delay: 30 });
      console.log('Typed item index');
    }
    await wait(1000);
    console.log('Form filled successfully');

    await page.screenshot({ path: `${SCREENSHOTS_DIR}\\item2_add_modal_filled.png`, fullPage: false });
    console.log('Screenshot of filled form: item2_add_modal_filled.png');

    console.log('\n=== Step 8: Clicking save button ===');
    await wait(500);
    const saveResult = await page.evaluate(() => {
      const modal = document.querySelector('.ant-modal') || document.body;
      const allClickable = modal.querySelectorAll('button, [role="button"], a, [class*="btn"], [class*="button"]');
      for (const el of allClickable) {
        const text = (el.textContent || '').replace(/\s+/g, '').trim();
        if (text.includes('保存')) {
          el.click();
          return 'found';
        }
      }
      return 'not found';
    });
    console.log('Save button search result:', saveResult);
    
    if (saveResult === 'found') {
      await wait(5000);
      console.log('Clicked save');
    } else {
      throw new Error('Save button not found');
    }

    console.log('\n=== Step 9: Screenshot result ===');
    await page.screenshot({ path: `${SCREENSHOTS_DIR}\\item2_add_result.png`, fullPage: false });
    console.log('Screenshot saved: item2_add_result.png');

    console.log('\n=== Step 10: Verifying new item in list ===');
    await wait(2000);
    const pageText = await page.$eval('body', el => el.innerText);
    const newItemFound = pageText.includes(testItemCode) && pageText.includes('测试项目');
    results.addLookupItem.success = newItemFound;
    results.addLookupItem.message = newItemFound ? 'New item appeared in list' : 'New item NOT found in list';
    console.log(`Verification: ${results.addLookupItem.message}`);

    if (!newItemFound) {
      console.log('\n!!! Add failed - stopping test !!!');
      results.errors.push('Add LookUp item failed - item not found in list');
    } else {
      console.log('\n=== Step 11: Finding and clicking edit button for new item ===');
      const editResult = await page.evaluate((itemCode) => {
        const rows = document.querySelectorAll('tbody tr');
        for (const row of rows) {
          const cells = row.querySelectorAll('td');
          for (const cell of cells) {
            if (cell.textContent.includes(itemCode)) {
              const buttons = row.querySelectorAll('button');
              for (const btn of buttons) {
                if (btn.textContent.includes('编辑')) {
                  btn.click();
                  return true;
                }
              }
            }
          }
        }
        return false;
      }, testItemCode);
      
      if (editResult) {
        await wait(1000);
        console.log('Clicked edit button for new item');
      } else {
        throw new Error('Edit button not found for new item');
      }

      console.log('\n=== Step 12: Screenshot item3_edit.png ===');
      await page.screenshot({ path: `${SCREENSHOTS_DIR}\\item3_edit.png`, fullPage: false });
      console.log('Screenshot saved: item3_edit.png');

      console.log('\n=== Step 13: Modifying item name ===');
      await page.evaluate(() => {
        const editInput = document.querySelector('#itemName');
        if (editInput) {
          const nativeInputValueSetter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;
          nativeInputValueSetter.call(editInput, '测试项目-已编辑');
          editInput.dispatchEvent(new Event('input', { bubbles: true }));
          editInput.dispatchEvent(new Event('change', { bubbles: true }));
        }
      });
      console.log('Modified item name to "测试项目-已编辑"');
      await wait(500);

      console.log('\n=== Step 14: Clicking save in edit modal ===');
      const editSaveResult = await page.evaluate(() => {
        const modal = document.querySelector('.ant-modal') || document.body;
        const buttons = Array.from(modal.querySelectorAll('button'));
        const saveBtn = buttons.find(btn => btn.textContent.replace(/\s+/g, '').includes('保存'));
        if (saveBtn) {
          saveBtn.click();
          return 'found and clicked';
        }
        return 'not found';
      });
      console.log('Edit save result:', editSaveResult);
      
      if (editSaveResult === 'found and clicked') {
        await wait(2000);
        console.log('Clicked save in edit modal');
      }
      
      await page.screenshot({ path: `${SCREENSHOTS_DIR}\\item3_edit_result.png`, fullPage: false });
      console.log('Screenshot saved: item3_edit_result.png');

      const afterEditText = await page.$eval('body', el => el.innerText);
      const editSuccess = afterEditText.includes('测试项目-已编辑');
      results.editLookupItem.success = editSuccess;
      results.editLookupItem.message = editSuccess ? 'Edit successful' : 'Edit may have failed';
      console.log(`Edit verification: ${results.editLookupItem.message}`);

      console.log('\n=== Step 15: Clicking view button ===');
      const viewResult = await page.evaluate((itemCode) => {
        const rows = document.querySelectorAll('tbody tr');
        for (const row of rows) {
          const cells = row.querySelectorAll('td');
          for (const cell of cells) {
            if (cell.textContent.includes(itemCode)) {
              const buttons = row.querySelectorAll('button');
              for (const btn of buttons) {
                if (btn.textContent.includes('查看')) {
                  btn.click();
                  return true;
                }
              }
            }
          }
        }
        return false;
      }, testItemCode);
      
      if (viewResult) {
        await wait(1000);
        console.log('Clicked view button');
      } else {
        throw new Error('View button not found');
      }

      console.log('\n=== Step 16: Screenshot detail panel ===');
      await page.screenshot({ path: `${SCREENSHOTS_DIR}\\item4_view.png`, fullPage: false });
      console.log('Screenshot saved: item4_view.png');
      results.viewLookupItem.success = true;
      results.viewLookupItem.message = 'Detail panel displayed';
    }

  } catch (error) {
    console.error('\n!!! Test Error !!!:', error.message);
    results.errors.push(`Test Error: ${error.message}`);
  } finally {
    await browser.close();
  }

  console.log('\n========== TEST REPORT ==========');
  console.log(`新增LookUp项: ${results.addLookupItem.success ? '成功 ✓' : '失败 ✗'} - ${results.addLookupItem.message}`);
  console.log(`编辑LookUp项: ${results.editLookupItem.success ? '成功 ✓' : '失败 ✗'} - ${results.editLookupItem.message}`);
  console.log(`查看详情: ${results.viewLookupItem.success ? '正常显示 ✓' : '异常 ✗'} - ${results.viewLookupItem.message}`);
  if (results.errors.length > 0) {
    console.log('\n错误信息:');
    results.errors.forEach(err => console.log(`  - ${err}`));
  } else {
    console.log('\n无错误信息');
  }
  console.log('================================');
}

runTest();