import React, { useState, useEffect } from 'react';
import { Drawer, Button, Tag } from 'antd';
import less from '../index.module.less';

/**
 * SDK版本详情抽屉组件
 */
const SdkVersionDetailDrawer = ({
  open,
  detail,
  onClose,
  onDeprecate,
  onRestore
}) => {
  const renderDetailRow = (label, value, isPre = false, isUrl = false) => {
    const displayValue = (value === null || value === undefined || value === '') ? '-' : value;
    const valueClass = isUrl && value
      ? less.detailValueUrl
      : (value === null || value === undefined || value === '')
        ? less.detailValueMuted
        : less.detailValue;

    return (
      <div className={less.detailRow} key={label}>
        <span className={less.detailLabel}>{label}</span>
        <span className={`${valueClass} ${isPre ? '' : ''}`}>{displayValue}</span>
      </div>
    );
  };

  return (
    <Drawer
      title="版本详情"
      width={520}
      onClose={onClose}
      open={open}
      destroyOnClose
      footer={
        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px' }}>
          <Button onClick={onClose}>关闭</Button>
          {detail && detail.status === 1 && (
            <Button danger onClick={() => onDeprecate(detail)}>废弃</Button>
          )}
          {detail && detail.status === 2 && (
            <Button type="primary" onClick={() => onRestore(detail)}>恢复</Button>
          )}
        </div>
      }
    >
      {detail && (
        <div>
          {renderDetailRow('版本号', detail.versionName)}
          {renderDetailRow('关联权限', detail.permissionName
            ? `${detail.permissionName}（${detail.permissionScope}）`
            : null)}
          {renderDetailRow('更新说明', detail.updateNotes, true)}
          {renderDetailRow('构建产物地址', detail.artifactUrl, false, true)}
          <div className={less.detailRow}>
            <span className={less.detailLabel}>状态</span>
            <span className={less.detailValue}>
              <Tag color={detail.status === 1 ? 'green' : 'orange'}>
                {detail.status === 1 ? '已发布' : '已废弃'}
              </Tag>
            </span>
          </div>
          {renderDetailRow('废弃原因', detail.deprecateReason, true)}
          {renderDetailRow('创建人', detail.createBy)}
          {renderDetailRow('创建时间', detail.createTime)}
          {renderDetailRow('最后更新人', detail.lastUpdateBy)}
          {renderDetailRow('最后更新时间', detail.lastUpdateTime)}
        </div>
      )}
    </Drawer>
  );
};

export default SdkVersionDetailDrawer;
