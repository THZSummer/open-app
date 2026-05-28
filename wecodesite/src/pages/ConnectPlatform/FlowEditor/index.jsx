/**
 * ========================================
 * 连接流编辑器页面
 * ========================================
 *
 * 功能：
 * - 创建新的连接流
 * - 编辑已有的连接流
 * - 全屏显示流程编辑画布
 * - 支持节点拖拽添加
 * - 支持节点连线
 * - 支持节点属性配置
 */

import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button, message, Modal, Space, Drawer } from 'antd';
import { SaveOutlined, CloseOutlined } from '@ant-design/icons';
import { ReactFlow, addEdge, useNodesState, useEdgesState } from '@xyflow/react';
import FlowCanvasWrapper from '../../../components/FlowCanvas/FlowCanvasWrapper';
import NodeLibrary from './NodeLibrary';
import NodeProperties from './NodeProperties';
import { nodeTypes } from './constants';
import { generateNodeId, generateEdgeId, getInitialNodePosition, transformFromBackend, transformToBackend } from '../../../utils/flowUtils';
import { fetchFlowDetail, saveFlowConfig } from './thunk';
import { queryParams } from '../../../utils/common';
import SimpleSidebar from '../../../components/SimpleSidebar/SimpleSidebar';
import './FlowEditor.m.less';

/**
 * 连接流编辑器页面组件
 */
function FlowEditor() {
  const navigate = useNavigate();

  /**
   * State定义
   */

  // 获取 URL 参数
  const flowId = queryParams('id');
  const flowName = queryParams('name') || '未命名流程';

  // 节点列表
  const [nodes, setNodes, onNodesChange] = useNodesState([]);

  // 连线列表
  const [edges, setEdges, onEdgesChange] = useEdgesState([]);

  // 选中的节点
  const [selectedNode, setSelectedNode] = useState(null);

  // 加载状态
  const [loading, setLoading] = useState(false);

  /**
   * 副作用
   */
  useEffect(() => {
    if (flowId) {
      loadFlowDetail(flowId);
    } else {
      // 没有flowId时，返回连接流列表页
      message.error('缺少连接流ID参数');
      navigate('/connect/flows');
    }
  }, [flowId]);

  /**
 * 加载流程详情
 * @param {string} id - 流程ID
 */
const loadFlowDetail = async (id) => {
  const result = await fetchFlowDetail(id);

  if (result && result.code === '200') {
    const flowData = result.data;

    let configObj;
    try {
      configObj = JSON.parse(flowData.orchestrationConfig);
    } catch (parseError) {
      message.error('加载流程配置失败：配置数据格式错误');
      return;
    }

    const transformed = transformFromBackend(configObj);
    setNodes(transformed.nodes);
    setEdges(transformed.edges);
  } else {
    message.error(result?.messageZh || '加载流程详情失败');
  }
};

  /**
   * 处理节点间连接创建
   */
  const onConnect = (params) => {
    const newEdge = {
      ...params,
      id: generateEdgeId(),
      type: 'smoothstep',
      animated: true,
      style: { stroke: '#1890ff', strokeWidth: 2 },
    };

    setEdges((eds) => addEdge(newEdge, eds));
  };

  /**
   * 处理节点放置（由FlowCanvasWrapper调用，已处理坐标转换）
   * @param {Event} event - 原始事件
   * @param {Object} data - 包含type, label, position的对象
   */
  const handleNodeDrop = (event, { type, label, position }) => {
    // 对位置进行网格对齐
    const alignedPosition = getInitialNodePosition(position.x, position.y);

    // 创建新节点
    const newNode = {
      id: generateNodeId(type),
      type,
      position: alignedPosition,
      data: {
        label,
        config: {},
      },
    };

    // 添加到节点列表
    setNodes((nds) => nds.concat(newNode));
  };

  /**
   * 处理节点点击
   */
  const handleNodeClick = (event, node) => {
    setSelectedNode(node);
  };

  /**
   * 更新节点
   */
  const handleUpdateNode = (updatedNode) => {
    // 处理删除节点
    if (updatedNode._delete) {
      const nodeIdToDelete = updatedNode.id;
      const nodeToDelete = nodes.find((node) => node.id === nodeIdToDelete);

      // 如果是触发器节点，不允许删除
      if (nodeToDelete && nodeToDelete.type === 'trigger') {
        return;
      }

      setNodes((nds) => nds.filter((node) => node.id !== nodeIdToDelete));
      setSelectedNode(null);
      return;
    }

    setNodes((nds) =>
      nds.map((node) =>
        node.id === updatedNode.id ? updatedNode : node
      )
    );
    setSelectedNode(updatedNode);
  };

  /**
   * 判断节点是否可以删除
   * 触发器节点不可删除
   */
  const canDeleteNode = (node) => {
    return node.type !== 'trigger';
  };

  /**
   * 保存流程
   */
  const handleSave = async () => {
    setLoading(true);

    const transformedData = transformToBackend(nodes, edges);
    
    const payload = {
      orchestrationConfig: JSON.stringify(transformedData),
    };

    const result = await saveFlowConfig(flowId, payload);

    if (result && result.code === '200') {
      message.success('保存成功');
      handleClose();
    } else {
      message.error(result?.messageZh || '保存失败');
    }

    setLoading(false);
  };

  /**
   * 关闭画布
   */
  const handleClose = () => {
    navigate('/connect/flows');
  };

  /**
   * 渲染
   */
  return (
    <div className="page-container">
      {/* 左侧导航栏 */}
      <SimpleSidebar />

      {/* 主内容区 */}
      <div className="main-content">
        <div className="flow-editor-page">
          {/* 顶部工具栏 */}
          <div className="editor-header">
            <div className="header-left">
              <Button
                type="text"
                icon={<CloseOutlined />}
                onClick={handleClose}
                className="back-btn"
              >
                返回
              </Button>
              <span className="flow-name">{flowName}</span>
            </div>
            <div className="header-right">
              <Space>
                <span className="node-count">
                  {nodes.length} 个节点 · {edges.length} 条连线
                </span>
                <Button
                  type="primary"
                  icon={<SaveOutlined />}
                  onClick={handleSave}
                  loading={loading}
                >
                  保存
                </Button>
              </Space>
            </div>
          </div>

          {/* 画布主体 */}
          <div className="editor-content">
            {/* 左侧节点库 */}
            <NodeLibrary nodes={nodes} />

            {/* 中央画布 */}
            <div className="editor-canvas-area">
              <FlowCanvasWrapper
                nodes={nodes}
                edges={edges}
                onNodesChange={onNodesChange}
                onEdgesChange={onEdgesChange}
                onConnect={onConnect}
                onNodeClick={handleNodeClick}
                onNodeDrop={handleNodeDrop}
                selectedNodeId={selectedNode?.id}
                canDeleteNode={canDeleteNode}
                nodeTypes={nodeTypes}
              />
            </div>
          </div>

          {/* 节点配置抽屉 */}
          <Drawer
            title="节点配置"
            placement="right"
            open={!!selectedNode}
            onClose={() => setSelectedNode(null)}
            width={600}
            destroyOnClose
            mask={false}
          >
            {selectedNode && (
              <NodeProperties
                selectedNode={selectedNode}
                onUpdateNode={handleUpdateNode}
                nodes={nodes}
                edges={edges}
              />
            )}
          </Drawer>
        </div>
      </div>
    </div>
  );
}

export default FlowEditor;
