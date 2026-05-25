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
  Divider,
  InputNumber,
  Radio,
  Card,
} from 'antd';
import {
  ArrowLeftOutlined,
  EditOutlined,
  SaveOutlined,
} from '@ant-design/icons';
import { createConnector, updateConnector, fetchConnectorDetail } from './thunk';
import SchemaEditor from '../../../components/SchemaEditor/SchemaEditor.jsx';
import SimpleSidebar from '../../../components/SimpleSidebar/SimpleSidebar';
import './ConnectorEditor.less';

const { TextArea } = Input;

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
        apiConfig: connectorData.apiConfig || {
          protocolType: 'GET',
          protocolAddress: '',
          authType: 'SOA',
          requestSchema: [],
          responseSchema: [],
          timeout: 30000,
          rateLimit: 100,
        },
      });
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
      apiConfig: values.apiConfig,
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
   * 渲染接口配置表单
   */
  const renderApiConfig = () => {
    const apiConfig = form.getFieldValue('apiConfig') || {
      protocolType: 'GET',
      protocolAddress: '',
      authType: 'SOA',
      requestSchema: [],
      responseSchema: [],
      timeout: 30000,
      rateLimit: 100,
    };

    const handleConfigChange = (field, value) => {
      const newConfig = { ...apiConfig, [field]: value };
      form.setFieldValue('apiConfig', newConfig);
    };

    return (
      <div className="api-config-form">
        <Card
          title="接口配置"
          size="small"
          style={{
            marginBottom: 16,
            borderRadius: 8,
            boxShadow: '0 1px 2px rgba(0, 0, 0, 0.05)',
          }}
        >
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>
            <Form.Item
              label="协议类型"
              style={{ marginBottom: 12 }}
            >
              <Radio.Group
                value={apiConfig.protocolType}
                onChange={(e) => handleConfigChange('protocolType', e.target.value)}
                disabled={!editable}
              >
                <Radio.Button value="GET">GET</Radio.Button>
                <Radio.Button value="POST">POST</Radio.Button>
                <Radio.Button value="PUT">PUT</Radio.Button>
                <Radio.Button value="DELETE">DELETE</Radio.Button>
                <Radio.Button value="PATCH">PATCH</Radio.Button>
              </Radio.Group>
            </Form.Item>

            <Form.Item
              label="认证方式"
              style={{ marginBottom: 12 }}
            >
              <Radio.Group
                value={apiConfig.authType}
                onChange={(e) => handleConfigChange('authType', e.target.value)}
                disabled={!editable}
              >
                <Radio value="SOA">SOA</Radio>
                <Radio value="APIG">APIG</Radio>
              </Radio.Group>
            </Form.Item>
          </div>

          <Form.Item label="协议地址" style={{ marginBottom: 0 }}>
            <Input
              value={apiConfig.protocolAddress}
              onChange={(e) => handleConfigChange('protocolAddress', e.target.value)}
              placeholder="https://api.example.com/endpoint"
              disabled={!editable}
              style={{ borderRadius: 6 }}
            />
          </Form.Item>
        </Card>

        <Card
          title="性能配置"
          size="small"
          style={{
            marginBottom: 16,
            borderRadius: 8,
            boxShadow: '0 1px 2px rgba(0, 0, 0, 0.05)',
          }}
        >
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>
            <Form.Item label="超时限制（ms）" style={{ marginBottom: 0 }}>
              <InputNumber
                value={apiConfig.timeout || 30000}
                onChange={(val) => handleConfigChange('timeout', val)}
                min={1000}
                max={300000}
                style={{ width: '100%', borderRadius: 6 }}
                disabled={!editable}
                formatter={(value) => `${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')}
                parser={(value) => value.replace(/\$\s?|(,*)/g, '')}
              />
            </Form.Item>

            <Form.Item label="限流限制（QPS）" style={{ marginBottom: 0 }}>
              <InputNumber
                value={apiConfig.rateLimit || 100}
                onChange={(val) => handleConfigChange('rateLimit', val)}
                min={1}
                max={10000}
                style={{ width: '100%', borderRadius: 6 }}
                disabled={!editable}
                formatter={(value) => `${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')}
                parser={(value) => value.replace(/\$\s?|(,*)/g, '')}
              />
            </Form.Item>
          </div>
        </Card>

        <Divider>入参Schema</Divider>
        <SchemaEditor
          form={form}
          schemaType="requestSchema"
          editable={editable}
        />

        <Divider>出参Schema</Divider>
        <SchemaEditor
          form={form}
          schemaType="responseSchema"
          editable={editable}
        />
      </div>
    );
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
      key: 'apiConfig',
      label: '接口配置',
      children: renderApiConfig(),
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
                <span className="page-desc">{isEdit ? '查看连接器的基本信息和接口配置' : '创建新的连接器，配置接口信息'}</span>
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
        </div>
      </div>
    </div>
  );
};

export default ConnectorEditor;
