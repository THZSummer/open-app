/**
 * ========================================
 * 条件分支节点组件
 * ========================================
 * 
 * 功能：
 * - 显示条件分支节点的标准样式
 * - 橙色边框表示条件判断
 * - 顶部输入连接点
 * - 底部两个输出连接点（条件满足/不满足）
 */

import React, { memo } from 'react';
import { Handle, Position } from '@xyflow/react';

/**
 * 条件分支节点组件
 * 
 * @param {Object} props
 * @param {Object} props.data - 节点数据
 * @param {string} props.data.label - 节点显示名称
 * @param {Object} props.data.config - 节点配置
 * @param {boolean} props.selected - 是否被选中
 */
const ConditionNode = ({ data, selected }) => {
  return (
    <div 
      style={{ 
        padding: '14px 18px', 
        border: `2px solid ${selected ? '#faad14' : '#faad14'}`,
        borderRadius: 10,
        backgroundColor: '#fff',
        minWidth: 180,
        maxWidth: 220,
        boxShadow: selected 
          ? '0 4px 12px rgba(250, 173, 20, 0.4)' 
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
          background: '#faad14',
          border: '2px solid #fff',
          width: 12,
          height: 12,
        }}
      />
      
      {/* 节点类型标签 */}
      <div style={{ 
        fontSize: 11, 
        color: '#faad14', 
        marginBottom: 6,
        fontWeight: 600,
        textTransform: 'uppercase',
        letterSpacing: '0.5px',
      }}>
        条件分支
      </div>
      
      {/* 节点名称 */}
      <div style={{ 
        fontSize: 14, 
        fontWeight: 600, 
        color: '#333',
        marginBottom: 10,
      }}>
        {data.label || '条件分支'}
      </div>
      
      {/* 分支标签 */}
      <div style={{ 
        display: 'flex', 
        justifyContent: 'space-between',
        marginTop: 8,
      }}>
        <div style={{
          flex: 1,
          textAlign: 'center',
          padding: '4px 8px',
          backgroundColor: '#fffbe6',
          borderRadius: 4,
          fontSize: 11,
          color: '#ad8b00',
          border: '1px solid #ffe58f',
        }}>
          条件满足
        </div>
        <div style={{ width: 8 }} />
        <div style={{
          flex: 1,
          textAlign: 'center',
          padding: '4px 8px',
          backgroundColor: '#f5f5f5',
          borderRadius: 4,
          fontSize: 11,
          color: '#666',
        }}>
          条件不满足
        </div>
      </div>
      
      {/* 输出连接点 */}
      <Handle 
        type="source" 
        position={Position.Bottom}
        id="yes"
        style={{
          bottom: -6,
          left: '30%',
          background: '#52c41a',
          border: '2px solid #fff',
          width: 10,
          height: 10,
        }}
      />
      <Handle 
        type="source" 
        position={Position.Bottom}
        id="no"
        style={{
          bottom: -6,
          left: '70%',
          background: '#999',
          border: '2px solid #fff',
          width: 10,
          height: 10,
        }}
      />
    </div>
  );
};

export default memo(ConditionNode);
