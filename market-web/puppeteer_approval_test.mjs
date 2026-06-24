/**
 * 审批管理模块 E2E 测试 — 覆盖 spec 7.2 前端测试用例 F-01 ~ F-15
 *
 * 使用 Puppeteer setRequestInterception 拦截 API 请求，mock 后端响应。
 * 启动：node puppeteer_approval_test.mjs
 *
 * 前置条件：
 *   - market-web dev server 已在 http://localhost:13000 运行
 */
import puppeteer from 'puppeteer';
import fs from 'fs';
import path from 'path';

const BASE_URL = 'http://localhost:13000/market-web';
const screenshotDir = path.resolve('D:/open-app/open-app-ls/market-web/test_screenshots');

if (!fs.existsSync(screenshotDir)) {
  fs.mkdirSync(screenshotDir, { recursive: true });
}

// ===================== Mock 数据 =====================

const MOCK_PENDING_DATA = {
  code: '200',
  messageZh: '成功',
  data: [
    { id:1, businessType:'app_version_publish', businessId:'1001', appId:'app_third_party_001',
      appNameCn:'订单管理应用', appNameEn:'Order Management App', versionNo:'v2.1.0',
      capabilityNames:'订单查询, 订单创建', applicantId:'u001', status:0, createTime:'2026-06-03 10:23:45' },
    { id:2, businessType:'app_version_publish', businessId:'1002', appId:'app_third_party_002',
      appNameCn:'库存同步工具', appNameEn:'Inventory Sync Tool', versionNo:'v1.0.0',
      capabilityNames:'库存查询', applicantId:'u002', status:0, createTime:'2026-06-05 14:30:00' },
    { id:3, businessType:'app_version_publish', businessId:'1003', appId:'app_third_party_003',
      appNameCn:'物流追踪系统', appNameEn:'Logistics Tracking System', versionNo:'v3.2.1',
      capabilityNames:'物流查询, 物流推送, 签收确认', applicantId:'u003', status:0, createTime:'2026-06-10 09:15:30' },
  ],
  page: { curPage:1, pageSize:10, total:3, totalPages:1 },
};

const MOCK_PUBLISHED_DATA = {
  code: '200',
  messageZh: '成功',
  data: [
    { id:101, appId:'app_pub_001', appNameCn:'支付网关', appNameEn:'Payment Gateway',
      versionNo:'v4.0.0', capabilityNames:'支付发起, 退款, 对账', applicantId:'u010', createTime:'2026-05-01 08:00:00' },
    { id:102, appId:'app_pub_002', appNameCn:'消息推送中心', appNameEn:'Message Push Center',
      versionNo:'v2.3.0', capabilityNames:'短信推送, 邮件推送', applicantId:'u011', createTime:'2026-05-15 12:00:00' },
  ],
  page: { curPage:1, pageSize:10, total:2, totalPages:1 },
};

const MOCK_APPROVE_SUCCESS = { code:'200', messageZh:'操作成功', messageEn:'Operation successful', data:null };
const MOCK_REJECT_SUCCESS = { code:'200', messageZh:'操作成功', messageEn:'Operation successful', data:null };

// ===================== 测试基础设施 =====================

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
  const p = path.join(screenshotDir, `approval_${name}.png`);
  await page.screenshot({ path: p, fullPage: true });
  console.log(`    [screenshot] ${p}`);
}

// ===== DOM 辅助函数 =====

/** 获取当前 Tab 数据行数 */
async function getDataRowCount(page) {
  return page.evaluate(() => {
    const pane = document.querySelector('.ant-tabs-tabpane-active');
    if (!pane) return 0;
    const tbody = pane.querySelector('.ant-table-tbody');
    if (!tbody) return 0;
    return Array.from(tbody.querySelectorAll('tr')).filter(tr => !tr.classList.contains('ant-table-measure-row')).length;
  });
}

/** 获取数据行文本 */
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

/** 获取操作按钮文本 */
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

/** 获取表头 */
async function getTableHeaders(page) {
  return page.evaluate(() => {
    const pane = document.querySelector('.ant-tabs-tabpane-active');
    if (!pane) return [];
    const thead = pane.querySelector('.ant-table-thead');
    if (!thead) return [];
    return Array.from(thead.querySelectorAll('th')).map(th => th.textContent?.trim()).filter(Boolean);
  });
}

