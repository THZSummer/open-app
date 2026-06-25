import React, { useState, useEffect, useRef } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { Card, Button, Form, Input, message, Spin, Tag, Upload, Modal, InputNumber, Space, Tooltip } from 'antd';
import { EyeInvisibleOutlined, EyeOutlined, CopyOutlined, EditOutlined, CloseOutlined, PlusOutlined, QuestionCircleOutlined } from '@ant-design/icons';
import AuthMethodCard from '../../components/AuthMethodCard/AuthMethodCard';
import BindEamapModal from '../../components/BindEamapModal/BindEamapModal';
import IconPicker from '../../components/IconPicker/IconPicker';
import { useAppDetail } from '../../contexts/AppContext';
import { fetchCurrentRole, updateApp, fetchAppIdentity, fetchVerifyType, updateVerifyType, bindEamap, fetchEamapList, uploadImage, fetchCardSetting, updateCardPeriod } from './thunk';
import { copyToClipboard, validateFile, validateImageDimensions } from '../../utils/common';
import { FILE_VALIDATION, VERIFY_TYPE_MAP } from '../../utils/constants';

import './BasicInfo.m.less';

/**
 * 凭证与基础信息页
 */
function BasicInfo() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { appDetail, reloadAppDetail } = useAppDetail();
  const appId = searchParams.get('appId');

  const [appData, setAppData] = useState(null);
  const [role, setRole] = useState(null);
  const [loading, setLoading] = useState(false);
  const [showSecret, setShowSecret] = useState(false);
  const [identity, setIdentity] = useState(null);
  const [verifyTypeData, setVerifyTypeData] = useState({ verifyType: [0], apiSecret: '' });
  const [basicInfoEditing, setBasicInfoEditing] = useState(false);
  const [authEditing, setAuthEditing] = useState(false);
  const [editForm] = Form.useForm();

  // 绑定 EAMAP 相关
  const [bindEamapModalVisible, setBindEamapModalVisible] = useState(false);
  const [eamapOptions, setEamapOptions] = useState([]);

  // 功能示意图相关
  const [diagram, setDiagram] = useState(null);
  // 编辑态图标选择器状态（fileId + 自定义上传 URL）
  const [iconId, setIconId] = useState('');
  const [iconUrl, setIconUrl] = useState('');

  // 示意图预览弹窗
  const [diagramPreviewVisible, setDiagramPreviewVisible] = useState(false);

  // 图标预览弹窗
  const [iconPreviewVisible, setIconPreviewVisible] = useState(false);

  // 查看示例图片弹窗
  const [exampleVisible, setExampleVisible] = useState(false);

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

  // 入口校验：先查角色，再从 Context 拿 detail 加载页面数据
  useEffect(() => {
    const checkAccess = async () => {
      if (!appId) {
        navigate('/');
        return;
      }
      const result = await fetchCurrentRole(appId);
      if (result?.code !== '200' || result.data?.role == null) {
        navigate('/');
        return;
      }
      setRole(result.data.role);
      // Context 会自动加载 appDetail，由下面的 useEffect 处理
    };
    checkAccess();
  }, [appId]);

  // 当 Context 的 appDetail 就绪时，填充页面数据 + 请求 identity/verify-type
  useEffect(() => {
    if (!appDetail) return;
    setAppData(appDetail);
    setDiagram(appDetail.diagramIdList?.[0] || null);
    const savedIconId = appDetail.icon?.fileId || appDetail.iconId || '';
    setIconId(savedIconId);
    setIconUrl(appDetail.icon?.url || '');
    editForm.setFieldsValue({
      nameCn: appDetail.nameCn,
      nameEn: appDetail.nameEn,
      descCn: appDetail.descCn,
      descEn: appDetail.descEn,
    });

    // 凭证：所有应用都查；认证方式：仅业务应用查
    const isPersonalApp = appDetail.appType === 0;
    const extraRequests = [fetchAppIdentity(appId)];
    if (!isPersonalApp) {
      extraRequests.push(fetchVerifyType(appId));
    }
    Promise.all(extraRequests).then((extraResults) => {
      if (extraResults[0]?.code === '200') {
        setIdentity(extraResults[0].data);
      }
      if (!isPersonalApp && extraResults[1]?.code === '200') {
        setVerifyTypeData(extraResults[1].data || { verifyType: [0], apiSecret: '' });
      }
    });
  }, [appDetail, appId]);

  // 卡片设置：独立加载（数据源为卡片服务，非 appDetail）
  useEffect(() => {
    if (!appId) return;
    fetchCardSetting(appId).then((res) => {
      if (res?.code === '200' || res?.code === 200) {
        setCardSetting(res.data || { expirationDays: null, deletionDays: null });
      }
    });
  }, [appId]);

  // 加载 EAMAP 列表
  const loadEamapOptions = async () => {
    const result = await fetchEamapList({ curPage: 1, pageSize: 100 });
    if (result?.code === '200') {
      setEamapOptions(result.data || []);
    }
  };

  const handleCopy = async (text) => {
    const success = await copyToClipboard(text);
    if (success) {
      message.success('已复制');
    } else {
      message.error('复制失败');
    }
  };

  // 编辑保存基本信息
  const handleBasicInfoSave = async () => {
    try {
      const values = await editForm.validateFields();
      const finalIconId = iconId || appData.icon?.fileId || '';
      if (!finalIconId) {
        message.warning('请选择应用图标');
        return;
      }
      const payload = {
        nameCn: values.nameCn,
        nameEn: values.nameEn,
        descCn: values.descCn || '',
        descEn: values.descEn || '',
        iconId: finalIconId,
        diagramIdList: diagram ? [diagram.fileId] : [],
      };
      const result = await updateApp(appId, payload);
      if (result?.code === '200') {
        message.success('保存成功');
        setBasicInfoEditing(false);
        reloadAppDetail();
      } else {
        message.error(result?.messageZh || '保存失败');
      }
    } catch (error) {
      message.error('保存失败');
    }
  };

  // 取消编辑
  const handleCancelEdit = () => {
    setBasicInfoEditing(false);
    if (appData) {
      setDiagram(appData.diagramIdList?.[0] || null);
      setIconId(appData.icon?.fileId || appData.iconId || '');
      setIconUrl(appData.icon?.url || '');
      editForm.setFieldsValue({
        nameCn: appData.nameCn,
        nameEn: appData.nameEn,
        descCn: appData.descCn,
        descEn: appData.descEn,
      });
    }
  };

  // 认证方式保存
  const handleAuthSave = async (types, secret) => {
    const result = await updateVerifyType(appId, { verifyType: types, apiSecret: secret });
    if (result?.code === '200') {
      message.success('认证方式保存成功');
      setAuthEditing(false);
      reloadAppDetail();
    } else {
      message.error(result?.messageZh || '保存失败');
    }
  };

  // 绑定 EAMAP
  const handleBindEamapOpen = () => {
    loadEamapOptions();
    setBindEamapModalVisible(true);
  };

  const handleBindEamapSubmit = async (eamapAppCode) => {
    const result = await bindEamap(appId, { eamapAppCode });
    if (result?.code === '200') {
      message.success('绑定成功，应用已升级为业务应用');
      setBindEamapModalVisible(false);
      reloadAppDetail();
    } else {
      message.error(result?.messageZh || '绑定失败');
    }
  };

  // 图标选择器回调：仅同步 iconId 和 iconUrl 到 appData
  const handleIconChange = (fileId, url) => {
    setIconId(fileId);
    setIconUrl(url);
  };

  // 功能示意图上传
  const handleDiagramUpload = async (file) => {
    const fileValidation = validateFile(file, FILE_VALIDATION.diagram);
    if (!fileValidation.valid) {
      message.error(fileValidation.message);
      return false;
    }
    const dimensionValidation = await validateImageDimensions(file, FILE_VALIDATION.diagram.width, FILE_VALIDATION.diagram.height);
    if (!dimensionValidation.valid) {
      message.error(dimensionValidation.message || FILE_VALIDATION.diagram.dimensionMessage);
      return false;
    }
    const formData = new FormData();
    formData.append('file', file);
    const result = await uploadImage(2, formData);
    if (result?.code === '200' && result.data) {
      setDiagram({ fileId: result.data.fileId, url: result.data.url });
      message.success('示意图上传成功');
    } else {
      message.error('示意图上传失败');
    }
    return false;
  };

  // 删除示意图
  const handleDeleteDiagram = () => {
    setDiagram(null);
  };

  // ========== 卡片设置 handlers ==========
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
      if (result?.code === '200' || result?.code === 200) {
        message.success('保存成功');
        const fresh = await fetchCardSetting(appId);
        if (fresh?.code === '200' || fresh?.code === 200) {
          setCardSetting(fresh.data || { expirationDays: null, deletionDays: null });
        }
        setCardSettingDraft({ ...cardSettingDraft, [field]: null });
        const otherField = field === 'expiration' ? 'deletion' : 'expiration';
        if (cardSettingDraft[otherField] == null) {
          setCardSettingEditing(false);
        }
      } else {
        message.error(result?.messageZh || '保存失败');
      }
    } finally {
      setCardSettingRowSaving({ ...cardSettingRowSaving, [field]: false });
    }
  };

  if (!appData) {
    return <Spin spinning={true} className="basic-info-loading" />;
  }

  const isLegacyPersonal = appData.appType === 0 && appData.appSubType === 0;
  const hasEamap = !!appData.eamapAppCode;

  return (
    <div className="basic-info-page">
      <Spin spinning={loading}>
        {/* 应用凭证 */}
        <Card title="应用凭证" className="info-card">
          <div className="credential-item">
            <span className="credential-label">APP ID</span>
            <span className="credential-value">{appData.appId}</span>
          </div>
          {identity && (
            <>
              <div className="credential-item">
                <span className="credential-label">APP Key</span>
                <span className="credential-value">{identity.ak}</span>
              </div>
              <div className="credential-item">
                <span className="credential-label">APP Secret</span>
                <span className="credential-value">
                  <span className="credential-text">
                    {showSecret ? identity.sk : '********'}
                  </span>
                  <span className="credential-actions">
                    <Button
                      type="link"
                      size="small"
                      icon={<CopyOutlined />}
                      onClick={() => handleCopy(identity.sk)}
                    />
                    <Button
                      type="link"
                      size="small"
                      icon={showSecret ? <EyeInvisibleOutlined /> : <EyeOutlined />}
                      onClick={() => setShowSecret(!showSecret)}
                    />
                  </span>
                </span>
              </div>
            </>
          )}
        </Card>

        {/* 基本信息 */}
        <Card
          className="info-card"
          title={
            <span className="card-title-with-action">
              基本信息
              {!basicInfoEditing && (
                <Button type="link" icon={<EditOutlined />} onClick={() => setBasicInfoEditing(true)}>
                  编辑
                </Button>
              )}
            </span>
          }
        >
          {basicInfoEditing ? (
            <Form form={editForm} layout="vertical" className="basic-info-edit-form">
              <div className="edit-form-row">
                <div className="edit-form-label">应用图标</div>
                <div className="edit-form-field">
                  <IconPicker
                    value={iconId}
                    uploadedUrl={iconUrl}
                    onChange={handleIconChange}
                  />
                </div>
              </div>

              <div className="edit-form-row">
                <div className="edit-form-label"><span className="edit-form-required">*</span> 中文名称</div>
                <div className="edit-form-field">
                  <Form.Item name="nameCn" rules={[{ required: true, message: '应用中文名不能为空' }, { max: 255, message: '不超过255字符' }]} style={{ marginBottom: 0 }}>
                    <Input maxLength={255} showCount />
                  </Form.Item>
                </div>
              </div>

              <div className="edit-form-row">
                <div className="edit-form-label"><span className="edit-form-required">*</span> 英文名称</div>
                <div className="edit-form-field">
                  <Form.Item name="nameEn" rules={[{ required: true, message: '应用英文名不能为空' }, { max: 255, message: '不超过255字符' }]} style={{ marginBottom: 0 }}>
                    <Input maxLength={255} showCount />
                  </Form.Item>
                </div>
              </div>

              <div className="edit-form-row">
                <div className="edit-form-label">中文描述</div>
                <div className="edit-form-field">
                  <Form.Item name="descCn" rules={[{ max: 2000, message: '描述不超过2000字符' }]} style={{ marginBottom: 0 }}>
                    <Input.TextArea rows={3} maxLength={2000} showCount />
                  </Form.Item>
                </div>
              </div>

              <div className="edit-form-row">
                <div className="edit-form-label">英文描述</div>
                <div className="edit-form-field">
                  <Form.Item name="descEn" rules={[{ max: 2000, message: '描述不超过2000字符' }]} style={{ marginBottom: 0 }}>
                    <Input.TextArea rows={3} maxLength={2000} showCount />
                  </Form.Item>
                </div>
              </div>

              <div className="edit-form-row">
                <div className="edit-form-label">功能示意图</div>
                <div className="edit-form-field">
                  <div className="diagram-list">
                    {diagram ? (
                      <div className="diagram-item has-image">
                        <img src={diagram.url} alt="功能示意图" className="diagram-thumb" />
                        <div className="diagram-overlay">
                          <span className="diagram-action" onClick={() => setDiagramPreviewVisible(true)} title="预览">
                            <EyeOutlined />
                          </span>
                          <span className="diagram-action" onClick={handleDeleteDiagram} title="删除">
                            <CloseOutlined />
                          </span>
                        </div>
                      </div>
                    ) : (
                      <Upload
                        showUploadList={false}
                        beforeUpload={() => false}
                        accept="image/png,image/jpeg,image/jpg"
                        onChange={({ file }) => handleDiagramUpload(file.originFileObj || file)}
                      >
                        <div className="diagram-add">
                          <PlusOutlined />
                          <span>添加示意图</span>
                        </div>
                      </Upload>
                    )}
                  </div>
                  <div className="diagram-tip">自定义上传：图片尺寸360px*200px, 推荐使用2倍图720px*400px,PNG、JPG、JEPG格式,小于500KB,<a className="diagram-example-link" onClick={() => setExampleVisible(true)}>查看示例</a></div>
                </div>
              </div>

              <div className="edit-form-row card-actions-row">
                <div className="edit-form-label"></div>
                <div className="edit-form-field">
                  <div className="card-actions">
                    <Button onClick={handleCancelEdit}>取消</Button>
                    <Button type="primary" onClick={handleBasicInfoSave}>保存</Button>
                  </div>
                </div>
              </div>
            </Form>
          ) : (
            <div className="basic-info-view">
              <div className="info-row">
                <span className="info-label">应用图标</span>
                <span className="info-value">
                  {appData.icon?.url ? (
                    <div className="info-icon-wrap">
                      <img src={appData.icon.url} alt="icon" className="info-icon-preview" />
                      <div className="info-icon-overlay" onClick={() => setIconPreviewVisible(true)} title="预览">
                        <EyeOutlined />
                      </div>
                    </div>
                  ) : '-'}
                </span>
              </div>
              <div className="info-row">
                <span className="info-label">中文名称</span>
                <span className="info-value">{appData.nameCn}</span>
              </div>
              <div className="info-row">
                <span className="info-label">英文名称</span>
                <span className="info-value">{appData.nameEn}</span>
              </div>
              <div className="info-row">
                <span className="info-label">中文描述</span>
                <span className="info-value">{appData.descCn || '-'}</span>
              </div>
              <div className="info-row">
                <span className="info-label">英文描述</span>
                <span className="info-value">{appData.descEn || '-'}</span>
              </div>
              {/* 功能示意图 — 始终展示，无图时显示 "-" */}
              <div className="info-row">
                <span className="info-label">功能示意图</span>
                <div className="info-value diagram-view-list">
                  {appData.diagramIdList && appData.diagramIdList.length > 0 ? (
                    <div className="diagram-view-item">
                      <img src={appData.diagramIdList[0].url} alt="功能示意图" className="diagram-view-thumb" />
                      <div className="diagram-overlay">
                        <span className="diagram-action" onClick={() => setDiagramPreviewVisible(true)} title="预览">
                          <EyeOutlined />
                        </span>
                      </div>
                    </div>
                  ) : (
                    '-'
                  )}
                </div>
              </div>
            </div>
          )}
        </Card>

        {/* 认证方式（个人应用不展示） */}
        {appData?.appType !== 0 && (
          <AuthMethodCard
            value={verifyTypeData.verifyType}
            apiSecret={verifyTypeData.apiSecret}
            editing={authEditing}
            onEdit={() => setAuthEditing(true)}
            onSave={handleAuthSave}
            onCancel={() => setAuthEditing(false)}
          />
        )}

        {/* 卡片设置 */}
        <Card
          className="info-card"
          title={
            <span className="card-title-with-action">
              卡片设置
              {!cardSettingEditing && (
                <Button type="link" icon={<EditOutlined />} onClick={handleCardSettingEdit}>
                  编辑
                </Button>
              )}
            </span>
          }
        >
          <div className="basic-info-view">
            <div className="info-row">
              <span className="info-label">
                定期失效时间
                <Tooltip title="根据每张消息卡片第一次投放时间开始计算，系统按设置的时间自动对卡片进行失效，失效的卡片在端侧不再支持交互">
                  <QuestionCircleOutlined style={{ marginLeft: 4, color: 'rgba(0,0,0,0.45)', cursor: 'help' }} />
                </Tooltip>
              </span>
              <span className="info-value">
                {cardSettingDraft.expiration != null ? (
                  <Space>
                    <InputNumber
                      min={1}
                      max={7}
                      value={cardSettingDraft.expiration}
                      onChange={(v) => setCardSettingDraft({ ...cardSettingDraft, expiration: v })}
                      disabled={cardSettingRowSaving.expiration}
                    />
                    <span>天</span>
                    <Button
                      type="primary"
                      size="small"
                      loading={cardSettingRowSaving.expiration}
                      onClick={() => handleCardSettingSaveRow('expiration')}
                    >
                      保存
                    </Button>
                  </Space>
                ) : (
                  <span>{cardSetting.expirationDays != null ? `${cardSetting.expirationDays} 天` : '— 天'}</span>
                )}
              </span>
            </div>
            <div className="info-row">
              <span className="info-label">
                定期删除时间
                <Tooltip title="只有失效的卡片可以删除，根据每张消息卡片失效时间开始计算，系统按照设置的时间自动对卡片进行删除">
                  <QuestionCircleOutlined style={{ marginLeft: 4, color: 'rgba(0,0,0,0.45)', cursor: 'help' }} />
                </Tooltip>
              </span>
              <span className="info-value">
                {cardSettingDraft.deletion != null ? (
                  <Space>
                    <InputNumber
                      min={1}
                      max={30}
                      value={cardSettingDraft.deletion}
                      onChange={(v) => setCardSettingDraft({ ...cardSettingDraft, deletion: v })}
                      disabled={cardSettingRowSaving.deletion}
                    />
                    <span>天</span>
                    <Button
                      type="primary"
                      size="small"
                      loading={cardSettingRowSaving.deletion}
                      onClick={() => handleCardSettingSaveRow('deletion')}
                    >
                      保存
                    </Button>
                  </Space>
                ) : (
                  <span>{cardSetting.deletionDays != null ? `${cardSetting.deletionDays} 天` : '— 天'}</span>
                )}
              </span>
            </div>
          </div>
          {cardSettingEditing && (
            <div className="edit-form-row card-actions-row">
              <div className="edit-form-label"></div>
              <div className="edit-form-field">
                <div className="card-actions">
                  <Button onClick={handleCardSettingCancel}>取消</Button>
                </div>
              </div>
            </div>
          )}
        </Card>

      </Spin>

      {/* 绑定 EAMAP Modal */}
      <BindEamapModal
        visible={bindEamapModalVisible}
        onCancel={() => setBindEamapModalVisible(false)}
        onOk={handleBindEamapSubmit}
        appId={appId}
        eamapOptions={eamapOptions}
      />

      {/* 功能示意图预览 */}
      <Modal
        title="功能示意图预览"
        open={diagramPreviewVisible}
        onCancel={() => setDiagramPreviewVisible(false)}
        footer={null}
        width={720}
      >
        <div style={{ textAlign: 'center', padding: '16px 0' }}>
          {(diagram || appData?.diagramIdList?.[0]) ? (
            <img src={(diagram || appData.diagramIdList[0]).url} alt="功能示意图" style={{ maxWidth: '100%', maxHeight: 500, objectFit: 'contain' }} />
          ) : (
            <span style={{ color: '#8f959e' }}>暂无示意图</span>
          )}
        </div>
      </Modal>

      {/* 查看功能示意图示例 */}
      <Modal
        title="功能示意图示例"
        open={exampleVisible}
        onCancel={() => setExampleVisible(false)}
        footer={null}
        width={520}
      >
        <div style={{ textAlign: 'center', padding: '16px 0' }}>
          <div style={{ background: '#f7f8fa', border: '1px dashed #d0d0d0', borderRadius: 8, padding: '40px 20px', color: '#8f959e', fontSize: 14 }}>
            <div style={{ marginBottom: 8, fontSize: 16, color: '#1f2329' }}>功能示意图示例</div>
            <div>建议尺寸：360×200px（推荐2倍图 720×400px）</div>
            <div style={{ marginTop: 8 }}>支持 PNG/JPG/JPEG 格式，不超过 500KB</div>
            <div style={{ marginTop: 16, background: '#e8e8e8', height: 120, borderRadius: 6, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <span style={{ color: '#bbb' }}>360 × 200</span>
            </div>
          </div>
        </div>
      </Modal>

      {/* 图标预览 */}
      <Modal
        title="应用图标预览"
        open={iconPreviewVisible}
        onCancel={() => setIconPreviewVisible(false)}
        footer={null}
        width={400}
      >
        <div style={{ textAlign: 'center', padding: '16px 0' }}>
          {appData?.icon?.url ? (
            <img src={appData.icon.url} alt="应用图标" style={{ maxWidth: '100%', maxHeight: 300, objectFit: 'contain' }} />
          ) : (
            <span style={{ color: '#8f959e' }}>暂无图标</span>
          )}
        </div>
      </Modal>
    </div>
  );
}

export default BasicInfo;
