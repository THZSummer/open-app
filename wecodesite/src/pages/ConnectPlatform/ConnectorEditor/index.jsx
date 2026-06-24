/**
 * ========================================
 * 连接器管理 - 接口配置页面（版本化）
 * ========================================
 *
 * 仅负责：
 * - 跨组件共享状态（版本列表 / 当前版本 / apiConfig / 编辑态 / 详情加载态）
 * - 版本数据加载编排（loadVersions / loadVersionDetail）
 * - 子组件组合与发布校验滚动定位
 *
 * 子组件职责：
 * - PageHeader：面包屑 + 标题
 * - VersionBar：版本下拉 + 版本动作 + 二次确认弹窗
 * - BaseSection：协议类型 / 协议地址
 * - AuthSection：认证多选 + 通用认证 + 数字签名
 * - SchemaTabsSection：入参 / 出参 Tab 化 SchemaEditor
 */

import React, { useState, useEffect, useRef } from 'react';
import { Form, Empty, Button, message } from 'antd';
import { fetchVersionList, fetchVersionDetail, createDraftVersion } from './thunk';
import {
  DEFAULT_API_CONFIG,
  REQUEST_SCHEMA_CONFIG,
  RESPONSE_SCHEMA_CONFIG,
  REQUEST_TABS,
  RESPONSE_TABS,
  VERSION_STATUS,
  VALIDATE_SECTION,
} from './constants';
import { queryParams } from '../../../utils/common';
import PageHeader from './components/PageHeader';
import VersionBar from './components/VersionBar';
import BaseSection from './components/BaseSection';
import AuthSection from './components/AuthSection';
import SchemaTabsSection from './components/SchemaTabsSection';
import './ConnectorEditor.m.less';

/**
 * 连接器编辑页面组件
 */
const ConnectorEditor = () => {
  // 当前连接器 ID（路由参数）
  const connectorId = queryParams('id');

  // antd Form 实例
  const [form] = Form.useForm();

  // 详情加载态
  const [detailLoading, setDetailLoading] = useState(!!connectorId);

  // 版本列表 & 当前选中版本
  const [versionList, setVersionList] = useState([]);
  const [currentVersion, setCurrentVersion] = useState(null);

  // 当前 API 配置（受控状态，驱动重渲染）
  const [apiConfig, setApiConfig] = useState(DEFAULT_API_CONFIG);

  // 是否处于编辑态（默认 false，只读，点击「编辑」才可改）
  const [isEditing, setIsEditing] = useState(false);

  // 空态下"创建草稿"按钮 loading
  const [creatingDraft, setCreatingDraft] = useState(false);

  // 各 section 锚点，用于发布校验失败滚动定位
  const sectionRefs = {
    [VALIDATE_SECTION.BASE]: useRef(null),
    [VALIDATE_SECTION.AUTH]: useRef(null),
    [VALIDATE_SECTION.REQUEST]: useRef(null),
    [VALIDATE_SECTION.RESPONSE]: useRef(null),
  };

  // 仅在「草稿」且「已点编辑」时可编辑
  const isDraft = currentVersion?.status === VERSION_STATUS.DRAFT;
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
    // 解构所需参数
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
   * 统一回写 apiConfig（子组件唯一的写入入口）
   * @param {Object} nextApiConfig 新的 apiConfig
   */
  const handleApiConfigChange = (nextApiConfig) => {
    setApiConfig(nextApiConfig);
    form.setFieldValue('apiConfig', nextApiConfig);
  };

  /**
   * 滚动到指定 section（由 VersionBar 在发布校验失败时调用）
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
   * 退出编辑态（VersionBar 在保存 / 发布成功后调用）
   */
  const handleExitEdit = () => {
    setIsEditing(false);
  };

  /**
   * 空态下创建草稿
   * 当前连接器无任何版本时，由空态区按钮触发
   */
  const handleCreateDraftFromEmpty = async () => {
    setCreatingDraft(true);
    const res = await createDraftVersion({
      connectorId,
      baseVersionId: null,
    });
    if (res?.code === '200') {
      message.success('已创建草稿');
      await loadVersions({ preferVersionId: res.data?.versionId });
    } else {
      message.error(res?.messageZh || '创建失败');
    }
    setCreatingDraft(false);
  };

  return (
    <div
      className={[
        'connector-editor-page',
        detailLoading ? 'loading' : '',
        editable ? '' : 'is-readonly',
      ].filter(Boolean).join(' ')}
    >
      {/* 顶部标题 + 版本条 */}
      <div className="content-card">
        {/* 面包屑 + 页面标题 */}
        <PageHeader />

        {/* 版本选择条：内含版本动作与二次确认弹窗 */}
        <VersionBar
          form={form}
          connectorId={connectorId}
          versionList={versionList}
          currentVersion={currentVersion}
          apiConfig={apiConfig}
          isEditing={isEditing}
          detailLoading={detailLoading}
          onVersionChange={handleVersionChange}
          onEnterEdit={handleEnterEdit}
          onExitEdit={handleExitEdit}
          onReloadVersions={loadVersions}
          onScrollToSection={scrollToSection}
        />
      </div>

      {/* 仅在有当前版本时展示主体表单 */}
      {currentVersion ? (
        <Form form={form} layout="vertical">
          {/* 接口基础配置 */}
          <BaseSection
            ref={sectionRefs[VALIDATE_SECTION.BASE]}
            form={form}
            apiConfig={apiConfig}
            editable={editable}
            onApiConfigChange={handleApiConfigChange}
          />

          {/* 认证方式配置 */}
          <AuthSection
            ref={sectionRefs[VALIDATE_SECTION.AUTH]}
            form={form}
            apiConfig={apiConfig}
            editable={editable}
            onApiConfigChange={handleApiConfigChange}
          />

          {/* 入参配置 */}
          <SchemaTabsSection
            ref={sectionRefs[VALIDATE_SECTION.REQUEST]}
            title="入参配置"
            tip="按 HTTP 请求头 / 请求体 / 查询参数分区维护"
            tabs={REQUEST_TABS}
            schemaConfig={REQUEST_SCHEMA_CONFIG}
            hideCarrier
            form={form}
            apiConfig={apiConfig}
            editable={editable}
          />

          {/* 出参配置 */}
          <SchemaTabsSection
            ref={sectionRefs[VALIDATE_SECTION.RESPONSE]}
            title="出参配置"
            tip="按 HTTP 响应头 / 响应体分区维护"
            tabs={RESPONSE_TABS}
            schemaConfig={RESPONSE_SCHEMA_CONFIG}
            hideCarrier
            form={form}
            apiConfig={apiConfig}
            editable={editable}
          />
        </Form>
      ) : (
        !detailLoading && (
          <div className="content-card empty-card">
            <Empty description="暂无版本，请先创建草稿">
              {/* 空态下创建草稿入口，点击新增一个草稿版本 */}
              <Button
                type="primary"
                className="primary-btn"
                loading={creatingDraft}
                onClick={handleCreateDraftFromEmpty}
              >
                创建草稿
              </Button>
            </Empty>
          </div>
        )
      )}
    </div>
  );
};

export default ConnectorEditor;
