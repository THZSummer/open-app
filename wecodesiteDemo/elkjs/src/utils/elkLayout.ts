import ELK from 'elkjs';
import { FlowNode, FlowEdge, ELKConfig, NodeType } from '../types/flow';

// 初始化 ELK 实例
const elk = new ELK();

/**
 * 节点纵向间距配置
 * 可插入连线保留加号空间，辅助连线不额外预留节点间距
 */
const nodeSpacingConfig = {
  insertableEdgeGap: 120,
  auxiliaryEdgeGap: 100
};

/**
 * ELK 布局算法配置
 */
const elkConfig: ELKConfig = {
  'elk.algorithm': 'layered',
  'elk.direction': 'DOWN',
  // 使用正交连线策略，让 ELK 的分层结果更接近流程图折线排布
  'elk.edgeRouting': 'ORTHOGONAL',
  // 尽量尊重 children 和 edges 的输入顺序，提升分支左右顺序稳定性
  'elk.layered.considerModelOrder.strategy': 'NODES_AND_EDGES',
  // 使用分层扫线交叉最小化策略，减少复杂嵌套结构中的连线交叉
  'elk.layered.crossingMinimization.strategy': 'LAYER_SWEEP',
  // 使用 Brandes-Koepf 节点放置策略，让同层节点更规整
  'elk.layered.nodePlacement.strategy': 'BRANDES_KOEPF',
  // 增大边与节点、边与边之间的间距，避免视觉上贴边或重叠
  'elk.spacing.edgeNode': '40',
  'elk.spacing.edgeEdge': '24',
  'elk.spacing.nodeNode': '180',
  'elk.layered.spacing.nodeNodeBetweenLayers': String(nodeSpacingConfig.insertableEdgeGap)
};

/**
 * 循环节点布局配置
 */
const loopV2LayoutConfig = {
  mainNodeWidth: 240,
  mainNodeHeight: 76,
  verticalGap: nodeSpacingConfig.auxiliaryEdgeGap,
  rightColumnOffsetX: 260,
  textNodeWidth: 240,
  textNodeHeight: 32,
  rightColumnNodeHeight: 76,
  rightColumnNodeGap: nodeSpacingConfig.insertableEdgeGap,
  auxiliaryEdgeGap: nodeSpacingConfig.auxiliaryEdgeGap
};

/**
 * 并行处理节点布局配置
 */
const parallelLayoutConfig = {
  mainNodeWidth: 240,
  mainNodeHeight: 76,
  textNodeWidth: 240,
  textNodeHeight: 32,
  branchTopGap: nodeSpacingConfig.auxiliaryEdgeGap,
  branchNodeGap: nodeSpacingConfig.insertableEdgeGap,
  branchColumnGap: 260,
  branchCollisionGap: 80,
  branchSafeGap: 80,
  mergeTopGap: nodeSpacingConfig.auxiliaryEdgeGap,
  nestedStructureWidth: 760
};

/**
 * 判断节点是否为循环结构主节点
 */
function isLoopV2StructureNode(node?: FlowNode): boolean {
  return Boolean(
    node?.type === NodeType.LOOP_V2 || node?.type === NodeType.ERROR_HANDLER
  );
}

/**
 * 判断节点是否为多分支结构主节点
 */
function isParallelStructureNode(node?: FlowNode): boolean {
  return Boolean(node?.type === NodeType.PARALLEL || node?.type === NodeType.CONDITION_BRANCH);
}

/**
 * 获取节点尺寸
 */
function getNodeSize(node: FlowNode) {
  if (node.type === NodeType.END) {
    // 结束节点实际使用基础节点样式，宽度需要与 CSS 最小宽度保持一致
    return {
      width: 240,
      height: 56
    };
  }

  // 文本节点使用较小尺寸
  if (node.type === NodeType.TEXT) {
    return {
      width: loopV2LayoutConfig.textNodeWidth,
      height: loopV2LayoutConfig.textNodeHeight
    };
  }

  return {
    width: 240,
    height: 76
  };
}

/**
 * ELK 层级节点结构
 */
type ElkCompoundNode = {
  /** 节点唯一标识 */
  id: string;
  /** 节点布局宽度 */
  width: number;
  /** 节点布局高度 */
  height: number;
  /** 子节点列表 */
  children?: ElkCompoundNode[];
  /** 当前层级内的连线 */
  edges?: ElkCompoundEdge[];
  /** 当前层级的布局配置 */
  layoutOptions?: ELKConfig;
};

/**
 * ELK 层级连线结构
 */
type ElkCompoundEdge = {
  /** 连线唯一标识 */
  id: string;
  /** 连线起点节点 ID */
  sources: string[];
  /** 连线终点节点 ID */
  targets: string[];
};

/**
 * ELK 根图结构
 */
type ElkCompoundGraph = {
  /** 根图唯一标识 */
  id: string;
  /** 根图布局配置 */
  layoutOptions: ELKConfig;
  /** 根图直接子节点 */
  children: ElkCompoundNode[];
  /** 根图直接连线 */
  edges: ElkCompoundEdge[];
};

/**
 * 节点布局占位尺寸
 */
type CompoundNodeSize = {
  /** 节点占位宽度 */
  width: number;
  /** 节点占位高度 */
  height: number;
};

/**
 * 结构节点横向占位信息
 */
type StructureFootprint = {
  /** 结构主轴向左扩展的宽度 */
  left: number;
  /** 结构主轴向右扩展的宽度 */
  right: number;
  /** 结构整体占位宽度 */
  width: number;
};

/**
 * 并行分支横向占位信息
 */
type ParallelBranchFootprint = StructureFootprint & {
  /** 分支 ID */
  branchId: string;
  /** 分支序号 */
  branchIndex: number;
};

/**
 * 获取虚拟复合容器节点 ID
 */
function getCompoundContainerId(params: {
  /** 真实结构节点 ID */
  nodeId: string;
}): string {
  const { nodeId } = params;
  return `compound:${nodeId}`;
}

/**
 * 判断节点是否为复合结构节点
 */
function isCompoundStructureNode(node?: FlowNode): boolean {
  return Boolean(node && (isLoopV2StructureNode(node) || isParallelStructureNode(node)));
}

/**
 * 获取节点所属的真实父级结构节点 ID
 */
function getRealParentNodeId(params: {
  /** 当前节点 */
  node: FlowNode;
  /** 全量节点 ID 集合 */
  nodeIds: Set<string>;
}): string | undefined {
  const { node, nodeIds } = params;
  const config = node.data.config || {};
  const parentId =
    config.parentLoopV2GroupId ||
    config.parentParallelGroupId ||
    config.loopV2GroupId ||
    config.parallelGroupId;

  if (!parentId || parentId === node.id || !nodeIds.has(String(parentId))) {
    return undefined;
  }

  return String(parentId);
}

