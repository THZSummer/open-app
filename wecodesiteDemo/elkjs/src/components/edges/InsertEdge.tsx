import React, { memo } from 'react';
import { EdgeProps, EdgeLabelRenderer } from 'reactflow';
import { triggerInsertNode } from '../../store/edgeEventBus';

/**
 * 计算横折线路径和标签位置
 * 多出边时先从源节点向下延伸再分叉，多入边时先在目标节点上方合并
 */
function getHorizontalStepPath(params: {
  sourceX: number;
  sourceY: number;
  targetX: number;
  targetY: number;
  sourceEdgeTotal?: number;
  targetEdgeTotal?: number;
}) {
  const {
    sourceX,
    sourceY,
    targetX,
    targetY,
    sourceEdgeTotal = 1,
    targetEdgeTotal = 1
  } = params;

  // 多出口时使用源节点下方的公共分叉点
  const branchOffset = 36;
  const shouldBranch = sourceEdgeTotal > 1;
  const branchY = shouldBranch ? sourceY + branchOffset : (sourceY + targetY) / 2;

  // 多入口时使用目标节点上方的公共汇合点
  const mergeOffset = 36;
  const shouldMerge = targetEdgeTotal > 1;
  const mergeY = shouldMerge ? targetY - mergeOffset : targetY;

  // 路径公式：源点 -> 公共分叉点 -> 水平段 -> 公共汇合点 -> 目标点
  // 多入口时统一在 mergeY 高度水平转折，确保左右两侧连线水平对齐，整体呈方形效果
  const horizontalY = shouldMerge ? mergeY : branchY;
  const path = shouldMerge
    ? `M ${sourceX},${sourceY} L ${sourceX},${horizontalY} L ${targetX},${horizontalY} L ${targetX},${targetY}`
    : `M ${sourceX},${sourceY} L ${sourceX},${horizontalY} L ${targetX},${horizontalY} L ${targetX},${targetY}`;

  // 标签位置放在主要水平段中点
  const labelX = (sourceX + targetX) / 2;
  const labelY = horizontalY;

  return {
    edgePath: path,
    labelX,
    labelY
  };
}

/**
 * 自定义边组件
 * 在连线中点显示插入按钮，支持点击插入节点
 */
const InsertEdge: React.FC<EdgeProps> = ({
  id,
  sourceX,
  sourceY,
  targetX,
  targetY,
  style = {},
  markerEnd,
  data
}) => {
  // 计算横折线的路径和插入按钮位置
  const { edgePath, labelX, labelY } = getHorizontalStepPath({
    sourceX,
    sourceY,
    targetX,
    targetY,
    sourceEdgeTotal: data?.sourceEdgeMeta?.total,
    targetEdgeTotal: data?.targetEdgeMeta?.total
  });

  // 处理插入按钮点击
  const handleInsertClick = (event: React.MouseEvent) => {
    event.stopPropagation();
    
    // 计算中点位置
    const midX = (sourceX + targetX) / 2;
    const midY = (sourceY + targetY) / 2;
    
    // 触发插入节点事件，并把按钮页面坐标传给下拉框定位
    triggerInsertNode({
      edgeId: id,
      position: { x: midX, y: midY },
      dropdownPosition: { x: event.clientX, y: event.clientY }
    });
  };

  return (
    <>
      {/* 边的路径 */}
      <path
        id={id}
        className="react-flow__edge-path"
        d={edgePath}
        style={{
          ...style,
          stroke: '#cbd5e1',
          strokeWidth: 2
        }}
        markerEnd={markerEnd}
      />
      
      {/* 插入按钮 */}
      {!data?.hideInsertButton && (
        <EdgeLabelRenderer>
          <div
            style={{
              position: 'absolute',
              transform: `translate(-50%, -50%) translate(${labelX}px, ${labelY}px)`,
              pointerEvents: 'all'
            }}
            className="edge-insert-button nodrag nopan"
          >
            <button
              className="insert-btn"
              onClick={handleInsertClick}
              title="插入节点"
              style={{
                width: '28px',
                height: '28px',
                borderRadius: '50%',
                border: '2px solid #6366f1',
                background: '#ffffff',
                color: '#6366f1',
                fontSize: '18px',
                fontWeight: 'bold',
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                transition: 'all 0.2s ease',
                boxShadow: '0 2px 8px rgba(99, 102, 241, 0.3)'
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.background = '#6366f1';
                e.currentTarget.style.color = 'white';
                e.currentTarget.style.transform = 'scale(1.1)';
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.background = '#ffffff';
                e.currentTarget.style.color = '#6366f1';
                e.currentTarget.style.transform = 'scale(1)';
              }}
            >
              +
            </button>
          </div>
        </EdgeLabelRenderer>
      )}
    </>
  );
};

export default memo(InsertEdge);
