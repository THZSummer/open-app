# 连接流编排页面修改设计方案

## 1. 需求概述

对 `flow-editor.html` 连接流编排页面进行功能增强，包含以下6项修改：

1. **触发器节点配置弹窗** - 新增 SYSTOKEN 凭证白名单，可添加多个
2. **触发器节点配置弹窗** - 新增入参配置（参考连接器入参配置交互）
3. **连接器节点配置弹窗** - 新增版本列表，选择不同版本展示对应出参配置
4. **数据输出节点配置弹窗** - 移除输出格式选择，添加参数组装配置
5. **新增异常处理节点** - 在节点库中添加异常处理节点类型
6. **（编号遗漏）数据处理节点完善** - 完善现有数据处理节点配置

## 2. 技术方案

### 2.1 实现方式

采用**最小改动方案**：在现有节点配置抽屉中添加新的配置项，复用连接器编辑器中的参数配置组件代码。

### 2.2 复用组件

从 `connector-editor.html` 中提取参数配置相关函数：
- `renderParamItem()` - 递归渲染参数项
- `addSiblingParam()` - 添加同级参数
- `addChildParam()` - 添加子参数/同级参数
- `removeParamItem()` - 移除参数项
- `handleParamTypeChange()` - 处理参数类型变更
- `isComplexType()` - 判断是否为复杂类型
- `getCarrierOptions()` - 获取 carrier 选项

### 2.3 数据结构

```javascript
// 触发器节点数据结构
{
    id: 'n1',
    type: 'trigger',
    x: 100,
    y: 50,
    data: {
        label: '触发器',
        description: '流程入口节点',
        triggerType: 'http',           // 触发方式
        systokenWhitelist: [            // SYSTOKEN凭证白名单（新增）
            { id: 't1', value: 'token123' },
            { id: 't2', value: 'token456' }
        ],
        inputParams: {                  // 入参配置（新增）
            header: [],
            body: [],
            query: []
        }
    }
}

// 连接器节点数据结构
{
    id: 'n2',
    type: 'action',
    x: 100,
    y: 200,
    data: {
        label: '连接器',
        description: '连接器节点',
        connectorId: 'c1',             // 选择的连接器
        connectorVersionId: 'v1',       // 选择的版本（新增）
        outputParams: {                 // 出参配置
            header: [],
            body: []
        }
    }
}

// 数据输出节点数据结构
{
    id: 'n3',
    type: 'output',
    x: 100,
    y: 350,
    data: {
        label: '数据输出',
        description: '返回处理结果',
        assembleParams: {               // 参数组装配置（替换输出格式）
            header: [],
            body: []
        }
    }
}

// 异常处理节点（新增）
{
    id: 'n4',
    type: 'error',
    x: 100,
    y: 500,
    data: {
        label: '异常处理',
        description: '处理流程异常',
        errorConfig: {}
    }
}
```

### 2.4 连接器模拟数据

```javascript
// 连接器列表数据
const CONNECTOR_DATA = {
    'c1': {
        id: 'c1',
        name: 'HTTP请求连接器',
        versions: [
            {
                id: 'v1',
                name: 'v1.0.0',
                outputParams: {
                    header: [],
                    body: [
                        { id: 'p1', paramName: 'code', paramType: 'number', description: '状态码' },
                        { id: 'p2', paramName: 'data', paramType: 'object', description: '数据', children: [] }
                    ]
                }
            },
            {
                id: 'v2',
                name: 'v0.9.0',
                outputParams: {
                    header: [],
                    body: [
                        { id: 'p3', paramName: 'result', paramType: 'string', description: '结果' }
                    ]
                }
            }
        ]
    },
    'c2': {
        id: 'c2',
        name: '数据库连接器',
        versions: [
            { id: 'v1', name: 'v1.0.0', outputParams: { header: [], body: [] } }
        ]
    }
};
```

## 3. 详细设计

### 3.1 触发器节点配置弹窗修改

**位置**：`openDrawer()` 函数中 `node.type === 'trigger'` 分支

**新增内容**：

