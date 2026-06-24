import React, { useState, useEffect, useCallback } from 'react';
import { useSearchParams, useNavigate, useLocation } from 'react-router-dom';
import { Table, Tag, Button, Form, Input, message, Spin, Pagination } from 'antd';
import { PlusOutlined, EyeOutlined, DeleteOutlined, RollbackOutlined, EditOutlined, SendOutlined, PushpinOutlined, NotificationOutlined, LinkOutlined, MessageOutlined, QrcodeOutlined, TeamOutlined, CreditCardOutlined } from '@ant-design/icons';
import DeleteConfirmModal from '../../components/DeleteConfirmModal/DeleteConfirmModal';
import { useSelector } from 'react-redux';
import { useRoleGuard } from '../../hooks/useRoleGuard';
import { fetchVersionList, createVersion, fetchVersionDetail, publishVersion, withdrawVersion, deleteVersion, updateVersion } from './thunk';
import { fetchSubscribedAbilities } from '../Capabilities/thunk';
import { VERSION_STATUS_MAP, VERSION_STATUS, ABILITY_TYPE_MAP, FORM_VALIDATION_RULES, INIT_PAGECONFIG, PAGE_SIZE_OPTIONS } from '../../utils/constants';

import './VersionRelease.m.less';

/** 页面视图模式：list / create / view / edit */
const MODE = { LIST: 'list', CREATE: 'create', VIEW: 'view', EDIT: 'edit' };

/** 能力类型 → Icon 组件映射表 */
const ABILITY_ICON_MAP = {
  PushpinOutlined,
  NotificationOutlined,
  LinkOutlined,
  MessageOutlined,
  QrcodeOutlined,
  TeamOutlined,
  CreditCardOutlined,
};

/**
 * 版本管理页（含版本列表 + 创建/查看/编辑 共用表单）
 */
