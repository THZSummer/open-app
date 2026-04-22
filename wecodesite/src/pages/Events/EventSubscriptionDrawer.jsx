import React, { useState, useEffect } from 'react';
import { Drawer, Form, Radio, Input, Button } from 'antd';
import { EVENT_CHANNEL_TYPE, AUTH_TYPE } from '../../utils/constants';
import './EventSubscriptionDrawer.m.less';

function EventSubscriptionDrawer({ open, onClose, onSave, event }) {
  const [form] = Form.useForm();
  const [channelType, setChannelType] = useState(0);

  useEffect(() => {
    if (event && open) {
      form.setFieldsValue({
        channelType: event.channelType ?? 0,
        channelAddress: event.channelAddress || '',
        authType: event.authType ?? 0
      });
      setChannelType(event.channelType ?? 0);
    }
  }, [event, open, form]);

  const handleChannelTypeChange = (e) => {
    setChannelType(e.target.value);
    form.setFieldsValue({ channelType: e.target.value });
  };

  const handleSave = () => {
    form.validateFields().then((values) => {
      onSave({
        ...event,
        channelType: values.channelType,
        channelAddress: values.channelAddress || '',
        authType: values.authType
      });
      onClose();
    });
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
          <Button type="primary" onClick={handleSave}>
            保存
          </Button>
        </div>
      }
    >
      {event && (
        <div className="event-info">
          <div className="info-item">
            <span className="label">事件名称:</span>
            <span className="value">{event.permission?.nameCn}</span>
          </div>
          <div className="info-item">
            <span className="label">Topic:</span>
            <span className="value">{event.event?.topic}</span>
          </div>
          <div className="info-item">
            <span className="label">所需权限:</span>
            <span className="value">{event.permission?.nameCn}</span>
          </div>
        </div>
      )}

      <Form form={form} layout="vertical" className="subscription-form">
        <Form.Item name="channelType" label="通道类型">
          <Radio.Group onChange={handleChannelTypeChange}>
            <Radio value={0}>{EVENT_CHANNEL_TYPE[0]}</Radio>
            <Radio value={1}>{EVENT_CHANNEL_TYPE[1]}</Radio>
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
              rules={[{ required: true, message: '请输入请求地址' }]}
            >
              <Input placeholder="https://your-domain.com/webhook" />
            </Form.Item>
            <Form.Item name="authType" label="认证类型">
              <Radio.Group>
                <Radio value={0}>{AUTH_TYPE[0]}</Radio>
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