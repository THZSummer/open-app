import React, { useState, useEffect } from 'react';
import { SearchOutlined, ReloadOutlined } from '@ant-design/icons';
import { Pagination, message } from 'antd';
import {
  getTaskList
} from './thunk';

import {
  downloadTaskResult
} from '../lookup-item/thunk';
import styles from './index.module.less';

const taskTypeMap = {
  1: { text: '导入', className: styles.tagImport },
  2: { text: '导出', className: styles.tagExport }
};

const bizTypeMap = {
  1: { text: 'LookUp', className: styles.tagLookup }
};

const statusMap = {
  0: { text: '待处理', dotClass: styles.dotPending, labelClass: styles.labelPending },
  1: { text: '处理中', dotClass: styles.dotProcessing, labelClass: styles.labelProcessing },
  2: { text: '已完成', dotClass: styles.dotCompleted, labelClass: styles.labelCompleted },
  3: { text: '失败', dotClass: styles.dotFailed, labelClass: styles.labelFailed }
};

const TaskList = () => {
  const [loading, setLoading] = useState(false);
  const [dataSource, setDataSource] = useState([]);
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0
  });

  const [queryParams, setQueryParams] = useState({
    pageNum: 1,
    pageSize: 10
  });

  const [filters, setFilters] = useState({
    taskType: '',
    bizType: '',
    status: ''
  });

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
    { title: '任务ID', key: 'taskId', width: 180 },
    { title: '业务类型', key: 'bizType', width: 100 },
    { title: '类型', key: 'taskType', width: 80 },
    { title: '状态', key: 'status', width: 100 },
    { title: '结果', key: 'result' },
    { title: '创建时间', key: 'createTime', width: 160 },
    { title: '完成时间', key: 'lastUpdateTime', width: 160 },
    { title: '操作', key: 'action', width: 100 }
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
            className: `${styles.tag} ${bizTypeMap[record.bizType]?.className || ''}`
          },
            bizTypeMap[record.bizType]?.text || '-'
          )
        ),
        React.createElement('td', null,
          React.createElement('span', {
            className: `${styles.tag} ${taskTypeMap[record.taskType]?.className || ''}`
          },
            taskTypeMap[record.taskType]?.text || '-'
          )
        ),
        React.createElement('td', null,
          React.createElement('div', { className: styles.statusBadge },
            React.createElement('span', { className: `${styles.dot} ${statusMap[record.status]?.dotClass || ''}` }),
            React.createElement('span', { className: `${styles.label} ${statusMap[record.status]?.labelClass || ''}` },
              statusMap[record.status]?.text || '-'
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
            pageSizeOptions: ['5', '10', '20', '50']
          })
        )
      )
    )
  );
};

export default TaskList;
