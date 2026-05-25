/**
 * ========================================
 * 数据输出节点组件
 * ========================================
 * 
 * 功能：
 * - 显示数据输出节点的标准样式
 * - 橙色边框表示数据输出
 * - 顶部输入连接点，底部输出连接点
 */

import React, { memo } from 'react';
import { Handle, Position } from '@xyflow/react';

/**
 * 数据输出节点组件
 * 
 * @param {Object} props
 * @param {Object} props.data - 节点数据
 * @param {string} props.data.label - 节点显示名称
 * @param {Object} props.data.config - 节点配置
 * @param {string} props.data.config.sourceType - 接口来源类型：connector 或 custom
 * @param {string} props.data.config.connectorName - 连接器名称（从连接器选择时）
 * @param {string} props.data.config.apiUrl - API地址（手动输入时）
 * @param {string} props.data.config.httpMethod - HTTP方法（手动输入时）
 * @param {boolean} props.selected - 是否被选中
 */
const DataOutputNode = ({ data, selected }) => {
  // 获取接口信息显示
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
    <div
      style={{
        padding: '14px 18px',
        borderRadius: 10,
        backgroundColor: '#fff',
        minWidth: 160,
        maxWidth: 200,
        boxShadow: '0 4px 12px rgba(0, 0, 0, 0.1)',
        transition: 'all 0.2s ease',
      }}
    >
      {/* 输入连接点 - 顶部 */}
      <Handle 
        type="target" 
        position={Position.Top}
        id="top-target"
        style={{
          background: '#fa8c16',
          border: '2px solid #fff',
          width: 12,
          height: 12,
        }}
      />
      
      {/* 输出连接点 - 底部 */}
      <Handle 
        type="source" 
        position={Position.Bottom}
        id="bottom-source"
        style={{
          background: '#fa8c16',
          border: '2px solid #fff',
          width: 12,
          height: 12,
        }}
      />
      
      {/* 节点类型标签 */}
      <div style={{ 
        fontSize: 11, 
        color: '#fa8c16', 
        marginBottom: 6,
        fontWeight: 600,
        textTransform: 'uppercase',
        letterSpacing: '0.5px',
      }}>
        数据输出
      </div>
      
      {/* 节点名称 */}
      <div style={{ 
        fontSize: 14, 
        fontWeight: 600, 
        color: '#333',
        marginBottom: 4,
      }}>
        {data.label || '数据输出'}
      </div>
      
      {/* 接口信息 */}
      {apiInfo && (
        <div style={{ 
          fontSize: 11, 
          color: '#666',
          backgroundColor: '#fff7e6',
          padding: '2px 6px',
          borderRadius: 3,
          display: 'inline-block',
          maxWidth: '100%',
          overflow: 'hidden',
          textOverflow: 'ellipsis',
          whiteSpace: 'nowrap',
        }}>
          {apiInfo}
        </div>
      )}
    </div>
  );
};

export default memo(DataOutputNode);
