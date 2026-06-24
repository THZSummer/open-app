# 数据结构转换方法对比文档

## 概述

本文档对比分析三个核心文件中的数据结构转换方法：
- `ConnectorEditor/thunk.js` - 连接器编辑页 API 调用
- `FlowEditor/thunk.js` - 连接流管理 API 调用
- `flowUtils.js` - 流程工具函数库（核心转换库）

---

## 一、ID生成与位置工具

| 文件 | 方法名 | 功能 | 参数 | 返回值 | 使用场景 |
|------|--------|------|------|--------|----------|
| flowUtils.js | `generateNodeId` | 生成唯一节点ID | `prefix?: string` (默认'node') | `string` 格式: `prefix_timestamp_random` | 创建新节点时 |
| flowUtils.js | `generateEdgeId` | 生成唯一连线ID | 无 | `string` 格式: `edge_timestamp_random` | 创建新连线时 |
| flowUtils.js | `getInitialNodePosition` | 网格对齐节点位置 | `x?: number, y?: number` (默认100) | `{x: number, y: number}` | 拖拽节点释放时 |

---

## 二、节点关系查询

| 文件 | 方法名 | 功能 | 参数 | 返回值 | 使用场景 |
|------|--------|------|------|--------|----------|
| flowUtils.js | `getUpstreamNodes` | 获取节点的所有上游节点（递归、去重） | `nodeId: string, nodes: Array, edges: Array` | `Array` 上游节点数组 | 获取参数映射可选源 |
| flowUtils.js | `getNodeOutputParams` | 获取节点的输出参数 | `node: Object` | `Array` 参数数组 | SchemaEditor加载参数 |
| flowUtils.js | `getUpstreamParams` | 获取上游节点及其参数列表 | `nodeId: string, nodes: Array, edges: Array` | `Array<{nodeName, nodeId, nodeType, params}>` | 参数映射下拉选项 |

---

## 三、前后端数据转换

### 3.1 连接器配置转换 (ConnectorEditor/thunk.js)

| 方法名 | 功能 | 数据流向 | 参数 | 返回值 |
|--------|------|---------|------|--------|
| `transformJsonSchemaToParams` | JSON Schema → SchemaEditor参数数组 | 后端 → 前端 | `jsonSchema: Object, defaultCarrier?: string` | `Array` |
| `transformAuthFieldsToParams` | 认证字段 → SchemaEditor格式 | 后端 → 前端 | `fields: Array` | `Array` |
| `transformFromSchemaFormat` | 后端4.3格式 → 表单格式 | 后端 → 前端 | `schemaData: Object` | `Object` apiConfig格式 |
| `transformParamToJsonSchemaField` | 单个参数 → JSON Schema字段 | 前端 → 后端 | `param: Object` | `Object` |
| `transformParamsToJsonSchema` | 参数数组 → JSON Schema | 前端 → 后端 | `params: Array` | `Object` |
| `transformAuthFields` | SchemaEditor认证参数 → authConfig.fields | 前端 → 后端 | `authParams: Array` | `Array` |
| `transformToSchemaFormat` | 表单apiConfig → 文档4.3格式 | 前端 → 后端 | `apiConfig: Object` | `Object` |

### 3.2 连接流编排转换 (FlowEditor/thunk.js)

| 方法名 | 功能 | 数据流向 | 参数 | 返回值 |
|--------|------|---------|------|--------|
| `extractUpdatedFields` | 提取需要更新的字段（智能合并） | 处理逻辑 | `{parsedConnectionConfig, currentNode}` | `Object` |
| `smartMergeMapping` | 智能合并mapping配置 | 处理逻辑 | `newMapping: Object, currentMapping: Object` | `Object \| null` |
| `smartMergeNestedProperties` | 智能合并嵌套属性（递归） | 处理逻辑 | `newProperties: Object, currentProperties: Object` | `Object \| null` |
| `smartMergeOutputParams` | 智能合并outputParams | 处理逻辑 | `newOutputParams: Array, currentOutputParams: Array` | `Array \| null` |

### 3.3 通用转换 (flowUtils.js)

| 方法名 | 功能 | 数据流向 | 参数 | 返回值 |
|--------|------|---------|------|--------|
| `transformFromBackend` | 后端编排配置 → React Flow格式 | 后端 → 前端 | `orchestrationConfig: Object` | `{nodes: Array, edges: Array}` |
| `transformToBackend` | React Flow格式 → 后端编排配置 | 前端 → 后端 | `nodes: Array, edges: Array` | `Object` orchestrationConfig |

---

## 四、Mapping数据转换 (flowUtils.js)

### 4.1 数组 ↔ 对象 转换

