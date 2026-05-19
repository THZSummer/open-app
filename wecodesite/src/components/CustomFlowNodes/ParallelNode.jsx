/**
 * ========================================
 * 并行执行节点组件
 * ========================================
 * 
 * 功能：
 * - 显示并行执行节点的标准样式
 * - 青色边框表示并行
 * - 顶部输入连接点
 * - 底部多个输出连接点（根据分支数量）
 */

import React, { memo } from 'react';
import { Handle, Position } from '@xyflow/react';

/**
 * 并行执行节点组件
 * 
 * @param {Object} props
 * @param {Object} props.data - 节点数据
 * @param {string} props.data.label - 节点显示名称
 * @param {Array} props.data.config?.branches - 分支列表
 * @param {boolean} props.selected - 是否被选中
 */
const ParallelNode = ({ data, selected }) => {
  const branches = data.config?.branches || [];
  const branchCount = Math.max(branches.length, 2);
  
  return (
    <div 
      style={{ 
        padding: '14px 18px', 
        border: `2px solid ${selected ? '#13c2c2' : '#13c2c2'}`,
        borderRadius: 10,
        backgroundColor: '#fff',
        minWidth: 180,
        maxWidth: 240,
        boxShadow: selected 
          ? '0 4px 12px rgba(19, 194, 194, 0.4)' 
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
          background: '#13c2c2',
          border: '2px solid #fff',
          width: 12,
          height: 12,
        }}
      />
      
      {/* 节点类型标签 */}
      <div style={{ 
        fontSize: 11, 
        color: '#13c2c2', 
        marginBottom: 6,
        fontWeight: 600,
        textTransform: 'uppercase',
        letterSpacing: '0.5px',
      }}>
        并行执行
      </div>
      
      {/* 节点名称 */}
      <div style={{ 
        fontSize: 14, 
        fontWeight: 600, 
        color: '#333',
        marginBottom: 8,
      }}>
        {data.label || '并行执行'}
      </div>
      
      {/* 分支数量指示 */}
      <div style={{ 
        fontSize: 11, 
        color: '#666',
        backgroundColor: '#e6fffb',
        padding: '2px 6px',
        borderRadius: 3,
        display: 'inline-block',
      }}>
        {branchCount} 个并行分支
      </div>
      
      {/* 输出连接点 - 底部 */}
      {branches.length > 0 ? (
        branches.map((branch, index) => (
          <Handle 
            key={index}
            type="source" 
            position={Position.Bottom}
            id={`branch-${index}`}
            style={{
              bottom: -6,
              left: `${30 + index * (40 / (branches.length - 1 || 1))}%`,
              background: '#13c2c2',
              border: '2px solid #fff',
              width: 10,
              height: 10,
            }}
          />
        ))
      ) : (
        <>
          <Handle 
            type="source" 
            position={Position.Bottom}
            style={{
              bottom: -6,
              left: '30%',
              background: '#13c2c2',
              border: '2px solid #fff',
              width: 10,
              height: 10,
            }}
          />
          <Handle 
            type="source" 
            position={Position.Bottom}
            style={{
              bottom: -6,
              left: '70%',
              background: '#13c2c2',
              border: '2px solid #fff',
              width: 10,
              height: 10,
            }}
          />
        </>
      )}
    </div>
  );
};

export default memo(ParallelNode);
