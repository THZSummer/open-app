# 连接流编排页面修改实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 对 flow-editor.html 进行功能增强，实现触发器SYSTOKEN凭证白名单、入参配置、连接器版本选择、数据输出参数组装配置，以及新增异常处理节点

**Architecture:** 在现有 flow-editor.html 文件中添加参数配置组件代码，复用 connector-editor.html 的参数配置逻辑，采用分组式配置展示

**Tech Stack:** 纯HTML/CSS/JavaScript，无外部依赖

---

## 文件结构

- Modify: `d:\myProject\open-app\wecodesiteDemo\flow-editor.html` (唯一需要修改的文件)

---

## 实现步骤

### 任务1: 添加辅助函数和常量

**目标**: 从 connector-editor.html 提取参数配置相关函数，添加到 flow-editor.html

- [ ] **Step 1: 在 flow-editor.html 的 `<script>` 标签开头添加参数配置辅助函数**

在 `NODE_TYPES` 定义之前添加以下函数定义：

```javascript
// ========================================
// 参数配置辅助函数（从connector-editor.html复用）
// ========================================

/**
 * 判断参数类型是否为复杂类型（object 或 array）
 * @param {string} paramType - 参数类型
 * @returns {boolean} 是否为复杂类型
 */
function isComplexType(paramType) {
    return paramType === 'object' || paramType === 'array';
}

/**
 * 获取carrier选项HTML
 * @param {string} currentCarrier - 当前选中的carrier值
 * @returns {string} carrier选项HTML
 */
function getCarrierOptions(currentCarrier) {
    const options = ['header', 'body'];
    return options.map(opt =>
        `<option value="${opt}" ${currentCarrier === opt ? 'selected' : ''}>${opt}</option>`
    ).join('');
}

/**
 * 递归渲染参数项（支持无限层级嵌套）
 * @param {Object} param - 参数配置对象
 * @param {number} depth - 当前层级深度
 * @returns {string} 参数项HTML字符串
 */
function renderParamItem(param, depth = 0) {
    const id = param.id || generateId();
    const paramType = param.paramType || 'string';
    const carrier = param.carrier || 'body';
    const isComplex = isComplexType(paramType);
    const childrenParams = param.children || [];

    const indent = 24 + (depth * 16);
    const rowPadding = depth === 0 ? '6px 10px' : '4px 8px';

    const childrenHtml = isComplex && childrenParams.length > 0
        ? childrenParams.map(child => renderParamItem(child, depth + 1)).join('')
        : '';

    const childrenContainerHtml = isComplex
        ? `<div class="children-container" style="margin-left: ${indent}px;">
            ${childrenHtml}
           </div>`
        : '';

    const carrierSelectHtml = `<select class="param-carrier" style="padding: ${rowPadding}; border: 1px solid var(--border-color); border-radius: 4px; font-size: 13px; width: 80px;">
        ${getCarrierOptions(carrier)}
       </select>`;

    const addButtonHtml = isComplex
        ? `<div class="dropdown-menu" style="position: relative; display: inline-block;">
            <button type="button" class="add-item-btn" onclick="toggleItemDropdown(this)" style="padding: ${rowPadding}; border: none; background: transparent;">
                添加
            </button>
            <div class="dropdown-content" style="display: none; position: absolute; background: var(--bg-white); border: 1px solid var(--border-color); border-radius: 4px; box-shadow: var(--shadow-md); z-index: 10; min-width: 120px;">
                <button type="button" onclick="addChildParam(this, 'child')" style="display: block; width: 100%; padding: 8px 12px; border: none; background: transparent; text-align: left; cursor: pointer; font-size: 13px;">
                    添加子节点
                </button>
                <button type="button" onclick="addChildParam(this, 'sibling')" style="display: block; width: 100%; padding: 8px 12px; border: none; background: transparent; text-align: left; cursor: pointer; font-size: 13px;">
                    添加兄弟节点
                </button>
            </div>
           </div>`
        : `<button type="button" class="add-item-btn" onclick="addSiblingParam(this)" style="padding: ${rowPadding}; border: none; background: transparent;">添加</button>`;

    return `
        <div class="param-item" data-id="${id}" data-param-type="${paramType}" data-depth="${depth}">
            <div class="param-row" style="display: flex; align-items: center; gap: 8px;">
                <input type="text" class="param-name" placeholder="参数名称" value="${param.paramName || ''}"
                    style="width: 100px; padding: ${rowPadding}; border: 1px solid var(--border-color); border-radius: 4px; font-size: 13px;">
                <select class="param-type" style="padding: ${rowPadding}; border: 1px solid var(--border-color); border-radius: 4px; font-size: 13px; width: 80px;" onchange="handleParamTypeChange(this)">
                    <option value="string" ${paramType === 'string' ? 'selected' : ''}>string</option>
                    <option value="number" ${paramType === 'number' ? 'selected' : ''}>number</option>
                    <option value="boolean" ${paramType === 'boolean' ? 'selected' : ''}>boolean</option>
                    <option value="object" ${paramType === 'object' ? 'selected' : ''}>object</option>
                    <option value="array" ${paramType === 'array' ? 'selected' : ''}>array</option>
                </select>
                ${carrierSelectHtml}
                <input type="text" class="param-desc" placeholder="描述" value="${param.description || ''}"
                    style="flex: 1; min-width: 80px; padding: ${rowPadding}; border: 1px solid var(--border-color); border-radius: 4px; font-size: 13px;">
                ${addButtonHtml}
                <button type="button" class="param-remove-btn" onclick="removeParamItem(this)">删除</button>
            </div>
            ${childrenContainerHtml}
        </div>
    `;
}

/**
 * 切换添加按钮下拉菜单
 * @param {HTMLElement} btn - 按钮元素
 */
function toggleItemDropdown(btn) {
    const dropdown = btn.closest('.dropdown-menu').querySelector('.dropdown-content');
    dropdown.style.display = dropdown.style.display === 'block' ? 'none' : 'block';
}

/**
 * 添加子参数或同级参数
 * @param {HTMLElement} btn - 添加按钮元素
 * @param {string} type - 添加类型（'child' 或 'sibling'）
 */
function addChildParam(btn, type) {
    btn.closest('.dropdown-content').style.display = 'none';
    const paramItem = btn.closest('.param-item');

    if (type === 'child') {
        const childrenContainer = paramItem.querySelector('.children-container');
        if (childrenContainer) {
            const childHtml = renderParamItem({}, 1);
            childrenContainer.insertAdjacentHTML('beforeend', childHtml);
        }
    } else {
        const parent = paramItem.parentElement;
        if (parent.classList.contains('children-container')) {
            const childHtml = renderParamItem({}, parseInt(paramItem.dataset.depth) || 0);
            parent.insertAdjacentHTML('beforeend', childHtml);
        }
    }
}

/**
 * 添加同级参数
 * @param {HTMLElement} btn - 添加按钮元素
 */
function addSiblingParam(btn) {
    const paramItem = btn.closest('.param-item');
    const parent = paramItem.parentElement;

    if (parent.classList.contains('children-container')) {
        const newParam = renderParamItem({}, parseInt(paramItem.dataset.depth) || 0);
        parent.insertAdjacentHTML('beforeend', newParam);
    } else {
        const schemaList = btn.closest('.schema-list');
        if (schemaList) {
            const newParam = renderParamItem({}, 0);
            schemaList.insertAdjacentHTML('beforeend', newParam);
        }
    }
}

/**
 * 移除参数项
 * @param {HTMLElement} btn - 移除按钮元素
 */
function removeParamItem(btn) {
    btn.closest('.param-item').remove();
}

/**
 * 处理参数类型变更
 * @param {HTMLElement} selectElement - 类型选择器元素
 */
function handleParamTypeChange(selectElement) {
    const paramItem = selectElement.closest('.param-item');
    const newType = selectElement.value;
    const isComplex = isComplexType(newType);
    const depth = parseInt(paramItem.dataset.depth) || 0;
    const rowPadding = depth === 0 ? '6px 10px' : '4px 8px';

    const existingChildren = paramItem.querySelector('.children-container');
    if (existingChildren) existingChildren.remove();

    const existingAddBtn = paramItem.querySelector('.add-item-btn');
    const parentDropdown = existingAddBtn?.closest('.dropdown-menu');

    if (isComplex) {
        if (!parentDropdown) {
            const newAddBtn = document.createElement('div');
            newAddBtn.className = 'dropdown-menu';
            newAddBtn.style.cssText = 'position: relative; display: inline-block;';
            newAddBtn.innerHTML = `
                <button type="button" class="add-item-btn" onclick="toggleItemDropdown(this)" style="padding: ${rowPadding}; border: none; background: transparent;">
                    添加
                </button>
                <div class="dropdown-content" style="display: none; position: absolute; background: var(--bg-white); border: 1px solid var(--border-color); border-radius: 4px; box-shadow: var(--shadow-md); z-index: 10; min-width: 120px;">
                    <button type="button" onclick="addChildParam(this, 'child')" style="display: block; width: 100%; padding: 8px 12px; border: none; background: transparent; text-align: left; cursor: pointer; font-size: 13px;">
                        添加子节点
                    </button>
                    <button type="button" onclick="addChildParam(this, 'sibling')" style="display: block; width: 100%; padding: 8px 12px; border: none; background: transparent; text-align: left; cursor: pointer; font-size: 13px;">
                        添加兄弟节点
                    </button>
                </div>
            `;
            existingAddBtn.replaceWith(newAddBtn);
        }
    } else {
        if (parentDropdown) {
            const newAddBtn = document.createElement('button');
            newAddBtn.type = 'button';
            newAddBtn.className = 'add-item-btn';
            newAddBtn.onclick = function() { addSiblingParam(this); };
            newAddBtn.style.cssText = `padding: ${rowPadding}; border: none; background: transparent;`;
            newAddBtn.textContent = '添加';
            parentDropdown.replaceWith(newAddBtn);
        }
    }

    if (isComplex) {
        const indent = 24 + (depth * 16);
        const childrenContainer = document.createElement('div');
        childrenContainer.className = 'children-container';
        childrenContainer.style.cssText = `margin-left: ${indent}px;`;
        paramItem.appendChild(childrenContainer);
    }

    paramItem.dataset.paramType = newType;
}

/**
 * 递归收集参数数据
 * @param {HTMLElement} container - 容器元素
 * @returns {Array} 参数字数组
 */
function collectParams(container) {
    const paramItems = container.querySelectorAll(':scope > .param-item');
    const params = [];

    paramItems.forEach(item => {
        const name = item.querySelector('.param-name').value.trim();
        const paramType = item.querySelector('.param-type').value;
        const isComplex = isComplexType(paramType);

        if (name) {
            const paramData = {
                id: item.dataset.id,
                paramName: name,
                paramType: paramType,
                carrier: item.querySelector('.param-carrier').value,
                description: item.querySelector('.param-desc').value.trim(),
                children: []
            };

            if (isComplex) {
                const childrenContainer = item.querySelector('.children-container');
                if (childrenContainer) {
                    paramData.children = collectParams(childrenContainer);
                }
            }

            params.push(paramData);
        }
    });

    return params;
}

/**
 * 渲染参数列表
 * @param {string} containerId - 容器ID
 * @param {Array} params - 参数数组
 */
function renderParams(containerId, params) {
    const container = document.getElementById(containerId);
    if (!container) return;

    if (params.length === 0) {
        container.innerHTML = '';
    } else {
        container.innerHTML = params.map(p => renderParamItem(p, 0)).join('');
    }
}
```

