import React, { useState } from 'react';

import './Pagination.m.less';

/**
 * 自定义分页组件 — 对照 demo-app-list.html .pagination-wrapper
 *
 * 设计：
 * - 左侧：共 N 条
 * - 右侧：[上一页] [1] [2] ... [N] [下一页]  +  最大分页数 [select]
 *
 * @param {Object} props - 组件属性
 * @param {Object} props.pagination - { curPage, pageSize, total }
 * @param {Function} props.onChange - (page, pageSize) => void
 * @param {Function} props.onShowSizeChange - (page, pageSize) => void
 * @param {Array} props.pageSizeOptions - 可选每页条数，默认 [10, 20, 50]
 */
function Pagination(props) {
  const { pagination, onChange, onShowSizeChange, pageSizeOptions = [10, 20, 50] } = props;
  const { curPage = 1, pageSize = 10, total = 0 } = pagination || {};
  const totalPages = Math.max(1, Math.ceil(total / pageSize));
  const [jumpValue, setJumpValue] = useState('');

  // 不显示分页：数据为空 或 总数不足一页且只有一页
  if (total === 0) return null;

  const handlePageChange = (page) => {
    if (page < 1 || page > totalPages || page === curPage) return;
    onChange && onChange(page, pageSize);
  };

  const handleSizeChange = (e) => {
    const newSize = parseInt(e.target.value, 10);
    onShowSizeChange && onShowSizeChange(1, newSize);
  };

  // 生成页码按钮序列（含省略号）
  // 规则（对照 demo renderPageButtons）：第1页、最后一页、当前±1页 显示；其余位置用 ...
  const renderPageButtons = () => {
    const buttons = [];
    for (let i = 1; i <= totalPages; i++) {
      if (i === 1 || i === totalPages || (i >= curPage - 1 && i <= curPage + 1)) {
        buttons.push(
          <button
            key={i}
            className={`page-btn ${i === curPage ? 'active' : ''}`}
            onClick={() => handlePageChange(i)}
          >
            {i}
          </button>
        );
      } else if (i === curPage - 2 || i === curPage + 2) {
        buttons.push(
          <span key={`ellipsis-${i}`} className="page-ellipsis">
            ...
          </span>
        );
      }
    }
    return buttons;
  };

  return (
    <div className="pagination-wrapper">
      <div className="pagination-info">
        共 <span className="pagination-info-count">{total}</span> 条
      </div>
      <div className="pagination-controls">
        <div className="pagination-btns">
          <button
            className="page-btn"
            disabled={curPage === 1}
            onClick={() => handlePageChange(curPage - 1)}
            aria-label="上一页"
          >
            &lt;
          </button>
          {renderPageButtons()}
          <button
            className="page-btn"
            disabled={curPage === totalPages}
            onClick={() => handlePageChange(curPage + 1)}
            aria-label="下一页"
          >
            &gt;
          </button>
        </div>
        <div className="page-size-wrapper">
          <span className="page-size-label">每页</span>
          <select
            className="page-size-select"
            value={pageSize}
            onChange={handleSizeChange}
          >
            {pageSizeOptions.map((opt) => (
              <option key={opt} value={opt}>
                {opt}
              </option>
            ))}
          </select>
          <span className="page-size-label">条</span>
        </div>
        <div className="page-jump-wrapper">
          <span className="page-size-label">跳至</span>
          <input
            className="page-jump-input"
            value={jumpValue}
            onChange={(e) => setJumpValue(e.target.value.replace(/\D/g, ''))}
            onKeyDown={(e) => {
              if (e.key === 'Enter') {
                const page = parseInt(jumpValue, 10);
                if (page >= 1 && page <= totalPages) {
                  handlePageChange(page);
                  setJumpValue('');
                }
              }
            }}
            onBlur={() => setJumpValue('')}
          />
          <span className="page-size-label">页</span>
        </div>
      </div>
    </div>
  );
}

export default Pagination;
