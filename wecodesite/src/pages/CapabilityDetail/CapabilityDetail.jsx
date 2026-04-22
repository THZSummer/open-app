import React from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { Card, Button, Form, Input, Switch, message } from 'antd';
import { ArrowLeftOutlined, RobotOutlined, GlobalOutlined, AppstoreOutlined, MailOutlined } from '@ant-design/icons';
import './CapabilityDetail.m.less';

const capabilityConfigs = {
  bot: {
    name: '机器人',
    icon: <RobotOutlined />,
    description: '通过飞书会话与用户进行消息交互',
    fields: [
      { name: 'botName', label: '机器人名称', placeholder: '请输入机器人名称', required: true },
      { name: 'botDesc', label: '机器人描述', placeholder: '请输入机器人描述', required: false },
    ],
  },
  web: {
    name: '网页应用',
    icon: <GlobalOutlined />,
    description: 'H5 开发，运行在飞书客户端内',
    fields: [
      { name: 'homepageUrl', label: '桌面端主页地址', placeholder: '请输入桌面端主页地址', required: true },
      { name: 'mobileHomepageUrl', label: '移动端主页地址', placeholder: '请输入移动端主页地址', required: false },
      { name: 'openMode', label: '桌面端主页打开方式', placeholder: '在飞书内新标签页打开', required: false },
    ],
  },
  miniapp: {
    name: '小程序',
    icon: <MailOutlined />,
    description: '支持在小程序中实现复杂交互',
    fields: [
      { name: 'appName', label: '小程序名称', placeholder: '请输入小程序名称', required: true },
      { name: 'appDesc', label: '小程序描述', placeholder: '请输入小程序描述', required: false },
    ],
  },
  widget: {
    name: '小组件',
    icon: <AppstoreOutlined />,
    description: '将应用嵌入到云文档、多维表格等飞书模块',
    fields: [
      { name: 'widgetName', label: '小组件名称', placeholder: '请输入小组件名称', required: true },
      { name: 'widgetDesc', label: '小组件描述', placeholder: '请输入小组件描述', required: false },
    ],
  },
};

function CapabilityDetail() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const [form] = Form.useForm();
  const appId = searchParams.get('appId') || '1';
  const capabilityType = searchParams.get('type') || 'bot';
  const enabledCapabilities = searchParams.get('caps')?.split(',').filter(Boolean) || ['bot'];

  const config = capabilityConfigs[capabilityType];
  const isEnabled = (type) => enabledCapabilities.includes(type);

  if (!config) {
    return (
      <div className="capability-detail">
        <div style={{ marginBottom: 16 }}>
          <Button type="link" icon={<ArrowLeftOutlined />} onClick={() => navigate(`/capabilities?appId=${appId}&caps=${enabledCapabilities.join(',')}`)}>
            返回应用能力
          </Button>
        </div>
        <h4>应用能力详情</h4>
        <span style={{ color: '#8c8c8c' }}>查看应用能力详情和权限配置</span>
      </div>
    );
  }

  if (!isEnabled(capabilityType)) {
    return (
      <div className="capability-detail">
        <div style={{ marginBottom: 16 }}>
          <Button type="link" icon={<ArrowLeftOutlined />} onClick={() => navigate(`/capabilities?appId=${appId}&caps=${enabledCapabilities.join(',')}`)}>
            返回应用能力
          </Button>
        </div>
        <h4>{config.name}</h4>
        <span style={{ color: '#8c8c8c' }}>该能力尚未启用，请先在添加应用能力页面启用</span>
      </div>
    );
  }

  const handleSave = () => {
    form.validateFields().then((values) => {
      console.log('Saved values:', values);
      message.success('保存成功');
    });
  };

  return (
    <div className="capability-detail">
      <div style={{ marginBottom: 16 }}>
        <Button type="link" icon={<ArrowLeftOutlined />} onClick={() => navigate(`/capabilities?appId=${appId}&caps=${enabledCapabilities.join(',')}`)}>
          返回应用能力
        </Button>
      </div>

      <div className="detail-header">
        <div className="detail-icon">{config.icon}</div>
        <div className="detail-title">
          <h4>{config.name}</h4>
          <span style={{ color: '#8c8c8c' }}>{config.description}</span>
        </div>
      </div>

      <Card title="基础配置" className="config-card">
        <Form
          form={form}
          layout="vertical"
          initialValues={{
            enabled: true,
          }}
        >
          <Form.Item
            name="enabled"
            label="能力开关"
            valuePropName="checked"
          >
            <Switch checkedChildren="开" unCheckedChildren="关" />
          </Form.Item>

          {config.fields.map((field) => (
            <Form.Item
              key={field.name}
              name={field.name}
              label={field.label}
              rules={field.required ? [{ required: true, message: `请输入${field.label}` }] : []}
            >
              <Input placeholder={field.placeholder} />
            </Form.Item>
          ))}
        </Form>

        <div className="form-actions">
          <Button type="primary" onClick={handleSave}>保存</Button>
          <Button onClick={() => navigate(`/capabilities?appId=${appId}`)}>取消</Button>
        </div>
      </Card>
    </div>
  );
}

export default CapabilityDetail;