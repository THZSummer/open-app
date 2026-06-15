/**
 * 边事件总线
 * 用于在自定义边组件和 FlowCanvas 组件之间传递插入节点事件
 */
type InsertNodeCallback = (params: {
  /** 被点击的连线 ID */
  edgeId: string;
  /** 节点插入到画布中的参考位置 */
  position: { x: number; y: number };
  /** 下拉框显示在页面中的参考位置 */
  dropdownPosition: { x: number; y: number };
}) => void;

type AddParallelBranchCallback = (params: {
  /** 并行处理主节点 ID */
  parallelId: string;
}) => void;

type RemoveParallelBranchCallback = (params: {
  /** 并行分组 ID */
  parallelGroupId: string;
  /** 要删除的分支 ID */
  branchId: string;
}) => void;

type RemoveFlowNodeCallback = (params: {
  /** 要删除的节点 ID */
  nodeId: string;
}) => void;

let insertNodeCallback: InsertNodeCallback | null = null;
let addParallelBranchCallback: AddParallelBranchCallback | null = null;
let removeParallelBranchCallback: RemoveParallelBranchCallback | null = null;
let removeFlowNodeCallback: RemoveFlowNodeCallback | null = null;

/**
 * 注册主流程插入节点回调
 */
export function registerInsertNodeCallback(callback: InsertNodeCallback) {
  insertNodeCallback = callback;
}

/**
 * 触发主流程插入节点事件
 */
export function triggerInsertNode(params: {
  /** 被点击的连线 ID */
  edgeId: string;
  /** 节点插入到画布中的参考位置 */
  position: { x: number; y: number };
  /** 下拉框显示在页面中的参考位置 */
  dropdownPosition: { x: number; y: number };
}) {
  if (insertNodeCallback) {
    insertNodeCallback(params);
  } else {
    console.warn('未注册插入节点回调');
  }
}

/**
 * 注册并行分支新增回调
 */
export function registerAddParallelBranchCallback(callback: AddParallelBranchCallback) {
  addParallelBranchCallback = callback;
}

/**
 * 触发并行分支新增事件
 */
export function triggerAddParallelBranch(params: {
  /** 并行处理主节点 ID */
  parallelId: string;
}) {
  if (addParallelBranchCallback) {
    addParallelBranchCallback(params);
  } else {
    console.warn('未注册并行分支新增回调');
  }
}

/**
 * 注册并行分支删除回调
 */
export function registerRemoveParallelBranchCallback(callback: RemoveParallelBranchCallback) {
  removeParallelBranchCallback = callback;
}

/**
 * 触发并行分支删除事件
 */
export function triggerRemoveParallelBranch(params: {
  /** 并行分组 ID */
  parallelGroupId: string;
  /** 要删除的分支 ID */
  branchId: string;
}) {
  if (removeParallelBranchCallback) {
    removeParallelBranchCallback(params);
  } else {
    console.warn('未注册并行分支删除回调');
  }
}

/**
 * 注册流程节点删除回调
 */
export function registerRemoveFlowNodeCallback(callback: RemoveFlowNodeCallback) {
  removeFlowNodeCallback = callback;
}

/**
 * 触发流程节点删除事件
 */
export function triggerRemoveFlowNode(params: {
  /** 要删除的节点 ID */
  nodeId: string;
}) {
  if (removeFlowNodeCallback) {
    removeFlowNodeCallback(params);
  } else {
    console.warn('未注册流程节点删除回调');
  }
}

/**
 * 注销主流程和并行结构回调
 */
export function unregisterInsertNodeCallback() {
  insertNodeCallback = null;
  addParallelBranchCallback = null;
  removeParallelBranchCallback = null;
  removeFlowNodeCallback = null;
}
