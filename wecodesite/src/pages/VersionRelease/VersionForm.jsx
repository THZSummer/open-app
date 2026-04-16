import React, { useState, useEffect } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { Typography, Card, Form, Input, Button, Space, Tag, Divider } from 'antd';
import { ArrowLeftOutlined, CheckOutlined, RobotOutlined, GlobalOutlined, AppstoreOutlined, MailOutlined } from '@ant-design/icons';
import { mockVersions } from './mock';
import './VersionRelease.m.less';

const { Title, Text } = Typography;

const capabilityIconMap = {
  '机器人': <RobotOutlined />,
  '网页应用': <GlobalOutlined />,
  '小程序': <AppstoreOutlined />,
  '小组件': <GlobalOutlined />,
};

const capabilityConfigs = [
  { type: 'bot', name: '机器人' },
  { type: 'web', name: '网页应用' },
  { type: 'miniapp', name: '小程序' },
  { type: 'widget', name: '小组件' },
];

function VersionForm() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const appId = searchParams.get('appId') || '1';
  const versionId = searchParams.get('versionId');
  const isViewMode = !!versionId;
  const [form] = Form.useForm();
  const enabledCapabilities = searchParams.get('caps')?.split(',').filter(Boolean) || ['bot'];

  const [versionData, setVersionData] = useState(null);

  useEffect(() => {
    if (versionId) {
      const version = mockVersions.find((v) => v.id === parseInt(versionId));
      if (version) {
        setVersionData(version);
        form.setFieldsValue({
          version: version.version,
          desc: version.desc,
        });
      }
    }
  }, [versionId, form]);

  const handleSave = () => {
    form.validateFields().then((values) => {
      console.log('Saved values:', values);
      navigate(`/version-release?appId=${appId}`);
    });
  };

  const handleCancel = () => {
    navigate(`/version-release?appId=${appId}`);
  };

  const getEnabledCapabilityNames = () => {
    return capabilityConfigs
      .filter((cap) => enabledCapabilities.includes(cap.type))
      .map((cap) => cap.name);
  };

  const getDisplayCapabilities = () => {
    if (isViewMode && versionData?.capabilities) {
      return versionData.capabilities;
    }
    return getEnabledCapabilityNames();
  };

  return (
    <div className="version-form">
      <Space style={{ marginBottom: 16 }}>
        <Button type="link" icon={<ArrowLeftOutlined />} onClick={handleCancel}>
          返回版本列表
        </Button>
      </Space>

      <div className="form-header">
        <Title level={4} className="page-title">
          {isViewMode ? '查看版本详情' : '创建新版本'}
        </Title>
        <Text type="secondary" className="page-desc">
          {isViewMode ? '查看版本信息和应用能力配置' : '填写版本信息并提交审核'}
        </Text>
      </div>

      <Card className="form-card">
        <Form
          form={form}
          layout="vertical"
        >
          <Form.Item
            name="version"
            label="版本号"
            rules={[{ required: true, message: '请输入版本号' }]}
          >
            {isViewMode ? (
              <Text className="form-text">{versionData?.version}</Text>
            ) : (
              <Input placeholder="请输入版本号，如：1.0.0" />
            )}
          </Form.Item>

          <Form.Item
            name="desc"
            label="版本描述"
            rules={[{ required: true, message: '请输入版本描述' }]}
          >
            {isViewMode ? (
              <Text className="form-text">{versionData?.desc}</Text>
            ) : (
              <Input.TextArea
                placeholder="请输入版本描述，说明本次发布的主要内容和变更"
                rows={4}
              />
            )}
          </Form.Item>

          <Divider />

          <Form.Item label="当前添加的应用能力">
            <div className="capabilities-display">
              {getDisplayCapabilities().length > 0 ? (
                getDisplayCapabilities().map((capName, index) => (
                  <Tag key={index} icon={capabilityIconMap[capName]} className="capability-tag">
                    {capName}
                  </Tag>
                ))
              ) : (
                <Text type="secondary">暂无已启用的应用能力</Text>
              )}
            </div>
          </Form.Item>
        </Form>

        <div className="form-actions">
          {isViewMode ? (
            <Button type="primary" icon={<CheckOutlined />} onClick={handleCancel}>
              完成
            </Button>
          ) : (
            <>
              <Button type="primary" onClick={handleSave}>
                提交审核
              </Button>
              <Button onClick={handleCancel}>
                取消
              </Button>
            </>
          )}
        </div>
      </Card>
    </div>
  );
}

export default VersionForm;