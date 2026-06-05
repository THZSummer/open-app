# 节点编排器设计文档

## 概述

基于 React Flow 实现可视化流程编排器，支持动作、条件分支、循环、异常处理、数据输出等节点类型的编排。

## 技术栈

- React 18+
- React Flow（节点编排核心库）
- Zustand（状态管理）
- TypeScript

## 项目结构

```
FlowEditDemo/
├── src/
│   ├── components/
│   │   ├── FlowCanvas/          # 主画布区域
│   │   │   ├── index.tsx        # ReactFlow 包装器
│   │   │   └── styles.css       # 画布样式
│   │   ├── NodePanel/           # 左侧节点面板（拖拽源）
│   │   │   └── index.tsx
│   │   ├── ConfigPanel/         # 右侧配置面板
│   │   │   └── index.tsx
│   │   └── nodes/               # 自定义节点组件
│   │       ├── ActionNode.tsx
│   │       ├── ConditionNode.tsx
│   │       ├── ForLoopNode.tsx
│   │       ├── TryCatchNode.tsx
│   │       ├── ParallelNode.tsx
│   │       └── OutputNode.tsx
│   ├── types/
│   │   └── flow.ts              # 类型定义
│   ├── store/
│   │   └── flowStore.ts         # 状态管理
│   ├── hooks/
│   │   └── useFlowPersistence.ts # 持久化逻辑
│   └── utils/
│       └── flowHelpers.ts       # 辅助函数
```

## 节点设计

### 节点类型与 Handle

| 节点类型 | 上 Handle | 下 Handle | 右 Handle | 多出口 |
|---------|----------|----------|----------|-------|
| 动作节点 | ✅ | ✅ | - | - |
| 条件分支 | ✅ | ✅ | - | ✅ |
| 循环 | ✅ | ✅ | ✅ | - |
| 异常处理 | ✅ | ✅ | ✅ | - |
| 并行节点 | ✅ | ✅ | - | ✅ |
| 数据输出 | ✅ | - | - | - |

### Handle 语义

- **上 Handle**：主流程入口
- **下 Handle**：主流程出口
- **右 Handle**（循环/异常处理）：子节点链入口
- **多出口**（条件/并行）：分支出口

## 数据结构

### 基础类型

```typescript
// 基础节点数据
interface BaseNodeData {
  title: string;           // 节点标题
  description?: string;    // 节点描述
}

// 动作节点
interface ActionNodeData extends BaseNodeData {
  actionType: 'http' | 'notification' | 'transform';
  config: Record<string, any>;
}

// 条件分支节点
interface ConditionNodeData extends BaseNodeData {
  conditions: Array<{
    id: string;
    expression: string;      // 条件表达式，如 ${ctx.status === 'active'}
    label: string;           // 分支标签
  }>;
}

// 循环节点
interface ForLoopNodeData extends BaseNodeData {
  iterable: string;          // 循环对象，如 ${ctx.items}
  itemName: string;         // 循环变量名，如 item
}

// 异常处理节点
interface TryCatchNodeData extends BaseNodeData {
  strategy: 'retry' | 'ignore';
  retryCount?: number;       // 重试次数（retry时）
  errorHandler?: string;     // 异常处理表达式
}

// 并行节点
interface ParallelNodeData extends BaseNodeData {
  branches: Array<{
    id: string;
    label: string;           // 分支标签
  }>;
}

// 数据输出节点
interface OutputNodeData extends BaseNodeData {
  fields: string[];          // 要输出的上下文字段
}
```

### 节点与边

```typescript
// 完整节点
interface FlowNode {
  id: string;
  type: 'action' | 'condition' | 'forLoop' | 'tryCatch' | 'parallel' | 'output';
  position: { x: number; y: number };
  data: ActionNodeData | ConditionNodeData | ForLoopNodeData |
        TryCatchNodeData | ParallelNodeData | OutputNodeData;
}

// 边
interface FlowEdge {
  id: string;
  source: string;
  target: string;
  sourceHandle?: string;     // 用于区分 Handle
  targetHandle?: string;
  label?: string;            // 边的标签（如条件分支）
}

// 上下文
interface FlowContext {
  [key: string]: any;
}
```

## 状态管理

