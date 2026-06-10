import React, { useState } from 'react';
import ReactFlow, {
  Background,
  Controls,
  useNodesState,
  useEdgesState,
  Connection,
  EdgeTypes,
  ReactFlowInstance
} from 'reactflow';
import 'reactflow/dist/style.css';
import { useFlowStore } from '../../store/flowStore';
import { nodeTypes } from '../nodes';
import InsertEdge from '../edges/InsertEdge';
import NodeSelectModal from '../NodeSelectModal';
import { NodeType, FlowEdge } from '../../types/flow';
import { applyElkLayout } from '../../utils/elkLayout';
import {
  registerInsertNodeCallback,
  registerAddParallelBranchCallback,
  registerRemoveParallelBranchCallback,
  registerRemoveFlowNodeCallback,
  unregisterInsertNodeCallback
} from '../../store/edgeEventBus';
import './FlowCanvas.css';

// 注册自定义边组件
const edgeTypes: EdgeTypes = {
  'insert-edge': InsertEdge
};

/**
 * 为边补充同源出边和同目标入边信息
 * 边组件根据这些信息计算分叉和合并折线路径
 */
function enrichEdgesWithRoutingMeta(params: { edges: FlowEdge[] }): FlowEdge[] {
  const { edges } = params;

  // 按源节点和目标节点分别收集边
  const sourceEdgeMap = new Map<string, FlowEdge[]>();
  const targetEdgeMap = new Map<string, FlowEdge[]>();

  for (const edge of edges) {
    const sourceEdges = sourceEdgeMap.get(edge.source) || [];
    sourceEdges.push(edge);
    sourceEdgeMap.set(edge.source, sourceEdges);

    const targetEdges = targetEdgeMap.get(edge.target) || [];
    targetEdges.push(edge);
    targetEdgeMap.set(edge.target, targetEdges);
  }

  // 给每条边写入当前边在同源/同目标集合中的位置
  return edges.map((edge) => {
    const sourceEdges = sourceEdgeMap.get(edge.source) || [];
    const targetEdges = targetEdgeMap.get(edge.target) || [];

    return {
      ...edge,
      data: {
        ...edge.data,
        sourceEdgeMeta: {
          total: sourceEdges.length,
          index: sourceEdges.findIndex((item) => item.id === edge.id)
        },
        targetEdgeMeta: {
          total: targetEdges.length,
          index: targetEdges.findIndex((item) => item.id === edge.id)
        }
      }
    };
  });
}

/**
 * 流程画布组件
 * 核心功能：
 * 1. 管理节点和连线的状态
 * 2. 处理连线上的动作节点和循环节点插入逻辑
 * 3. 集成 ELK 自动布局功能
 */
