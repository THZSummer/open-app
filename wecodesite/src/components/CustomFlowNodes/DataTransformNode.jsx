/**
 * ========================================
 * 数据处理节点组件
 * ========================================
 * 
 * 功能：
 * - 显示数据处理节点的标准样式
 * - 蓝色边框表示数据处理
 * - 顶部输入连接点，底部输出连接点
 */

import React, { memo } from 'react';
import { Handle, Position } from '@xyflow/react';

/**
 * 数据处理节点组件
 * 
 * @param {Object} props
 * @param {Object} props.data - 节点数据
 * @param {string} props.data.label - 节点显示名称
 * @param {Object} props.data.config - 节点配置
 * @param {Array} props.data.config.mappings - 字段映射列表
 * @param {boolean} props.selected - 是否被选中
 */
const DataTransformNode = ({ data, selected }) => {
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
          background: '#1890ff',
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
        数据处理
      </div>
      
      {/* 节点名称 */}
      <div style={{ 
        fontSize: 14, 
        fontWeight: 600, 
        color: '#333',
        marginBottom: 4,
      }}>
        {data.label || '数据处理'}
      </div>
      
      {/* 映射数量提示 */}
      {data.config?.mappings?.length > 0 && (
        <div style={{ 
          fontSize: 12, 
          color: '#1890ff',
          fontWeight: 500,
        }}>
          📝 {data.config.mappings.length} 个字段映射
        </div>
      )}
    </div>
  );
};

export default memo(DataTransformNode);
