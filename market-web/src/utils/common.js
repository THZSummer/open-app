/**
 * 通用工具函数
 */
import { Tooltip } from 'antd';

/**
 * 始终渲染带 tooltip 的单元格内容
 * 不判断文本长度，始终显示 tooltip
 * @param {string} text - 单元格文本
 * @returns {JSX.Element} 渲染后的单元格元素
 */
export const renderAlwaysWithTooltip = (text) => {
  if (!text) return '-';
  return (
    <Tooltip title={text} placement="topLeft">
      <span>{text}</span>
    </Tooltip>
  );
};