/**
 * 获取节点在 ELK 层级图中的父级容器 ID
 */
function getElkParentContainerId(params: {
  /** 当前节点 */
  node: FlowNode;
  /** 全量节点 ID 集合 */
  nodeIds: Set<string>;
}): string | undefined {
  const { node, nodeIds } = params;
  const realParentId = getRealParentNodeId({ node, nodeIds });

  if (!realParentId) {
    return undefined;
  }

  return getCompoundContainerId({ nodeId: realParentId });
}

/**
 * 获取并行结构的分支开始节点列表
 */
function getParallelBranchStartNodes(params: {
  /** 并行结构节点 ID */
  parallelId: string;
  /** 当前节点列表 */
  nodes: FlowNode[];
}): FlowNode[] {
  const { parallelId, nodes } = params;

  return nodes
    .filter(
      (node) =>
        node.data.config?.parallelGroupId === parallelId &&
        node.data.config?.parallelRole === 'branch-start'
    )
    .sort(
      (prev, next) =>
        Number(prev.data.config?.parallelBranchIndex || 0) -
        Number(next.data.config?.parallelBranchIndex || 0)
    );
}

/**
 * 获取错误处理结构横向占位
 */
function getErrorHandlerFootprint(): StructureFootprint {
  const halfWidth = loopV2LayoutConfig.rightColumnOffsetX + loopV2LayoutConfig.textNodeWidth / 2;

  return {
    left: halfWidth,
    right: halfWidth,
    width: halfWidth * 2
  };
}

/**
 * 获取普通节点默认横向占位
 */
function getBaseNodeFootprint(params: {
  /** 当前节点 */
  node: FlowNode;
}): StructureFootprint {
  const { node } = params;
  const size = getNodeSize(node);
  const halfWidth = size.width / 2;

  return {
    left: halfWidth,
    right: halfWidth,
    width: size.width
  };
}

/**
 * 获取结构节点横向占位
 */
function getStructureFootprint(params: {
  /** 当前节点 */
  node: FlowNode;
  /** 当前节点列表 */
  nodes: FlowNode[];
  /** 已访问的结构节点 ID */
  visitedNodeIds?: Set<string>;
}): StructureFootprint {
  const { node, nodes, visitedNodeIds = new Set<string>() } = params;

  if (visitedNodeIds.has(node.id)) {
    return getBaseNodeFootprint({ node });
  }

  visitedNodeIds.add(node.id);

  if (node.type === NodeType.ERROR_HANDLER) {
    return getErrorHandlerFootprint();
  }

  if (isParallelStructureNode(node)) {
    return getParallelFootprint({
      parallelId: node.id,
      nodes,
      visitedNodeIds: new Set(visitedNodeIds)
    });
  }

  return getBaseNodeFootprint({ node });
}

/**
 * 获取并行分支中的节点列表
 */
function getParallelBranchNodes(params: {
  /** 分支 ID */
  branchId: string;
  /** 当前节点列表 */
  nodes: FlowNode[];
}): FlowNode[] {
  const { branchId, nodes } = params;

  return nodes.filter(
    (node) =>
      node.data.config?.parallelBranchId === branchId ||
      node.data.config?.parentParallelBranchId === branchId
  );
}

/**
 * 获取并行分支横向占位
 */
function getParallelBranchFootprint(params: {
  /** 分支开始节点 */
  branchStartNode: FlowNode;
  /** 当前节点列表 */
  nodes: FlowNode[];
  /** 已访问的结构节点 ID */
  visitedNodeIds?: Set<string>;
}): ParallelBranchFootprint {
  const { branchStartNode, nodes, visitedNodeIds = new Set<string>() } = params;
  const branchId = String(branchStartNode.data.config?.parallelBranchId);
  const branchIndex = Number(branchStartNode.data.config?.parallelBranchIndex || 0);
  const branchNodes = getParallelBranchNodes({ branchId, nodes });
  const footprint = branchNodes.reduce<StructureFootprint>(
    (currentFootprint, branchNode) => {
      const nodeFootprint = getStructureFootprint({
        node: branchNode,
        nodes,
        visitedNodeIds: new Set(visitedNodeIds)
      });

      return {
        left: Math.max(currentFootprint.left, nodeFootprint.left),
        right: Math.max(currentFootprint.right, nodeFootprint.right),
        width: Math.max(currentFootprint.width, nodeFootprint.left + nodeFootprint.right)
      };
    },
    {
      left: parallelLayoutConfig.textNodeWidth / 2,
      right: parallelLayoutConfig.textNodeWidth / 2,
      width: parallelLayoutConfig.textNodeWidth
    }
  );

  return {
    branchId,
    branchIndex,
    left: footprint.left,
    right: footprint.right,
    width: footprint.left + footprint.right
  };
}

/**
 * 获取并行结构横向占位
 */
function getParallelFootprint(params: {
  /** 并行结构节点 ID */
  parallelId: string;
  /** 当前节点列表 */
  nodes: FlowNode[];
  /** 已访问的结构节点 ID */
  visitedNodeIds?: Set<string>;
}): StructureFootprint {
  const { parallelId, nodes, visitedNodeIds = new Set<string>() } = params;
  const branchStartNodes = getParallelBranchStartNodes({ parallelId, nodes });
  const branchFootprints = branchStartNodes.map((branchStartNode) =>
    getParallelBranchFootprint({
      branchStartNode,
      nodes,
      visitedNodeIds: new Set(visitedNodeIds)
    })
  );

  if (branchFootprints.length === 0) {
    return {
      left: parallelLayoutConfig.mainNodeWidth / 2,
      right: parallelLayoutConfig.mainNodeWidth / 2,
      width: parallelLayoutConfig.mainNodeWidth
    };
  }

  let currentCenterX = 0;
  let minX = -branchFootprints[0].left;
  let maxX = branchFootprints[0].right;

  for (let index = 1; index < branchFootprints.length; index += 1) {
    const previousFootprint = branchFootprints[index - 1];
    const currentFootprint = branchFootprints[index];
    currentCenterX += previousFootprint.right + currentFootprint.left + parallelLayoutConfig.branchSafeGap;
    minX = Math.min(minX, currentCenterX - currentFootprint.left);
    maxX = Math.max(maxX, currentCenterX + currentFootprint.right);
  }

  const baseHalfWidth = parallelLayoutConfig.mainNodeWidth / 2;
  const left = Math.max(baseHalfWidth, Math.abs(minX));
  const right = Math.max(baseHalfWidth, maxX);

  return {
    left,
    right,
    width: left + right
  };
}

