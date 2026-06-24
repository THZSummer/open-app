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
import React, { useState, useEffect } from 'react';
import { Button, message, Spin } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import AppCard from '../../components/AppCard/AppCard';
import CreateAppModal from '../../components/CreateAppModal/CreateAppModal';
import EmptyState from '../../components/EmptyState/EmptyState';
import Pagination from '../../components/Pagination/Pagination';
import CardGrid from '../../components/CardGrid/CardGrid';
import { fetchAppList, fetchEamapOptions, createApp } from './thunk';
import { INIT_PAGECONFIG } from '../../utils/constants';
import { HERO_CONFIG, EMPTY_CONFIG, DEFAULT_GRID_COLUMNS, DEFAULT_GRID_GAP } from './constant';

import './AppList.m.less';

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
  const [eamapOptions, setEamapOptions] = useState([]);
  const [modalLoading, setModalLoading] = useState(false);

  // 加载应用列表
  const loadAppList = async (params = { curPage: 1, pageSize: 10 }) => {
    setLoading(true);
    try {
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
    } catch (error) {
      message.error('加载应用列表失败');
      setApps([]);
    } finally {
      setLoading(false);
    }
  };

  // 加载弹窗所需数据
  const loadModalData = async () => {
    try {
      const eamapResult = await fetchEamapOptions({ curPage: 1, pageSize: 100 });
      if (eamapResult && eamapResult.code === '200') {
        setEamapOptions(eamapResult.data || []);
      }
    } catch (error) {
      message.error('加载弹窗数据失败');
    }
  };

  useEffect(() => {
    loadAppList();
    loadModalData();
  }, []);

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
    try {
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
    } catch (error) {
      message.error('创建应用失败');
    } finally {
      setModalLoading(false);
    }
  };

  // 渲染单个卡片
  const renderAppCard = (app) => (
    <AppCard app={app} onClick={() => handleCardClick(app)} />
  );

  return (
    <div className="app-list-page">
      {/* Hero 区域 */}
      <div className="app-list-hero">
        <h1 className="hero-title">{HERO_CONFIG.title}</h1>
        <p className="hero-desc">{HERO_CONFIG.description}</p>
        <Button
          type="primary"
          icon={<PlusOutlined />}
          className="hero-btn"
          onClick={() => setModalVisible(true)}
        >
          {HERO_CONFIG.buttonText}
        </Button>
      </div>

      {/* 应用列表 */}
      <div className="app-list-section">
        <h2 className="section-title">企业应用</h2>
        <Spin spinning={loading}>
          {apps.length > 0 ? (
            <>
              <CardGrid items={apps} renderItem={renderAppCard} columns={DEFAULT_GRID_COLUMNS} gap={DEFAULT_GRID_GAP} />
              <Pagination
                pagination={pagination}
                onChange={handlePageChange}
                onShowSizeChange={handleShowSizeChange}
              />
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
      <CreateAppModal
        visible={modalVisible}
        onCancel={() => setModalVisible(false)}
        onOk={handleCreateApp}
        eamapOptions={eamapOptions}
        confirmLoading={modalLoading}
      />
    </div>
  );
}

export default AppList;
