import React, { useState, useEffect } from 'react';
import { Drawer, Form, Radio, Input, Button, message } from 'antd';
import { CHANNEL_TYPE, AUTH_TYPE } from '../../utils/constants';
import { configCallbackSubscription } from './thunk';
import { queryParams } from '../../utils/common';
import './CallbackConfigDrawer.m.less';

function CallbackConfigDrawer({ open, onClose, onSave, callback }) {
  const [form] = Form.useForm();
  const [channelType, setChannelType] = useState(0);
  const [saving, setSaving] = useState(false);
  const appId = queryParams('appId');

  useEffect(() => {
    if (callback && open) {
      form.setFieldsValue(callback);
      setChannelType(callback.channelType);
    }
  }, [callback, open, form]);

  const handleSave = async () => {
    const values = await form.validateFields();
    setSaving(true);
    const res = await configCallbackSubscription(appId, callback.id, {
      authType: values.authType,
      channelType: values.channelType,
      channelAddress: values.channelAddress || '',
    });
    if (res && res.code === '200') {
      onSave();
      onClose();
      message.success('配置已保存');
    } else {
      message.error(res?.messageZh || res?.message || '保存失败');
    }
    setSaving(false);
  };

  const handleChannelTypeChange = (e) => {
    const newChannelType = e.target.value;
    setChannelType(newChannelType);
    form.setFieldsValue({
      channelType: newChannelType,
      channelAddress: '',
      authType: 1
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
          <Button type="primary" onClick={handleSave} loading={saving}>
            保存
          </Button>
        </div>
      }
    >
      {callback && (
        <div className="callback-info">
          <div className="info-item">
            <span className="label">权限名称:</span>
            <span className="value">{callback.permission?.nameCn}</span>
          </div>
          <div className="info-item">
            <span className="label">Scope标识:</span>
            <span className="value">{callback.permission?.scope}</span>
          </div>
        </div>
      )}

      <Form form={form} layout="vertical" className="subscription-form">
        <Form.Item name="channelType" label="订阅方式"
        rules={[
          { required: true, message: '请选择订阅方式' }
        ]}
        >
          <Radio.Group onChange={handleChannelTypeChange}>
            <Radio value={1}>{CHANNEL_TYPE[1]}</Radio>
            <Radio value={2}>{CHANNEL_TYPE[2]}</Radio>
            <Radio value={3}>{CHANNEL_TYPE[3]}</Radio>
          </Radio.Group>
        </Form.Item>

        {channelType && (
          <>
            <Form.Item
              name="channelAddress"
              label="回调地址"
              rules={[
                { required: true, message: '请输入回调地址' },
                {
                  validator(_, value) {
                    if (!value) {
                      return Promise.reject('请输入回调地址');
                    }
                    
                    const protocolPattern = channelType === 3 ? /^wss?:\/\/.+/ : /^https?:\/\/.+/;
                    const errorMessage = channelType === 3 ? 'WebSocket 回调地址必须以 ws:// 或 wss:// 开头'  : '回调地址必须以 http:// 或 https:// 开头';
                    
                    if (protocolPattern.test(value)) {
                      return Promise.resolve();
                    }
                    return Promise.reject(errorMessage);
                  },
                },
              ]}
              validateFirst={true}
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

export default CallbackConfigDrawer;