/**
 * 预估节点作为复合结构时对父级暴露的占位尺寸
 */
function getCompoundNodeSize(params: {
  /** ReactFlow 节点 */
  node: FlowNode;
  /** 当前节点列表 */
  nodes: FlowNode[];
  /** 已访问的结构节点 ID */
  visitedNodeIds?: Set<string>;
}): CompoundNodeSize {
  const { node, nodes, visitedNodeIds = new Set<string>() } = params;
  const baseSize = getNodeSize(node);

  if (visitedNodeIds.has(node.id)) {
    return baseSize;
  }

  visitedNodeIds.add(node.id);

  if (isParallelStructureNode(node)) {
    const footprint = getParallelFootprint({
      parallelId: node.id,
      nodes,
      visitedNodeIds: new Set(visitedNodeIds)
    });
    const childHeight = nodes
      .filter((item) => item.data.config?.parallelGroupId === node.id)
      .reduce((height, childNode) => {
        const childSize = getCompoundNodeSize({
          node: childNode,
          nodes,
          visitedNodeIds: new Set(visitedNodeIds)
        });

        return Math.max(height, childSize.height);
      }, 0);

    return {
      width: Math.max(baseSize.width, footprint.width),
      height:
        baseSize.height +
        parallelLayoutConfig.branchTopGap +
        childHeight +
        parallelLayoutConfig.mergeTopGap
    };
  }

  if (isLoopV2StructureNode(node)) {
    const childNodes = nodes.filter(
      (item) =>
        item.data.config?.loopV2GroupId === node.id ||
        item.data.config?.parentLoopV2GroupId === node.id
    );
    const childWidth = childNodes.reduce((width, childNode) => {
      const childSize = getCompoundNodeSize({
        node: childNode,
        nodes,
        visitedNodeIds: new Set(visitedNodeIds)
      });

      return Math.max(width, childSize.width);
    }, 0);

    return {
      width: Math.max(
        baseSize.width,
        childWidth + loopV2LayoutConfig.rightColumnOffsetX * 2
      ),
      height:
        baseSize.height +
        loopV2LayoutConfig.verticalGap +
        childNodes.length * loopV2LayoutConfig.rightColumnNodeGap
    };
  }

  return baseSize;
}

/**
 * 创建真实节点对应的 ELK 节点
 */
function createRealElkNode(params: {
  /** ReactFlow 节点 */
  node: FlowNode;
}): ElkCompoundNode {
  const { node } = params;
  const size = getNodeSize(node);

  return {
    id: node.id,
    width: size.width,
    height: size.height
  };
}

/**
 * 创建复合结构的虚拟 ELK 容器节点
 */
function createVirtualContainerNode(params: {
  /** 复合结构真实节点 */
  node: FlowNode;
  /** 当前节点列表 */
  nodes: FlowNode[];
}): ElkCompoundNode {
  const { node, nodes } = params;
  const size = getCompoundNodeSize({ node, nodes });

  return {
    id: getCompoundContainerId({ nodeId: node.id }),
    width: size.width,
    height: size.height,
    children: [],
    edges: [],
    layoutOptions: elkConfig
  };
}

/**
 * 获取循环结构节点排序权重
 */
function getLoopNodeOrder(params: {
  /** ReactFlow 节点 */
  node: FlowNode;
}): number {
  const { node } = params;
  const role = node.data.config?.loopV2Role;

  if (isLoopV2StructureNode(node)) {
    return 0;
  }

  if (role === 'region') {
    return 1;
  }

  if (role === 'start') {
    return 2;
  }

  if (role === 'end') {
    return 3;
  }

  if (role === 'break') {
    return 4;
  }

  return 10;
}

/**
 * 获取并行结构节点排序权重
 */
function getParallelNodeOrder(params: {
  /** ReactFlow 节点 */
  node: FlowNode;
}): number {
  const { node } = params;
  const role = node.data.config?.parallelRole;
  const branchIndex = Number(node.data.config?.parallelBranchIndex || 0);

  if (isParallelStructureNode(node)) {
    return 0;
  }

  if (role === 'branch-start') {
    return 10 + branchIndex * 10;
  }

  if (role === 'branch-end') {
    return 11 + branchIndex * 10;
  }

  if (role === 'merge') {
    return 10000;
  }

  return 5000;
}

/**
 * 获取节点在 ELK children 中的业务排序权重
 */
function getBusinessNodeOrder(params: {
  /** ReactFlow 节点 */
  node: FlowNode;
}): number {
  const { node } = params;

  if (node.data.config?.loopV2GroupId) {
    return getLoopNodeOrder({ node });
  }

  if (node.data.config?.parallelGroupId) {
    return getParallelNodeOrder({ node });
  }

  return 100;
}

/**
 * 按业务语义排序 ELK 节点，辅助 ELK 保持分支左右顺序
 */
function sortElkChildrenByBusinessOrder(params: {
  /** 当前层级内的 ELK 节点 */
  children?: ElkCompoundNode[];
  /** ReactFlow 节点映射 */
  flowNodeMap: Map<string, FlowNode>;
}): ElkCompoundNode[] | undefined {
  const { children, flowNodeMap } = params;

  if (!children) {
    return children;
  }

  return children.sort((prev, next) => {
    const prevNode = flowNodeMap.get(prev.id);
    const nextNode = flowNodeMap.get(next.id);
    const prevOrder = prevNode ? getBusinessNodeOrder({ node: prevNode }) : 100;
    const nextOrder = nextNode ? getBusinessNodeOrder({ node: nextNode }) : 100;

    return prevOrder - nextOrder;
  });
}

/**
 * 获取节点到根层级的 ELK 父级容器链路
 */
function getContainerAncestorIds(params: {
  /** 节点 ID */
  nodeId: string;
  /** 节点父级容器映射 */
  parentIdMap: Map<string, string | undefined>;
}): string[] {
  const { nodeId, parentIdMap } = params;
  const ancestorIds = [nodeId];
  let currentId: string | undefined = nodeId;

  while (currentId) {
    const parentId = parentIdMap.get(currentId);
    if (!parentId || ancestorIds.includes(parentId)) {
      break;
    }

    ancestorIds.push(parentId);
    currentId = parentId;
  }

  return ancestorIds;
}

/**
 * 获取两个节点共同所属的 ELK 容器 ID
 */
