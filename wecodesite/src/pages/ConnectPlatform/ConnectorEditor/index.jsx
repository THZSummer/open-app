/**
 * ========================================
 * 连接器管理 - 接口配置页面
 * ========================================
 *
 * 功能：
 * - 编辑连接器接口配置
 * - 保存连接器接口配置
 */

import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Form,
  Input,
  Button,
  Space,
  message,
  Divider,
  Radio,
  Card,
} from 'antd';
import {
  fetchConnectorConfig,
  saveConnectorConfig,
  transformFromSchemaFormat,
  transformToSchemaFormat,
} from './thunk';
import SchemaEditor from '../../../components/SchemaEditor/SchemaEditor.jsx';
import SimpleSidebar from '../../../components/SimpleSidebar/SimpleSidebar';
import {
  DEFAULT_API_CONFIG,
  AUTH_SCHEMA_MAP,
  HTTP_METHOD_OPTIONS,
  AUTH_TYPE_OPTIONS,
  AUTH_REQUEST_SCHEMA_CONFIG,
  REQUEST_SCHEMA_CONFIG,
  RESPONSE_SCHEMA_CONFIG,
} from './constants';
import { queryParams } from '../../../utils/common';
import './ConnectorEditor.m.less';

/**
 * 连接器编辑页面组件
 */
const ConnectorEditor = () => {
  const navigate = useNavigate();
  const connectorId = queryParams('id');

  /**
   * State定义
   */
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [detailLoading, setDetailLoading] = useState(!!connectorId);

  // API配置状态（用于触发重新渲染）
  const [apiConfig, setApiConfig] = useState(DEFAULT_API_CONFIG);

  /**
   * 处理认证方式变化
   * @param {string} newAuthType - 新的认证方式
   */
  const handleAuthTypeChange = (newAuthType) => {
    const currentConfig = form.getFieldValue('apiConfig') || {};
    const newAuthSchema = AUTH_SCHEMA_MAP[newAuthType] || [];

    const newConfig = {
      ...currentConfig,
      authType: newAuthType,
      authRequestSchema: newAuthSchema,
    };

    setApiConfig(newConfig);
    form.setFieldValue('apiConfig', newConfig);
  };

  /**
   * 副作用 - 加载连接器详情
   */
  useEffect(() => {
    if (connectorId) {
      loadDetail(connectorId);
    }
  }, [connectorId]);

  /**
   * 加载连接器接口配置
   * @param {string} id - 连接器ID
   */
  const loadDetail = async (id) => {
    setDetailLoading(true);

    const result = await fetchConnectorConfig(id);

    if (result && result.code === '200') {
      // 检查是否有配置且 connectionConfig 存在
      if (result.data?.hasConfig && result.data?.connectionConfig) {
        try {
          // 解析 connectionConfig JSON 字符串为对象
          const parsedConfig = JSON.parse(result.data.connectionConfig);
          // 将解析后的数据转换为表单可编辑的格式
          const config = transformFromSchemaFormat(parsedConfig);
          setApiConfig(config);
          form.setFieldValue('apiConfig', config);
        } catch (error) {
          message.error('解析接口配置失败');
        }
      }
      // 没有配置或解析失败时，保持默认状态（初始值即为默认值）
    } else {
      message.error(result?.messageZh || '加载接口配置失败');
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
   * 提交表单
   */
  const handleSubmit = async () => {
    setLoading(true);

    // 获取接口配置数据
    const apiConfig = form.getFieldValue('apiConfig') || {};

    // 转换为符合文档4.3示例的数据格式
    const params = {
      connectionConfig: JSON.stringify(transformToSchemaFormat(apiConfig)),
    };

    // 调用API保存接口配置
    const result = await saveConnectorConfig(connectorId, params);

    if (result && result.code === '200') {
      message.success('保存成功');
      handleBack();
    } else {
      message.error(result?.messageZh || '保存失败');
    }

    setLoading(false);
  };

  /**
   * 渲染接口配置表单
   */
  const renderApiConfig = () => {
    const handleConfigChange = (field, value) => {
      const currentConfig = form.getFieldValue('apiConfig') || {};
      const newConfig = { ...currentConfig, [field]: value };
      setApiConfig(newConfig);
      form.setFieldValue('apiConfig', newConfig);
    };

    return (
      <div className="api-config-form">
        <Card
          title="接口配置"
          size="small"
          className="api-config-card"
        >
          <Form.Item label="协议类型" className="form-item-spacing">
            <Radio.Group
              value={apiConfig.protocolType}
              onChange={(e) => handleConfigChange('protocolType', e.target.value)}
            >
              {HTTP_METHOD_OPTIONS.map(method => (
                <Radio.Button key={method} value={method}>
                  {method}
                </Radio.Button>
              ))}
            </Radio.Group>
          </Form.Item>

          <Form.Item label="协议地址" className="form-item-no-spacing">
            <Input
              value={apiConfig.protocolAddress}
              onChange={(e) => handleConfigChange('protocolAddress', e.target.value)}
              placeholder="https://api.example.com/endpoint"
              className="input-border-radius"
            />
          </Form.Item>
        </Card>

        <Card
          title="认证方式配置"
          size="small"
          className="auth-config-card"
        >
          <Form.Item label="认证方式" className="form-item-spacing">
            <Radio.Group
              value={apiConfig.authType}
              onChange={(e) => handleAuthTypeChange(e.target.value)}
            >
              {AUTH_TYPE_OPTIONS.map(option => (
                <Radio key={option.value} value={option.value}>
                  {option.label}
                </Radio>
              ))}
            </Radio.Group>
          </Form.Item>

          <Divider className="divider-spacing">认证参数配置</Divider>
          {apiConfig.authType ? (
            <SchemaEditor
              form={form}
              apiConfig={apiConfig}
              {...AUTH_REQUEST_SCHEMA_CONFIG}
            />
          ) : (
            <div className="placeholder-text">
              请先选择认证方式
            </div>
          )}
        </Card>

        <Card
          title="入参配置"
          size="small"
          className="request-config-card"
        >
          <SchemaEditor
            form={form}
            apiConfig={apiConfig}
            {...REQUEST_SCHEMA_CONFIG}
          />
        </Card>

        <Card
          title="出参配置"
          size="small"
          className="response-config-card"
        >
          <SchemaEditor
            form={form}
            apiConfig={apiConfig}
            {...RESPONSE_SCHEMA_CONFIG}
          />
        </Card>
      </div>
    );
  };

  /**
   * 渲染
   */
  return (
    <div className="connector-editor-wrapper">
      {/* 左侧导航栏 */}
      <SimpleSidebar />

      {/* 主内容区 */}
      <div className="editor-main-content">
        <div
          className={`connector-editor-page ${detailLoading ? 'loading' : ''}`}
        >
          {/* 页面头部 */}
          <div className="page-header">
            <div className="page-header-title">
              <h4 className="page-title">接口配置</h4>
              <span className="page-desc">编辑连接器的接口配置信息</span>
            </div>
          </div>

          {/* 接口配置内容 */}
          {renderApiConfig()}

          {/* 操作按钮区域 */}
          <div className="action-bar">
            <Button
              className="back-button"
              onClick={handleBack}
            >
              返回
            </Button>
            <Button
              type="primary"
              onClick={handleSubmit}
              loading={loading}
            >
              保存
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ConnectorEditor;
