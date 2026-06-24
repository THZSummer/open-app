/**
 * ========================================
 * 连接器管理 - 接口配置页面（版本化）
 * ========================================
 *
 * 功能：
 * - 版本选择 / 创建草稿 / 复制到草稿 / 失效 / 恢复 / 删除
 * - 默认只读，点击「编辑」进入编辑态，「保存」退出编辑态
 * - 发布前统一校验（接口/认证/入参/出参/层级）
 * - 多选认证（SOA / APIG / Cookie / 数字签名），行布局对齐 demo
 * - 入参 / 出参按 carrier 分 Tab 维护；入参隐藏 carrier 下拉
 */

import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Form,
  Input,
  Button,
  message,
  Radio,
  Select,
  Checkbox,
  Tag,
  Tabs,
  Empty,
} from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import {
  fetchVersionList,
  createDraftVersion,
  fetchVersionDetail,
  saveDraft,
  publishVersion,
  expireVersion,
  restoreVersion,
  deleteVersion,
  validateForPublish,
} from './thunk';
import SchemaEditor from '../../../components/SchemaEditor/SchemaEditorV2.jsx';
import DeleteConfirmModal from '../../../components/DeleteConfirmModal/DeleteConfirmModal';
import {
  DEFAULT_API_CONFIG,
  AUTH_SCHEMA_MAP,
  HTTP_METHOD_OPTIONS,
  AUTH_TYPE_OPTIONS,
  AUTH_TYPE_NAMES,
  AUTH_CARRIER_OPTIONS,
  COOKIE_FIELD_MAPPING_PLACEHOLDER,
  SIGNATURE_DEFAULT_FIXED_VALUE,
  REQUEST_SCHEMA_CONFIG,
  RESPONSE_SCHEMA_CONFIG,
  REQUEST_TABS,
  RESPONSE_TABS,
  VERSION_STATUS,
  VERSION_STATUS_MAP,
  COMMON_AUTH_TYPES,
  SIGNATURE_AUTH_TYPE,
  VALIDATE_SECTION,
  CONNECTOR_VERSION_EXPIRE_SECOND_MODAL_INFO,
  CONNECTOR_VERSION_DELETE_SECOND_MODAL_INFO,
} from './constants';
import { queryParams, getCurrentAppId, getSecondModalInfo } from '../../../utils/common';
import './ConnectorEditor.m.less';

const { Option } = Select;

/**
 * 连接器编辑页面组件
 */