function getEdgeContainerId(params: {
  /** 起点节点 ID */
  sourceId: string;
  /** 终点节点 ID */
  targetId: string;
  /** 节点父级容器映射 */
  parentIdMap: Map<string, string | undefined>;
}): string | undefined {
  const { sourceId, targetId, parentIdMap } = params;
  const sourceParentId = parentIdMap.get(sourceId);
  const targetParentId = parentIdMap.get(targetId);

  if (sourceParentId === targetParentId) {
    return sourceParentId;
  }

  const sourceAncestors = getContainerAncestorIds({ nodeId: sourceId, parentIdMap });
  const targetAncestors = getContainerAncestorIds({ nodeId: targetId, parentIdMap });

  return sourceAncestors.find((ancestorId) => targetAncestors.includes(ancestorId));
}

/**
 * 获取节点在指定容器下参与连线的直接节点 ID
 */
function getEdgeEndpointId(params: {
  /** 原始端点节点 ID */
  nodeId: string;
  /** 连线所在容器 ID，undefined 表示根容器 */
  containerId?: string;
  /** 节点父级容器映射 */
  parentIdMap: Map<string, string | undefined>;
}): string {
  const { nodeId, containerId, parentIdMap } = params;
  let currentId = nodeId;

  while (parentIdMap.get(currentId) && parentIdMap.get(currentId) !== containerId) {
    currentId = String(parentIdMap.get(currentId));
  }

  return currentId;
}

/**
 * 将 ReactFlow 节点和连线建模为 ELK 复合层级图
 */
function buildCompoundElkGraph(params: {
  /** 当前节点列表 */
  nodes: FlowNode[];
  /** 当前连线列表 */
  edges: FlowEdge[];
}): ElkCompoundGraph {
  const { nodes, edges } = params;
  const nodeIds = new Set(nodes.map((node) => node.id));
  const flowNodeMap = new Map(nodes.map((node) => [node.id, node]));
  const nodeMap = new Map<string, ElkCompoundNode>();
  const parentIdMap = new Map<string, string | undefined>();
  const graph: ElkCompoundGraph = {
    id: 'root',
    layoutOptions: elkConfig,
    children: [],
    edges: []
  };

  for (const node of nodes) {
    nodeMap.set(node.id, createRealElkNode({ node }));
    parentIdMap.set(node.id, getElkParentContainerId({ node, nodeIds }));

    if (isCompoundStructureNode(node)) {
      const containerId = getCompoundContainerId({ nodeId: node.id });
      nodeMap.set(containerId, createVirtualContainerNode({ node, nodes }));
      parentIdMap.set(containerId, getElkParentContainerId({ node, nodeIds }));
      parentIdMap.set(node.id, containerId);
    }
  }

  for (const node of nodes) {
    const nodeIdsToMount = isCompoundStructureNode(node)
      ? [getCompoundContainerId({ nodeId: node.id }), node.id]
      : [node.id];

    for (const nodeId of nodeIdsToMount) {
      const elkNode = nodeMap.get(nodeId);
      const parentId = parentIdMap.get(nodeId);
      const parentNode = parentId ? nodeMap.get(parentId) : undefined;

      if (!elkNode) {
        continue;
      }

      if (parentNode) {
        parentNode.children = parentNode.children || [];
        parentNode.edges = parentNode.edges || [];
        parentNode.layoutOptions = parentNode.layoutOptions || elkConfig;
        parentNode.children.push(elkNode);
      } else {
        graph.children.push(elkNode);
      }
    }
  }

  for (const elkNode of nodeMap.values()) {
    sortElkChildrenByBusinessOrder({
      children: elkNode.children,
      flowNodeMap
    });
  }
  sortElkChildrenByBusinessOrder({
    children: graph.children,
    flowNodeMap
  });

  for (const edge of edges) {
    const sourceId = edge.source;
    const targetId = edge.target;

    if (!nodeIds.has(sourceId) || !nodeIds.has(targetId)) {
      continue;
    }

    const containerId = getEdgeContainerId({
      sourceId,
      targetId,
      parentIdMap
    });
    const sourceEndpointId = getEdgeEndpointId({
      nodeId: sourceId,
      containerId,
      parentIdMap
    });
    const targetEndpointId = getEdgeEndpointId({
      nodeId: targetId,
      containerId,
      parentIdMap
    });

    if (sourceEndpointId === targetEndpointId) {
      continue;
    }

    const elkEdge = {
      id: edge.id,
      sources: [sourceEndpointId],
      targets: [targetEndpointId]
    };
    const containerNode = containerId ? nodeMap.get(containerId) : undefined;

    if (containerNode) {
      containerNode.edges = containerNode.edges || [];
      containerNode.edges.push(elkEdge);
    } else {
      graph.edges.push(elkEdge);
    }
  }

  return graph;
}

/**
 * 递归收集 ELK 布局结果中的节点绝对坐标
 */
function collectCompoundNodePositions(params: {
  /** ELK 布局后的节点列表 */
  elkNodes?: any[];
  /** 父级节点绝对 X 坐标 */
  parentX?: number;
  /** 父级节点绝对 Y 坐标 */
  parentY?: number;
  /** 节点坐标收集结果 */
  positionMap: Map<string, { x: number; y: number }>;
}) {
  const { elkNodes = [], parentX = 0, parentY = 0, positionMap } = params;

  for (const elkNode of elkNodes) {
    const x = parentX + (elkNode.x || 0);
    const y = parentY + (elkNode.y || 0);

    positionMap.set(elkNode.id, { x, y });
    collectCompoundNodePositions({
      elkNodes: elkNode.children,
      parentX: x,
      parentY: y,
      positionMap
    });
  }
}

/**
 * 获取主流程参考中心线 X 坐标
 */
function getPrimaryAxisCenterX(params: {
  /** ELK 计算后的节点列表 */
  nodes: FlowNode[];
}): number | undefined {
  const { nodes } = params;
  const triggerNode = nodes.find((node) => node.type === NodeType.TRIGGER);
  const endNode = nodes.find((node) => node.type === NodeType.END);
  const referenceNode = triggerNode || endNode;

  if (!referenceNode) {
    return undefined;
  }

  const referenceSize = getNodeSize(referenceNode);

  return referenceNode.position.x + referenceSize.width / 2;
}

/**
 * 获取节点按指定中心线对齐后的 X 坐标
 */
function getAlignedXByCenter(params: {
  /** 需要对齐的节点 */
  node: FlowNode;
  /** 目标中心线 X 坐标 */
  centerX: number;
}): number {
  const { node, centerX } = params;
  const size = getNodeSize(node);

  return centerX - size.width / 2;
}

/**
 * 移动节点到指定中心轴线
 */
function alignNodeToAxis(params: {
  /** 当前节点 */
  node: FlowNode;
  /** 目标中心线 X 坐标 */
  axisCenterX: number;
  /** 节点位置映射 */
  positionMap: Map<string, { x: number; y: number }>;
}) {
  const { node, axisCenterX, positionMap } = params;
  const currentPosition = positionMap.get(node.id) || node.position;

  positionMap.set(node.id, {
    ...currentPosition,
    x: getAlignedXByCenter({
      node,
      centerX: axisCenterX
    })
  });
}