- [ ] **Step 2: 在辅助函数后添加连接器模拟数据**

```javascript
// ========================================
// 连接器模拟数据
// ========================================
const CONNECTOR_DATA = {
    'c1': {
        id: 'c1',
        name: 'HTTP请求连接器',
        versions: [
            {
                id: 'v1',
                name: 'v1.0.0',
                outputParams: {
                    header: [
                        { id: 'p1', paramName: 'Content-Type', paramType: 'string', carrier: 'header', description: '内容类型' }
                    ],
                    body: [
                        { id: 'p2', paramName: 'code', paramType: 'number', carrier: 'body', description: '状态码' },
                        { id: 'p3', paramName: 'data', paramType: 'object', carrier: 'body', description: '响应数据', children: [
                            { id: 'p4', paramName: 'id', paramType: 'number', carrier: 'body', description: '数据ID' },
                            { id: 'p5', paramName: 'name', paramType: 'string', carrier: 'body', description: '数据名称' }
                        ]}
                    ]
                }
            },
            {
                id: 'v2',
                name: 'v0.9.0',
                outputParams: {
                    header: [],
                    body: [
                        { id: 'p6', paramName: 'result', paramType: 'string', carrier: 'body', description: '结果' }
                    ]
                }
            }
        ]
    },
    'c2': {
        id: 'c2',
        name: '数据库连接器',
        versions: [
            {
                id: 'v1',
                name: 'v1.0.0',
                outputParams: {
                    header: [],
                    body: [
                        { id: 'p7', paramName: 'rows', paramType: 'array', carrier: 'body', description: '查询结果', children: [] }
                    ]
                }
            }
        ]
    },
    'c3': {
        id: 'c3',
        name: 'Redis缓存连接器',
        versions: [
            {
                id: 'v1',
                name: 'v1.0.0',
                outputParams: {
                    header: [],
                    body: [
                        { id: 'p8', paramName: 'value', paramType: 'string', carrier: 'body', description: '缓存值' }
                    ]
                }
            }
        ]
    }
};
```

