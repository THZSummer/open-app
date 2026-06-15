import { create } from 'zustand';
import { nanoid } from 'nanoid';
import {
  FlowState,
  FlowNode,
  FlowEdge,
  NodeType
} from '../types/flow';

/**
 * 流程编辑器状态管理 Hook
 * 使用 zustand 管理全局状态，包括节点、连线、选中状态等
 */

// 创建初始的触发器节点
const initialTriggerNode: FlowNode = {
  id: nanoid(10),
  type: NodeType.TRIGGER,
  position: { x: 250, y: 50 },
  data: {
    label: '触发器',
    type: NodeType.TRIGGER,
    description: '触发工作流开始'
  }
};

// 创建初始的结束节点
const initialEndNode: FlowNode = {
  id: nanoid(10),
  type: NodeType.END,
  position: { x: 250, y: 300 },
  data: {
    label: '结束',
    type: NodeType.END,
    description: '流程结束'
  }
};

// 创建初始的连线
const initialEdge: FlowEdge = {
  id: nanoid(10),
  source: initialTriggerNode.id,
  target: initialEndNode.id,
  type: 'insert-edge',
  animated: false,
  style: { stroke: '#6366f1', strokeWidth: 2 }
};

/**
 * 获取插入节点的初始位置
 * 主流程节点横向继承上一节点位置，避免把连线中点误用为节点左上角
 */
function getInsertedNodePosition(params: {
  /** 插入按钮传入的位置 */
  position: { x: number; y: number };
  /** 被拆分连线的源节点 */
  sourceNode?: FlowNode;
  /** 是否插入在并行或条件分支内部 */
  parallelBranchConfig?: ReturnType<typeof getParallelBranchConfig>;
}) {
  const { position, sourceNode, parallelBranchConfig } = params;

  if (parallelBranchConfig) {
    return position;
  }

  // 主流程新增节点与上一个节点保持 X 轴对齐，Y 轴后续由布局统一计算
  return {
    x: sourceNode?.position.x ?? position.x,
    y: position.y
  };
}

/**
 * 获取循环节点在父级右侧链路中的归属组 ID
 */
function getNestedLoopParentGroupId(params: {
  node?: FlowNode;
  nodes: FlowNode[];
}): string | undefined {
  const { node, nodes } = params;
  const loopV2GroupId = node?.data.config?.loopV2GroupId;

  // 只有循环内部的跳出节点需要回到父级右侧链路继续插入
  if (!loopV2GroupId || node?.data.config?.loopV2Role !== 'break') {
    return undefined;
  }

  const loopV2Node = nodes.find((item) => item.id === loopV2GroupId);
  const parentGroupId = loopV2Node?.data.config?.parentLoopV2GroupId;
  const parentRole = loopV2Node?.data.config?.parentLoopV2Role;

  // 嵌套循环主节点本身属于父级右侧链路时，跳出节点也归属父级链路
  if (parentGroupId && parentRole === 'right-column-node') {
    return String(parentGroupId);
  }

  return undefined;
}

/**
 * 获取被拆分连线所属的循环右侧链路组 ID
 */
function getLoopV2RightColumnGroupId(params: {
  sourceNode?: FlowNode;
  targetNode?: FlowNode;
  nodes: FlowNode[];
}): string | undefined {
  const { sourceNode, targetNode, nodes } = params;
  const sourceParentGroupId = sourceNode?.data.config?.parentLoopV2GroupId;
  const sourceParentRole = sourceNode?.data.config?.parentLoopV2Role;
  const sourceGroupId = sourceNode?.data.config?.loopV2GroupId;
  const sourceRole = sourceNode?.data.config?.loopV2Role;
  const targetGroupId = targetNode?.data.config?.loopV2GroupId;
  const targetRole = targetNode?.data.config?.loopV2Role;
  const targetParentGroupId = targetNode?.data.config?.parentLoopV2GroupId;
  const targetParentRole = targetNode?.data.config?.parentLoopV2Role;

  // 已有右侧列节点之后继续插入时，沿用源节点的父级链路归属
  if (sourceParentGroupId && sourceParentRole === 'right-column-node') {
    return String(sourceParentGroupId);
  }

  // 从循环开始节点向右侧链路插入时，归属当前循环组
  if (sourceGroupId && sourceRole === 'start' && targetRole !== 'region') {
    return String(sourceGroupId);
  }

  const nestedParentGroupId = getNestedLoopParentGroupId({
    node: sourceNode,
    nodes
  });

  // 嵌套循环跳出节点之后插入时，回到嵌套循环所属的父级右侧链路
  if (nestedParentGroupId) {
    return nestedParentGroupId;
  }

  // 目标节点已属于父级右侧链路时，使用目标节点的父级链路归属兜底
  if (targetParentGroupId && targetParentRole === 'right-column-node') {
    return String(targetParentGroupId);
  }

  // 目标节点是循环结束节点时，结合源节点归属确认右侧链路
  if (targetGroupId && targetRole === 'end') {
    return String(targetGroupId);
  }

  return undefined;
}

/**
 * 获取并行结构中已有分支信息
 */
