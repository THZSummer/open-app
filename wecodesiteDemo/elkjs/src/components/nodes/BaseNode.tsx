import React, { memo } from 'react';
import { Handle, Position, NodeProps } from 'reactflow';
import { FlowNodeData, NodeType } from '../../types/flow';
import {
  triggerAddParallelBranch,
  triggerRemoveFlowNode
} from '../../store/edgeEventBus';
import './BaseNode.css';

/**
 * 基础节点组件
 * 提供通用的节点结构和样式
 */
const BaseNode: React.FC<NodeProps<FlowNodeData>> = ({
  id,
  data,
  selected
}) => {
  // 根据节点类型获取颜色
  const getNodeColor = (type: NodeType): string => {
    const colors: Record<NodeType, string> = {
      [NodeType.TRIGGER]: 'var(--node-trigger)',
      [NodeType.ACTION]: 'var(--node-action)',
      [NodeType.END]: 'var(--node-end)',
      [NodeType.TEXT]: 'transparent',
      [NodeType.LOOP_V2]: 'var(--node-loop)',
      [NodeType.ERROR_HANDLER]: 'var(--node-loop)',
      [NodeType.PARALLEL]: 'var(--node-action)'
    };
    return colors[type] || 'var(--node-action)';
  };

  // 获取节点图标
  const getNodeIcon = (type: NodeType): string => {
    const icons: Record<NodeType, string> = {
      [NodeType.TRIGGER]: '⚡',
      [NodeType.ACTION]: '🔧',
      [NodeType.END]: '🏁',
      [NodeType.TEXT]: '',
      [NodeType.LOOP_V2]: '🔄',
      [NodeType.ERROR_HANDLER]: '⚠️',
      [NodeType.PARALLEL]: '☷'
    };
    return icons[type] || '📦';
  };

  const nodeColor = getNodeColor(data.type);
  const nodeIcon = getNodeIcon(data.type);
  const canDeleteNode = [
    NodeType.ACTION,
    NodeType.LOOP_V2,
    NodeType.ERROR_HANDLER,
    NodeType.PARALLEL
  ].includes(data.type);

  /**
   * 处理基础节点点击
   * 并行处理节点被点击时直接新增一个分支
   */
  const handleNodeClick = (event: React.MouseEvent) => {
    if (data.type !== NodeType.PARALLEL) {
      return;
    }

    event.stopPropagation();
    triggerAddParallelBranch({
      parallelId: id
    });
  };

  /**
   * 处理流程节点删除按钮点击
   * 删除按钮需要阻止冒泡，避免并行节点误触发新增分支
   */
  const handleDeleteNode = (event: React.MouseEvent) => {
    event.stopPropagation();
    triggerRemoveFlowNode({
      nodeId: id
    });
  };

  return (
    <div
      className={`base-node ${selected ? 'selected' : ''}`}
      draggable={false}
      onClick={handleNodeClick}
      style={{
        borderColor: nodeColor,
        boxShadow: selected
          ? `0 0 0 2px ${nodeColor}, 0 4px 12px rgba(0,0,0,0.15)`
          : '0 4px 12px rgba(0,0,0,0.08)',
        cursor: 'default'
      }}
    >
      {/* 节点删除按钮 */}
      {canDeleteNode && (
        <button
          className="base-node-delete"
          type="button"
          title="删除节点"
          onClick={handleDeleteNode}
        >
          ×
        </button>
      )}

      {/* 节点输入 Handle */}
      <Handle
        type="target"
        position={Position.Top}
        className="node-handle"
        style={{ background: nodeColor }}
      />

      {/* 节点内容 */}
      <div className="node-content">
        <div className="node-header" style={{ background: nodeColor }}>
          <span className="node-icon">{nodeIcon}</span>
          <span className="node-type">
            {String(data.type).toUpperCase()}
          </span>
        </div>
        <div className="node-body">
          <div className="node-label">{data.label}</div>
          {data.description && (
            <div className="node-description">{data.description}</div>
          )}
        </div>
      </div>

      {/* 节点输出 Handle */}
      <Handle
        type="source"
        position={Position.Bottom}
        className="node-handle"
        style={{ background: nodeColor }}
      />
    </div>
  );
};

export default memo(BaseNode);
