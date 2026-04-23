import React, { useState, useEffect, useCallback } from 'react';
import { Drawer, Tabs, Table, Button, Tag, Pagination, Input, Select } from 'antd';
import { fetchApis, fetchCategories } from './thunk';
import { mockFeatureFlag } from './mock';
import { AUTH_TYPE } from '../../utils/constants';
import './ApiPermissionDrawer.m.less';

// 分页每页显示条数选项
const PAGE_SIZE_OPTIONS = [10, 20, 50];

// 是否需要审核下拉选项
const NEED_REVIEW_OPTIONS = [
  { value: 'all', label: '全部' },
  { value: 'true', label: '需要审核' },
  { value: 'false', label: '无需审核' },
];

// 身份权限Tab配置（第一层Tab）
const IDENTITY_TABS = [
  { key: 'BUSINESS_IDENTITY', label: '业务身份权限' },
  { key: 'PERSONAL_IDENTITY', label: '个人身份权限' },
];

// 业务应用业务身份权限API类型Tab配置（第二层Tab）
const BUSINESS_BUSINESS_API_TABS = [
  { key: 'api_business_app_soa', label: 'SOA类型' },
  { key: 'api_business_app_apig', label: 'APIG类型' },
];

// 业务应用个人身份权限API类型Tab配置（第二层Tab）
const BUSINESS_PERSONAL_API_TABS = [
  { key: 'api_business_user_soa', label: 'SOA类型' },
  { key: 'api_business_user_apig', label: 'APIG类型' },
];

// 个人应用API类型Tab配置（第二层Tab）
const PERSONAL_API_TABS = [
  { key: 'api_personal_user_aksk', label: 'AKSK类型' },
];

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
 * @param {string} appType - 应用类型：'business'或'personal'
 */