// ===================== Mock 注册 =====================

async function setupMocks(page) {
  page._apiCalls = page._apiCalls || [];
  await page.setRequestInterception(true);
  page.on('request', (req) => {
    const url = req.url();
    if (url.includes('/service/open/v2/apps/pending')) {
      page._apiCalls.push({ url, method:'GET', type:'pending' });
      req.respond({ status:200, contentType:'application/json', body:JSON.stringify(MOCK_PENDING_DATA) });
      return;
    }
    if (url.includes('/service/open/v2/apps/publish')) {
      page._apiCalls.push({ url, method:'GET', type:'published' });
      req.respond({ status:200, contentType:'application/json', body:JSON.stringify(MOCK_PUBLISHED_DATA) });
      return;
    }
    if (url.includes('/service/open/v2/apps/approval')) {
      let body = {};
      try { body = JSON.parse(req.postData() || '{}'); } catch (_) {}
      page._apiCalls.push({ url, method:'POST', type:'approval', body });
      req.respond({ status:200, contentType:'application/json', body:JSON.stringify(body.action === 0 ? MOCK_APPROVE_SUCCESS : MOCK_REJECT_SUCCESS) });
      return;
    }
    req.continue();
  });
}

/** 点击 Tab */
async function clickTab(page, tabName) {
  await page.evaluate(([name]) => {
    const tabs = document.querySelectorAll('.ant-tabs-tab');
    for (const tab of tabs) { if (tab.textContent.includes(name)) { tab.click(); return; } }
  }, [tabName]);
}

/** 点击指定行的操作按钮 */
async function clickAction(page, rowIdx, actionName) {
  return page.evaluate(([r, name]) => {
    const pane = document.querySelector('.ant-tabs-tabpane-active');
    if (!pane) return false;
    const tbody = pane.querySelector('.ant-table-tbody');
    if (!tbody) return false;
    const rows = Array.from(tbody.querySelectorAll('tr')).filter(tr => !tr.classList.contains('ant-table-measure-row'));
    if (!rows[r]) return false;
    const fixCell = rows[r].querySelector('.ant-table-cell-fix-right') || rows[r].querySelector('td:last-child');
    if (!fixCell) return false;
    for (const span of fixCell.querySelectorAll('span')) {
      if (span.textContent.trim() === name) { span.click(); return true; }
    }
    return false;
  }, [rowIdx, actionName]);
}

// ===================== 测试用例 =====================

