import React, { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Card, Form, Input, Button, Radio, message } from 'antd';
import { EditOutlined, EyeOutlined, EyeInvisibleOutlined, CopyOutlined } from '@ant-design/icons';
import { fetchAppById, updateBasicInfo, updateAuthType } from './thunk';
import styles from './BasicInfo.module.less';

const BasicInfo: React.FC = () => {
  const [searchParams] = useSearchParams();
  const appId = searchParams.get('appId') || '';
  const [loading, setLoading] = useState(false);
  const [info, setInfo] = useState<any>(null);
  const [editingBasic, setEditingBasic] = useState(false);
  const [editingAuth, setEditingAuth] = useState(false);
  const [secretVisible, setSecretVisible] = useState(false);
  const [basicForm] = Form.useForm();
  const [authForm] = Form.useForm();

  useEffect(() => {
    loadAppInfo();
  }, [appId]);

  const loadAppInfo = async () => {
    setLoading(true);
    const data = await fetchAppById(appId);
    setInfo(data);
    basicForm.setFieldsValue(data);
    authForm.setFieldsValue({ authType: data.authType });
    setLoading(false);
  };

  const handleSaveBasic = async () => {
    const values = await basicForm.validateFields();
    await updateBasicInfo({ ...info, ...values });
    setEditingBasic(false);
    message.success('保存成功');
    loadAppInfo();
  };

  const handleSaveAuth = async () => {
    const values = await authForm.validateFields();
    await updateAuthType(appId, values.authType);
    setEditingAuth(false);
    message.success('保存成功');
    loadAppInfo();
  };

  const handleCopySecret = () => {
    navigator.clipboard.writeText(info?.appSecret || '');
    message.success('已复制到剪贴板');
  };

  if (!info) return null;

  return (
    <div className={styles.container}>
      <h2 className={styles.pageTitle}>凭证和基础信息</h2>
      <p className={styles.pageDesc}>管理应用凭证、安全配置和回调地址</p>

      <Card className={styles.card} loading={loading}>
        <div className={styles.cardTitle}>应用凭证</div>
        <div className={styles.formRow}>
          <span className={styles.label}>APP ID:</span>
          <span className={styles.value}>{info.appId}</span>
        </div>
        <div className={styles.formRow}>
          <span className={styles.label}>APP Secret:</span>
          <span className={styles.value}>
            {secretVisible ? info.appSecret : '********'}
            <Button 
              type="text" 
              icon={secretVisible ? <EyeInvisibleOutlined /> : <EyeOutlined />}
              onClick={() => setSecretVisible(!secretVisible)}
            />
            <Button 
              type="text" 
              icon={<CopyOutlined />}
              onClick={handleCopySecret}
            />
          </span>
        </div>
      </Card>

      <Card className={styles.card} loading={loading}>
        <div className={styles.cardHeader}>
          <div className={styles.cardTitle}>基础信息</div>
          {!editingBasic && (
            <Button type="link" icon={<EditOutlined />} onClick={() => setEditingBasic(true)}>
              编辑
            </Button>
          )}
        </div>
        {editingBasic ? (
          <Form form={basicForm} layout="vertical">
            <Form.Item label="应用图标" name="icon">
              <Input />
            </Form.Item>
            <Form.Item label="中文名称" name="nameZh" rules={[{ required: true }]}>
              <Input />
            </Form.Item>
            <Form.Item label="英文名称" name="nameEn" rules={[{ required: true }]}>
              <Input />
            </Form.Item>
            <Form.Item label="应用中文描述" name="descZh">
              <Input.TextArea rows={2} />
            </Form.Item>
            <Form.Item label="应用英文描述" name="descEn">
              <Input.TextArea rows={2} />
            </Form.Item>
            <Form.Item>
              <Button onClick={() => setEditingBasic(false)}>取消</Button>
              <Button type="primary" onClick={handleSaveBasic} style={{ marginLeft: 8 }}>
                保存
              </Button>
            </Form.Item>
          </Form>
        ) : (
          <>
            <div className={styles.formRow}>
              <span className={styles.label}>应用图标:</span>
              <span className={styles.value}>{info.icon}</span>
            </div>
            <div className={styles.formRow}>
              <span className={styles.label}>中文名称:</span>
              <span className={styles.value}>{info.nameZh}</span>
            </div>
            <div className={styles.formRow}>
              <span className={styles.label}>英文名称:</span>
              <span className={styles.value}>{info.nameEn}</span>
            </div>
            <div className={styles.formRow}>
              <span className={styles.label}>应用中文描述:</span>
              <span className={styles.value}>{info.descZh}</span>
            </div>
            <div className={styles.formRow}>
              <span className={styles.label}>应用英文描述:</span>
              <span className={styles.value}>{info.descEn}</span>
            </div>
          </>
        )}
      </Card>

      <Card className={styles.card} loading={loading}>
        <div className={styles.cardHeader}>
          <div className={styles.cardTitle}>认证方式</div>
          {!editingAuth && (
            <Button type="link" icon={<EditOutlined />} onClick={() => setEditingAuth(true)}>
              编辑
            </Button>
          )}
        </div>
        {editingAuth ? (
          <Form form={authForm}>
            <Form.Item name="authType">
              <Radio.Group>
                <Radio value="cookie">Cookie</Radio>
                <Radio value="signature">数字签名</Radio>
                <Radio value="soaheader">SOAHeader</Radio>
                <Radio value="soaurl">SOAURL</Radio>
              </Radio.Group>
            </Form.Item>
            <Form.Item>
              <Button onClick={() => setEditingAuth(false)}>取消</Button>
              <Button type="primary" onClick={handleSaveAuth} style={{ marginLeft: 8 }}>
                保存
              </Button>
            </Form.Item>
          </Form>
        ) : (
          <div className={styles.formRow}>
            <span className={styles.label}>认证方式:</span>
            <span className={styles.value}>{info.authType}</span>
          </div>
        )}
      </Card>
    </div>
  );
};

export default BasicInfo;