function getParallelBranches(params: {
  /** 并行分组 ID */
  parallelGroupId: string;
  /** 当前节点列表 */
  nodes: FlowNode[];
}) {
  const { parallelGroupId, nodes } = params;

  return nodes
    .filter(
      (node) =>
        node.data.config?.parallelGroupId === parallelGroupId &&
        node.data.config?.parallelRole === 'branch-start'
    )
    .sort(
      (prev, next) =>
        Number(prev.data.config?.parallelBranchIndex || 0) -
        Number(next.data.config?.parallelBranchIndex || 0)
    );
}

/**
 * 获取被拆分连线所属的并行分支信息
 */
function getParallelBranchConfig(params: {
  /** 被拆分连线源节点 */
  sourceNode?: FlowNode;
  /** 被拆分连线目标节点 */
  targetNode?: FlowNode;
}) {
  const { sourceNode, targetNode } = params;
  const sourceBranchId = sourceNode?.data.config?.parallelBranchId;
  const targetBranchId = targetNode?.data.config?.parallelBranchId;
  const branchId = sourceBranchId || targetBranchId;

  if (!branchId) {
    return undefined;
  }

  return {
    parentParallelGroupId:
      sourceNode?.data.config?.parallelGroupId ||
      sourceNode?.data.config?.parentParallelGroupId ||
      targetNode?.data.config?.parallelGroupId ||
      targetNode?.data.config?.parentParallelGroupId,
    parentParallelBranchId: branchId,
    parentParallelBranchIndex:
      sourceNode?.data.config?.parallelBranchIndex ||
      sourceNode?.data.config?.parentParallelBranchIndex ||
      targetNode?.data.config?.parallelBranchIndex ||
      targetNode?.data.config?.parentParallelBranchIndex,
    parentParallelRole: 'branch-node'
  };
}

/**
 * 判断节点是否为指定错误处理结构的右侧处理链路端点
 */
function isErrorHandlerRightColumnEndpoint(params: {
  /** 当前节点 */
  node?: FlowNode;
  /** 错误处理结构节点 ID */
  errorHandlerId: string;
}) {
  const { node, errorHandlerId } = params;
  const config = node?.data.config;

  if (!node || !config) {
    return false;
  }

  // 错误处理开始和结束文本节点属于右侧处理链路的固定端点
  if (
    config.loopV2GroupId === errorHandlerId &&
    (config.loopV2Role === 'start' || config.loopV2Role === 'end')
  ) {
    return true;
  }

  // 用户插入到错误处理内部的动作节点会记录为右侧链路节点
  return Boolean(
    config.parentLoopV2GroupId === errorHandlerId &&
      config.parentLoopV2Role === 'right-column-node'
  );
}

/**
 * 判断错误处理结构内部是否已经存在动作节点
 */
function hasErrorHandlerActionNode(params: {
  /** 错误处理结构节点 ID */
  errorHandlerId: string;
  /** 当前节点列表 */
  nodes: FlowNode[];
}) {
  const { errorHandlerId, nodes } = params;

  return nodes.some(
    (node) =>
      node.type === NodeType.ACTION &&
      node.data.config?.parentLoopV2GroupId === errorHandlerId &&
      node.data.config?.parentLoopV2Role === 'right-column-node'
  );
}

/**
 * 同步错误处理内部动作节点数量限制对应的插入按钮状态
 */
function syncErrorHandlerActionInsertLimit(params: {
  /** 当前节点列表 */
  nodes: FlowNode[];
  /** 当前连线列表 */
  edges: FlowEdge[];
}) {
  const { nodes, edges } = params;
  const nodeMap = new Map(nodes.map((node) => [node.id, node]));
  const errorHandlerIds = nodes
    .filter((node) => node.type === NodeType.ERROR_HANDLER)
    .map((node) => node.id);

  if (errorHandlerIds.length === 0) {
    return edges;
  }

  return edges.map((edge) => {
    const matchedErrorHandlerId = errorHandlerIds.find((errorHandlerId) => {
      const sourceNode = nodeMap.get(edge.source);
      const targetNode = nodeMap.get(edge.target);

      return (
        isErrorHandlerRightColumnEndpoint({ node: sourceNode, errorHandlerId }) &&
        isErrorHandlerRightColumnEndpoint({ node: targetNode, errorHandlerId })
      );
    });

    if (!matchedErrorHandlerId) {
      return edge;
    }

    const shouldHideInsertButton = hasErrorHandlerActionNode({
      errorHandlerId: matchedErrorHandlerId,
      nodes
    });

    return {
      ...edge,
      data: {
        ...edge.data,
        hideInsertButton: shouldHideInsertButton || undefined
      }
    };
  });
}

/**
 * 创建并行结构单条分支的开始和结束文本节点
 */
function createParallelBranchNodes(params: {
  /** 并行分组 ID */
  parallelGroupId: string;
  /** 分支 ID */
  branchId: string;
  /** 分支序号 */
  branchIndex: number;
  /** 分支文案前缀 */
  branchLabelPrefix: string;
  /** 分支初始位置 */
  position: { x: number; y: number };
}) {
  const { parallelGroupId, branchId, branchIndex, branchLabelPrefix, position } = params;
  const branchStartId = nanoid(10);
  const branchEndId = nanoid(10);

  // 创建分支开始文本节点，删除按钮会根据分支配置展示
  const branchStartNode: FlowNode = {
    id: branchStartId,
    type: NodeType.TEXT,
    position,
    data: {
      label: `${branchLabelPrefix}${branchIndex}开始`,
      type: NodeType.TEXT,
      config: {
        parallelGroupId,
        parallelRole: 'branch-start',
        parallelBranchId: branchId,
        parallelBranchIndex: branchIndex
      }
    }
  };

  // 创建分支结束文本节点，作为分支内部链路汇合前的出口
  const branchEndNode: FlowNode = {
    id: branchEndId,
    type: NodeType.TEXT,
    position: { x: position.x, y: position.y + 200 },
    data: {
      label: `${branchLabelPrefix}${branchIndex}结束`,
      type: NodeType.TEXT,
      config: {
        parallelGroupId,
        parallelRole: 'branch-end',
        parallelBranchId: branchId,
        parallelBranchIndex: branchIndex
      }
    }
  };

  return {
    branchStartNode,
    branchEndNode
  };
}

