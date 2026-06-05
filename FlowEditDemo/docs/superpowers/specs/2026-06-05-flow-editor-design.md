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

## 注意事项

1. **循环/异常处理子节点**：通过右侧 Handle 连接，形成独立的子节点链
2. **条件/并行多出口**：多个出口 Handle，支持分支连接
3. **上下文传播**：循环输出最后节点结果，异常处理输出正常执行结果
4. **嵌套支持**：循环和异常处理可嵌套
