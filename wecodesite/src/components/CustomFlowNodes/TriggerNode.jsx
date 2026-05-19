/**
 * ========================================
 * 触发器节点组件
 * ========================================
 * 
 * 功能：
 * - 显示触发器节点的标准样式
 * - 蓝色边框表示起始节点
 * - 底部输出连接点
 */

import React, { memo } from 'react';
import { Handle, Position } from '@xyflow/react';

/**
 * 触发器节点组件
 * 
 * @param {Object} props
 * @param {Object} props.data - 节点数据
 * @param {string} props.data.label - 节点显示名称
 * @param {Object} props.data.config - 节点配置
 * @param {boolean} props.selected - 是否被选中
 */
const TriggerNode = ({ data, selected }) => {
  return (
    <div 
      style={{ 
        padding: '14px 18px', 
        border: `2px solid ${selected ? '#1890ff' : '#1890ff'}`,
        borderRadius: 10,
        backgroundColor: '#fff',
        minWidth: 160,
        maxWidth: 200,
        boxShadow: selected 
          ? '0 4px 12px rgba(24, 144, 255, 0.4)' 
          : '0 2px 8px rgba(0, 0, 0, 0.1)',
        transition: 'all 0.2s ease',
      }}
    >
      {/* 输出连接点 - 底部 */}
      <Handle 
        type="source" 
        position={Position.Bottom}
        style={{
          bottom: -6,
          background: '#1890ff',
          border: '2px solid #fff',
          width: 12,
          height: 12,
        }}
      />
      
      {/* 节点类型标签 */}
      <div style={{ 
        fontSize: 11, 
        color: '#1890ff', 
        marginBottom: 6,
        fontWeight: 600,
        textTransform: 'uppercase',
        letterSpacing: '0.5px',
      }}>
        触发器
      </div>
      
      {/* 节点名称 */}
      <div style={{ 
        fontSize: 14, 
        fontWeight: 600, 
        color: '#333',
        marginBottom: 4,
      }}>
        {data.label || '触发器'}
      </div>
      
      {/* 触发类型 */}
      {data.config?.triggerType && (
        <div style={{ 
          fontSize: 11, 
          color: '#666',
          backgroundColor: '#e6f7ff',
          padding: '2px 6px',
          borderRadius: 3,
          display: 'inline-block',
        }}>
          {data.config.triggerType === 'schedule' ? '定时触发' :
           data.config.triggerType === 'webhook' ? 'Webhook' : 'API触发'}
        </div>
      )}
    </div>
  );
};

export default memo(TriggerNode);