---

### 任务2: 修改 NODE_TYPES 添加异常处理节点

**目标**: 在节点类型数组中添加 error 类型

- [ ] **Step 1: 在 NODE_TYPES 数组末尾添加异常处理节点定义**

找到 `NODE_TYPES` 数组定义（约在第803行），在最后一个元素后添加：

```javascript
,{
    type: 'error',
    name: '异常处理',
    desc: '处理流程异常',
    icon: '❌'
}
```

---

### 任务3: 修改 getNodeColor 函数添加异常处理节点颜色

**目标**: 添加 error 类型的颜色映射

- [ ] **Step 1: 在 getNodeColor 函数中添加 error 类型**

找到 `getNodeColor` 函数（约在第1281行），在 colors 对象中添加：

```javascript
error: '#ff4d4f'
```

---

### 任务4: 添加CSS样式

**目标**: 添加异常处理节点和参数配置相关样式

- [ ] **Step 1: 在 CSS 样式区域的 `.node-icon.output` 后添加新样式**

在第281行 `.node-icon.output` 后添加：

```css
.node-icon.error { background: linear-gradient(135deg, #ff4d4f, #cf1322); }

/* ========================================
   参数配置样式
   ======================================== */
.schema-list {
    margin-bottom: 12px;
}

.param-tabs {
    display: flex;
    gap: 8px;
    margin-bottom: 16px;
    border-bottom: 1px solid var(--border-color);
    padding-bottom: 8px;
}

.tab-btn {
    padding: 8px 16px;
    border: none;
    background: transparent;
    cursor: pointer;
    font-size: 14px;
    color: var(--text-secondary);
    border-radius: 4px;
    transition: all 0.3s;
}

.tab-btn:hover {
    color: var(--primary-color);
    background: var(--bg-light);
}

.tab-btn.active {
    color: var(--primary-color);
    background: #e6f7ff;
    font-weight: 500;
}

.param-tab-content .tab-panel {
    display: none;
}

.param-tab-content .tab-panel.active {
    display: block;
}

.param-item .param-row {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 8px;
}

.add-param-btn {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    padding: 6px 12px;
    border: 1px dashed var(--border-color);
    background: transparent;
    border-radius: 4px;
    cursor: pointer;
    font-size: 13px;
    color: var(--text-secondary);
    transition: all 0.3s;
}

.add-param-btn:hover {
    border-color: var(--primary-color);
    color: var(--primary-color);
}

.add-item-btn {
    padding: 4px 8px;
    border: 1px dashed var(--border-color);
    background: transparent;
    color: var(--primary-color);
    cursor: pointer;
    border-radius: 4px;
    font-size: 13px;
    transition: all 0.3s;
    flex-shrink: 0;
}

.add-item-btn:hover {
    border-color: var(--primary-color);
    background: #e6f7ff;
}

.param-remove-btn {
    padding: 4px 8px;
    border: none;
    background: transparent;
    color: var(--error-color);
    cursor: pointer;
    border-radius: 4px;
    font-size: 13px;
    transition: all 0.3s;
}

.param-remove-btn:hover {
    background: #fff1f0;
}

/* ========================================
   SYSTOKEN凭证白名单样式
   ======================================== */
.systoken-list {
    display: flex;
    flex-direction: column;
    gap: 8px;
}

.systoken-item {
    display: flex;
    gap: 8px;
    align-items: center;
}

.systoken-item input {
    flex: 1;
}

.systoken-add-btn {
    margin-top: 8px;
}
```

