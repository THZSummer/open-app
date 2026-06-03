import React from 'react';
import { SearchOutlined, ReloadOutlined, PlusOutlined } from '@ant-design/icons';
import less from './SearchBar.module.less';

/**
 * 分类搜索栏组件
 * 提供分类页面的搜索、重置和新增功能
 */
const SearchBar = ({
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
          <span className={less.searchLabel}>类别编码</span>
          <input
            type="text"
            className={less.searchInput}
            placeholder="请输入"
            value={searchValues.classifyCode}
            onChange={(e) => handleInputChange('classifyCode', e.target.value)}
          />
        </div>
        <div className={less.searchWrap}>
          <span className={less.searchLabel}>类别名称</span>
          <input
            type="text"
            className={less.searchInput}
            placeholder="请输入"
            value={searchValues.classifyName}
            onChange={(e) => handleInputChange('classifyName', e.target.value)}
          />
        </div>
        <div className={less.searchWrap}>
          <span className={less.searchLabel}>描述</span>
          <input
            type="text"
            className={`${less.searchInput} ${less.descInput}`}
            placeholder="请输入"
            value={searchValues.classifyDesc}
            onChange={(e) => handleInputChange('classifyDesc', e.target.value)}
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
          <PlusOutlined /> 新增分类
        </button>
      </div>
    </div>
  );
};

export default SearchBar;