/**
 * 获取节点当前中心线 X 坐标
 */
function getNodeAxisCenterX(params: {
  /** 当前节点 */
  node: FlowNode;
  /** 节点位置映射 */
  positionMap: Map<string, { x: number; y: number }>;
}): number {
  const { node, positionMap } = params;
  const position = positionMap.get(node.id) || node.position;
  const size = getNodeSize(node);

  return position.x + size.width / 2;
}

/**
 * 移动节点到指定水平线
 */
function alignNodeToHorizontalLine(params: {
  /** 当前节点 */
  node: FlowNode;
  /** 目标 Y 坐标 */
  y: number;
  /** 节点位置映射 */
  positionMap: Map<string, { x: number; y: number }>;
}) {
  const { node, y, positionMap } = params;
  const currentPosition = positionMap.get(node.id) || node.position;

  positionMap.set(node.id, {
    ...currentPosition,
    y
  });
}

/**
 * 按纵向偏移量移动单个节点
 */
function moveNodeByDeltaY(params: {
  /** 当前节点 */
  node: FlowNode;
  /** 纵向偏移量 */
  deltaY: number;
  /** 节点位置映射 */
  positionMap: Map<string, { x: number; y: number }>;
}) {
  const { node, deltaY, positionMap } = params;
  const currentPosition = positionMap.get(node.id) || node.position;

  positionMap.set(node.id, {
    ...currentPosition,
    y: currentPosition.y + deltaY
  });
}

/**
 * 判断节点是否属于指定结构节点内部
 */
function isStructureInnerNode(params: {
  /** 当前节点 */
  node: FlowNode;
  /** 结构节点 ID */
  structureNodeId: string;
}) {
  const { node, structureNodeId } = params;
  const config = node.data.config;

  return Boolean(
    config?.parallelGroupId === structureNodeId ||
      config?.parentParallelGroupId === structureNodeId ||
      config?.loopV2GroupId === structureNodeId ||
      config?.parentLoopV2GroupId === structureNodeId
  );
}

/**
 * 按纵向偏移量整体移动结构节点和内部节点
 */
function moveStructureByDeltaY(params: {
  /** 结构节点 */
  structureNode: FlowNode;
  /** 当前节点列表 */
  nodes: FlowNode[];
  /** 纵向偏移量 */
  deltaY: number;
  /** 节点位置映射 */
  positionMap: Map<string, { x: number; y: number }>;
}) {
  const { structureNode, nodes, deltaY, positionMap } = params;

  moveNodeByDeltaY({ node: structureNode, deltaY, positionMap });

  for (const node of nodes) {
    if (node.id !== structureNode.id && isStructureInnerNode({ node, structureNodeId: structureNode.id })) {
      moveNodeByDeltaY({ node, deltaY, positionMap });
    }
  }
}

/**
 * 对齐并行结构内分支开始和结束节点的水平线
 */
function alignParallelBranchBoundaryNodesY(params: {
  /** 并行结构节点 */
  parallelNode: FlowNode;
  /** 当前节点列表 */
  nodes: FlowNode[];
  /** 节点位置映射 */
  positionMap: Map<string, { x: number; y: number }>;
}) {
  const { parallelNode, nodes, positionMap } = params;
  const branchStartNodes = getParallelBranchStartNodes({
    parallelId: parallelNode.id,
    nodes
  });

  if (branchStartNodes.length > 0) {
    // 分支开始节点取最靠上的位置，保证所有分支从同一水平线展开
    const branchStartY = Math.min(
      ...branchStartNodes.map((node) => (positionMap.get(node.id) || node.position).y)
    );

    for (const node of branchStartNodes) {
      alignNodeToHorizontalLine({ node, y: branchStartY, positionMap });
    }
  }
}

/**
 * 获取分支开始节点后的首个业务节点
 */
function getFirstParallelBranchContentNode(params: {
  /** 分支开始节点 */
  branchStartNode: FlowNode;
  /** 当前节点映射 */
  nodeMap: Map<string, FlowNode>;
  /** 当前连线列表 */
  edges: FlowEdge[];
}): FlowNode | undefined {
  const { branchStartNode, nodeMap, edges } = params;
  const firstEdge = edges.find((edge) => edge.source === branchStartNode.id);
  const firstNode = firstEdge ? nodeMap.get(firstEdge.target) : undefined;

  // 分支开始直接连到分支结束时，说明该分支暂时没有业务节点
  if (!firstNode || firstNode.data.config?.parallelRole === 'branch-end') {
    return undefined;
  }

  return firstNode;
}

/**
 * 获取并行结构的外部出口节点
 */
function getParallelStructureExitNode(params: {
  /** 并行结构节点 */
  parallelNode: FlowNode;
  /** 当前节点列表 */
  nodes: FlowNode[];
}): FlowNode | undefined {
  const { parallelNode, nodes } = params;

  return nodes.find(
    (node) =>
      node.data.config?.parallelGroupId === parallelNode.id &&
      node.data.config?.parallelRole === 'merge'
  );
}

/**
 * 获取循环或错误处理结构的外部出口节点
 */
function getLoopStructureExitNode(params: {
  /** 循环或错误处理结构节点 */
  structureNode: FlowNode;
  /** 当前节点列表 */
  nodes: FlowNode[];
}): FlowNode | undefined {
  const { structureNode, nodes } = params;

  return nodes.find(
    (node) =>
      node.data.config?.loopV2GroupId === structureNode.id &&
      node.data.config?.loopV2Role === 'break'
  );
}

/**
 * 获取节点当前位置的底部 Y 坐标
 */
function getNodeBottomY(params: {
  /** 当前节点 */
  node: FlowNode;
  /** 节点位置映射 */
  positionMap: Map<string, { x: number; y: number }>;
}): number {
  const { node, positionMap } = params;
  const position = positionMap.get(node.id) || node.position;
  const size = getNodeSize(node);

  return position.y + size.height;
}

/**
 * 获取节点在外层主链路中继续向下的出口节点
 */
function getMainChainSourceNode(params: {
  /** 当前节点 */
  node: FlowNode;
  /** 当前节点列表 */
  nodes: FlowNode[];
}): FlowNode {
  const { node, nodes } = params;

  if (isParallelStructureNode(node)) {
    return getParallelStructureExitNode({ parallelNode: node, nodes }) || node;
  }

  if (isLoopV2StructureNode(node)) {
    return getLoopStructureExitNode({ structureNode: node, nodes }) || node;
  }

  return node;
}

