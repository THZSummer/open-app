import React, { useState, useEffect } from 'react';
import { Drawer, Tabs, Table, Button, Tag, Pagination, Input, Select, message } from 'antd';
const { TabPane } = Tabs;
import { fetchApis, fetchCategories } from './thunk';
import { mockAppInfo } from '../BasicInfo/mock';
import { AUTH_TYPE, PAGE_SIZE_OPTIONS, INIT_PAGECONFIG } from '../../utils/constants';
import {
  NEED_REVIEW_OPTIONS,
  IDENTITY_TABS,
  BUSINESS_BUSINESS_API_TABS,
  BUSINESS_PERSONAL_API_TABS,
  PERSONAL_API_TABS,
  getApiPermissionDrawerColumns,
} from './constants';
import './ApiPermissionDrawer.m.less';

/**
 * 将分类数据转换为模块列表格式
 * 后端返回的分类数据结构转换为侧边栏模块列表所需的格式
 * @param {Array} categories - 分类数据数组
 * @returns {Array} 转换后的模块列表，包含'全部'选项
 */
const transformCategoriesToModules = (categories) => {
  if (!Array.isArray(categories) || categories.length === 0) return [];

  const result = [];
  const firstCategoryId = categories[0]?.id;
  if (firstCategoryId) {
    result.push({
      key: 'all',
      value: firstCategoryId,
      name: '全部分类'
    });
  }

  categories.forEach(cat => {
    if (cat.children && Array.isArray(cat.children)) {
      cat.children.forEach(child => {
        if (child.id) {
          result.push({
            key: child.id,
            value: child.id,
            name: child.nameCn || child.name
          });
        }
      });
    }
  });

  return result;
};

/**
 * API权限开通抽屉组件
 * 用于从可用的API列表中选择需要开通的权限
 * @param {boolean} open - 抽屉显示状态
 * @param {Function} onClose - 关闭抽屉回调
 * @param {Function} onConfirm - 确认开通权限回调
 * @param {string} appId - 应用ID
 */
