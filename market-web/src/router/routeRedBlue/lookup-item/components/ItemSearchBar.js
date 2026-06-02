import React from 'react';
import less from './ItemSearchBar.module.less';

/**
 * LookUp项搜索栏组件
 * 提供项页面的搜索、重置功能
 */
const ItemSearchBar = ({
  searchValues,
  onSearchItemCodeChange,
  onSearchItemNameChange,
  onSearchStatusChange,
  onSearch,
  onReset,
  onAdd
}) => {
  return (
    <div className={less.toolbar}>
      <div className={less.toolbarLeft}>
        <div className={less.searchArea}>
          <input
            type="text"
            className={less.searchInput}
            placeholder="项编码"
            value={searchValues.itemCode}
            onChange={(e) => onSearchItemCodeChange?.(e.target.value)}
          />
          <input
            type="text"
            className={less.searchInput}
            placeholder="项名称"
            value={searchValues.itemName}
            onChange={(e) => onSearchItemNameChange?.(e.target.value)}
          />
          <select
            className={less.filterSelect}
            value={searchValues.status ?? ''}
            onChange={(e) => onSearchStatusChange?.(e.target.value)}
          >
            <option value="">全部状态</option>
            <option value={1}>有效</option>
            <option value={0}>失效</option>
          </select>
          <button
            className={`${less.btn} ${less.btnPrimary} ${less.btnSm}`}
            onClick={onSearch}
          >
            查询
          </button>
          <button
            className={`${less.btn} ${less.btnOutline} ${less.btnSm}`}
            onClick={onReset}
          >
            ↻ 重置
          </button>
        </div>
      </div>
      <div className={less.toolbarRight}>
        <button
          className={`${less.btn} ${less.btnPrimary} ${less.btnSm}`}
          onClick={onAdd}
        >
          + 新增LookUp项
        </button>
      </div>
    </div>
  );
};

export default ItemSearchBar;
