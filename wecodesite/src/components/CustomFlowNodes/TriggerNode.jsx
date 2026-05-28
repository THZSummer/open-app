/**
 * ========================================
 * 触发器节点组件
 * ========================================
 * 
 * 功能：
 * - 显示触发器节点的标准样式
 * - 蓝色边框表示起始节点
 * - 底部输出连接点
 * - 左右两侧各添加一个连接点，支持更多连线场景
 * - 节点名称支持中英文显示
 */

import React, { memo } from 'react';
import { Handle, Position } from '@xyflow/react';
import './FlowNodes.m.less';

/**
 * 触发器节点组件
 *
 * @param {Object} props
 * @param {Object} props.data - 节点数据
 * @param {string} props.data.label - 节点显示名称（中文）
 * @param {string} props.data.labelEn - 节点英文名称
 * @param {Object} props.data.config - 节点配置
 * @param {boolean} props.selected - 是否被选中
 */
const TriggerNode = ({ data, selected }) => {
  return (
    <div className="triggerNode">
      {/* 输出连接点 - 底部 */}
      <Handle
        type="source"
        position={Position.Bottom}
        id="bottom-source"
        className="handleTrigger"
      />

      {/* 节点类型标签 */}
      <div className="nodeTypeLabel">
        触发器
      </div>

      {/* 节点名称 */}
      <div className="nodeName">
        {data.labelCn}
      </div>

      {/* 触发类型 */}
      {data.config?.triggerType && (
        <div className="nodeInfo">
          {data.config.triggerType.toUpperCase()}
        </div>
      )}
    </div>
  );
};

export default memo(TriggerNode);
