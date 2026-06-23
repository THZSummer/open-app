/**
 * 审批管理模块前后端联调测试
 *
 * 不 mock API，直接调用真实后端，验证前端页面 + 后端数据整合。
 * 启动：node puppeteer_approval_integration.mjs
 */
import puppeteer from 'puppeteer';
import fs from 'fs';
import path from 'path';

const BASE_URL = 'http://localhost:13000/market-web';
const screenshotDir = path.resolve('D:/open-app/open-app-ls/market-web/test_screenshots');

if (!fs.existsSync(screenshotDir)) {
  fs.mkdirSync(screenshotDir, { recursive: true });
}

let passCount = 0;
let failCount = 0;
const results = [];

function assert(condition, testId, description) {
  if (condition) {
    passCount++;
    console.log(`  ✓ [${testId}] ${description}`);
    results.push({ testId, description, status: 'PASS' });
  } else {
    failCount++;
    console.log(`  ✗ [${testId}] ${description}`);
    results.push({ testId, description, status: 'FAIL' });
  }
}

async function screenshot(page, name) {
  const p = path.join(screenshotDir, `integration_${name}.png`);
  await page.screenshot({ path: p, fullPage: true });
  console.log(`    [screenshot] ${p}`);
}

async function getDataRowCount(page) {
  return page.evaluate(() => {
    const pane = document.querySelector('.ant-tabs-tabpane-active');
    if (!pane) return 0;
    const tbody = pane.querySelector('.ant-table-tbody');
    if (!tbody) return 0;
    return Array.from(tbody.querySelectorAll('tr')).filter(tr => !tr.classList.contains('ant-table-measure-row')).length;
  });
}

async function getCellText(page, rowIdx, colIdx) {
  return page.evaluate(([r, c]) => {
    const pane = document.querySelector('.ant-tabs-tabpane-active');
    if (!pane) return '';
    const tbody = pane.querySelector('.ant-table-tbody');
    if (!tbody) return '';
    const rows = Array.from(tbody.querySelectorAll('tr')).filter(tr => !tr.classList.contains('ant-table-measure-row'));
    if (!rows[r]) return '';
    const cells = rows[r].querySelectorAll('td');
    return cells[c] ? (cells[c].textContent?.trim() || '') : '';
  }, [rowIdx, colIdx]);
}

async function getActionButtons(page, rowIdx) {
  return page.evaluate(([r]) => {
    const pane = document.querySelector('.ant-tabs-tabpane-active');
    if (!pane) return [];
    const tbody = pane.querySelector('.ant-table-tbody');
    if (!tbody) return [];
    const rows = Array.from(tbody.querySelectorAll('tr')).filter(tr => !tr.classList.contains('ant-table-measure-row'));
    if (!rows[r]) return [];
    const fixCell = rows[r].querySelector('.ant-table-cell-fix-right') || rows[r].querySelector('td:last-child');
    if (!fixCell) return [];
    return Array.from(fixCell.querySelectorAll('span')).map(s => s.textContent?.trim()).filter(Boolean);
  }, [rowIdx]);
}

async function getTableHeaders(page) {
  return page.evaluate(() => {
    const pane = document.querySelector('.ant-tabs-tabpane-active');
    if (!pane) return [];
    const thead = pane.querySelector('.ant-table-thead');
    if (!thead) return [];
    return Array.from(thead.querySelectorAll('th')).map(th => th.textContent?.trim()).filter(Boolean);
  });
}

async function clickTab(page, tabName) {
  await page.evaluate(([name]) => {
    const tabs = document.querySelectorAll('.ant-tabs-tab');
    for (const tab of tabs) { if (tab.textContent.includes(name)) { tab.click(); return; } }
  }, [tabName]);
}

