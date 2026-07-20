/**
 * 能力目录管理列表页面
 *
 * 提供能力目录的分页列表展示、关键词搜索和排序功能。
 * 对应 FR-001：平台管理员在 market-web 查看所有 ability 类型列表。
 */
import React, { useState, useEffect } from 'react';
import { Table, Pagination, Input, Button, message, Tooltip, Select } from 'antd';
import { SearchOutlined, ReloadOutlined } from '@ant-design/icons';
import { getAbilityList } from './thunk';
import { PAGE_SIZE_OPTIONS } from '../../../utils/constant';
import { renderAlwaysWithTooltip } from '../../../utils/common';
import less from './index.module.less';

/** 默认分页 */
const DEFAULT_PAGE_SIZE = 20;

/** 排序选项 */
const SORT_OPTIONS = [
  { field: 'orderNum', label: '排序号' },
  { field: 'abilityType', label: '能力编码' },
  { field: 'nameCn', label: '中文名' },
  { field: 'nameEn', label: '英文名' },
  { field: 'createTime', label: '创建时间' },
  { field: 'updateTime', label: '更新时间' },
];

/**
 * 能力目录管理列表页面
 */
const AbilityAdminList = () => {
  const [loading, setLoading] = useState(false);
  const [dataSource, setDataSource] = useState([]);
  const [pagination, setPagination] = useState({
    curPage: 1,
    pageSize: DEFAULT_PAGE_SIZE,
    total: 0,
  });
  const [queryParams, setQueryParams] = useState({
    curPage: 1,
    pageSize: DEFAULT_PAGE_SIZE,
  });
  const [keyword, setKeyword] = useState('');
  const [sortField, setSortField] = useState('orderNum');
  const [sortOrder, setSortOrder] = useState('asc');

  /**
   * 获取列表数据
   */
  const fetchData = async () => {
    setLoading(true);
    const result = await getAbilityList(queryParams);
    if (result && result.code === '200') {
      setDataSource(result.data || []);
      if (result.page) {
        setPagination({
          curPage: Number(result.page.curPage) || 1,
          pageSize: Number(result.page.pageSize) || DEFAULT_PAGE_SIZE,
          total: Number(result.page.total) || 0,
        });
      }
    } else {
      message.error(result?.messageZh || result?.messageEn || '获取能力列表失败');
      setDataSource([]);
    }
    setLoading(false);
  };

  useEffect(() => {
    fetchData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [queryParams]);

  /**
   * 搜索
   */
  const handleSearch = () => {
    const newParams = {
      curPage: 1,
      pageSize: pagination.pageSize,
      keyword: keyword || undefined,
      sortField: sortField || undefined,
      sortOrder: sortOrder || undefined,
    };
    setQueryParams(newParams);
  };

  /**
   * 重置
   */
  const handleReset = () => {
    setKeyword('');
    setSortField('orderNum');
    setSortOrder('asc');
    setQueryParams({
      curPage: 1,
      pageSize: DEFAULT_PAGE_SIZE,
    });
  };

  /**
   * 分页改变
   */
  const handlePageChange = (page, pageSize) => {
    const newParams = {
      ...queryParams,
      curPage: page,
      pageSize: pageSize || queryParams.pageSize,
    };
    setQueryParams(newParams);
  };

  /**
   * 排序字段变更
   */
  const handleSortFieldChange = (value) => {
    setSortField(value);
  };

  /**
   * 排序方向切换
   */
  const handleSortOrderChange = (value) => {
    setSortOrder(value);
  };

  /** 表格列定义 */
  const columns = [
    {
      title: '能力编码',
      dataIndex: 'abilityType',
      key: 'abilityType',
      width: 90,
      render: (text) => (
        <span>{text ?? '-'}</span>
      ),
    },
    {
      title: '中文名',
      dataIndex: 'nameCn',
      key: 'nameCn',
      width: 150,
      ellipsis: true,
      render: (text) => renderAlwaysWithTooltip(text),
    },
    {
      title: '英文名',
      dataIndex: 'nameEn',
      key: 'nameEn',
      width: 150,
      ellipsis: true,
      render: (text) => renderAlwaysWithTooltip(text),
    },
    {
      title: '中文描述',
      dataIndex: 'descCn',
      key: 'descCn',
      width: 200,
      ellipsis: true,
      render: (text) => renderAlwaysWithTooltip(text),
    },
    {
      title: '英文描述',
      dataIndex: 'descEn',
      key: 'descEn',
      width: 200,
      ellipsis: true,
      render: (text) => renderAlwaysWithTooltip(text),
    },
    {
      title: '图标',
      dataIndex: 'iconUrl',
      key: 'iconUrl',
      width: 60,
      render: (url) => {
        if (!url) return <span className={less.noIcon}>-</span>;
        return (
          <Tooltip title="点击查看大图">
            <img
              className={less.iconPreview}
              src={url}
              alt="图标"
              onError={(e) => {
                e.currentTarget.style.display = 'none';
              }}
            />
          </Tooltip>
        );
      },
    },
    {
      title: '示意图',
      dataIndex: 'exampleDiagramUrl',
      key: 'exampleDiagramUrl',
      width: 60,
      render: (url) => {
        if (!url) return <span className={less.noIcon}>-</span>;
        return (
          <Tooltip title="点击查看大图">
            <img
              className={less.iconPreview}
              src={url}
              alt="示意图"
              onError={(e) => {
                e.currentTarget.style.display = 'none';
              }}
            />
          </Tooltip>
        );
      },
    },
    {
      title: '排序号',
      dataIndex: 'orderNum',
      key: 'orderNum',
      width: 70,
      sorter: false,
      render: (text) => text ?? '-',
    },
    {
      title: '加载类型',
      dataIndex: 'loadType',
      key: 'loadType',
      width: 100,
      render: (type) => {
        switch (type) {
          case 1:
            return <span className={less.loadTypeTag}>路由加载</span>;
          case 2:
            return <span className={`${less.loadTypeTag} ${less.loadTypeMicro}`}>微前端加载</span>;
          default:
            return <span>{type ?? '-'}</span>;
        }
      },
    },
    {
      title: '进入地址',
      dataIndex: 'entryUrl',
      key: 'entryUrl',
      width: 180,
      ellipsis: true,
      render: (text) => {
        if (!text) return '-';
        return (
          <Tooltip title={text}>
            <a href={text} target="_blank" rel="noopener noreferrer">
              {text}
            </a>
          </Tooltip>
        );
      },
    },
    {
      title: '路由路径',
      dataIndex: 'routePath',
      key: 'routePath',
      width: 120,
      ellipsis: true,
      render: (text) => renderAlwaysWithTooltip(text || '-'),
    },
    {
      title: '别名',
      dataIndex: 'aliasName',
      key: 'aliasName',
      width: 120,
      ellipsis: true,
      render: (text) => renderAlwaysWithTooltip(text || '-'),
    },
    {
      title: '隐藏',
      dataIndex: 'hidden',
      key: 'hidden',
      width: 60,
      render: (val) => (
        <span className={val === 1 ? less.hiddenYes : less.hiddenNo}>
          {val === 1 ? '是' : '否'}
        </span>
      ),
    },
    {
      title: '需版本发布',
      dataIndex: 'requireRelease',
      key: 'requireRelease',
      width: 90,
      render: (val) => (val === 1 ? '是' : '否'),
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      key: 'createTime',
      width: 160,
      render: (text) => text || '-',
    },
    {
      title: '更新人',
      dataIndex: 'updateBy',
      key: 'updateBy',
      width: 120,
      ellipsis: true,
      render: (text) => text || '-',
    },
    {
      title: '更新时间',
      dataIndex: 'updateTime',
      key: 'updateTime',
      width: 160,
      render: (text) => text || '-',
    },
  ];

  return (
    <div className={less.container}>
      <div className={less.page}>
        {/* 页面头部 */}
        <div className={less.pageHead}>
          <div className={less.pageHeadLeft}>
            <span className={less.pageHeadTitle}>能力目录管理</span>
          </div>
        </div>

        {/* 搜索栏 */}
        <div className={less.searchBar}>
          <div className={less.searchWrap}>
            <span className={less.searchLabel}>关键词：</span>
            <Input
              className={less.searchInput}
              placeholder="搜索中文名/英文名"
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              onPressEnter={handleSearch}
              prefix={<SearchOutlined />}
              allowClear
            />
          </div>
          <div className={less.searchWrap}>
            <span className={less.searchLabel}>排序字段：</span>
            <Select
              value={sortField}
              onChange={handleSortFieldChange}
              style={{ width: 120 }}
              options={SORT_OPTIONS.map((opt) => ({
                value: opt.field,
                label: opt.label,
              }))}
            />
          </div>
          <div className={less.searchWrap}>
            <span className={less.searchLabel}>排序方向：</span>
            <Select
              value={sortOrder}
              onChange={handleSortOrderChange}
              style={{ width: 100 }}
              options={[
                { value: 'asc', label: '升序' },
                { value: 'desc', label: '降序' },
              ]}
            />
          </div>
          <Button type="primary" icon={<SearchOutlined />} onClick={handleSearch}>
            搜索
          </Button>
          <Button icon={<ReloadOutlined />} onClick={handleReset}>
            重置
          </Button>
        </div>

        {/* 表格 */}
        <div className={less.tableWrap}>
          <Table
            columns={columns}
            dataSource={dataSource}
            rowKey="abilityType"
            loading={loading}
            pagination={false}
            scroll={{ x: 2200 }}
            size="middle"
          />
        </div>

        {/* 分页 */}
        <div className={less.paginationWrapper}>
          <Pagination
            total={pagination.total}
            current={pagination.curPage}
            pageSize={pagination.pageSize}
            pageSizeOptions={PAGE_SIZE_OPTIONS}
            showSizeChanger
            showQuickJumper
            showTotal={(total) => `共 ${total} 条`}
            onChange={handlePageChange}
          />
        </div>
      </div>
    </div>
  );
};

export default AbilityAdminList;
