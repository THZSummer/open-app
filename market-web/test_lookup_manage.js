import puppeteer from 'puppeteer';

const SCREENSHOTS_DIR = 'F:\\workspace\\open-app\\test_screenshots';
const BASE_URL = 'http://localhost:13000/market-web/';

const results = {
  addCategorySuccess: false,
  exportTaskSuccess: false,
  taskCenterHasTasks: false,
  errors: []
};

async function saveScreenshot(page, path) {
  await page.screenshot({ path, fullPage: false });
  console.log(`Screenshot saved: ${path}`);
}

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
    console.log('Page loaded');

    console.log('\n=== Step 2: Clicking "LookUp管理" ===');
    await page.click('text=LookUp管理');
    await new Promise(r => setTimeout(r, 2000));
    console.log('Clicked LookUp管理');

    console.log('\n=== Step 3: Screenshot test1_classify_list.png ===');
    await saveScreenshot(page, `${SCREENSHOTS_DIR}\\test1_classify_list.png`);

    console.log('\n=== Step 4: Clicking "新增分类" ===');
    const addBtn = await page.$('text=新增分类');
    if (addBtn) {
      await addBtn.click();
      await page.waitForSelector('.ant-modal-content', { timeout: 5000 });
      await new Promise(r => setTimeout(r, 1000));
      console.log('Clicked 新增分类');
      
      await saveScreenshot(page, `${SCREENSHOTS_DIR}\\test2_add_modal.png`);
      
      console.log('\n=== Filling form ===');
      const inputDebug = await page.evaluate(() => {
        const modal = document.querySelector('.ant-modal-content');
        if (!modal) return { found: false };
        const inputs = Array.from(modal.querySelectorAll('input'));
        return { found: true, count: inputs.length, placeholders: inputs.map(i => i.placeholder) };
      });
      console.log('Input debug:', inputDebug);
      
      const inputs = await page.$$('.ant-modal-content input');
      if (inputs.length >= 2) {
        await inputs[0].type('TEST_CJ_001', { delay: 100 });
        await inputs[1].type('测试新增分类', { delay: 100 });
      } else {
        results.errors.push('Not enough inputs found in modal');
      }
      await new Promise(r => setTimeout(r, 500));
      
      const inputValuesAfterTyping = await page.evaluate(() => {
        const modal = document.querySelector('.ant-modal-content');
        if (!modal) return [];
        const inputs = Array.from(modal.querySelectorAll('input'));
        return inputs.slice(0, 2).map(i => i.value);
      });
      console.log('Input values after typing:', inputValuesAfterTyping);
      
      await saveScreenshot(page, `${SCREENSHOTS_DIR}\\test2_add_modal_filled.png`);
      
      let saveClicked = false;
      try {
        await page.click('text=保 存');
        console.log('Clicked 保存 via text selector');
        saveClicked = true;
      } catch (e) {
        console.log('Text selector failed, trying evaluate');
        const saveBtn = await page.evaluate(() => {
          const modal = document.querySelector('.ant-modal-content');
          if (!modal) return null;
          const buttons = modal.querySelectorAll('button');
          for (const btn of buttons) {
            const text = btn.textContent.replace(/\s/g, '');
            if (text.includes('保存')) return btn;
          }
          return null;
        });
        if (saveBtn) {
          await saveBtn.click();
          console.log('Clicked 保存 via evaluate');
          saveClicked = true;
        }
      }
      
      if (saveClicked) {
        await new Promise(r => setTimeout(r, 3000));
        
        const saveResult = await page.evaluate(() => {
          const modal = document.querySelector('.ant-modal-content');
          const modalVisible = modal ? window.getComputedStyle(modal).display !== 'none' : false;
          const errorMsgs = Array.from(document.querySelectorAll('.ant-form-item-explain-error')).map(e => e.textContent);
          const antMessage = document.querySelector('.ant-message');
          const hasNewCategory = document.body.innerText.includes('TEST_CJ_001');
          return { 
            modalPresent: !!modal,
            modalVisible, 
            errorMsgs,
            antMessage: antMessage ? antMessage.textContent : null,
            hasNewCategory
          };
        });
        console.log('Save result before screenshot:', saveResult);
        
        await saveScreenshot(page, `${SCREENSHOTS_DIR}\\test2_add_result.png`);
        console.log('Clicked 保存');
        
        if (!saveResult.modalVisible && saveResult.errorMsgs.length === 0) {
          results.addCategorySuccess = true;
        } else if (saveResult.errorMsgs.length > 0) {
          results.errors.push('Form errors: ' + saveResult.errorMsgs.join(', '));
        }
      } else {
        results.errors.push('Save button not found in modal');
      }
    } else {
      results.errors.push('新增分类 button not found');
    }

    console.log('\n=== Step 5: Refreshing page ===');
    await page.reload({ waitUntil: 'networkidle2' });
    await new Promise(r => setTimeout(r, 2000));
    const pageText = await page.$eval('body', el => el.innerText);
    results.addCategorySuccess = pageText.includes('TEST_CJ_001') && pageText.includes('测试新增分类');
    console.log(`New category visible after refresh: ${results.addCategorySuccess}`);

    console.log('\n=== Step 6: Testing export ===');
    const exportBtn = await page.$('text=导出');
    if (exportBtn) {
      await exportBtn.click();
      await page.waitForSelector('.ant-modal-content', { timeout: 5000 });
      await new Promise(r => setTimeout(r, 1000));
      console.log('Clicked 导出');
      
      await saveScreenshot(page, `${SCREENSHOTS_DIR}\\test2_export_confirm.png`);
      
      try {
        const confirmBtnResult = await page.evaluate(() => {
          const modal = document.querySelector('.ant-modal-content');
          if (!modal) return { found: false, reason: 'no modal' };
          const buttons = Array.from(modal.querySelectorAll('button'));
          const buttonInfo = buttons.map(b => ({ text: b.textContent, trimmed: b.textContent.replace(/\s/g, '') }));
          for (const btn of buttons) {
            const text = btn.textContent.replace(/\s/g, '');
            if (text.includes('确认') || text.includes('确定')) return { found: true, buttonInfo };
          }
          return { found: false, reason: 'no match', buttonInfo };
        });
        console.log('Export confirm button search result:', confirmBtnResult);
        
        if (confirmBtnResult.found) {
          const confirmBtn = await page.$('.ant-modal-content button');
          if (confirmBtn) await confirmBtn.click();
          await new Promise(r => setTimeout(r, 2000));
          console.log('Export confirmed');
          results.exportTaskSuccess = true;
        } else {
          results.errors.push('Export confirm button not found');
        }
      } catch (e) {
        results.errors.push('Export confirm button error: ' + e.message);
      }
    } else {
      results.errors.push('导出 button not found');
    }

    console.log('\n=== Step 7: Clicking task center ===');
    await page.click('text=任务中心');
    await new Promise(r => setTimeout(r, 2000));
    console.log('Clicked 任务中心');

    console.log('\n=== Step 8: Screenshot test3_task_center.png ===');
    await saveScreenshot(page, `${SCREENSHOTS_DIR}\\test3_task_center.png`);
    
    const taskText = await page.$eval('body', el => el.innerText);
    results.taskCenterHasTasks = !taskText.includes('暂无数据') && (taskText.includes('任务') || taskText.includes('导出'));
    console.log(`Task center has tasks: ${results.taskCenterHasTasks}`);

  } catch (error) {
    results.errors.push(`Test Error: ${error.message}`);
    console.error('Test failed:', error.message);
  } finally {
    await browser.close();
  }

  console.log('\n=== Test Report ===');
  console.log(`新增分类是否成功: ${results.addCategorySuccess ? '✅ 成功' : '❌ 失败'}`);
  console.log(`导出任务是否提交成功: ${results.exportTaskSuccess ? '✅ 成功' : '❌ 失败'}`);
  console.log(`任务中心是否有任务显示: ${results.taskCenterHasTasks ? '✅ 有' : '❌ 无'}`);
  if (results.errors.length > 0) {
    console.log('\n错误信息:');
    results.errors.forEach(err => console.log(`  - ${err}`));
  } else {
    console.log('\n无错误信息');
  }
}

runTest();