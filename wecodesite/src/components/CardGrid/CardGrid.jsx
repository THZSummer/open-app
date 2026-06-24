import React from 'react';

import './CardGrid.m.less';

/**
 * 卡片网格组件
 *
 * @param {Object} props - 组件属性
 * @param {Array} props.items - 数据项列表
 * @param {Function} props.renderItem - 渲染单项的函数
 * @param {number} props.columns - 列数（默认 3）
 * @param {number} props.gap - 间距（默认 16）
 */
function CardGrid(props) {
  const { items = [], renderItem, columns = 3, gap = 16 } = props;

  return (
    <div
      className={`card-grid card-grid-cols-${columns}`}
      style={{ gap: 0 }}
    >
      {items.map((item, index) => (
        <div key={item.id || index} className="card-grid-item">
          {renderItem(item, index)}
        </div>
      ))}
    </div>
  );
}

export default CardGrid;
