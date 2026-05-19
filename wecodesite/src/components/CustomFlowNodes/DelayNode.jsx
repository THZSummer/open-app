/**
 * ========================================
 * 延时节点组件
 * ========================================
 * 
 * 功能：
 * - 显示延时节点的标准样式
 * - 紫色边框表示延时
 * - 顶部输入连接点，底部输出连接点
 */

import React, { memo } from 'react';
import { Handle, Position } from '@xyflow/react';

/**
 * 延时节点组件
 * 
 * @param {Object} props
 * @param {Object} props.data - 节点数据
 * @param {string} props.data.label - 节点显示名称
 * @param {Object} props.data.config - 节点配置
 * @param {number} props.data.config.duration - 延时时长（秒）
 * @param {boolean} props.selected - 是否被选中
 */
const DelayNode = ({ data, selected }) => {
  return (
    <div 
      style={{ 
        padding: '14px 18px', 
        border: `2px solid ${selected ? '#722ed1' : '#722ed1'}`,
        borderRadius: 10,
        backgroundColor: '#fff',
        minWidth: 160,
        maxWidth: 200,
        boxShadow: selected 
          ? '0 4px 12px rgba(114, 46, 209, 0.4)' 
          : '0 2px 8px rgba(0, 0, 0, 0.1)',
        transition: 'all 0.2s ease',
      }}
    >
      {/* 输入连接点 - 顶部 */}
      <Handle 
        type="target" 
        position={Position.Top}
        style={{
          top: -6,
          background: '#722ed1',
          border: '2px solid #fff',
          width: 12,
          height: 12,
        }}
      />
      
      {/* 输出连接点 - 底部 */}
      <Handle 
        type="source" 
        position={Position.Bottom}
        style={{
          bottom: -6,
          background: '#722ed1',
          border: '2px solid #fff',
          width: 12,
          height: 12,
        }}
      />
      
      {/* 节点类型标签 */}
      <div style={{ 
        fontSize: 11, 
        color: '#722ed1', 
        marginBottom: 6,
        fontWeight: 600,
        textTransform: 'uppercase',
        letterSpacing: '0.5px',
      }}>
        延时
      </div>
      
      {/* 节点名称 */}
      <div style={{ 
        fontSize: 14, 
        fontWeight: 600, 
        color: '#333',
        marginBottom: 4,
      }}>
        {data.label || '延时'}
      </div>
      
      {/* 延时时长 */}
      {data.config?.duration && (
        <div style={{ 
          fontSize: 12, 
          color: '#722ed1',
          fontWeight: 500,
        }}>
          ⏱ {data.config.duration} 秒
        </div>
      )}
    </div>
  );
};

export default memo(DelayNode);