const FlowCanvas: React.FC = () => {
  // 从 store 获取状态和操作
  const {
    nodes: storeNodes,
    edges: storeEdges,
    setNodes: setStoreNodes,
    onConnect: storeOnConnect,
    insertNodeOnEdge,
    insertLoopV2,
    insertParallel,
    addParallelBranch,
    removeParallelBranch,
    removeFlowNode,
    isLayouting,
    setIsLayouting
  } = useFlowStore();

  // 本地状态，用于 ReactFlow
  const [nodes, setNodes, onNodesChange] = useNodesState(storeNodes);
  const [edges, setEdges, onEdgesChange] = useEdgesState(storeEdges);

  // 模态框状态
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [insertContext, setInsertContext] = useState<{
    edgeId: string;
    position: { x: number; y: number };
  } | null>(null);

  // 保存 ReactFlow 实例
  const reactFlowInstanceRef = React.useRef<ReactFlowInstance | null>(null);

  // ReactFlow 初始化时的回调
  const onInit = (instance: ReactFlowInstance) => {
    reactFlowInstanceRef.current = instance;
  };

  // 同步 store 中的数据到本地状态
  React.useEffect(() => {
    setNodes(storeNodes);
  }, [storeNodes, setNodes]);

  React.useEffect(() => {
    const enrichedEdges = enrichEdgesWithRoutingMeta({ edges: storeEdges });
    setEdges(enrichedEdges);
  }, [storeEdges, setEdges]);

  // 节点或连线变化时，打印当前画布完整数据结构
  React.useEffect(() => {
    console.log('当前画布数据结构:', {
      nodes: storeNodes,
      edges: storeEdges
    });
  }, [storeNodes, storeEdges]);

  // 初始加载时，自适应画布
  React.useEffect(() => {
    if (nodes.length > 0) {
      setTimeout(() => {
        reactFlowInstanceRef.current?.fitView({
          padding: 0.2,
          duration: 300
        });
      }, 100);
    }
  }, []);

  /**
   * 打开节点选择弹窗
   */
  const openNodeSelectModal = (params: {
    edgeId: string;
    position: { x: number; y: number };
  }) => {
    setInsertContext(params);
    setIsModalOpen(true);
  };

  /**
   * 处理主流程连线上的插入按钮点击
   */
  const handleInsertNode = (params: {
    edgeId: string;
    position: { x: number; y: number };
  }) => {
    openNodeSelectModal({
      edgeId: params.edgeId,
      position: params.position
    });
  };

  /**
   * 同步当前 store 数据并重新执行自动布局
   */
  const syncStoreAndApplyLayout = () => {
    const { nodes: updatedNodes, edges: updatedEdges } = useFlowStore.getState();
    setNodes(updatedNodes);
    setEdges(updatedEdges);

    setTimeout(async () => {
      if (isLayouting) return;
      setIsLayouting(true);

      try {
        const { nodes: currentNodes, edges: currentEdges } = useFlowStore.getState();
        const layoutedNodes = await applyElkLayout(currentNodes, currentEdges);
        setStoreNodes(layoutedNodes);
        setNodes(layoutedNodes);

        // ELK 布局完成后，自适应画布
        setTimeout(() => {
          reactFlowInstanceRef.current?.fitView({
            padding: 0.2,
            duration: 300
          });
        }, 50);
      } catch (error) {
        console.error('布局失败:', error);
      } finally {
        setIsLayouting(false);
      }
    }, 100);
  };

  /**
   * 处理节点类型选择
   */
  const handleNodeSelect = async (type: NodeType, label: string) => {
    if (!insertContext) return;

    if (type === NodeType.LOOP_V2 || type === NodeType.ERROR_HANDLER) {
      insertLoopV2({
        edgeId: insertContext.edgeId,
        position: insertContext.position,
        nodeType: type
      });
    } else if (type === NodeType.PARALLEL || type === NodeType.CONDITION_BRANCH) {
      insertParallel({
        edgeId: insertContext.edgeId,
        position: insertContext.position,
        nodeType: type
      });
    } else {
      insertNodeOnEdge({
        edgeId: insertContext.edgeId,
        position: insertContext.position,
        nodeType: type,
        nodeLabel: label
      });
    }

    // 更新本地状态后重新布局
    syncStoreAndApplyLayout();

    setIsModalOpen(false);
    setInsertContext(null);
  };

  /**
   * 处理模态框关闭
   */
  const handleModalClose = () => {
    setIsModalOpen(false);
    setInsertContext(null);
  };

  /**
   * 处理并行处理节点点击新增分支
   */
  const handleAddParallelBranch = (params: {
    parallelId: string;
  }) => {
    addParallelBranch({
      parallelId: params.parallelId
    });
    syncStoreAndApplyLayout();
  };

  /**
   * 处理并行分支删除按钮点击
   */
  const handleRemoveParallelBranch = (params: {
    parallelGroupId: string;
    branchId: string;
  }) => {
    removeParallelBranch({
      parallelGroupId: params.parallelGroupId,
      branchId: params.branchId
    });
    syncStoreAndApplyLayout();
  };

  /**
   * 处理流程节点删除按钮点击
   */
  const handleRemoveFlowNode = (params: {
    /** 要删除的节点 ID */
    nodeId: string;
  }) => {
    removeFlowNode({
      nodeId: params.nodeId
    });
    syncStoreAndApplyLayout();
  };

  // 注册插入节点和并行结构事件回调
  React.useEffect(() => {
    registerInsertNodeCallback(handleInsertNode);
    registerAddParallelBranchCallback(handleAddParallelBranch);
    registerRemoveParallelBranchCallback(handleRemoveParallelBranch);
    registerRemoveFlowNodeCallback(handleRemoveFlowNode);
    return () => {
      unregisterInsertNodeCallback();
    };
  }, []);

  /**
   * 处理连线创建
   */
  const onConnect = (params: Connection) => {
    storeOnConnect(params);
    const { nodes: updatedNodes, edges: updatedEdges } = useFlowStore.getState();
    setNodes(updatedNodes);
    setEdges(updatedEdges);
  };

  /**
   * 应用自动布局
   * 使用 ELK 算法自动排列节点
   */
  const handleAutoLayout = async () => {
    if (isLayouting) return;
    setIsLayouting(true);

    try {
      const { nodes: currentNodes, edges: currentEdges } = useFlowStore.getState();
      const layoutedNodes = await applyElkLayout(currentNodes, currentEdges);
      setStoreNodes(layoutedNodes);
      setNodes(layoutedNodes);
    } catch (error) {
      console.error('布局失败:', error);
    } finally {
      setIsLayouting(false);
    }
  };

  return (
    <div className="flow-canvas">
      <ReactFlow
        nodes={nodes}
        edges={edges}
        onNodesChange={onNodesChange}
        onEdgesChange={onEdgesChange}
        onConnect={onConnect}
        onInit={onInit}
        nodeTypes={nodeTypes}
        edgeTypes={edgeTypes}
        // nodesDraggable={false}
        // nodesConnectable={false}
        fitView
        defaultEdgeOptions={{
          type: 'insert-edge',
          animated: false,
          style: { stroke: '#6366f1', strokeWidth: 2 }
        }}
      >
        <Background color="#f1f5f9" gap={20} />
        <Controls showInteractive={false} />
      </ReactFlow>

      {/* 节点选择模态框 */}
      <NodeSelectModal
        isOpen={isModalOpen}
        onClose={handleModalClose}
        onSelect={handleNodeSelect}
        position={{ x: 0, y: 0 }}
      />

      {/* 工具栏 */}
      <div className="canvas-toolbar">
        <button
          className="toolbar-btn"
          onClick={handleAutoLayout}
          disabled={isLayouting}
          title="自动布局"
        >
          {isLayouting ? '⏳' : '🎯'} 自动布局
        </button>
        <div className="toolbar-divider" />
        <div className="toolbar-info">
          节点: {nodes.length} | 连线: {edges.length}
        </div>
      </div>
    </div>
  );
};

export default FlowCanvas;