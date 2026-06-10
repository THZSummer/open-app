import React, { memo } from 'react';
import { Handle, NodeProps, Position } from 'reactflow';
import { FlowNodeData } from '../../types/flow';
import { triggerRemoveParallelBranch } from '../../store/edgeEventBus';
import './TextNode.css';

/**
 * 文本节点组件
 * 用于展示流程线或特殊结构中的说明文字，同时保留输入和输出连接点
 */
const TextNode: React.FC<NodeProps<FlowNodeData>> = ({ data, selected }) => {
  const parallelGroupId = data.config?.parallelGroupId;
  const parallelBranchId = data.config?.parallelBranchId;
  const shouldShowDeleteButton =
    data.config?.parallelRole === 'branch-start' && parallelGroupId && parallelBranchId;

  /**
   * 处理并行分支删除按钮点击
   * 删除按钮只在分支开始文本节点上展示
   */
  const handleDeleteBranch = (event: React.MouseEvent) => {
    event.stopPropagation();

    if (!parallelGroupId || !parallelBranchId) {
      return;
    }

    triggerRemoveParallelBranch({
      parallelGroupId: String(parallelGroupId),
      branchId: String(parallelBranchId)
    });
  };

  return (
    <div className={`text-node ${selected ? 'selected' : ''}`}>
      {/* 文本节点输入连接点 */}
      <Handle
        type="target"
        position={Position.Top}
        className="text-node-handle"
      />

      {/* 文本节点主体内容 */}
      <div className="text-node-content">{data.label}</div>

      {/* 并行分支删除按钮 */}
      {shouldShowDeleteButton && (
        <button
          className="text-node-delete"
          type="button"
          title="删除分支"
          onClick={handleDeleteBranch}
        >
          ×
        </button>
      )}

      {/* 文本节点输出连接点 */}
      <Handle
        type="source"
        position={Position.Bottom}
        className="text-node-handle"
      />
    </div>
  );
};

export default memo(TextNode);