/**
 * 获取节点在主链路中的底部参考 Y 坐标
 */
function getMainChainBottomY(params: {
  /** 当前节点 */
  node: FlowNode;
  /** 当前节点列表 */
  nodes: FlowNode[];
  /** 节点位置映射 */
  positionMap: Map<string, { x: number; y: number }>;
}): number {
  const { node, nodes, positionMap } = params;
  const sourceNode = getMainChainSourceNode({ node, nodes });

  return getNodeBottomY({ node: sourceNode, positionMap });
}

/**
 * 按目标 Y 移动节点，结构节点保持内部相对位置整体平移
 */
function moveNodeOrStructureToY(params: {
  /** 当前节点 */
  node: FlowNode;
  /** 当前节点列表 */
  nodes: FlowNode[];
  /** 目标 Y 坐标 */
  y: number;
  /** 节点位置映射 */
  positionMap: Map<string, { x: number; y: number }>;
}) {
  const { node, nodes, y, positionMap } = params;
  const currentPosition = positionMap.get(node.id) || node.position;
  const deltaY = y - currentPosition.y;

  if (isCompoundStructureNode(node)) {
    // 结构节点作为主链路节点移动时，内部节点需要同步平移
    moveStructureByDeltaY({
      structureNode: node,
      nodes,
      deltaY,
      positionMap
    });
    return;
  }

  alignNodeToHorizontalLine({ node, y, positionMap });
}

/**
 * 对齐并行结构内各分支的首个业务节点水平线
 */
function alignParallelBranchFirstContentNodesY(params: {
  /** 并行结构节点 */
  parallelNode: FlowNode;
  /** 当前节点列表 */
  nodes: FlowNode[];
  /** 当前连线列表 */
  edges: FlowEdge[];
  /** 节点位置映射 */
  positionMap: Map<string, { x: number; y: number }>;
}) {
  const { parallelNode, nodes, edges, positionMap } = params;
  const nodeMap = new Map(nodes.map((node) => [node.id, node]));
  const branchStartNodes = getParallelBranchStartNodes({
    parallelId: parallelNode.id,
    nodes
  });
  const firstContentNodes = branchStartNodes
    .map((branchStartNode) =>
      getFirstParallelBranchContentNode({
        branchStartNode,
        nodeMap,
        edges
      })
    )
    .filter((node): node is FlowNode => Boolean(node));

  if (branchStartNodes.length === 0 || firstContentNodes.length === 0) {
    return;
  }

  // 首个业务节点基于分支开始节点下方的固定间距对齐，避免结构节点被拉得过高
  const branchStartY = Math.min(
    ...branchStartNodes.map((node) => (positionMap.get(node.id) || node.position).y)
  );
  const firstContentY = branchStartY + parallelLayoutConfig.branchNodeGap;

  for (const node of firstContentNodes) {
    moveNodeOrStructureToY({ node, nodes, y: firstContentY, positionMap });
  }
}

/**
 * 压缩并行结构内每条分支的主链路纵向间距
 */
function compactParallelBranchMainChainsY(params: {
  /** 并行结构节点 */
  parallelNode: FlowNode;
  /** 当前节点列表 */
  nodes: FlowNode[];
  /** 当前连线列表 */
  edges: FlowEdge[];
  /** 节点位置映射 */
  positionMap: Map<string, { x: number; y: number }>;
}) {
  const { parallelNode, nodes, edges, positionMap } = params;
  const nodeMap = new Map(nodes.map((node) => [node.id, node]));
  const branchStartNodes = getParallelBranchStartNodes({
    parallelId: parallelNode.id,
    nodes
  });

  for (const branchStartNode of branchStartNodes) {
    const visitedNodeIds = new Set<string>([branchStartNode.id]);
    let previousNode = branchStartNode;
    let currentNode = branchStartNode;

    while (currentNode) {
      const sourceNode = getMainChainSourceNode({ node: currentNode, nodes });
      const nextEdge = edges.find((edge) => edge.source === sourceNode.id);
      const nextNode = nextEdge ? nodeMap.get(nextEdge.target) : undefined;

      if (!nextNode || visitedNodeIds.has(nextNode.id)) {
        break;
      }

      const previousBottomY = getMainChainBottomY({
        node: previousNode,
        nodes,
        positionMap
      });
      const nextY = previousBottomY + parallelLayoutConfig.branchNodeGap;

      moveNodeOrStructureToY({ node: nextNode, nodes, y: nextY, positionMap });
      visitedNodeIds.add(nextNode.id);

      if (nextNode.data.config?.parallelRole === 'branch-end') {
        break;
      }

      previousNode = nextNode;
      currentNode = nextNode;
    }
  }
}

/**
 * 获取并行结构每条分支的中心线
 */
function getParallelBranchAxisMap(params: {
  /** 并行结构节点 ID */
  parallelId: string;
  /** 当前节点列表 */
  nodes: FlowNode[];
  /** 并行结构主轴中心线 */
  axisCenterX: number;
}): Map<string, number> {
  const { parallelId, nodes, axisCenterX } = params;
  const branchStartNodes = getParallelBranchStartNodes({ parallelId, nodes });
  const branchAxisMap = new Map<string, number>();
  const branchFootprints = branchStartNodes.map((branchStartNode) =>
    getParallelBranchFootprint({ branchStartNode, nodes })
  );
  let currentAxisCenterX = axisCenterX;

  for (let index = 0; index < branchFootprints.length; index += 1) {
    const branchFootprint = branchFootprints[index];

    if (index > 0) {
      const previousFootprint = branchFootprints[index - 1];
      currentAxisCenterX += previousFootprint.right + branchFootprint.left + parallelLayoutConfig.branchSafeGap;
    }

    branchAxisMap.set(branchFootprint.branchId, currentAxisCenterX);
  }

  return branchAxisMap;
}

/**
 * 根据循环结构角色获取内部文本节点
 */
function getLoopRoleNode(params: {
  /** 循环或错误处理结构节点 ID */
  structureNodeId: string;
  /** 内部文本节点角色 */
  role: string;
  /** 当前节点列表 */
  nodes: FlowNode[];
}): FlowNode | undefined {
  const { structureNodeId, role, nodes } = params;

  return nodes.find(
    (node) =>
      node.data.config?.loopV2GroupId === structureNodeId &&
      node.data.config?.loopV2Role === role
  );
}

/**
 * 按内部连线顺序重排循环或错误处理结构的纵向位置
 */
