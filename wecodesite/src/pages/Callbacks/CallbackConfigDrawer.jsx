import React, { useState, useEffect } from 'react';
import { Drawer, Form, Radio, Input, Button } from 'antd';
import { CALLBACK_CHANNEL_TYPE, AUTH_TYPE } from '../../utils/constants';
import './CallbackConfigDrawer.m.less';

function CallbackConfigDrawer({ open, onClose, onSave, callback }) {
  const [form] = Form.useForm();
  const [channelType, setChannelType] = useState(0);

  useEffect(() => {
    if (callback && open) {
      form.setFieldsValue({
        channelType: callback.channelType ?? 0,
        channelAddress: callback.channelAddress || '',
        authType: callback.authType ?? 0
      });
      setChannelType(callback.channelType ?? 0);
    }
  }, [callback, open, form]);

  const handleChannelTypeChange = (e) => {
    setChannelType(e.target.value);
    form.setFieldsValue({ channelType: e.target.value });
  };

  const handleSave = () => {
    form.validateFields().then((values) => {
      onSave({
        ...callback,
        channelType: values.channelType,
        channelAddress: values.channelAddress || '',
        authType: values.authType
      });
      onClose();
    });
  };

  return (
    <Drawer
      title="回调方式"
      placement="right"
      width={500}
      onClose={onClose}
      open={open}
      className="callback-config-drawer"
      footer={
        <div className="drawer-footer">
          <Button onClick={onClose}>取消</Button>
          <Button type="primary" onClick={handleSave}>
            保存
          </Button>
        </div>
      }
    >
      {callback && (
        <div className="callback-info">
          <div className="info-item">
            <span className="label">回调名称:</span>
            <span className="value">{callback.permission?.nameCn}</span>
          </div>
          <div className="info-item">
            <span className="label">所需权限:</span>
            <span className="value">{callback.permission?.nameCn}</span>
          </div>
        </div>
      )}

      <Form form={form} layout="vertical" className="subscription-form">
        <Form.Item name="channelType" label="通道类型">
          <Radio.Group onChange={handleChannelTypeChange}>
            <Radio value={0}>{CALLBACK_CHANNEL_TYPE[0]}</Radio>
            <Radio value={1}>{CALLBACK_CHANNEL_TYPE[1]}</Radio>
            <Radio value={2}>{CALLBACK_CHANNEL_TYPE[2]}</Radio>
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

        {(channelType === 1 || channelType === 2) && (
          <>
            <Form.Item
              name="channelAddress"
              label="回调地址"
              rules={[{ required: true, message: '请输入回调地址' }]}
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

export default CallbackConfigDrawer;