function VersionRelease() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const location = useLocation();
  const { appBaseInfo } = useSelector(state => state.app);
  const appId = searchParams.get('appId');

  // 页面级权限守卫
  const { loading: roleLoading } = useRoleGuard(appId);

  // 页面模式
  const [mode, setMode] = useState(MODE.LIST);

  // 列表
  const [versions, setVersions] = useState([]);
  const [pagination, setPagination] = useState(INIT_PAGECONFIG);
  const [loading, setLoading] = useState(false);

  // 表单（创建/编辑共用）
  const [form] = Form.useForm();

  // 详情数据
  const [versionDetail, setVersionDetail] = useState(null);
  const [detailLoading, setDetailLoading] = useState(false);

  // 确认弹窗
  const [confirmModalVisible, setConfirmModalVisible] = useState(false);
  const [confirmAction, setConfirmAction] = useState(null);
  const [confirmModalInfo, setConfirmModalInfo] = useState({});
  const [confirmLoading, setConfirmLoading] = useState(false);

  // 已订阅能力
  const [subscribedAbilities, setSubscribedAbilities] = useState([]);

  const loadVersions = useCallback(async (params = { curPage: 1, pageSize: 10 }) => {
    setLoading(true);
    try {
      const result = await fetchVersionList(appId, params);
      if (result?.code === '200') {
        setVersions(result.data || []);
        setPagination(result.page ? {
          ...result.page,
          total: parseInt(result.page.total, 10) || 0,
        } : INIT_PAGECONFIG);
      }
    } catch (error) {
      message.error('加载版本列表失败');
    } finally {
      setLoading(false);
    }
  }, [appId]);

  useEffect(() => {
    if (!appId || roleLoading) {
      if (!appId) navigate('/');
      return;
    }
    if (!appBaseInfo) return; // Context 还没加载好
    if (appBaseInfo.appType !== 1) {
      navigate(`/basic-info?appId=${appId}`);
      return;
    }

    // 每次依赖变化都刷新
    setMode(MODE.LIST);
    setVersionDetail(null);
    loadVersions();

    fetchSubscribedAbilities(appId).then((abilityRes) => {
      if (abilityRes?.code === '200' && Array.isArray(abilityRes.data)) {
        setSubscribedAbilities(abilityRes.data);
      }
    });
  }, [appId, appBaseInfo, roleLoading, location.pathname, loadVersions]);

  const handlePageChange = (page, pageSize) => {
    loadVersions({ curPage: page, pageSize });
  };

  // 返回列表
  const backToList = () => {
    setMode(MODE.LIST);
    setVersionDetail(null);
    form.resetFields();
    loadVersions();
  };

  // 进入创建模式
  const enterCreate = () => {
    setMode(MODE.CREATE);
    form.resetFields();
  };

  // 进入查看模式
  const enterView = async (versionId) => {
    setDetailLoading(true);
    setMode(MODE.VIEW);
    try {
      const result = await fetchVersionDetail(appId, versionId);
      if (result?.code === '200') {
        const data = result.data;
        setVersionDetail(data);
        form.setFieldsValue({
          versionCode: data.versionCode,
          versionDescCn: data.versionDescCn,
          versionDescEn: data.versionDescEn,
        });
      } else {
        message.error(result?.messageZh || '加载版本详情失败');
        backToList();
      }
    } catch (error) {
      message.error('加载版本详情失败');
      backToList();
    } finally {
      setDetailLoading(false);
    }
  };

  // 进入编辑模式（从查看进入）
  const enterEdit = () => {
    setMode(MODE.EDIT);
  };

  // 创建版本
  const handleCreate = async () => {
    try {
      const values = await form.validateFields();
      const result = await createVersion(appId, values);
      if (result?.code === '200') {
        message.success('版本创建成功');
        const newVersionId = result.data;
        if (newVersionId) {
          enterView(newVersionId);
        } else {
          backToList();
          loadVersions();
        }
      } else {
        message.error(result?.messageZh || '创建失败');
      }
    } catch (error) {
      // validation error
    }
  };

  // 保存编辑
  const handleSave = async () => {
    try {
      const values = await form.validateFields();
      const result = await updateVersion(appId, versionDetail.id, values);
      if (result?.code === '200') {
        message.success('保存成功');
        // 回到查看模式
        enterView(versionDetail.id);
      } else {
        message.error(result?.messageZh || '保存失败');
      }
    } catch (error) {
      // validation error
    }
  };

  // 统一确认弹窗
  const showConfirm = (action, versionId, versionCode) => {
    let modalInfo = {};
    let handler = null;

    if (action === 'publish') {
      modalInfo = {
        title: '确认发布',
        content: `确认发布版本 ${versionCode}？发布后将提交审批。`,
        confirmButtonText: '确认发布',
        loadingText: '发布中...',
        dangerColor: '#1677ff',
      };
      handler = async () => {
        const result = await publishVersion(appId, versionId);
        if (result?.code === '200') {
          message.success('已提交审批');
          backToList();
          loadVersions();
        } else {
          message.error(result?.messageZh || '发布失败');
        }
      };
    } else if (action === 'withdraw') {
      modalInfo = {
        title: '确认撤回',
        content: `确认撤回版本 ${versionCode} 的发布申请？`,
        confirmButtonText: '确认撤回',
        loadingText: '撤回中...',
        dangerColor: '#faad14',
      };
      handler = async () => {
        const result = await withdrawVersion(appId, versionId);
        if (result?.code === '200') {
          message.success('版本已撤回');
          backToList();
          loadVersions();
        } else {
          message.error(result?.messageZh || '撤回失败');
        }
      };
    } else if (action === 'delete') {
      modalInfo = {
        title: '确认删除',
        content: `确认删除版本 ${versionCode}？该操作不可撤销。`,
        confirmButtonText: '确认删除',
        loadingText: '删除中...',
        dangerColor: '#ff4d4f',
      };
      handler = async () => {
        const result = await deleteVersion(appId, versionId);
        if (result?.code === '200') {
          message.success('版本已删除');
          backToList();
          loadVersions();
        } else {
          message.error(result?.messageZh || '删除失败');
        }
      };
    }

    setConfirmAction(() => handler);
    setConfirmModalInfo(modalInfo);
    setConfirmModalVisible(true);
  };

  const handleConfirmOk = async () => {
    setConfirmLoading(true);
    if (confirmAction) {
      await confirmAction();
    }
    setConfirmLoading(false);
    setConfirmModalVisible(false);
    setConfirmAction(null);
  };

  const formatTime = (isoStr) => {
    if (!isoStr) return '-';
    const d = new Date(isoStr);
    if (isNaN(d.getTime())) return isoStr;
    const pad = (n) => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
  };

  // 判断是否只读模式
  const isReadOnly = mode === MODE.VIEW;
  // 判断是否为新建
  const isCreate = mode === MODE.CREATE;

  // 面包屑文案
  const breadcrumbTitle = isCreate ? '创建版本' : (isReadOnly ? '版本详情' : '编辑版本');

  // 操作按钮权限
  const canEdit = versionDetail?.status === VERSION_STATUS.DRAFT;
  const canPublish = versionDetail?.status === VERSION_STATUS.DRAFT;
  const canWithdraw = versionDetail?.status === VERSION_STATUS.UNDER_REVIEW;
  const canDelete = versionDetail?.status === VERSION_STATUS.DRAFT || versionDetail?.status === VERSION_STATUS.REJECTED;

  const columns = [
    { title: '版本号', dataIndex: 'versionCode', key: 'versionCode', render: (text) => <strong>{text}</strong> },
    {
      title: '版本状态',
      dataIndex: 'status',
      key: 'status',
      render: (status) => (
        <Tag color={VERSION_STATUS_MAP[status]?.color || 'default'}>
          {VERSION_STATUS_MAP[status]?.text || status}
        </Tag>
      ),
    },
    { title: '发布人', dataIndex: 'createBy', key: 'createBy', render: (t) => t || '-' },
    {
      title: '审核通过时间',
      dataIndex: 'approvedTime',
      key: 'approvedTime',
      render: (t, record) => record.status === VERSION_STATUS.APPROVED ? formatTime(t) : '-',
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
        <div className="action-btns">
          <Button type="link" icon={<EyeOutlined />} onClick={() => enterView(record.id)}>查看</Button>
          {record.status === VERSION_STATUS.UNDER_REVIEW && (
            <Button type="link" icon={<RollbackOutlined />} onClick={() => showConfirm('withdraw', record.id, record.versionCode)}>
              撤回
            </Button>
          )}
          {(record.status === VERSION_STATUS.DRAFT || record.status === VERSION_STATUS.REJECTED) && (
            <Button type="link" danger icon={<DeleteOutlined />} onClick={() => showConfirm('delete', record.id, record.versionCode)}>
              删除
            </Button>
          )}
        </div>
      ),
    },
  ];

  return (
    <div className="version-release-page">
      {mode === MODE.LIST ? (
        /* ====== 版本列表 ====== */
        <div className="content-card">
          <div className="page-header">
            <h2>版本管理</h2>
            <Button type="primary" icon={<PlusOutlined />} onClick={enterCreate}
              disabled={versions.some(v => v.status === VERSION_STATUS.DRAFT || v.status === VERSION_STATUS.UNDER_REVIEW || v.status === VERSION_STATUS.REJECTED)}
            >
              创建版本
            </Button>
          </div>
          <Spin spinning={loading}>
            <Table columns={columns} dataSource={versions} rowKey="id" pagination={false} />
            <div style={{ marginTop: 16 }}>
              <div className="version-pagination">
                <Pagination
                  current={pagination.curPage}
                  pageSize={pagination.pageSize}
                  total={pagination.total}
                  onChange={handlePageChange}
                  showSizeChanger
                  pageSizeOptions={PAGE_SIZE_OPTIONS}
                  showQuickJumper
                  showTotal={(total) => `共 ${total} 条`}
                />
              </div>
            </div>
          </Spin>
        </div>
      ) : (
        /* ====== 创建 / 查看 / 编辑 共用表单 ====== */
        <Spin spinning={detailLoading}>
          <div className="content-card">
            <div className="version-breadcrumb">
              <span className="version-breadcrumb-link" onClick={backToList}>版本发布与审核</span>
              <span className="version-breadcrumb-sep">&gt;</span>
              <span>{breadcrumbTitle}</span>
            </div>

            <div className="version-form-title-row">
              <h3 className="version-form-title">版本详情</h3>
              {!isCreate && isReadOnly && canEdit && (
                <Button type="link" icon={<EditOutlined />} onClick={enterEdit}>编辑</Button>
              )}
            </div>

            <Form form={form} className="version-create-form">
              <div className="version-form-row">
                <div className="version-form-label"><span className="version-form-required">*</span> 版本号</div>
                <div className="version-form-field">
                  <Form.Item
                    name="versionCode"
                    rules={[
                      { required: true, message: '请输入版本号' },
                      { pattern: FORM_VALIDATION_RULES.versionCode.pattern, message: FORM_VALIDATION_RULES.versionCode.message },
                    ]}
                    style={{ marginBottom: 0 }}
                  >
                    {isReadOnly
                      ? <span className="version-form-text">{versionDetail?.versionCode}</span>
                      : <Input placeholder="请输入版本号, 如X.X.X" maxLength={100} showCount />}
                  </Form.Item>
                </div>
              </div>

              <div className="version-form-row">
                <div className="version-form-label"><span className="version-form-required">*</span> 版本描述</div>
                <div className="version-form-field">
                  <Form.Item
                    name="versionDescCn"
                    rules={[{ required: true, message: '请输入版本描述' }, { max: 200, message: '描述不超过200字符' }]}
                    style={{ marginBottom: 0 }}
                  >
                    {isReadOnly
                      ? <span className="version-form-text">{versionDetail?.versionDescCn || '-'}</span>
                      : <Input.TextArea rows={3} maxLength={200} showCount placeholder="请输入版本描述" />}
                  </Form.Item>
                </div>
              </div>

              <div className="version-form-row">
                <div className="version-form-label">应用能力</div>
                <div className="version-form-field version-form-field-wide">
                  <div className="ability-card-wrapper">
                    <div className="ability-card-list">
                      {subscribedAbilities.map((ability) => {
                        const typeInfo = ABILITY_TYPE_MAP[ability.abilityType] || {};
                        const IconComp = ABILITY_ICON_MAP[typeInfo.icon];
                        return (
                          <div key={ability.abilityType} className="ability-card-item">
                            <div className="ability-card-row">
                              <div className="ability-card-icon">
                                {ability.iconUrl ? (
                                  <img src={ability.iconUrl} alt="" />
                                ) : IconComp ? (
                                  <IconComp style={{ fontSize: 24, color: '#3370ff' }} />
                                ) : (
                                  <span style={{ fontSize: 24, color: '#3370ff' }}>⚡</span>
                                )}
                              </div>
                              <span className="ability-card-name">{ability.nameCn || typeInfo.text || '能力'}</span>
                            </div>
                            <div className="ability-card-status">已开启</div>
                          </div>
                        );
                      })}
                    </div>
                  </div>
                  <div className="ability-notice">应用被添加入群后，将自动发送通知，确认是否需要更新入群通知</div>
                </div>
              </div>

              {/* 查看模式：发布按钮在应用能力下方（详情页只有编辑+发布，撤回在列表页） */}
              {!isCreate && isReadOnly && (
                <div className="version-detail-actions">
                  {canPublish && (
                    <Button type="primary" icon={<SendOutlined />} onClick={() => showConfirm('publish', versionDetail.id, versionDetail.versionCode)}>
                      发布
                    </Button>
                  )}
                </div>
              )}

              {/* 创建/编辑模式：保存 */}
              {(isCreate || (!isCreate && !isReadOnly)) && (
                <div className="version-form-actions">
                  <Button type="primary" onClick={isCreate ? handleCreate : handleSave}>保存</Button>
                </div>
              )}
            </Form>
          </div>
        </Spin>
      )}

      {/* 统一确认弹窗 */}
      <DeleteConfirmModal
        open={confirmModalVisible}
        onClose={() => { setConfirmModalVisible(false); setConfirmAction(null); }}
        onConfirm={handleConfirmOk}
        modalInfo={confirmModalInfo}
        loading={confirmLoading}
      />
    </div>
  );
}

export default VersionRelease;