function layoutLoopStructureY(params: {
  /** 循环或错误处理结构节点 */
  structureNode: FlowNode;
  /** 当前节点列表 */
  nodes: FlowNode[];
  /** 当前连线列表 */
  edges: FlowEdge[];
  /** 右侧处理链路中心线 */
  rightColumnCenterX: number;
  /** 节点位置映射 */
  positionMap: Map<string, { x: number; y: number }>;
  /** 已处理结构 ID */
  visitedStructureIds: Set<string>;
}): void {
  const { structureNode, nodes, edges, rightColumnCenterX, positionMap, visitedStructureIds } = params;
  const nodeMap = new Map(nodes.map((node) => [node.id, node]));
  const regionNode = getLoopRoleNode({ structureNodeId: structureNode.id, role: 'region', nodes });
  const startNode = getLoopRoleNode({ structureNodeId: structureNode.id, role: 'start', nodes });
  const breakNode = getLoopRoleNode({ structureNodeId: structureNode.id, role: 'break', nodes });
  const structurePosition = positionMap.get(structureNode.id) || structureNode.position;
  const firstRowY = structurePosition.y + loopV2LayoutConfig.mainNodeHeight + loopV2LayoutConfig.verticalGap;

  if (regionNode) {
    alignNodeToHorizontalLine({ node: regionNode, y: firstRowY, positionMap });
  }

  if (!startNode) {
    return;
  }

  alignNodeToHorizontalLine({ node: startNode, y: firstRowY, positionMap });

  let currentNode = startNode;
  const visitedNodeIds = new Set<string>([startNode.id]);

  while (currentNode) {
    const nextEdge = edges.find((edge) => edge.source === currentNode.id);
    const nextNode = nextEdge ? nodeMap.get(nextEdge.target) : undefined;

    if (!nextNode || visitedNodeIds.has(nextNode.id) || nextNode.id === breakNode?.id) {
      break;
    }

    const nextY = getNodeBottomY({ node: currentNode, positionMap }) + loopV2LayoutConfig.rightColumnNodeGap;
    moveNodeOrStructureToY({ node: nextNode, nodes, y: nextY, positionMap });
    alignNodeToAxis({ node: nextNode, axisCenterX: rightColumnCenterX, positionMap });

    if (isParallelStructureNode(nextNode)) {
      layoutParallelStructureOnAxis({
        parallelNode: nextNode,
        nodes,
        edges,
        axisCenterX: rightColumnCenterX,
        positionMap,
        visitedStructureIds
      });
    }

    if (isLoopV2StructureNode(nextNode)) {
      layoutLoopStructureOnAxis({
        structureNode: nextNode,
        nodes,
        edges,
        axisCenterX: rightColumnCenterX,
        positionMap,
        visitedStructureIds
      });
    }

    visitedNodeIds.add(nextNode.id);
    currentNode = nextNode;
  }

  if (breakNode) {
    const regionBottomY = regionNode ? getNodeBottomY({ node: regionNode, positionMap }) : firstRowY;
    const rightColumnBottomY = getNodeBottomY({ node: currentNode, positionMap });
    const breakY = Math.max(regionBottomY, rightColumnBottomY) + loopV2LayoutConfig.auxiliaryEdgeGap;
    alignNodeToHorizontalLine({ node: breakNode, y: breakY, positionMap });
  }
}

/**
 * 按指定轴线布局循环或错误处理结构
 */
function layoutLoopStructureOnAxis(params: {
  /** 循环或错误处理结构节点 */
  structureNode: FlowNode;
  /** 当前节点列表 */
  nodes: FlowNode[];
  /** 当前连线列表 */
  edges: FlowEdge[];
  /** 结构主轴中心线 */
  axisCenterX: number;
  /** 节点位置映射 */
  positionMap: Map<string, { x: number; y: number }>;
  /** 已处理结构 ID */
  visitedStructureIds: Set<string>;
}) {
  const { structureNode, nodes, edges, axisCenterX, positionMap, visitedStructureIds } = params;
  const rightColumnCenterX = axisCenterX + loopV2LayoutConfig.rightColumnOffsetX;
  const leftColumnCenterX = axisCenterX - loopV2LayoutConfig.rightColumnOffsetX;

  if (visitedStructureIds.has(structureNode.id)) {
    return;
  }

  visitedStructureIds.add(structureNode.id);
  alignNodeToAxis({ node: structureNode, axisCenterX, positionMap });

  for (const node of nodes) {
    const role = node.data.config?.loopV2Role;
    const parentRole = node.data.config?.parentLoopV2Role;
    const isCurrentLoopChild = node.data.config?.loopV2GroupId === structureNode.id;
    const isCurrentRightColumnNode = node.data.config?.parentLoopV2GroupId === structureNode.id;

    if (isCurrentLoopChild && role === 'region') {
      alignNodeToAxis({ node, axisCenterX: leftColumnCenterX, positionMap });
      continue;
    }

    if (isCurrentLoopChild && role === 'break') {
      alignNodeToAxis({ node, axisCenterX, positionMap });
      continue;
    }

    if (isCurrentLoopChild && (role === 'start' || role === 'end')) {
      alignNodeToAxis({ node, axisCenterX: rightColumnCenterX, positionMap });
      continue;
    }

    if (isCurrentRightColumnNode && parentRole === 'right-column-node') {
      alignNodeToAxis({ node, axisCenterX: rightColumnCenterX, positionMap });
    }
  }

  layoutLoopStructureY({
    structureNode,
    nodes,
    edges,
    rightColumnCenterX,
    positionMap,
    visitedStructureIds
  });
}

/**
 * 获取并行结构内所有分支结束节点
 */
function getParallelBranchEndNodes(params: {
  /** 并行结构节点 ID */
  parallelId: string;
  /** 当前节点列表 */
  nodes: FlowNode[];
}): FlowNode[] {
  const { parallelId, nodes } = params;

  return nodes
    .filter(
      (node) =>
        node.data.config?.parallelGroupId === parallelId &&
        node.data.config?.parallelRole === 'branch-end'
    )
    .sort(
      (prev, next) =>
        Number(prev.data.config?.parallelBranchIndex || 0) -
        Number(next.data.config?.parallelBranchIndex || 0)
    );
}

/**
 * 根据分支结束节点重新定位并行合并节点的纵向位置
 */
function alignParallelMergeNodeY(params: {
  /** 并行结构节点 */
  parallelNode: FlowNode;
  /** 当前节点列表 */
  nodes: FlowNode[];
  /** 节点位置映射 */
  positionMap: Map<string, { x: number; y: number }>;
}) {
  const { parallelNode, nodes, positionMap } = params;
  const mergeNode = getParallelStructureExitNode({ parallelNode, nodes });
  const branchEndNodes = getParallelBranchEndNodes({
    parallelId: parallelNode.id,
    nodes
  });

  if (!mergeNode || branchEndNodes.length === 0) {
    return;
  }

  const branchEndBottomY = Math.max(
    ...branchEndNodes.map((node) => getNodeBottomY({ node, positionMap }))
  );

  alignNodeToHorizontalLine({
    node: mergeNode,
    y: branchEndBottomY + parallelLayoutConfig.mergeTopGap,
    positionMap
  });
}

