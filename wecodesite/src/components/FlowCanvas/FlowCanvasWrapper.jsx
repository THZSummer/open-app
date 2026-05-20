import React, { useCallback } from 'react';
import { ReactFlow, Controls, Background, BackgroundVariant, ReactFlowProvider, useReactFlow, useOnSelectionChange } from '@xyflow/react';
import '@xyflow/react/dist/style.css';
import './FlowCanvas.less';

/**
 * 内部ReactFlow组件
 * 使用ReactFlowProvider提供的上下文处理拖拽事件和坐标转换
 *
 * @param {Object} props - 组件属性
 */
function ReactFlowWithDragDrop(props) {
  const {
    nodes,
    edges,
    onNodesChange,
    onEdgesChange,
    onConnect,
    onNodeDragStop,
    onNodeClick,
    onNodeDrop,
    disableNodeInteraction,
    children,
    canDeleteNode,
    nodeTypes,
  } = props;

  const { screenToFlowPosition } = useReactFlow();

  /**
   * 处理节点变更（拦截删除操作）
   * 如果canDeleteNode返回false，则阻止删除节点
   *
   * @param {Array} changes - 节点变更数组
   */
  const handleNodesChange = useCallback((changes) => {
    if (!onNodesChange) return;

    // 过滤掉触发器节点的删除操作
    if (canDeleteNode) {
      const filteredChanges = changes.filter((change) => {
        // 如果是移除操作且节点不可删除，则过滤掉
        if (change.type === 'remove') {
          const node = nodes.find((n) => n.id === change.id);
          if (node && !canDeleteNode(node)) {
            return false;
          }
        }
        return true;
      });
      onNodesChange(filteredChanges);
    } else {
      onNodesChange(changes);
    }
  }, [onNodesChange, nodes, canDeleteNode]);

  /**
   * 处理节点放置
   * 使用screenToFlowPosition将屏幕坐标转换为画布坐标
   * 自动处理画布的缩放和平移变换
   *
   * @param {DragEvent} event - 拖拽事件
   */
  const handleDrop = useCallback((event) => {
    if (disableNodeInteraction) return;

    event.preventDefault();

    const data = event.dataTransfer.getData('application/reactflow');
    if (!data) return;

    try {
      const { type, label } = JSON.parse(data);

      const position = screenToFlowPosition({
        x: event.clientX,
        y: event.clientY,
      });

      if (onNodeDrop) {
        onNodeDrop(event, { type, label, position });
      }
    } catch (error) {
      console.error('处理节点放置失败:', error);
    }
  }, [screenToFlowPosition, onNodeDrop, disableNodeInteraction]);

  /**
   * 处理拖拽悬停
   * 阻止默认行为以允许放置
   *
   * @param {DragEvent} event - 拖拽事件
   */
  const handleDragOver = useCallback((event) => {
    if (disableNodeInteraction) return;
    event.preventDefault();
    event.dataTransfer.dropEffect = 'move';
  }, [disableNodeInteraction]);

  return (
    <ReactFlow
      nodes={nodes}
      edges={edges}
      onNodesChange={handleNodesChange}
      onEdgesChange={onEdgesChange}
      onConnect={onConnect}
      onNodeDragStop={onNodeDragStop}
      onNodeClick={onNodeClick}
      onDrop={handleDrop}
      onDragOver={handleDragOver}
      nodeTypes={nodeTypes}
      fitView
      snapToGrid
      snapGrid={[20, 20]}
      deleteKeyCode={['Backspace', 'Delete']}
      multiSelectionKeyCode={['Control', 'Meta']}
      nodesDraggable={!disableNodeInteraction}
      nodesConnectable={!disableNodeInteraction}
      elementsSelectable
    >
      <Controls />
      <Background
        variant={BackgroundVariant.Dots}
        gap={20}
        size={1}
      />
      {children}
    </ReactFlow>
  );
}

/**
 * 流程画布包装组件
 *
 * 封装ReactFlow的核心功能，提供统一的画布基础能力
 * 使用ReactFlowProvider确保所有子组件可以访问ReactFlow的状态和方法
 *
 * @param {Object} props - 组件属性
 * @param {Array} props.nodes - 节点数组
 * @param {Array} props.edges - 连线数组
 * @param {Function} props.onNodesChange - 节点变更回调
 * @param {Function} props.onEdgesChange - 连线变更回调
 * @param {Function} props.onConnect - 连接创建回调
 * @param {Function} props.onNodeDragStop - 节点拖拽结束回调
 * @param {Function} props.onNodeClick - 节点点击回调
 * @param {Function} props.onNodeDrop - 节点放置回调
 * @param {Boolean} props.disableNodeInteraction - 是否禁用节点交互（拖拽和添加）
 * @param {Function} props.canDeleteNode - 判断节点是否可删除的回调函数，参数为节点对象，返回布尔值
 * @param {Object} props.nodeTypes - 自定义节点类型映射表
 * @param {React.ReactNode} props.children - 子组件
 */
function FlowCanvasWrapper({
  children,
  nodes,
  edges,
  onNodesChange,
  onEdgesChange,
  onConnect,
  onNodeDragStop,
  onNodeClick,
  onNodeDrop,
  disableNodeInteraction,
  canDeleteNode,
  nodeTypes,
}) {
  return (
    <div className="flow-canvas-wrapper">
      <ReactFlowProvider>
        <ReactFlowWithDragDrop
          nodes={nodes}
          edges={edges}
          onNodesChange={onNodesChange}
          onEdgesChange={onEdgesChange}
          onConnect={onConnect}
          onNodeDragStop={onNodeDragStop}
          onNodeClick={onNodeClick}
          onNodeDrop={onNodeDrop}
          disableNodeInteraction={disableNodeInteraction}
          canDeleteNode={canDeleteNode}
          nodeTypes={nodeTypes}
        >
          {children}
        </ReactFlowWithDragDrop>
      </ReactFlowProvider>
    </div>
  );
}

export default FlowCanvasWrapper;
