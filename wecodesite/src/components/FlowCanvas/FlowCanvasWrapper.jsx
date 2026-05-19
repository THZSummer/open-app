import React from 'react';
import { ReactFlow, Controls, Background, BackgroundVariant } from '@xyflow/react';
import '@xyflow/react/dist/style.css';
import './FlowCanvas.less';

/**
 * 流程画布包装组件
 * 
 * 封装ReactFlow的核心功能，提供统一的画布基础能力
 * 
 * @param {Object} props
 * @param {Array} props.nodes - 节点数组
 * @param {Array} props.edges - 连线数组
 * @param {Function} props.onNodesChange - 节点变更回调
 * @param {Function} props.onEdgesChange - 连线变更回调
 * @param {Function} props.onConnect - 连接创建回调
 * @param {Function} props.onNodeDragStop - 节点拖拽结束回调
 * @param {Function} props.onNodeClick - 节点点击回调
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
}) {
  return (
    <div className="flow-canvas-wrapper">
      <ReactFlow
        nodes={nodes}
        edges={edges}
        onNodesChange={onNodesChange}
        onEdgesChange={onEdgesChange}
        onConnect={onConnect}
        onNodeDragStop={onNodeDragStop}
        onNodeClick={onNodeClick}
        fitView
        snapToGrid
        snapGrid={[20, 20]}
        deleteKeyCode={['Backspace', 'Delete']}
        multiSelectionKeyCode={['Control', 'Meta']}
      >
        {/* Controls组件提供缩放和重置大小功能 */}
        <Controls />
        
        {/* 背景网格 */}
        <Background 
          variant={BackgroundVariant.Dots} 
          gap={20} 
          size={1} 
        />
        
        {/* 子组件插槽 */}
        {children}
      </ReactFlow>
    </div>
  );
}

export default FlowCanvasWrapper;
