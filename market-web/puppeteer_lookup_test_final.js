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
    
    console.log('3. 检查分类列表中的"项数量"列...');
    await page.waitForSelector('table', { timeout: 10000 });
    
    const categoryData = await page.evaluate(() => {
      const rows = document.querySelectorAll('table tr');
      const result = [];
      
      rows.forEach((row, index) => {
        if (index === 0) return; // skip header
        const cells = row.querySelectorAll('td');
        if (cells.length >= 2) {
          const categoryName = cells[0].textContent?.trim() || '';
          const itemCountCell = cells[cells.length - 2]; // 项数量可能在倒数第二列
          const itemCount = itemCountCell?.textContent?.trim() || '0';
          const hasData = itemCount !== '0' && itemCount !== '' && itemCount !== '-';
          result.push({ name: categoryName, itemCount, hasData, rowIndex: index });
        }
      });
      
      return result;
    });
    
    console.log('分类列表:', JSON.stringify(categoryData, null, 2));
    
    let foundCategoryWithData = null;
    for (const cat of categoryData) {
      if (cat.hasData) {
        foundCategoryWithData = cat;
        break;
      }
    }
    
    if (!foundCategoryWithData) {
      console.log('没有找到有数据的分类，尝试点击第一个分类查看...');
      if (categoryData.length > 0) {
        const firstCategoryRow = await page.$('table tr:nth-child(2)');
        if (firstCategoryRow) {
          const firstCell = await firstCategoryRow.$('td:first-child');
          if (firstCell) {
            const catName = await firstCell.textContent();
            console.log(`点击分类: ${catName}`);
            await firstCell.click();
            await new Promise(r => setTimeout(r, 2000));
            
            const backButton = await page.$('text=返回分类列表');
            if (backButton) {
              console.log('该分类没有Item数据，返回重新选择...');
              await backButton.click();
              await page.waitForTimeout(1000);
              
              // 再次检查所有分类
              const newCategoryData = await page.evaluate(() => {
                const rows = document.querySelectorAll('table tr');
                const result = [];
                
                rows.forEach((row, index) => {
                  if (index === 0) return;
                  const cells = row.querySelectorAll('td');
                  if (cells.length >= 2) {
                    const categoryName = cells[0].textContent?.trim() || '';
                    const itemCountCell = cells[cells.length - 2];
                    const itemCount = itemCountCell?.textContent?.trim() || '0';
                    const hasData = itemCount !== '0' && itemCount !== '' && itemCount !== '-';
                    result.push({ name: categoryName, itemCount, hasData });
                  }
                });
                
                return result;
              });
              
              for (const cat of newCategoryData) {
                if (cat.hasData) {
                  foundCategoryWithData = cat;
                  break;
                }
              }
              
              if (foundCategoryWithData) {
                console.log(`再次点击有数据的分类: ${foundCategoryWithData.name}`);
                await page.click(`text=${foundCategoryWithData.name}`);
                await new Promise(r => setTimeout(r, 2000));
              }
            }
          }
        }
      }
    } else {
      console.log(`找到有数据的分类: ${foundCategoryWithData.name}, 项数量: ${foundCategoryWithData.itemCount}`);
      console.log(`点击分类: ${foundCategoryWithData.name}`);
      await page.click(`text=${foundCategoryWithData.name}`);
      await new Promise(r => setTimeout(r, 2000));
    }
    
    if (foundCategoryWithData) {
      console.log('6. 截图保存当前页面...');
      await page.screenshot({ path: 'F:/workspace/open-app/test_screenshots/item_with_data.png', fullPage: true });
      console.log('截图已保存到 F:/workspace/open-app/test_screenshots/item_with_data.png');
      
      console.log('检查操作列按钮样式...');
      const buttonStyles = await page.evaluate(() => {
        const buttons = document.querySelectorAll('table button, table a');
        const result = [];
        
        buttons.forEach(btn => {
          const style = window.getComputedStyle(btn);
          const isLinkStyle = style.background === 'none' || style.backgroundColor === 'transparent' || 
                              style.border === 'none' || style.borderWidth === '0px';
          const isText = style.color && style.color !== 'transparent' && style.color !== 'white';
          const textDecoration = style.textDecoration || '';
          
          result.push({
            text: btn.textContent?.trim().substring(0, 20),
            isLinkStyle,
            color: style.color,
            background: style.background,
            textDecoration,
            fontWeight: style.fontWeight,
            border: style.border,
            display: style.display
          });
        });
        
        return result;
      });
      
      console.log('按钮样式检查:', JSON.stringify(buttonStyles, null, 2));
      
      const linkStyleButtons = buttonStyles.filter(b => b.isLinkStyle && b.display !== 'none');
      const buttonStyleOk = linkStyleButtons.length >= 3; // 至少有查看、编辑、删除等
      
      console.log('检查数据显示是否整齐...');
      const tableLayout = await page.evaluate(() => {
        const table = document.querySelector('table');
        if (!table) return null;
        
        const rows = table.querySelectorAll('tr');
        const firstRowCells = rows[0]?.querySelectorAll('th, td');
        const cellWidths = [];
        firstRowCells?.forEach(cell => {
          const rect = cell.getBoundingClientRect();
          cellWidths.push(Math.round(rect.width));
        });
        
        return {
          rowCount: rows.length,
          cellCount: firstRowCells?.length || 0,
          cellWidths,
          hasAlignment: cellWidths.length > 0 && cellWidths.every(w => w > 50)
        };
      });
      
      console.log('表格布局:', JSON.stringify(tableLayout, null, 2));
      
      console.log('\n========== 测试报告 ==========');
      console.log(`1. 找到有数据的分类: ${foundCategoryWithData ? '是 - ' + foundCategoryWithData.name : '否'}`);
      console.log(`2. 按钮样式: ${buttonStyleOk ? '已修复（文字链接样式）' : '可能未完全修复（请查看截图确认）'}`);
      console.log(`3. 数据显示: ${tableLayout && tableLayout.hasAlignment ? '整齐' : '需人工确认'}`);
      console.log('==============================\n');
      
    } else {
      console.log('\n========== 测试报告 ==========');
      console.log('1. 找到有数据的分类: 否');
      console.log('2. 按钮样式: 未测试');
      console.log('3. 数据显示: 未测试');
      console.log('==============================\n');
    }
    
  } catch (error) {
    console.error('测试出错:', error.message);
    await page.screenshot({ path: 'F:/workspace/open-app/test_screenshots/error.png' });
  } finally {
    await browser.close();
  }
}

runTest();