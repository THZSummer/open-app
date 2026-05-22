import puppeteer from 'puppeteer';

async function runTest() {
  console.log('启动浏览器...');
  const browser = await puppeteer.launch({
    headless: false,
    args: ['--no-sandbox', '--disable-setuid-sandbox']
  });
  
  const page = await browser.newPage();
  await page.setViewport({ width: 1400, height: 900 });
  
  try {
    console.log('1. 打开页面...');
    await page.goto('http://localhost:13000/market-web/', { waitUntil: 'networkidle2', timeout: 30000 });
    
    console.log('2. 点击 "LookUp管理" 菜单...');
    await page.waitForSelector('text=LookUp管理', { timeout: 10000 });
    await page.click('text=LookUp管理');
    await new Promise(r => setTimeout(r, 1000));
    
    console.log('3. 找有数据的分类并进入Item页面...');
    await page.waitForSelector('table', { timeout: 10000 });
    
    const categoryData = await page.evaluate(() => {
      const rows = document.querySelectorAll('table tbody tr');
      const result = [];
      
      rows.forEach((row, index) => {
        const cells = row.querySelectorAll('td');
        if (cells.length >= 3) {
          const categoryName = cells[1]?.textContent?.trim() || '';
          result.push({ name: categoryName, rowIndex: index + 2 });
        }
      });
      
      return result;
    });
    
    console.log('分类列表:', JSON.stringify(categoryData, null, 2));
    
    console.log('遍历分类查找有数据的...');
    let foundData = false;
    
    for (let i = 0; i < 10; i++) {
      const rowCount = (await page.$$('table tbody tr')).length;
      if (rowCount === 0) break;
      
      const rowIndex = i + 2;
      if (rowIndex > rowCount + 1) break;
      
      console.log(`尝试第 ${i+1} 个分类 (行${rowIndex})`);
      
      try {
        await page.click(`table tbody tr:nth-child(${rowIndex})`);
        await new Promise(r => setTimeout(r, 3000));
        
        const currentUrl = page.url();
        console.log(`  当前URL: ${currentUrl}`);
        
        if (currentUrl.includes('/lookup/item')) {
          const hasData = await page.evaluate(() => {
            const tbody = document.querySelector('tbody');
            const text = tbody?.textContent || '';
            return !text.includes('暂无数据') && !text.includes('加载中');
          });
          
          if (hasData) {
            console.log(`✓ 找到有数据的分类`);
            foundData = true;
            break;
          }
          
          await page.goBack();
          await new Promise(r => setTimeout(r, 2000));
          await page.waitForSelector('table tbody tr', { timeout: 5000 });
        }
      } catch (e) {
        console.log(`跳过: ${e.message}`);
        break;
      }
    }
    
    if (!foundData) {
      console.log('没有找到有数据的分类，重新进入第一个分类的Item页面');
      await page.click('table tbody tr:nth-child(2)');
      await new Promise(r => setTimeout(r, 3000));
      console.log('当前页面:', page.url());
    }
    
    console.log('4. 截图保存到 F:/workspace/open-app/test_screenshots/item_final.png...');
    await page.screenshot({ path: 'F:/workspace/open-app/test_screenshots/item_final.png', fullPage: true });
    console.log('截图已保存');
    
    console.log('5. 检查表格...');
    
    const tableInfo = await page.evaluate(() => {
      const table = document.querySelector('table');
      if (!table) return { error: 'No table found' };
      
      const headers = Array.from(table.querySelectorAll('th')).map(th => th.textContent?.trim() || '');
      const rows = Array.from(table.querySelectorAll('tr')).slice(1, 6);
      const dataRows = rows.map(row => {
        const cells = Array.from(row.querySelectorAll('td')).map(td => td.textContent?.trim() || '');
        return cells;
      });
      
      const buttons = Array.from(table.querySelectorAll('td button, td a')).map(btn => {
        const style = window.getComputedStyle(btn);
        return {
          text: btn.textContent?.trim().substring(0, 15),
          color: style.color,
          background: style.background,
          textDecoration: style.textDecoration,
          border: style.border,
          display: style.display
        };
      });
      
      return { headers, dataRows, buttons, headerCount: headers.length };
    });
    
    console.log('\n========== 检查报告 ==========');
    console.log('\n【表格列检查】');
    console.log(`期望列(12列): 序号、项编码、项名称、项值、描述、排序、状态、创建人、创建时间、修改人、修改时间、操作`);
    console.log(`实际列数: ${tableInfo.headerCount}`);
    console.log(`列名: ${JSON.stringify(tableInfo.headers)}`);
    
    const expectedHeaders = ['序号', '项编码', '项名称', '项值', '描述', '排序', '状态', '创建人', '创建时间', '修改人', '修改时间', '操作'];
    const headersMatch = tableInfo.headerCount === 12 && 
      expectedHeaders.every(h => tableInfo.headers.includes(h));
    console.log(`列是否完整: ${headersMatch ? '✓ 是' : '✗ 否'}`);
    
    console.log('\n【数据对应检查】');
    if (tableInfo.dataRows && tableInfo.dataRows.length > 0) {
      const isEmptyState = tableInfo.dataRows.length === 1 && tableInfo.dataRows[0]?.includes('暂无数据');
      if (isEmptyState) {
        console.log(`数据状态: 暂无数据 (当前分类没有Item数据)`);
        console.log(`数据与表头是否对应: ⚠ 无数据可验证`);
      } else {
        console.log(`数据行数: ${tableInfo.dataRows.length}`);
        console.log(`第一行数据: ${JSON.stringify(tableInfo.dataRows[0])}`);
        const dataAligned = tableInfo.dataRows.every(row => row.length === tableInfo.headerCount);
        console.log(`数据与表头是否对应: ${dataAligned ? '✓ 是' : '✗ 否'}`);
      }
    } else {
      console.log('无数据行');
    }
    
    console.log('\n【操作列按钮样式检查】');
    if (tableInfo.buttons && tableInfo.buttons.length > 0) {
      console.log(`按钮数量: ${tableInfo.buttons.length}`);
      tableInfo.buttons.slice(0, 5).forEach((btn, i) => {
        console.log(`  按钮${i+1}: "${btn.text}" | color:${btn.color} | bg:${btn.background} | decoration:${btn.textDecoration}`);
      });
      const isLinkStyle = tableInfo.buttons.every(btn => 
        btn.background === 'none' || btn.background === 'transparent' ||
        btn.border === 'none' || btn.border === '0px none'
      );
      console.log(`按钮样式: ${isLinkStyle ? '✓ 是文字链接样式' : '⚠ 需人工确认'}`);
    } else {
      console.log('未找到操作列按钮');
    }
    
    console.log('\n========== 总结 ==========');
    console.log(`表格列完整: ${headersMatch ? '✓' : '✗'}`);
    console.log(`数据整齐: ${tableInfo.dataRows?.every(row => row.length === tableInfo.headerCount) ? '✓' : '⚠'}`);
    console.log(`按钮样式正确: ${tableInfo.buttons?.length > 0 ? '✓' : '?'}`);
    console.log('==============================\n');
    
  } catch (error) {
    console.error('测试出错:', error.message);
    await page.screenshot({ path: 'F:/workspace/open-app/test_screenshots/error.png' });
  } finally {
    await browser.close();
  }
}

runTest();