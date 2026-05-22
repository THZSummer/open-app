import React, { useState, useEffect } from 'react';
import {
  Form,
  Input,
  Button,
  Modal,
  message,
  Pagination,
  Badge,
  Popconfirm,
  Drawer,
  Upload
} from 'antd';
import { EditOutlined } from '@ant-design/icons';

import { useLocation, useNavigate } from 'react-router-dom';
import {
  getItemList,
  createItem,
  updateItem,
  deleteItem
} from './thunk';
import {
  DEFAULT_SEARCH_VALUES,
  DETAIL_MODE_VIEW,
  DETAIL_MODE_EDIT,
  STATUS_MAP,
  FORM_VALIDATION_RULES,
  DRAWER_WIDTH,
  TABLE_COLUMN_WIDTHS
} from './constant';
import { DEFAULT_PAGINATION, DEFAULT_QUERY_PARAMS, PAGE_SIZE_OPTIONS } from '../../../utils/constant';
import styles from './index.module.less';

const { TextArea } = Input;

const ItemList = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const [form] = Form.useForm();
  const [detailForm] = Form.useForm();

  const classifyInfo = location.state || {};
  const classifyId = classifyInfo.classifyId || '';
  const classifyCode = classifyInfo.classifyCode || '';
  const classifyName = classifyInfo.classifyName || '';

  const [loading, setLoading] = useState(false);
  const [dataSource, setDataSource] = useState([]);
  const [pagination, setPagination] = useState(DEFAULT_PAGINATION);

  const [queryParams, setQueryParams] = useState(DEFAULT_QUERY_PARAMS);

  const [searchItemCode, setSearchItemCode] = useState(DEFAULT_SEARCH_VALUES.itemCode);
  const [searchItemName, setSearchItemName] = useState(DEFAULT_SEARCH_VALUES.itemName);
  const [searchStatus, setSearchStatus] = useState(DEFAULT_SEARCH_VALUES.status);

  const [detailVisible, setDetailVisible] = useState(false);
  const [detailMode, setDetailMode] = useState(DETAIL_MODE_VIEW);
  const [currentItem, setCurrentItem] = useState(null);
  const [saving, setSaving] = useState(false);

  const fetchData = async () => {
    if (!classifyId) {
      message.warning('请先选择分类');
      navigate('/lookup-classify');
      return;
    }

    setLoading(true);
    try {
      const result = await getItemList(classifyId, queryParams);
      const responseData = result.data || {};
      setDataSource(responseData.list || []);
      setPagination({
        current: responseData.pageNum,
        pageSize: responseData.pageSize,
        total: Number(responseData.total) || 0
      });
    } catch (error) {
      console.error('获取项列表失败:', error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, [queryParams]);

  useEffect(() => {
    if (detailVisible && currentItem && detailMode === DETAIL_MODE_EDIT) {
      detailForm.setFieldsValue({
        itemCode: currentItem.itemCode,
        itemName: currentItem.itemName,
        itemValue: currentItem.itemValue,
        itemDesc: currentItem.itemDesc,
        itemIndex: currentItem.itemIndex,
        itemAttr1: currentItem.itemAttr1,
        itemAttr2: currentItem.itemAttr2,
        itemAttr3: currentItem.itemAttr3,
        itemAttr4: currentItem.itemAttr4,
        itemAttr5: currentItem.itemAttr5,
        itemAttr6: currentItem.itemAttr6
      });
    }
  }, [detailVisible, currentItem, detailMode, detailForm]);

  const handleSearch = () => {
    const newParams = {
      ...queryParams,
      itemCode: searchItemCode || undefined,
      itemName: searchItemName || undefined,
      status: searchStatus,
      pageNum: 1
    };
    setQueryParams(newParams);
  };

  const handleReset = () => {
    setSearchItemCode(DEFAULT_SEARCH_VALUES.itemCode);
    setSearchItemName(DEFAULT_SEARCH_VALUES.itemName);
    setSearchStatus(DEFAULT_SEARCH_VALUES.status);
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
    setCurrentItem(null);
    setDetailMode('edit');
    detailForm.resetFields();
    setDetailVisible(true);
  };

  const handleView = (record) => {
    setCurrentItem(record);
    setDetailMode('view');
    detailForm.resetFields();
    setDetailVisible(true);
  };

  const handleEdit = (record) => {
    setCurrentItem(record);
    setDetailMode('edit');
    setDetailVisible(true);
  };

  const switchToEdit = () => {
    setDetailMode('edit');
  };

  const handleSave = async (values) => {
    if (!classifyId) return;

    setSaving(true);
    try {
      if (currentItem) {
        await updateItem(currentItem.itemId, values);
        message.success('编辑成功');
      } else {
        await createItem(classifyId, values);
        message.success('新增成功');
      }
      setDetailVisible(false);
      fetchData();
    } catch (error) {
      console.error('保存项失败:', error);
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (record) => {
    try {
      await deleteItem(record.itemId);
      message.success('删除成功');
      if (currentItem?.itemId === record.itemId) {
        setDetailVisible(false);
      }
      fetchData();
    } catch (error) {
      console.error('删除项失败:', error);
    }
  };

  const handleToggleStatus = (record) => {
    const newStatus = record.status === 1 ? 0 : 1;
    const actionText = newStatus === 1 ? '生效' : '失效';
    Modal.confirm({
      title: '确认操作',
      content: `确定要${actionText} "${record.itemName}" 吗？`,
      okText: '确定',
      cancelText: '取消',
      onOk: async () => {
        try {
          await updateItem(record.itemId, {
            itemCode: record.itemCode,
            itemName: record.itemName,
            itemValue: record.itemValue,
            itemIndex: record.itemIndex,
            itemDesc: record.itemDesc || record.itemDesc,
            itemAttr1: record.itemAttr1,
            itemAttr2: record.itemAttr2,
            itemAttr3: record.itemAttr3,
            itemAttr4: record.itemAttr4,
            itemAttr5: record.itemAttr5,
            itemAttr6: record.itemAttr6,
            status: newStatus
          });
          message.success(`${actionText}成功`);
          fetchData();
          if (currentItem?.itemId === record.itemId) {
            setCurrentItem({ ...currentItem, status: newStatus });
          }
        } catch (error) {
          console.error('切换状态失败:', error);
        }
      }
    });
  };

  const handleBack = () => {
    navigate('/lookup-classify');
  };

  const columns = [
    {
      title: '项编码',
      dataIndex: 'itemCode',
      key: 'itemCode',
      width: 100
    },
    {
      title: '项名称',
      dataIndex: 'itemName',
      key: 'itemName',
      width: 120
    },
    {
      title: '项值',
      dataIndex: 'itemValue',
      key: 'itemValue',
      width: 100
    },
    {
      title: '描述',
      dataIndex: 'itemDesc',
      key: 'itemDesc',
      ellipsis: true,
      render: (text) => text || '-'
    },
    {
      title: '排序',
      dataIndex: 'itemIndex',
      key: 'itemIndex',
      width: 60,
      align: 'center'
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 80,
      render: (status) => React.createElement(Badge, {
        status: STATUS_MAP[status]?.color,
        text: STATUS_MAP[status]?.text
      })
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
      width: 180,
      fixed: 'right',
      render: (_, record) => React.createElement('div', { className: styles.actions },
        React.createElement('button', { className: styles.actionLink, onClick: () => handleEdit(record) }, '编辑'),
        React.createElement('button', { className: styles.actionLink, onClick: () => handleToggleStatus(record) },
          record.status === 1 ? '失效' : '生效'
        ),
        React.createElement(Popconfirm, {
          title: `确定要删除项 "${record.itemName}" 吗？`,
          onConfirm: () => handleDelete(record),
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

  const renderTableBody = () => {
    if (loading) {
      return React.createElement('tr', null,
        React.createElement('td', { colSpan: columns.length, style: { textAlign: 'center', padding: '60px 0' } }, '加载中...')
      );
    }

    if (dataSource.length === 0) {
      return React.createElement('tr', null,
        React.createElement('td', { colSpan: columns.length },
          React.createElement('div', { style: { textAlign: 'center', padding: '60px 0', color: 'var(--text-hint)' } }, '暂无数据')
        )
      );
    }

    return dataSource.map((record, index) =>
      React.createElement('tr', { key: record.itemId },
        React.createElement('td', null,
          React.createElement('span', {
            className: styles.linkText,
            onClick: () => handleView(record)
          }, record.itemCode)
        ),
        React.createElement('td', null, record.itemName),
        React.createElement('td', null, record.itemValue),
        React.createElement('td', null, record.itemDesc || '-'),
        React.createElement('td', { style: { textAlign: 'center' } }, record.itemIndex),
        React.createElement('td', null,
          React.createElement('span', { className: styles.statusBadge },
            React.createElement('span', { className: `${styles.dot} ${record.status === 1 ? styles.dotActive : styles.dotInactive}` }),
            React.createElement('span', { className: record.status === 1 ? styles.labelActive : styles.labelInactive },
              record.status === 1 ? '有效' : '失效'
            )
          )
        ),
        React.createElement('td', null, record.createBy),
        React.createElement('td', null, record.createTime),
        React.createElement('td', null, record.lastUpdateBy || '-'),
        React.createElement('td', null, record.lastUpdateTime || '-'),
        React.createElement('td', null,
          React.createElement('div', { className: styles.actions },
            React.createElement('button', { className: styles.actionLink, onClick: () => handleEdit(record) }, '编辑'),
            React.createElement('button', { className: styles.actionLink, onClick: () => handleToggleStatus(record) },
              record.status === 1 ? '失效' : '生效'
            ),
            React.createElement('button', {
              className: `${styles.actionLink} ${styles.danger}`,
              onClick: () => {
                if (record.status === 1) {
                  Modal.warning({
                    title: '无法删除',
                    content: '有效状态的项不能删除，请先设置为失效状态'
                  });
                  return;
                }
                Modal.confirm({
                  title: '确认删除',
                  content: `确定要删除项 "${record.itemName}" 吗？此操作不可恢复。`,
                  okText: '确定',
                  cancelText: '取消',
                  onOk: () => handleDelete(record)
                });
              }
            }, '删除')
          )
        )
      )
    );
  };

  const renderDetailView = () => {
    if (!currentItem) return null;
    
    return React.createElement('div', { className: styles.detailPanelBody },
      React.createElement('div', { className: styles.detailCard },
        React.createElement('div', { className: styles.detailCardTitle }, '📋 基本信息'),
        React.createElement('div', { className: styles.detailCardBody },
          React.createElement('div', { className: styles.detailRow },
            React.createElement('span', { className: styles.detailLabel }, '所属分类'),
            React.createElement('span', { className: styles.detailValue }, classifyName)
          ),
          React.createElement('div', { className: styles.detailRow },
            React.createElement('span', { className: styles.detailLabel }, '项编码'),
            React.createElement('span', { className: styles.detailValue }, currentItem.itemCode)
          ),
          React.createElement('div', { className: styles.detailRow },
            React.createElement('span', { className: styles.detailLabel }, '项名称'),
            React.createElement('span', { className: styles.detailValue }, currentItem.itemName)
          ),
          React.createElement('div', { className: styles.detailRow },
            React.createElement('span', { className: styles.detailLabel }, '项值'),
            React.createElement('span', { className: styles.detailValue }, currentItem.itemValue)
          ),
          React.createElement('div', { className: styles.detailRow },
            React.createElement('span', { className: styles.detailLabel }, '描述'),
            React.createElement('span', { className: styles.detailValue }, currentItem.itemDesc || '-')
          ),
          React.createElement('div', { className: styles.detailRow },
            React.createElement('span', { className: styles.detailLabel }, '排序'),
            React.createElement('span', { className: styles.detailValue }, currentItem.itemIndex)
          ),
          React.createElement('div', { className: styles.detailRow },
            React.createElement('span', { className: styles.detailLabel }, '状态'),
            React.createElement('span', { className: styles.detailValue },
              React.createElement('span', { className: styles.statusBadge },
                React.createElement('span', { className: `${styles.dot} ${currentItem.status === 1 ? styles.dotActive : styles.dotInactive}` }),
                React.createElement('span', { className: currentItem.status === 1 ? styles.labelActive : styles.labelInactive },
                  currentItem.status === 1 ? '有效' : '失效'
                )
              )
            )
          )
        )
      ),
      React.createElement('div', { className: styles.detailCard },
        React.createElement('div', { className: styles.detailCardTitle }, '⚙️ 扩展属性'),
        React.createElement('div', { className: styles.detailCardBody },
          React.createElement('div', { className: styles.detailRow },
            React.createElement('span', { className: styles.detailLabel }, '属性1'),
            React.createElement('span', { className: styles.detailValue }, currentItem.itemAttr1 || '-')
          ),
          React.createElement('div', { className: styles.detailRow },
            React.createElement('span', { className: styles.detailLabel }, '属性2'),
            React.createElement('span', { className: styles.detailValue }, currentItem.itemAttr2 || '-')
          ),
          React.createElement('div', { className: styles.detailRow },
            React.createElement('span', { className: styles.detailLabel }, '属性3'),
            React.createElement('span', { className: styles.detailValue }, currentItem.itemAttr3 || '-')
          ),
          React.createElement('div', { className: styles.detailRow },
            React.createElement('span', { className: styles.detailLabel }, '属性4'),
            React.createElement('span', { className: styles.detailValue }, currentItem.itemAttr4 || '-')
          ),
          React.createElement('div', { className: styles.detailRow },
            React.createElement('span', { className: styles.detailLabel }, '属性5'),
            React.createElement('span', { className: styles.detailValue }, currentItem.itemAttr5 || '-')
          ),
          React.createElement('div', { className: styles.detailRow },
            React.createElement('span', { className: styles.detailLabel }, '属性6'),
            React.createElement('span', { className: styles.detailValue }, currentItem.itemAttr6 || '-')
          )
        )
      ),
      React.createElement('div', { className: styles.detailCard },
        React.createElement('div', { className: styles.detailCardTitle }, '📝 系统信息'),
        React.createElement('div', { className: styles.detailCardBody },
          React.createElement('div', { className: styles.detailRow },
            React.createElement('span', { className: styles.detailLabel }, '创建人'),
            React.createElement('span', { className: styles.detailValue }, currentItem.createBy)
          ),
          React.createElement('div', { className: styles.detailRow },
            React.createElement('span', { className: styles.detailLabel }, '创建时间'),
            React.createElement('span', { className: styles.detailValue }, currentItem.createTime)
          ),
          React.createElement('div', { className: styles.detailRow },
            React.createElement('span', { className: styles.detailLabel }, '修改人'),
            React.createElement('span', { className: styles.detailValue }, currentItem.lastUpdateBy || '-')
          ),
          React.createElement('div', { className: styles.detailRow },
            React.createElement('span', { className: styles.detailLabel }, '修改时间'),
            React.createElement('span', { className: styles.detailValue }, currentItem.lastUpdateTime || '-')
          )
        )
      )
    );
  };

  const renderDetailEdit = () => {
    return React.createElement(Form, {
      form: detailForm,
      layout: 'vertical',
      onFinish: handleSave,
      className: styles.editForm
    },
      React.createElement('div', { className: styles.editFormRow },
        React.createElement('div', { className: styles.editFormItem },
          React.createElement(Form.Item, {
            name: 'itemCode',
            label: '项编码',
            rules: [{ required: true, message: '请输入项编码' }]
          },
            React.createElement(Input, { placeholder: '如: ADMIN', disabled: !!currentItem })
          )
        ),
        React.createElement('div', { className: styles.editFormItem },
          React.createElement(Form.Item, {
            name: 'itemName',
            label: '项名称',
            rules: [{ required: true, message: '请输入项名称' }]
          },
            React.createElement(Input, { placeholder: '如: 管理员' })
          )
        )
      ),
      React.createElement('div', { className: styles.editFormRow },
        React.createElement('div', { className: styles.editFormItem },
          React.createElement(Form.Item, {
            name: 'itemValue',
            label: '项值',
            rules: [{ required: true, message: '请输入项值' }]
          },
            React.createElement(Input, { placeholder: '如: 1' })
          )
        ),
        React.createElement('div', { className: styles.editFormItem },
          React.createElement(Form.Item, {
            name: 'itemIndex',
            label: '排序',
            initialValue: 1
          },
            React.createElement(Input, { type: 'number', placeholder: '数字越小越靠前' })
          )
        )
      ),
      React.createElement(Form.Item, {
        name: 'itemDesc',
        label: '描述'
      },
        React.createElement(TextArea, { rows: 3, placeholder: '请输入描述...', showCount: true, maxLength: 500 })
      ),
      React.createElement('div', { style: { borderTop: '1px solid var(--border)', margin: '12px 0', paddingTop: '12px' } },
        React.createElement('span', { style: { fontSize: '12px', color: 'var(--text-hint)', fontWeight: 500 } }, '扩展属性')
      ),
      React.createElement(Form.Item, { name: 'itemAttr1', label: '属性1' },
        React.createElement(TextArea, { rows: 2, placeholder: '扩展属性1', style: { width: '100%', resize: 'vertical' } })
      ),
      React.createElement(Form.Item, { name: 'itemAttr2', label: '属性2' },
        React.createElement(TextArea, { rows: 2, placeholder: '扩展属性2', style: { width: '100%', resize: 'vertical' } })
      ),
      React.createElement(Form.Item, { name: 'itemAttr3', label: '属性3' },
        React.createElement(TextArea, { rows: 2, placeholder: '扩展属性3', style: { width: '100%', resize: 'vertical' } })
      ),
      React.createElement(Form.Item, { name: 'itemAttr4', label: '属性4' },
        React.createElement(TextArea, { rows: 2, placeholder: '扩展属性4', style: { width: '100%', resize: 'vertical' } })
      ),
      React.createElement(Form.Item, { name: 'itemAttr5', label: '属性5' },
        React.createElement(TextArea, { rows: 2, placeholder: '扩展属性5', style: { width: '100%', resize: 'vertical' } })
      ),
      React.createElement(Form.Item, { name: 'itemAttr6', label: '属性6' },
        React.createElement(TextArea, { rows: 2, placeholder: '扩展属性6', style: { width: '100%', resize: 'vertical' } })
      )
    );
  };

  const renderDrawerFooter = () => {
    if (detailMode === 'edit') {
      return React.createElement('div', { className: styles.drawerFooter },
        React.createElement(Button, { onClick: () => setDetailVisible(false) }, '取消'),
        React.createElement(Button, { type: 'primary', onClick: () => detailForm.submit(), loading: saving }, '保存')
      );
    }
    return React.createElement('div', { className: styles.drawerFooter },
      React.createElement(Button, { onClick: () => setDetailVisible(false) }, '关闭')
    );
  };

  return React.createElement('div', { className: styles.container },
    React.createElement('div', { className: styles.page },
      React.createElement('div', { className: styles.pageHead },
        React.createElement('div', { className: styles.pageHeadLeft },
          React.createElement('button', { className: styles.backBtn, onClick: handleBack }, '← 返回'),
          React.createElement('h2', { className: styles.pageHeadTitle }, 'LookUp项列表')
        ),
        React.createElement('div', { className: styles.classifyInfo },
          React.createElement('span', { className: styles.infoTag },
            React.createElement('span', { className: styles.infoLabel }, '分类编码:'),
            React.createElement('span', { className: styles.infoValue }, classifyCode)
          ),
          React.createElement('span', { className: styles.infoTag },
            React.createElement('span', { className: styles.infoLabel }, '分类名称:'),
            React.createElement('span', { className: styles.infoValue }, classifyName)
          )
        )
      ),
      React.createElement('div', { className: styles.toolbar },
        React.createElement('div', { className: styles.toolbarLeft },
          React.createElement('div', { className: styles.searchArea },
            React.createElement('input', {
              type: 'text',
              className: styles.searchInput,
              placeholder: '项编码',
              value: searchItemCode,
              onChange: (e) => setSearchItemCode(e.target.value)
            }),
            React.createElement('input', {
              type: 'text',
              className: styles.searchInput,
              placeholder: '项名称',
              value: searchItemName,
              onChange: (e) => setSearchItemName(e.target.value)
            }),
            React.createElement('select', {
              className: styles.filterSelect,
              value: searchStatus ?? '',
              onChange: (e) => setSearchStatus(e.target.value ? Number(e.target.value) : undefined)
            },
              React.createElement('option', { value: '' }, '全部状态'),
              React.createElement('option', { value: 1 }, '有效'),
              React.createElement('option', { value: 0 }, '失效')
            ),
            React.createElement('button', {
              className: `${styles.btn} ${styles.btnPrimary} ${styles.btnSm}`,
              onClick: handleSearch
            }, '查询'),
            React.createElement('button', {
              className: `${styles.btn} ${styles.btnOutline} ${styles.btnSm}`,
              onClick: handleReset
            }, '↻ 重置')
          )
        ),
        React.createElement('div', { className: styles.toolbarRight },
          React.createElement('button', {
            className: `${styles.btn} ${styles.btnPrimary} ${styles.btnSm}`,
            onClick: handleAdd
          }, '+ 新增LookUp项')
        )
      ),
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
            pageSizeOptions: ['10', '20', '50', '100', '200', '500', '1000']
          })
        )
      )
    ),
    React.createElement(Drawer, {
      title: React.createElement('div', { className: styles.drawerTitle },
        React.createElement('span', null, currentItem ? 'LookUp项详情' : '新增LookUp项'),
        currentItem && detailMode === DETAIL_MODE_VIEW && React.createElement(Button, {
          type: 'link',
          icon: React.createElement(EditOutlined),
          onClick: switchToEdit
        }, '编辑')
      ),
      width: DRAWER_WIDTH,
      onClose: () => setDetailVisible(false),
      open: detailVisible,
      bodyStyle: { padding: 0 },
      footer: renderDrawerFooter()
    },
      detailMode === DETAIL_MODE_VIEW && currentItem ? renderDetailView() : renderDetailEdit()
    )
  );
};

export default ItemList;
