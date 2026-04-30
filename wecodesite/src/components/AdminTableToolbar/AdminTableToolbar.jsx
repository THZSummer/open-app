import React from 'react';
import { Input, TreeSelect, Select } from 'antd';
import { convertToTreeData } from '../../utils/common';
import './AdminTableToolbar.less';

const { Search } = Input;

const AdminTableToolbar = ({
  keyword,
  onKeywordChange,
  onSearch,
  placeholder = '搜索名称',
  categoryId,
  categories,
  onCategoryChange,
  status,
  statusOptions = [
    { value: 0, label: '草稿' },
    { value: 1, label: '待审' },
    { value: 2, label: '已发布' },
    { value: 3, label: '已下线' },
  ],
  onStatusChange,
}) => {
  return (
    <div className="admin-toolbar">
      <Search
        placeholder={placeholder}
        value={keyword}
        onChange={(e) => onKeywordChange(e.target.value)}
        style={{ width: 200 }}
        onSearch={onSearch}
      />
      <TreeSelect
        placeholder="选择分类"
        value={categoryId}
        onChange={onCategoryChange}
        treeData={convertToTreeData(categories)}
        treeDefaultExpandAll
        allowClear
        style={{ width: 150 }}
        dropdownStyle={{ maxHeight: 400, overflow: 'auto' }}
      />
      <Select
        placeholder="选择状态"
        value={status}
        onChange={onStatusChange}
        style={{ width: 120 }}
        allowClear
      >
        {statusOptions.map(option => (
          <Select.Option key={option.value} value={option.value}>
            {option.label}
          </Select.Option>
        ))}
      </Select>
    </div>
  );
};

export default AdminTableToolbar;
