/**
 * ========================================
 * 连接器节点组件（原执行动作节点）
 * ========================================
 * 
 * 功能：
 * - 显示连接器节点的标准样式
 * - 绿色边框表示连接器节点
 * - 顶部输入连接点，底部输出连接点
 * - 左右两侧各添加一个连接点，支持更多连线场景
 * - 节点名称支持中英文显示
 */

import React, { memo } from 'react';
import { Handle, Position } from '@xyflow/react';
import './FlowNodes.m.less';

/**
 * 连接器节点组件
 *
 * @param {Object} props
 * @param {Object} props.data - 节点数据
 * @param {string} props.data.label - 节点显示名称（中文）
 * @param {string} props.data.labelEn - 节点英文名称
 * @param {Object} props.data.config - 节点配置
 * @param {boolean} props.selected - 是否被选中
 */
const ActionNode = ({ data, selected }) => {
  return (
    <div className="actionNode">
      {/* 输入连接点 - 顶部 */}
      <Handle
        type="target"
        position={Position.Top}
        id="top-target"
        className="handleAction"
      />

      {/* 输出连接点 - 底部 */}
      <Handle
        type="source"
        position={Position.Bottom}
        id="bottom-source"
        className="handleAction"
      />

      {/* 节点类型标签 */}
      <div className="nodeTypeLabel">
        连接器
      </div>

      {/* 节点名称 */}
      <div className="nodeName">
        {data.labelCn}
      </div>

      {/* 连接器名称 */}
      {data.config?.connectorName && (
        <div className="nodeInfo">
          {data.config.connectorName}
        </div>
      )}
    </div>
  );
};

export default memo(ActionNode);
