import React from 'react';
import { Empty, Button } from 'antd';
import { InboxOutlined } from '@ant-design/icons';

import './EmptyState.m.less';

/**
 * 空状态组件
 *
 * @param {Object} props - 组件属性
 * @param {string} props.icon - 图标类型
 * @param {string} props.title - 标题
 * @param {string} props.description - 描述
 * @param {string} props.actionText - 操作按钮文字
 * @param {Function} props.onAction - 操作按钮回调
 */
function EmptyState(props) {
  const {
    icon = 'inbox',
    title = '暂无数据',
    description = '',
    actionText = '',
    onAction = null,
  } = props;
  const renderIcon = () => {
    switch (icon) {
      case 'inbox':
        return <InboxOutlined className="empty-state-icon" />;
      default:
        return <InboxOutlined className="empty-state-icon" />;
    }
  };

  return (
    <div className="empty-state">
      <Empty
        image={renderIcon()}
        description={
          <div className="empty-state-content">
            <div className="empty-state-title">{title}</div>
            {description && (
              <div className="empty-state-desc">{description}</div>
            )}
            {actionText && onAction && (
              <Button type="primary" onClick={onAction} className="empty-state-action">
                {actionText}
              </Button>
            )}
          </div>
        }
      />
    </div>
  );
}

export default EmptyState;