---

### 任务5: 修改 openDrawer 函数

**目标**: 修改节点配置抽屉的内容，适配新的配置项

- [ ] **Step 1: 替换 openDrawer 函数中的节点配置表单**

找到 `openDrawer` 函数（约在第1405行），将其整个内容替换为以下代码：

```javascript
function openDrawer(node) {
    const drawer = document.getElementById('configDrawer');
    const body = document.getElementById('drawerBody');

    let configHtml = '';

    // 根据节点类型生成配置表单
    if (node.type === 'trigger') {
        // 触发器节点配置
        const systokenList = (node.data.systokenWhitelist || []).map((item, index) => `
            <div class="systoken-item">
                <input type="text" class="form-input systoken-input" value="${item.value || ''}" placeholder="请输入凭证值">
                <button type="button" class="btn" onclick="this.parentElement.remove()" style="padding: 6px 12px;">删除</button>
            </div>
        `).join('');

        const inputParams = node.data.inputParams || { header: [], body: [], query: [] };

        configHtml = `
            <div class="form-group">
                <label class="form-label">节点名称</label>
                <input type="text" class="form-input" id="nodeLabel" value="${node.data.label || ''}">
            </div>
            <div class="form-group">
                <label class="form-label">节点描述</label>
                <textarea class="form-textarea" id="nodeDesc">${node.data.description || ''}</textarea>
            </div>
            <div class="form-group">
                <label class="form-label">触发方式</label>
                <select class="form-select" id="nodeTriggerType">
                    <option value="http" ${node.data.triggerType === 'http' ? 'selected' : ''}>HTTP触发</option>
                    <option value="schedule" ${node.data.triggerType === 'schedule' ? 'selected' : ''}>定时触发</option>
                    <option value="event" ${node.data.triggerType === 'event' ? 'selected' : ''}>事件触发</option>
                </select>
            </div>

            <!-- SYSTOKEN凭证白名单 -->
            <div class="form-group" style="margin-top: 24px;">
                <label class="form-label" style="font-weight: 600;">SYSTOKEN凭证白名单</label>
                <div class="systoken-list" id="systokenList">
                    ${systokenList}
                </div>
                <button type="button" class="add-param-btn systoken-add-btn" onclick="addSystokenItem()">
                    + 添加凭证
                </button>
            </div>

            <!-- 入参配置 -->
            <div class="form-group" style="margin-top: 24px;">
                <label class="form-label" style="font-weight: 600;">入参配置</label>
                <div class="param-tabs" id="triggerInputTabs">
                    <button class="tab-btn active" onclick="switchTriggerInputTab('header')">HTTP请求头</button>
                    <button class="tab-btn" onclick="switchTriggerInputTab('body')">HTTP请求体</button>
                    <button class="tab-btn" onclick="switchTriggerInputTab('query')">URL查询参数</button>
                </div>
                <div class="param-tab-content" id="triggerInputContent">
                    <div class="tab-panel active" data-tab="header">
                        <div class="schema-list" id="trigger-input-params-header"></div>
                        <button type="button" class="add-param-btn" onclick="addTriggerInputParam('header')">+ 添加参数</button>
                    </div>
                    <div class="tab-panel" data-tab="body">
                        <div class="schema-list" id="trigger-input-params-body"></div>
                        <button type="button" class="add-param-btn" onclick="addTriggerInputParam('body')">+ 添加参数</button>
                    </div>
                    <div class="tab-panel" data-tab="query">
                        <div class="schema-list" id="trigger-input-params-query"></div>
                        <button type="button" class="add-param-btn" onclick="addTriggerInputParam('query')">+ 添加参数</button>
                    </div>
                </div>
            </div>
        `;
    } else if (node.type === 'action') {
        // 连接器节点配置
        const connectorOptions = Object.values(CONNECTOR_DATA).map(c =>
            `<option value="${c.id}" ${node.data.connectorId === c.id ? 'selected' : ''}>${c.name}</option>`
        ).join('');

        const currentConnector = CONNECTOR_DATA[node.data.connectorId];
        const versionOptions = currentConnector
            ? currentConnector.versions.map(v =>
                `<option value="${v.id}" ${node.data.connectorVersionId === v.id ? 'selected' : ''}>${v.name}</option>`
              ).join('')
            : '<option value="">请先选择连接器</option>';

        const outputParams = node.data.outputParams || { header: [], body: [] };
        const currentVersion = currentConnector?.versions.find(v => v.id === node.data.connectorVersionId);
        const displayParams = currentVersion?.outputParams || outputParams;

        configHtml = `
            <div class="form-group">
                <label class="form-label">节点名称</label>
                <input type="text" class="form-input" id="nodeLabel" value="${node.data.label || ''}">
            </div>
            <div class="form-group">
                <label class="form-label">节点描述</label>
                <textarea class="form-textarea" id="nodeDesc">${node.data.description || ''}</textarea>
            </div>
            <div class="form-group">
                <label class="form-label">选择连接器</label>
                <select class="form-select" id="nodeConnector" onchange="handleConnectorChange(this.value)">
                    <option value="">请选择连接器</option>
                    ${connectorOptions}
                </select>
            </div>
            <div class="form-group">
                <label class="form-label">选择版本</label>
                <select class="form-select" id="nodeConnectorVersion" onchange="handleConnectorVersionChange(this.value)">
                    ${versionOptions || '<option value="">请先选择连接器</option>'}
                </select>
            </div>

            <!-- 出参配置 -->
            <div class="form-group" style="margin-top: 24px;">
                <label class="form-label" style="font-weight: 600;">出参配置</label>
                <div class="param-tabs" id="connectorOutputTabs">
                    <button class="tab-btn active" onclick="switchConnectorOutputTab('header')">HTTP请求头</button>
                    <button class="tab-btn" onclick="switchConnectorOutputTab('body')">HTTP请求体</button>
                </div>
                <div class="param-tab-content" id="connectorOutputContent">
                    <div class="tab-panel active" data-tab="header">
                        <div class="schema-list" id="connector-output-params-header"></div>
                        <button type="button" class="add-param-btn" onclick="addConnectorOutputParam('header')">+ 添加参数</button>
                    </div>
                    <div class="tab-panel" data-tab="body">
                        <div class="schema-list" id="connector-output-params-body"></div>
                        <button type="button" class="add-param-btn" onclick="addConnectorOutputParam('body')">+ 添加参数</button>
                    </div>
                </div>
            </div>
        `;

        body.innerHTML = configHtml + `
            <div class="form-group" style="margin-top: 24px; padding-top: 16px; border-top: 1px solid var(--border-color);">
                <button class="btn" style="color: var(--error-color); border-color: var(--error-color);" onclick="deleteSelectedNode()">
                    删除节点
                </button>
            </div>
        `;

        drawer.classList.add('active');

        // 渲染出参配置
        renderParams('connector-output-params-header', displayParams.header || []);
        renderParams('connector-output-params-body', displayParams.body || []);

        return;
    } else if (node.type === 'output') {
        // 数据输出节点配置
        const assembleParams = node.data.assembleParams || { header: [], body: [] };

        configHtml = `
            <div class="form-group">
                <label class="form-label">节点名称</label>
                <input type="text" class="form-input" id="nodeLabel" value="${node.data.label || ''}">
            </div>
            <div class="form-group">
                <label class="form-label">节点描述</label>
                <textarea class="form-textarea" id="nodeDesc">${node.data.description || ''}</textarea>
            </div>

            <!-- 参数组装配置 -->
            <div class="form-group" style="margin-top: 24px;">
                <label class="form-label" style="font-weight: 600;">参数组装配置</label>
                <div class="param-tabs" id="outputAssembleTabs">
                    <button class="tab-btn active" onclick="switchOutputAssembleTab('header')">HTTP请求头</button>
                    <button class="tab-btn" onclick="switchOutputAssembleTab('body')">HTTP请求体</button>
                </div>
                <div class="param-tab-content" id="outputAssembleContent">
                    <div class="tab-panel active" data-tab="header">
                        <div class="schema-list" id="output-assemble-params-header"></div>
                        <button type="button" class="add-param-btn" onclick="addOutputAssembleParam('header')">+ 添加参数</button>
                    </div>
                    <div class="tab-panel" data-tab="body">
                        <div class="schema-list" id="output-assemble-params-body"></div>
                        <button type="button" class="add-param-btn" onclick="addOutputAssembleParam('body')">+ 添加参数</button>
                    </div>
                </div>
            </div>
        `;
    } else if (node.type === 'error') {
        // 异常处理节点配置
        configHtml = `
            <div class="form-group">
                <label class="form-label">节点名称</label>
                <input type="text" class="form-input" id="nodeLabel" value="${node.data.label || ''}">
            </div>
            <div class="form-group">
                <label class="form-label">节点描述</label>
                <textarea class="form-textarea" id="nodeDesc">${node.data.description || ''}</textarea>
            </div>
            <div class="form-group">
                <label class="form-label">异常处理类型</label>
                <select class="form-select" id="nodeErrorType">
                    <option value="continue" ${node.data.errorType === 'continue' ? 'selected' : ''}>继续执行</option>
                    <option value="abort" ${node.data.errorType === 'abort' ? 'selected' : ''}>终止流程</option>
                    <option value="retry" ${node.data.errorType === 'retry' ? 'selected' : ''}>重试</option>
                </select>
            </div>
            <div class="form-group">
                <label class="form-label">错误码</label>
                <input type="text" class="form-input" id="nodeErrorCode" value="${node.data.errorCode || ''}" placeholder="请输入错误码">
            </div>
            <div class="form-group">
                <label class="form-label">错误信息</label>
                <input type="text" class="form-input" id="nodeErrorMessage" value="${node.data.errorMessage || ''}" placeholder="请输入错误信息">
            </div>
        `;
    } else if (node.type === 'data') {
        // 数据处理节点配置
        configHtml = `
            <div class="form-group">
                <label class="form-label">节点名称</label>
                <input type="text" class="form-input" id="nodeLabel" value="${node.data.label || ''}">
            </div>
            <div class="form-group">
                <label class="form-label">节点描述</label>
                <textarea class="form-textarea" id="nodeDesc">${node.data.description || ''}</textarea>
            </div>
            <div class="form-group">
                <label class="form-label">数据转换表达式</label>
                <textarea class="form-textarea" id="nodeDataTransform" placeholder="如: input.map(item => item.value)">${node.data.transform || ''}</textarea>
            </div>
            <div class="form-group">
                <label class="form-label">输出字段</label>
                <input type="text" class="form-input" id="nodeOutputField" value="${node.data.outputField || ''}" placeholder="请输入输出字段名">
            </div>
        `;
    }

    // 通用底部删除按钮
    const deleteButton = (node.type !== 'trigger') ? `
        <div class="form-group" style="margin-top: 24px; padding-top: 16px; border-top: 1px solid var(--border-color);">
            <button class="btn" style="color: var(--error-color); border-color: var(--error-color);" onclick="deleteSelectedNode()">
                删除节点
            </button>
        </div>
    ` : '';

    body.innerHTML = configHtml + deleteButton;

    drawer.classList.add('active');

    // 触发器节点：渲染入参配置
    if (node.type === 'trigger') {
        const inputParams = node.data.inputParams || { header: [], body: [], query: [] };
        renderParams('trigger-input-params-header', inputParams.header || []);
        renderParams('trigger-input-params-body', inputParams.body || []);
        renderParams('trigger-input-params-query', inputParams.query || []);
    }

    // 数据输出节点：渲染参数组装配置
    if (node.type === 'output') {
        const assembleParams = node.data.assembleParams || { header: [], body: [] };
        renderParams('output-assemble-params-header', assembleParams.header || []);
        renderParams('output-assemble-params-body', assembleParams.body || []);
    }
}
```