```
1. SYSTOKEN凭证白名单（新增分组）
   - 凭证值输入框 + 删除按钮
   - 添加凭证按钮
   - 支持新增、删除多个凭证

2. 入参配置（新增分组，参考connector-editor.html交互）
   - Tab导航：HTTP请求头 | HTTP请求体 | URL查询参数
   - 每个Tab下使用 renderParamItem() 渲染参数列表
   - 支持添加、删除、修改参数
   - 支持参数类型：string/number/boolean/object/array
   - object和array类型支持嵌套子参数
```

**UI布局**：
```
┌─────────────────────────────────────────┐
│ 节点配置                            ✕   │
├─────────────────────────────────────────┤
│ 节点名称    [___________________]       │
│ 节点描述    [___________________]       │
│ 触发方式    [HTTP触发        ▼]         │
├─────────────────────────────────────────┤
│ ▼ SYSTOKEN凭证白名单                    │
│   ┌─────────────────────┐ ┌──────┐     │
│   │ token_value_1       │ │ 删除 │     │
│   └─────────────────────┘ └──────┘     │
│   ┌─────────────────────┐ ┌──────┐     │
│   │ token_value_2       │ │ 删除 │     │
│   └─────────────────────┘ └──────┘     │
│   [+ 添加凭证]                          │
├─────────────────────────────────────────┤
│ ▼ 入参配置                              │
│   [HTTP请求头] [HTTP请求体] [URL查询参数] │
│   ┌───────────────────────────────┐     │
│   │ 参数名称   │类型│描述   │操作│     │
│   │ param1    │str │       │删除│     │
│   └───────────────────────────────┘     │
│   [添加参数]                            │
└─────────────────────────────────────────┘
```

### 3.2 连接器节点配置弹窗修改

**位置**：`openDrawer()` 函数中 `node.type === 'action'` 分支

**修改内容**：

```
1. 选择连接器（下拉框，变更）
   - 变更后清空已选版本和出参配置

2. 选择版本（下拉框，新增）
   - 根据选择的连接器动态显示版本列表
   - 选择版本后显示对应版本的出参配置

3. 出参配置（Tab展示，新增/调整）
   - Tab导航：HTTP请求头 | HTTP请求体
   - 使用 renderParamItem() 渲染参数列表
   - 参数可配置（可参考连接器出参配置交互）
```

**UI布局**：
```
┌─────────────────────────────────────────┐
│ 节点配置                            ✕   │
├─────────────────────────────────────────┤
│ 节点名称    [___________________]       │
│ 节点描述    [___________________]       │
│ 选择连接器  [HTTP请求连接器  ▼]         │
│ 选择版本    [v1.0.0           ▼]         │
├─────────────────────────────────────────┤
│ ▼ 出参配置                              │
│   [HTTP请求头] [HTTP请求体]              │
│   ┌───────────────────────────────┐     │
│   │ 参数名称   │类型│载体│描述  │操作│  │
│   │ code      │num │body│状态码 │删除│  │
│   │ data      │obj │body│数据   │删除│  │
│   └───────────────────────────────┘     │
│   [添加参数]                            │
└─────────────────────────────────────────┘
```

### 3.3 数据输出节点配置弹窗修改

**位置**：`openDrawer()` 函数中 `node.type === 'output'` 分支

**修改内容**：

```
1. 移除输出格式选择（下拉框：JSON/XML/纯文本）

2. 新增参数组装配置（参考连接器出参配置交互）
   - Tab导航：HTTP请求头 | HTTP请求体
   - 使用 renderParamItem() 渲染参数列表
   - 支持添加、删除、修改参数
```

**UI布局**：
```
┌─────────────────────────────────────────┐
│ 节点配置                            ✕   │
├─────────────────────────────────────────┤
│ 节点名称    [___________________]       │
│ 节点描述    [___________________]       │
├─────────────────────────────────────────┤
│ ▼ 参数组装配置                          │
│   [HTTP请求头] [HTTP请求体]              │
│   ┌───────────────────────────────┐     │
│   │ 参数名称   │类型│载体│描述  │操作│  │
│   │ result    │obj │body│结果   │删除│  │
│   └───────────────────────────────┘     │
│   [添加参数]                            │
└─────────────────────────────────────────┘
```

### 3.4 新增异常处理节点

