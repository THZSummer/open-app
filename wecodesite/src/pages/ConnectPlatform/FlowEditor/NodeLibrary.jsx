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
import { NODE_LIBRARY } from './customNodes';

const { Title, Text } = Typography;

/**
 * 节点库面板组件
 *
 * @param {Object} props
 * @param {Function} props.onDragStart - 开始拖拽回调
 */
function NodeLibrary({ onDragStart }) {

  /**
   * 处理拖拽开始
   */
  const handleDragStart = (event, nodeType, label) => {
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
  const renderNodeCard = (item, index) => (
    <Card
      key={`${item.type}-${index}`}
      size="small"
      draggable
      onDragStart={(e) => handleDragStart(e, item.type, item.label)}
      style={{
        marginBottom: 8,
        cursor: 'grab',
        userSelect: 'none',
        transition: 'all 0.2s ease',
      }}
      bodyStyle={{ padding: '10px 12px' }}
      hoverable
    >
      <div style={{
        display: 'flex',
        alignItems: 'center',
        gap: 8,
      }}>
        <div style={{
          width: 28,
          height: 28,
          borderRadius: 6,
          backgroundColor: getNodeColor(item.type),
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          color: '#fff',
          fontSize: 12,
          fontWeight: 600,
        }}>
          {item.label.charAt(0)}
        </div>
        <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <div style={{
            fontSize: 13,
            fontWeight: 500,
            color: '#333',
          }}>
            {item.label}
          </div>
          {item.description && (
            <Tooltip title={item.description}>
              <QuestionCircleOutlined style={{ fontSize: 12, color: '#999', cursor: 'help' }} />
            </Tooltip>
          )}
        </div>
      </div>
    </Card>
  );

  /**
   * 获取节点类型对应的颜色
   */
  const getNodeColor = (type) => {
    const colors = {
      trigger: '#1890ff',
      action: '#52c41a',
      condition: '#faad14',
      delay: '#722ed1',
      parallel: '#13c2c2',
      loop: '#eb2f96',
      dataTransform: '#1890ff',
      dataOutput: '#fa8c16',
    };
    return colors[type] || '#999';
  };

  /**
   * 过滤出 visible=true 的节点
   */
  const visibleNodes = NODE_LIBRARY.filter(item => item.visible !== false);

  return (
    <div
      className="node-library-panel"
      style={{
        width: 200,
        height: '100%',
        backgroundColor: '#fafafa',
        borderRight: '1px solid #e8e8e8',
        overflow: 'auto',
        display: 'flex',
        flexDirection: 'column',
      }}
    >
      {/* 面板标题 */}
      <div style={{
        padding: '16px 16px 12px',
        borderBottom: '1px solid #e8e8e8',
        backgroundColor: '#fff',
      }}>
        <Title level={5} style={{ margin: 0 }}>
          节点库
        </Title>
        <Text type="secondary" style={{ fontSize: 12 }}>
          拖拽节点到画布进行编排
        </Text>
      </div>

      {/* 节点列表 */}
      <div style={{ flex: 1, overflow: 'auto', padding: '12px' }}>
        {visibleNodes.map((item, index) =>
          renderNodeCard(item, index)
        )}
      </div>
    </div>
  );
}

export default NodeLibrary;
