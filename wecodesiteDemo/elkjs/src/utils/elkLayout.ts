import ELK from 'elkjs';
import { FlowNode, FlowEdge, ELKConfig, NodeType } from '../types/flow';

// 初始化 ELK 实例
const elk = new ELK();

/**
 * ELK 布局算法配置
 */
const elkConfig: ELKConfig = {
  'elk.algorithm': 'layered',
  'elk.direction': 'DOWN',
  'elk.spacing.nodeNode': '80',
  'elk.layered.spacing.nodeNodeBetweenLayers': '120'
};

/**
 * 循环节点布局配置
 */
const loopV2LayoutConfig = {
  mainNodeWidth: 240,
  mainNodeHeight: 76,
  verticalGap: 90,
  rightColumnOffsetX: 260,
  regionTextGapY: 60,
  textNodeWidth: 240,
  textNodeHeight: 32,
  rightColumnNodeHeight: 76,
  rightColumnNodeGap: 90,
  rightColumnEndGap: 80
};

/**
 * 并行处理节点布局配置
 */
const parallelLayoutConfig = {
  mainNodeWidth: 240,
  mainNodeHeight: 76,
  textNodeWidth: 240,
  textNodeHeight: 32,
  branchTopGap: 90,
  branchNodeGap: 90,
  branchColumnGap: 120,
  mergeTopGap: 90,
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
 * 判断节点是否为循环节点的关联节点
 */
function isLoopV2ChildNode(node?: FlowNode): boolean {
  return Boolean(node?.data.config?.loopV2GroupId && node.data.config?.loopV2Role);
}

/**
 * 判断节点是否为并行结构内部文本节点
 */
function isParallelChildNode(node?: FlowNode): boolean {
  return Boolean(
    node?.data.config?.parallelGroupId && node.data.config?.parallelRole !== 'root'
  );
}

/**
 * 判断节点是否作为父级并行分支内部节点参与布局
 */
function isParentParallelBranchNode(node?: FlowNode): boolean {
  return Boolean(node?.data.config?.parentParallelBranchId);
}

/**
 * 判断节点是否作为父级循环 V2 的右侧列节点参与布局
 */
function isParentLoopV2RightColumnNode(node?: FlowNode): boolean {
  return Boolean(
    node?.data.config?.parentLoopV2GroupId &&
      node.data.config?.parentLoopV2Role === 'right-column-node'
  );
}

/**
 * 获取节点参与 ELK 主布局时使用的节点 ID
 */
function getElkLayoutNodeId(node: FlowNode | undefined, fallbackId: string): string {
  if (isLoopV2ChildNode(node)) {
    return String(node?.data.config?.loopV2GroupId);
  }

  if (isParallelChildNode(node)) {
    return String(node?.data.config?.parallelGroupId);
  }

  if (isParentLoopV2RightColumnNode(node)) {
    return String(node?.data.config?.parentLoopV2GroupId);
  }

  if (isParentParallelBranchNode(node)) {
    return String(node?.data.config?.parentParallelGroupId);
  }

  return fallbackId;
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
 * 将 ReactFlow 节点转换为 ELK 节点格式
 */
function convertToElkNodes(nodes: FlowNode[]) {
  return nodes.map((node) => {
    const size = getNodeSize(node);

    return {
      id: node.id,
      width: size.width,
      height: size.height
    };
  });
}

/**
 * 将 ReactFlow 连线转换为 ELK 连线格式
 * 循环节点的文本子节点由专用布局处理，布局时映射回所属循环节点
 */
function convertToElkEdges(params: {
  edges: FlowEdge[];
  nodes: FlowNode[];
  layoutNodeIds: Set<string>;
}) {
  const { edges, nodes, layoutNodeIds } = params;

  return edges
    .map((edge) => {
      const sourceNode = nodes.find((node) => node.id === edge.source);
      const targetNode = nodes.find((node) => node.id === edge.target);
      const mappedSource = getElkLayoutNodeId(sourceNode, edge.source);
      const mappedTarget = getElkLayoutNodeId(targetNode, edge.target);

      return {
        id: edge.id,
        source: mappedSource,
        target: mappedTarget
      };
    })
    .filter(
      (edge) =>
        edge.source !== edge.target &&
        layoutNodeIds.has(edge.source) &&
        layoutNodeIds.has(edge.target)
    )
    .map((edge) => ({
      id: edge.id,
      sources: [edge.source],
      targets: [edge.target]
    }));
}

/**
 * 将 ELK 节点转换回 ReactFlow 节点格式
 */
function convertFromElkNodes(
  elkNodes: any[],
  originalNodes: FlowNode[]
): FlowNode[] {
  return originalNodes.map((node) => {
    const elkNode = elkNodes.find((en) => en.id === node.id);
    if (elkNode) {
      return {
        ...node,
        position: {
          x: elkNode.x,
          y: elkNode.y
        }
      };
    }
    return node;
  });
}

/**
 * 应用循环节点组内文本节点的布局
 * 循环节点下方分两列：左侧循环区域文本，右侧循环开始/结束文本
 * 右侧列新增节点根据连线顺序排列在循环开始和循环结束之间
 */
function applyLoopV2ChildrenLayout(params: {
  nodes: FlowNode[];
  edges: FlowEdge[];
}): FlowNode[] {
  const { nodes, edges } = params;

  // 获取循环 V2 组自身占用的整体高度
  const getLoopV2GroupHeight = (params: {
    groupId: string;
    visitedGroupIds?: Set<string>;
  }): number => {
    const { groupId, visitedGroupIds = new Set<string>() } = params;
    if (visitedGroupIds.has(groupId)) {
      return loopV2LayoutConfig.rightColumnNodeHeight;
    }

    visitedGroupIds.add(groupId);
    const rightColumnOrder = rightColumnOrderMap.get(groupId) || [];
    const insertedNodeIds = rightColumnOrder.filter((id) => {
      const node = nodeMap.get(id);
      return (
        node?.data.config?.loopV2Role !== 'end' &&
        node?.data.config?.loopV2Role !== 'break'
      );
    });
    const insertedNodesHeight = insertedNodeIds.reduce((totalHeight, nodeId) => {
      const nodeHeight = getRightColumnNodeHeight({
        nodeId,
        parentGroupId: groupId,
        visitedGroupIds
      });

      return totalHeight + nodeHeight + loopV2LayoutConfig.rightColumnNodeGap;
    }, 0);

    const endBottomHeight =
      loopV2LayoutConfig.mainNodeHeight +
      loopV2LayoutConfig.verticalGap +
      loopV2LayoutConfig.textNodeHeight +
      loopV2LayoutConfig.regionTextGapY +
      insertedNodesHeight +
      (insertedNodeIds.length > 0 ? loopV2LayoutConfig.rightColumnEndGap : 0) +
      loopV2LayoutConfig.textNodeHeight;
    const breakBottomHeight =
      loopV2LayoutConfig.mainNodeHeight +
      loopV2LayoutConfig.verticalGap +
      loopV2LayoutConfig.textNodeHeight +
      loopV2LayoutConfig.regionTextGapY +
      insertedNodesHeight +
      (insertedNodeIds.length > 0 ? loopV2LayoutConfig.rightColumnEndGap : 0) +
      loopV2LayoutConfig.textNodeHeight +
      loopV2LayoutConfig.regionTextGapY +
      loopV2LayoutConfig.textNodeHeight;

    // 嵌套循环在父级右侧列占位时，需要覆盖循环结束和循环跳出两条路径的最低点
    return Math.max(endBottomHeight, breakBottomHeight);
  };

  // 获取节点在指定父级循环组内占用的高度
  const getRightColumnNodeHeight = (params: {
    nodeId: string;
    parentGroupId: string;
    visitedGroupIds?: Set<string>;
  }) => {
    const { nodeId, parentGroupId, visitedGroupIds } = params;
    const node = nodeMap.get(nodeId);
    if (!node) {
      return loopV2LayoutConfig.rightColumnNodeHeight;
    }

    // 嵌套循环在父级右侧列中需要按自身整组高度占位
    if (
      isLoopV2StructureNode(node) &&
      node.data.config?.parentLoopV2GroupId === parentGroupId
    ) {
      return getLoopV2GroupHeight({
        groupId: node.id,
        visitedGroupIds
      });
    }

    return getNodeSize(node).height;
  };

  // 收集每个循环组右侧列节点的连线顺序
  const rightColumnOrderMap = new Map<string, string[]>();
  const nodeMap = new Map(nodes.map((node) => [node.id, node]));
  const loopV2Nodes = nodes.filter((node) => isLoopV2StructureNode(node));

  for (const loopV2Node of loopV2Nodes) {
    const groupId = loopV2Node.id;
    const startNode = nodes.find(
      (node) =>
        node.data.config?.loopV2GroupId === groupId &&
        node.data.config?.loopV2Role === 'start'
    );
    const endNode = nodes.find(
      (node) =>
        node.data.config?.loopV2GroupId === groupId &&
        node.data.config?.loopV2Role === 'end'
    );

    if (!startNode || !endNode) {
      continue;
    }

    // 从循环开始文本节点沿右侧链路追踪到循环结束文本节点
    const orderedIds: string[] = [];
    let currentId = startNode.id;
    while (currentId !== endNode.id) {
      const nextEdge = edges.find((edge) => edge.source === currentId);
      if (!nextEdge || orderedIds.includes(nextEdge.target)) {
        break;
      }

      orderedIds.push(nextEdge.target);

      // 如果下一个节点是嵌套循环，则跳过其内部结构并继续追踪父级右侧链路
      const nextNode = nodeMap.get(nextEdge.target);
      if (
        isLoopV2StructureNode(nextNode) &&
        nextNode?.data.config?.parentLoopV2GroupId === groupId
      ) {
        const nestedBreakNode = nodes.find(
          (node) =>
            node.data.config?.loopV2GroupId === nextNode.id &&
            node.data.config?.loopV2Role === 'break'
        );
        currentId = nestedBreakNode?.id || nextEdge.target;
        continue;
      }

      currentId = nextEdge.target;
    }

    rightColumnOrderMap.set(groupId, orderedIds);
  }

  let layoutedNodes = nodes;
  for (let layoutIndex = 0; layoutIndex < loopV2Nodes.length; layoutIndex += 1) {
    const currentNodeMap = new Map(layoutedNodes.map((node) => [node.id, node]));
    layoutedNodes = layoutedNodes.map((node) => {
      const isParentRightColumnNode = isParentLoopV2RightColumnNode(node);
    if (!isLoopV2ChildNode(node) && !isParentRightColumnNode) {
      return node;
    }

    // 找到所属的循环节点
    const groupId = isParentRightColumnNode
      ? (node.data.config?.parentLoopV2GroupId as string)
      : (node.data.config?.loopV2GroupId as string);
    const loopV2Node = currentNodeMap.get(groupId);
    if (!loopV2Node) {
      return node;
    }

    const role = isParentRightColumnNode
      ? (node.data.config?.parentLoopV2Role as string)
      : (node.data.config?.loopV2Role as string);
    const baseX = loopV2Node.position.x;
    const baseY = loopV2Node.position.y;
    const mainCenterX = baseX + loopV2LayoutConfig.mainNodeWidth / 2;
    const rightColumnX = baseX + loopV2LayoutConfig.rightColumnOffsetX;
    const rightColumnY = baseY + loopV2LayoutConfig.mainNodeHeight + loopV2LayoutConfig.verticalGap;

    if (role === 'region') {
      const rightColumnCenterX = rightColumnX + loopV2LayoutConfig.textNodeWidth / 2;
      const leftColumnX =
        mainCenterX -
        (rightColumnCenterX - mainCenterX) -
        loopV2LayoutConfig.textNodeWidth / 2;

      // 循环区域文本跟随循环节点当前位置，保持在主节点中线左侧
      return {
        ...node,
        position: {
          x: leftColumnX,
          y: rightColumnY
        }
      };
    }

    if (role === 'start') {
      // 循环开始文本跟随循环节点当前位置，和循环结束文本保持同一横向坐标
      return {
        ...node,
        position: {
          x: rightColumnX,
          y: rightColumnY
        }
      };
    }

    if (role === 'right-column-node') {
      const rightColumnOrder = rightColumnOrderMap.get(groupId) || [];
      const nodeIndex = rightColumnOrder.indexOf(node.id);
      const previousNodeIds = rightColumnOrder.slice(0, Math.max(nodeIndex, 0));
      const previousNodesHeight = previousNodeIds.reduce((totalHeight, nodeId) => {
        const nodeHeight = getRightColumnNodeHeight({
          nodeId,
          parentGroupId: groupId
        });

        return totalHeight + nodeHeight + loopV2LayoutConfig.rightColumnNodeGap;
      }, 0);
      const y =
        rightColumnY +
        loopV2LayoutConfig.textNodeHeight +
        loopV2LayoutConfig.rightColumnNodeGap +
        previousNodesHeight;

      const currentNodeWidth = getNodeSize(node).width;
      const rightColumnCenterX = rightColumnX + loopV2LayoutConfig.textNodeWidth / 2;
      const x = rightColumnCenterX - currentNodeWidth / 2;

      // 循环右侧列插入节点按 handle 中心对齐父级循环开始节点，纵向按顺序排布
      return {
        ...node,
        position: {
          x,
          y
        }
      };
    }

    if (role === 'end') {
      const rightColumnOrder = rightColumnOrderMap.get(groupId) || [];
      const insertedNodeIds = rightColumnOrder.filter((id) => id !== node.id);
      const insertedNodesHeight = insertedNodeIds.reduce((totalHeight, nodeId) => {
        const nodeHeight = getRightColumnNodeHeight({
          nodeId,
          parentGroupId: groupId
        });

        return totalHeight + nodeHeight + loopV2LayoutConfig.rightColumnNodeGap;
      }, 0);
      const y =
        rightColumnY +
        loopV2LayoutConfig.textNodeHeight +
        loopV2LayoutConfig.regionTextGapY +
        insertedNodesHeight +
        (insertedNodeIds.length > 0 ? loopV2LayoutConfig.rightColumnEndGap : 0);

      // 循环结束文本根据右侧列实际占位高度自动下移，末尾增加额外间隙
      return {
        ...node,
        position: {
          x: rightColumnX,
          y
        }
      };
    }

    if (role === 'break') {
      const rightColumnOrder = rightColumnOrderMap.get(groupId) || [];
      const insertedNodeIds = rightColumnOrder.filter((id) => {
        const orderedNode = nodeMap.get(id);
        return orderedNode?.data.config?.loopV2Role !== 'end';
      });
      const insertedNodesHeight = insertedNodeIds.reduce((totalHeight, nodeId) => {
        const nodeHeight = getRightColumnNodeHeight({
          nodeId,
          parentGroupId: groupId
        });

        return totalHeight + nodeHeight + loopV2LayoutConfig.rightColumnNodeGap;
      }, 0);
      const y =
        rightColumnY +
        loopV2LayoutConfig.textNodeHeight +
        loopV2LayoutConfig.regionTextGapY +
        insertedNodesHeight +
        (insertedNodeIds.length > 0 ? loopV2LayoutConfig.rightColumnEndGap : 0) +
        loopV2LayoutConfig.textNodeHeight +
        loopV2LayoutConfig.regionTextGapY;
      const nextEdge = edges.find((edge) => edge.source === node.id);
      const nextNode = nextEdge ? currentNodeMap.get(nextEdge.target) : undefined;
      const nextNodeSize = nextNode ? getNodeSize(nextNode) : undefined;
      const nextNodeCenterX = nextNode && nextNodeSize
        ? nextNode.position.x + nextNodeSize.width / 2
        : mainCenterX;
      const x = nextNodeCenterX - loopV2LayoutConfig.textNodeWidth / 2;

      // 循环跳出文本随右侧列高度下移，并让自身 handle 与下一个节点 handle 垂直对齐
      return {
        ...node,
        position: {
          x,
          y
        }
      };
    }

      return node;
    });
  }

  return layoutedNodes;
}

/**
 * 应用并行结构内部节点布局
 * 分支1与并行节点 X 轴对齐，后续分支按实际占用宽度向右排列
 */
function applyParallelChildrenLayout(params: {
  /** 当前节点列表 */
  nodes: FlowNode[];
  /** 当前连线列表 */
  edges: FlowEdge[];
}): FlowNode[] {
  const { nodes, edges } = params;
  const nodeMap = new Map(nodes.map((node) => [node.id, node]));
  const parallelNodes = nodes.filter((node) => node.type === NodeType.PARALLEL);
  let layoutedNodes = nodes;

  /**
   * 获取分支内部节点的布局宽度
   */
  const getBranchItemWidth = (node?: FlowNode): number => {
    if (!node) {
      return parallelLayoutConfig.textNodeWidth;
    }

    if (isLoopV2StructureNode(node) || node.type === NodeType.PARALLEL) {
      return parallelLayoutConfig.nestedStructureWidth;
    }

    return getNodeSize(node).width;
  };

  /**
   * 获取分支内部节点的布局高度
   */
  const getBranchItemHeight = (node?: FlowNode): number => {
    if (!node) {
      return parallelLayoutConfig.textNodeHeight;
    }

    if (isLoopV2StructureNode(node)) {
      const loopChildren = layoutedNodes.filter(
        (child) => child.data.config?.loopV2GroupId === node.id
      );
      const loopBottomY = loopChildren.reduce((bottomY, child) => {
        return Math.max(bottomY, child.position.y + getNodeSize(child).height);
      }, node.position.y + getNodeSize(node).height);

      // 循环或错误处理节点在分支内按实际子节点底部计算占位高度
      return Math.max(getNodeSize(node).height, loopBottomY - node.position.y);
    }

    if (node.type === NodeType.PARALLEL) {
      const parallelChildren = layoutedNodes.filter(
        (child) => child.data.config?.parallelGroupId === node.id
      );
      const parallelBottomY = parallelChildren.reduce((bottomY, child) => {
        return Math.max(bottomY, child.position.y + getNodeSize(child).height);
      }, node.position.y + getNodeSize(node).height);

      // 嵌套并行节点按实际子节点底部计算占位高度，避免固定高度造成过大空隙
      return Math.max(getNodeSize(node).height, parallelBottomY - node.position.y);
    }

    return getNodeSize(node).height;
  };

  /**
   * 获取分支内部节点后方间距
   * 嵌套结构后方保留常规间距，确保结构出口到父级分支结束节点之间有插入空间
   */
  const getBranchItemGap = (node?: FlowNode): number => {
    if (node && (isLoopV2StructureNode(node) || node.type === NodeType.PARALLEL)) {
      return parallelLayoutConfig.branchNodeGap;
    }

    return parallelLayoutConfig.branchNodeGap;
  };

  /**
   * 获取嵌套结构在父级分支链路中的出口节点 ID
   * 父级分支只把嵌套结构主节点作为整体内容，不能继续穿透结构内部节点
   */
  const getNestedStructureExitNodeId = (node?: FlowNode): string | undefined => {
    if (!node) {
      return undefined;
    }

    if (node.type === NodeType.PARALLEL) {
      const mergeNode = layoutedNodes.find(
        (child) =>
          child.data.config?.parallelGroupId === node.id &&
          child.data.config?.parallelRole === 'merge'
      );

      return mergeNode?.id;
    }

    if (isLoopV2StructureNode(node)) {
      const breakNode = layoutedNodes.find(
        (child) =>
          child.data.config?.loopV2GroupId === node.id &&
          child.data.config?.loopV2Role === 'break'
      );

      return breakNode?.id;
    }

    return undefined;
  };

  for (const parallelNode of parallelNodes) {
    const groupId = parallelNode.id;
    const branchStarts = layoutedNodes
      .filter(
        (node) =>
          node.data.config?.parallelGroupId === groupId &&
          node.data.config?.parallelRole === 'branch-start'
      )
      .sort(
        (prev, next) =>
          Number(prev.data.config?.parallelBranchIndex || 0) -
          Number(next.data.config?.parallelBranchIndex || 0)
      );
    const mergeNode = layoutedNodes.find(
      (node) =>
        node.data.config?.parallelGroupId === groupId &&
        node.data.config?.parallelRole === 'merge'
    );

    if (!mergeNode || branchStarts.length === 0) {
      continue;
    }

    const branchLayouts = branchStarts.map((branchStart) => {
      const branchId = String(branchStart.data.config?.parallelBranchId);
      const branchEnd = layoutedNodes.find(
        (node) =>
          node.data.config?.parallelGroupId === groupId &&
          node.data.config?.parallelBranchId === branchId &&
          node.data.config?.parallelRole === 'branch-end'
      );
      const orderedNodeIds: string[] = [];
      let currentId = branchStart.id;

      while (branchEnd && currentId !== branchEnd.id) {
        const nextEdge = edges.find((edge) => edge.source === currentId);
        if (!nextEdge || orderedNodeIds.includes(nextEdge.target)) {
          break;
        }

        orderedNodeIds.push(nextEdge.target);

        const nextNode = nodeMap.get(nextEdge.target);
        const nestedStructureExitNodeId = getNestedStructureExitNodeId(nextNode);
        if (nestedStructureExitNodeId) {
          const exitEdge = edges.find((edge) => edge.source === nestedStructureExitNodeId);
          currentId = exitEdge?.target || nextEdge.target;
          continue;
        }

        currentId = nextEdge.target;
      }

      const contentNodeIds = orderedNodeIds.filter((nodeId) => nodeId !== branchEnd?.id);
      const maxWidth = [branchStart.id, ...contentNodeIds, branchEnd?.id]
        .filter(Boolean)
        .reduce((width, nodeId) => {
          const node = nodeMap.get(String(nodeId));
          return Math.max(width, getBranchItemWidth(node));
        }, parallelLayoutConfig.textNodeWidth);

      return {
        branchId,
        branchStartId: branchStart.id,
        branchEndId: branchEnd?.id,
        contentNodeIds,
        width: maxWidth
      };
    });

    let currentX = parallelNode.position.x;
    let maxBranchEndY = parallelNode.position.y;
    const branchPositionMap = new Map<string, { x: number; width: number }>();

    for (const branchLayout of branchLayouts) {
      branchPositionMap.set(branchLayout.branchId, {
        x: currentX,
        width: branchLayout.width
      });
      currentX += branchLayout.width + parallelLayoutConfig.branchColumnGap;
    }

    layoutedNodes = layoutedNodes.map((node) => {
      const branchId =
        node.data.config?.parallelBranchId || node.data.config?.parentParallelBranchId;
      const branchPosition = branchId
        ? branchPositionMap.get(String(branchId))
        : undefined;

      if (!branchPosition) {
        return node;
      }

      const branchLayout = branchLayouts.find((item) => item.branchId === String(branchId));
      if (!branchLayout) {
        return node;
      }

      const itemWidth = getBranchItemWidth(node);
      const centeredX = branchPosition.x + branchPosition.width / 2 - itemWidth / 2;
      const startY =
        parallelNode.position.y +
        parallelLayoutConfig.mainNodeHeight +
        parallelLayoutConfig.branchTopGap;

      if (node.id === branchLayout.branchStartId) {
        return {
          ...node,
          position: {
            x: branchPosition.x,
            y: startY
          }
        };
      }

      const contentIndex = branchLayout.contentNodeIds.indexOf(node.id);
      if (contentIndex >= 0) {
        const previousContentHeight = branchLayout.contentNodeIds
          .slice(0, contentIndex)
          .reduce((height, nodeId) => {
            const previousNode = nodeMap.get(nodeId);
            return height + getBranchItemHeight(previousNode) + getBranchItemGap(previousNode);
          }, 0);

        return {
          ...node,
          position: {
            x: centeredX,
            y:
              startY +
              parallelLayoutConfig.textNodeHeight +
              parallelLayoutConfig.branchNodeGap +
              previousContentHeight
          }
        };
      }

      if (node.id === branchLayout.branchEndId) {
        const contentHeight = branchLayout.contentNodeIds.reduce((height, nodeId) => {
          const contentNode = nodeMap.get(nodeId);
          return height + getBranchItemHeight(contentNode) + getBranchItemGap(contentNode);
        }, 0);
        const endY =
          startY +
          parallelLayoutConfig.textNodeHeight +
          parallelLayoutConfig.branchNodeGap +
          contentHeight;
        maxBranchEndY = Math.max(maxBranchEndY, endY);

        return {
          ...node,
          position: {
            x: branchPosition.x,
            y: endY
          }
        };
      }

      return node;
    });

    const firstBranch = branchPositionMap.get(branchLayouts[0]?.branchId || '');
    const lastBranch = branchPositionMap.get(branchLayouts[branchLayouts.length - 1]?.branchId || '');
    if (!firstBranch || !lastBranch) {
      continue;
    }

    layoutedNodes = layoutedNodes.map((node) => {
      if (node.id !== mergeNode.id) {
        return node;
      }

      // 并行合并文本节点与分支1开始文本节点保持 X 轴对齐
      return {
        ...node,
        position: {
          x: firstBranch.x,
          y: maxBranchEndY + parallelLayoutConfig.mergeTopGap
        }
      };
    });
  }

  return layoutedNodes;
}

/**
 * 判断两个节点是否都属于同一个循环 V2 内部结构
 */
function isSameLoopV2GroupEdge(params: {
  sourceNode?: FlowNode;
  targetNode?: FlowNode;
}): boolean {
  const { sourceNode, targetNode } = params;
  const sourceGroupId = sourceNode?.data.config?.loopV2GroupId;
  const targetGroupId = targetNode?.data.config?.loopV2GroupId;

  // 循环 V2 内部文本节点之间的连线不参与主流程级联下推
  return Boolean(sourceGroupId && targetGroupId && sourceGroupId === targetGroupId);
}

/**
 * 判断两个节点是否都属于同一个并行结构
 */
function isSameParallelGroupEdge(params: {
  sourceNode?: FlowNode;
  targetNode?: FlowNode;
}): boolean {
  const { sourceNode, targetNode } = params;
  const sourceGroupId = sourceNode?.data.config?.parallelGroupId;
  const targetGroupId = targetNode?.data.config?.parallelGroupId;

  // 并行结构内部连线不参与主流程级联下推
  return Boolean(sourceGroupId && targetGroupId && sourceGroupId === targetGroupId);
}

/**
 * 判断目标节点是否属于源循环 V2 的内部或右侧链路
 */
function isLoopV2GroupInnerTarget(params: {
  sourceGroupId: string;
  targetNode?: FlowNode;
}): boolean {
  const { sourceGroupId, targetNode } = params;

  // 当前循环 V2 主节点、文本子节点和右侧列节点都属于组内目标
  return Boolean(
    targetNode?.id === sourceGroupId ||
      targetNode?.data.config?.loopV2GroupId === sourceGroupId ||
      targetNode?.data.config?.parentLoopV2GroupId === sourceGroupId
  );
}

/**
 * 获取节点作为主流程节点时的底部坐标
 */
function getMainFlowNodeBottomY(params: {
  node: FlowNode;
  groupBottomYMap: Map<string, number>;
}): number {
  const { node, groupBottomYMap } = params;

  // 结构节点作为主流程节点时，需要使用整组最低点作为底部
  if (isLoopV2StructureNode(node) || node.type === NodeType.PARALLEL) {
    return groupBottomYMap.get(node.id) ?? node.position.y + getNodeSize(node).height;
  }

  return node.position.y + getNodeSize(node).height;
}

/**
 * 判断节点是否可以作为主流程级联下推节点
 */
function isMainFlowCascadeNode(node?: FlowNode): boolean {
  // 结构内部节点由各自专用布局处理，不参与主流程级联下推
  return Boolean(
    node &&
      !isLoopV2ChildNode(node) &&
      !isParentLoopV2RightColumnNode(node) &&
      !isParallelChildNode(node) &&
      !isParentParallelBranchNode(node)
  );
}

/**
 * 计算每个循环 V2 组当前布局后的最低坐标
 */
function getLoopV2GroupBottomYMap(nodes: FlowNode[]): Map<string, number> {
  const groupBottomYMap = new Map<string, number>();
  const loopV2Nodes = nodes.filter((node) => isLoopV2StructureNode(node));
  const parallelNodes = nodes.filter((node) => node.type === NodeType.PARALLEL);

  for (const loopV2Node of loopV2Nodes) {
    const groupId = loopV2Node.id;
    const children = nodes.filter(
      (node) =>
        isLoopV2ChildNode(node) && node.data.config?.loopV2GroupId === groupId
    );
    const bottomY = children.reduce((maxY, child) => {
      const childBottom = child.position.y + getNodeSize(child).height;
      return Math.max(maxY, childBottom);
    }, loopV2Node.position.y + getNodeSize(loopV2Node).height);

    groupBottomYMap.set(groupId, bottomY);
  }

  for (const parallelNode of parallelNodes) {
    const groupId = parallelNode.id;
    const children = nodes.filter(
      (node) => node.data.config?.parallelGroupId === groupId
    );
    const bottomY = children.reduce((maxY, child) => {
      const childBottom = child.position.y + getNodeSize(child).height;
      return Math.max(maxY, childBottom);
    }, parallelNode.position.y + getNodeSize(parallelNode).height);

    groupBottomYMap.set(groupId, bottomY);
  }

  return groupBottomYMap;
}

/**
 * 调整循环节点之后的主流程节点位置，确保后续链路整体下推
 */
function adjustLoopV2NextNodePosition(params: {
  nodes: FlowNode[];
  edges: FlowEdge[];
}): FlowNode[] {
  const { nodes, edges } = params;
  const groupBottomYMap = getLoopV2GroupBottomYMap(nodes);

  if (groupBottomYMap.size === 0) {
    return nodes;
  }

  const nodeMap = new Map(nodes.map((node) => [node.id, node]));
  const requiredTopYMap = new Map<string, number>();
  const mainFlowGap = 120;

  // 从循环 V2 出口开始记录第一个需要下推的主流程节点
  for (const edge of edges) {
    const sourceNode = nodeMap.get(edge.source);
    const targetNode = nodeMap.get(edge.target);
    const sourceGroupId =
      sourceNode?.data.config?.loopV2GroupId || sourceNode?.data.config?.parallelGroupId;

    if (!sourceGroupId || !targetNode) {
      continue;
    }

    if (isLoopV2GroupInnerTarget({ sourceGroupId, targetNode })) {
      continue;
    }

    if (isSameParallelGroupEdge({ sourceNode, targetNode })) {
      continue;
    }

    if (!isMainFlowCascadeNode(targetNode)) {
      continue;
    }

    const groupBottomY = groupBottomYMap.get(String(sourceGroupId));
    if (!groupBottomY) {
      continue;
    }

    requiredTopYMap.set(
      targetNode.id,
      Math.max(requiredTopYMap.get(targetNode.id) ?? targetNode.position.y, groupBottomY + mainFlowGap)
    );
  }

  // 沿主流程连线向后传递下推距离，覆盖普通节点夹在多个循环 V2 中间的场景
  for (let cascadeIndex = 0; cascadeIndex < nodes.length; cascadeIndex += 1) {
    let hasChanged = false;

    for (const edge of edges) {
      const sourceNode = nodeMap.get(edge.source);
      const targetNode = nodeMap.get(edge.target);

      if (!sourceNode || !targetNode) {
        continue;
      }

      if (!isMainFlowCascadeNode(sourceNode) || !isMainFlowCascadeNode(targetNode)) {
        continue;
      }

      if (isSameLoopV2GroupEdge({ sourceNode, targetNode })) {
        continue;
      }

      const sourceRequiredTopY = requiredTopYMap.get(sourceNode.id) ?? sourceNode.position.y;
      const sourceBottomY =
        sourceRequiredTopY +
        (getMainFlowNodeBottomY({ node: sourceNode, groupBottomYMap }) - sourceNode.position.y);
      const targetRequiredTopY = sourceBottomY + mainFlowGap;
      const currentRequiredTopY = requiredTopYMap.get(targetNode.id) ?? targetNode.position.y;

      if (targetRequiredTopY > currentRequiredTopY) {
        requiredTopYMap.set(targetNode.id, targetRequiredTopY);
        hasChanged = true;
      }
    }

    if (!hasChanged) {
      break;
    }
  }

  if (requiredTopYMap.size === 0) {
    return nodes;
  }

  return nodes.map((node) => {
    const requiredTopY = requiredTopYMap.get(node.id);

    if (!requiredTopY || node.position.y >= requiredTopY) {
      return node;
    }

    // 只调整纵向位置，保留 ELK 计算出的横向位置
    return {
      ...node,
      position: {
        x: node.position.x,
        y: requiredTopY
      }
    };
  });
}

/**
 * 判断两组节点位置是否完全一致
 */
function isSameNodePositions(params: {
  previousNodes: FlowNode[];
  nextNodes: FlowNode[];
}): boolean {
  const { previousNodes, nextNodes } = params;

  // 通过位置对比判断循环 V2 布局是否已经收敛
  return nextNodes.every((node) => {
    const previousNode = previousNodes.find((item) => item.id === node.id);

    return (
      previousNode?.position.x === node.position.x &&
      previousNode?.position.y === node.position.y
    );
  });
}

/**
 * 收敛结构子节点布局和后续主流程节点下推
 * 先处理并行结构再处理循环结构，保证嵌套分支内的布局准确
 */
function settleLoopV2Layout(params: {
  nodes: FlowNode[];
  edges: FlowEdge[];
}): FlowNode[] {
  const { nodes, edges } = params;
  let layoutedNodes = nodes;
  const maxSettleCount = Math.max(nodes.length, 2);

  // 多结构串行嵌套时，通过有限收敛让子节点重算和主流程下推相互稳定
  for (let settleIndex = 0; settleIndex < maxSettleCount; settleIndex += 1) {
    const previousNodes = layoutedNodes;
    layoutedNodes = applyParallelChildrenLayout({
      nodes: layoutedNodes,
      edges
    });
    layoutedNodes = applyLoopV2ChildrenLayout({
      nodes: layoutedNodes,
      edges
    });
    layoutedNodes = adjustLoopV2NextNodePosition({
      nodes: layoutedNodes,
      edges
    });

    if (isSameNodePositions({ previousNodes, nextNodes: layoutedNodes })) {
      break;
    }
  }

  return layoutedNodes;
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

  const mainNodes = nodes.filter(
    (node) =>
      !isLoopV2ChildNode(node) &&
      !isParentLoopV2RightColumnNode(node) &&
      !isParallelChildNode(node) &&
      !isParentParallelBranchNode(node)
  );

  // 如果没有连线，使用简单的垂直布局
  if (edges.length === 0) {
    const startX = 400;
    const startY = 50;
    const verticalSpacing = 140;
    const simpleNodes = nodes.map((node, index) => ({
      ...node,
      position: {
        x: startX,
        y: startY + index * verticalSpacing
      }
    }));

    return settleLoopV2Layout({ nodes: simpleNodes, edges });
  }

  try {
    // 转换为 ELK 格式
    const elkLayoutNodeIds = new Set(mainNodes.map((n) => n.id));
    const elkNodes = convertToElkNodes(mainNodes);
    const elkEdges = convertToElkEdges({
      edges,
      nodes,
      layoutNodeIds: elkLayoutNodeIds
    });

    // 执行 ELK 布局算法
    const layoutedGraph = await elk.layout({
      id: 'root',
      layoutOptions: elkConfig,
      children: elkNodes,
      edges: elkEdges
    });

    // 转换回 ReactFlow 格式
    if (layoutedGraph.children) {
      const layoutedMainNodes = convertFromElkNodes(
        layoutedGraph.children,
        mainNodes
      );
      const layoutedNodes = nodes.map((node) => {
        const mainNode = layoutedMainNodes.find((item) => item.id === node.id);
        return mainNode || node;
      });

      return settleLoopV2Layout({ nodes: layoutedNodes, edges });
    }

    return settleLoopV2Layout({ nodes, edges });
  } catch (error) {
    console.error('ELK 布局失败:', error);

    // 布局失败时使用简单的垂直布局作为降级方案
    const startX = 400;
    const startY = 50;
    const verticalSpacing = 140;
    const fallbackNodes = nodes.map((node, index) => ({
      ...node,
      position: {
        x: startX,
        y: startY + index * verticalSpacing
      }
    }));

    return settleLoopV2Layout({ nodes: fallbackNodes, edges });
  }
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
