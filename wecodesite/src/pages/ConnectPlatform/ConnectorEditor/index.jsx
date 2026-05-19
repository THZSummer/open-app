/**
 * ========================================
 * 连接器管理 - 编辑页面
 * ========================================
 *
 * 功能：
 * - 创建新连接器
 * - 编辑已有连接器
 */

import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import {
  Form,
  Input,
  Switch,
  Tabs,
  Button,
  Space,
  message,
  Select,
  Divider,
  Tag,
} from 'antd';
import DeleteConfirmModal from '../../../components/DeleteConfirmModal/DeleteConfirmModal';
import {
  ArrowLeftOutlined,
  PlusOutlined,
  DeleteOutlined,
  EditOutlined,
  SaveOutlined,
} from '@ant-design/icons';
import { createConnector, updateConnector, fetchConnectorDetail } from './thunk';
import SimpleSidebar from '../../../components/SimpleSidebar/SimpleSidebar';
import './ConnectorEditor.less';
import { getSecondModalInfo } from '../../../utils/common';

const { TextArea } = Input;
const { Option } = Select;

/**
 * 连接器编辑页面组件
 */
const ConnectorEditor = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const searchParams = new URLSearchParams(location.search);
  const connectorId = searchParams.get('id');
  const isEdit = !!connectorId;

  /**
   * State定义
   */
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [detailLoading, setDetailLoading] = useState(isEdit);

  // 是否可编辑状态
  const [editable, setEditable] = useState(!isEdit);

  // 触发事件列表
  const [triggers, setTriggers] = useState([]);

  // 执行动作列表
  const [actions, setActions] = useState([]);

  // 当前编辑的触发事件
  const [editingTrigger, setEditingTrigger] = useState(null);

  // 当前编辑的执行动作
  const [editingAction, setEditingAction] = useState(null);

  // 删除确认弹窗
  const [deleteModalVisible, setDeleteModalVisible] = useState(false);
  const [deleteType, setDeleteType] = useState(null);
  const [deleteId, setDeleteId] = useState(null);

  /**
   * 副作用 - 加载连接器详情
   */
  useEffect(() => {
    if (isEdit && connectorId) {
      loadDetail(connectorId);
    }
  }, [connectorId]);

  /**
   * 加载连接器详情
   * @param {string} id - 连接器ID
   */
  const loadDetail = async (id) => {
    setDetailLoading(true);
    const result = await fetchConnectorDetail(id);

    if (result && result.code === '200') {
      const connectorData = result.data;

      // 设置表单数据
      form.setFieldsValue({
        name: connectorData.name,
        description: connectorData.description,
        status: connectorData.status === 1,
      });

      // 设置触发事件和执行动作列表
      setTriggers(connectorData.triggers || []);
      setActions(connectorData.actions || []);
    } else {
      message.error(result?.messageZh || result?.message || '加载连接器详情失败');
    }

    setDetailLoading(false);
  };

  /**
   * 返回列表页
   */
  const handleBack = () => {
    navigate('/connect/connectors');
  };

  /**
   * 点击编辑按钮
   */
  const handleEdit = () => {
    setEditable(true);
  };

  /**
   * 点击删除按钮
   * @param {string} type - 删除类型（trigger/action）
   * @param {string} id - 删除项ID
   */
  const handleDeleteClick = (type, id) => {
    setDeleteType(type);
    setDeleteId(id);
    setDeleteModalVisible(true);
  };

  /**
   * 确认删除
   */
  const handleDeleteConfirm = () => {
    if (deleteId) {
      if (deleteType === 'trigger') {
        setTriggers(triggers.filter(t => t.id !== deleteId));
      } else if (deleteType === 'action') {
        setActions(actions.filter(a => a.id !== deleteId));
      }
      message.success('删除成功');
    }
    setDeleteModalVisible(false);
    setDeleteType(null);
    setDeleteId(null);
  };

  /**
   * 取消删除
   */
  const handleDeleteCancel = () => {
    setDeleteModalVisible(false);
    setDeleteType(null);
    setDeleteId(null);
  };

  /**
   * 提交表单
   */
  const handleSubmit = async () => {
    if (!editable) {
      return;
    }

    // 表单验证
    const values = await form.validateFields();
    setLoading(true);

    // 构建提交数据
    const payload = {
      name: values.name,
      description: values.description || '',
      status: values.status ? 1 : 0,
      triggers,
      actions,
    };

    // 调用API
    const api = isEdit ? updateConnector : createConnector;
    const apiParams = isEdit ? [connectorId, payload] : [payload];
    const result = await api(...apiParams);

    if (result && result.code === '200') {
      message.success(isEdit ? '更新成功' : '创建成功');
      if (isEdit) {
        setEditable(false);
      } else {
        navigate('/connect/connectors');
      }
    } else {
      message.error(result?.messageZh || result?.message || '操作失败');
    }

    setLoading(false);
  };

  /**
   * 触发事件管理 - 新增
   */
  const handleAddTrigger = () => {
    setEditingTrigger({
      id: null,
      name: '',
      description: '',
      type: 'webhook',
      config: {},
    });
  };

  /**
   * 触发事件管理 - 编辑
   */
  const handleEditTrigger = (trigger) => {
    setEditingTrigger({ ...trigger });
  };

  /**
   * 触发事件管理 - 保存
   */
  const handleSaveTrigger = () => {
    if (!editingTrigger.name || !editingTrigger.name.trim()) {
      message.error('请输入触发事件名称');
      return;
    }

    if (editingTrigger.id) {
      setTriggers(triggers.map(t =>
        t.id === editingTrigger.id ? editingTrigger : t
      ));
    } else {
      setTriggers([...triggers, {
        ...editingTrigger,
        id: `trigger_${Date.now()}`
      }]);
    }

    setEditingTrigger(null);
    message.success('保存成功');
  };

  /**
   * 执行动作管理 - 新增
   */
  const handleAddAction = () => {
    setEditingAction({
      id: null,
      name: '',
      description: '',
      method: 'GET',
      endpoint: '',
      requestSchema: {},
      responseSchema: {},
    });
  };

  /**
   * 执行动作管理 - 编辑
   */
  const handleEditAction = (action) => {
    setEditingAction({ ...action });
  };

  /**
   * 执行动作管理 - 保存
   */
  const handleSaveAction = () => {
    if (!editingAction.name || !editingAction.name.trim()) {
      message.error('请输入执行动作名称');
      return;
    }

    if (editingAction.id) {
      setActions(actions.map(a =>
        a.id === editingAction.id ? editingAction : a
      ));
    } else {
      setActions([...actions, {
        ...editingAction,
        id: `action_${Date.now()}`
      }]);
    }

    setEditingAction(null);
    message.success('保存成功');
  };

  /**
   * 渲染Tab内容
   */
  const renderTabItems = () => [
    {
      key: 'basic',
      label: '基本信息',
      children: renderBasicInfo(),
    },
    {
      key: 'triggers',
      label: `触发事件 (${triggers.length})`,
      children: renderTriggerForm(),
    },
    {
      key: 'actions',
      label: `执行动作 (${actions.length})`,
      children: renderActionForm(),
    },
  ];

  /**
   * 渲染基本信息表单
   */
  const renderBasicInfo = () => (
    <Form
      form={form}
      layout="vertical"
      className="basic-info-form"
      disabled={!editable}
    >
      <Form.Item
        name="name"
        label="连接器名称"
        rules={[
          { required: true, message: '请输入连接器名称' },
          { max: 50, message: '名称不能超过50个字符' }
        ]}
      >
        <Input
          placeholder="请输入连接器名称"
          maxLength={50}
          showCount
        />
      </Form.Item>

      <Form.Item
        name="description"
        label="连接器描述"
        rules={[
          { max: 500, message: '描述不能超过500个字符' }
        ]}
      >
        <TextArea
          placeholder="请输入连接器描述"
          maxLength={500}
          showCount
          rows={4}
        />
      </Form.Item>

      <Form.Item
        name="status"
        label="状态"
        valuePropName="checked"
        tooltip="启用后，该连接器可以在流程中被使用"
      >
        <Switch
          checkedChildren="启用"
          unCheckedChildren="禁用"
        />
      </Form.Item>
    </Form>
  );

  /**
   * 渲染触发事件表单
   */
  const renderTriggerForm = () => (
    <div className="trigger-form">
      {triggers.map(trigger => (
        <div key={trigger.id} className="trigger-item">
          <div className="trigger-item-content">
            <div className="trigger-item-info">
              <span className="trigger-name">{trigger.name}</span>
              <span className="trigger-type">
                {trigger.type === 'webhook' ? 'Webhook' :
                  trigger.type === 'api' ? 'API轮询' : '定时触发'}
              </span>
            </div>
            {trigger.description && (
              <div className="trigger-desc">{trigger.description}</div>
            )}
          </div>
          {editable && (
            <Space>
              <Button
                type="link"
                size="small"
                icon={<EditOutlined />}
                onClick={() => handleEditTrigger(trigger)}
              >
                编辑
              </Button>
              <Button
                type="link"
                size="small"
                danger
                icon={<DeleteOutlined />}
                onClick={() => handleDeleteClick('trigger', trigger.id)}
              >
                删除
              </Button>
            </Space>
          )}
        </div>
      ))}

      {editable && (
        <Button
          type="dashed"
          icon={<PlusOutlined />}
          onClick={handleAddTrigger}
          block
          style={{ marginTop: 16 }}
        >
          添加触发事件
        </Button>
      )}

      {editingTrigger && (
        <div className="trigger-edit-form">
          <Divider>编辑触发事件</Divider>
          <Form layout="vertical">
            <Form.Item label="事件名称" required>
              <Input
                value={editingTrigger.name}
                onChange={e => setEditingTrigger({
                  ...editingTrigger,
                  name: e.target.value
                })}
                placeholder="例如：用户创建事件"
              />
            </Form.Item>

            <Form.Item label="事件描述">
              <Input
                value={editingTrigger.description}
                onChange={e => setEditingTrigger({
                  ...editingTrigger,
                  description: e.target.value
                })}
                placeholder="请输入事件描述"
              />
            </Form.Item>

            <Form.Item label="触发类型">
              <Select
                value={editingTrigger.type}
                onChange={val => setEditingTrigger({
                  ...editingTrigger,
                  type: val
                })}
              >
                <Option value="webhook">Webhook</Option>
                <Option value="api">API轮询</Option>
                <Option value="schedule">定时触发</Option>
              </Select>
            </Form.Item>
          </Form>

          <Space>
            <Button
              type="primary"
              icon={<SaveOutlined />}
              onClick={handleSaveTrigger}
            >
              保存
            </Button>
            <Button onClick={() => setEditingTrigger(null)}>取消</Button>
          </Space>
        </div>
      )}
    </div>
  );

  /**
   * 渲染执行动作表单
   */
  const renderActionForm = () => (
    <div className="action-form">
      {actions.map(action => (
        <div key={action.id} className="action-item">
          <div className="action-item-content">
            <div className="action-item-info">
              <span className="action-name">{action.name}</span>
              <span className="action-method">
                <Tag color={
                  action.method === 'GET' ? 'green' :
                    action.method === 'POST' ? 'blue' :
                      action.method === 'PUT' ? 'orange' : 'red'
                }>
                  {action.method}
                </Tag>
                <span className="action-endpoint">{action.endpoint}</span>
              </span>
            </div>
            {action.description && (
              <div className="action-desc">{action.description}</div>
            )}
          </div>
          {editable && (
            <Space>
              <Button
                type="link"
                size="small"
                icon={<EditOutlined />}
                onClick={() => handleEditAction(action)}
              >
                编辑
              </Button>
              <Button
                type="link"
                size="small"
                danger
                icon={<DeleteOutlined />}
                onClick={() => handleDeleteClick('action', action.id)}
              >
                删除
              </Button>
            </Space>
          )}
        </div>
      ))}

      {editable && (
        <Button
          type="dashed"
          icon={<PlusOutlined />}
          onClick={handleAddAction}
          block
          style={{ marginTop: 16 }}
        >
          添加执行动作
        </Button>
      )}

      {editingAction && (
        <div className="action-edit-form">
          <Divider>编辑执行动作</Divider>
          <Form layout="vertical">
            <Form.Item label="动作名称" required>
              <Input
                value={editingAction.name}
                onChange={e => setEditingAction({
                  ...editingAction,
                  name: e.target.value
                })}
                placeholder="例如：发送消息"
              />
            </Form.Item>

            <Form.Item label="动作描述">
              <Input
                value={editingAction.description}
                onChange={e => setEditingAction({
                  ...editingAction,
                  description: e.target.value
                })}
                placeholder="请输入动作描述"
              />
            </Form.Item>

            <Form.Item label="请求方法">
              <Select
                value={editingAction.method}
                onChange={val => setEditingAction({
                  ...editingAction,
                  method: val
                })}
              >
                <Option value="GET">GET</Option>
                <Option value="POST">POST</Option>
                <Option value="PUT">PUT</Option>
                <Option value="DELETE">DELETE</Option>
              </Select>
            </Form.Item>

            <Form.Item label="API端点">
              <Input
                value={editingAction.endpoint}
                onChange={e => setEditingAction({
                  ...editingAction,
                  endpoint: e.target.value
                })}
                placeholder="/api/v1/xxx"
              />
            </Form.Item>
          </Form>

          <Space>
            <Button
              type="primary"
              icon={<SaveOutlined />}
              onClick={handleSaveAction}
            >
              保存
            </Button>
            <Button onClick={() => setEditingAction(null)}>取消</Button>
          </Space>
        </div>
      )}
    </div>
  );

  /**
   * 渲染
   */
  return (
    <div style={{ display: 'flex', height: '100vh', overflow: 'hidden' }}>
      {/* 左侧导航栏 */}
      <SimpleSidebar />

      {/* 主内容区 */}
      <div style={{ flex: 1, overflow: 'auto' }}>
        <div
          className="connector-editor-page"
          style={{
            opacity: detailLoading ? 0.6 : 1,
            pointerEvents: detailLoading ? 'none' : 'auto'
          }}
        >
          {/* 页面头部 */}
          <div className="page-header">
            <div className="page-header-left">
              <Button
                className="back-button"
                icon={<ArrowLeftOutlined />}
                onClick={handleBack}
              >
                返回
              </Button>
              <div className="page-header-title">
                <h4 className="page-title">{isEdit ? '连接器详情' : '新建连接器'}</h4>
                <span className="page-desc">{isEdit ? '查看连接器的基本信息、触发事件和执行动作' : '创建新的连接器，配置触发事件和执行动作'}</span>
              </div>
            </div>
            <Space>
              {isEdit && !editable && (
                <Button
                  type="primary"
                  icon={<EditOutlined />}
                  onClick={handleEdit}
                  style={{ borderRadius: 6 }}
                >
                  编辑
                </Button>
              )}
              {editable && (
                <Button
                  type="primary"
                  icon={<SaveOutlined />}
                  onClick={handleSubmit}
                  loading={loading}
                  style={{ borderRadius: 6 }}
                >
                  保存
                </Button>
              )}
            </Space>
          </div>

          {/* Tabs 内容区域 */}
          <Tabs
            items={renderTabItems()}
            defaultActiveKey="basic"
          />

          {/* 删除确认弹窗 */}
          <DeleteConfirmModal
            open={deleteModalVisible}
            onClose={handleDeleteCancel}
            onConfirm={handleDeleteConfirm}
            modalInfo={getSecondModalInfo(deleteType === 'trigger' ? '触发事件' : '执行动作'), 'delete', true}
          />
        </div>
      </div>
    </div>
  );
};

export default ConnectorEditor;