- [ ] **Step 2: 在文件末尾添加 Tab 切换和参数添加辅助函数**

在 `</script>` 标签之前添加以下函数：

```javascript
// ========================================
// Tab切换和参数添加辅助函数
// ========================================

/**
 * 切换触发器入参Tab
 * @param {string} tabName - Tab名称
 */
function switchTriggerInputTab(tabName) {
    document.querySelectorAll('#triggerInputTabs .tab-btn').forEach(btn => {
        btn.classList.toggle('active', btn.textContent.includes(
            tabName === 'header' ? 'HTTP请求头' :
            tabName === 'body' ? 'HTTP请求体' : 'URL查询参数'
        ) || (tabName === 'header' && btn.textContent === 'HTTP请求头') ||
           (tabName === 'body' && btn.textContent === 'HTTP请求体') ||
           (tabName === 'query' && btn.textContent === 'URL查询参数'));
    });
    document.querySelectorAll('#triggerInputContent .tab-panel').forEach(panel => {
        panel.classList.toggle('active', panel.dataset.tab === tabName);
    });
}

/**
 * 添加触发器入参参数
 * @param {string} tabName - Tab名称
 */
function addTriggerInputParam(tabName) {
    const container = document.getElementById(`trigger-input-params-${tabName}`);
    if (container) {
        container.insertAdjacentHTML('beforeend', renderParamItem({}, 0));
    }
}

/**
 * 切换连接器出参Tab
 * @param {string} tabName - Tab名称
 */
function switchConnectorOutputTab(tabName) {
    document.querySelectorAll('#connectorOutputTabs .tab-btn').forEach(btn => {
        btn.classList.toggle('active',
            (tabName === 'header' && btn.textContent === 'HTTP请求头') ||
            (tabName === 'body' && btn.textContent === 'HTTP请求体'));
    });
    document.querySelectorAll('#connectorOutputContent .tab-panel').forEach(panel => {
        panel.classList.toggle('active', panel.dataset.tab === tabName);
    });
}

/**
 * 添加连接器出参参数
 * @param {string} tabName - Tab名称
 */
function addConnectorOutputParam(tabName) {
    const container = document.getElementById(`connector-output-params-${tabName}`);
    if (container) {
        container.insertAdjacentHTML('beforeend', renderParamItem({}, 0));
    }
}

/**
 * 切换数据输出参数组装Tab
 * @param {string} tabName - Tab名称
 */
function switchOutputAssembleTab(tabName) {
    document.querySelectorAll('#outputAssembleTabs .tab-btn').forEach(btn => {
        btn.classList.toggle('active',
            (tabName === 'header' && btn.textContent === 'HTTP请求头') ||
            (tabName === 'body' && btn.textContent === 'HTTP请求体'));
    });
    document.querySelectorAll('#outputAssembleContent .tab-panel').forEach(panel => {
        panel.classList.toggle('active', panel.dataset.tab === tabName);
    });
}

/**
 * 添加数据输出参数组装参数
 * @param {string} tabName - Tab名称
 */
function addOutputAssembleParam(tabName) {
    const container = document.getElementById(`output-assemble-params-${tabName}`);
    if (container) {
        container.insertAdjacentHTML('beforeend', renderParamItem({}, 0));
    }
}

/**
 * 添加SYSTOKEN凭证项
 */
function addSystokenItem() {
    const list = document.getElementById('systokenList');
    const item = document.createElement('div');
    item.className = 'systoken-item';
    item.innerHTML = `
        <input type="text" class="form-input systoken-input" placeholder="请输入凭证值">
        <button type="button" class="btn" onclick="this.parentElement.remove()" style="padding: 6px 12px;">删除</button>
    `;
    list.appendChild(item);
}

/**
 * 处理连接器变更
 * @param {string} connectorId - 连接器ID
 */
function handleConnectorChange(connectorId) {
    const versionSelect = document.getElementById('nodeConnectorVersion');
    const connector = CONNECTOR_DATA[connectorId];

    if (connector) {
        versionSelect.innerHTML = connector.versions.map(v =>
            `<option value="${v.id}">${v.name}</option>`
        ).join('');
    } else {
        versionSelect.innerHTML = '<option value="">请先选择连接器</option>';
    }
}

/**
 * 处理连接器版本变更
 * @param {string} versionId - 版本ID
 */
function handleConnectorVersionChange(versionId) {
    const connectorId = document.getElementById('nodeConnector').value;
    const connector = CONNECTOR_DATA[connectorId];
    const version = connector?.versions.find(v => v.id === versionId);

    if (version) {
        renderParams('connector-output-params-header', version.outputParams.header || []);
        renderParams('connector-output-params-body', version.outputParams.body || []);
    }
}
```

