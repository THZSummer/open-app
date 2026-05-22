import React, { useState, useEffect } from 'react';
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
  deleteClassify
} from './thunk';

import {
  exportItemsAsync,
  importItemsAsync,
  downloadImportTemplate
} from '../lookup-item/thunk';
import {
  DEFAULT_SEARCH_VALUES,
  MODAL_TITLE_ADD,
  MODAL_TITLE_EDIT,
  TASK_NOTIFY_TYPE_IMPORT,
  TASK_NOTIFY_TYPE_EXPORT,
  STATUS_MAP,
  FORM_VALIDATION_RULES,
  TABLE_COLUMN_WIDTHS
} from './constant';
import { DEFAULT_PAGINATION, DEFAULT_QUERY_PARAMS, PAGE_SIZE_OPTIONS } from '../../../utils/constant';
import styles from './index.module.less';

const ClassifyList = () => {
  const navigate = useNavigate();
  const [form] = Form.useForm();
  const [modalForm] = Form.useForm();

  const [loading, setLoading] = useState(false);
  const [dataSource, setDataSource] = useState([]);
  const [pagination, setPagination] = useState(DEFAULT_PAGINATION);

  const [queryParams, setQueryParams] = useState(DEFAULT_QUERY_PARAMS);

  const [searchValues, setSearchValues] = useState(DEFAULT_SEARCH_VALUES);

  const [modalVisible, setModalVisible] = useState(false);
  const [modalTitle, setModalTitle] = useState(MODAL_TITLE_ADD);
  const [editingId, setEditingId] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  const [taskNotifyVisible, setTaskNotifyVisible] = useState(false);
  const [taskNotifyType, setTaskNotifyType] = useState('import');
  const [currentTaskId, setCurrentTaskId] = useState('');

  const fetchData = async () => {
    setLoading(true);
    try {
      const result = await getClassifyList(queryParams);
      const responseData = result.data || {};
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
  };

  useEffect(() => {
    fetchData();
  }, [queryParams]);

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

  const handlePageChange = (page, pageSize) => {
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

  const handleEdit = (record) => {
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

  const handleSave = async (values) => {
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

  const handleDelete = async (id) => {
    try {
      await deleteClassify(id);
      message.success('删除成功');
      fetchData();
    } catch (error) {
      console.error('删除分类失败:', error);
    }
  };

  const handleToggleStatus = (record) => {
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

  const handleRowClick = (record) => {
    navigate('/lookup-item', {
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
          const result = await exportItemsAsync({});
          const taskId = String(result.data?.taskId || '');
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
  const [importFile, setImportFile] = useState(null);

  const handleDownloadTemplate = () => {
    downloadImportTemplate().then((blob) => {
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = 'lookup_import_template.xlsx';
      a.click();
      window.URL.revokeObjectURL(url);
    });
  };

  const handleFileChange = (info) => {
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
      const result = await importItemsAsync('', importFile);
      const taskId = String(result.data?.taskId || '');
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
      title: React.createElement('input', { type: 'checkbox', className: styles.checkbox }),
      key: 'checkbox',
      width: 40,
      render: () => React.createElement('span', { onClick: (e) => e.stopPropagation() },
        React.createElement('input', { type: 'checkbox', className: styles.checkbox })
      )
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
      render: (text) => text || '-'
    },
    {
      title: '描述',
      dataIndex: 'classifyDesc',
      key: 'classifyDesc',
      ellipsis: true,
      render: (text) => text || '-'
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: TABLE_COLUMN_WIDTHS.status,
      render: (status) => {
        const statusInfo = STATUS_MAP[status];
        return React.createElement('div', { className: styles.statusBadge },
          React.createElement('span', { className: `${styles.dot} ${statusInfo?.dotClass || ''}` }),
          React.createElement('span', { className: `${styles.label} ${statusInfo?.labelClass || ''}` },
            statusInfo?.text || '-'
          )
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
      fixed: 'right',
      render: (_, record) => React.createElement('div', { className: styles.actions },
        React.createElement('button', {
          className: styles.actionLink,
          onClick: (e) => { e.stopPropagation(); handleEdit(record); }
        }, '编辑'),
        React.createElement('button', {
          className: styles.actionLink,
          onClick: (e) => { e.stopPropagation(); handleToggleStatus(record); }
        }, record.status === 1 ? '失效' : '生效'),
        React.createElement(Popconfirm, {
          title: `确定要删除分类 "${record.classifyName}" 吗？`,
          onConfirm: () => handleDelete(record.classifyId),
          okText: '确定',
          cancelText: '取消'
        },
          React.createElement('button', {
            className: `${styles.actionLink} ${styles.danger}`,
            onClick: (e) => e.stopPropagation()
          }, '删除')
        )
      )
    }
  ];

  const renderSearchBar = () => {
    return React.createElement('div', { className: styles.toolbar },
      React.createElement('div', { className: styles.toolbarLeft },
        React.createElement('div', { className: styles.searchWrap },
          React.createElement('span', { className: styles.searchLabel }, '类别编码'),
          React.createElement('input', {
            type: 'text',
            className: styles.searchInput,
            placeholder: '请输入',
            value: searchValues.classifyCode,
            onChange: (e) => setSearchValues({...searchValues, classifyCode: e.target.value})
          })
        ),
        React.createElement('div', { className: styles.searchWrap },
          React.createElement('span', { className: styles.searchLabel }, '类别名称'),
          React.createElement('input', {
            type: 'text',
            className: styles.searchInput,
            placeholder: '请输入',
            value: searchValues.classifyName,
            onChange: (e) => setSearchValues({...searchValues, classifyName: e.target.value})
          })
        ),
        React.createElement('div', { className: styles.searchWrap },
          React.createElement('span', { className: styles.searchLabel }, '描述'),
          React.createElement('input', {
            type: 'text',
            className: styles.searchInput,
            placeholder: '请输入',
            style: { width: 100 },
            value: searchValues.classifyDesc,
            onChange: (e) => setSearchValues({...searchValues, classifyDesc: e.target.value})
          })
        ),
        React.createElement('select', {
          className: styles.filterSelect,
          value: searchValues.status,
          onChange: (e) => setSearchValues({...searchValues, status: e.target.value})
        },
          React.createElement('option', { value: '' }, '全部状态'),
          React.createElement('option', { value: 1 }, '有效'),
          React.createElement('option', { value: 0 }, '失效')
        ),
        React.createElement('button', {
          className: `${styles.btn} ${styles.btnPrimary} ${styles.btnSm}`,
          onClick: handleSearch
        }, React.createElement(SearchOutlined), ' 查询'),
        React.createElement('button', {
          className: `${styles.btn} ${styles.btnOutline} ${styles.btnSm}`,
          onClick: handleReset
        }, React.createElement(ReloadOutlined), ' 重置')
      ),
      React.createElement('div', { className: styles.toolbarRight },
        React.createElement('button', {
          className: `${styles.btn} ${styles.btnOutline} ${styles.btnSm}`,
          onClick: handleExport
        }, React.createElement(DownloadOutlined), ' 导出'),
        React.createElement('button', {
          className: `${styles.btn} ${styles.btnOutline} ${styles.btnSm}`,
          onClick: handleImport
        }, React.createElement(UploadOutlined), ' 导入'),
        React.createElement('button', {
          className: `${styles.btn} ${styles.btnPrimary} ${styles.btnSm}`,
          onClick: handleAdd
        }, React.createElement(PlusOutlined), ' 新增分类')
      )
    );
  };

  const renderTableBody = () => {
    if (loading) {
      return React.createElement('tr', null,
        React.createElement('td', { colSpan: columns.length, style: { textAlign: 'center', padding: '60px 0' } }, '加载中...')
      );
    }
    
    if (dataSource.length === 0) {
      return React.createElement('tr', null,
        React.createElement('td', { colSpan: columns.length },
          React.createElement('div', { className: styles.empty },
            React.createElement('div', { className: styles.icon }, '📋'),
            React.createElement('p', null, '暂无数据')
          )
        )
      );
    }

    return dataSource.map((record) =>
      React.createElement('tr', { key: record.classifyId },
        React.createElement('td', { onClick: (e) => e.stopPropagation() },
          React.createElement('input', { type: 'checkbox', className: styles.checkbox })
        ),
        React.createElement('td', { className: styles.linkText, onClick: () => handleRowClick(record) }, record.classifyCode),
        React.createElement('td', null, record.classifyName),
        React.createElement('td', null, record.path || '-'),
        React.createElement('td', null, record.classifyDesc || '-'),
        React.createElement('td', null,
          React.createElement('div', { className: styles.statusBadge },
            React.createElement('span', { className: `${styles.dot} ${STATUS_MAP[record.status]?.dotClass || ''}` }),
            React.createElement('span', { className: `${styles.label} ${STATUS_MAP[record.status]?.labelClass || ''}` },
              STATUS_MAP[record.status]?.text || '-'
            )
          )
        ),
        React.createElement('td', null, record.createBy),
        React.createElement('td', null, record.createTime),
        React.createElement('td', null, record.lastUpdateBy),
        React.createElement('td', null, record.lastUpdateTime),
        React.createElement('td', { onClick: (e) => e.stopPropagation() },
          React.createElement('div', { className: styles.actions },
            React.createElement('button', {
              className: styles.actionLink,
              onClick: (e) => { e.stopPropagation(); handleEdit(record); }
            }, '编辑'),
            React.createElement('button', {
              className: styles.actionLink,
              onClick: (e) => { e.stopPropagation(); handleToggleStatus(record); }
            }, record.status === 1 ? '失效' : '生效'),
            React.createElement(Popconfirm, {
              title: `确定要删除分类 "${record.classifyName}" 吗？`,
              onConfirm: () => handleDelete(record.classifyId),
              okText: '确定',
              cancelText: '取消'
            },
              React.createElement('button', {
                className: `${styles.actionLink} ${styles.danger}`,
                onClick: (e) => e.stopPropagation()
              }, '删除')
            )
          )
        )
      )
    );
  };

  const renderModalForm = () => {
    return React.createElement(Modal, {
      title: modalTitle,
      open: modalVisible,
      onOk: () => modalForm.submit(),
      onCancel: () => setModalVisible(false),
      confirmLoading: submitting,
      width: 520,
      okText: '保存',
      cancelText: '取消'
    },
      React.createElement(Form, {
        form: modalForm,
        layout: 'vertical',
        onFinish: handleSave
      },
        React.createElement('div', { style: { display: 'flex', gap: 16 } },
          React.createElement(Form.Item, {
            name: 'classifyCode',
            label: '分类编码',
            rules: [
              { required: true, message: '请输入分类编码' },
              { max: 100, message: '最多100个字符' }
            ],
            style: { flex: 1 }
          },
            React.createElement(Input, { placeholder: '如: USER_TYPE', disabled: !!editingId })
          ),
          React.createElement(Form.Item, {
            name: 'classifyName',
            label: '分类名称',
            rules: [
              { required: true, message: '请输入分类名称' },
              { max: 100, message: '最多100个字符' }
            ],
            style: { flex: 1 }
          },
            React.createElement(Input, { placeholder: '如: 用户类型' })
          )
        ),
        React.createElement(Form.Item, {
          name: 'path',
          label: '路径',
          rules: [{ max: 100, message: '最多100个字符' }]
        },
          React.createElement(Input, {
            placeholder: '如: system/user_type，用于层级归类',
            disabled: !!editingId
          })
        ),
        React.createElement('div', { className: styles.formHint }, '路径可用于分类的层级管理，斜杠分隔'),
        React.createElement(Form.Item, {
          name: 'classifyDesc',
          label: '描述',
          rules: [{ max: 500, message: '最多500个字符' }]
        },
          React.createElement(Input.TextArea, {
            rows: 3,
            placeholder: '请输入分类描述...',
            maxLength: 500
          })
        )
      )
    );
  };

  const renderTaskNotifyModal = () => {
    return React.createElement(Modal, {
      title: null,
      footer: null,
      open: taskNotifyVisible,
      onCancel: () => setTaskNotifyVisible(false),
      width: 400,
      centered: true
    },
      React.createElement('div', { style: { textAlign: 'center', padding: '20px 0' } },
        React.createElement('div', { style: { fontSize: '48px', marginBottom: '16px' } },
          taskNotifyType === TASK_NOTIFY_TYPE_IMPORT ? '📥' : '📤'
        ),
        React.createElement('div', { style: { fontSize: '16px', fontWeight: 600, marginBottom: '8px' } },
          taskNotifyType === TASK_NOTIFY_TYPE_IMPORT ? '导入任务已提交' : '导出任务已提交'
        ),
        React.createElement('div', { style: { fontSize: '13px', color: 'var(--text-secondary)' } },
          '任务ID: ', currentTaskId
        ),
        React.createElement('div', { style: { fontSize: '13px', color: 'var(--text-secondary)', marginTop: '8px' } },
          '请在任务中心查看进度'
        ),
        React.createElement(Button, {
          type: 'primary',
          style: { marginTop: '20px' },
          onClick: () => {
            setTaskNotifyVisible(false);
            navigate('/task-center');
          }
        }, '查看任务中心')
      )
    );
  };

  const renderImportModal = () => {
    return React.createElement(Modal, {
      title: '批量导入',
      open: importModalVisible,
      onCancel: () => {
        setImportModalVisible(false);
        setImportFile(null);
      },
      onOk: handleImportSubmit,
      confirmLoading: importing,
      width: 520,
      okText: '确认导入',
      cancelText: '取消'
    },
      React.createElement('div', { style: { padding: '8px 0' } },
        React.createElement('div', { style: { textAlign: 'right', marginBottom: '12px' } },
          React.createElement('a', {
            onClick: handleDownloadTemplate,
            style: { color: 'var(--primary)', cursor: 'pointer', fontSize: '13px', textDecoration: 'none' },
            onMouseEnter: (e) => e.currentTarget.style.textDecoration = 'underline',
            onMouseLeave: (e) => e.currentTarget.style.textDecoration = 'none'
          },
            React.createElement(DownloadOutlined, { style: { marginRight: '4px' } }),
            '下载 Excel 导入模板'
          )
        ),
        React.createElement(Upload.Dragger, {
          name: 'file',
          accept: '.xlsx,.xls',
          showUploadList: false,
          onChange: handleFileChange,
          beforeUpload: () => false
        },
          React.createElement('div', { style: { padding: '20px 0' } },
            React.createElement('div', { style: { fontSize: '32px', marginBottom: '8px', opacity: 0.3 } }, '📄'),
            React.createElement('div', { style: { fontSize: '14px', color: 'var(--text-secondary)', marginBottom: '4px' } },
              '点击选择或拖拽 Excel 文件到此处'
            ),
            React.createElement('div', { style: { fontSize: '12px', color: 'var(--text-hint)' } },
              '支持 .xlsx / .xls 格式，单次最多导入1000条数据'
            )
          )
        ),
        importFile && React.createElement('div', { style: { marginTop: '12px', fontSize: '13px', color: 'var(--text-secondary)' } },
          '已选择: ', importFile.name
        )
      )
    );
  };

  return React.createElement('div', { className: styles.container },
    React.createElement('div', { className: styles.page },
      React.createElement('div', { className: styles.pageHead },
        React.createElement('div', { className: styles.pageHeadLeft },
          React.createElement('span', { className: styles.pageHeadTitle }, 'LookUp分类')
        )
      ),
      renderSearchBar(),
      React.createElement('div', { className: styles.tableWrap },
        React.createElement('table', { className: styles.table },
          React.createElement('thead', null,
            React.createElement('tr', null,
              columns.map((col, index) =>
                React.createElement('th', { key: index, style: { width: col.width } }, col.title)
              )
            )
          ),
          React.createElement('tbody', null, renderTableBody())
        )
      ),
      React.createElement('div', { className: styles.pagination },
        React.createElement('div', { className: styles.paginationLeft },
          React.createElement('span', { className: styles.paginationInfo }, '共 ', pagination.total, ' 条')
        ),
        React.createElement('div', { className: styles.paginationRight },
          React.createElement(Pagination, {
            current: pagination.current,
            pageSize: pagination.pageSize,
            total: pagination.total,
            showSizeChanger: true,
            showQuickJumper: true,
            onChange: handlePageChange,
            pageSizeOptions: PAGE_SIZE_OPTIONS
          })
        )
      )
    ),
    renderModalForm(),
    renderTaskNotifyModal(),
    renderImportModal()
  );
};

export default ClassifyList;