/**
 * 创建并行结构单条分支的三条连线
 */
function createParallelBranchEdges(params: {
  /** 并行处理主节点 ID */
  parallelId: string;
  /** 分支开始节点 ID */
  branchStartId: string;
  /** 分支结束节点 ID */
  branchEndId: string;
  /** 并行合并节点 ID */
  mergeId: string;
}) {
  const { parallelId, branchStartId, branchEndId, mergeId } = params;

  return [
    {
      id: nanoid(10),
      source: parallelId,
      target: branchStartId,
      sourceHandle: 'top',
      targetHandle: 'bottom',
      type: 'insert-edge',
      animated: false,
      style: { stroke: '#6366f1', strokeWidth: 2 },
      data: {
        hideInsertButton: true
      }
    },
    {
      id: nanoid(10),
      source: branchStartId,
      target: branchEndId,
      sourceHandle: 'top',
      targetHandle: 'bottom',
      type: 'insert-edge',
      animated: false,
      style: { stroke: '#6366f1', strokeWidth: 2 }
    },
    {
      id: nanoid(10),
      source: branchEndId,
      target: mergeId,
      sourceHandle: 'top',
      targetHandle: 'bottom',
      type: 'insert-edge',
      animated: false,
      style: { stroke: '#6366f1', strokeWidth: 2 },
      data: {
        hideInsertButton: true
      }
    }
  ] as FlowEdge[];
}

/**
 * 获取多分支结构的展示文案
 */
function getParallelStructureText(params: {
  /** 多分支结构节点类型 */
  nodeType: NodeType;
}) {
  const { nodeType } = params;

  if (nodeType === NodeType.CONDITION_BRANCH) {
    return {
      main: '条件分支节点',
      description: '多条件分支处理结构',
      branchLabelPrefix: '条件',
      merge: '条件分支合并'
    };
  }

  return {
    main: '并行处理节点',
    description: '多分支并行处理结构',
    branchLabelPrefix: '分支',
    merge: '并行合并'
  };
}

