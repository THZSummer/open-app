/**
 * ========================================
 * 流程画布主组件（增强版）
 * ========================================
 * 
 * 功能：
 * - 全屏显示流程编辑画布
 * - 支持节点拖拽添加
 * - 支持节点连线
 * - 支持节点属性配置
 * - 支持流程验证
 */

import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Button, message, Modal, Input, Space, Tag, Tooltip } from 'antd';
import { SaveOutlined, CloseOutlined, CheckCircleOutlined, QuestionCircleOutlined } from '@ant-design/icons';
import { ReactFlow, addEdge, useNodesState, useEdgesState } from '@xyflow/react';
import FlowCanvasWrapper from '../../../components/FlowCanvas/FlowCanvasWrapper';
import NodeLibrary from './NodeLibrary';
import NodeProperties from './NodeProperties';
import { nodeTypes } from './customNodes';
import { generateNodeId, generateEdgeId, getInitialNodePosition, validateFlowConfig } from '../../../utils/flowUtils';
import { createFlow, updateFlow, fetchFlowDetail } from './thunk';
import './FlowCanvas.m.less';

/**
 * 流程画布主组件（增强版）
 * 
 * @param {boolean} props.isNew - 是否为新建模式
 */
function FlowCanvas({ isNew = false }) {
  const navigate = useNavigate();
  const { id } = useParams();

  /**
   * State定义
   */
  
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
  const [flowType, setFlowType] = useState('business');

  /**
   * 副作用
   */
  useEffect(() => {
    if (!isNew && id) {
      loadFlowDetail(id);
    }
  }, [id, isNew]);

  /**
   * 加载流程详情（编辑模式）
   */
  const loadFlowDetail = async (flowId) => {
    const result = await fetchFlowDetail(flowId);
    
    if (result && result.code === '200') {
      const flowData = result.data;
      
      setFlowName(flowData.name);
      setFlowDescription(flowData.description || '');
      setFlowType(flowData.type || 'business');
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
   * 处理拖拽悬停
   */
  const onDragOver = (event) => {
    event.preventDefault();
    event.dataTransfer.dropEffect = 'move';
  };

  /**
   * 处理节点放置
   */
  const onDrop = (event) => {
    event.preventDefault();
    
    // 获取拖拽的节点数据
    const data = event.dataTransfer.getData('application/reactflow');
    if (!data) return;

    const { type, label } = JSON.parse(data);
    
    // 计算放置位置（需要减去侧边栏宽度）
    const reactFlowBounds = event.currentTarget.getBoundingClientRect();
    const position = getInitialNodePosition(
      event.clientX - reactFlowBounds.left,
      event.clientY - reactFlowBounds.top
    );

    // 创建新节点
    const newNode = {
      id: generateNodeId(type),
      type,
      position,
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
    setNodes((nds) =>
      nds.map((node) =>
        node.id === updatedNode.id ? updatedNode : node
      )
    );
    setSelectedNode(updatedNode);
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
      type: flowType,
      status: 0, // 草稿状态
      nodes,
      edges,
    };

    // 调用API
    let result;
    if (isNew) {
      result = await createFlow(payload);
    } else {
      result = await updateFlow(id, payload);
    }

    if (result && result.code === '200') {
      message.success(isNew ? '创建成功' : '保存成功');
      navigate('/admin/flows');
    } else {
      message.error(result?.message || '操作失败');
    }
    
    setLoading(false);
  };

  /**
   * 关闭画布
   */
  const handleClose = () => {
    Modal.confirm({
      title: '确认离开',
      content: '确定要离开吗？未保存的更改将丢失',
      onOk: () => navigate('/admin/flows'),
    });
  };

  /**
   * 键盘快捷键提示
   */
  const renderKeyboardShortcuts = () => (
    <Tooltip title={
      <div>
        <p style={{ fontWeight: 'bold', marginBottom: 8 }}>快捷键</p>
        <p style={{ margin: '4px 0' }}><Tag size="small">Delete</Tag> 删除选中节点/连线</p>
        <p style={{ margin: '4px 0' }}><Tag size="small">Ctrl/Cmd + Z</Tag> 撤销</p>
        <p style={{ margin: '4px 0' }}><Tag size="small">Space + 拖拽</Tag> 平移画布</p>
        <p style={{ margin: '4px 0' }}><Tag size="small">Ctrl + 滚轮</Tag> 缩放画布</p>
      </div>
    }>
      <Button 
        type="text" 
        icon={<QuestionCircleOutlined />}
      >
        帮助
      </Button>
    </Tooltip>
  );

  /**
   * 渲染
   */
  return (
    <div className="flow-canvas-page">
      {/* 顶部工具栏 */}
      <div className="canvas-header">
        <div className="header-left">
          <Button 
            type="text" 
            icon={<CloseOutlined />}
            onClick={handleClose}
            style={{ marginRight: 16 }}
          >
            返回
          </Button>
          <Input
            value={flowName}
            onChange={(e) => setFlowName(e.target.value)}
            placeholder="请输入流程名称"
            style={{ width: 300 }}
          />
          <Tag color={flowType === 'business' ? 'blue' : flowType === 'schedule' ? 'green' : 'purple'} style={{ marginLeft: 8 }}>
            {flowType === 'business' ? '业务流' : flowType === 'schedule' ? '定时流' : '子流程'}
          </Tag>
        </div>
        <div className="header-right">
          <Space>
            <span style={{ color: '#999', fontSize: 12 }}>
              {nodes.length} 个节点 · {edges.length} 条连线
            </span>
            {renderKeyboardShortcuts()}
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
          </Space>
        </div>
      </div>

      {/* 画布主体 */}
      <div className="canvas-content">
        {/* 左侧节点库 */}
        <NodeLibrary />
        
        {/* 中央画布 */}
        <div 
          className="flow-canvas-area" 
          onDrop={onDrop} 
          onDragOver={onDragOver}
        >
          <FlowCanvasWrapper
            nodes={nodes}
            edges={edges}
            onNodesChange={onNodesChange}
            onEdgesChange={onEdgesChange}
            onConnect={onConnect}
            onNodeClick={handleNodeClick}
            selectedNodeId={selectedNode?.id}
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

export default FlowCanvas;