function ApiPermissionDrawer({ open, onClose, onConfirm, appId }) {
  // 是否启用身份权限功能开关（仅控制第一层Tab是否显示）
  const enableIdentityPermission = true;
  
  // 根据 appId 获取应用信息，判断是业务应用还是个人应用
  const appInfo = appId ? mockAppInfo[appId] : null;
  const appType = appInfo && appInfo.eamap ? 'business' : 'personal';

  // 当前选中的身份类型（BUSINESS_IDENTITY或PERSONAL_IDENTITY）
  const [activeIdentityType, setActiveIdentityType] = useState('BUSINESS_IDENTITY');
  // 当前选中的API类型（app_type_a、app_type_b、personal_aksk等）
  const [activeApiType, setActiveApiType] = useState('api_business_app_soa');
  // 当前选中的模块（侧边栏分类）
  const [activeModule, setActiveModule] = useState('all');
  // 表格选中行的key数组
  const [selectedRowKeys, setSelectedRowKeys] = useState([]);
  // 分页配置对象
  const [pagination, setPagination] = useState(INIT_PAGECONFIG);
  // API列表数据
  const [apisData, setApisData] = useState([]);
  // 模块列表数据（侧边栏）
  const [modulesData, setModulesData] = useState([]);
  // 数据加载状态
  const [loading, setLoading] = useState(false);
  // 搜索关键词（权限名称/Scope合并搜索）
  const [filterKeyword, setFilterKeyword] = useState('');
  // 是否需要审核筛选条件
  const [filterNeedReview, setFilterNeedReview] = useState('all');

  /**
   * 加载模块列表
   * @param {string} identityType - 身份类型
   * @param {string} apiType - API类型
   */
  const loadModules = async (apiType) => {
    setLoading(true);
    const categoriesRes = await fetchCategories(apiType);
    if (categoriesRes && categoriesRes.code === '200') {
      const transformedModules = transformCategoriesToModules(categoriesRes.data || []);
      setModulesData(transformedModules);
      return transformedModules;
    } else if (Array.isArray(categoriesRes)) {
      const transformedModules = transformCategoriesToModules(categoriesRes);
      setModulesData(transformedModules);
      return transformedModules;
    } else {
      message.error(categoriesRes?.message || '加载分类失败');
      setModulesData([]);
      return [];
    }
  };

  /**
   * 加载API列表
   * @param {Object} params - 可选的参数覆盖
   * @param {Array} modules - 可选的手动传入的模块列表
   * @param {string} activeModuleKey - 可选的手动传入的当前选中模块key
   */
  const loadApis = async (params = {}, modules = null, activeModuleKey = null) => {
    setLoading(true);
    let currentCategoryId;
    const targetModules = modules || modulesData;
    const currentModule = activeModuleKey !== null ? activeModuleKey : activeModule;
    
    if (currentModule === 'all') {
      currentCategoryId = targetModules[0]?.value || ''
    } else {
      const selectModule = targetModules.find(module => module.key === currentModule);
      currentCategoryId = selectModule?.value || ''
    }

    if (!currentCategoryId) {
      setLoading(false);
      return;
    }
    
    const defaultParams = {
      identityType: activeIdentityType,
      apiType: activeApiType,
      keyword: filterKeyword,
      needReview: filterNeedReview,
      categoryId: currentCategoryId,
      curPage: pagination.curPage,
      pageSize: pagination.pageSize,
      appId: appId,
      ...params
    };
    
    const result = await fetchApis(defaultParams);
    if (result && result.code === '200') {
      const resultData = result.data || [];
      const resultTotal = result.total || resultData.length;
      setApisData(resultData);
      setPagination(prev => ({ ...prev, total: resultTotal }));
    } else if (Array.isArray(result?.data)) {
      setApisData(result.data);
      setPagination(prev => ({ ...prev, total: result.total || result.data.length }));
    } else {
      message.error(result?.message || result?.messageZh || '加载API列表失败');
      setApisData([]);
      setPagination(prev => ({ ...prev, total: 0 }));
    }
    setLoading(false);
  };

  /**
   * 抽屉打开时初始化状态并加载数据
   */
  useEffect(() => {
    if (!open) return;
    
    if (appType === 'business') {
      setActiveIdentityType('BUSINESS_IDENTITY');
      setActiveApiType('api_business_app_soa');
    } else {
      setActiveIdentityType('PERSONAL_IDENTITY');
      setActiveApiType('api_personal_user_aksk');
    }
    setActiveModule('all');
    setFilterKeyword('');
    setFilterNeedReview('all');
    setPagination(INIT_PAGECONFIG);
    setSelectedRowKeys([]);
    
    const initData = async () => {
      const apiType = appType === 'business' ? 'api_business_app_soa' : 'api_personal_user_aksk';
      const modules = await loadModules(apiType);
      await loadApis({}, modules, 'all');
    };
    
    initData();
  }, [open]);

  /**
   * 处理模块点击事件
   */
  const handleModuleClick = async (module) => {
    setActiveModule(module.key);
    setPagination(INIT_PAGECONFIG);
    setSelectedRowKeys([]);
    await loadApis({ curPage: 1 }, null, module.key);
  };

  /**
   * 处理身份类型Tab切换
   */
  const handleIdentityChange = async (identityType) => {
    let newApiType;
    if (appType === 'business') {
      if (identityType === 'BUSINESS_IDENTITY') {
        newApiType = 'api_business_app_soa';
      } else {
        newApiType = 'api_business_user_soa';
      }
    } else {
      newApiType = 'api_personal_user_aksk';
    }
    
    setActiveIdentityType(identityType);
    setActiveApiType(newApiType);
    setActiveModule('all');
    setFilterKeyword('');
    setFilterNeedReview('all');
    setPagination(INIT_PAGECONFIG);
    setSelectedRowKeys([]);
    
    const modules = await loadModules(newApiType);
    await loadApis({ 
      identityType, 
      apiType: newApiType,
      keyword: '',
      needReview: 'all',
      curPage: 1,
      categoryId: modules[0]?.value
    }, modules, 'all');
  };

  /**
   * 处理API类型Tab切换
   */
  const handleApiTypeChange = async (type) => {
    setActiveApiType(type);
    setActiveModule('all');
    setFilterKeyword('');
    setFilterNeedReview('all');
    setPagination(INIT_PAGECONFIG);
    setSelectedRowKeys([]);
    
    const modules = await loadModules(type);
    await loadApis({
      identityType: activeIdentityType,
      apiType: type,
      keyword: '',
      needReview: 'all',
      curPage: 1,
      categoryId: modules[0]?.value
    }, modules, 'all');
  };

  /**
   * 处理分页变化
   */
  const handlePageChange = async (page, newPageSize) => {
    const newPagination = {
      ...pagination,
      curPage: page,
      ...(newPageSize && newPageSize !== pagination.pageSize ? { pageSize: newPageSize } : {})
    };
    setPagination(newPagination);
    await loadApis({ 
      curPage: page, 
      pageSize: newPageSize || pagination.pageSize 
    }, null, activeModule);
  };

  /**
   * 处理表格选中行变化
   * @param {Array} keys - 选中行的key数组
   */
  const handleSelectChange = (keys) => {
    setSelectedRowKeys(keys);
  };

  /**
   * 处理确认开通权限
   * 将选中的API传递给父组件
   */
  const handleConfirm = () => {
    const selectedApis = apisData.filter(api => selectedRowKeys.includes(api.id));
    onConfirm(selectedApis);
    setSelectedRowKeys([]);
    setPagination(INIT_PAGECONFIG);
    onClose();
  };

  /**
   * 处理关键词输入变化
   */
  const handleFilterChange = async (e) => {
    const keyword = e.target.value;
    setFilterKeyword(keyword);
    setPagination(INIT_PAGECONFIG);
    await loadApis({ keyword, curPage: 1 }, null, activeModule);
  };

  /**
   * 处理是否需要审核筛选变化
   */
  const handleNeedReviewChange = async (value) => {
    setFilterNeedReview(value);
    setPagination(INIT_PAGECONFIG);
    await loadApis({ needReview: value, curPage: 1 }, null, activeModule);
  };

  const handleOpenDoc = (docUrl) => {
    if (docUrl) {
      window.open(docUrl, '_blank');
    }
  };

  const columns = getApiPermissionDrawerColumns({ handleOpenDoc });

  // 表格行选择器配置
  const rowSelection = {
    selectedRowKeys,
    onChange: handleSelectChange,
    getCheckboxProps: (record) => ({
      disabled: record.isSubscribed === 1,  // 已订阅的权限禁用勾选
    }),
  };

  /**
   * 渲染第一层Tab（身份权限Tab）
   * 仅在启用身份权限时显示
   */
  const renderFirstLevelTabs = () => {
    if (!enableIdentityPermission) {
      return null;
    }
    const identityTabs = appType === 'business' ?
      IDENTITY_TABS : [IDENTITY_TABS[1]];
    return (
      <Tabs
        activeKey={activeIdentityType}
        onChange={handleIdentityChange}
      >
        {identityTabs.map(tab => (
          <TabPane key={tab.key} tab={tab.label} />
        ))}
      </Tabs>
    );
  };

  /**
   * 渲染第二层Tab（API类型Tab）
   * 企业应用显示SOA/APIG类型，个人应用显示AKSK类型
   */
  const renderSecondLevelTabs = () => {
    const apiTabs = appType === 'business' 
      ? (activeIdentityType === 'BUSINESS_IDENTITY' ? BUSINESS_BUSINESS_API_TABS : BUSINESS_PERSONAL_API_TABS)
      : PERSONAL_API_TABS;
    console.log('apiTabs, activeApiType', apiTabs, activeApiType);
    return (
      <Tabs
        activeKey={activeApiType}
        onChange={handleApiTypeChange}
      >
        {apiTabs.map(tab => (
          <TabPane key={tab.key} tab={tab.label} />
        ))}
      </Tabs>
    );
  };

  return (
    <Drawer
      title="开通权限"
      placement="right"
      width={900}
      onClose={onClose}
      open={open}
      className="api-permission-drawer"
      footer={
        <div className="drawer-footer">
          <Button onClick={onClose}>取消</Button>
          <Button
            type="primary"
            disabled={selectedRowKeys.length === 0}
            onClick={handleConfirm}
          >
            确认开通权限
          </Button>
        </div>
      }
    >
      <div className="drawer-content">
        {renderFirstLevelTabs()}
        {renderSecondLevelTabs()}
        <div className="drawer-filter">
          <Input
            placeholder="权限名称/Scope"
            value={filterKeyword}
            onChange={handleFilterChange}
            style={{ width: 200 }}
            allowClear
          />
          <Select
            placeholder="是否需要审核"
            value={filterNeedReview}
            onChange={handleNeedReviewChange}
            options={NEED_REVIEW_OPTIONS}
            style={{ width: 150 }}
          />
        </div>
        <div className="drawer-main">
          <div className="drawer-sidebar">
            <ul className="module-list">
              {modulesData.map(module => (
                <li
                  key={module.key}
                  className={`module-item ${activeModule === module.key ? 'active' : ''}`}
                  onClick={() => handleModuleClick(module)}
                >
                  {module.name}
                </li>
              ))}
            </ul>
          </div>
          <div className="drawer-table">
            <Table
              rowSelection={rowSelection}
              columns={columns}
              dataSource={apisData}
              rowKey="id"
              pagination={false}
              loading={loading}
            />
            <div className="drawer-pagination">
              <span className="pagination-total">共 {pagination.total} 条</span>
              <Pagination
                current={pagination.curPage}
                pageSize={pagination.pageSize}
                total={pagination.total}
                onChange={handlePageChange}
                showSizeChanger
                pageSizeOptions={PAGE_SIZE_OPTIONS}
                showQuickJumper
              />
            </div>
          </div>
        </div>
      </div>
    </Drawer>
  );
}

export default ApiPermissionDrawer;