---

### 任务6: 修改 saveNodeConfig 函数

**目标**: 修改保存逻辑，适配新的数据结构

- [ ] **Step 1: 替换 saveNodeConfig 函数**

找到 `saveNodeConfig` 函数（约在第1481行），将其整个内容替换为：

```javascript
function saveNodeConfig() {
    if (!selectedNode) return;

    // 基础配置
    selectedNode.data.label = document.getElementById('nodeLabel')?.value || '';
    selectedNode.data.description = document.getElementById('nodeDesc')?.value || '';

    // 根据节点类型保存特定配置
    if (selectedNode.type === 'trigger') {
        // 触发器配置
        selectedNode.data.triggerType = document.getElementById('nodeTriggerType')?.value;

        // SYSTOKEN凭证白名单
        const systokenInputs = document.querySelectorAll('.systoken-input');
        selectedNode.data.systokenWhitelist = Array.from(systokenInputs).map((input, index) => ({
            id: `t${index}`,
            value: input.value.trim()
        })).filter(item => item.value);

        // 入参配置
        selectedNode.data.inputParams = {
            header: collectParams(document.getElementById('trigger-input-params-header')),
            body: collectParams(document.getElementById('trigger-input-params-body')),
            query: collectParams(document.getElementById('trigger-input-params-query'))
        };
    } else if (selectedNode.type === 'action') {
        // 连接器配置
        selectedNode.data.connectorId = document.getElementById('nodeConnector')?.value;
        selectedNode.data.connectorVersionId = document.getElementById('nodeConnectorVersion')?.value;
        selectedNode.data.outputParams = {
            header: collectParams(document.getElementById('connector-output-params-header')),
            body: collectParams(document.getElementById('connector-output-params-body'))
        };
    } else if (selectedNode.type === 'output') {
        // 数据输出配置
        selectedNode.data.assembleParams = {
            header: collectParams(document.getElementById('output-assemble-params-header')),
            body: collectParams(document.getElementById('output-assemble-params-body'))
        };
    } else if (selectedNode.type === 'error') {
        // 异常处理配置
        selectedNode.data.errorType = document.getElementById('nodeErrorType')?.value;
        selectedNode.data.errorCode = document.getElementById('nodeErrorCode')?.value;
        selectedNode.data.errorMessage = document.getElementById('nodeErrorMessage')?.value;
    } else if (selectedNode.type === 'data') {
        // 数据处理配置
        selectedNode.data.transform = document.getElementById('nodeDataTransform')?.value;
        selectedNode.data.outputField = document.getElementById('nodeOutputField')?.value;
    }

    closeDrawer();
    renderCanvas();
    showToast('配置已保存', 'success');
}
```

