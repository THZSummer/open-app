/**
 * ========================================
 * 循环执行节点组件
 * ========================================
 * 
 * 功能：
 * - 显示循环执行节点的标准样式
 * - 粉色边框表示循环
 * - 顶部输入连接点，底部输出连接点
 * - 左右两侧各添加一个连接点，支持更多连线场景
 */

import React, { memo } from 'react';
import { Handle, Position } from '@xyflow/react';

/**
 * 循环执行节点组件
 * 
 * @param {Object} props
 * @param {Object} props.data - 节点数据
 * @param {string} props.data.label - 节点显示名称
 * @param {Object} props.data.config - 节点配置
 * @param {string} props.data.config.loopType - 循环类型（times/while/until）
 * @param {number} props.data.config.maxIterations - 最大迭代次数
 * @param {boolean} props.selected - 是否被选中
 */
const LoopNode = ({ data, selected }) => {
  const loopType = data.config?.loopType;
  const maxIterations = data.config?.maxIterations;

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
      {/* 左侧输入连接点 */}
      <Handle 
        type="target" 
        position={Position.Left}
        id="left-target"
        style={{
          background: '#eb2f96',
          border: '2px solid #fff',
          width: 12,
          height: 12,
        }}
      />
      
      {/* 输入连接点 - 顶部 */}
      <Handle 
        type="target" 
        position={Position.Top}
        id="top-target"
        style={{
          background: '#eb2f96',
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
          background: '#eb2f96',
          border: '2px solid #fff',
          width: 12,
          height: 12,
        }}
      />
      
      {/* 右侧输出连接点 */}
      <Handle 
        type="source" 
        position={Position.Right}
        id="right-source"
        style={{
          background: '#eb2f96',
          border: '2px solid #fff',
          width: 12,
          height: 12,
        }}
      />
      
      {/* 节点类型标签 */}
      <div style={{ 
        fontSize: 11, 
        color: '#eb2f96', 
        marginBottom: 6,
        fontWeight: 600,
        textTransform: 'uppercase',
        letterSpacing: '0.5px',
      }}>
        循环执行
      </div>
      
      {/* 节点名称 */}
      <div style={{ 
        fontSize: 14, 
        fontWeight: 600, 
        color: '#333',
        marginBottom: 4,
      }}>
        {data.label || '循环执行'}
      </div>
      
      {/* 循环信息 */}
      {loopType && (
        <div style={{ 
          fontSize: 12, 
          color: '#eb2f96',
          fontWeight: 500,
        }}>
          {loopType === 'times' ? (
            <>🔄 循环 {maxIterations || 1} 次</>
          ) : loopType === 'while' ? (
            <>🔄 条件循环</>
          ) : (
            <>🔄 直到条件</>
          )}
        </div>
      )}
    </div>
  );
};

export default memo(LoopNode);
