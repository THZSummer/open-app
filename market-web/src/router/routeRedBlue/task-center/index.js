import React, { useState, useEffect } from 'react';
import { SearchOutlined, ReloadOutlined } from '@ant-design/icons';
import { Pagination, message } from 'antd';
import {
  getTaskList
} from './thunk';

import {
  downloadTaskResult
} from '../lookup-item/thunk';
import {
  TASK_TYPE_MAP,
  BIZ_TYPE_MAP,
  STATUS_MAP,
  DEFAULT_FILTERS,
  TASK_TYPE_OPTIONS,
  BIZ_TYPE_OPTIONS,
  STATUS_OPTIONS,
  TABLE_COLUMN_WIDTHS
} from './constant';
import { DEFAULT_PAGINATION, DEFAULT_QUERY_PARAMS, TASK_PAGE_SIZE_OPTIONS } from '../../../utils/constant';
import styles from './index.module.less';

const TaskList = () => {
  const [loading, setLoading] = useState(false);
  const [dataSource, setDataSource] = useState([]);
  const [pagination, setPagination] = useState(DEFAULT_PAGINATION);

  const [queryParams, setQueryParams] = useState(DEFAULT_QUERY_PARAMS);

  const [filters, setFilters] = useState(DEFAULT_FILTERS);

  const fetchData = async () => {
    setLoading(true);
    try {
      const result = await getTaskList(queryParams);
      const responseData = result.data || {};
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
  };

  useEffect(() => {
    fetchData();
  }, [queryParams]);

  const handleSearch = () => {
    const newParams = {
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

  const handlePageChange = (page, pageSize) => {
    setQueryParams({
      ...queryParams,
      pageNum: page,
      pageSize: pageSize || queryParams.pageSize
    });
  };

  const handleFilterChange = (key, value) => {
    setFilters(prev => ({ ...prev, [key]: value }));
  };

  const handleDownload = async (task) => {
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
    { title: '任务ID', key: 'taskId', width: TABLE_COLUMN_WIDTHS.taskId },
    { title: '业务类型', key: 'bizType', width: TABLE_COLUMN_WIDTHS.bizType },
    { title: '类型', key: 'taskType', width: TABLE_COLUMN_WIDTHS.taskType },
    { title: '状态', key: 'status', width: TABLE_COLUMN_WIDTHS.status },
    { title: '结果', key: 'result' },
    { title: '创建时间', key: 'createTime', width: TABLE_COLUMN_WIDTHS.createTime },
    { title: '完成时间', key: 'lastUpdateTime', width: TABLE_COLUMN_WIDTHS.lastUpdateTime },
    { title: '操作', key: 'action', width: TABLE_COLUMN_WIDTHS.action }
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
          React.createElement('div', { className: styles.empty },
            React.createElement('div', { className: styles.icon }, '📋'),
            React.createElement('p', null, '暂无数据')
          )
        )
      );
    }

    return dataSource.map((record) =>
      React.createElement('tr', { key: record.taskId },
        React.createElement('td', { className: styles.taskIdCell }, record.taskId),
        React.createElement('td', null,
          React.createElement('span', {
            className: `${styles.tag} ${BIZ_TYPE_MAP[record.bizType]?.className || ''}`
          },
            BIZ_TYPE_MAP[record.bizType]?.text || '-'
          )
        ),
        React.createElement('td', null,
          React.createElement('span', {
            className: `${styles.tag} ${TASK_TYPE_MAP[record.taskType]?.className || ''}`
          },
            TASK_TYPE_MAP[record.taskType]?.text || '-'
          )
        ),
        React.createElement('td', null,
          React.createElement('div', { className: styles.statusBadge },
            React.createElement('span', { className: `${styles.dot} ${STATUS_MAP[record.status]?.dotClass || ''}` }),
            React.createElement('span', { className: `${styles.label} ${STATUS_MAP[record.status]?.labelClass || ''}` },
              STATUS_MAP[record.status]?.text || '-'
            )
          )
        ),
        React.createElement('td', { className: styles.resultCell }, record.result || '-'),
        React.createElement('td', null, record.createTime || '-'),
        React.createElement('td', null, record.lastUpdateTime || '-'),
        React.createElement('td', null,
          React.createElement('div', { className: styles.actions },
            record.status === 2 && record.fileId ? (
              React.createElement('button', {
                className: styles.actionLink,
                onClick: () => handleDownload(record)
              }, '下载')
            ) : (
              React.createElement('span', null, '-')
            )
          )
        )
      )
    );
  };

  return React.createElement('div', { className: styles.container },
    React.createElement('div', { className: styles.page },
      React.createElement('div', { className: styles.pageHead },
        React.createElement('div', { className: styles.pageHeadLeft },
          React.createElement('span', { className: styles.pageHeadTitle }, '任务中心')
        )
      ),
      React.createElement('div', { className: styles.toolbar },
        React.createElement('div', { className: styles.toolbarLeft },
          React.createElement('div', { className: styles.searchWrap },
            React.createElement('span', { className: styles.searchLabel }, '任务类型'),
            React.createElement('select', {
              className: styles.filterSelect,
              value: filters.taskType,
              onChange: (e) => handleFilterChange('taskType', e.target.value)
            },
              React.createElement('option', { value: '' }, '全部'),
              React.createElement('option', { value: '1' }, '导入'),
              React.createElement('option', { value: '2' }, '导出')
            )
          ),
          React.createElement('div', { className: styles.searchWrap },
            React.createElement('span', { className: styles.searchLabel }, '业务类型'),
            React.createElement('select', {
              className: styles.filterSelect,
              value: filters.bizType,
              onChange: (e) => handleFilterChange('bizType', e.target.value)
            },
              React.createElement('option', { value: '' }, '全部'),
              React.createElement('option', { value: '1' }, 'LookUp')
            )
          ),
          React.createElement('div', { className: styles.searchWrap },
            React.createElement('span', { className: styles.searchLabel }, '状态'),
            React.createElement('select', {
              className: styles.filterSelect,
              value: filters.status,
              onChange: (e) => handleFilterChange('status', e.target.value)
            },
              React.createElement('option', { value: '' }, '全部'),
              React.createElement('option', { value: '0' }, '待处理'),
              React.createElement('option', { value: '1' }, '处理中'),
              React.createElement('option', { value: '2' }, '已完成'),
              React.createElement('option', { value: '3' }, '失败')
            )
          ),
          React.createElement('button', {
            className: `${styles.btn} ${styles.btnPrimary} ${styles.btnSm}`,
            onClick: handleSearch
          }, React.createElement(SearchOutlined), ' 查询'),
          React.createElement('button', {
            className: `${styles.btn} ${styles.btnOutline} ${styles.btnSm}`,
            onClick: handleReset
          }, React.createElement(ReloadOutlined), ' 重置')
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
            pageSizeOptions: TASK_PAGE_SIZE_OPTIONS
          })
        )
      )
    )
  );
};

export default TaskList;
