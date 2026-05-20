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
 * - 支持流程验证
 */

import React, { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Button, message, Modal, Input, Space, Tag } from 'antd';
import { SaveOutlined, CloseOutlined, CheckCircleOutlined } from '@ant-design/icons';
import { ReactFlow, addEdge, useNodesState, useEdgesState } from '@xyflow/react';
import FlowCanvasWrapper from '../../../components/FlowCanvas/FlowCanvasWrapper';
import NodeLibrary from './NodeLibrary';
import NodeProperties from './NodeProperties';
import { nodeTypes } from './customNodes';
import { generateNodeId, generateEdgeId, getInitialNodePosition, validateFlowConfig } from '../../../utils/flowUtils';
import { createFlow, updateFlow, fetchFlowDetail } from './thunk';
import './FlowEditor.less';

/**
 * 连接流编辑器页面组件
 */
function FlowEditor() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  /**
   * State定义
   */

  // 获取 URL 参数
  const flowId = searchParams.get('id');
  const action = searchParams.get('action') || 'create';

  // 判断是否为新建模式
  const isNew = action === 'create' || !flowId;

  // 判断是否为查看模式
  const isViewMode = action === 'view';

  // 节点列表
  const [nodes, setNodes, onNodesChange] = useNodesState([]);

  // 连线列表
  const [edges, setEdges, onEdgesChange] = useEdgesState([]);

  // 选中的节点
  const [selectedNode, setSelectedNode] = useState(null);

  // 加载状态
  const [loading, setLoading] = useState(false);

  // 流程基本信息
  const [flowName, setFlowName] = useState('');
  const [flowDescription, setFlowDescription] = useState('');

  /**
   * 副作用
   */
  useEffect(() => {
    if (!isNew && flowId) {
      loadFlowDetail(flowId);
    } else if (isNew) {
      // 新建模式：自动创建一个默认的触发器节点
      initializeTriggerNode();
    }
  }, [flowId, isNew]);

  /**
   * 初始化触发器节点
   * 每个连接流必须且仅有一个触发器
   */
  const initializeTriggerNode = () => {
    const triggerNode = {
      id: generateNodeId('trigger'),
      type: 'trigger',
      position: { x: 250, y: 50 },
      data: {
        label: '触发器',
        config: {
          triggerType: 'schedule',
          cronExpression: '0 0 * * * ?',
          timezone: 'Asia/Shanghai',
        },
      },
    };
    setNodes([triggerNode]);
  };

  /**
   * 加载流程详情（编辑模式）
   */
  const loadFlowDetail = async (id) => {
    const result = await fetchFlowDetail(id);

    if (result && result.code === '200') {
      const flowData = result.data;

      setFlowName(flowData.name);
      setFlowDescription(flowData.description || '');
      setNodes(flowData.nodes || []);
      setEdges(flowData.edges || []);
    } else {
      message.error(result?.message || '加载流程详情失败');
    }
  };

  /**
   * 处理连接创建
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
   * 处理画布点击（取消选中）
   */
  const handlePaneClick = () => {
    setSelectedNode(null);
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
   * 流程验证
   */
  const handleValidate = () => {
    const validation = validateFlowConfig(nodes, edges);

    if (validation.valid) {
      if (validation.warnings.length > 0) {
        Modal.warning({
          title: '验证通过（有警告）',
          content: (
            <div>
              <p style={{ color: '#52c41a', fontWeight: 'bold' }}>✓ 流程配置验证通过</p>
              {validation.warnings.length > 0 && (
                <>
                  <p>但发现以下问题：</p>
                  <ul style={{ color: '#faad14' }}>
                    {validation.warnings.map((warning, index) => (
                      <li key={index}>{warning}</li>
                    ))}
                  </ul>
                </>
              )}
            </div>
          ),
        });
      } else {
        message.success('✓ 流程配置验证通过');
      }
    } else {
      Modal.error({
        title: '验证失败',
        content: (
          <div>
            <p>以下问题需要修复：</p>
            <ul style={{ color: '#ff4d4f' }}>
              {validation.errors.map((error, index) => (
                <li key={index}>{error}</li>
              ))}
            </ul>
          </div>
        ),
      });
    }
  };

  /**
   * 保存流程
   */
  const handleSave = async () => {
    // 验证流程名称
    if (!flowName.trim()) {
      message.error('请输入流程名称');
      return;
    }

    // 验证流程配置
    const validation = validateFlowConfig(nodes, edges);

    if (!validation.valid) {
      Modal.error({
        title: '流程配置错误',
        content: (
          <div>
            <p>以下问题需要修复：</p>
            <ul style={{ color: '#ff4d4f' }}>
              {validation.errors.map((error, index) => (
                <li key={index}>{error}</li>
              ))}
            </ul>
          </div>
        ),
      });
      return;
    }

    // 显示警告（如果有）
    if (validation.warnings.length > 0) {
      const confirmed = await new Promise((resolve) => {
        Modal.confirm({
          title: '流程配置警告',
          content: (
            <div>
              <p>发现以下问题：</p>
              <ul style={{ color: '#faad14' }}>
                {validation.warnings.map((warning, index) => (
                  <li key={index}>{warning}</li>
                ))}
              </ul>
              <p>是否仍要保存？</p>
            </div>
          ),
          onOk: () => resolve(true),
          onCancel: () => resolve(false),
        });
      });

      if (!confirmed) {
        return;
      }
    }

    setLoading(true);

    // 构建保存数据
    const payload = {
      name: flowName,
      description: flowDescription,
      status: 0, // 草稿状态
      nodes,
      edges,
    };

    // 调用API
    let result;
    if (isNew) {
      result = await createFlow(payload);
    } else {
      result = await updateFlow(flowId, payload);
    }

    if (result && result.code === '200') {
      message.success(isNew ? '创建成功' : '保存成功');
      navigate('/connect/flows');
    } else {
      message.error(result?.message || '操作失败');
    }

    setLoading(false);
  };

  /**
   * 关闭画布
   */
  const handleClose = () => {
    if (isViewMode) {
      navigate('/connect/flows');
      return;
    }

    Modal.confirm({
      title: '确认离开',
      content: '确定要离开吗？未保存的更改将丢失',
      onOk: () => navigate('/connect/flows'),
    });
  };

  /**
   * 渲染页面标题
   */
  const renderPageTitle = () => {
    const titles = {
      create: '新建连接流',
      edit: '编辑连接流',
      view: '查看连接流',
    };
    return titles[action] || '连接流编辑器';
  };

  /**
   * 渲染
   */
  return (
    <div className="flow-editor-page">
      {/* 顶部工具栏 */}
      <div className="editor-header">
        <div className="header-left">
          <Button
            type="text"
            icon={<CloseOutlined />}
            onClick={handleClose}
            style={{ marginRight: 16 }}
          >
            返回
          </Button>
          {isViewMode ? (
            <span style={{ fontSize: 16, fontWeight: 500 }}>{flowName}</span>
          ) : (
            <>
              <Input
                value={flowName}
                onChange={(e) => setFlowName(e.target.value)}
                placeholder="请输入流程名称"
                style={{ width: 300 }}
              />
            </>
          )}
        </div>
        <div className="header-right">
          <Space>
            <span style={{ color: '#999', fontSize: 12 }}>
              {nodes.length} 个节点 · {edges.length} 条连线
            </span>
            {isViewMode && (
              <Button
                icon={<CheckCircleOutlined />}
                onClick={handleValidate}
              >
                验证
              </Button>
            )}
            {!isViewMode && (
              <>
                <Button
                  icon={<CheckCircleOutlined />}
                  onClick={handleValidate}
                >
                  验证
                </Button>
                <Button
                  type="primary"
                  icon={<SaveOutlined />}
                  onClick={handleSave}
                  loading={loading}
                >
                  保存
                </Button>
              </>
            )}
          </Space>
        </div>
      </div>

      {/* 画布主体 */}
      <div className="editor-content">
        {/* 左侧节点库 */}
        <NodeLibrary />

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
            disableNodeInteraction={isViewMode}
            canDeleteNode={canDeleteNode}
            nodeTypes={nodeTypes}
          />
        </div>

        {/* 右侧属性面板 */}
        <NodeProperties
          selectedNode={selectedNode}
          onUpdateNode={handleUpdateNode}
        />
      </div>
    </div>
  );
}

export default FlowEditor;
