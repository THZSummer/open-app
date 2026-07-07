/**
 * ========================================
 * 应用列表 - 页面入口
 * ========================================
 *
 * 功能：
 * - 展示当前用户有权限的应用列表
 * - 支持分页（10/20/50）
 * - 点击卡片进入应用详情
 * - 创建应用弹窗
 */
import React, { useState, useEffect, lazy, Suspense } from 'react';
import { message, Spin, Pagination } from 'antd';
import { useNavigate } from 'react-router-dom';
import AppCard from '../../components/AppCard/AppCard';
import EmptyState from '../../components/EmptyState/EmptyState';
import CardGrid from '../../components/CardGrid/CardGrid';
import { fetchAppList, createApp } from './thunk';
import { INIT_PAGECONFIG } from '../../utils/constants';
import { EMPTY_CONFIG, DEFAULT_GRID_COLUMNS, DEFAULT_GRID_GAP } from './constant';

import './AppList.m.less';

const CreateAppModal = lazy(() => import('../../components/CreateAppModal/CreateAppModal'));

/**
 * 应用列表页
 *
 * 功能：
 * - 展示当前用户有权限的应用列表
 * - 支持分页（10/20/50）
 * - 点击卡片进入应用详情
 * - 创建应用弹窗
 */
function AppList() {
  const navigate = useNavigate();
  const [apps, setApps] = useState([]);
  const [pagination, setPagination] = useState(INIT_PAGECONFIG);
  const [loading, setLoading] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);
  const [modalLoading, setModalLoading] = useState(false);

  useEffect(() => {
    loadAppList();
  }, []);

  // 加载应用列表
  const loadAppList = async (params = { curPage: 1, pageSize: 10 }) => {
    setLoading(true);
    const result = await fetchAppList(params);
    if (result && result.code === '200') {
      setApps(result.data || []);
      setPagination(result.page ? {
        ...result.page,
        total: parseInt(result.page.total, 10) || 0,
      } : INIT_PAGECONFIG);
    } else {
       message.error(result?.messageZh || '加载应用列表失败');
      setApps([]);
    }
    setLoading(false);
  };

  // 分页变化
  const handlePageChange = (page, pageSize) => {
    loadAppList({ curPage: page, pageSize });
  };

  // 每页条数变化
  const handleShowSizeChange = (page, pageSize) => {
    loadAppList({ curPage: 1, pageSize });
  };

  // 点击卡片
  const handleCardClick = (app) => {
    navigate(`/basic-info?appId=${app.appId}`);
  };

  // 创建应用
  const handleCreateApp = async (values) => {
    setModalLoading(true);
    // 前端表单字段映射到后端字段
    const payload = {
      nameCn: values.chineseName,
      nameEn: values.englishName,
      descCn: values.chineseDesc || '',
      descEn: values.englishDesc || '',
      iconId: values.icon || '',
      eamapAppCode: values.eamap || '',
    };
    const result = await createApp(payload);
    if (result && result.code === '200') {
      message.success('创建应用成功');
      setModalVisible(false);
      const newAppId = result.data?.appId;
      if (newAppId) {
        navigate(`/basic-info?appId=${newAppId}`);
      } else {
        loadAppList();
      }
    } else {
      message.error(result?.messageZh || '创建应用失败');
    }
    setModalLoading(false);
  };

  // 渲染单个卡片
  const renderAppCard = (app) => (
    <AppCard key={app.appId} app={app} onClick={() => handleCardClick(app)} />
  );

  return (
    <div className="app-list-page">
      <div className='homeBanner'>
        <div style={{ width: 1200, height: 283, background: '#66ccff' }}></div>
        <div className='bannerButton' onClick={() => setModalLoading(true)}></div>
      </div>

      {/* 应用列表 */}
      <div className="app-list-section">
        <div className="appTypeName">企业应用</div>
        <Spin spinning={loading}>
          {apps.length > 0 ? (
            <>
              <CardGrid items={apps} renderItem={renderAppCard} columns={DEFAULT_GRID_COLUMNS} gap={DEFAULT_GRID_GAP} />
              <div className="app-list-pagination">
                <Pagination
                  current={pagination.curPage}
                  pageSize={pagination.pageSize}
                  total={pagination.total}
                  onChange={handlePageChange}
                  showSizeChanger
                  pageSizeOptions={[10, 20, 50]}
                  onShowSizeChange={handleShowSizeChange}
                />
              </div>
            </>
          ) : (
            <EmptyState
              icon={EMPTY_CONFIG.icon}
              title={EMPTY_CONFIG.title}
              description={EMPTY_CONFIG.description}
            />
          )}
        </Spin>
      </div>

      {/* 创建应用弹窗 */}
      <Suspense fallback={null}>
        {modalVisible && <CreateAppModal
          visible={modalVisible}
          onCancel={() => setModalVisible(false)}
          onOk={handleCreateApp}
          confirmLoading={modalLoading}
        />}
      </Suspense>
    </div>
  );
}

export default AppList;
