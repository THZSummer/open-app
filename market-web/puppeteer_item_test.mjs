import puppeteer from 'puppeteer';
import fs from 'fs';
import path from 'path';

const screenshotDir = 'F:\\workspace\\open-app\\test_screenshots';

if (!fs.existsSync(screenshotDir)) {
  fs.mkdirSync(screenshotDir, { recursive: true });
}

async function runTest() {
  const browser = await puppeteer.launch({
    headless: false,
    args: ['--no-sandbox', '--disable-setuid-sandbox']
  });
  
  const page = await browser.newPage();
  await page.setViewport({ width: 1920, height: 1080 });
  
  console.log('1. Opening page...');
  await page.goto('http://localhost:13000/market-web/', { waitUntil: 'networkidle0', timeout: 60000 });
  await new Promise(r => setTimeout(r, 2000));
  
  console.log('2. Clicking "LookUp管理" menu...');
  const menuItems = await page.$$('li');
  for (const item of menuItems) {
    const text = await item.evaluate(el => el.textContent);
    if (text && text.includes('LookUp管理')) {
      await item.click();
      console.log('   Found and clicked LookUp管理');
      break;
    }
  }
  await new Promise(r => setTimeout(r, 2000));
  
  console.log('3. Clicking first row in the list...');
  const rows = await page.$$('tr');
  let clicked = false;
  for (const row of rows) {
    const text = await row.evaluate(el => el.textContent);
    if (text && text.includes('Item') || (text && text.match(/\d{4,}/))) {
      await row.click();
      clicked = true;
      console.log('   Clicked a row');
      break;
    }
  }
  if (!clicked) {
    const firstRow = rows[1];
    if (firstRow) {
      await firstRow.click();
      console.log('   Clicked first data row');
    }
  }
  await new Promise(r => setTimeout(r, 3000));
  
  console.log('4. Taking screenshot...');
  const screenshotPath = path.join(screenshotDir, 'item_fixed.png');
  await page.screenshot({ path: screenshotPath, fullPage: true });
  console.log(`   Saved to: ${screenshotPath}`);
  
  console.log('5. Checking button styles and data...');
  
  const buttonCheck = await page.evaluate(() => {
    const buttons = document.querySelectorAll('button, a, .ant-btn, .lookup-link');
    const styles = [];
    buttons.forEach(btn => {
      const style = window.getComputedStyle(btn);
      styles.push({
        tag: btn.tagName,
        text: btn.textContent?.substring(0, 50),
        color: style.color,
        textDecoration: style.textDecoration,
        background: style.background
      });
    });
    return styles.slice(0, 10);
  });
  console.log('   Button/Link styles:', JSON.stringify(buttonCheck, null, 2));
  
  const operationColumn = await page.evaluate(() => {
    const cells = document.querySelectorAll('td');
    const opCells = [];
    cells.forEach(cell => {
      if (cell.textContent?.includes('编辑') || cell.textContent?.includes('删除') || cell.textContent?.includes('查看')) {
        const html = cell.innerHTML;
        const style = window.getComputedStyle(cell);
        opCells.push({
          text: cell.textContent?.substring(0, 100),
          html: html.substring(0, 200),
          color: style.color,
          textDecoration: style.textDecoration
        });
      }
    });
    return opCells;
  });
  console.log('   Operation column:', JSON.stringify(operationColumn, null, 2));
  
  const tableHeaders = await page.evaluate(() => {
    const headers = document.querySelectorAll('th');
    return Array.from(headers).map(h => h.textContent?.trim()).filter(Boolean);
  });
  console.log('   Table headers:', tableHeaders);
  
  const tableData = await page.evaluate(() => {
    const rows = document.querySelectorAll('tr');
    const data = [];
    rows.forEach((row, i) => {
      const cells = row.querySelectorAll('td');
      if (cells.length > 0) {
        const rowData = Array.from(cells).map(c => c.textContent?.trim().substring(0, 30));
        data.push(rowData);
      }
    });
    return data.slice(0, 5);
  });
  console.log('   Table data (first 5 rows):', JSON.stringify(tableData, null, 2));
  
  const hasEncodingIssues = await page.evaluate(() => {
    const body = document.body.textContent;
    const hasMojiBango = body?.includes('�') || body?.includes('���') || body?.includes('锟斤拷');
    return hasMojiBango;
  });
  console.log('   Has encoding issues:', hasEncodingIssues);
  
  await browser.close();
  
  console.log('\n=== REPORT ===');
  console.log('Screenshot saved to:', screenshotPath);
  console.log('Table headers found:', tableHeaders.length > 0 ? 'YES' : 'NO');
  console.log('Headers:', tableHeaders);
  console.log('Data rows found:', tableData.length);
  console.log('Encoding issues:', hasEncodingIssues ? 'YES - has garbled characters' : 'NO');
}

runTest().catch(console.error);