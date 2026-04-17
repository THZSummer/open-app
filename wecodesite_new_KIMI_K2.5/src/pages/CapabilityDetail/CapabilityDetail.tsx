import React, { useState, useEffect } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { Card, Form, Input, Switch, Button, message } from 'antd';
import { ArrowLeftOutlined } from '@ant-design/icons';
import { fetchCapabilityConfig, saveCapabilityConfig } from './thunk';
import { capabilityFields } from './mock';
import styles from './CapabilityDetail.module.less';

const capabilityNames: Record<string, { name: string; icon: string; desc: string }> = {
  bot: { name: '机器人', icon: '🤖', desc: '通过会话与用户进行交互' },
  web: { name: '网页应用', icon: '🌐', desc: 'H5 开发能力' },
  miniapp: { name: '小程序', icon: '📧', desc: '轻量级应用' },
  widget: { name: '小组件', icon: '📱', desc: '桌面组件' },
};

const CapabilityDetail: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const appId = searchParams.get('appId') || '';
  const caps = searchParams.get('caps') || '';
  const type = searchParams.get('type') || '';
  const [form] = Form.useForm();

  const [config, setConfig] = useState<Record<string, string>>({});
  const [enabled, setEnabled] = useState(true);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    loadConfig();
  }, [type]);

  const loadConfig = async () => {
    setLoading(true);
    const data = await fetchCapabilityConfig(type);
    setConfig(data);
    form.setFieldsValue(data);
    setLoading(false);
  };

  const handleSave = async () => {
    const values = await form.validateFields();
    await saveCapabilityConfig(type, values);
    message.success('保存成功');
  };

  const handleGoBack = () => {
    navigate(`/capabilities?appId=${appId}&caps=${caps}`);
  };

  const info = capabilityNames[type];
  const fields = capabilityFields[type] || [];

  return (
    <div className={styles.container}>
      <div className={styles.backLink} onClick={handleGoBack}>
        <ArrowLeftOutlined /> 返回应用能力
      </div>

      <div className={styles.header}>
        <div className={styles.icon}>{info?.icon}</div>
        <div className={styles.info}>
          <h2 className={styles.name}>{info?.name}</h2>
          <p className={styles.desc}>{info?.desc}</p>
        </div>
      </div>

      <Card className={styles.card} loading={loading}>
        <div className={styles.sectionTitle}>基础配置</div>
        <div className={styles.switchRow}>
          <span>能力开关</span>
          <Switch checked={enabled} onChange={setEnabled} />
        </div>

        <Form form={form} layout="vertical" className={styles.form}>
          {fields.map((field) => (
            <Form.Item
              key={field.name}
              label={field.label}
              name={field.name}
              rules={field.required ? [{ required: true, message: `请输入${field.label}` }] : []}
            >
              <Input placeholder={`请输入${field.label}`} />
            </Form.Item>
          ))}
        </Form>

        <div className={styles.actions}>
          <Button onClick={handleGoBack}>取消</Button>
          <Button type="primary" onClick={handleSave} style={{ marginLeft: 8 }}>
            保存
          </Button>
        </div>
      </Card>
    </div>
  );
};

export default CapabilityDetail;
