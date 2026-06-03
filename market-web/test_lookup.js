import puppeteer from 'puppeteer';
import fs from 'fs';
import path from 'path';

const screenshotDir = 'F:\\workspace\\open-app\\test_screenshots';
if (!fs.existsSync(screenshotDir)) {
  fs.mkdirSync(screenshotDir, { recursive: true });
}

async function delay(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

async function run() {
  const browser = await puppeteer.launch({
    headless: true,
    defaultViewport: { width: 1920, height: 1080 }
  });
  
  const page = await browser.newPage();
  
  console.log('1. 刷新页面 http://localhost:13000/market-web/');
  await page.goto('http://localhost:13000/market-web/', { waitUntil: 'networkidle2' });
  
  console.log('2. 点击 "LookUp管理"');
  await page.waitForSelector('text=LookUp管理', { timeout: 10000 });
  await page.click('text=LookUp管理');
  await delay(1000);
  
  console.log('3. 点击第一个分类进入Item页面');
  await page.waitForSelector('.ant-menu-item', { timeout: 10000 });
  const menuItems = await page.$$('.ant-menu-item');
  if (menuItems.length > 0) {
    await menuItems[0].click();
    await delay(2000);
  }
  
  console.log('4. 截图保存到 item_now.png');
  await page.screenshot({ path: 'F:\\workspace\\open-app\\test_screenshots\\item_now.png' });
  
  console.log('5. 检查页面元素');
  const report = {
    buttons: [],
    tableData: [],
    actionButtons: []
  };
  
  const buttonStyles = await page.evaluate(() => {
    const styles = [];
    const btnSelectors = [
      'button.ant-btn',
      '.ant-form-item button',
      '.ant-modal button',
      '.ant-table button'
    ];
    const buttons = document.querySelectorAll(btnSelectors.join(', '));
    buttons.forEach(btn => {
      const computed = window.getComputedStyle(btn);
      styles.push({
        text: btn.textContent?.trim().substring(0, 20),
        backgroundColor: computed.backgroundColor,
        color: computed.color,
        borderRadius: computed.borderRadius,
        padding: computed.padding,
        border: computed.border
      });
    });
    return styles;
  });
  report.buttons = buttonStyles;
  
  const tableData = await page.evaluate(() => {
    const rows = document.querySelectorAll('.ant-table-tbody tr');
    const data = [];
    rows.forEach(row => {
      const cells = row.querySelectorAll('td');
      if (cells.length > 0) {
        const rowData = [];
        cells.forEach(cell => rowData.push(cell.textContent?.trim().substring(0, 30)));
        data.push(rowData);
      }
    });
    return data;
  });
  report.tableData = tableData;
  
  const actionStyles = await page.evaluate(() => {
    const styles = [];
    const actionLinks = document.querySelectorAll('.ant-table-cell a, .ant-table-cell span[role="button"]');
    actionLinks.forEach(el => {
      const computed = window.getComputedStyle(el);
      styles.push({
        text: el.textContent?.trim().substring(0, 20),
        display: computed.display,
        color: computed.color,
        textDecoration: computed.textDecoration,
        background: computed.background,
        fontSize: computed.fontSize,
        cursor: computed.cursor
      });
    });
    return styles;
  });
  report.actionButtons = actionStyles;
  
  console.log('\n=== 检查报告 ===');
  console.log('\n【按钮样式】');
  report.buttons.forEach(btn => {
    console.log(`  - ${btn.text}: bg=${btn.backgroundColor}, color=${btn.color}, radius=${btn.borderRadius}`);
  });
  
  console.log('\n【表格数据】');
  console.log(`  共 ${report.tableData.length} 行数据`);
  if (report.tableData.length > 0) {
    console.log('  前3行:');
    report.tableData.slice(0, 3).forEach((row, i) => {
      console.log(`    第${i+1}行: ${row.join(' | ')}`);
    });
  }
  
  console.log('\n【操作列按钮样式】');
  report.actionButtons.forEach(btn => {
    console.log(`  - ${btn.text}: display=${btn.display}, color=${btn.color}, decoration=${btn.textDecoration}`);
  });
  
  await browser.close();
  
  fs.writeFileSync('F:\\workspace\\open-app\\test_screenshots\\report.json', JSON.stringify(report, null, 2));
  console.log('\n报告已保存到 test_screenshots/report.json');
}

run().catch(console.error);