| 方法名 | 功能 | 输入格式 | 输出格式 | 参数 |
|--------|------|---------|---------|------|
| `transformMappingToNewFormat` | 数组 → 对象 | `[{sourceType, paramName, paramType, ...}]` | `{header: {properties: {...}}, body: {...}, query: {...}}` | `{mappingArray: Array}` |
| `transformInputMappingFromNested` | 对象 → 数组 | `{header: {...}, body: {...}, query: {...}}` | `[{paramName, carrier, sourceType, ...}]` | `inputMappingObj: Object` |
| `transformOutputMappingFromNested` | 对象 → 数组 | `{header: {...}, body: {...}}` | `[{paramName, carrier, sourceType, ...}]` | `outputMappingObj: Object` |

### 4.2 占位符解析

| 方法名 | 功能 | 输入 | 输出 |
|--------|------|------|------|
| `parseMappingValue` | 解析占位符值 | `${$.node.xxx}` 或 `${$.constant:xxx}` | `{sourceType, referencePath, paramValue}` |

### 4.3 Properties处理

| 方法名 | 功能 | 输入 | 输出 |
|--------|------|------|------|
| `flattenObjectProperties` | 对象 → 平铺数组（带路径） | `{paramName: {type, description, properties}}` | `[{paramName, paramType, paramPath}]` |
| `extractPropertiesFromCarrier` | 提取properties层 | `{type: 'object', properties: {...}}` | `{...}` (properties内容) |
| `extractPropertiesFromMappingData` | 批量提取多个carrier | `{header: {...}, body: {...}}` | `{header: {...}, body: {...}}` (提取后) |
| `extractPropertiesFromNodes` | 批量提取节点列表 | `nodes: [{data: {inputContract: {...}}}]` | `nodes: [{data: {inputContract: {...}}}]` (提取后) |

### 4.4 Properties包装

| 方法名 | 功能 | 输入 | 输出 |
|--------|------|------|------|
| `wrapCarrierData` | 包装成标准格式 | `{fieldName: {...}}` | `{type: 'object', properties: {...}}` |
| `wrapMappingDataCarriers` | 批量包装多个carrier | `{header: {...}, body: {...}}` | `{header: {type: 'object', properties: {...}}}` |

---

## 五、智能合并策略 (FlowEditor/thunk.js)

### 5.1 merge策略说明

```
新配置参数 + 用户已配置的参数值 → 合并结果
```

### 5.2 合并规则

| 规则 | 说明 |
|------|------|
| **保留用户值** | 用户已配置的参数值不会被新配置覆盖 |
| **补充缺失** | 新配置中有但用户未配置的参数会被添加 |
| **更新元数据** | 参数的type、description等元数据会被更新 |
| **删除多余** | 新配置中已删除的参数会从用户配置中移除 |

### 5.3 方法调用链

```
extractUpdatedFields
  ├─> smartMergeMapping (inputContract)
  │     └─> smartMergeNestedProperties (递归)
  └─> smartMergeOutputParams (outputContract)
```

---

## 六、API请求方法

### 6.1 ConnectorEditor/thunk.js

| 方法名 | 功能 | HTTP方法 | 参数 | 返回值 |
|--------|------|---------|------|--------|
| `fetchConnectorConfig` | 获取连接器配置 | GET | `connectorId: string` | `Promise<Object>` |
| `saveConnectorConfig` | 保存连接器配置 | PUT | `connectorId: string, config: Object` | `Promise<Object>` |

### 6.2 FlowEditor/thunk.js

| 方法名 | 功能 | HTTP方法 | 参数 | 返回值 |
|--------|------|---------|------|--------|
| `fetchFlowDetail` | 获取连接流详情 | GET | `flowId: string` | `Promise<Object>` |
| `saveFlowConfig` | 保存连接流编排配置 | PUT | `flowId: string, config: Object` | `Promise<Object>` |

---

## 七、数据流转全景图

```
┌─────────────────────────────────────────────────────────────────┐
│                        后端 API 数据                              │
│  Connector: {protocolConfig, authConfig, inputContract, ...}   │
│  Flow: {orchestrationConfig: {nodes, edges}}                    │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                   ConnectorEditor/thunk.js                       │
│  transformFromSchemaFormat (后端格式 → 表单格式)                   │
│  transformJsonSchemaToParams (JSON Schema → 参数数组)              │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                      flowUtils.js (核心转换库)                    │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ 1. 数据加载 (后端 → 前端)                                    │    │
│  │    transformFromBackend                                    │    │
│  │      └─> extractPropertiesFromNodes                        │    │
│  │            └─> extractPropertiesFromMappingData            │    │
│  │                  └─> extractPropertiesFromCarrier         │    │
│  │                                                           │    │
│  │ 2. 数据保存 (前端 → 后端)                                    │    │
│  │    transformToBackend                                      │    │
│  │      ├─> transformMappingToNewFormat (数组转换)            │    │
│  │      └─> wrapMappingDataCarriers (对象包装)                 │    │
│  │            └─> wrapCarrierData                              │    │
│  │                                                           │    │
│  │ 3. SchemaEditor 编辑                                       │    │
│  │    transformInputMappingFromNested                         │    │
│  │      └─> transformMappingFromNested                        │    │
│  │            └─> processMappingCarrierFields                  │    │
│  │                  └─> processNestedProperties                │    │
│  │                        └─> parseMappingValue                │    │
│  └─────────────────────────────────────────────────────────┘    │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                   FlowEditor/thunk.js                            │
│  extractUpdatedFields (智能合并策略)                              │
│    ├─> smartMergeMapping                                        │
│    │     └─> smartMergeNestedProperties (递归)                  │
│    └─> smartMergeOutputParams                                   │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                      后端 API 存储                               │
└─────────────────────────────────────────────────────────────────┘
```