const ConnectorEditor = () => {
  const navigate = useNavigate();
  const connectorId = queryParams('id');

  // antd Form 实例
  const [form] = Form.useForm();

  // 动作 loading（保存/发布/版本操作）
  const [actionLoading, setActionLoading] = useState(false);
  // 详情加载态
  const [detailLoading, setDetailLoading] = useState(!!connectorId);

  // 版本列表 & 当前选中版本
  const [versionList, setVersionList] = useState([]);
  const [currentVersion, setCurrentVersion] = useState(null);

  // 当前 API 配置（受控状态，驱动重渲染）
  const [apiConfig, setApiConfig] = useState(DEFAULT_API_CONFIG);

  // 是否处于编辑态（默认 false，进入后只读，点击「编辑」才可改）
  const [isEditing, setIsEditing] = useState(false);

  // 数字签名密钥掩码展示状态（独立于编辑态）
  const [signatureSecretMasked, setSignatureSecretMasked] = useState(true);

  // 二次确认弹窗（统一管理 expire / restore / delete 三种）
  const [confirmModal, setConfirmModal] = useState({ open: false, type: null });

  // 各 section 锚点，用于发布校验失败滚动定位
  const sectionRefs = {
    [VALIDATE_SECTION.BASE]: useRef(null),
    [VALIDATE_SECTION.AUTH]: useRef(null),
    [VALIDATE_SECTION.REQUEST]: useRef(null),
    [VALIDATE_SECTION.RESPONSE]: useRef(null),
  };

  // 版本状态便捷判断
  const isDraft = currentVersion?.status === VERSION_STATUS.DRAFT;
  const isPublished = currentVersion?.status === VERSION_STATUS.PUBLISHED;
  const isExpired = currentVersion?.status === VERSION_STATUS.EXPIRED;
  // 只有「草稿」且「已点编辑」时才可编辑
  const editable = isDraft && isEditing;

  /**
   * 初始化：加载版本列表
   */
  useEffect(() => {
    if (connectorId) {
      loadVersions({ keepCurrent: false });
    }
  }, [connectorId]);

  /**
   * 加载版本列表
   * @param {Object} options
   * options.keepCurrent 是否保留当前选中版本
   * options.preferVersionId 优先选中指定版本
   */
  const loadVersions = async (options) => {
    // options.keepCurrent / options.preferVersionId
    const { keepCurrent = false, preferVersionId = null } = options || {};

    setDetailLoading(true);
    const res = await fetchVersionList(connectorId);
    if (res?.code !== '200') {
      message.error(res?.messageZh || '加载版本列表失败');
      setDetailLoading(false);
      return;
    }

    const list = res.data || [];
    setVersionList(list);

    if (list.length === 0) {
      // 无版本：清空当前版本
      setCurrentVersion(null);
      setApiConfig(DEFAULT_API_CONFIG);
      form.setFieldValue('apiConfig', DEFAULT_API_CONFIG);
      setIsEditing(false);
      setDetailLoading(false);
      return;
    }

    // 选择目标版本
    let target = null;
    if (preferVersionId) {
      target = list.find(v => v.versionId === preferVersionId);
    } else if (keepCurrent && currentVersion) {
      target = list.find(v => v.versionId === currentVersion.versionId);
    }
    if (!target) target = list[0];

    await loadVersionDetail(target.versionId);
    setDetailLoading(false);
  };

  /**
   * 加载版本详情
   * @param {string} versionId 版本 ID
   */
  const loadVersionDetail = async (versionId) => {
    const res = await fetchVersionDetail({ connectorId, versionId });
    if (res?.code !== '200') {
      message.error(res?.messageZh || '加载版本详情失败');
      return;
    }
    const detail = res.data;
    setCurrentVersion({
      versionId: detail.versionId,
      status: detail.status,
      createTime: detail.createTime,
      name: detail.name,
    });
    // 兼容历史配置缺失字段
    const merged = { ...DEFAULT_API_CONFIG, ...(detail.config || {}) };
    setApiConfig(merged);
    form.setFieldValue('apiConfig', merged);
    // 切版本必退出编辑态
    setIsEditing(false);
  };

  /**
   * 切换版本
   * @param {string} versionId 选中的版本 ID
   */
  const handleVersionChange = (versionId) => {
    if (versionId === currentVersion?.versionId) return;
    loadVersionDetail(versionId);
  };

  /**
   * 通用配置字段更新
   * @param {string} field 字段名
   * @param {any} value 字段值
   */
  const updateConfig = (field, value) => {
    const current = form.getFieldValue('apiConfig') || {};
    const next = { ...current, [field]: value };
    setApiConfig(next);
    form.setFieldValue('apiConfig', next);
  };

  /**
   * 处理认证多选变化
   * 勾选时初始化默认参数；取消时清除对应参数
   * @param {Array} newAuthTypes 新的认证方式数组
   */
  const handleAuthTypeChange = (newAuthTypes) => {
    const current = form.getFieldValue('apiConfig') || {};
    const oldAuthRequestSchema = current.authRequestSchema || {};
    const nextAuthRequestSchema = {};

    // 仅保留仍勾选的通用认证参数
    COMMON_AUTH_TYPES.forEach(t => {
      if (newAuthTypes.includes(t)) {
        nextAuthRequestSchema[t] = oldAuthRequestSchema[t] || JSON.parse(JSON.stringify(AUTH_SCHEMA_MAP[t] || []));
      }
    });

    const next = {
      ...current,
      authType: newAuthTypes,
      authRequestSchema: nextAuthRequestSchema,
    };
    setApiConfig(next);
    form.setFieldValue('apiConfig', next);
  };

  /**
   * 更新指定认证方式的单个参数字段
   * @param {Object} options
   * options.authType 认证方式标识
   * options.index 参数索引
   * options.field 字段名
   * options.value 字段值
   */
  const updateAuthParam = (options) => {
    // options.authType / options.index / options.field / options.value
    const { authType, index, field, value } = options;
    const current = form.getFieldValue('apiConfig') || {};
    const oldItems = (current.authRequestSchema || {})[authType] || [];
    const nextItems = oldItems.map((item, i) => (i === index ? { ...item, [field]: value } : item));
    const nextSchema = { ...(current.authRequestSchema || {}), [authType]: nextItems };
    const next = { ...current, authRequestSchema: nextSchema };
    setApiConfig(next);
    form.setFieldValue('apiConfig', next);
  };

  /**
   * 更新数字签名配置（独立结构）
   * @param {string} field 字段名（paramName / carrier / fixedValue / secret）
   * @param {string} value 字段值
   */
  const handleSignatureChange = (field, value) => {
    const current = form.getFieldValue('apiConfig') || {};
    const nextSig = { ...(current.signatureConfig || {}), [field]: value };
    const next = { ...current, signatureConfig: nextSig };
    setApiConfig(next);
    form.setFieldValue('apiConfig', next);
  };

  /**
   * 切换密钥掩码显示
   */
  const toggleSecretMask = () => {
    setSignatureSecretMasked(prev => !prev);
  };

  /**
   * 滚动到指定 section
   * @param {string} sectionKey section 标识
   */
  const scrollToSection = (sectionKey) => {
    const ref = sectionRefs[sectionKey];
    if (ref && ref.current) {
      ref.current.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  };

  /**
   * 进入编辑态
   */
  const handleEnterEdit = () => {
    if (!isDraft) return;
    setIsEditing(true);
  };

  /**
   * 保存（保存草稿后退出编辑态，不做发布校验）
   */
  const handleSave = async () => {
    if (!currentVersion?.versionId) return;
    setActionLoading(true);
    const res = await saveDraft({
      connectorId,
      versionId: currentVersion.versionId,
      config: form.getFieldValue('apiConfig') || apiConfig,
    });
    if (res?.code === '200') {
      message.success('保存成功');
      setIsEditing(false);
    } else {
      message.error(res?.messageZh || '保存失败');
    }
    setActionLoading(false);
  };

  /**
   * 发布草稿（前置校验）
   */
  const handlePublish = async () => {
    if (!currentVersion?.versionId) return;
    const cfg = form.getFieldValue('apiConfig') || apiConfig;

    const err = validateForPublish(cfg);
    if (err) {
      message.error(err.message);
      scrollToSection(err.section);
      return;
    }

    setActionLoading(true);
    const res = await publishVersion({
      connectorId,
      versionId: currentVersion.versionId,
      config: cfg,
    });
    if (res?.code === '200') {
      message.success('发布成功');
      setIsEditing(false);
      await loadVersions({ preferVersionId: currentVersion.versionId });
    } else {
      message.error(res?.messageZh || '发布失败');
    }
    setActionLoading(false);
  };

  /**
   * 创建草稿 / 复制到草稿
   */
  const handleCreateDraft = async () => {
    setActionLoading(true);
    const res = await createDraftVersion({
      connectorId,
      baseVersionId: currentVersion?.versionId || null,
    });
    if (res?.code === '200') {
      message.success('已创建草稿');
      await loadVersions({ preferVersionId: res.data?.versionId });
    } else {
      message.error(res?.messageZh || '创建失败');
    }
    setActionLoading(false);
  };

  /**
   * 失效当前版本
   */
  const doExpire = async () => {
    setActionLoading(true);
    const res = await expireVersion({
      connectorId,
      versionId: currentVersion.versionId,
    });
    if (res?.code === '200') {
      message.success('已失效');
      await loadVersions({ preferVersionId: currentVersion.versionId });
    } else {
      message.error(res?.messageZh || '失效失败');
    }
    setActionLoading(false);
    setConfirmModal({ open: false, type: null });
  };

  /**
   * 恢复当前版本（已失效 -> 已发布）
   */
  const doRestore = async () => {
    setActionLoading(true);
    const res = await restoreVersion({
      connectorId,
      versionId: currentVersion.versionId,
    });
    if (res?.code === '200') {
      message.success('已恢复');
      await loadVersions({ preferVersionId: currentVersion.versionId });
    } else {
      message.error(res?.messageZh || '恢复失败');
    }
    setActionLoading(false);
    setConfirmModal({ open: false, type: null });
  };

  /**
   * 删除当前版本（草稿 / 已失效）
   */
  const doDelete = async () => {
    setActionLoading(true);
    const res = await deleteVersion({
      connectorId,
      versionId: currentVersion.versionId,
    });
    if (res?.code === '200') {
      message.success('已删除');
      await loadVersions({ keepCurrent: false });
    } else {
      message.error(res?.messageZh || '删除失败');
    }
    setActionLoading(false);
    setConfirmModal({ open: false, type: null });
  };

  /**
   * 渲染版本栏右侧操作按钮（按版本状态显隐）
   */
  const renderVersionActions = () => {
    // 无版本：仅展示"创建草稿"
    if (!currentVersion) {
      return (
        <Button
          type="primary"
          icon={<PlusOutlined />}
          className="primary-btn"
          loading={actionLoading}
          onClick={handleCreateDraft}
        >
          创建草稿
        </Button>
      );
    }

    return (
      <>
        {isDraft && (
          <>
            {/* 编辑 / 保存 切换 */}
            {!isEditing ? (
              <Button
                type="primary"
                className="primary-btn"
                onClick={handleEnterEdit}
              >
                编辑
              </Button>
            ) : (
              <Button
                type="primary"
                className="primary-btn"
                loading={actionLoading}
                onClick={handleSave}
              >
                保存
              </Button>
            )}
            <Button loading={actionLoading} onClick={handlePublish}>
              发布
            </Button>
            <Button
              className="danger-btn"
              onClick={() => setConfirmModal({ open: true, type: 'delete' })}
            >
              删除
            </Button>
          </>
        )}

        {isPublished && (
          <>
            <Button onClick={handleCreateDraft} loading={actionLoading}>
              复制到草稿
            </Button>
            <Button
              className="danger-btn"
              onClick={() => setConfirmModal({ open: true, type: 'expire' })}
            >
              失效
            </Button>
          </>
        )}

        {isExpired && (
          <>
            <Button onClick={handleCreateDraft} loading={actionLoading}>
              复制到草稿
            </Button>
            <Button
              type="primary"
              className="primary-btn"
              onClick={() => setConfirmModal({ open: true, type: 'restore' })}
            >
              恢复
            </Button>
            <Button
              className="danger-btn"
              onClick={() => setConfirmModal({ open: true, type: 'delete' })}
            >
              删除
            </Button>
          </>
        )}
      </>
    );
  };

  /**
   * 渲染版本选择条（左侧版本下拉 + 右侧操作按钮）
   */
  const renderVersionBar = () => (
    <div className="version-bar">
      {/* 左侧：版本下拉或空提示 */}
      <div className="version-bar-left">
        <span className="version-bar-label">当前版本</span>
        {versionList.length === 0 ? (
          <span className="version-empty">暂无版本，点击右侧"创建草稿"开始配置</span>
        ) : (
          <Select
            className="version-select"
            value={currentVersion?.versionId}
            onChange={handleVersionChange}
            disabled={actionLoading || detailLoading}
          >
            {versionList.map(v => (
              <Option key={v.versionId} value={v.versionId} label={v.name}>
                <div className="version-option">
                  <span className="version-option-name">{v.name}</span>
                  <span className="version-option-time">{v.createTime}</span>
                  <Tag color={VERSION_STATUS_MAP[v.status]?.color}>
                    {VERSION_STATUS_MAP[v.status]?.text}
                  </Tag>
                </div>
              </Option>
            ))}
          </Select>
        )}
      </div>

      {/* 右侧：操作按钮组 */}
      <div className="version-bar-actions">
        {renderVersionActions()}
      </div>
    </div>
  );

  /**
   * 渲染接口基础配置
   */
  const renderBaseSection = () => (
    <div className="section-card" ref={sectionRefs[VALIDATE_SECTION.BASE]}>
      <div className="section-title">
        接口配置
        <span className="section-tip">配置接口协议与地址</span>
      </div>
      <Form.Item label="协议类型" className="form-item-spacing">
        <Radio.Group
          value={apiConfig.protocolType}
          disabled={!editable}
          onChange={(e) => updateConfig('protocolType', e.target.value)}
        >
          {HTTP_METHOD_OPTIONS.map(method => (
            <Radio.Button key={method} value={method}>
              {method}
            </Radio.Button>
          ))}
        </Radio.Group>
      </Form.Item>

      <Form.Item label="协议地址" className="form-item-no-spacing">
        <Input
          value={apiConfig.protocolAddress}
          disabled={!editable}
          onChange={(e) => updateConfig('protocolAddress', e.target.value)}
          placeholder="https://api.example.com/endpoint"
          className="input-border-radius"
        />
      </Form.Item>
    </div>
  );

  /**
   * 渲染单条通用认证参数行（SOA / APIG / Cookie）
   * @param {Object} options
   * options.authType 认证方式标识
   * options.item 参数对象
   * options.index 参数索引
   */
  const renderCommonAuthRow = (options) => {
    // options.authType / options.item / options.index
    const { authType, item, index } = options;
    // 值来源/字段映射列：Cookie 默认空 + 占位文案；SOA/APIG 与参数名一致，仅展示
    const isCookie = authType === 'Cookie';
    const valueColumnValue = isCookie ? '' : (item.paramName || '');
    const valueColumnPlaceholder = isCookie ? COOKIE_FIELD_MAPPING_PLACEHOLDER : '值来源';

    return (
      <div className="auth-param-row" key={`${authType}-${index}`}>
        <Input
          className="auth-field-name input-border-radius"
          value={item.paramName}
          placeholder="参数名称"
          disabled={!editable}
          onChange={(e) => updateAuthParam({ authType, index, field: 'paramName', value: e.target.value })}
        />
        <Select className="auth-field-type" value="string" disabled>
          <Option value="string">string</Option>
        </Select>
        <Select
          className="auth-field-carrier"
          value={item.carrier || 'header'}
          disabled={!editable}
          onChange={(val) => updateAuthParam({ authType, index, field: 'carrier', value: val })}
        >
          {AUTH_CARRIER_OPTIONS.map(opt => (
            <Option key={opt} value={opt}>{opt}</Option>
          ))}
        </Select>
        <Input
          className="auth-field-value input-border-radius"
          value={valueColumnValue}
          placeholder={valueColumnPlaceholder}
          disabled
        />
      </div>
    );
  };

  /**
   * 渲染通用认证子区块
   * @param {string} authType 认证方式标识
   */
  const renderCommonAuthBlock = (authType) => {
    const items = (apiConfig.authRequestSchema || {})[authType] || [];
    return (
      <div className="auth-sub-section" key={authType}>
        <div className="auth-sub-title">
          <span className="auth-sub-tag">{authType}</span>
          {AUTH_TYPE_NAMES[authType]}
        </div>
        {items.length === 0 ? (
          <div className="placeholder-text">暂无参数</div>
        ) : (
          items.map((item, index) => renderCommonAuthRow({ authType, item, index }))
        )}
      </div>
    );
  };

  /**
   * 渲染数字签名子区块（单行：参数名 + string + carrier + 固定值 + 密钥 + 显隐）
   */
  const renderSignatureBlock = () => {
    const sig = apiConfig.signatureConfig || {};
    return (
      <div className="auth-sub-section" key="SIGNATURE">
        <div className="auth-sub-title">
          <span className="auth-sub-tag">数字签名</span>
          {AUTH_TYPE_NAMES[SIGNATURE_AUTH_TYPE]}
        </div>

        <div className="auth-param-row">
          <Input
            className="auth-field-name input-border-radius"
            value={sig.paramName}
            placeholder="参数名称"
            disabled={!editable}
            onChange={(e) => handleSignatureChange('paramName', e.target.value)}
          />
          <Select className="auth-field-type" value="string" disabled>
            <Option value="string">string</Option>
          </Select>
          <Select
            className="auth-field-carrier"
            value={sig.carrier || 'header'}
            disabled={!editable}
            onChange={(val) => handleSignatureChange('carrier', val)}
          >
            {AUTH_CARRIER_OPTIONS.map(opt => (
              <Option key={opt} value={opt}>{opt}</Option>
            ))}
          </Select>
          <Input
            className="auth-field-value input-border-radius"
            value={sig.fixedValue || SIGNATURE_DEFAULT_FIXED_VALUE}
            placeholder="签名固定值"
            disabled
          />
          <Input
            className="auth-field-value input-border-radius"
            type={signatureSecretMasked ? 'password' : 'text'}
            value={sig.secret}
            placeholder="签名密钥"
            disabled={!editable}
            onChange={(e) => handleSignatureChange('secret', e.target.value)}
          />
          <Button className="signature-mask-btn" onClick={toggleSecretMask}>
            {signatureSecretMasked ? '显示' : '隐藏'}
          </Button>
        </div>
      </div>
    );
  };

  /**
   * 渲染认证方式配置
   */
  const renderAuthSection = () => {
    const authTypes = apiConfig.authType || [];
    return (
      <div className="section-card" ref={sectionRefs[VALIDATE_SECTION.AUTH]}>
        <div className="section-title">
          认证方式配置
          <span className="section-tip">支持多选，勾选后可分别维护参数</span>
        </div>

        <Form.Item label="认证方式（可多选）" className="form-item-spacing">
          <Checkbox.Group
            value={authTypes}
            disabled={!editable}
            onChange={handleAuthTypeChange}
          >
            {AUTH_TYPE_OPTIONS.map(opt => (
              <Checkbox key={opt.value} value={opt.value}>
                {opt.label}
              </Checkbox>
            ))}
          </Checkbox.Group>
        </Form.Item>

        {authTypes.length === 0 ? (
          <div className="placeholder-text">请先选择认证方式</div>
        ) : (
          <>
            {COMMON_AUTH_TYPES.filter(t => authTypes.includes(t)).map(renderCommonAuthBlock)}
            {authTypes.includes(SIGNATURE_AUTH_TYPE) && renderSignatureBlock()}
          </>
        )}
      </div>
    );
  };

  /**
   * 渲染入参 / 出参 Tab 化区块
   * @param {Object} options
   * options.title 标题
   * options.tip 标题旁说明
   * options.tabs Tab 配置数组
   * options.schemaConfig 传给 SchemaEditor 的配置
   * options.sectionKey 区域标识（用于滚动定位）
   * options.hideCarrier 是否隐藏 carrier 下拉（入参 Tab 隐藏）
   */
  const renderSchemaTabsSection = (options) => {
    // options.title / options.tip / options.tabs / options.schemaConfig / options.sectionKey / options.hideCarrier
    const { title, tip, tabs, schemaConfig, sectionKey, hideCarrier } = options;

    const tabItems = tabs.map(tab => ({
      key: tab.key,
      label: tab.label,
      children: (
        <SchemaEditor
          form={form}
          apiConfig={apiConfig}
          {...schemaConfig}
          editable={editable}
          carrierFilter={tab.carrier}
          hideCarrier={hideCarrier}
        />
      ),
    }));

    return (
      <div className="section-card" ref={sectionRefs[sectionKey]}>
        <div className="section-title">
          {title}
          <span className="section-tip">{tip}</span>
        </div>
        <Tabs defaultActiveKey={tabs[0].key} items={tabItems} />
      </div>
    );
  };

  /**
   * 拼接版本对象名
   * 优先 "v{versionNo} ({versionName})"，缺失则回退 versionId
   */
  const getVersionObjectName = () => {
    // 当前选中版本
    const version = currentVersion;
    if (!version) return '';

    // 版本号字段（兼容多种命名）
    const versionNo = version.versionNo || version.versionNumber;
    // 版本名称字段（兼容多种命名）
    const versionName = version.versionName || version.name;

    if (versionNo && versionName) {
      return `v${versionNo} (${versionName})`;
    }
    if (versionNo) {
      return `v${versionNo}`;
    }
    if (versionName) {
      return versionName;
    }
    return version.versionId || '';
  };

  /**
   * 二次确认弹窗内容
   * - 失效 / 删除：走新版 getSecondModalInfo 范式，包含确认文案 + 操作影响双段
   * - 恢复：保持原样（非破坏性操作）
   */
  const getConfirmModalInfo = () => {
    if (confirmModal.type === 'expire') {
      // 失效版本：使用版本对象名拼接确认文案
      return {
        ...getSecondModalInfo({
          ...CONNECTOR_VERSION_EXPIRE_SECOND_MODAL_INFO,
          objectName: getVersionObjectName(),
        }),
        onConfirm: doExpire,
      };
    }
    if (confirmModal.type === 'restore') {
      // 恢复版本：保持原有交互（非破坏性操作，不在本次整改范围）
      return {
        title: '确认恢复',
        content: `恢复后该版本将重新变为已发布版本，原已发布版本会被设为已失效。`,
        confirmButtonText: '确认恢复',
        loadingText: '处理中...',
        dangerColor: '#1a7f1a',
        onConfirm: doRestore,
      };
    }
    // 删除版本：使用版本对象名拼接确认文案
    return {
      ...getSecondModalInfo({
        ...CONNECTOR_VERSION_DELETE_SECOND_MODAL_INFO,
        objectName: getVersionObjectName(),
      }),
      onConfirm: doDelete,
    };
  };

  const confirmInfo = getConfirmModalInfo();

  return (
    <div
      className={[
        'connector-editor-page',
        detailLoading ? 'loading' : '',
        editable ? '' : 'is-readonly',
      ].filter(Boolean).join(' ')}
    >
      {/* 顶部标题 + 版本条（按钮已挪入版本条右侧） */}
      <div className="content-card">
        {/* 面包屑：连接器列表 / 接口配置（参考版本详情页样式） */}
        <div className="page-breadcrumb">
          {/* 可点击：跳转到连接器列表（携带 appId 避免列表页拿不到数据） */}
          <span className="page-breadcrumb-link" onClick={() => navigate(`/connect/connectors?appId=${getCurrentAppId()}`)}>连接器列表</span>
          <span className="page-breadcrumb-sep">&gt;</span>
          {/* 当前详情页：纯文本，不可点击 */}
          <span>接口配置</span>
        </div>

        <div className="page-header">
          <div>
            <h2>接口配置</h2>
            <p className="page-desc">维护连接器接口契约。默认只读，草稿点击"编辑"后可修改。</p>
          </div>
        </div>

        {/* 版本选择条：始终渲染（空版本展示提示文案） */}
        {renderVersionBar()}
      </div>

      {/* 仅在有当前版本时展示主体表单 */}
      {currentVersion ? (
        <Form form={form} layout="vertical">
          {renderBaseSection()}
          {renderAuthSection()}
          {renderSchemaTabsSection({
            title: '入参配置',
            tip: '按 HTTP 请求头 / 请求体 / 查询参数分区维护',
            tabs: REQUEST_TABS,
            schemaConfig: REQUEST_SCHEMA_CONFIG,
            sectionKey: VALIDATE_SECTION.REQUEST,
            hideCarrier: true,
          })}
          {renderSchemaTabsSection({
            title: '出参配置',
            tip: '按 HTTP 响应头 / 响应体分区维护',
            tabs: RESPONSE_TABS,
            schemaConfig: RESPONSE_SCHEMA_CONFIG,
            sectionKey: VALIDATE_SECTION.RESPONSE,
            hideCarrier: false,
          })}
        </Form>
      ) : (
        !detailLoading && (
          <div className="content-card">
            <Empty description="暂无版本，请先创建草稿" />
          </div>
        )
      )}

      {/* 二次确认弹窗（失效 / 恢复 / 删除复用） */}
      <DeleteConfirmModal
        open={confirmModal.open}
        onClose={() => setConfirmModal({ open: false, type: null })}
        onConfirm={confirmInfo.onConfirm}
        modalInfo={{
          title: confirmInfo.title,
          content: confirmInfo.content,
          confirmButtonText: confirmInfo.confirmButtonText,
          loadingText: confirmInfo.loadingText,
          dangerColor: confirmInfo.dangerColor,
        }}
      />
    </div>
  );
};

export default ConnectorEditor;
