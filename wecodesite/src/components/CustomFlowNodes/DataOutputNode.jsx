/**
 * ========================================
 * 数据输出节点组件
 * ========================================
 * 
 * 功能：
 * - 显示数据输出节点的标准样式
 * - 橙色边框表示数据输出
 * - 顶部输入连接点，底部输出连接点
 * - 节点名称支持中英文显示
 */

import React, { memo } from 'react';
import { Handle, Position } from '@xyflow/react';
import './FlowNodes.m.less';

/**
 * 数据输出节点组件
 *
 * @param {Object} props
 * @param {Object} props.data - 节点数据
 * @param {string} props.data.label - 节点显示名称（中文）
 * @param {string} props.data.labelEn - 节点英文名称
 * @param {Object} props.data.config - 节点配置
 * @param {string} props.data.config.sourceType - 接口来源类型：connector 或 custom
 * @param {string} props.data.config.connectorName - 连接器名称（从连接器选择时）
 * @param {string} props.data.config.apiUrl - API地址（手动输入时）
 * @param {string} props.data.config.httpMethod - HTTP方法（手动输入时）
 * @param {boolean} props.selected - 是否被选中
 */
const DataOutputNode = ({ data, selected }) => {
  /**
   * 获取接口信息显示
   *
   * @returns {string|null} 接口信息文本或null
   */
  const getApiInfo = () => {
    if (data.config?.sourceType === 'connector' && data.config?.connectorName) {
      return data.config.connectorName;
    }
    if (data.config?.sourceType === 'custom' && data.config?.apiUrl) {
      const url = data.config.apiUrl;
      return url.length > 20 ? url.substring(0, 20) + '...' : url;
    }
    return null;
  };

  const apiInfo = getApiInfo();

  return (
    <div className="dataOutputNode">
      {/* 输入连接点 - 顶部 */}
      <Handle
        type="target"
        position={Position.Top}
        id="top-target"
        className="handleDataOutput"
      />

      {/* 节点类型标签 */}
      <div className="nodeTypeLabel">
        数据输出
      </div>

      {/* 节点名称 */}
      <div className="nodeName">
        {data.labelCn}
      </div>

      {/* 接口信息 */}
      {apiInfo && (
        <div className="nodeInfo apiInfoText">
          {apiInfo}
        </div>
      )}
    </div>
  );
};

export default memo(DataOutputNode);
