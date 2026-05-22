import React, { useState, useEffect, useCallback } from 'react';
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
  deleteItem,
  type LookUpItem,
  type ItemForm,
  type ItemQueryParams
} from '@/api/lookup';
import styles from './index.module.less';

const { TextArea } = Input;

/**
 * 状态映射
 */
const statusMap = {
  1: { text: '有效', color: 'success' },
  0: { text: '失效', color: 'default' }
};

/**
 * LookUp 项列表页面
 */
const ItemList: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const [form] = Form.useForm();
  const [detailForm] = Form.useForm();

  // 从路由状态获取分类信息
  const classifyInfo = (location.state as any) || {};
  const classifyId = classifyInfo.classifyId || '';
  const classifyCode = classifyInfo.classifyCode || '';
  const classifyName = classifyInfo.classifyName || '';

  // 列表数据状态
  const [loading, setLoading] = useState(false);
  const [dataSource, setDataSource] = useState<LookUpItem[]>([]);
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0
  });

  // 查询参数
  const [queryParams, setQueryParams] = useState<ItemQueryParams>({
    pageNum: 1,
    pageSize: 10
  });

  // 搜索参数状态
  const [searchItemCode, setSearchItemCode] = useState('');
  const [searchItemName, setSearchItemName] = useState('');
  const [searchStatus, setSearchStatus] = useState<number | undefined>();

  // 详情面板状态
  const [detailVisible, setDetailVisible] = useState(false);
  const [detailMode, setDetailMode] = useState<'view' | 'edit'>('view');
  const [currentItem, setCurrentItem] = useState<LookUpItem | null>(null);
  const [saving, setSaving] = useState(false);

  /**
   * 获取项列表
   */
  const fetchData = useCallback(async () => {
    if (!classifyId) {
      message.warning('请先选择分类');
      navigate('/lookup/classify');
      return;
    }

    setLoading(true);
    try {
      const res = await getItemList(classifyId, queryParams);
      const responseData = res.data.data;
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
  }, [classifyId, queryParams, navigate]);

  /**
   * 初始化加载数据
   */
  useEffect(() => {
    fetchData();
  }, [fetchData]);

  /**
   * 监听抽屉打开状态，当编辑模式时设置表单值
   */
  useEffect(() => {
    if (detailVisible && currentItem && detailMode === 'edit') {
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

  /**
   * 处理查询
   */
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

  /**
   * 重置查询
   */
  const handleReset = () => {
    setSearchItemCode('');
    setSearchItemName('');
    setSearchStatus(undefined);
    const newParams = {
      pageNum: 1,
      pageSize: queryParams.pageSize
    };
    setQueryParams(newParams);
  };

  /**
   * 处理分页变化
   */
  const handlePageChange = (page: number, pageSize?: number) => {
    const newParams = {
      ...queryParams,
      pageNum: page,
      pageSize: pageSize || queryParams.pageSize
    };
    setQueryParams(newParams);
  };

  /**
   * 打开详情面板 - 新增
   */
  const handleAdd = () => {
    setCurrentItem(null);
    setDetailMode('edit');
    detailForm.resetFields();
    setDetailVisible(true);
  };

  /**
   * 打开详情面板 - 查看
   */
  const handleView = (record: LookUpItem) => {
    setCurrentItem(record);
    setDetailMode('view');
    detailForm.resetFields();
    setDetailVisible(true);
  };

  /**
   * 打开详情面板 - 编辑
   */
  const handleEdit = (record: LookUpItem) => {
    setCurrentItem(record);
    setDetailMode('edit');
    setDetailVisible(true);
  };

  /**
   * 切换到编辑模式
   */
  const switchToEdit = () => {
    setDetailMode('edit');
  };

  /**
   * 保存项
   */
  const handleSave = async (values: ItemForm) => {
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

  /**
   * 删除项
   */
  const handleDelete = async (record: LookUpItem) => {
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

  /**
   * 切换状态
   */
  const handleToggleStatus = (record: LookUpItem) => {
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

  /**
   * 返回分类列表
   */
  const handleBack = () => {
    navigate('/lookup/classify');
  };

  /**
   * 表格列定义
   */
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
      render: (text: string) => text || '-'
    },
    {
      title: '排序',
      dataIndex: 'itemIndex',
      key: 'itemIndex',
      width: 60,
      align: 'center' as const
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 80,
      render: (status: number) => (
        <Badge
          status={statusMap[status as keyof typeof statusMap].color as any}
          text={statusMap[status as keyof typeof statusMap].text}
        />
      )
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
      fixed: 'right' as const,
      render: (_: any, record: LookUpItem) => (
        <div className={styles.actions}>
          <button className={styles.actionLink} onClick={() => handleEdit(record)}>
            编辑
          </button>
          <button className={styles.actionLink} onClick={() => handleToggleStatus(record)}>
            {record.status === 1 ? '失效' : '生效'}
          </button>
          <Popconfirm
            title={`确定要删除项 "${record.itemName}" 吗？`}
            onConfirm={() => handleDelete(record)}
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
      {/* 页面头部 */}
      <div className={styles.pageHead}>
        <div className={styles.pageHeadLeft}>
          <button className={styles.backBtn} onClick={handleBack}>
            ← 返回
          </button>
          <h2 className={styles.pageHeadTitle}>LookUp项列表</h2>
        </div>
        <div className={styles.classifyInfo}>
          <span className={styles.infoTag}>
            <span className={styles.infoLabel}>分类编码:</span>
            <span className={styles.infoValue}>{classifyCode}</span>
          </span>
          <span className={styles.infoTag}>
            <span className={styles.infoLabel}>分类名称:</span>
            <span className={styles.infoValue}>{classifyName}</span>
          </span>
        </div>
      </div>

      {/* 工具栏 */}
      <div className={styles.toolbar}>
        <div className={styles.toolbarLeft}>
          <div className={styles.searchArea}>
            <input
              type="text"
              className={styles.searchInput}
              placeholder="项编码"
              value={searchItemCode}
              onChange={(e) => setSearchItemCode(e.target.value)}
            />
            <input
              type="text"
              className={styles.searchInput}
              placeholder="项名称"
              value={searchItemName}
              onChange={(e) => setSearchItemName(e.target.value)}
            />
            <select
              className={styles.filterSelect}
              value={searchStatus ?? ''}
              onChange={(e) => setSearchStatus(e.target.value ? Number(e.target.value) : undefined)}
            >
              <option value="">全部状态</option>
              <option value={1}>有效</option>
              <option value={0}>失效</option>
            </select>
            <button className={`${styles.btn} ${styles.btnPrimary} ${styles.btnSm}`} onClick={handleSearch}>
              查询
            </button>
            <button className={`${styles.btn} ${styles.btnOutline} ${styles.btnSm}`} onClick={handleReset}>
              ↻ 重置
            </button>
          </div>
        </div>
        <div className={styles.toolbarRight}>
          <button className={`${styles.btn} ${styles.btnPrimary} ${styles.btnSm}`} onClick={handleAdd}>
            + 新增LookUp项
          </button>
        </div>
      </div>

      {/* 表格 */}
      <div className={styles.tableWrap}>
        <table className={styles.table}>
          <thead>
            <tr>
              {columns.map((col, index) => (
                <th key={index} style={{ width: col.width as number }}>
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
                  <div style={{ textAlign: 'center', padding: '60px 0', color: 'var(--text-hint)' }}>
                    暂无数据
                  </div>
                </td>
              </tr>
            ) : (
              dataSource.map((record, index) => (
                <tr key={record.itemId}>
                  <td>
                    <span 
                      className={styles.linkText} 
                      onClick={() => handleView(record)}
                    >
                      {record.itemCode}
                    </span>
                  </td>
                  <td>{record.itemName}</td>
                  <td>{record.itemValue}</td>
                  <td>{record.itemDesc || '-'}</td>
                  <td style={{ textAlign: 'center' }}>{record.itemIndex}</td>
                  <td>
                    <span className={styles.statusBadge}>
                      <span className={`${styles.dot} ${record.status === 1 ? styles.dotActive : styles.dotInactive}`}></span>
                      <span className={record.status === 1 ? styles.labelActive : styles.labelInactive}>
                        {record.status === 1 ? '有效' : '失效'}
                      </span>
                    </span>
                  </td>
                  <td>{record.createBy}</td>
                  <td>{record.createTime}</td>
                  <td>{record.lastUpdateBy || '-'}</td>
                  <td>{record.lastUpdateTime || '-'}</td>
                  <td>
                    <div className={styles.actions}>
                      <button className={styles.actionLink} onClick={() => handleEdit(record)}>编辑</button>
                      <button className={styles.actionLink} onClick={() => handleToggleStatus(record)}>
                        {record.status === 1 ? '失效' : '生效'}
                      </button>
                      <button className={`${styles.actionLink} ${styles.danger}`} onClick={() => {
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
                      }}>删除</button>
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* 分页 */}
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

    {/* 详情/编辑抽屉 */}
    <Drawer
      title={
        <div className={styles.drawerTitle}>
          <span>{currentItem ? 'LookUp项详情' : '新增LookUp项'}</span>
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
          {/* 基本信息 */}
          <div className={styles.detailCard}>
            <div className={styles.detailCardTitle}>📋 基本信息</div>
            <div className={styles.detailCardBody}>
              <div className={styles.detailRow}>
                <span className={styles.detailLabel}>所属分类</span>
                <span className={styles.detailValue}>{classifyName}</span>
              </div>
              <div className={styles.detailRow}>
                <span className={styles.detailLabel}>项编码</span>
                <span className={styles.detailValue}>{currentItem.itemCode}</span>
              </div>
              <div className={styles.detailRow}>
                <span className={styles.detailLabel}>项名称</span>
                <span className={styles.detailValue}>{currentItem.itemName}</span>
              </div>
              <div className={styles.detailRow}>
                <span className={styles.detailLabel}>项值</span>
                <span className={styles.detailValue}>{currentItem.itemValue}</span>
              </div>
              <div className={styles.detailRow}>
                <span className={styles.detailLabel}>描述</span>
                <span className={styles.detailValue}>{currentItem.itemDesc || '-'}</span>
              </div>
              <div className={styles.detailRow}>
                <span className={styles.detailLabel}>排序</span>
                <span className={styles.detailValue}>{currentItem.itemIndex}</span>
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

          {/* 扩展属性 */}
          <div className={styles.detailCard}>
            <div className={styles.detailCardTitle}>⚙️ 扩展属性</div>
            <div className={styles.detailCardBody}>
              <div className={styles.detailRow}>
                <span className={styles.detailLabel}>属性1</span>
                <span className={styles.detailValue}>{currentItem.itemAttr1 || '-'}</span>
              </div>
              <div className={styles.detailRow}>
                <span className={styles.detailLabel}>属性2</span>
                <span className={styles.detailValue}>{currentItem.itemAttr2 || '-'}</span>
              </div>
              <div className={styles.detailRow}>
                <span className={styles.detailLabel}>属性3</span>
                <span className={styles.detailValue}>{currentItem.itemAttr3 || '-'}</span>
              </div>
              <div className={styles.detailRow}>
                <span className={styles.detailLabel}>属性4</span>
                <span className={styles.detailValue}>{currentItem.itemAttr4 || '-'}</span>
              </div>
              <div className={styles.detailRow}>
                <span className={styles.detailLabel}>属性5</span>
                <span className={styles.detailValue}>{currentItem.itemAttr5 || '-'}</span>
              </div>
              <div className={styles.detailRow}>
                <span className={styles.detailLabel}>属性6</span>
                <span className={styles.detailValue}>{currentItem.itemAttr6 || '-'}</span>
              </div>
            </div>
          </div>

          {/* 系统信息 */}
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
              <Form.Item name="itemCode" label="项编码" rules={[{ required: true, message: '请输入项编码' }]}>
                <Input placeholder="如: ADMIN" disabled={!!currentItem} />
              </Form.Item>
            </div>
            <div className={styles.editFormItem}>
              <Form.Item name="itemName" label="项名称" rules={[{ required: true, message: '请输入项名称' }]}>
                <Input placeholder="如: 管理员" />
              </Form.Item>
            </div>
          </div>
          <div className={styles.editFormRow}>
            <div className={styles.editFormItem}>
              <Form.Item name="itemValue" label="项值" rules={[{ required: true, message: '请输入项值' }]}>
                <Input placeholder="如: 1" />
              </Form.Item>
            </div>
            <div className={styles.editFormItem}>
              <Form.Item name="itemIndex" label="排序" initialValue={1}>
                <Input type="number" placeholder="数字越小越靠前" />
              </Form.Item>
            </div>
          </div>
          <Form.Item name="itemDesc" label="描述">
            <TextArea rows={3} placeholder="请输入描述..." showCount maxLength={500} />
          </Form.Item>
          <div style={{ borderTop: '1px solid var(--border)', margin: '12px 0', paddingTop: '12px' }}>
            <span style={{ fontSize: '12px', color: 'var(--text-hint)', fontWeight: 500 }}>扩展属性</span>
          </div>
          <Form.Item name="itemAttr1" label="属性1">
            <TextArea rows={2} placeholder="扩展属性1" style={{ width: '100%', resize: 'vertical' }} />
          </Form.Item>
          <Form.Item name="itemAttr2" label="属性2">
            <TextArea rows={2} placeholder="扩展属性2" style={{ width: '100%', resize: 'vertical' }} />
          </Form.Item>
          <Form.Item name="itemAttr3" label="属性3">
            <TextArea rows={2} placeholder="扩展属性3" style={{ width: '100%', resize: 'vertical' }} />
          </Form.Item>
          <Form.Item name="itemAttr4" label="属性4">
            <TextArea rows={2} placeholder="扩展属性4" style={{ width: '100%', resize: 'vertical' }} />
          </Form.Item>
          <Form.Item name="itemAttr5" label="属性5">
            <TextArea rows={2} placeholder="扩展属性5" style={{ width: '100%', resize: 'vertical' }} />
          </Form.Item>
          <Form.Item name="itemAttr6" label="属性6">
            <TextArea rows={2} placeholder="扩展属性6" style={{ width: '100%', resize: 'vertical' }} />
          </Form.Item>
        </Form>
      )}
    </Drawer>
  </div>
);
};

export default ItemList;
