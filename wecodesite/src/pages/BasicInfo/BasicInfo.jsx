import React, { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Form, Input, Button, Card, Radio, Upload, message } from 'antd';
import { EyeInvisibleOutlined, EyeOutlined, CopyOutlined } from '@ant-design/icons';
import { mockAppInfo } from './mock';
import './BasicInfo.m.less';

const authOptions = [
  { label: 'Cookie', value: 'Cookie' },
  { label: '数字签名', value: '数字签名' },
  { label: 'SOAHeader', value: 'SOAHeader' },
  { label: 'SOAURL', value: 'SOAURL' },
];

function BasicInfo() {
  const [searchParams] = useSearchParams();
  const appId = searchParams.get('appId') || '1';
  const [appData, setAppData] = useState(mockAppInfo[appId] || mockAppInfo['1']);
  const [credentialForm] = Form.useForm();
  const [basicInfoForm] = Form.useForm();
  const [authMethodForm] = Form.useForm();
  const [basicInfoEditing, setBasicInfoEditing] = useState(false);
  const [authMethodEditing, setAuthMethodEditing] = useState(false);
  const [showSecret, setShowSecret] = useState(false);

  useEffect(() => {
    const appInfo = mockAppInfo[appId] || mockAppInfo['1'];
    setAppData(appInfo);
    credentialForm.setFieldsValue({
      appId: appInfo.appId,
      appSecret: appInfo.appSecret,
    });
    basicInfoForm.setFieldsValue({
      chineseName: appInfo.chineseName,
      englishName: appInfo.englishName,
      chineseDesc: appInfo.chineseDesc,
      englishDesc: appInfo.englishDesc,
      functionDiagram: appInfo.functionDiagram,
    });
    authMethodForm.setFieldsValue({
      authMethod: appInfo.authMethod,
    });
  }, [appId]);

  const handleCopySecret = () => {
    navigator.clipboard.writeText(appData.appSecret);
    message.success('APP Secret 已复制');
  };

  const handleBasicInfoSave = () => {
    basicInfoForm.validateFields().then((values) => {
      const updatedData = {
        ...appData,
        ...values,
      };
      setAppData(updatedData);
      setBasicInfoEditing(false);
      message.success('基础信息保存成功');
    });
  };

  const handleBasicInfoCancel = () => {
    basicInfoForm.setFieldsValue({
      chineseName: appData.chineseName,
      englishName: appData.englishName,
      chineseDesc: appData.chineseDesc,
      englishDesc: appData.englishDesc,
      functionDiagram: appData.functionDiagram,
    });
    setBasicInfoEditing(false);
  };

  const handleAuthMethodSave = () => {
    authMethodForm.validateFields().then((values) => {
      const updatedData = {
        ...appData,
        ...values,
      };
      setAppData(updatedData);
      setAuthMethodEditing(false);
      message.success('认证方式保存成功');
    });
  };

  const handleAuthMethodCancel = () => {
    authMethodForm.setFieldsValue({
      authMethod: appData.authMethod,
    });
    setAuthMethodEditing(false);
  };

  const renderCredential = () => (
    <div className='formContent'>
      <Form form={credentialForm} layout="horizontal">
        <Form.Item label="APP ID" name="appId">
          <span className="detail-text">{appData.appId}</span>
        </Form.Item>
        <Form.Item label="APP Secret" name="appSecret">
          <span className="detail-text app-secret">
            {showSecret ? appData.appSecret : '********'}
            <span className="secret-actions">
              <span className="secret-btn" onClick={() => setShowSecret(!showSecret)}>
                {showSecret ? <EyeInvisibleOutlined /> : <EyeOutlined />}
              </span>
              <span className="secret-btn" onClick={handleCopySecret}>
                <CopyOutlined />
              </span>
            </span>
          </span>
        </Form.Item>
      </Form>
    </div>
  );

  const renderBasicInfo = () => (
    <div className='formContent'>
      <Form form={basicInfoForm} layout="horizontal">
        <Form.Item label="应用图标">
          {basicInfoEditing ? (
            <div className="icon-upload-wrapper">
              <div className="icon-preview">{appData.icon}</div>
              <Upload showUploadList={false} beforeChange={() => false}>
                <Button>更换图标</Button>
              </Upload>
            </div>
          ) : (
            <div className="icon-preview">{appData.icon}</div>
          )}
        </Form.Item>
        <Form.Item label="中文名称" name="chineseName" rules={basicInfoEditing ? [{ required: true, message: '请输入中文名称' }] : []}>
          {basicInfoEditing ? (
            <Input placeholder="请输入应用中文名称" />
          ) : (
            <span className="detail-text">{appData.chineseName}</span>
          )}
        </Form.Item>
        <Form.Item label="英文名称" name="englishName" rules={basicInfoEditing ? [{ required: true, message: '请输入英文名称' }] : []}>
          {basicInfoEditing ? (
            <Input placeholder="请输入应用英文名称" />
          ) : (
            <span className="detail-text">{appData.englishName}</span>
          )}
        </Form.Item>
        <Form.Item label="应用中文描述" name="chineseDesc" rules={basicInfoEditing ? [{ required: true, message: '请输入应用中文描述' }] : []}>
          {basicInfoEditing ? (
            <Input.TextArea placeholder="请输入应用中文描述" rows={3} />
          ) : (
            <span className="detail-text">{appData.chineseDesc}</span>
          )}
        </Form.Item>
        <Form.Item label="应用英文描述" name="englishDesc" rules={basicInfoEditing ? [{ required: true, message: '请输入应用英文描述' }] : []}>
          {basicInfoEditing ? (
            <Input.TextArea placeholder="请输入应用英文描述" rows={3} />
          ) : (
            <span className="detail-text">{appData.englishDesc}</span>
          )}
        </Form.Item>
        <Form.Item label="功能示意图" name="functionDiagram">
          {basicInfoEditing ? (
            <Upload showUploadList={false} beforeChange={() => false}>
              <Button>上传功能示意图</Button>
            </Upload>
          ) : (
            <span className="detail-text">{appData.functionDiagram || '暂无'}</span>
          )}
        </Form.Item>
      </Form>
    </div>

  );

  const renderAuthMethod = () => (
    <div className='formContent'>
      <Form form={authMethodForm} layout="horizontal">
        <Form.Item label="认证方式" name="authMethod" rules={authMethodEditing ? [{ required: true, message: '请选择认证方式' }] : []}>
          <Radio.Group options={authOptions} disabled={!authMethodEditing} />
        </Form.Item>
      </Form>
    </div>
  );

  return (
    <div className="basic-info">
      <div className="page-header">
        <div className="page-header-left">
          <h4 className="page-title">凭证和基础信息</h4>
          <span className="page-desc">管理应用凭证、安全配置和回调地址</span>
        </div>
      </div>

      <Card title="应用凭证" style={{ marginBottom: 16 }} className="info-card">
        {renderCredential()}
      </Card>

      <Card
        title="基础信息"
        style={{ marginBottom: 16 }}
        className="info-card"
        extra={
          !basicInfoEditing && (
            <Button type="link" onClick={() => setBasicInfoEditing(true)}>
              编辑
            </Button>
          )
        }
      >
        {renderBasicInfo()}
        {basicInfoEditing && (
          <div className="card-footer">
            <Button onClick={handleBasicInfoCancel}>取消</Button>
            <Button type="primary" onClick={handleBasicInfoSave}>保存</Button>
          </div>
        )}
      </Card>

      <Card
        title="认证方式"
        style={{ marginBottom: 16 }}
        className="info-card"
        extra={
          !authMethodEditing && (
            <Button type="link" onClick={() => setAuthMethodEditing(true)}>
              编辑
            </Button>
          )
        }
      >
        {renderAuthMethod()}
        {authMethodEditing && (
          <div className="card-footer">
            <Button onClick={handleAuthMethodCancel}>取消</Button>
            <Button type="primary" onClick={handleAuthMethodSave}>保存</Button>
          </div>
        )}
      </Card>
    </div>
  );
}

export default BasicInfo;