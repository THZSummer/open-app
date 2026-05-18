/**
 * ========================================
 * 分页表格列表组件
 * ========================================
 *
 * 功能：
 * - 展示数据表格
 * - 支持分页和分页大小切换
 * - 提供统一的加载状态展示
 */

import React from 'react';
import { Table, Pagination } from 'antd';
import { PAGE_SIZE_OPTIONS } from '../../utils/constants';
import './PageList.less';

/**
 * 分页表格列表组件
 *
 * @param {Object} props - 组件属性
 * @param {Array} props.columns - 表格列配置
 * @param {Array} props.dataSource - 数据源
 * @param {boolean} props.loading - 加载状态
 * @param {Object} props.pagination - 分页配置
 * @param {number} props.pagination.total - 总条数
 * @param {number} props.pagination.curPage - 当前页
 * @param {number} props.pagination.pageSize - 每页条数
 * @param {Function} props.onPageChange - 分页变化回调
 * @param {string} [props.rowKey='id'] - 行唯一标识字段
 * @param {string} [props.className=''] - 自定义样式类名
 */
const PageList = ({
  columns,
  dataSource,
  loading,
  pagination,
  onPageChange,
  rowKey = 'id',
  className = '',
}) => {
  return (
    <>
      <div className="table-wrapper">
        <Table
          columns={columns}
          dataSource={dataSource}
          rowKey={rowKey}
          pagination={false}
          loading={loading}
          className={className}
        />
      </div>

      {pagination.total > 0 && (
        <div className="pagination-wrapper">
          <Pagination
            current={pagination.curPage}
            pageSize={pagination.pageSize}
            total={pagination.total}
            onChange={onPageChange}
            showSizeChanger
            pageSizeOptions={PAGE_SIZE_OPTIONS}
            showQuickJumper
            showTotal={(total) => `共 ${total} 条`}
          />
        </div>
      )}
    </>
  );
};

export default PageList;