async function runTests() {
  console.log('========================================');
  console.log('  审批管理模块 E2E 测试');
  console.log('  覆盖 spec 7.2 前端测试用例 F-01 ~ F-15');
  console.log('========================================\n');

  const browser = await puppeteer.launch({
    headless: false,
    args: ['--no-sandbox', '--disable-setuid-sandbox'],
  });
  const page = await browser.newPage();
  await page.setViewport({ width: 1920, height: 1080 });

  page.on('console', (msg) => {
    if (msg.type() === 'error' && !msg.text().includes('favicon')) {
      console.log(`    [browser-err] ${msg.text().substring(0, 100)}`);
    }
  });

  try {
    // ============================================
    // F-04: 菜单导航
    // ============================================
    console.log('\n--- F-04: 菜单导航 ---');
    await setupMocks(page);
    await page.goto(`${BASE_URL}/`, { waitUntil:'networkidle0', timeout:30000 });
    await new Promise(r => setTimeout(r, 2000));

    await page.evaluate(() => {
      const items = document.querySelectorAll('.ant-menu-item');
      for (const item of items) { if (item.textContent.includes('审批管理')) { item.click(); return true; } }
      return false;
    });
    await new Promise(r => setTimeout(r, 2000));

    const url = page.url();
    assert(url.includes('/approval'), 'F-04a', `导航到 /approval (URL: ${url})`);

    const highlighted = await page.evaluate(() => {
      const items = document.querySelectorAll('.ant-menu-item-selected, .ant-menu-item-active');
      return Array.from(items).some(item => item.textContent.includes('审批管理'));
    });
    assert(highlighted, 'F-04b', '审批管理菜单项高亮');
    await screenshot(page, 'F04_menu_navigation');

    // ============================================
    // F-01: 默认显示待审批 Tab + 自动加载
    // ============================================
    console.log('\n--- F-01: 默认显示待审批 Tab ---');

    const defaultTab = await page.evaluate(() => {
      const active = document.querySelector('.ant-tabs-tab-active');
      return active?.textContent?.trim() || '';
    });
    assert(defaultTab === '待审批应用', 'F-01a', `默认 Tab = "待审批应用" (实际: "${defaultTab}")`);

    await new Promise(r => setTimeout(r, 1500));

    const rowCount = await getDataRowCount(page);
    assert(rowCount === 3, 'F-01b', `待审批列表 3 条数据 (实际: ${rowCount})`);

    const headers = await getTableHeaders(page);
    const expectedHeaders = ['应用名称','应用能力','版本号','应用ID','申请账号','申请时间','操作'];
    const headersMatch = expectedHeaders.every(h => headers.includes(h));
    assert(headersMatch, 'F-01c', `待审批表头完整 (${headers.join(', ')})`);
    await screenshot(page, 'F01_pending_default');

    // ============================================
    // F-03: 待审批 Tab 操作按钮
    // ============================================
    console.log('\n--- F-03: 待审批 Tab 操作按钮 ---');

    const pendingActions = await getActionButtons(page, 0);
    assert(
      pendingActions.includes('查看') && pendingActions.includes('同意') && pendingActions.includes('拒绝'),
      'F-03a', `待审批操作按钮: 查看/同意/拒绝 (实际: ${pendingActions.join('/')})`
    );
    await screenshot(page, 'F03_pending_actions');

    // ============================================
    // F-02: 切换到已上架 Tab
    // ============================================
    console.log('\n--- F-02: 切换到已上架 Tab ---');
    page._apiCalls = [];

    await clickTab(page, '已上架应用');
    await new Promise(r => setTimeout(r, 2000));

    const activeTab = await page.evaluate(() => {
      const active = document.querySelector('.ant-tabs-tab-active');
      return active?.textContent?.trim() || '';
    });
    assert(activeTab === '已上架应用', 'F-02a', `已上架 Tab 激活 (实际: "${activeTab}")`);

    const pubRowCount = await getDataRowCount(page);
    assert(pubRowCount === 2, 'F-02b', `已上架列表 2 条数据 (实际: ${pubRowCount})`);

    const pubActions = await getActionButtons(page, 0);
    assert(
      pubActions.includes('查看') && !pubActions.includes('同意') && !pubActions.includes('拒绝'),
      'F-02c', `已上架仅显示查看 (实际: ${pubActions.join('/')})`
    );

    const pubApiCalled = page._apiCalls.some(c => c.type === 'published');
    assert(pubApiCalled, 'F-02d', '切换后调用了 publish API');
    await screenshot(page, 'F02_published_tab');

    // ============================================
    // F-13: 已上架 Tab 所有行仅显示查看
    // ============================================
    console.log('\n--- F-13: 已上架 Tab 操作列验证 ---');

    const allPubActions = await page.evaluate(() => {
      const pane = document.querySelector('.ant-tabs-tabpane-active');
      if (!pane) return [];
      const tbody = pane.querySelector('.ant-table-tbody');
      if (!tbody) return [];
      const rows = Array.from(tbody.querySelectorAll('tr')).filter(tr => !tr.classList.contains('ant-table-measure-row'));
      return rows.map(row => {
        const fixCell = row.querySelector('.ant-table-cell-fix-right') || row.querySelector('td:last-child');
        return fixCell ? Array.from(fixCell.querySelectorAll('span')).map(s => s.textContent?.trim()).filter(Boolean) : [];
      });
    });
    const onlyView = allPubActions.every(acts => acts.length === 1 && acts[0] === '查看');
    assert(onlyView, 'F-13', `所有行仅含查看 (${JSON.stringify(allPubActions)})`);

    // ============================================
    // F-03 (续): 切换回待审批
    // ============================================
    console.log('\n--- F-03: 切换回待审批 Tab ---');
    page._apiCalls = [];

    await clickTab(page, '待审批应用');
    await new Promise(r => setTimeout(r, 2000));

    const backTab = await page.evaluate(() => {
      const active = document.querySelector('.ant-tabs-tab-active');
      return active?.textContent?.trim() || '';
    });
    assert(backTab === '待审批应用', 'F-03b', `切换回待审批 (实际: "${backTab}")`);

    const backActions = await getActionButtons(page, 0);
    assert(
      backActions.includes('查看') && backActions.includes('同意') && backActions.includes('拒绝'),
      'F-03c', '切换回后重新显示 查看/同意/拒绝'
    );

    // ============================================
    // F-05: 语言切换
    // ============================================
    console.log('\n--- F-05: 语言切换 ---');

    const cnName = await getCellText(page, 0, 0);
    assert(cnName === '订单管理应用', 'F-05a', `中文显示 appNameCn (实际: "${cnName}")`);

    await page.evaluate(() => {
      const spans = document.querySelectorAll('span');
      for (const s of spans) { if (s.textContent.trim() === 'EN') { s.click(); return; } }
    });
    await new Promise(r => setTimeout(r, 500));

    const enName = await getCellText(page, 0, 0);
    assert(enName === 'Order Management App', 'F-05b', `英文显示 appNameEn (实际: "${enName}")`);

    await page.evaluate(() => {
      const spans = document.querySelectorAll('span');
      for (const s of spans) { if (s.textContent.trim() === '中文') { s.click(); return; } }
    });
    await new Promise(r => setTimeout(r, 500));
    await screenshot(page, 'F05_lang_switch');

    // ============================================
    // F-14/F-15: 时间列标题
    // ============================================
    console.log('\n--- F-14/F-15: 时间列标题 ---');

    const pendingTimeHeader = await page.evaluate(() => {
      const pane = document.querySelector('.ant-tabs-tabpane-active');
      const ths = pane?.querySelectorAll('.ant-table-thead th');
      if (!ths) return '';
      for (const th of ths) {
        const text = th.textContent?.trim();
        if (text === '申请时间' || text === '创建时间') return text;
      }
      return '';
    });
    assert(pendingTimeHeader === '申请时间', 'F-14', `待审批时间列标题 = "申请时间" (实际: "${pendingTimeHeader}")`);

    await clickTab(page, '已上架应用');
    await new Promise(r => setTimeout(r, 2000));

    const publishedTimeHeader = await page.evaluate(() => {
      const pane = document.querySelector('.ant-tabs-tabpane-active');
      const ths = pane?.querySelectorAll('.ant-table-thead th');
      if (!ths) return '';
      for (const th of ths) {
        const text = th.textContent?.trim();
        if (text === '申请时间' || text === '创建时间') return text;
      }
      return '';
    });
    assert(publishedTimeHeader === '创建时间', 'F-15', `已上架时间列标题 = "创建时间" (实际: "${publishedTimeHeader}")`);
    await screenshot(page, 'F15_published_time_header');

    // ============================================
    // F-06: 待审批 Tab 查看 → window.open
    // ============================================
    console.log('\n--- F-06/F-07: 查看按钮 window.open ---');

    await clickTab(page, '待审批应用');
    await new Promise(r => setTimeout(r, 2000));

    await page.evaluate(() => {
      window.__openedUrls = [];
      window.open = (url, target) => { window.__openedUrls.push({ url, target }); };
    });

    const clicked = await clickAction(page, 0, '查看');
    assert(clicked, 'F-06-pre', '成功点击查看按钮');
    await new Promise(r => setTimeout(r, 500));

    const openedUrls = await page.evaluate(() => window.__openedUrls);
    const viewCorrect = openedUrls && openedUrls.length > 0 &&
      openedUrls[0].url === '/app-detail/app_third_party_001' && openedUrls[0].target === '_blank';
    assert(viewCorrect, 'F-06',
      `window.open('/app-detail/app_third_party_001', '_blank') (实际: ${JSON.stringify(openedUrls?.[0])})`);

    // ============================================
    // F-07: 已上架 Tab 查看
    // ============================================
    await clickTab(page, '已上架应用');
    await new Promise(r => setTimeout(r, 2000));

    await page.evaluate(() => {
      window.__openedUrls = [];
      window.open = (url, target) => { window.__openedUrls.push({ url, target }); };
    });

    const pubClicked = await clickAction(page, 0, '查看');
    assert(pubClicked, 'F-07-pre', '成功点击已上架查看按钮');
    await new Promise(r => setTimeout(r, 500));

    const pubOpenedUrls = await page.evaluate(() => window.__openedUrls);
    const pubViewCorrect = pubOpenedUrls && pubOpenedUrls.length > 0 &&
      pubOpenedUrls[0].url === '/app-detail/app_pub_001' && pubOpenedUrls[0].target === '_blank';
    assert(pubViewCorrect, 'F-07',
      `window.open('/app-detail/app_pub_001', '_blank') (实际: ${JSON.stringify(pubOpenedUrls?.[0])})`);
    await screenshot(page, 'F07_published_view');

    // ============================================
    // F-08/F-09: 同意 → 确认/取消
    // ============================================
    console.log('\n--- F-08/F-09: 同意操作 ---');

    await clickTab(page, '待审批应用');
    await new Promise(r => setTimeout(r, 2000));
    page._apiCalls = [];

    // F-09: 取消
    await clickAction(page, 0, '同意');
    await new Promise(r => setTimeout(r, 1000));

    const modalVisible = await page.evaluate(() => {
      const modal = document.querySelector('.ant-modal-confirm');
      return modal ? window.getComputedStyle(modal).display !== 'none' : false;
    });
    assert(modalVisible, 'F-09a', 'Modal.confirm 弹出');

    const modalContent = await page.evaluate(() => {
      const el = document.querySelector('.ant-modal-confirm-content');
      return el?.textContent?.trim() || '';
    });
    const hasAppInfo = modalContent.includes('订单管理应用') && modalContent.includes('v2.1.0') && modalContent.includes('u001');
    assert(hasAppInfo, 'F-09b', `Modal 展示应用信息 (内容: "${modalContent.substring(0, 80)}...")`);

    // 取消
    await page.evaluate(() => {
      const btns = document.querySelectorAll('.ant-modal-confirm-btns button');
      for (const btn of btns) { if (!btn.classList.contains('ant-btn-primary')) { btn.click(); return; } }
      const close = document.querySelector('.ant-modal-close');
      if (close) close.click();
    });
    await new Promise(r => setTimeout(r, 1000));

    const noApiAfterCancel = !page._apiCalls.some(c => c.type === 'approval');
    assert(noApiAfterCancel, 'F-09c', '取消后无 API 调用');
    await screenshot(page, 'F09_approve_cancel');

    // F-08: 确认通过
    page._apiCalls = [];
    await clickAction(page, 0, '同意');
    await new Promise(r => setTimeout(r, 1000));

    await page.evaluate(() => {
      const okBtn = document.querySelector('.ant-modal-confirm-btns button.ant-btn-primary');
      if (okBtn) okBtn.click();
    });
    await new Promise(r => setTimeout(r, 2000));

    const approveCalled = page._apiCalls.some(c => c.type === 'approval' && c.body?.action === 0);
    assert(approveCalled, 'F-08a', '调用 process API (action=0)');

    const approveCall = page._apiCalls.find(c => c.type === 'approval' && c.body?.action === 0);
    assert(approveCall && approveCall.body.id === '1', 'F-08b', `ID 正确传递 (id=${approveCall?.body?.id})`);

    const approveMsg = await page.evaluate(() => {
      const msgs = document.querySelectorAll('.ant-message-notice-content');
      return Array.from(msgs).some(m => m.textContent?.includes('审批通过'));
    });
    assert(approveMsg, 'F-08c', '显示 "审批通过" 提示');

    const pendingReloaded = page._apiCalls.filter(c => c.type === 'pending').length >= 1;
    assert(pendingReloaded, 'F-08d', '列表刷新（重新调用 pending API）');
    await screenshot(page, 'F08_approve_success');

    // ============================================
    // F-10/F-11/F-12: 拒绝 → 弹窗/确认/取消
    // ============================================
    console.log('\n--- F-10/F-11/F-12: 拒绝操作 ---');
    page._apiCalls = [];

    await clickAction(page, 1, '拒绝');
    await new Promise(r => setTimeout(r, 1000));

    const rejectModalVisible = await page.evaluate(() => {
      const modal = document.querySelector('.ant-modal-confirm');
      return modal ? window.getComputedStyle(modal).display !== 'none' : false;
    });
    assert(rejectModalVisible, 'F-10a', '拒绝 Modal.confirm 弹出');

    const rejectContent = await page.evaluate(() => {
      const el = document.querySelector('.ant-modal-confirm-content');
      return el?.textContent?.trim() || '';
    });
    const rejectHasInfo = rejectContent.includes('库存同步工具') || rejectContent.includes('v1.0.0') || rejectContent.includes('u002');
    assert(rejectHasInfo, 'F-10b', `拒绝 Modal 展示应用信息 (内容: "${rejectContent.substring(0, 100)}...")`);

    const rejectBtnDanger = await page.evaluate(() => {
      // Ant Design v4 Modal.confirm with okType='danger': button has class ant-btn-dangerous (NOT ant-btn-primary)
      const dangerBtn = document.querySelector('.ant-modal-confirm-btns button.ant-btn-dangerous');
      return dangerBtn !== null;
    });
    assert(rejectBtnDanger, 'F-10c', '拒绝按钮为 danger 样式 (ant-btn-dangerous)');
    await screenshot(page, 'F10_reject_modal');

    // F-12: 取消
    await page.evaluate(() => {
      const btns = document.querySelectorAll('.ant-modal-confirm-btns button');
      for (const btn of btns) { if (!btn.classList.contains('ant-btn-primary')) { btn.click(); return; } }
      const close = document.querySelector('.ant-modal-close');
      if (close) close.click();
    });
    await new Promise(r => setTimeout(r, 1000));

    const noRejectAfterCancel = !page._apiCalls.some(c => c.type === 'approval');
    assert(noRejectAfterCancel, 'F-12', '拒绝取消后无 API 调用');

    // F-11: 确认拒绝
    page._apiCalls = [];

    // Ensure any lingering modal is closed
    await page.evaluate(() => {
      const modals = document.querySelectorAll('.ant-modal-confirm');
      modals.forEach(m => { if (m.parentNode) m.parentNode.removeChild(m); });
    });
    await new Promise(r => setTimeout(r, 500));

    const rejectClicked = await clickAction(page, 1, '拒绝');
    assert(rejectClicked, 'F-11-pre', '成功点击拒绝按钮');
    await new Promise(r => setTimeout(r, 1000));

    // Verify modal opened
    const f11ModalVisible = await page.evaluate(() => {
      const modal = document.querySelector('.ant-modal-confirm');
      return modal ? window.getComputedStyle(modal).display !== 'none' : false;
    });
    assert(f11ModalVisible, 'F-11-modal', '拒绝 Modal 再次弹出');

    await page.evaluate(() => {
      // Reject button has class ant-btn-dangerous (not ant-btn-primary)
      const okBtn = document.querySelector('.ant-modal-confirm-btns button.ant-btn-dangerous');
      if (okBtn) okBtn.click();
    });
    await new Promise(r => setTimeout(r, 2000));

    const rejectCalled = page._apiCalls.some(c => c.type === 'approval' && c.body?.action === 1);
    assert(rejectCalled, 'F-11a', '调用 process API (action=1)');

    const rejectCall = page._apiCalls.find(c => c.type === 'approval' && c.body?.action === 1);
    assert(rejectCall && rejectCall.body.id === '2', 'F-11b', `拒绝 ID 正确传递 (id=${rejectCall?.body?.id})`);

    const rejectMsg = await page.evaluate(() => {
      const msgs = document.querySelectorAll('.ant-message-notice-content');
      return Array.from(msgs).some(m => m.textContent?.includes('已拒绝'));
    });
    assert(rejectMsg, 'F-11c', '显示 "已拒绝" 提示');
    await screenshot(page, 'F11_reject_success');

    await screenshot(page, 'final_state');

  } catch (err) {
    console.error('\n测试出错:', err.message);
    await screenshot(page, 'error');
  } finally {
    await browser.close();
  }

  // ===================== 测试报告 =====================
  console.log('\n========================================');
  console.log('  测试报告');
  console.log('========================================');
  console.log(`  总计: ${passCount + failCount} 条`);
  console.log(`  通过: ${passCount} 条 ✓`);
  console.log(`  失败: ${failCount} 条 ✗`);
  console.log('----------------------------------------');
  results.forEach(r => {
    console.log(`  ${r.status === 'PASS' ? '✓' : '✗'} [${r.testId}] ${r.description}`);
  });
  console.log('========================================\n');

  const indexPath = path.join(screenshotDir, 'approval_test_report.txt');
  fs.writeFileSync(indexPath,
    `审批管理前端测试报告\n${'='.repeat(50)}\n` +
    results.map(r => `- [${r.status}] ${r.testId}: ${r.description}`).join('\n') +
    `\n\n通过: ${passCount} / 失败: ${failCount}\n`);
  console.log(`报告已保存: ${indexPath}`);
}

runTests().catch(console.error);
