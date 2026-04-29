import React, { useState, useEffect } from 'react';
import { Drawer, Tabs, Table, Button, Tag, Pagination, Input, Select, message } from 'antd';
const { TabPane } = Tabs;
import { fetchApis, fetchCategories, fetchTabConfig } from './thunk';
import { mockAppInfo } from '../BasicInfo/mock';
import { AUTH_TYPE, PAGE_SIZE_OPTIONS, INIT_PAGECONFIG } from '../../utils/constants';
import {
  NEED_REVIEW_OPTIONS,
  TAB_CONFIG_SEARCH_KEY,
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

  // Tab配置状态
  const [tabConfig, setTabConfig] = useState({
    firstLevelTabs: [],
    secondLevelTabs: []
  });
  
  // 当前选中的身份类型
  const [activeIdentityType, setActiveIdentityType] = useState('');
  // 当前选中的API类型
  const [activeApiType, setActiveApiType] = useState('');
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
   * 解析原始接口数据，返回指定应用类型的Tab配置
   * @param {Object} rawData - 原始接口响应数据
   * @param {string} targetAppType - 应用类型：'business'（业务应用）或 'person'（个人应用）
   * @returns {Object} 解析后的Tab配置 { firstLevelTabs, secondLevelTabs }
   */
  const parseTabConfig = (rawData, targetAppType) => {
    try {
      if (!rawData?.data?.lookups?.[TAB_CONFIG_SEARCH_KEY]?.items) {
        return { firstLevelTabs: [], secondLevelTabs: [] };
      }
      
      const items = rawData.data.lookups[TAB_CONFIG_SEARCH_KEY].items;
      const targetItem = items.find(item => item.itemCode === targetAppType);
      
      if (!targetItem?.itemValue) {
        return { firstLevelTabs: [], secondLevelTabs: [] };
      }
      
      const parsedTabs = JSON.parse(targetItem.itemValue);
      return {
        firstLevelTabs: parsedTabs,
        secondLevelTabs: parsedTabs[0]?.children || []
      };
    } catch (error) {
      console.error('解析Tab配置失败', error);
      return { firstLevelTabs: [], secondLevelTabs: [] };
    }
  };

  /**
   * 加载Tab配置
   * 抽屉打开时调用一次，缓存到组件状态
   * 数据处理逻辑在此处完成
   */
  const loadTabConfig = async () => {
    setLoading(true);
    try {
      // 调用接口时传入 searchKey 参数
      const rawData = await fetchTabConfig(TAB_CONFIG_SEARCH_KEY);
      
      if (rawData && rawData.code === 200) {
        // 数据处理：根据应用类型筛选并解析数据
        const appTypeKey = appType === 'business' ? 'business' : 'person';
        const parsedConfig = parseTabConfig(rawData, appTypeKey);
        
        setTabConfig({
          firstLevelTabs: parsedConfig.firstLevelTabs,
          secondLevelTabs: parsedConfig.secondLevelTabs
        });
        
        // 设置默认选中的Tab
        const firstTab = parsedConfig.firstLevelTabs[0];
        const firstChildTab = firstTab?.children?.[0];
        
        setActiveIdentityType(firstTab?.key || '');
        setActiveApiType(firstChildTab?.key || '');
        
        return { firstTab, firstChildTab };
      } else {
        message.error(rawData?.message || '加载Tab配置失败');
        setTabConfig({ firstLevelTabs: [], secondLevelTabs: [] });
        return null;
      }
    } catch (error) {
      console.error('加载Tab配置失败', error);
      message.error('加载Tab配置失败');
      setTabConfig({ firstLevelTabs: [], secondLevelTabs: [] });
      return null;
    } finally {
      setLoading(false);
    }
  };

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
    
    // 重置状态
    setActiveModule('all');
    setFilterKeyword('');
    setFilterNeedReview('all');
    setPagination(INIT_PAGECONFIG);
    setSelectedRowKeys([]);
    
    const initData = async () => {
      // 1. 加载Tab配置
      const tabResult = await loadTabConfig();
      
      if (!tabResult) {
        return;
      }
      
      // 2. 加载模块列表
      const modules = await loadModules(tabResult.firstChildTab.key);
      
      // 3. 加载API列表
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
   * 切换一级Tab时，更新二级Tab列表和默认选中项
   */
  const handleIdentityChange = async (identityType) => {
    // 查找当前选中的一级Tab
    const currentFirstTab = tabConfig.firstLevelTabs.find(tab => tab.key === identityType);
    const newSecondLevelTabs = currentFirstTab?.children || [];
    const defaultSecondLevel = newSecondLevelTabs[0]?.key || '';
    
    setActiveIdentityType(identityType);
    setActiveApiType(defaultSecondLevel);
    setActiveModule('all');
    setFilterKeyword('');
    setFilterNeedReview('all');
    setPagination(INIT_PAGECONFIG);
    setSelectedRowKeys([]);
    
    // 重新加载模块列表和API列表
    if (defaultSecondLevel) {
      const modules = await loadModules(defaultSecondLevel);
      await loadApis({
        identityType,
        apiType: defaultSecondLevel,
        keyword: '',
        needReview: 'all',
        curPage: 1,
        categoryId: modules[0]?.value
      }, modules, 'all');
    }
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
   * 仅在一级Tab数量大于1时显示
   */
  const renderFirstLevelTabs = () => {
    if (!enableIdentityPermission) {
      return null;
    }
    
    const tabs = tabConfig.firstLevelTabs;
    if (!tabs || tabs.length <= 1) {
      return null;
    }
    
    return (
      <Tabs
        activeKey={activeIdentityType}
        onChange={handleIdentityChange}
      >
        {tabs.map(tab => (
          <TabPane key={tab.key} tab={tab.label} />
        ))}
      </Tabs>
    );
  };

  /**
   * 渲染第二层Tab（API类型Tab）
   * 显示当前选中的一级Tab下的所有二级Tab
   */
  const renderSecondLevelTabs = () => {
    const firstLevelTab = tabConfig.firstLevelTabs.find(tab => tab.key === activeIdentityType);
    const apiTabs = firstLevelTab?.children || [];
    
    if (apiTabs.length === 0) {
      return null;
    }
    
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