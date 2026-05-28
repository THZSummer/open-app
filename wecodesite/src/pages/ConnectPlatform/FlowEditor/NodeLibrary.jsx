/**
 * ========================================
 * 节点库面板组件
 * ========================================
 *
 * 功能：
 * - 显示所有可用的节点类型列表
 * - 支持拖拽节点到画布
 * - 只显示 visible=true 的节点
 */

import React from 'react';
import { Card, Typography, Tooltip } from 'antd';
import { QuestionCircleOutlined } from '@ant-design/icons';
import { NODE_LIBRARY, NODE_COLORS } from './constants';
import './NodeLibrary.m.less';

const { Title, Text } = Typography;

/**
 * 节点库面板组件
 *
 * @param {Object} props
 * @param {Function} props.onDragStart - 开始拖拽回调
 * @param {Array} props.nodes - 当前画布中的节点列表
 */
function NodeLibrary({ onDragStart, nodes = [] }) {
  // 判断画布中是否已存在触发器节点
  const hasTriggerNode = nodes.some(node => node.type === 'trigger');

  /**
   * 处理拖拽开始
   */
  const handleDragStart = (event, nodeType, label) => {
    // 如果是触发器类型且画布中已存在触发器，则不允许添加
    if (nodeType === 'trigger' && hasTriggerNode) {
      event.preventDefault();
      return;
    }

    // 将节点数据存储到拖拽事件中
    const nodeData = JSON.stringify({
      type: nodeType,
      label: label,
    });

    event.dataTransfer.setData('application/reactflow', nodeData);
    event.dataTransfer.effectAllowed = 'move';

    // 调用回调函数
    if (onDragStart) {
      onDragStart(nodeType, label);
    }
  };

  /**
   * 渲染节点卡片
   */
  const renderNodeCard = (item, index) => {
    // 触发器节点特殊处理：如果已存在触发器则禁用
    const isTriggerDisabled = item.type === 'trigger' && hasTriggerNode;

    return (
      <Card
        key={`${item.type}-${index}`}
        size="small"
        draggable={!isTriggerDisabled}
        onDragStart={(e) => handleDragStart(e, item.type, item.label)}
        className="node-card"
        style={{
          cursor: isTriggerDisabled ? 'not-allowed' : 'grab',
          opacity: isTriggerDisabled ? 0.5 : 1,
          backgroundColor: isTriggerDisabled ? '#f5f5f5' : '#fff',
        }}
        bodyStyle={{ padding: '10px 12px' }}
        hoverable={!isTriggerDisabled}
      >
        <div className="node-content">
          <div
            className="node-icon"
            style={{ backgroundColor: NODE_COLORS[item.type] || '#999' }}
          >
            {item.label.charAt(0)}
          </div>
          <div className="node-info">
            <div style={{ color: isTriggerDisabled ? '#999' : '#333' }}>
              {item.label}
              {isTriggerDisabled && (
                <span className="node-label-added">(已添加)</span>
              )}
            </div>
            {item.description && (
              <Tooltip title={isTriggerDisabled ? '每个流程只能添加一个触发器' : item.description}>
                <QuestionCircleOutlined className="node-help-icon" />
              </Tooltip>
            )}
          </div>
        </div>
      </Card>
    );
  };

  /**
   * 过滤出 visible=true 的节点
   */
  const visibleNodes = NODE_LIBRARY.filter(item => item.visible !== false);

  return (
    <div className="node-library-panel">
      {/* 面板标题 */}
      <div className="panel-header">
        <Title level={5} className="panel-title">
          节点库
        </Title>
        <Text type="secondary" className="panel-desc">
          拖拽节点到画布进行编排
        </Text>
      </div>

      {/* 节点列表 */}
      <div className="node-list">
        {visibleNodes.map((item, index) =>
          renderNodeCard(item, index)
        )}
      </div>
    </div>
  );
}

export default NodeLibrary;