```typescript
interface FlowStore {
  // 数据
  nodes: FlowNode[];
  edges: FlowEdge[];
  context: FlowContext;

  // 选中状态
  selectedNodeId: string | null;

  // 操作
  addNode: (type: NodeType, position: Position) => void;
  updateNodeData: (id: string, data: Partial<FlowNode['data']>) => void;
  removeNode: (id: string) => void;
  addEdge: (edge: FlowEdge) => void;
  removeEdge: (id: string) => void;

  // 持久化
  saveFlow: (api: FlowPersistenceAPI) => Promise<void>;
  loadFlow: (api: FlowPersistenceAPI, flowId: string) => Promise<void>;
}
```

## 持久化接口

```typescript
interface FlowPersistenceAPI {
  save: (data: { nodes: FlowNode[], edges: FlowEdge[] }) => Promise<void>;
  load: (flowId: string) => Promise<{ nodes: FlowNode[], edges: FlowEdge[] }>;
}
```

## 核心功能

| 功能 | 描述 |
|------|------|
| 节点拖拽添加 | 从左侧面板拖拽节点到画布 |
| 节点移动 | 拖拽调整节点位置 |
| 节点配置 | 选中节点后右侧面板编辑配置 |
| 节点连线 | 拖拽 Handle 创建连线 |
| 连线删除 | 选中连线按删除 |
| 画布操作 | 缩放、平移、适应屏幕 |
| 多选 | 框选多个节点 |
| 撤销/重做 | Ctrl+Z / Ctrl+Y |
| 持久化 | 调用接口保存/加载流程 |

## 节点视觉示例

### 循环节点

```
┌─────────────────────────────────────────────┐
│  For 循环                                    │
│  迭代: ${ctx.items}                          │
│                                              │
│   [上]                                       │
│    │                                         │
│    │     右 ──▶ [循环体节点A]               │
│    │              │                          │
│    │              ▼                          │
│    │         [循环体节点B]                  │
│    │                                         │
│   [下]                                       │
└─────────────────────────────────────────────┘
```

### 异常处理节点

```
┌─────────────────────────────────────────────┐
│  异常处理                                     │
│  策略: ○重试  ●忽略                          │
│                                              │
│   [上]                                       │
│    │                                         │
│    │     右 ──▶ [处理节点A]                  │
│    │              │                          │
│    │              ▼                          │
│    │         [处理节点B]                     │
│    │                                         │
│   [下]                                       │
└─────────────────────────────────────────────┘
```

### 条件分支节点

```
┌─────────────────────────────────────────────┐
│  条件分支                                     │
│                                              │
│   [上]                                       │
│    │                                         │
│    │     条件1 ──▶ [分支1节点]              │
│    │     条件2 ──▶ [分支2节点]              │
│    │     else  ──▶ [分支3节点]              │
│                                              │
│   （多出口，出口顺序代表条件优先级）           │
└─────────────────────────────────────────────┘
```

### 并行节点

```
┌─────────────────────────────────────────────┐
│  并行                                          │
│                                              │
│   [上]                                       │
│    │                                         │
│    │     分支1 ──▶ [并行节点1]               │
│    │     分支2 ──▶ [并行节点2]               │
│    │     分支3 ──▶ [并行节点3]               │
│                                              │
│   （所有分支同时执行）                         │
└─────────────────────────────────────────────┘
```

## 完整流程示例

```
动作节点2 ──▶ 循环节点 ──▶ 动作节点3 ──▶ 异常处理 ──▶ 条件分支 ──▶ 并行 ──▶ 动作节点4 ──▶ 数据输出
                │                │                │              │            │
                │                │                │              │            │
                ▼                ▼                ▼              ▼            ▼
           循环子节点1       异常处理子节点1   分支1/2/3      所有分支      最终输出
           循环子节点2       异常处理子节点2    同时执行
```

## 备选方案：容器渲染（方案 A）

> 方案 B（连线式）为推荐方案，此处记录方案 A 的实现细节供参考。

### 核心思路

循环/异常处理节点作为**容器节点**，内部渲染子节点画布。父子关系通过**空间位置**判定，而非连线。

### 数据结构

```typescript
// 容器节点扩展数据
interface ContainerNodeData extends BaseNodeData {
  childIds: string[];           // 子节点ID列表
  bounds: {
    minX: number;
    minY: number;
    maxX: number;
    maxY: number;
  };
  expanded: boolean;             // 是否展开
}

// 子节点结构
interface ChildNode {
  id: string;
  parentId: string;              // 父容器节点ID
  position: { x: number; y: number };  // 相对于父容器的坐标
  type: 'action' | 'condition' | 'forLoop' | 'tryCatch' | 'parallel' | 'output';
  data: any;
}
```