**修改位置**：
1. `NODE_TYPES` 数组添加新类型
2. `getNodeColor()` 函数添加颜色映射
3. CSS样式添加 `.node-icon.error` 和 `.flow-node.error`

**节点配置弹窗**：
```
┌─────────────────────────────────────────┐
│ 节点配置                            ✕   │
├─────────────────────────────────────────┤
│ 节点名称    [___________________]       │
│ 节点描述    [___________________]       │
│ 异常处理类型 [继续执行      ▼]         │
│ 错误码      [___________________]       │
│ 错误信息    [___________________]       │
└─────────────────────────────────────────┘
```

### 3.5 保存逻辑修改

**位置**：`saveNodeConfig()` 函数

**修改内容**：

```javascript
function saveNodeConfig() {
    if (!selectedNode) return;

    // 基础配置
    selectedNode.data.label = document.getElementById('nodeLabel')?.value || '';
    selectedNode.data.description = document.getElementById('nodeDesc')?.value || '';

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
            header: collectParams('trigger-input-params-header'),
            body: collectParams('trigger-input-params-body'),
            query: collectParams('trigger-input-params-query')
        };
    } else if (selectedNode.type === 'action') {
        // 连接器配置
        selectedNode.data.connectorId = document.getElementById('nodeConnector')?.value;
        selectedNode.data.connectorVersionId = document.getElementById('nodeConnectorVersion')?.value;
        selectedNode.data.outputParams = {
            header: collectParams('connector-output-params-header'),
            body: collectParams('connector-output-params-body')
        };
    } else if (selectedNode.type === 'output') {
        // 数据输出配置（移除outputFormat，新增assembleParams）
        selectedNode.data.assembleParams = {
            header: collectParams('output-assemble-params-header'),
            body: collectParams('output-assemble-params-body')
        };
    } else if (selectedNode.type === 'error') {
        // 异常处理配置
        selectedNode.data.errorType = document.getElementById('nodeErrorType')?.value;
        selectedNode.data.errorCode = document.getElementById('nodeErrorCode')?.value;
        selectedNode.data.errorMessage = document.getElementById('nodeErrorMessage')?.value;
    }

    closeDrawer();
    renderCanvas();
    showToast('配置已保存', 'success');
}
```

## 4. 实现步骤

### 步骤1：添加辅助函数
- 复制 connector-editor.html 中的参数配置相关函数到 flow-editor.html
- 调整函数命名避免冲突

### 步骤2：添加连接器模拟数据
- 在 flow-editor.html 顶部添加 CONNECTOR_DATA 模拟数据

### 步骤3：修改 NODE_TYPES 数组
- 添加 error 类型节点定义

### 步骤4：修改 getNodeColor() 函数
- 添加 error 类型颜色映射

### 步骤5：添加CSS样式
- 添加 `.flow-node.error` 样式
- 添加分组折叠面板样式

### 步骤6：修改 openDrawer() 函数
- 触发器节点：新增 SYSTOKEN 凭证白名单 + 入参配置
- 连接器节点：新增版本选择 + 出参配置
- 数据输出节点：替换为参数组装配置
- 异常处理节点：新增配置表单

### 步骤7：修改 saveNodeConfig() 函数
- 适配新增的数据结构

### 步骤8：测试验证
- 测试各节点类型的添加、配置、保存功能

## 5. 风险与注意事项

1. **代码复用**：直接从 connector-editor.html 复制参数配置函数，需确保函数独立无依赖
2. **向后兼容**：现有流程数据需要能够正常加载显示
3. **性能考虑**：参数列表使用递归渲染，需注意大数据量场景下的性能
4. **交互一致性**：新增配置与现有配置风格保持一致

## 6. 验收标准

- [ ] 触发器节点可配置多个 SYSTOKEN 凭证
- [ ] 触发器节点可配置入参（Tab切换，参数可添加/删除）
- [ ] 连接器节点可选择不同版本，版本切换后出参配置相应变化
- [ ] 数据输出节点移除输出格式选择，替换为参数组装配置
- [ ] 节点库中显示异常处理节点
- [ ] 异常处理节点可拖拽到画布并配置
- [ ] 所有配置保存后重新打开能正确回显
