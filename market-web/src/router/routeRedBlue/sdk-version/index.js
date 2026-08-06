import React, { useState, useEffect } from 'react';
import { message, Table, Pagination, Input, Select, Button, Modal, Tag } from 'antd';
import {
  getSdkVersionList,
  getSdkVersionDetail,
  createSdkVersion,
  updateSdkVersion,
  updateSdkVersionStatus
} from './thunk';
import {
  DEFAULT_SEARCH_VALUES,
  STATUS_MAP,
  STATUS_OPTIONS,
  MODAL_TITLE_CREATE,
  MODAL_TITLE_EDIT,
  getTableColumns
} from './constant';
import { DEFAULT_PAGINATION, PAGE_SIZE_OPTIONS } from '../../../utils/constant';
import less from './index.module.less';
import SdkVersionFormModal from './components/SdkVersionFormModal';
import SdkVersionDetailDrawer from './components/SdkVersionDetailDrawer';
import DeprecateModal from './components/DeprecateModal';
import { renderAlwaysWithTooltip } from '../../../utils/common';

/**
 * SDK版本管理列表页面组件
 */
const SdkVersionList = () => {
  const [loading, setLoading] = useState(false);
  const [dataSource, setDataSource] = useState([]);
  const [pagination, setPagination] = useState(DEFAULT_PAGINATION);
  const [queryParams, setQueryParams] = useState({
    ...DEFAULT_PAGINATION,
    ...DEFAULT_SEARCH_VALUES
  });
  const [searchValues, setSearchValues] = useState(DEFAULT_SEARCH_VALUES);

  const [formVisible, setFormVisible] = useState(false);
  const [formTitle, setFormTitle] = useState(MODAL_TITLE_CREATE);
  const [editingId, setEditingId] = useState(null);
  const [editingRecord, setEditingRecord] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  const [detailVisible, setDetailVisible] = useState(false);
  const [detail, setDetail] = useState(null);

  const [deprecateVisible, setDeprecateVisible] = useState(false);
  const [deprecateRecord, setDeprecateRecord] = useState(null);
  const [deprecateLoading, setDeprecateLoading] = useState(false);

  const [restoreVisible, setRestoreVisible] = useState(false);
  const [restoreRecord, setRestoreRecord] = useState(null);

  const fetchData = async () => {
    setLoading(true);
    const result = await getSdkVersionList(queryParams);
    if (result && result.code === '200') {
      const responseData = result.data || [];
      setDataSource(responseData);
      const page = result.page || {};
      setPagination({
        pageNum: page.curPage || 1,
        pageSize: page.pageSize || DEFAULT_PAGINATION.pageSize,
        total: page.total || 0
      });
    } else {
      message.error({ content: result?.messageZh || '获取数据失败' });
      setDataSource([]);
    }
    setLoading(false);
  };

  useEffect(() => {
    fetchData();
  }, [queryParams]);

  const handleSearch = () => {
    setQueryParams({
      ...DEFAULT_PAGINATION,
      ...searchValues
    });
  };

  const handleReset = () => {
    setSearchValues(DEFAULT_SEARCH_VALUES);
    setQueryParams({
      ...DEFAULT_PAGINATION,
      ...DEFAULT_SEARCH_VALUES
    });
  };

  const handlePageChange = (page, pageSize) => {
    setQueryParams({
      ...queryParams,
      pageNum: page,
      pageSize: pageSize || queryParams.pageSize
    });
  };

  const handleCreate = () => {
    setFormTitle(MODAL_TITLE_CREATE);
    setEditingId(null);
    setEditingRecord(null);
    setFormVisible(true);
  };

  const handleEdit = async (record) => {
    // 编辑需用详情数据完整回填（列表接口不返回 artifactUrl 等字段）
    const result = await getSdkVersionDetail(record.id);
    if (result && result.code === '200') {
      setFormTitle(MODAL_TITLE_EDIT);
      setEditingId(record.id);
      setEditingRecord(result.data);
      setFormVisible(true);
    } else {
      message.error({ content: result?.messageZh || '获取编辑数据失败' });
    }
  };

  const handleFormSubmit = async (values) => {
    setSubmitting(true);
    let result;
    if (editingId) {
      result = await updateSdkVersion(editingId, values);
    } else {
      result = await createSdkVersion(values);
    }

    if (result && result.code === '200') {
      message.success(editingId ? '编辑成功' : '上架成功');
      setFormVisible(false);
      fetchData();
    } else {
      message.error({ content: result?.messageZh || '保存失败' });
    }
    setSubmitting(false);
  };

  const handleViewDetail = async (record) => {
    const result = await getSdkVersionDetail(record.id);
    if (result && result.code === '200') {
      setDetail(result.data);
      setDetailVisible(true);
    } else {
      message.error({ content: result?.messageZh || '获取详情失败' });
    }
  };

  const handleDeprecate = (record) => {
    setDeprecateRecord(record);
    setDeprecateVisible(true);
    if (detailVisible) {
      setDetailVisible(false);
    }
  };

  const handleDeprecateConfirm = async (values) => {
    setDeprecateLoading(true);
    const result = await updateSdkVersionStatus({
      id: deprecateRecord.id,
      status: 2,
      deprecateReason: values.deprecateReason
    });
    if (result && result.code === '200') {
      message.success('废弃成功');
      setDeprecateVisible(false);
      fetchData();
    } else {
      message.error({ content: result?.messageZh || '废弃失败' });
    }
    setDeprecateLoading(false);
  };

  const handleRestore = (record) => {
    setRestoreRecord(record);
    setRestoreVisible(true);
    if (detailVisible) {
      setDetailVisible(false);
    }
  };

  const handleRestoreConfirm = async () => {
    const result = await updateSdkVersionStatus({
      id: restoreRecord.id,
      status: 1
    });
    if (result && result.code === '200') {
      message.success('恢复成功');
      setRestoreVisible(false);
      fetchData();
    } else {
      message.error({ content: result?.messageZh || '恢复失败' });
    }
  };

  const columns = getTableColumns({
    renderVersionName: (text, record) => (
      <span
        className={less.cellLink}
        onClick={() => handleViewDetail(record)}
      >
        {text}
      </span>
    ),
    renderPermission: (text) => text || '-',
    renderStatus: (status) => {
      const statusInfo = STATUS_MAP[status] || {};
      return (
        <Tag color={status === 1 ? 'green' : 'orange'}>
          {statusInfo.text || '-'}
        </Tag>
      );
    },
    renderAction: (_, record) => (
      <div className={less.actions}>
        <span
          className={less.actionLink}
          onClick={() => handleViewDetail(record)}
        >
          详情
        </span>
        {record.status === 1 && (
          <>
            <span
              className={less.actionLink}
              onClick={() => handleEdit(record)}
            >
              编辑
            </span>
            <span
              className={`${less.actionLink} ${less.danger}`}
              onClick={() => handleDeprecate(record)}
            >
              废弃
            </span>
          </>
        )}
        {record.status === 2 && (
          <span
            className={`${less.actionLink} ${less.success}`}
            onClick={() => handleRestore(record)}
          >
            恢复
          </span>
        )}
      </div>
    )
  });

  return (
    <div className={less.container}>
      <div className={less.page}>
        <div className={less.pageHead}>
          <div className={less.pageHeadLeft}>
            <span className={less.pageHeadTitle}>SDK版本管理</span>
          </div>
        </div>

        <div className={less.toolbar}>
          <div className={less.toolbarLeft}>
            <div className={less.searchWrap}>
              <span className={less.searchLabel}>状态</span>
              <Select
                style={{ width: 120 }}
                value={searchValues.status}
                onChange={(value) => setSearchValues({ ...searchValues, status: value })}
                options={STATUS_OPTIONS}
              />
            </div>
            <div className={less.searchWrap}>
              <span className={less.searchLabel}>版本号</span>
              <Input
                style={{ width: 150 }}
                placeholder="如 1.2.0"
                value={searchValues.versionName}
                onChange={(e) => setSearchValues({ ...searchValues, versionName: e.target.value })}
                onPressEnter={handleSearch}
              />
            </div>
            <Button onClick={handleSearch}>搜索</Button>
            <Button onClick={handleReset}>重置</Button>
          </div>
          <div className={less.toolbarRight}>
            <Button type="primary" onClick={handleCreate}>上架新版本</Button>
          </div>
        </div>

        <Table
          columns={columns}
          dataSource={dataSource}
          rowKey="id"
          loading={loading}
          pagination={false}
          scroll={{ x: 'max-content' }}
        />

        <div className={less.paginationWrapper}>
          <Pagination
            total={pagination.total}
            current={pagination.pageNum}
            pageSize={pagination.pageSize}
            pageSizeOptions={PAGE_SIZE_OPTIONS}
            showSizeChanger
            showQuickJumper
            showTotal={(total) => `共 ${total} 条`}
            onChange={handlePageChange}
          />
        </div>
      </div>

      <SdkVersionFormModal
        open={formVisible}
        title={formTitle}
        loading={submitting}
        editingId={editingId}
        initialValues={editingRecord}
        onClose={() => {
          setFormVisible(false);
          setEditingRecord(null);
        }}
        onSubmit={handleFormSubmit}
      />

      <SdkVersionDetailDrawer
        open={detailVisible}
        detail={detail}
        onClose={() => setDetailVisible(false)}
        onDeprecate={handleDeprecate}
        onRestore={handleRestore}
      />

      <DeprecateModal
        open={deprecateVisible}
        record={deprecateRecord}
        loading={deprecateLoading}
        onClose={() => setDeprecateVisible(false)}
        onConfirm={handleDeprecateConfirm}
      />

      <Modal
        title="恢复版本"
        open={restoreVisible}
        onOk={handleRestoreConfirm}
        onCancel={() => setRestoreVisible(false)}
        okText="确认恢复"
        cancelText="取消"
        width={400}
      >
        <p style={{ color: '#4e5969' }}>
          确定恢复版本 <strong>{restoreRecord?.versionName}</strong> 吗？
        </p>
      </Modal>
    </div>
  );
};

export default SdkVersionList;