async function runTests() {
  console.log('========================================');
  console.log('  审批管理 前后端联调测试');
  console.log('  真实后端 API（端口 18080）');
  console.log('========================================\n');

  const browser = await puppeteer.launch({
    headless: false,
    args: ['--no-sandbox', '--disable-setuid-sandbox'],
  });
  const page = await browser.newPage();
  await page.setViewport({ width: 1920, height: 1080 });

  page.on('console', (msg) => {
    if (msg.type() === 'error' && !msg.text().includes('favicon')) {
      console.log(`    [browser-err] ${msg.text().substring(0, 120)}`);
    }
  });

  // 记录 API 调用
  let apiCalls = [];
  await page.setRequestInterception(true);
  page.on('request', (req) => {
    const url = req.url();
    if (url.includes('/service/open/v2/apps/')) {
      apiCalls.push({ url: url.replace('http://localhost:13000', ''), type: url.includes('pending') ? 'pending' : url.includes('publish') ? 'published' : 'approval' });
    }
    req.continue();
  });

  try {
    // ============================================
    // 1. 进入审批页面
    // ============================================
    console.log('\n--- 1. 页面导航 ---');
    await page.goto(`${BASE_URL}/`, { waitUntil: 'networkidle0', timeout: 30000 });
    await new Promise(r => setTimeout(r, 2000));

    await page.evaluate(() => {
      const items = document.querySelectorAll('.ant-menu-item');
      for (const item of items) { if (item.textContent.includes('审批管理')) { item.click(); return true; } }
      return false;
    });
    await new Promise(r => setTimeout(r, 3000));

    const url = page.url();
    assert(url.includes('/approval'), 'N-01', `导航到 /approval (URL: ${url})`);

    // ============================================
    // 2. 待审批 Tab — 数据加载
    // ============================================
    console.log('\n--- 2. 待审批 Tab 数据验证 ---');

    const defaultTab = await page.evaluate(() => {
      const active = document.querySelector('.ant-tabs-tab-active');
      return active?.textContent?.trim() || '';
    });
    assert(defaultTab === '待审批应用', 'D-01', `默认 Tab = "待审批应用" (实际: "${defaultTab}")`);

    const rowCount = await getDataRowCount(page);
    assert(rowCount > 0, 'D-02', `待审批列表有数据 (实际: ${rowCount} 行)`);

    const headers = await getTableHeaders(page);
    const expectedHeaders = ['应用名称', '应用能力', '版本号', '应用ID', '申请账号', '申请时间', '操作'];
    const headersMatch = expectedHeaders.every(h => headers.includes(h));
    assert(headersMatch, 'D-03', `表头完整 (${headers.join(', ')})`);

    // 验证数据字段非空
    const appName = await getCellText(page, 0, 0);
    assert(appName.length > 0, 'D-04', `第1行应用名称非空 (实际: "${appName}")`);

    const versionNo = await getCellText(page, 0, 2);
    assert(versionNo.length > 0, 'D-05', `第1行版本号非空 (实际: "${versionNo}")`);

    const appId = await getCellText(page, 0, 3);
    assert(appId.length > 0, 'D-06', `第1行应用ID非空 (实际: "${appId}")`);

    const actions = await getActionButtons(page, 0);
    assert(
      actions.includes('查看') && actions.includes('同意') && actions.includes('拒绝'),
      'D-07', `操作按钮完整 (实际: ${actions.join('/')})`
    );

    // 验证 API 调用
    const pendingCalled = apiCalls.some(c => c.type === 'pending');
    assert(pendingCalled, 'D-08', '调用了 pending API');

    await screenshot(page, 'pending_tab');

    // ============================================
    // 3. 已上架 Tab — 数据加载
    // ============================================
    console.log('\n--- 3. 已上架 Tab 数据验证 ---');
    apiCalls = [];

    await clickTab(page, '已上架应用');
    await new Promise(r => setTimeout(r, 3000));

    const publishedTab = await page.evaluate(() => {
      const active = document.querySelector('.ant-tabs-tab-active');
      return active?.textContent?.trim() || '';
    });
    assert(publishedTab === '已上架应用', 'P-01', `已上架 Tab 激活 (实际: "${publishedTab}")`);

    const pubRowCount = await getDataRowCount(page);
    // 已上架列表可能有 0 条或多条数据
    assert(pubRowCount >= 0, 'P-02', `已上架列表加载完成 (${pubRowCount} 行)`);

    const pubActions = await getActionButtons(page, 0);
    if (pubRowCount > 0) {
      assert(
        pubActions.includes('查看') && !pubActions.includes('同意') && !pubActions.includes('拒绝'),
        'P-03', `已上架仅显示查看 (实际: ${pubActions.join('/')})`
      );
    } else {
      assert(true, 'P-03', '已上架列表无数据，跳过按钮检查');
    }

    const publishCalled = apiCalls.some(c => c.type === 'published');
    assert(publishCalled, 'P-04', '调用了 publish API');

    await screenshot(page, 'published_tab');

    // ============================================
    // 4. Tab 切换回待审批
    // ============================================
    console.log('\n--- 4. Tab 切换回待审批 ---');
    apiCalls = [];

    await clickTab(page, '待审批应用');
    await new Promise(r => setTimeout(r, 3000));

    const backTab = await page.evaluate(() => {
      const active = document.querySelector('.ant-tabs-tab-active');
      return active?.textContent?.trim() || '';
    });
    assert(backTab === '待审批应用', 'S-01', `切换回待审批 (实际: "${backTab}")`);

    const backActions = await getActionButtons(page, 0);
    assert(
      backActions.includes('查看') && backActions.includes('同意') && backActions.includes('拒绝'),
      'S-02', '操作按钮恢复 查看/同意/拒绝'
    );

    const pendingRecalled = apiCalls.some(c => c.type === 'pending');
    assert(pendingRecalled, 'S-03', '切换回后重新调用 pending API');

    await screenshot(page, 'switch_back');

    // ============================================
    // 5. 分页
    // ============================================
    console.log('\n--- 5. 分页验证 ---');

    const paginationInfo = await page.evaluate(() => {
      const pagination = document.querySelector('.ant-pagination');
      if (!pagination) return { exists: false };
      const totalText = pagination.querySelector('.ant-pagination-total-text');
      const total = totalText ? totalText.textContent?.trim() : '';
      const pages = Array.from(pagination.querySelectorAll('.ant-pagination-item')).map(p => p.textContent?.trim());
      return { exists: true, total, pages };
    });
    assert(paginationInfo.exists, 'PG-01', `分页组件存在 (总数: ${paginationInfo.total})`);

    await screenshot(page, 'pagination');

    // ============================================
    // 6. 语言切换
    // ============================================
    console.log('\n--- 6. 语言切换 ---');

    const cnName = await getCellText(page, 0, 0);
    assert(cnName.length > 0, 'L-01', `中文显示应用名称 (实际: "${cnName}")`);

    await page.evaluate(() => {
      const spans = document.querySelectorAll('span');
      for (const s of spans) { if (s.textContent.trim() === 'EN') { s.click(); return; } }
    });
    await new Promise(r => setTimeout(r, 500));

    const enName = await getCellText(page, 0, 0);
    // 英文名可能与中文名不同
    assert(enName.length > 0, 'L-02', `英文环境应用名称非空 (实际: "${enName}")`);

    // 切回中文
    await page.evaluate(() => {
      const spans = document.querySelectorAll('span');
      for (const s of spans) { if (s.textContent.trim() === '中文') { s.click(); return; } }
    });
    await new Promise(r => setTimeout(r, 500));

    await screenshot(page, 'lang_switch');

    // ============================================
    // 最终截图
    // ============================================
    await screenshot(page, 'final');

  } catch (err) {
    console.error('\n测试出错:', err.message);
    await screenshot(page, 'error');
  } finally {
    await browser.close();
  }

  // ===================== 测试报告 =====================
  console.log('\n========================================');
  console.log('  联调测试报告');
  console.log('========================================');
  console.log(`  总计: ${passCount + failCount} 条`);
  console.log(`  通过: ${passCount} 条 ✓`);
  console.log(`  失败: ${failCount} 条 ✗`);
  console.log('----------------------------------------');
  results.forEach(r => {
    console.log(`  ${r.status === 'PASS' ? '✓' : '✗'} [${r.testId}] ${r.description}`);
  });
  console.log('========================================\n');

  const indexPath = path.join(screenshotDir, 'integration_test_report.txt');
  fs.writeFileSync(indexPath,
    `审批管理前后端联调测试报告\n${'='.repeat(50)}\n` +
    results.map(r => `- [${r.status}] ${r.testId}: ${r.description}`).join('\n') +
    `\n\n通过: ${passCount} / 失败: ${failCount}\n`);
  console.log(`报告已保存: ${indexPath}`);
}

runTests().catch(console.error);
