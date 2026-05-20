import React, { useState, useEffect } from 'react';
import { Drawer, Form, Radio, Input, Button, message } from 'antd';
import { CHANNEL_TYPE, AUTH_TYPE } from '../../utils/constants';
import { configEventSubscription } from './thunk';
import { queryParams } from '../../utils/common';
import './EventSubscriptionDrawer.m.less';

function EventSubscriptionDrawer({ open, onClose, onSave, event }) {
  const [form] = Form.useForm();
  const [channelType, setChannelType] = useState(0);
  const [saving, setSaving] = useState(false);
  const appId = queryParams('appId');

  useEffect(() => {
    if (event && open) {
      form.setFieldsValue(event);
      setChannelType(event.channelType);
    }
  }, [event, open, form]);

  const handleChannelTypeChange = (e) => {
    const newChannelType = e.target.value;
    setChannelType(newChannelType);
    const updateFields = { channelType: newChannelType };
    updateFields.authType = newChannelType === 1 ? 1 : null;
    updateFields.channelAddress = '';
    form.setFieldsValue(updateFields);
  };

  const handleSave = async () => {
    const values = await form.validateFields();
    setSaving(true);
    const res = await configEventSubscription(appId, event.id, {
      channelAddress: values.channelAddress || '',
      channelType: values.channelType,
      authType: values.authType
    });
    if (res && res.code === '200') {
      message.success('配置已保存');
      onSave();
      onClose();
    } else {
      message.error(res?.messageZh || res?.message || '保存失败');
    }
    setSaving(false);
  };

  return (
    <Drawer
      title="订阅方式"
      placement="right"
      width={500}
      onClose={onClose}
      open={open}
      className="event-subscription-drawer"
      footer={
        <div className="drawer-footer">
          <Button onClick={onClose}>取消</Button>
          <Button type="primary" onClick={handleSave} loading={saving}>
            保存
          </Button>
        </div>
      }
    >
      {event && (
        <div className="event-info">
          <div className="info-item">
            <span className="label">权限名称:</span>
            <span className="value">{event.permission?.nameCn}</span>
          </div>
          <div className="info-item">
            <span className="label">事件Topic:</span>
            <span className="value">{event.event?.topic}</span>
          </div>
          <div className="info-item">
            <span className="label">Scope标识:</span>
            <span className="value">{event.permission?.scope}</span>
          </div>
        </div>
      )}

      <Form form={form} layout="vertical" className="subscription-form">
        <Form.Item name="channelType" label="订阅类型"
        rules={[
          { required: true, message: '请选择订阅方式' }
        ]}
        >
          <Radio.Group onChange={handleChannelTypeChange}>
            <Radio value={0}>{CHANNEL_TYPE[0]}</Radio>
            <Radio value={1}>{CHANNEL_TYPE[1]}</Radio>
          </Radio.Group>
        </Form.Item>

        {channelType === 0 && (
          <Form.Item label="说明文档">
            <a href="https://example.com/mqs-docs" target="_blank" rel="noopener noreferrer">
              <span style={{ color: '#000' }}>查看</span>
              <span style={{ color: '#1890ff' }}>MQS内置消息队列使用规范</span>
            </a>
          </Form.Item>
        )}

        {channelType === 1 && (
          <>
            <Form.Item
              name="channelAddress"
              label="请求地址"
              rules={[
                { required: true, message: '请输入请求地址' },
                { pattern: /^https?:\/\/.+/, message: '请求地址必须以 http:// 或 https:// 开头' }
              ]}
            >
              <Input placeholder="https://your-domain.com/webhook" />
            </Form.Item>
            <Form.Item name="authType" label="认证方式">
              <Radio.Group>
                <Radio value={1}>{AUTH_TYPE[1]}</Radio>
              </Radio.Group>
            </Form.Item>
          </>
        )}
      </Form>
    </Drawer>
  );
}

export default EventSubscriptionDrawer;