/**
 * 按指定轴线布局并行结构
 */
function layoutParallelStructureOnAxis(params: {
  /** 并行结构节点 */
  parallelNode: FlowNode;
  /** 当前节点列表 */
  nodes: FlowNode[];
  /** 当前连线列表 */
  edges: FlowEdge[];
  /** 并行结构主轴中心线 */
  axisCenterX: number;
  /** 节点位置映射 */
  positionMap: Map<string, { x: number; y: number }>;
  /** 已处理结构 ID */
  visitedStructureIds: Set<string>;
}) {
  const { parallelNode, nodes, edges, axisCenterX, positionMap, visitedStructureIds } = params;

  if (visitedStructureIds.has(parallelNode.id)) {
    return;
  }

  visitedStructureIds.add(parallelNode.id);
  alignNodeToAxis({ node: parallelNode, axisCenterX, positionMap });
  alignParallelBranchBoundaryNodesY({
    parallelNode,
    nodes,
    positionMap
  });
  alignParallelBranchFirstContentNodesY({
    parallelNode,
    nodes,
    edges,
    positionMap
  });
  compactParallelBranchMainChainsY({
    parallelNode,
    nodes,
    edges,
    positionMap
  });

  const branchAxisMap = getParallelBranchAxisMap({
    parallelId: parallelNode.id,
    nodes,
    axisCenterX
  });

  for (const node of nodes) {
    const branchId = node.data.config?.parallelBranchId || node.data.config?.parentParallelBranchId;
    const branchAxisCenterX = branchId ? branchAxisMap.get(String(branchId)) : undefined;

    if (node.data.config?.parallelGroupId === parallelNode.id && node.data.config?.parallelRole === 'merge') {
      alignNodeToAxis({ node, axisCenterX, positionMap });
      continue;
    }

    if (branchAxisCenterX === undefined) {
      continue;
    }

    const isDirectBranchText = node.data.config?.parallelGroupId === parallelNode.id;
    const isBranchInnerNode = node.data.config?.parentParallelGroupId === parallelNode.id;

    if (!isDirectBranchText && !isBranchInnerNode) {
      continue;
    }

    alignNodeToAxis({ node, axisCenterX: branchAxisCenterX, positionMap });

    if (isParallelStructureNode(node)) {
      layoutParallelStructureOnAxis({
        parallelNode: node,
        nodes,
        edges,
        axisCenterX: branchAxisCenterX,
        positionMap,
        visitedStructureIds
      });
    }

    if (isLoopV2StructureNode(node)) {
      layoutLoopStructureOnAxis({
        structureNode: node,
        nodes,
        edges,
        axisCenterX: branchAxisCenterX,
        positionMap,
        visitedStructureIds
      });
    }
  }

  alignParallelMergeNodeY({
    parallelNode,
    nodes,
    positionMap
  });
}

/**
 * 应用结构化语义布局，统一接管结构节点横向坐标
 */
function applyStructuredSemanticLayout(params: {
  /** ELK 和语义修正后的节点列表 */
  nodes: FlowNode[];
  /** 当前连线列表 */
  edges: FlowEdge[];
}): FlowNode[] {
  const { nodes, edges } = params;
  const positionMap = new Map(nodes.map((node) => [node.id, node.position]));
  const primaryAxisCenterX = getPrimaryAxisCenterX({ nodes });
  const visitedStructureIds = new Set<string>();

  for (const node of nodes) {
    if (node.type === NodeType.TRIGGER || node.type === NodeType.END) {
      continue;
    }

    const hasParallelParent = Boolean(node.data.config?.parentParallelGroupId);
    const hasLoopParent = Boolean(node.data.config?.parentLoopV2GroupId);
    const fallbackAxisCenterX = getNodeAxisCenterX({ node, positionMap });
    const axisCenterX = !hasParallelParent && !hasLoopParent && primaryAxisCenterX !== undefined
      ? primaryAxisCenterX
      : fallbackAxisCenterX;

    if (isParallelStructureNode(node) && !hasParallelParent && !hasLoopParent) {
      layoutParallelStructureOnAxis({
        parallelNode: node,
        nodes,
        edges,
        axisCenterX,
        positionMap,
        visitedStructureIds
      });
    }

    if (isLoopV2StructureNode(node) && !hasParallelParent && !hasLoopParent) {
      layoutLoopStructureOnAxis({
        structureNode: node,
        nodes,
        edges,
        axisCenterX,
        positionMap,
        visitedStructureIds
      });
    }
  }

  return nodes.map((node) => ({
    ...node,
    position: positionMap.get(node.id) || node.position
  }));
}

/**
 * 使用 ELK 复合层级图计算全部节点位置
 */
async function applyCompoundElkLayout(params: {
  /** 当前节点列表 */
  nodes: FlowNode[];
  /** 当前连线列表 */
  edges: FlowEdge[];
}): Promise<FlowNode[]> {
  const { nodes, edges } = params;
  const graph = buildCompoundElkGraph({ nodes, edges });
  const layoutedGraph = await elk.layout(graph);
  const positionMap = new Map<string, { x: number; y: number }>();

  collectCompoundNodePositions({
    elkNodes: layoutedGraph.children,
    positionMap
  });

  const layoutedNodes = nodes.map((node) => {
    const position = positionMap.get(node.id);

    if (!position) {
      return node;
    }

    return {
      ...node,
      position
    };
  });

  return applyStructuredSemanticLayout({ nodes: layoutedNodes, edges });
}

/**
 * 使用 ELK 算法自动布局节点
 * @param nodes 当前节点列表
 * @param edges 当前连线列表
 * @returns 布局后的节点列表
 */
export async function applyElkLayout(
  nodes: FlowNode[],
  edges: FlowEdge[]
): Promise<FlowNode[]> {
  // 如果没有节点，直接返回
  if (nodes.length === 0) {
    return [];
  }

  return applyCompoundElkLayout({ nodes, edges });
}

/**
 * 手动触发自动布局
 * 这是一个便捷函数，用于从组件中调用
 */
export async function triggerAutoLayout(params: {
  nodes: FlowNode[];
  edges: FlowEdge[];
  onLayoutComplete: (nodes: FlowNode[]) => void;
}) {
  const { nodes, edges, onLayoutComplete } = params;
  const layoutedNodes = await applyElkLayout(nodes, edges);
  onLayoutComplete(layoutedNodes);
}

