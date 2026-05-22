import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Form,
  Input,
  Modal,
  message,
  Pagination,
  Popconfirm,
  Button,
  Upload
} from 'antd';
import { SearchOutlined, ReloadOutlined, PlusOutlined, DownloadOutlined, UploadOutlined } from '@ant-design/icons';
import {
  getClassifyList,
  createClassify,
  updateClassify,
  deleteClassify,
  exportItemsAsync,
  importItemsAsync,
  downloadImportTemplate,
  type Classify,
  type ClassifyForm,
  type ClassifyQueryParams
} from '@/api/lookup';
import styles from './index.module.less';

const statusMap = {
  1: { text: '有效', dotClass: styles.dotActive, labelClass: styles.labelActive },
  0: { text: '失效', dotClass: styles.dotInactive, labelClass: styles.labelInactive }
};

const ClassifyList: React.FC = () => {
  const navigate = useNavigate();
  const [form] = Form.useForm();
  const [modalForm] = Form.useForm();

  const [loading, setLoading] = useState(false);
  const [dataSource, setDataSource] = useState<Classify[]>([]);
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0
  });

  const [queryParams, setQueryParams] = useState<ClassifyQueryParams>({
    pageNum: 1,
    pageSize: 10
  });

  const [searchValues, setSearchValues] = useState({
    classifyCode: '',
    classifyName: '',
    classifyDesc: '',
    status: ''
  });

  const [modalVisible, setModalVisible] = useState(false);
  const [modalTitle, setModalTitle] = useState('新增分类');
  const [editingId, setEditingId] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const [taskNotifyVisible, setTaskNotifyVisible] = useState(false);
  const [taskNotifyType, setTaskNotifyType] = useState<'import' | 'export'>('import');
  const [currentTaskId, setCurrentTaskId] = useState('');

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const res = await getClassifyList(queryParams);
      const responseData = res.data.data;
      setDataSource(responseData.list || []);
      setPagination({
        current: responseData.pageNum,
        pageSize: responseData.pageSize,
        total: responseData.total
      });
    } catch (error) {
      console.error('获取分类列表失败:', error);
    } finally {
      setLoading(false);
    }
  }, [queryParams]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const handleSearch = () => {
    const newParams = {
      ...queryParams,
      ...searchValues,
      pageNum: 1
    };
    setQueryParams(newParams);
  };

  const handleReset = () => {
    setSearchValues({
      classifyCode: '',
      classifyName: '',
      classifyDesc: '',
      status: ''
    });
    const newParams = {
      pageNum: 1,
      pageSize: queryParams.pageSize
    };
    setQueryParams(newParams);
  };

  const handlePageChange = (page: number, pageSize?: number) => {
    const newParams = {
      ...queryParams,
      pageNum: page,
      pageSize: pageSize || queryParams.pageSize
    };
    setQueryParams(newParams);
  };

  const handleAdd = () => {
    setModalTitle('新增分类');
    setEditingId(null);
    modalForm.resetFields();
    setModalVisible(true);
  };

  const handleEdit = (record: Classify) => {
    setModalTitle('编辑分类');
    setEditingId(record.classifyId);
    modalForm.setFieldsValue({
      classifyCode: record.classifyCode,
      classifyName: record.classifyName,
      path: record.path,
      classifyDesc: record.classifyDesc
    });
    setModalVisible(true);
  };

  const handleSave = async (values: ClassifyForm) => {
    setSubmitting(true);
    try {
      if (editingId) {
        await updateClassify(editingId, values);
        message.success('编辑成功');
      } else {
        await createClassify(values);
        message.success('新增成功');
      }
      setModalVisible(false);
      fetchData();
    } catch (error) {
      console.error('保存分类失败:', error);
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (id: string) => {
    try {
      await deleteClassify(id);
      message.success('删除成功');
      fetchData();
    } catch (error) {
      console.error('删除分类失败:', error);
    }
  };

  const handleToggleStatus = (record: Classify) => {
    const newStatus = record.status === 1 ? 0 : 1;
    const actionText = newStatus === 1 ? '生效' : '失效';
    Modal.confirm({
      title: `确认${actionText}`,
      content: `确定要${actionText}分类 "${record.classifyName}" 吗？`,
      okText: '确定',
      cancelText: '取消',
      onOk: async () => {
        try {
          await updateClassify(record.classifyId, {
            classifyCode: record.classifyCode,
            classifyName: record.classifyName,
            path: record.path,
            classifyDesc: record.classifyDesc,
            status: newStatus
          });
          message.success(`${actionText}成功`);
          fetchData();
        } catch (error) {
          console.error('切换状态失败:', error);
        }
      }
    });
  };

  const handleRowClick = (record: Classify) => {
    navigate('/lookup/item', {
      state: {
        classifyId: record.classifyId,
        classifyCode: record.classifyCode,
        classifyName: record.classifyName
      }
    });
  };

  const handleExport = () => {
    Modal.confirm({
      title: '确认导出',
      content: '单次最多导出1000条数据，是否继续？',
      okText: '确认',
      cancelText: '取消',
      onOk: async () => {
        try {
          const res = await exportItemsAsync({});
          const taskId = String(res.data.data.taskId);
          setCurrentTaskId(taskId);
          setTaskNotifyType('export');
          setTaskNotifyVisible(true);
          message.loading({ content: '正在提交导出任务...', key: 'export' });
        } catch (error) {
          console.error('导出失败:', error);
          message.error({ content: '导出失败', key: 'export' });
        }
      }
    });
  };

  const handleImport = () => {
    setImportModalVisible(true);
  };

  const [importModalVisible, setImportModalVisible] = useState(false);
  const [importing, setImporting] = useState(false);
  const [importFile, setImportFile] = useState<File | null>(null);

  const handleDownloadTemplate = () => {
    downloadImportTemplate().then((blob: any) => {
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = 'lookup_import_template.xlsx';
      a.click();
      window.URL.revokeObjectURL(url);
    });
  };

  const handleFileChange = (info: any) => {
    const file = info.file.originFileObj || info.file;
    if (file) {
      setImportFile(file);
    }
  };

  const handleImportSubmit = async () => {
    if (!importFile) {
      message.warning('请选择要导入的文件');
      return;
    }
    setImporting(true);
    try {
      const res = await importItemsAsync('', importFile);
      const taskId = String(res.data.data.taskId);
      setCurrentTaskId(taskId);
      setTaskNotifyType('import');
      setTaskNotifyVisible(true);
      setImportModalVisible(false);
      setImportFile(null);
      message.loading({ content: '正在提交导入任务...', key: 'import' });
    } catch (error) {
      console.error('导入失败:', error);
      message.error({ content: '导入失败', key: 'import' });
    } finally {
      setImporting(false);
    }
  };

  const columns = [
    {
      title: <input type="checkbox" className={styles.checkbox} />,
      key: 'checkbox',
      width: 40,
      render: () => <span onClick={(e) => e.stopPropagation()}><input type="checkbox" className={styles.checkbox} /></span>
    },
    {
      title: '类别编码',
      dataIndex: 'classifyCode',
      key: 'classifyCode',
      width: 120
    },
    {
      title: '类别名称',
      dataIndex: 'classifyName',
      key: 'classifyName',
      width: 140
    },
    {
      title: '路径',
      dataIndex: 'path',
      key: 'path',
      width: 120,
      render: (text: string) => text || '-'
    },
    {
      title: '描述',
      dataIndex: 'classifyDesc',
      key: 'classifyDesc',
      ellipsis: true,
      render: (text: string) => text || '-'
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 80,
      render: (status: number) => {
        const statusInfo = statusMap[status as keyof typeof statusMap];
        return (
          <div className={styles.statusBadge}>
            <span className={`${styles.dot} ${statusInfo?.dotClass || ''}`}></span>
            <span className={`${styles.label} ${statusInfo?.labelClass || ''}`}>
              {statusInfo?.text || '-'}
            </span>
          </div>
        );
      }
    },
    {
      title: '创建人',
      dataIndex: 'createBy',
      key: 'createBy',
      width: 90
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      key: 'createTime',
      width: 160
    },
    {
      title: '修改人',
      dataIndex: 'lastUpdateBy',
      key: 'lastUpdateBy',
      width: 90
    },
    {
      title: '修改时间',
      dataIndex: 'lastUpdateTime',
      key: 'lastUpdateTime',
      width: 160
    },
    {
      title: '操作',
      key: 'action',
      width: 140,
      fixed: 'right' as const,
      render: (_: any, record: Classify) => (
        <div className={styles.actions}>
          <button className={styles.actionLink} onClick={(e) => { e.stopPropagation(); handleEdit(record); }}>
            编辑
          </button>
          <button className={styles.actionLink} onClick={(e) => { e.stopPropagation(); handleToggleStatus(record); }}>
            {record.status === 1 ? '失效' : '生效'}
          </button>
          <Popconfirm
            title={`确定要删除分类 "${record.classifyName}" 吗？`}
            onConfirm={() => handleDelete(record.classifyId)}
            okText="确定"
            cancelText="取消"
          >
            <button className={`${styles.actionLink} ${styles.danger}`} onClick={(e) => e.stopPropagation()}>
              删除
            </button>
          </Popconfirm>
        </div>
      )
    }
  ];

  return (
    <div className={styles.container}>
      <div className={styles.page}>
        <div className={styles.pageHead}>
          <div className={styles.pageHeadLeft}>
            <span className={styles.pageHeadTitle}>LookUp分类</span>
          </div>
        </div>

        <div className={styles.toolbar}>
          <div className={styles.toolbarLeft}>
            <div className={styles.searchWrap}>
              <span className={styles.searchLabel}>类别编码</span>
              <input
                type="text"
                className={styles.searchInput}
                placeholder="请输入"
                value={searchValues.classifyCode}
                onChange={(e) => setSearchValues({...searchValues, classifyCode: e.target.value})}
              />
            </div>
            <div className={styles.searchWrap}>
              <span className={styles.searchLabel}>类别名称</span>
              <input
                type="text"
                className={styles.searchInput}
                placeholder="请输入"
                value={searchValues.classifyName}
                onChange={(e) => setSearchValues({...searchValues, classifyName: e.target.value})}
              />
            </div>
            <div className={styles.searchWrap}>
              <span className={styles.searchLabel}>描述</span>
              <input
                type="text"
                className={styles.searchInput}
                placeholder="请输入"
                style={{ width: 100 }}
                value={searchValues.classifyDesc}
                onChange={(e) => setSearchValues({...searchValues, classifyDesc: e.target.value})}
              />
            </div>
            <select
              className={styles.filterSelect}
              value={searchValues.status}
              onChange={(e) => setSearchValues({...searchValues, status: e.target.value})}
            >
              <option value="">全部状态</option>
              <option value={1}>有效</option>
              <option value={0}>失效</option>
            </select>
            <button className={`${styles.btn} ${styles.btnPrimary} ${styles.btnSm}`} onClick={handleSearch}>
              <SearchOutlined /> 查询
            </button>
            <button className={`${styles.btn} ${styles.btnOutline} ${styles.btnSm}`} onClick={handleReset}>
              <ReloadOutlined /> 重置
            </button>
          </div>
          <div className={styles.toolbarRight}>
            <button className={`${styles.btn} ${styles.btnOutline} ${styles.btnSm}`} onClick={handleExport}>
              <DownloadOutlined /> 导出
            </button>
            <button className={`${styles.btn} ${styles.btnOutline} ${styles.btnSm}`} onClick={handleImport}>
              <UploadOutlined /> 导入
            </button>
            <button className={`${styles.btn} ${styles.btnPrimary} ${styles.btnSm}`} onClick={handleAdd}>
              <PlusOutlined /> 新增分类
            </button>
          </div>
        </div>

        <div className={styles.tableWrap}>
          <table className={styles.table}>
            <thead>
              <tr>
                {columns.map((col, index) => (
                  <th key={index} style={{ width: col.width }}>
                    {col.title as string}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={columns.length} style={{ textAlign: 'center', padding: '60px 0' }}>
                    加载中...
                  </td>
                </tr>
              ) : dataSource.length === 0 ? (
                <tr>
                  <td colSpan={columns.length}>
                    <div className={styles.empty}>
                      <div className={styles.icon}>📋</div>
                      <p>暂无数据</p>
                    </div>
                  </td>
                </tr>
              ) : (
                dataSource.map((record) => (
                  <tr key={record.classifyId}>
                    <td onClick={(e) => e.stopPropagation()}>
                      <input type="checkbox" className={styles.checkbox} />
                    </td>
                    <td className={styles.linkText} onClick={() => handleRowClick(record)}>
                      {record.classifyCode}
                    </td>
                    <td>{record.classifyName}</td>
                    <td>{record.path || '-'}</td>
                    <td>{record.classifyDesc || '-'}</td>
                    <td>
                      <div className={styles.statusBadge}>
                        <span className={`${styles.dot} ${statusMap[record.status as keyof typeof statusMap]?.dotClass || ''}`}></span>
                        <span className={`${styles.label} ${statusMap[record.status as keyof typeof statusMap]?.labelClass || ''}`}>
                          {statusMap[record.status as keyof typeof statusMap]?.text || '-'}
                        </span>
                      </div>
                    </td>
                    <td>{record.createBy}</td>
                    <td>{record.createTime}</td>
                    <td>{record.lastUpdateBy}</td>
                    <td>{record.lastUpdateTime}</td>
                    <td onClick={(e) => e.stopPropagation()}>
                      <div className={styles.actions}>
                        <button className={styles.actionLink} onClick={(e) => { e.stopPropagation(); handleEdit(record); }}>
                          编辑
                        </button>
                        <button className={styles.actionLink} onClick={(e) => { e.stopPropagation(); handleToggleStatus(record); }}>
                          {record.status === 1 ? '失效' : '生效'}
                        </button>
                        <Popconfirm
                          title={`确定要删除分类 "${record.classifyName}" 吗？`}
                          onConfirm={() => handleDelete(record.classifyId)}
                          okText="确定"
                          cancelText="取消"
                        >
                          <button className={`${styles.actionLink} ${styles.danger}`} onClick={(e) => e.stopPropagation()}>
                            删除
                          </button>
                        </Popconfirm>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        <div className={styles.pagination}>
          <div className={styles.paginationLeft}>
            <span className={styles.paginationInfo}>共 {pagination.total} 条</span>
          </div>
          <div className={styles.paginationRight}>
            <Pagination
              current={pagination.current}
              pageSize={pagination.pageSize}
              total={pagination.total}
              showSizeChanger
              showQuickJumper
              onChange={handlePageChange}
              pageSizeOptions={['10', '20', '50', '100', '200', '500', '1000']}
            />
          </div>
        </div>
      </div>

      <Modal
        title={modalTitle}
        open={modalVisible}
        onOk={() => modalForm.submit()}
        onCancel={() => setModalVisible(false)}
        confirmLoading={submitting}
        width={520}
        okText="保存"
        cancelText="取消"
      >
        <Form
          form={modalForm}
          layout="vertical"
          onFinish={handleSave}
        >
          <div style={{ display: 'flex', gap: 16 }}>
            <Form.Item
              name="classifyCode"
              label="分类编码"
              rules={[
                { required: true, message: '请输入分类编码' },
                { max: 100, message: '最多100个字符' }
              ]}
              style={{ flex: 1 }}
            >
              <Input placeholder="如: USER_TYPE" disabled={!!editingId} />
            </Form.Item>
            <Form.Item
              name="classifyName"
              label="分类名称"
              rules={[
                { required: true, message: '请输入分类名称' },
                { max: 100, message: '最多100个字符' }
              ]}
              style={{ flex: 1 }}
            >
              <Input placeholder="如: 用户类型" />
            </Form.Item>
          </div>
          <Form.Item
            name="path"
            label="路径"
            rules={[{ max: 100, message: '最多100个字符' }]}
          >
            <Input 
  placeholder="如: system/user_type，用于层级归类" 
  disabled={!!editingId}
/>
          </Form.Item>
          <div className={styles.formHint}>路径可用于分类的层级管理，斜杠分隔</div>
          <Form.Item
            name="classifyDesc"
            label="描述"
            rules={[{ max: 500, message: '最多500个字符' }]}
          >
            <Input.TextArea
              rows={3}
              placeholder="请输入分类描述..."
              maxLength={500}
            />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={null}
        footer={null}
        open={taskNotifyVisible}
        onCancel={() => setTaskNotifyVisible(false)}
        width={400}
        centered
      >
        <div style={{ textAlign: 'center', padding: '20px 0' }}>
          <div style={{ fontSize: '48px', marginBottom: '16px' }}>
            {taskNotifyType === 'import' ? '📥' : '📤'}
          </div>
          <div style={{ fontSize: '16px', fontWeight: 600, marginBottom: '8px' }}>
            {taskNotifyType === 'import' ? '导入任务已提交' : '导出任务已提交'}
          </div>
          <div style={{ fontSize: '13px', color: 'var(--text-secondary)' }}>
            任务ID: {currentTaskId}
          </div>
          <div style={{ fontSize: '13px', color: 'var(--text-secondary)', marginTop: '8px' }}>
            请在任务中心查看进度
          </div>
          <Button 
            type="primary" 
            style={{ marginTop: '20px' }}
            onClick={() => {
              setTaskNotifyVisible(false);
              navigate('/lookup/task');
            }}
          >
            查看任务中心
          </Button>
        </div>
      </Modal>

      <Modal
        title="批量导入"
        open={importModalVisible}
        onCancel={() => {
          setImportModalVisible(false);
          setImportFile(null);
        }}
        onOk={handleImportSubmit}
        confirmLoading={importing}
        width={520}
        okText="确认导入"
        cancelText="取消"
      >
        <div style={{ padding: '8px 0' }}>
          <div style={{ textAlign: 'right', marginBottom: '12px' }}>
            <a 
              onClick={handleDownloadTemplate}
              style={{ color: 'var(--primary)', cursor: 'pointer', fontSize: '13px', textDecoration: 'none' }}
              onMouseEnter={(e) => e.currentTarget.style.textDecoration = 'underline'}
              onMouseLeave={(e) => e.currentTarget.style.textDecoration = 'none'}
            >
              <DownloadOutlined style={{ marginRight: '4px' }} />
              下载 Excel 导入模板
            </a>
          </div>
          <Upload.Dragger
            name="file"
            accept=".xlsx,.xls"
            showUploadList={false}
            onChange={handleFileChange}
            beforeUpload={() => false}
          >
            <div style={{ padding: '20px 0' }}>
              <div style={{ fontSize: '32px', marginBottom: '8px', opacity: 0.3 }}>📄</div>
              <div style={{ fontSize: '14px', color: 'var(--text-secondary)', marginBottom: '4px' }}>
                点击选择或拖拽 Excel 文件到此处
              </div>
              <div style={{ fontSize: '12px', color: 'var(--text-hint)' }}>
                支持 .xlsx / .xls 格式，单次最多导入1000条数据
              </div>
            </div>
          </Upload.Dragger>
          {importFile && (
            <div style={{ marginTop: '12px', fontSize: '13px', color: 'var(--text-secondary)' }}>
              已选择: {importFile.name}
            </div>
          )}
        </div>
      </Modal>
    </div>
  );
};

export default ClassifyList;
