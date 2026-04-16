import React, { useState } from 'react';
import { Button, Input, Switch, message } from 'antd';
import { ArrowLeftOutlined } from '@ant-design/icons';
import { useLocation, useNavigate } from 'react-router-dom';
import './CapabilityDetail.m.less';

const { TextArea } = Input;

const CapabilityDetail = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const searchParams = new URLSearchParams(location.search);
  const appId = searchParams.get('appId');
  const capType = searchParams.get('type');
  const caps = searchParams.get('caps') || '';

  // 能力配置
  const capabilityConfig = {
    bot: {
      name: '机器人',
      icon: '🤖',
      description: '通过飞书会话与用户进行消息交互',
      fields: [
        {
          name: '机器人名称',
          key: 'botName',
          required: true,
          component: 'Input'
        },
        {
          name: '机器人描述',
          key: 'botDescription',
          required: false,
          component: 'TextArea'
        }
      ]
    },
    web: {
      name: '网页应用',
      icon: '🌐',
      description: 'H5开发，运行在飞书客户端内',
      fields: [
        {
          name: '桌面端主页地址',
          key: 'desktopUrl',
          required: true,
          component: 'Input'
        },
        {
          name: '移动端主页地址',
          key: 'mobileUrl',
          required: false,
          component: 'Input'
        },
        {
          name: '桌面端主页打开方式',
          key: 'desktopOpenType',
          required: false,
          component: 'Input'
        }
      ]
    },
    miniapp: {
      name: '小程序',
      icon: '📧',
      description: '支持在小程序中实现复杂交互',
      fields: [
        {
          name: '小程序名称',
          key: 'miniappName',
          required: true,
          component: 'Input'
        },
        {
          name: '小程序描述',
          key: 'miniappDescription',
          required: false,
          component: 'TextArea'
        }
      ]
    },
    widget: {
      name: '小组件',
      icon: '📱',
      description: '将应用嵌入到云文档等飞书模块',
      fields: [
        {
          name: '小组件名称',
          key: 'widgetName',
          required: true,
          component: 'Input'
        },
        {
          name: '小组件描述',
          key: 'widgetDescription',
          required: false,
          component: 'TextArea'
        }
      ]
    }
  };

  const capability = capabilityConfig[capType] || capabilityConfig.bot;
  const [enabled, setEnabled] = useState(false);
  const [formData, setFormData] = useState({});

  const handleBack = () => {
    const params = new URLSearchParams();
    if (appId) params.append('appId', appId);
    if (caps) params.append('caps', caps);
    navigate(`/capabilities?${params.toString()}`);
  };

  const handleSave = () => {
    // 验证必填项
    const hasEmptyRequired = capability.fields.some(field => 
      field.required && !formData[field.key]
    );
    if (hasEmptyRequired) {
      message.error('请填写必填项');
      return;
    }
    message.success('保存成功');
  };

  const handleCancel = () => {
    handleBack();
  };

  const handleInputChange = (key, value) => {
    setFormData(prev => ({ ...prev, [key]: value }));
  };

  return (
    <div className="capabilityDetail">
      <Button
        type="text"
        icon={<ArrowLeftOutlined />}
        onClick={handleBack}
        className="backButton"
      >
        返回应用能力
      </Button>
      <div className="header">
        <div className="capIcon">{capability.icon}</div>
        <div className="capInfo">
          <h1 className="capName">{capability.name}</h1>
          <p className="capDescription">{capability.description}</p>
        </div>
      </div>
      <div className="formCard">
        <div className="formHeader">
          <h2 className="formTitle">基础配置</h2>
        </div>
        <div className="formContent">
          <div className="formRow">
            <span className="label">能力开关：</span>
            <Switch
              checked={enabled}
              onChange={setEnabled}
            />
          </div>
          {capability.fields.map((field) => (
            <div key={field.key} className="formRow">
              <span className={`label ${field.required ? 'required' : ''}`}>
                {field.name}：
              </span>
              {field.component === 'Input' ? (
                <Input
                  placeholder={`请输入${field.name}`}
                  value={formData[field.key] || ''}
                  onChange={(e) => handleInputChange(field.key, e.target.value)}
                />
              ) : field.component === 'TextArea' ? (
                <TextArea
                  rows={3}
                  placeholder={`请输入${field.name}`}
                  value={formData[field.key] || ''}
                  onChange={(e) => handleInputChange(field.key, e.target.value)}
                />
              ) : null}
            </div>
          ))}
          <div className="formActions">
            <Button onClick={handleSave}>保存</Button>
            <Button onClick={handleCancel}>取消</Button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default CapabilityDetail;