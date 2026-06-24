import React, { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Form, Input, Button, Card, Radio, Upload, message, InputNumber, Space, Tooltip } from 'antd';
import { EyeInvisibleOutlined, EyeOutlined, CopyOutlined, QuestionCircleOutlined } from '@ant-design/icons';
import { mockAppInfo } from './mock';
import { fetchCardSetting, updateCardPeriod } from './thunk';
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

  // 卡片设置 state
  const [cardSetting, setCardSetting] = useState({
    expirationDays: null,
    deletionDays: null,
  });
  const [cardSettingEditing, setCardSettingEditing] = useState(false);
  const [cardSettingDraft, setCardSettingDraft] = useState({
    expiration: null,
    deletion: null,
  });
  const [cardSettingRowSaving, setCardSettingRowSaving] = useState({
    expiration: false,
    deletion: false,
  });

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

  // 卡片设置：拉取真实 API（独立于现有 mock 逻辑）
  useEffect(() => {
    fetchCardSetting(appId).then((data) => {
      if (data) setCardSetting(data);
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

  // 卡片设置：字段约束配置
  const CARD_FIELD_CONSTRAINTS = {
    expiration: { min: 1, max: 7, periodType: 1 },
    deletion: { min: 1, max: 30, periodType: 0 },
  };

  const clampToEditable = (v, field) => {
    if (v == null) return null;
    const { min, max } = CARD_FIELD_CONSTRAINTS[field];
    return Math.max(min, Math.min(max, Math.round(v)));
  };

  const handleCardSettingEdit = () => {
    setCardSettingDraft({
      expiration: clampToEditable(cardSetting.expirationDays, 'expiration'),
      deletion: clampToEditable(cardSetting.deletionDays, 'deletion'),
    });
    setCardSettingEditing(true);
  };

  const handleCardSettingCancel = () => {
    setCardSettingEditing(false);
    setCardSettingDraft({ expiration: null, deletion: null });
    setCardSettingRowSaving({ expiration: false, deletion: false });
  };

  const handleCardSettingSaveRow = async (field) => {
    const constraint = CARD_FIELD_CONSTRAINTS[field];
    const draftValue = cardSettingDraft[field];
    if (draftValue == null || draftValue < constraint.min || draftValue > constraint.max) {
      return;
    }
    setCardSettingRowSaving({ ...cardSettingRowSaving, [field]: true });
    try {
      const result = await updateCardPeriod(appId, constraint.periodType, draftValue);
      if (result.success) {
        message.success('保存成功');
        const fresh = await fetchCardSetting(appId);
        if (fresh) setCardSetting(fresh);
        setCardSettingDraft({ ...cardSettingDraft, [field]: null });
        const otherField = field === 'expiration' ? 'deletion' : 'expiration';
        if (cardSettingDraft[otherField] == null) {
          setCardSettingEditing(false);
        }
      } else {
        message.error(result.message || '保存失败');
      }
    } finally {
      setCardSettingRowSaving({ ...cardSettingRowSaving, [field]: false });
    }
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

  const renderCardSetting = () => (
    <div className='formContent'>
      <Form layout="horizontal">
        <Form.Item label="定期失效时间">
          <Space>
            {cardSettingDraft.expiration != null ? (
              <InputNumber
                min={1}
                max={7}
                value={cardSettingDraft.expiration}
                onChange={(v) =>
                  setCardSettingDraft({ ...cardSettingDraft, expiration: v })
                }
                disabled={cardSettingRowSaving.expiration}
              />
            ) : (
              <span className="detail-text">
                {cardSetting.expirationDays != null ? `${cardSetting.expirationDays} 天` : '— 天'}
              </span>
            )}
            <Tooltip title="根据每张消息卡片第一次投放时间开始计算，系统按设置的时间自动对卡片进行失效，失效的卡片在端侧不再支持交互">
              <QuestionCircleOutlined style={{ color: 'rgba(0,0,0,0.45)', cursor: 'help' }} />
            </Tooltip>
            {cardSettingDraft.expiration != null && (
              <Button
                type="primary"
                size="small"
                loading={cardSettingRowSaving.expiration}
                onClick={() => handleCardSettingSaveRow('expiration')}
              >
                保存
              </Button>
            )}
          </Space>
        </Form.Item>
        <Form.Item label="定期删除时间">
          <Space>
            {cardSettingDraft.deletion != null ? (
              <InputNumber
                min={1}
                max={30}
                value={cardSettingDraft.deletion}
                onChange={(v) =>
                  setCardSettingDraft({ ...cardSettingDraft, deletion: v })
                }
                disabled={cardSettingRowSaving.deletion}
              />
            ) : (
              <span className="detail-text">
                {cardSetting.deletionDays != null ? `${cardSetting.deletionDays} 天` : '— 天'}
              </span>
            )}
            <Tooltip title="只有失效的卡片可以删除，根据每张消息卡片失效时间开始计算，系统按照设置的时间自动对卡片进行删除">
              <QuestionCircleOutlined style={{ color: 'rgba(0,0,0,0.45)', cursor: 'help' }} />
            </Tooltip>
            {cardSettingDraft.deletion != null && (
              <Button
                type="primary"
                size="small"
                loading={cardSettingRowSaving.deletion}
                onClick={() => handleCardSettingSaveRow('deletion')}
              >
                保存
              </Button>
            )}
          </Space>
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

      <Card
        title="卡片设置"
        style={{ marginBottom: 16 }}
        className="info-card"
        extra={
          !cardSettingEditing && (
            <Button type="link" onClick={handleCardSettingEdit}>
              编辑
            </Button>
          )
        }
      >
        {renderCardSetting()}
        {cardSettingEditing && (
          <div className="card-footer">
            <Button onClick={handleCardSettingCancel}>取消</Button>
          </div>
        )}
      </Card>
    </div>
  );
}

export default BasicInfo;