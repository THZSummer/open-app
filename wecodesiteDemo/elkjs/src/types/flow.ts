import { Node, Edge } from 'reactflow';

// 节点类型枚举，仅保留固定起止节点、动作节点、文本节点、新版循环节点和错误处理节点
export enum NodeType {
  TRIGGER = 'trigger',
  ACTION = 'action',
  END = 'end',
  /** 文本说明节点，仅作为文字展示，不可主动添加 */
  TEXT = 'text',
  /** 循环节点，带文本说明的循环结构 */
  LOOP_V2 = 'loop-v2',
  /** 错误处理节点，复用循环结构交互逻辑 */
  ERROR_HANDLER = 'error-handler',
  /** 并行处理节点，支持多分支处理和汇合 */
  PARALLEL = 'parallel',
  /** 条件分支节点，复用多分支处理和汇合交互 */
  CONDITION_BRANCH = 'condition-branch'
}

// 节点数据接口
export interface FlowNodeData {
  label: string;
  type: NodeType;
  description?: string;
  config?: Record<string, any>;
}

// 自定义节点扩展 ReactFlow 的 Node
export type FlowNode = Node<FlowNodeData>;

// 连线数据接口
export interface FlowEdgeData {
  label?: string;
  hideInsertButton?: boolean;
  // 同源出边信息，用于多出口时先共用竖线再分叉
  sourceEdgeMeta?: {
    total: number;
    index: number;
  };
  // 同目标入边信息，用于多入口时先合并再连接
  targetEdgeMeta?: {
    total: number;
    index: number;
  };
}

// 自定义连线扩展 ReactFlow 的 Edge
export type FlowEdge = Edge<FlowEdgeData>;

// 节点类型配置
export interface NodeTypeConfig {
  type: NodeType;
  label: string;
  icon: string;
  color: string;
  description: string;
}

// ELK 布局配置
export interface ELKConfig {
  [key: string]: string;
  'elk.algorithm': string;
  'elk.direction': string;
  'elk.spacing.nodeNode': string;
  'elk.layered.spacing.nodeNodeBetweenLayers': string;
}

// 流程配置
export interface FlowConfig {
  nodes: FlowNode[];
  edges: FlowEdge[];
}

// 拖拽数据
export interface DragData {
  type: NodeType;
  label: string;
}

// 状态管理 state 接口
export interface FlowState {
  nodes: FlowNode[];
  edges: FlowEdge[];
  selectedNode: FlowNode | null;
  isLayouting: boolean;

  // Actions
  setNodes: (nodes: FlowNode[]) => void;
  setEdges: (edges: FlowEdge[]) => void;
  addNode: (node: FlowNode) => void;
  removeNode: (nodeId: string) => void;
  removeFlowNode: (params: {
    /** 要删除的节点 ID */
    nodeId: string;
  }) => void;
  updateNode: (nodeId: string, data: Partial<FlowNodeData>) => void;
  addEdge: (edge: FlowEdge) => void;
  removeEdge: (edgeId: string) => void;
  setSelectedNode: (node: FlowNode | null) => void;
  setIsLayouting: (isLayouting: boolean) => void;
  applyLayout: (nodes: FlowNode[], edges: FlowEdge[]) => void;
  onNodesChange: (changes: any) => void;
  onEdgesChange: (changes: any) => void;
  onConnect: (connection: any) => void;
  insertNodeOnEdge: (params: {
    edgeId: string;
    position: { x: number; y: number };
    nodeType: NodeType;
    nodeLabel: string;
  }) => void;
  insertLoopV2: (params: {
    /** 被拆分的连线 ID */
    edgeId: string;
    /** 新结构节点的初始位置 */
    position: { x: number; y: number };
    /** 结构节点类型，默认插入循环节点 */
    nodeType?: NodeType;
  }) => void;
  insertParallel: (params: {
    /** 被拆分的连线 ID */
    edgeId: string;
    /** 多分支结构节点的初始位置 */
    position: { x: number; y: number };
    /** 多分支结构节点类型，默认插入并行处理节点 */
    nodeType?: NodeType;
  }) => void;
  addParallelBranch: (params: {
    /** 并行处理主节点 ID */
    parallelId: string;
  }) => void;
  removeParallelBranch: (params: {
    /** 并行分组 ID */
    parallelGroupId: string;
    /** 要删除的分支 ID */
    branchId: string;
  }) => void;
}