---

## 八、方法分类速查表

### 8.1 按功能分类

| 分类 | 方法列表 |
|------|----------|
| **ID生成** | `generateNodeId`, `generateEdgeId` |
| **位置处理** | `getInitialNodePosition` |
| **节点查询** | `getUpstreamNodes`, `getNodeOutputParams`, `getUpstreamParams` |
| **前后端转换** | `transformFromBackend`, `transformToBackend`, `transformFromSchemaFormat`, `transformToSchemaFormat` |
| **Mapping转换** | `transformMappingToNewFormat`, `transformInputMappingFromNested`, `transformOutputMappingFromNested` |
| **Properties处理** | `flattenObjectProperties`, `extractPropertiesFromCarrier`, `extractPropertiesFromMappingData`, `extractPropertiesFromNodes`, `wrapCarrierData`, `wrapMappingDataCarriers` |
| **Schema转换** | `transformJsonSchemaToParams`, `transformParamsToJsonSchema`, `transformParamToJsonSchemaField` |
| **认证字段** | `transformAuthFieldsToParams`, `transformAuthFields` |
| **智能合并** | `extractUpdatedFields`, `smartMergeMapping`, `smartMergeNestedProperties`, `smartMergeOutputParams` |
| **API调用** | `fetchConnectorConfig`, `saveConnectorConfig`, `fetchFlowDetail`, `saveFlowConfig` |

### 8.2 按数据流向分类

| 流向 | 方法列表 |
|------|----------|
| **后端 → 前端** | `transformFromBackend`, `transformFromSchemaFormat`, `transformJsonSchemaToParams`, `transformAuthFieldsToParams`, `fetchConnectorConfig`, `fetchFlowDetail` |
| **前端 → 后端** | `transformToBackend`, `transformToSchemaFormat`, `transformParamsToJsonSchema`, `transformParamToJsonSchemaField`, `transformAuthFields`, `saveConnectorConfig`, `saveFlowConfig` |
| **内部处理** | `extractPropertiesFromNodes`, `extractPropertiesFromMappingData`, `transformMappingToNewFormat`, `transformInputMappingFromNested`, `extractUpdatedFields`, `smartMergeMapping` |

---

## 九、使用示例

### 9.1 加载连接流配置

```javascript
import { fetchFlowDetail } from './FlowEditor/thunk';
import { transformFromBackend } from '../../utils/flowUtils';

// 1. 调用API获取后端数据
const backendData = await fetchFlowDetail(flowId);

// 2. 转换为React Flow格式
const { nodes, edges } = transformFromBackend(backendData.orchestrationConfig);
```

### 9.2 保存连接流配置

```javascript
import { saveFlowConfig } from './FlowEditor/thunk';
import { transformToBackend } from '../../utils/flowUtils';

// 1. 转换为后端格式
const backendData = {
  orchestrationConfig: transformToBackend(nodes, edges)
};

// 2. 调用API保存
await saveFlowConfig(flowId, backendData);
```

### 9.3 智能合并连接器配置

```javascript
import { extractUpdatedFields } from './FlowEditor/thunk';
import { fetchConnectorConfig } from './ConnectorEditor/thunk';

// 1. 获取连接器配置
const connectorConfig = await fetchConnectorConfig(connectorId);

// 2. 智能合并（保留用户配置，补充新参数）
const updatedFields = extractUpdatedFields({
  parsedConnectionConfig: connectorConfig,
  currentNode: currentNode
});

// 3. 更新节点数据
Object.assign(currentNode.data, updatedFields);
```

---

## 十、注意事项

1. **SchemaEditor格式 vs JSON Schema格式**
   - SchemaEditor: `[{paramName, paramType, carrier, children}]` (数组)
   - JSON Schema: `{type: 'object', properties: {...}}` (对象)

2. **carrier类型**
   - 请求参数: `header`, `query`, `body`
   - 响应参数: `header`, `body` (无query)

3. **智能合并场景**
   - 用户手动配置的值不会被覆盖
   - 只补充缺失参数和更新元数据
   - 接口删除的参数也会从用户配置中移除

4. **递归处理**
   - `smartMergeNestedProperties` 支持无限层级嵌套
   - `transformMappingToNewFormat` 支持children递归
   - `flattenObjectProperties` 会平铺所有嵌套层级