### 父子关系判定逻辑

```typescript
/**
 * 判断节点归属于哪个容器
 * @param nodePosition 待判断节点的位置
 * @param containers 所有容器节点
 * @returns 父容器ID（取最内层匹配）
 */
function findParentContainer(
  nodePosition: { x: number; y: number },
  containers: Array<{ id: string; bounds: ContainerBounds }>
): string | null {
  // 从后往前遍历，保证取到最内层容器
  for (let i = containers.length - 1; i >= 0; i--) {
    const container = containers[i];
    const { left, top, width, height } = container.bounds;

    // 判断节点是否在容器范围内（留出边距）
    const padding = 20;
    if (
      nodePosition.x >= left + padding &&
      nodePosition.x <= left + width - padding &&
      nodePosition.y >= top + padding &&
      nodePosition.y <= top + height - padding
    ) {
      return container.id;
    }
  }
  return null;
}
```

### 容器尺寸自适应

```typescript
/**
 * 计算容器尺寸，基于子节点的包围盒
 * @param childNodes 子节点列表
 * @param padding 内边距
 * @returns 新的容器尺寸
 */
function calculateContainerBounds(
  childNodes: ChildNode[],
  padding: number = 40
): ContainerBounds {
  if (childNodes.length === 0) {
    return { left: 0, top: 0, width: 200, height: 150 };
  }

  // 计算子节点包围盒
  const nodeWidth = 180;
  const nodeHeight = 80;

  let minX = Infinity, minY = Infinity;
  let maxX = -Infinity, maxY = -Infinity;

  for (const node of childNodes) {
    minX = Math.min(minX, node.position.x);
    minY = Math.min(minY, node.position.y);
    maxX = Math.max(maxX, node.position.x + nodeWidth);
    maxY = Math.max(maxY, node.position.y + nodeHeight);
  }

  // 添加内边距
  return {
    left: minX - padding,
    top: minY - padding,
    width: maxX - minX + padding * 2,
    height: maxY - minY + padding * 2,
  };
}
```

### React Flow 集成

```tsx
// 容器节点组件
const ForLoopNode = ({ data, selected }) => {
  const { childNodes, updateBounds } = useContainerChildren(data.id);

  // 子节点移动时，重新计算容器尺寸
  const handleChildNodeDrag = useCallback(() => {
    const newBounds = calculateContainerBounds(childNodes);
    updateBounds(newBounds);
  }, [childNodes, updateBounds]);

  return (
    <div
      className="container-node"
      style={{
        width: data.bounds.width,
        height: data.bounds.height,
      }}
    >
      <div className="container-header">
        <span>For 循环</span>
        <span>{data.iterable}</span>
      </div>

      {/* 子节点渲染区域 */}
      <div className="container-body">
        <ReactFlow
          nodes={childNodes}
          edges={childEdges}
          onNodeDrag={handleChildNodeDrag}
          // 禁用主画布的交互，只作为渲染容器
          nodesDraggable={true}
          nodesConnectable={false}
          panEnabled={false}
          zoomEnabled={false}
        />
      </div>
    </div>
  );
};
```

### 嵌套层级判断

```typescript
/**
 * 获取节点的最顶层父容器（用于嵌套场景）
 * @param nodeId 节点ID
 * @param allNodes 所有节点
 * @returns 父容器链
 */
function getParentChain(
  nodeId: string,
  allNodes: FlowNode[]
): string[] {
  const chain: string[] = [];
  let currentId: string | null = nodeId;

  while (currentId) {
    const node = allNodes.find(n => n.id === currentId);
    if (node?.data.parentId) {
      chain.unshift(node.data.parentId);
      currentId = node.data.parentId;
    } else {
      break;
    }
  }

  return chain;
}
```

### 方案对比

| 特性 | 方案 A（容器） | 方案 B（连线） |
|------|---------------|---------------|
| 父子关系 | 空间位置判定 | 连线定义 |
| 嵌套层级 | 需额外计算 | 无需处理 |
| 实现复杂度 | 高 | 低 |
| 视觉直观性 | 高 | 中 |
| 子节点布局 | 受容器限制 | 自由摆放 |
| 推荐度 | 备选 | **推荐** |

### 何时选用方案 A

- 对视觉层次要求极高，需要一眼看出父子关系
- 子节点数量少，嵌套深度不超过 2 层
- 愿意投入更多开发时间
