import React, { useState, useEffect, useCallback } from 'react';
import { SearchOutlined, ReloadOutlined } from '@ant-design/icons';
import { Pagination, message } from 'antd';
import {
  getTaskList,
  downloadTaskResult,
  type Task,
  type TaskQueryParams
} from '@/api/lookup';
import styles from './index.module.less';

const taskTypeMap: Record<number, { text: string; className: string }> = {
  1: { text: '导入', className: styles.tagImport },
  2: { text: '导出', className: styles.tagExport }
};

const bizTypeMap: Record<number, { text: string; className: string }> = {
  1: { text: 'LookUp', className: styles.tagLookup }
};

const statusMap: Record<number, { text: string; dotClass: string; labelClass: string }> = {
  0: { text: '待处理', dotClass: styles.dotPending, labelClass: styles.labelPending },
  1: { text: '处理中', dotClass: styles.dotProcessing, labelClass: styles.labelProcessing },
  2: { text: '已完成', dotClass: styles.dotCompleted, labelClass: styles.labelCompleted },
  3: { text: '失败', dotClass: styles.dotFailed, labelClass: styles.labelFailed }
};

const TaskList: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [dataSource, setDataSource] = useState<Task[]>([]);
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0
  });

  const [queryParams, setQueryParams] = useState<TaskQueryParams>({
    pageNum: 1,
    pageSize: 10
  });

  const [filters, setFilters] = useState({
    taskType: '',
    bizType: '',
    status: ''
  });

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const res = await getTaskList(queryParams);
      const responseData = res.data.data;
      setDataSource(responseData.list || []);
      setPagination({
        current: responseData.pageNum,
        pageSize: responseData.pageSize,
        total: responseData.total
      });
    } catch (error) {
      console.error('获取任务列表失败:', error);
    } finally {
      setLoading(false);
    }
  }, [queryParams]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const handleSearch = () => {
    const newParams: TaskQueryParams = {
      pageNum: 1,
      pageSize: queryParams.pageSize
    };
    if (filters.taskType) {
      newParams.taskType = Number(filters.taskType);
    }
    if (filters.bizType) {
      newParams.bizType = Number(filters.bizType);
    }
    if (filters.status) {
      newParams.status = Number(filters.status);
    }
    setQueryParams(newParams);
  };

  const handleReset = () => {
    setFilters({
      taskType: '',
      bizType: '',
      status: ''
    });
    setQueryParams({
      pageNum: 1,
      pageSize: queryParams.pageSize
    });
  };

  const handlePageChange = (page: number, pageSize?: number) => {
    setQueryParams({
      ...queryParams,
      pageNum: page,
      pageSize: pageSize || queryParams.pageSize
    });
  };

  const handleFilterChange = (key: keyof typeof filters, value: string) => {
    setFilters(prev => ({ ...prev, [key]: value }));
  };

  const handleDownload = async (task: Task) => {
    try {
      message.loading({ content: '正在下载...', key: 'download' });
      const blob = await downloadTaskResult(task.taskId);
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = task.fileName || `任务结果_${task.taskId}.xlsx`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
      message.success({ content: '下载成功', key: 'download' });
    } catch (error) {
      console.error('下载失败:', error);
      message.error({ content: '下载失败', key: 'download' });
    }
  };

  const columns = [
    { title: '任务ID', key: 'taskId', width: 180 },
    { title: '业务类型', key: 'bizType', width: 100 },
    { title: '类型', key: 'taskType', width: 80 },
    { title: '状态', key: 'status', width: 100 },
    { title: '结果', key: 'result' },
    { title: '创建时间', key: 'createTime', width: 160 },
    { title: '完成时间', key: 'lastUpdateTime', width: 160 },
    { title: '操作', key: 'action', width: 100 }
  ];

  return (
    <div className={styles.container}>
      <div className={styles.page}>
        <div className={styles.pageHead}>
          <div className={styles.pageHeadLeft}>
            <span className={styles.pageHeadTitle}>任务中心</span>
          </div>
        </div>

        <div className={styles.toolbar}>
          <div className={styles.toolbarLeft}>
            <div className={styles.searchWrap}>
              <span className={styles.searchLabel}>任务类型</span>
              <select
                className={styles.filterSelect}
                value={filters.taskType}
                onChange={(e) => handleFilterChange('taskType', e.target.value)}
              >
                <option value="">全部</option>
                <option value="1">导入</option>
                <option value="2">导出</option>
              </select>
            </div>
            <div className={styles.searchWrap}>
              <span className={styles.searchLabel}>业务类型</span>
              <select
                className={styles.filterSelect}
                value={filters.bizType}
                onChange={(e) => handleFilterChange('bizType', e.target.value)}
              >
                <option value="">全部</option>
                <option value="1">LookUp</option>
              </select>
            </div>
            <div className={styles.searchWrap}>
              <span className={styles.searchLabel}>状态</span>
              <select
                className={styles.filterSelect}
                value={filters.status}
                onChange={(e) => handleFilterChange('status', e.target.value)}
              >
                <option value="">全部</option>
                <option value="0">待处理</option>
                <option value="1">处理中</option>
                <option value="2">已完成</option>
                <option value="3">失败</option>
              </select>
            </div>
            <button className={`${styles.btn} ${styles.btnPrimary} ${styles.btnSm}`} onClick={handleSearch}>
              <SearchOutlined /> 查询
            </button>
            <button className={`${styles.btn} ${styles.btnOutline} ${styles.btnSm}`} onClick={handleReset}>
              <ReloadOutlined /> 重置
            </button>
          </div>
        </div>

        <div className={styles.tableWrap}>
          <table className={styles.table}>
            <thead>
              <tr>
                {columns.map((col, index) => (
                  <th key={index} style={{ width: col.width as number }}>
                    {col.title}
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
                  <tr key={record.taskId}>
                    <td className={styles.taskIdCell}>{record.taskId}</td>
                    <td>
                      <span className={`${styles.tag} ${bizTypeMap[record.bizType]?.className || ''}`}>
                        {bizTypeMap[record.bizType]?.text || '-'}
                      </span>
                    </td>
                    <td>
                      <span className={`${styles.tag} ${taskTypeMap[record.taskType]?.className || ''}`}>
                        {taskTypeMap[record.taskType]?.text || '-'}
                      </span>
                    </td>
                    <td>
                      <div className={styles.statusBadge}>
                        <span className={`${styles.dot} ${statusMap[record.status]?.dotClass || ''}`}></span>
                        <span className={`${styles.label} ${statusMap[record.status]?.labelClass || ''}`}>
                          {statusMap[record.status]?.text || '-'}
                        </span>
                      </div>
                    </td>
                    <td className={styles.resultCell}>{record.result || '-'}</td>
                    <td>{record.createTime || '-'}</td>
                    <td>{record.lastUpdateTime || '-'}</td>
                    <td>
                      <div className={styles.actions}>
                        {record.status === 2 && record.fileId ? (
                          <button 
                            className={styles.actionLink}
                            onClick={() => handleDownload(record)}
                          >
                            下载
                          </button>
                        ) : (
                          <span>-</span>
                        )}
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
              pageSizeOptions={['5', '10', '20', '50']}
            />
          </div>
        </div>
      </div>
    </div>
  );
};

export default TaskList;