export const useFlowStore = create<FlowState>((set, get) => ({
  // 初始状态：包含触发器节点、结束节点和一条连线
  nodes: [initialTriggerNode, initialEndNode],
  edges: [initialEdge],
  selectedNode: null,
  isLayouting: false,

  /**
   * 设置节点列表
   */
  setNodes: (nodes) => set({ nodes }),

  /**
   * 设置连线列表
   */
  setEdges: (edges) => {
    const { nodes } = get();

    // 外部同步连线时也刷新错误处理内部动作数量限制
    set({
      edges: syncErrorHandlerActionInsertLimit({ nodes, edges })
    });
  },

  /**
   * 添加单个节点
   */
  addNode: (node) =>
    set((state) => ({
      nodes: [...state.nodes, node]
    })),

  /**
   * 删除节点
   */
  removeNode: (nodeId) =>
    set((state) => ({
      nodes: state.nodes.filter((n) => n.id !== nodeId),
      edges: state.edges.filter(
        (e) => e.source !== nodeId && e.target !== nodeId
      )
    })),

  /**
   * 删除流程节点并自动重连前后节点
   * 结构节点会同步删除自身关联的文本节点、分支节点和内部插入节点
   */
  removeFlowNode: (params) => {
    const { nodeId } = params;
    const { nodes, edges } = get();
    const nodeToRemove = nodes.find((node) => node.id === nodeId);

    if (!nodeToRemove || nodeToRemove.type === NodeType.TRIGGER || nodeToRemove.type === NodeType.END) {
      return;
    }

    const removeNodeIds = new Set<string>([nodeId]);

    /**
     * 收集需要随结构节点一起删除的关联节点
     */
    const collectRelatedNodeIds = (currentNodeId: string) => {
      const currentNode = nodes.find((node) => node.id === currentNodeId);
      if (!currentNode) {
        return;
      }

      if (currentNode.type === NodeType.LOOP_V2 || currentNode.type === NodeType.ERROR_HANDLER) {
        for (const node of nodes) {
          const isLoopChild = node.data.config?.loopV2GroupId === currentNode.id;
          const isLoopRightColumnChild = node.data.config?.parentLoopV2GroupId === currentNode.id;
          if (isLoopChild || isLoopRightColumnChild) {
            removeNodeIds.add(node.id);
          }
        }
      }

      if (currentNode.type === NodeType.PARALLEL || currentNode.type === NodeType.CONDITION_BRANCH) {
        for (const node of nodes) {
          const isParallelChild = node.data.config?.parallelGroupId === currentNode.id;
          const isParallelBranchChild = node.data.config?.parentParallelGroupId === currentNode.id;
          if (isParallelChild || isParallelBranchChild) {
            removeNodeIds.add(node.id);
          }
        }
      }
    };

    let hasCollectedNewNode = true;
    while (hasCollectedNewNode) {
      hasCollectedNewNode = false;
      const currentRemoveNodeIds = Array.from(removeNodeIds);
      for (const currentNodeId of currentRemoveNodeIds) {
        const beforeSize = removeNodeIds.size;
        collectRelatedNodeIds(currentNodeId);
        hasCollectedNewNode = hasCollectedNewNode || removeNodeIds.size > beforeSize;
      }
    }

    const incomingEdge = edges.find(
      (edge) => removeNodeIds.has(edge.target) && !removeNodeIds.has(edge.source)
    );
    const outgoingEdge = edges.find(
      (edge) => removeNodeIds.has(edge.source) && !removeNodeIds.has(edge.target)
    );
    const nextEdges = edges.filter(
      (edge) => !removeNodeIds.has(edge.source) && !removeNodeIds.has(edge.target)
    );

    if (incomingEdge && outgoingEdge) {
      nextEdges.push({
        id: nanoid(10),
        source: incomingEdge.source,
        target: outgoingEdge.target,
        sourceHandle: incomingEdge.sourceHandle,
        targetHandle: outgoingEdge.targetHandle,
        type: 'insert-edge',
        animated: false,
        style: { stroke: '#6366f1', strokeWidth: 2 }
      });
    }

    const nextNodes = nodes.filter((node) => !removeNodeIds.has(node.id));

    set({
      nodes: nextNodes,
      edges: syncErrorHandlerActionInsertLimit({
        nodes: nextNodes,
        edges: nextEdges
      })
    });
  },

  /**
   * 更新节点数据
   */
  updateNode: (nodeId, data) =>
    set((state) => ({
      nodes: state.nodes.map((node) =>
        node.id === nodeId
          ? { ...node, data: { ...node.data, ...data } }
          : node
      )
    })),

  /**
   * 添加连线
   */
  addEdge: (edge) =>
    set((state) => ({
      edges: [...state.edges, edge]
    })),

  /**
   * 删除连线
   */
  removeEdge: (edgeId) =>
    set((state) => ({
      edges: state.edges.filter((e) => e.id !== edgeId)
    })),

  /**
   * 设置选中的节点
   */
  setSelectedNode: (node) => set({ selectedNode: node }),

  /**
   * 设置是否正在进行布局计算
   */
  setIsLayouting: (isLayouting) => set({ isLayouting }),

  /**
   * 应用布局后的节点和连线
   */
  applyLayout: (nodes, edges) => set({ nodes, edges }),

  /**
   * 处理节点变化（拖拽、位置更新等）
   */
  onNodesChange: (changes) => {
    const { nodes } = get();
    set({
      nodes: changes.reduce(
        (acc: FlowNode[], change: any) => {
          // 应用位置变化
          if (change.type === 'position' && change.position) {
            return acc.map((node) =>
              node.id === change.id
                ? { ...node, position: change.position }
                : node
            );
          }
          // 应用选中状态变化
          if (change.type === 'select') {
            const selectedNode =
              acc.find((n) => n.id === change.id) || null;
            if (change.selected) {
              set({ selectedNode });
            } else if (get().selectedNode?.id === change.id) {
              set({ selectedNode: null });
            }
          }
          return acc;
        },
        [...nodes]
      )
    });
  },

  /**
   * 处理连线变化
   */
  onEdgesChange: (changes) => {
    const { edges } = get();
    set({
      edges: changes.reduce(
        (acc: FlowEdge[], change: any) => {
          // 处理删除连线
          if (change.type === 'remove') {
            return acc.filter((e) => e.id !== change.id);
          }
          // 处理选中状态变化
          if (change.type === 'select') {
            return acc.map((e) =>
              e.id === change.id ? { ...e, selected: change.selected } : e
            );
          }
          return acc;
        },
        [...edges]
      )
    });
  },

  /**
   * 处理连线创建
   * 直接添加连线，不自动插入节点
   */
  onConnect: (connection) => {
    const { edges } = get();

    // 生成连线 ID
    const newEdgeId = nanoid(10);

    // 创建新连线
    const newEdge: FlowEdge = {
      id: newEdgeId,
      source: connection.source,
      target: connection.target,
      sourceHandle: connection.sourceHandle,
      targetHandle: connection.targetHandle,
      type: 'insert-edge',
      animated: false,
      style: { stroke: '#6366f1', strokeWidth: 2 }
    };

    // 直接添加连线
    set({
      edges: [...edges, newEdge]
    });
  },

  /**
   * 在连线上插入动作节点
   * 特殊处理：如果在循环右侧链路插入节点，新节点继承循环组标记
   */
  insertNodeOnEdge: (params) => {
    const { edgeId, position, nodeType, nodeLabel } = params;
    const { nodes, edges } = get();

    // 找到要拆分的连线
    const edgeToSplit = edges.find((e) => e.id === edgeId);
    if (!edgeToSplit) {
      console.error('未找到要插入节点的连线');
      return;
    }

    // 查找源节点和目标节点
    const sourceNode = nodes.find((n) => n.id === edgeToSplit.source);
    const targetNode = nodes.find((n) => n.id === edgeToSplit.target);

    // 获取当前连线所属的循环右侧链路组 ID
    const rightColumnGroupId = getLoopV2RightColumnGroupId({
      sourceNode,
      targetNode,
      nodes
    });
    const rightColumnGroupNode = nodes.find((node) => node.id === rightColumnGroupId);
    const isErrorHandlerRightColumn = rightColumnGroupNode?.type === NodeType.ERROR_HANDLER;

    // 错误处理内部右侧链路最多只允许添加一个动作节点
    if (
      nodeType === NodeType.ACTION &&
      rightColumnGroupId &&
      isErrorHandlerRightColumn &&
      hasErrorHandlerActionNode({ errorHandlerId: rightColumnGroupId, nodes })
    ) {
      return;
    }

    const parallelBranchConfig = getParallelBranchConfig({
      sourceNode,
      targetNode
    });
    const insertedPosition = getInsertedNodePosition({
      position,
      sourceNode,
      parallelBranchConfig
    });

    // 生成新节点 ID
    const newNodeId = nanoid(10);

    // 创建新动作节点，循环右侧链路节点继承组标记
    const newNode: FlowNode = {
      id: newNodeId,
      type: nodeType,
      position: insertedPosition,
      data: {
        label: nodeLabel,
        type: nodeType,
        description: `${nodeLabel} 节点`,
        config:
          rightColumnGroupId || parallelBranchConfig
            ? {
                ...(rightColumnGroupId
                  ? {
                      parentLoopV2GroupId: rightColumnGroupId,
                      parentLoopV2Role: 'right-column-node'
                    }
                  : {}),
                ...parallelBranchConfig
              }
            : undefined
      }
    };

    // 生成两条新连线 ID
    const newEdge1Id = nanoid(10);
    const newEdge2Id = nanoid(10);

    // 创建第一条连线：源节点 -> 新节点
    const newEdge1: FlowEdge = {
      id: newEdge1Id,
      source: edgeToSplit.source,
      target: newNodeId,
      sourceHandle: edgeToSplit.sourceHandle,
      targetHandle: 'bottom',
      type: 'insert-edge',
      animated: false,
      style: { stroke: '#6366f1', strokeWidth: 2 }
    };

    // 创建第二条连线：新节点 -> 目标节点
    const newEdge2: FlowEdge = {
      id: newEdge2Id,
      source: newNodeId,
      target: edgeToSplit.target,
      sourceHandle: 'top',
      targetHandle: edgeToSplit.targetHandle,
      type: 'insert-edge',
      animated: false,
      style: { stroke: '#6366f1', strokeWidth: 2 }
    };

    const nextNodes = [...nodes, newNode];
    const nextEdges = [...edges.filter((e) => e.id !== edgeId), newEdge1, newEdge2];

    // 更新节点和连线后，同步错误处理内部动作节点数量限制
    set({
      nodes: nextNodes,
      edges: syncErrorHandlerActionInsertLimit({
        nodes: nextNodes,
        edges: nextEdges
      })
    });
  },

  /**
   * 插入循环结构节点
   * 在主流程连线上插入循环或错误处理节点，并生成配套文本说明节点
   */
  insertLoopV2: (params) => {
    const { edgeId, position, nodeType = NodeType.LOOP_V2 } = params;
    const { nodes, edges } = get();

    // 根据结构节点类型获取不同的展示文案，交互结构保持一致
    const structureText =
      nodeType === NodeType.ERROR_HANDLER
        ? {
            main: '错误处理节点',
            description: '带文本说明的错误处理结构',
            region: '错误处理区域',
            start: '错误处理开始',
            end: '错误处理结束',
            break: '错误处理跳出'
          }
        : {
            main: '循环节点',
            description: '带文本说明的循环结构',
            region: '循环区域',
            start: '循环开始',
            end: '循环结束',
            break: '循环跳出'
          };

    // 找到要拆分的主流程连线
    const edgeToSplit = edges.find((e) => e.id === edgeId);
    if (!edgeToSplit) {
      console.error('未找到要插入节点的连线');
      return;
    }

    // 查找被拆分连线两端节点，用于判断是否插入在父级循环右侧链路中
    const sourceNode = nodes.find((n) => n.id === edgeToSplit.source);
    const targetNode = nodes.find((n) => n.id === edgeToSplit.target);
    const rightColumnGroupId = getLoopV2RightColumnGroupId({
      sourceNode,
      targetNode,
      nodes
    });
    const rightColumnGroupNode = nodes.find((node) => node.id === rightColumnGroupId);

    // 错误处理内部只允许添加一个动作节点，不允许继续嵌套结构节点
    if (rightColumnGroupNode?.type === NodeType.ERROR_HANDLER) {
      return;
    }

    const parallelBranchConfig = getParallelBranchConfig({
      sourceNode,
      targetNode
    });
    const insertedPosition = getInsertedNodePosition({
      position,
      sourceNode,
      parallelBranchConfig
    });

    // 生成循环节点及文本节点 ID
    const loopV2Id = nanoid(10);
    const loopRegionTextId = nanoid(10);
    const loopStartTextId = nanoid(10);
    const loopEndTextId = nanoid(10);
    const loopBreakTextId = nanoid(10);

    // 计算循环文本节点的初始横向位置，保证左右两侧到主节点中线距离一致
    const loopMainWidth = 240;
    const loopTextWidth = 240;
    const loopRightColumnOffsetX = 260;
    const loopMainCenterX = insertedPosition.x + loopMainWidth / 2;
    const loopRightTextX = insertedPosition.x + loopRightColumnOffsetX;
    const loopRightTextCenterX = loopRightTextX + loopTextWidth / 2;
    const loopRegionTextX =
      loopMainCenterX -
      (loopRightTextCenterX - loopMainCenterX) -
      loopTextWidth / 2;
    const targetNodeWidth = 240;
    const loopBreakTextX = targetNode
      ? targetNode.position.x + targetNodeWidth / 2 - loopTextWidth / 2
      : insertedPosition.x;

    // 创建循环节点，嵌套在父级右侧链路时额外记录父级循环组
    const loopV2Node: FlowNode = {
      id: loopV2Id,
      type: nodeType,
      position: insertedPosition,
      data: {
        label: structureText.main,
        type: nodeType,
        description: structureText.description,
        config: {
          loopV2GroupId: loopV2Id,
          parentLoopV2GroupId: rightColumnGroupId,
          parentLoopV2Role: rightColumnGroupId ? 'right-column-node' : undefined,
          ...parallelBranchConfig
        }
      }
    };

    // 创建循环区域文本节点
    const loopRegionTextNode: FlowNode = {
      id: loopRegionTextId,
      type: NodeType.TEXT,
      position: { x: loopRegionTextX, y: insertedPosition.y + 140 },
      data: {
        label: structureText.region,
        type: NodeType.TEXT,
        config: {
          loopV2GroupId: loopV2Id,
          loopV2Role: 'region'
        }
      }
    };

    // 创建循环开始文本节点
    const loopStartTextNode: FlowNode = {
      id: loopStartTextId,
      type: NodeType.TEXT,
      position: { x: insertedPosition.x + 260, y: insertedPosition.y + 140 },
      data: {
        label: structureText.start,
        type: NodeType.TEXT,
        config: {
          loopV2GroupId: loopV2Id,
          loopV2Role: 'start'
        }
      }
    };

    // 创建循环结束文本节点
    const loopEndTextNode: FlowNode = {
      id: loopEndTextId,
      type: NodeType.TEXT,
      position: { x: insertedPosition.x + 260, y: insertedPosition.y + 340 },
      data: {
        label: structureText.end,
        type: NodeType.TEXT,
        config: {
          loopV2GroupId: loopV2Id,
          loopV2Role: 'end'
        }
      }
    };

    // 创建循环跳出文本节点，用于汇合循环区域和循环结束后的出口
    const loopBreakTextNode: FlowNode = {
      id: loopBreakTextId,
      type: NodeType.TEXT,
      position: { x: loopBreakTextX, y: insertedPosition.y + 420 },
      data: {
        label: structureText.break,
        type: NodeType.TEXT,
        config: {
          loopV2GroupId: loopV2Id,
          loopV2Role: 'break'
        }
      }
    };

    // 生成循环结构连线 ID
    const previousToLoopEdgeId = nanoid(10);
    const loopToRegionEdgeId = nanoid(10);
    const regionToBreakEdgeId = nanoid(10);
    const loopToStartEdgeId = nanoid(10);
    const startToEndEdgeId = nanoid(10);
    const endToBreakEdgeId = nanoid(10);
    const breakToNextEdgeId = nanoid(10);

    // 上一个节点 -> 循环节点
    const previousToLoopEdge: FlowEdge = {
      id: previousToLoopEdgeId,
      source: edgeToSplit.source,
      target: loopV2Id,
      sourceHandle: edgeToSplit.sourceHandle,
      targetHandle: 'bottom',
      type: 'insert-edge',
      animated: false,
      style: { stroke: '#6366f1', strokeWidth: 2 }
    };

    // 循环节点 -> 循环区域文本
    const loopToRegionEdge: FlowEdge = {
      id: loopToRegionEdgeId,
      source: loopV2Id,
      target: loopRegionTextId,
      sourceHandle: 'top',
      targetHandle: 'bottom',
      type: 'insert-edge',
      animated: false,
      style: { stroke: '#6366f1', strokeWidth: 2 },
      data: {
        hideInsertButton: true
      }
    };

    // 循环区域文本 -> 循环跳出文本
    const regionToBreakEdge: FlowEdge = {
      id: regionToBreakEdgeId,
      source: loopRegionTextId,
      target: loopBreakTextId,
      sourceHandle: 'top',
      targetHandle: 'bottom',
      type: 'insert-edge',
      animated: false,
      style: { stroke: '#6366f1', strokeWidth: 2 },
      data: {
        hideInsertButton: true
      }
    };

    // 循环节点 -> 循环开始文本
    const loopToStartEdge: FlowEdge = {
      id: loopToStartEdgeId,
      source: loopV2Id,
      target: loopStartTextId,
      sourceHandle: 'top',
      targetHandle: 'bottom',
      type: 'insert-edge',
      animated: false,
      style: { stroke: '#6366f1', strokeWidth: 2 },
      data: {
        hideInsertButton: true
      }
    };

    // 循环开始文本 -> 循环结束文本，用户可在中间插入动作节点
    const startToEndEdge: FlowEdge = {
      id: startToEndEdgeId,
      source: loopStartTextId,
      target: loopEndTextId,
      sourceHandle: 'top',
      targetHandle: 'bottom',
      type: 'insert-edge',
      animated: false,
      style: { stroke: '#6366f1', strokeWidth: 2 }
    };

    // 循环结束文本 -> 循环跳出文本，与循环区域出口先汇合
    const endToBreakEdge: FlowEdge = {
      id: endToBreakEdgeId,
      source: loopEndTextId,
      target: loopBreakTextId,
      sourceHandle: 'top',
      targetHandle: 'bottom',
      type: 'insert-edge',
      animated: false,
      style: { stroke: '#6366f1', strokeWidth: 2 },
      data: {
        hideInsertButton: true
      }
    };

    // 循环跳出文本 -> 下一个节点
    const breakToNextEdge: FlowEdge = {
      id: breakToNextEdgeId,
      source: loopBreakTextId,
      target: edgeToSplit.target,
      sourceHandle: 'top',
      targetHandle: edgeToSplit.targetHandle,
      type: 'insert-edge',
      animated: false,
      style: { stroke: '#6366f1', strokeWidth: 2 }
    };

    set({
      nodes: [
        ...nodes,
        loopV2Node,
        loopRegionTextNode,
        loopStartTextNode,
        loopEndTextNode,
        loopBreakTextNode
      ],
      edges: [
        ...edges.filter((e) => e.id !== edgeId),
        previousToLoopEdge,
        loopToRegionEdge,
        regionToBreakEdge,
        loopToStartEdge,
        startToEndEdge,
        endToBreakEdge,
        breakToNextEdge
      ]
    });
  },

  /**
   * 插入多分支结构节点
   * 在主流程连线上创建默认两条分支和合并文本节点
   */
  insertParallel: (params) => {
    const { edgeId, position, nodeType = NodeType.PARALLEL } = params;
    const { nodes, edges } = get();

    // 找到要拆分的主流程连线
    const edgeToSplit = edges.find((edge) => edge.id === edgeId);
    if (!edgeToSplit) {
      console.error('未找到要插入节点的连线');
      return;
    }

    const sourceNode = nodes.find((node) => node.id === edgeToSplit.source);
    const targetNode = nodes.find((node) => node.id === edgeToSplit.target);
    const rightColumnGroupId = getLoopV2RightColumnGroupId({
      sourceNode,
      targetNode,
      nodes
    });
    const rightColumnGroupNode = nodes.find((node) => node.id === rightColumnGroupId);

    // 错误处理内部只允许添加一个动作节点，不允许继续嵌套多分支结构
    if (rightColumnGroupNode?.type === NodeType.ERROR_HANDLER) {
      return;
    }

    const parallelBranchConfig = getParallelBranchConfig({
      sourceNode,
      targetNode
    });
    const insertedPosition = getInsertedNodePosition({
      position,
      sourceNode,
      parallelBranchConfig
    });
    const structureText = getParallelStructureText({ nodeType });
    const parallelId = nanoid(10);
    const mergeId = nanoid(10);
    const branchOneId = nanoid(10);
    const branchTwoId = nanoid(10);

    // 创建多分支结构主节点，点击该节点可继续添加分支
    const parallelNode: FlowNode = {
      id: parallelId,
      type: nodeType,
      position: insertedPosition,
      data: {
        label: structureText.main,
        type: nodeType,
        description: structureText.description,
        config: {
          ...(rightColumnGroupId
            ? {
                parentLoopV2GroupId: rightColumnGroupId,
                parentLoopV2Role: 'right-column-node'
              }
            : {}),
          ...parallelBranchConfig,
          parallelGroupId: parallelId,
          parallelRole: 'root'
        }
      }
    };

    const branchOne = createParallelBranchNodes({
      parallelGroupId: parallelId,
      branchId: branchOneId,
      branchIndex: 1,
      branchLabelPrefix: structureText.branchLabelPrefix,
      position: { x: insertedPosition.x, y: insertedPosition.y + 160 }
    });
    const branchTwo = createParallelBranchNodes({
      parallelGroupId: parallelId,
      branchId: branchTwoId,
      branchIndex: 2,
      branchLabelPrefix: structureText.branchLabelPrefix,
      position: { x: insertedPosition.x + 320, y: insertedPosition.y + 160 }
    });

    // 创建并行合并文本节点，作为所有分支共同出口
    const mergeNode: FlowNode = {
      id: mergeId,
      type: NodeType.TEXT,
      position: { x: insertedPosition.x + 160, y: insertedPosition.y + 420 },
      data: {
        label: structureText.merge,
        type: NodeType.TEXT,
        config: {
          parallelGroupId: parallelId,
          parallelRole: 'merge'
        }
      }
    };

    const previousToParallelEdge: FlowEdge = {
      id: nanoid(10),
      source: edgeToSplit.source,
      target: parallelId,
      sourceHandle: edgeToSplit.sourceHandle,
      targetHandle: 'bottom',
      type: 'insert-edge',
      animated: false,
      style: { stroke: '#6366f1', strokeWidth: 2 }
    };
    const branchEdges = [
      ...createParallelBranchEdges({
        parallelId,
        branchStartId: branchOne.branchStartNode.id,
        branchEndId: branchOne.branchEndNode.id,
        mergeId
      }),
      ...createParallelBranchEdges({
        parallelId,
        branchStartId: branchTwo.branchStartNode.id,
        branchEndId: branchTwo.branchEndNode.id,
        mergeId
      })
    ];
    const mergeToNextEdge: FlowEdge = {
      id: nanoid(10),
      source: mergeId,
      target: edgeToSplit.target,
      sourceHandle: 'top',
      targetHandle: edgeToSplit.targetHandle,
      type: 'insert-edge',
      animated: false,
      style: { stroke: '#6366f1', strokeWidth: 2 }
    };

    set({
      nodes: [
        ...nodes,
        parallelNode,
        branchOne.branchStartNode,
        branchOne.branchEndNode,
        branchTwo.branchStartNode,
        branchTwo.branchEndNode,
        mergeNode
      ],
      edges: [
        ...edges.filter((edge) => edge.id !== edgeId),
        previousToParallelEdge,
        ...branchEdges,
        mergeToNextEdge
      ]
    });
  },

  /**
   * 给并行处理节点新增一条分支
   */
  addParallelBranch: (params) => {
    const { parallelId } = params;
    const { nodes, edges } = get();
    const parallelNode = nodes.find((node) => node.id === parallelId);
    const mergeNode = nodes.find(
      (node) =>
        node.data.config?.parallelGroupId === parallelId &&
        node.data.config?.parallelRole === 'merge'
    );

    if (!parallelNode || !mergeNode) {
      console.error('未找到并行结构节点');
      return;
    }

    const branches = getParallelBranches({
      parallelGroupId: parallelId,
      nodes
    });
    const structureText = getParallelStructureText({
      nodeType: parallelNode.type as NodeType
    });
    const nextBranchIndex = branches.length + 1;
    const branchId = nanoid(10);
    const branchNodes = createParallelBranchNodes({
      parallelGroupId: parallelId,
      branchId,
      branchIndex: nextBranchIndex,
      branchLabelPrefix: structureText.branchLabelPrefix,
      position: {
        x: parallelNode.position.x + (nextBranchIndex - 1) * 320,
        y: parallelNode.position.y + 160
      }
    });
    const branchEdges = createParallelBranchEdges({
      parallelId,
      branchStartId: branchNodes.branchStartNode.id,
      branchEndId: branchNodes.branchEndNode.id,
      mergeId: mergeNode.id
    });

    set({
      nodes: [...nodes, branchNodes.branchStartNode, branchNodes.branchEndNode],
      edges: [...edges, ...branchEdges]
    });
  },

  /**
   * 删除并行结构中的单条分支
   * 保留至少两条分支，避免并行结构退化为普通链路
   */
  removeParallelBranch: (params) => {
    const { parallelGroupId, branchId } = params;
    const { nodes, edges } = get();
    const branches = getParallelBranches({
      parallelGroupId,
      nodes
    });

    if (branches.length <= 2) {
      return;
    }

    const parallelNode = nodes.find((node) => node.id === parallelGroupId);
    const structureText = getParallelStructureText({
      nodeType: (parallelNode?.type as NodeType) || NodeType.PARALLEL
    });

    const branchNodeIds = new Set(
      nodes
        .filter(
          (node) =>
            node.data.config?.parallelBranchId === branchId ||
            node.data.config?.parentParallelBranchId === branchId
        )
        .map((node) => node.id)
    );

    // 沿分支内部连线收集开始到结束之间的节点，避免删除其他分支节点
    let hasChanged = true;
    while (hasChanged) {
      hasChanged = false;
      for (const edge of edges) {
        if (!branchNodeIds.has(edge.source)) {
          continue;
        }

        const targetNode = nodes.find((node) => node.id === edge.target);
        const isSameBranchTarget =
          targetNode?.data.config?.parallelBranchId === branchId;
        const isParallelMergeTarget =
          targetNode?.data.config?.parallelGroupId === parallelGroupId &&
          targetNode?.data.config?.parallelRole === 'merge';

        if (targetNode && !isParallelMergeTarget && !branchNodeIds.has(targetNode.id)) {
          branchNodeIds.add(targetNode.id);
          hasChanged = hasChanged || !isSameBranchTarget;
        }
      }
    }

    const nextNodes = nodes
      .filter((node) => !branchNodeIds.has(node.id))
      .map((node) => {
        const currentBranchId = node.data.config?.parallelBranchId;
        if (!currentBranchId) {
          return node;
        }

        const sortedBranches = branches.filter(
          (branch) => branch.data.config?.parallelBranchId !== branchId
        );
        const nextBranchIndex =
          sortedBranches.findIndex(
            (branch) => branch.data.config?.parallelBranchId === currentBranchId
          ) + 1;

        if (!nextBranchIndex) {
          return node;
        }

        const role = node.data.config?.parallelRole;
        const label =
          role === 'branch-start'
            ? `${structureText.branchLabelPrefix}${nextBranchIndex}开始`
            : role === 'branch-end'
              ? `${structureText.branchLabelPrefix}${nextBranchIndex}结束`
              : node.data.label;

        // 删除分支后对剩余分支重新编号，保持显示顺序连续
        return {
          ...node,
          data: {
            ...node.data,
            label,
            config: {
              ...node.data.config,
              parallelBranchIndex: nextBranchIndex
            }
          }
        };
      });

    set({
      nodes: nextNodes,
      edges: edges.filter(
        (edge) => !branchNodeIds.has(edge.source) && !branchNodeIds.has(edge.target)
      )
    });
  }
}));