function ApiPermissionDrawer({ open, onClose, onConfirm, appType = 'business' }) {
  // 是否启用身份权限功能开关（仅控制第一层Tab是否显示）
  const enableIdentityPermission = mockFeatureFlag.enableIdentityPermission;

  // 当前选中的身份类型（BUSINESS_IDENTITY或PERSONAL_IDENTITY）
  const [activeIdentityType, setActiveIdentityType] = useState('BUSINESS_IDENTITY');
  // 当前选中的API类型（app_type_a、app_type_b、personal_aksk等）
  const [activeApiType, setActiveApiType] = useState('api_business_app_soa');
  // 当前选中的模块（侧边栏分类）
  const [activeModule, setActiveModule] = useState('all');
  // 表格选中行的key数组
  const [selectedRowKeys, setSelectedRowKeys] = useState([]);
  // 当前页码
  const [currentPage, setCurrentPage] = useState(1);
  // 每页显示条数
  const [pageSize, setPageSize] = useState(10);
  // API列表数据
  const [apisData, setApisData] = useState([]);
  // 模块列表数据（侧边栏）
  const [modulesData, setModulesData] = useState([]);
  // 数据加载状态
  const [loading, setLoading] = useState(false);
  // API列表总数
  const [total, setTotal] = useState(0);
  // 搜索关键词（权限名称/Scope合并搜索）
  const [filterKeyword, setFilterKeyword] = useState('');
  // 是否需要审核筛选条件
  const [filterNeedReview, setFilterNeedReview] = useState('all');

  /**
   * 统一的数据加载函数
   */
  const fetchApiData = useCallback(async (params = {}) => {
    setLoading(true);
    let currentCategoryId;
    if (activeModule === 'all') {
      currentCategoryId = modulesData[0]?.value || ''
    } else {
      const selectModule = modulesData.find(module => module.key === activeModule);
      currentCategoryId = selectModule?.value || ''
    }

    if (!currentCategoryId) {
      return;
    }
    
    const defaultParams = {
      identityType: activeIdentityType,
      apiType: activeApiType,
      keyword: filterKeyword,
      needReview: filterNeedReview,
      categoryId: currentCategoryId,
      curPage: currentPage,
      pageSize: pageSize,
      ...params
    };
    
    const result = await fetchApis(defaultParams);
    const resultData = result?.data || result || [];
    const resultTotal = result?.total || (Array.isArray(resultData) ? resultData.length : 0);
    
    setApisData(resultData);
    setTotal(resultTotal);
    setLoading(false);
  }, [activeIdentityType, activeApiType, filterKeyword, filterNeedReview, activeModule, modulesData, currentPage, pageSize]);

  /**
   * 抽屉打开时初始化状态
   * 根据应用类型设置默认选中的Tab
   */
  useEffect(() => {
    if (open) {
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
      setCurrentPage(1);
      setSelectedRowKeys([]);
      setTotal(0);
    }
  }, [open, appType]);

  /**
   * 当身份类型或API类型变化时，加载对应的模块列表
   */
  useEffect(() => {
    const loadModules = async () => {
      const categories = await fetchCategories(activeApiType);
      setModulesData(transformCategoriesToModules(categories));
    };
    if (open) {
      loadModules();
    }
  }, [activeApiType, activeIdentityType, open]);

  /**
   * 当筛选条件、分页变化时，通过接口获取API列表
   */
  useEffect(() => {
    if (open) {
      fetchApiData();
    }
  }, [fetchApiData, open]);

  /**
   * 处理模块点击事件
   * @param {Object} param - 包含key的对象
   */
  const handleModuleClick = (module) => {
    setActiveModule(module.key);
    setCurrentPage(1);
    setSelectedRowKeys([]);
  };

  /**
   * 处理身份类型Tab切换
   * 切换后重新加载模块和API数据
   * @param {string} identityType - 选中的身份类型
   */
  const handleIdentityChange = async (identityType) => {
    setActiveIdentityType(identityType);
    
    const categories = await fetchCategories(activeApiType, identityType);
    setModulesData(transformCategoriesToModules(categories));
    
    setActiveModule('all');
    setFilterKeyword('');
    setFilterNeedReview('all');
    setCurrentPage(1);
    setSelectedRowKeys([]);
  };

  /**
   * 处理API类型Tab切换
   * 切换后重新加载模块和API数据
   * @param {string} type - 选中的API类型
   */
  const handleApiTypeChange = async (type) => {
    setActiveApiType(type);
    
    const categories = await fetchCategories(type, activeIdentityType);
    setModulesData(transformCategoriesToModules(categories));
    
    setActiveModule('all');
    setFilterKeyword('');
    setFilterNeedReview('all');
    setCurrentPage(1);
    setSelectedRowKeys([]);
  };

  /**
   * 处理分页变化
   * @param {number} page - 新的页码
   * @param {number} newPageSize - 新的每页条数
   */
  const handlePageChange = (page, newPageSize) => {
    setCurrentPage(page);
    if (newPageSize && newPageSize !== pageSize) {
      setPageSize(newPageSize);
    }
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
    setCurrentPage(1);
    onClose();
  };

  /**
   * 处理关键词输入变化
   * @param {Event} e - 输入事件对象
   */
  const handleFilterChange = (e) => {
    setFilterKeyword(e.target.value);
    setCurrentPage(1);
  };

  /**
   * 处理是否需要审核筛选变化
   * @param {string} value - 筛选值：'all', 'true', 'false'
   */
  const handleNeedReviewChange = (value) => {
    setFilterNeedReview(value);
    setCurrentPage(1);
  };

  // 表格列配置
  const columns = [
    {
      title: '权限名称',
      dataIndex: 'nameCn',
      key: 'nameCn',
      render: (text, record) => {
        const name = record.nameCn || record.name || '-';
        return <span>{name}</span>;
      }
    },
    {
      title: 'Scope',
      dataIndex: 'scope',
      key: 'scope',
      render: (scope) => <code>{scope || '-'}</code>,
    },
    {
      title: '是否需要审核',
      dataIndex: 'needApproval',
      key: 'needApproval',
      render: (needApproval, record) => {
        const val = needApproval !== undefined ? needApproval : record.needReview;
        return val ?
          <Tag color="orange">需要审核</Tag> :
          <Tag color="green">无需审核</Tag>;
      },
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => {
        const docUrl = record.api?.docUrl || record.docUrl;
        return (
          <Button type="link" size="small" onClick={() => window.open(docUrl, '_blank')}>
            查看文档
          </Button>
        );
      },
    },
  ];

  // 表格行选择器配置
  const rowSelection = {
    selectedRowKeys,
    onChange: handleSelectChange,
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
        items={identityTabs}
      />
    );
  };

  /**
   * 渲染第二层Tab（API类型Tab）
   * 企业应用显示SOA/APIG类型，个人应用显示AKSK类型
   */
  const renderSecondLevelTabs = () => {
    if (appType === 'business') {
      return (
        <Tabs
          activeKey={activeApiType}
          onChange={handleApiTypeChange}
          items={activeIdentityType === 'BUSINESS_IDENTITY' ? BUSINESS_BUSINESS_API_TABS : BUSINESS_PERSONAL_API_TABS}
        />
      );
    } else {
      return (
        <Tabs
          activeKey={activeApiType}
          onChange={handleApiTypeChange}
          items={PERSONAL_API_TABS}
        />
      );
    }
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
              <span className="pagination-total">共 {total} 条</span>
              <Pagination
                current={currentPage}
                pageSize={pageSize}
                total={total}
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