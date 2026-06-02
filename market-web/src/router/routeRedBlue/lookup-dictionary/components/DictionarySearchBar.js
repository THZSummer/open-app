import React from 'react';
import { SearchOutlined, ReloadOutlined, PlusOutlined } from '@ant-design/icons';
import less from './DictionarySearchBar.module.less';

/**
 * 数据字典搜索栏组件
 * 提供字典页面的搜索、重置和新增功能
 */
const DictionarySearchBar = ({
  searchValues,
  onSearchValuesChange,
  onSearch,
  onReset,
  onAdd
}) => {
  const handleInputChange = (field, value) => {
    onSearchValuesChange?.({
      ...searchValues,
      [field]: value
    });
  };
  
  return (
    <div className={less.toolbar}>
      <div className={less.toolbarLeft}>
        <div className={less.searchWrap}>
          <span className={less.searchLabel}>编码</span>
          <input
            type="text"
            className={less.searchInput}
            placeholder="请输入"
            value={searchValues.code}
            onChange={(e) => handleInputChange('code', e.target.value)}
          />
        </div>
        <div className={less.searchWrap}>
          <span className={less.searchLabel}>名称</span>
          <input
            type="text"
            className={less.searchInput}
            placeholder="请输入"
            value={searchValues.name}
            onChange={(e) => handleInputChange('name', e.target.value)}
          />
        </div>
        <div className={less.searchWrap}>
          <span className={less.searchLabel}>路径</span>
          <input
            type="text"
            className={less.searchInput}
            placeholder="请输入"
            value={searchValues.path}
            onChange={(e) => handleInputChange('path', e.target.value)}
          />
        </div>
        <div className={less.searchWrap}>
          <span className={less.searchLabel}>状态</span>
          <select
            className={less.filterSelect}
            value={searchValues.status}
            onChange={(e) => handleInputChange('status', e.target.value)}
          >
            <option value="">全部状态</option>
            <option value={1}>有效</option>
            <option value={0}>失效</option>
          </select>
        </div>
        <button
          className={`${less.btn} ${less.btnPrimary} ${less.btnSm}`}
          onClick={onSearch}
        >
          <SearchOutlined /> 查询
        </button>
        <button
          className={`${less.btn} ${less.btnOutline} ${less.btnSm}`}
          onClick={onReset}
        >
          <ReloadOutlined /> 重置
        </button>
      </div>
      <div className={less.toolbarRight}>
        <button
          className={`${less.btn} ${less.btnPrimary} ${less.btnSm}`}
          onClick={onAdd}
        >
          <PlusOutlined /> 新增
        </button>
      </div>
    </div>
  );
};

export default DictionarySearchBar;