---

### 任务7: 修改调试功能

**目标**: 更新调试弹窗，从触发器入参配置中读取参数

- [ ] **Step 1: 修改 openDebugModal 函数**

找到 `openDebugModal` 函数（约在第1110行），将其内容替换为：

```javascript
function openDebugModal() {
    // 获取触发器节点的入参配置
    const triggerNode = nodes.find(n => n.type === 'trigger');

    if (!triggerNode) {
        showToast('未找到触发器节点', 'error');
        return;
    }

    const inputParams = triggerNode.data.inputParams || { header: [], body: [], query: [] };
    const allParams = [...inputParams.header, ...inputParams.body, ...inputParams.query];

    if (allParams.length === 0) {
        showToast('触发器未配置入参', 'error');
        return;
    }

    const paramsList = document.getElementById('debugParamsList');
    paramsList.innerHTML = allParams.map(param => `
        <div style="display: flex; align-items: center; gap: 12px; margin-bottom: 12px;">
            <div style="flex: 1;">
                <div style="font-size: 13px; font-weight: 500; margin-bottom: 4px;">
                    ${param.paramName} <span style="color: var(--text-tertiary); font-weight: normal;">(${param.paramType})</span>
                </div>
                <input type="text"
                       class="debug-param-value"
                       data-param-name="${param.paramName}"
                       data-param-type="${param.paramType}"
                       placeholder="请输入参数值"
                       style="width: 100%; padding: 8px 12px; border: 1px solid var(--border-color); border-radius: 4px; font-size: 13px;">
            </div>
        </div>
    `).join('');

    // 清空输出
    document.getElementById('debugOutput').innerHTML = '<span style="color: var(--text-tertiary);">点击"立即调试"查看执行结果...</span>';

    openModal('debugModal');
}
```

---

## 自检清单

- [ ] 所有函数命名一致，无拼写错误
- [ ] 参数容器ID在添加和收集函数中一致
- [ ] Tab切换函数的Tab名称与data-tab属性一致
- [ ] 保存函数正确收集各类型的配置数据
- [ ] 异常处理节点图标和颜色正确显示
- [ ] 调试弹窗正确读取触发器入参配置

---

**计划完成并保存至**: `d:\myProject\open-app\docs\superpowers\plans\2026-06-04-wecodesite-flow-editor-modify.md`

两种执行方式：

1. **Subagent-Driven (推荐)** - 我将为每个任务启动专门的子代理，逐个审查任务
2. **Inline Execution** - 在当前会话中批量执行任务，带检查点

您希望采用哪种方式？