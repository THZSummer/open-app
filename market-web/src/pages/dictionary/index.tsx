import React, { useState, useEffect, useCallback } from 'react';
import {
  Form,
  Input,
  Button,
  Modal,
  message,
  Pagination,
  Drawer,
  Upload,
  Select
} from 'antd';
import { EditOutlined, SearchOutlined, ReloadOutlined, PlusOutlined, DownloadOutlined, UploadOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import {
  getDictionaryList,
  createDictionary,
  updateDictionary,
  deleteDictionary,
  downloadImportTemplate,
  submitImportTask,
  submitExportTask,
  type Dictionary,
  type DictionaryForm,
  type DictionaryQueryParams
} from '@/api/dictionary';
import styles from './index.module.less';

const { TextArea } = Input;

const statusMap = {
  1: { text: '有效', dotClass: styles.dotActive, labelClass: styles.labelActive },
  0: { text: '失效', dotClass: styles.dotInactive, labelClass: styles.labelInactive }
};

const DictionaryList: React.FC = () => {
  const navigate = useNavigate();
  const [detailForm] = Form.useForm();

  const [loading, setLoading] = useState(false);
  const [dataSource, setDataSource] = useState<Dictionary[]>([]);
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0
  });

  const [queryParams, setQueryParams] = useState<DictionaryQueryParams>({
    pageNum: 1,
    pageSize: 10
  });

  const [searchValues, setSearchValues] = useState({
    code: '',
    name: '',
    path: '',
    status: ''
  });

  const [detailVisible, setDetailVisible] = useState(false);
  const [detailMode, setDetailMode] = useState<'view' | 'edit'>('view');
  const [currentItem, setCurrentItem] = useState<Dictionary | null>(null);
  const [saving, setSaving] = useState(false);

  const [taskNotifyVisible, setTaskNotifyVisible] = useState(false);
  const [taskNotifyType, setTaskNotifyType] = useState<'import' | 'export'>('import');
  const [currentTaskId, setCurrentTaskId] = useState('');

  const [importModalVisible, setImportModalVisible] = useState(false);
  const [importing, setImporting] = useState(false);
  const [importFile, setImportFile] = useState<File | null>(null);

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const res = await getDictionaryList(queryParams);
      const responseData = res.data.data;
      setDataSource(responseData.list || []);
      setPagination({
        current: responseData.pageNum,
        pageSize: responseData.pageSize,
        total: responseData.total
      });
    } catch (error) {
      console.error('获取字典列表失败:', error);
    } finally {
      setLoading(false);
    }
  }, [queryParams]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  useEffect(() => {
    if (detailVisible && currentItem && detailMode === 'edit') {
      detailForm.setFieldsValue({
        code: currentItem.code,
        name: currentItem.name,
        value: currentItem.value,
        path: currentItem.path,
        description: currentItem.description
      });
    }
  }, [detailVisible, currentItem, detailMode, detailForm]);

  const handleSearch = () => {
    const newParams: DictionaryQueryParams = {
      pageNum: 1,
      pageSize: queryParams.pageSize
    };
    if (searchValues.code) newParams.code = searchValues.code;
    if (searchValues.name) newParams.name = searchValues.name;
    if (searchValues.path) newParams.path = searchValues.path;
    if (searchValues.status !== '') newParams.status = Number(searchValues.status);
    setQueryParams(newParams);
  };

  const handleReset = () => {
    setSearchValues({
      code: '',
      name: '',
      path: '',
      status: ''
    });
    setQueryParams({
      pageNum: 1,
      pageSize: queryParams.pageSize
    });
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
    setCurrentItem(null);
    setDetailMode('edit');
    detailForm.resetFields();
    setDetailVisible(true);
  };

  const handleView = (record: Dictionary) => {
    setCurrentItem(record);
    setDetailMode('view');
    detailForm.resetFields();
    setDetailVisible(true);
  };

  const handleEdit = (record: Dictionary) => {
    setCurrentItem(record);
    setDetailMode('edit');
    setDetailVisible(true);
  };

  const switchToEdit = () => {
    setDetailMode('edit');
  };

  const handleSave = async (values: DictionaryForm) => {
    setSaving(true);
    try {
      const submitData = currentItem ? values : { ...values, language: 1 };
      if (currentItem) {
        await updateDictionary(currentItem.id, submitData);
        message.success('编辑成功');
      } else {
        await createDictionary(submitData);
        message.success('新增成功');
      }
      setDetailVisible(false);
      fetchData();
    } catch (error: any) {
      console.error('保存字典失败:', error);
      message.error(error?.message || error?.response?.data?.messageZh || '保存失败，请重试');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (record: Dictionary) => {
    try {
      await deleteDictionary(record.id);
      message.success('删除成功');
      if (currentItem?.id === record.id) {
        setDetailVisible(false);
      }
      fetchData();
    } catch (error) {
      console.error('删除字典失败:', error);
    }
  };

  const handleToggleStatus = (record: Dictionary) => {
    const newStatus = record.status === 1 ? 0 : 1;
    const actionText = newStatus === 1 ? '生效' : '失效';
    Modal.confirm({
      title: `确认${actionText}`,
      content: `确定要${actionText}字典 "${record.name}" 吗？`,
      okText: '确定',
      cancelText: '取消',
      onOk: async () => {
        try {
          await updateDictionary(record.id, {
            code: record.code,
            name: record.name,
            value: record.value,
            path: record.path,
            description: record.description,
            status: newStatus
          });
          message.success(`${actionText}成功`);
          fetchData();
          if (currentItem?.id === record.id) {
            setCurrentItem({ ...currentItem, status: newStatus });
          }
        } catch (error) {
          console.error('切换状态失败:', error);
        }
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
          const res = await submitExportTask();
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

  const handleDownloadTemplate = () => {
    downloadImportTemplate().then((blob: any) => {
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = 'dictionary_import_template.xlsx';
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
      const res = await submitImportTask(importFile);
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
      title: '编码',
      dataIndex: 'code',
      key: 'code',
      width: 120
    },
    {
      title: '名称',
      dataIndex: 'name',
      key: 'name',
      width: 140
    },
    {
      title: '值',
      dataIndex: 'value',
      key: 'value',
      width: 150,
      ellipsis: true,
      render: (text: string) => text || '-'
    },
    {
      title: '描述',
      dataIndex: 'description',
      key: 'description',
      ellipsis: true,
      render: (text: string) => text || '-'
    },
    {
      title: '路径',
      dataIndex: 'path',
      key: 'path',
      width: 120,
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
      render: (_: any, record: Dictionary) => (
        <div className={styles.actions}>
          <button className={styles.actionLink} onClick={(e) => { e.stopPropagation(); handleEdit(record); }}>
            编辑
          </button>
          <button className={styles.actionLink} onClick={(e) => { e.stopPropagation(); handleToggleStatus(record); }}>
            {record.status === 1 ? '失效' : '生效'}
          </button>
          <button 
            className={`${styles.actionLink} ${styles.danger}`} 
            onClick={(e) => {
              e.stopPropagation();
              if (record.status === 1) {
                Modal.warning({
                  title: '无法删除',
                  content: '有效状态的数据不能删除，请先设置为失效状态'
                });
                return;
              }
              Modal.confirm({
                title: '确认删除',
                content: `确定要删除字典 "${record.name}" 吗？此操作不可恢复。`,
                okText: '确定',
                cancelText: '取消',
                onOk: () => handleDelete(record)
              });
            }}
          >
            删除
          </button>
        </div>
      )
    }
  ];

  return (
    <div className={styles.container}>
      <div className={styles.page}>
        <div className={styles.pageHead}>
          <div className={styles.pageHeadLeft}>
            <span className={styles.pageHeadTitle}>数据字典</span>
          </div>
        </div>

        <div className={styles.toolbar}>
          <div className={styles.toolbarLeft}>
            <div className={styles.searchWrap}>
              <span className={styles.searchLabel}>编码</span>
              <input
                type="text"
                className={styles.searchInput}
                placeholder="请输入"
                value={searchValues.code}
                onChange={(e) => setSearchValues({...searchValues, code: e.target.value})}
              />
            </div>
            <div className={styles.searchWrap}>
              <span className={styles.searchLabel}>名称</span>
              <input
                type="text"
                className={styles.searchInput}
                placeholder="请输入"
                value={searchValues.name}
                onChange={(e) => setSearchValues({...searchValues, name: e.target.value})}
              />
            </div>
            <div className={styles.searchWrap}>
              <span className={styles.searchLabel}>路径</span>
              <input
                type="text"
                className={styles.searchInput}
                placeholder="请输入"
                value={searchValues.path}
                onChange={(e) => setSearchValues({...searchValues, path: e.target.value})}
              />
            </div>
            <Select
              className={styles.filterSelect}
              value={searchValues.status || undefined}
              onChange={(value) => setSearchValues({...searchValues, status: value})}
              allowClear
              placeholder="全部状态"
              style={{ width: 100 }}
            >
              <Select.Option value={1}>有效</Select.Option>
              <Select.Option value={0}>失效</Select.Option>
            </Select>
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
              <PlusOutlined /> 新增
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
                  <tr key={record.id}>
                    <td onClick={(e) => e.stopPropagation()}>
                      <input type="checkbox" className={styles.checkbox} />
                    </td>
                    <td className={styles.linkText} onClick={() => handleView(record)}>
                      {record.code}
                    </td>
                    <td>{record.name}</td>
                    <td>{record.value || '-'}</td>
                    <td>{record.description || '-'}</td>
                    <td>{record.path || '-'}</td>
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
                        <button 
                          className={`${styles.actionLink} ${styles.danger}`} 
                          onClick={(e) => {
                            e.stopPropagation();
                            if (record.status === 1) {
                              Modal.warning({
                                title: '无法删除',
                                content: '有效状态的数据不能删除，请先设置为失效状态'
                              });
                              return;
                            }
                            Modal.confirm({
                              title: '确认删除',
                              content: `确定要删除字典 "${record.name}" 吗？此操作不可恢复。`,
                              okText: '确定',
                              cancelText: '取消',
                              onOk: () => handleDelete(record)
                            });
                          }}
                        >
                          删除
                        </button>
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

      <Drawer
        title={
          <div className={styles.drawerTitle}>
            <span>{currentItem ? '字典详情' : '新增字典'}</span>
            {currentItem && detailMode === 'view' && (
              <Button type="link" icon={<EditOutlined />} onClick={switchToEdit}>
                编辑
              </Button>
            )}
          </div>
        }
        width={520}
        onClose={() => setDetailVisible(false)}
        open={detailVisible}
        destroyOnClose
        bodyStyle={{ padding: 0 }}
        footer={
          detailMode === 'edit' ? (
            <div className={styles.drawerFooter}>
              <Button onClick={() => setDetailVisible(false)}>取消</Button>
              <Button type="primary" onClick={() => detailForm.submit()} loading={saving}>
                保存
              </Button>
            </div>
          ) : (
            <div className={styles.drawerFooter}>
              <Button onClick={() => setDetailVisible(false)}>关闭</Button>
            </div>
          )
        }
      >
        {detailMode === 'view' && currentItem ? (
          <div className={styles.detailPanelBody}>
            <div className={styles.detailCard}>
              <div className={styles.detailCardTitle}>📋 基本信息</div>
              <div className={styles.detailCardBody}>
                <div className={styles.detailRow}>
                  <span className={styles.detailLabel}>编码</span>
                  <span className={styles.detailValue}>{currentItem.code}</span>
                </div>
                <div className={styles.detailRow}>
                  <span className={styles.detailLabel}>名称</span>
                  <span className={styles.detailValue}>{currentItem.name}</span>
                </div>
                <div className={styles.detailRow}>
                  <span className={styles.detailLabel}>值</span>
                  <span className={styles.detailValue}>{currentItem.value || '-'}</span>
                </div>
                <div className={styles.detailRow}>
                  <span className={styles.detailLabel}>路径</span>
                  <span className={styles.detailValue}>{currentItem.path || '-'}</span>
                </div>
                <div className={styles.detailRow}>
                  <span className={styles.detailLabel}>描述</span>
                  <span className={styles.detailValue}>{currentItem.description || '-'}</span>
                </div>
                <div className={styles.detailRow}>
                  <span className={styles.detailLabel}>状态</span>
                  <span className={styles.detailValue}>
                    <span className={styles.statusBadge}>
                      <span className={`${styles.dot} ${currentItem.status === 1 ? styles.dotActive : styles.dotInactive}`}></span>
                      <span className={currentItem.status === 1 ? styles.labelActive : styles.labelInactive}>
                        {currentItem.status === 1 ? '有效' : '失效'}
                      </span>
                    </span>
                  </span>
                </div>
              </div>
            </div>

            <div className={styles.detailCard}>
              <div className={styles.detailCardTitle}>📝 系统信息</div>
              <div className={styles.detailCardBody}>
                <div className={styles.detailRow}>
                  <span className={styles.detailLabel}>创建人</span>
                  <span className={styles.detailValue}>{currentItem.createBy}</span>
                </div>
                <div className={styles.detailRow}>
                  <span className={styles.detailLabel}>创建时间</span>
                  <span className={styles.detailValue}>{currentItem.createTime}</span>
                </div>
                <div className={styles.detailRow}>
                  <span className={styles.detailLabel}>修改人</span>
                  <span className={styles.detailValue}>{currentItem.lastUpdateBy || '-'}</span>
                </div>
                <div className={styles.detailRow}>
                  <span className={styles.detailLabel}>修改时间</span>
                  <span className={styles.detailValue}>{currentItem.lastUpdateTime || '-'}</span>
                </div>
              </div>
            </div>
          </div>
        ) : (
          <Form form={detailForm} layout="vertical" onFinish={handleSave} className={styles.editForm}>
            <div className={styles.editFormRow}>
              <div className={styles.editFormItem}>
                <Form.Item name="code" label="编码" rules={[{ required: true, message: '请输入编码' }]}>
                  <Input placeholder="如: USER_STATUS" disabled={!!currentItem} />
                </Form.Item>
              </div>
              <div className={styles.editFormItem}>
                <Form.Item name="name" label="名称" rules={[{ required: true, message: '请输入名称' }]}>
                  <Input placeholder="如: 用户状态" />
                </Form.Item>
              </div>
            </div>
            <div className={styles.editFormRow}>
              <div className={styles.editFormItem}>
                <Form.Item name="value" label="值">
                  <Input placeholder="如: active" />
                </Form.Item>
              </div>
              <div className={styles.editFormItem}>
                <Form.Item name="path" label="路径">
                  <Input placeholder="如: system/user" disabled />
                </Form.Item>
              </div>
            </div>
            <Form.Item name="description" label="描述">
              <TextArea rows={3} placeholder="请输入描述..." showCount maxLength={4000} />
            </Form.Item>
          </Form>
        )}
      </Drawer>

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

export default DictionaryList;
