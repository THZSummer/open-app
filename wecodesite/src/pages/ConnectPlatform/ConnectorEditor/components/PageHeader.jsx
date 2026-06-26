/**
 * ========================================
 * 连接器编辑页 - 页面头部组件
 * ========================================
 *
 * 包含：面包屑导航 + 页面标题
 */

import React from 'react';
import { useNavigate } from 'react-router-dom';
import { queryParams } from '../../../../utils/common';

/**
 * 页面头部组件
 * 渲染面包屑（连接器列表 > 接口配置）与标题区域
 * 标题优先展示 URL 中携带的连接器名称
 */
const PageHeader = () => {
  // 路由跳转 hook
  const navigate = useNavigate();

  // 当前连接器名称，用于页面标题展示
  const connectorName = queryParams('name') || '接口配置';

  /**
   * 跳转到连接器列表页
   * 携带 appId，避免列表页拿不到数据
   */
  const handleBackToList = () => {
    // 当前 URL 上的 appId
    const appId = queryParams('appId');
    navigate(`/connect/connectors?appId=${appId}`);
  };

  return (
    <>
      {/* 面包屑：连接器列表 / 接口配置（参考版本详情页样式） */}
      <div className="page-breadcrumb">
        {/* 可点击：跳转到连接器列表 */}
        <span className="page-breadcrumb-link" onClick={handleBackToList}>
          连接器列表
        </span>
        <span className="page-breadcrumb-sep">&gt;</span>
        {/* 当前详情页：纯文本，不可点击 */}
        <span>接口配置</span>
      </div>

      <div className="page-header">
        <div>
          <h2>{connectorName}</h2>
          <p className="page-desc">维护连接器接口契约。默认只读，草稿点击"编辑"后可修改。</p>
        </div>
      </div>
    </>
  );
};

export default PageHeader;
