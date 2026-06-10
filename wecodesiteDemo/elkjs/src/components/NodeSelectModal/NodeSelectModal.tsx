import React from 'react';
import { NodeType } from '../../types/flow';
import { defaultNodeTypes } from '../nodes';
import './NodeSelectModal.css';

/**
 * 节点选择模态框组件
 * 用于在插入节点时选择动作或循环节点
 */
interface NodeSelectModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSelect: (type: NodeType, label: string) => void;
  position: { x: number; y: number };
}

const NodeSelectModal: React.FC<NodeSelectModalProps> = ({
  isOpen,
  onClose,
  onSelect
}) => {
  // 如果模态框未打开，不渲染
  if (!isOpen) {
    return null;
  }

  /**
   * 处理节点类型选择
   */
  const handleSelect = (type: NodeType, label: string) => {
    onSelect(type, label);
    onClose();
  };

  /**
   * 处理背景点击关闭
   */
  const handleBackdropClick = (e: React.MouseEvent) => {
    if (e.target === e.currentTarget) {
      onClose();
    }
  };

  return (
    <div className="modal-backdrop" onClick={handleBackdropClick}>
      <div className="modal-content">
        <div className="modal-header">
          <h3>选择节点类型</h3>
          <button className="modal-close" onClick={onClose}>
            ×
          </button>
        </div>

        <div className="modal-body">
          <div className="node-type-grid">
            {defaultNodeTypes.map((nodeType) => (
              <div
                key={nodeType.type}
                className="node-type-item"
                onClick={() =>
                  handleSelect(nodeType.type as NodeType, nodeType.label)
                }
                style={{ borderColor: nodeType.color }}
              >
                <div
                  className="node-type-icon"
                  style={{ background: nodeType.color }}
                >
                  {getNodeIcon(nodeType.type)}
                </div>
                <div className="node-type-info">
                  <div className="node-type-label">{nodeType.label}</div>
                  <div className="node-type-desc">
                    {getNodeDescription(nodeType.type)}
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};

/**
 * 获取节点图标
 */
function getNodeIcon(type: string): string {
  const icons: Record<string, string> = {
    action: '🔧',
    'loop-v2': '🔄',
    'error-handler': '⚠️',
    parallel: '☷'
  };
  return icons[type] || '📦';
}

/**
 * 获取节点描述
 */
function getNodeDescription(type: string): string {
  const descriptions: Record<string, string> = {
    action: '执行具体动作',
    'loop-v2': '循环执行节点',
    'error-handler': '错误处理节点',
    parallel: '并行处理节点'
  };
  return descriptions[type] || '';
}

export default NodeSelectModal;
