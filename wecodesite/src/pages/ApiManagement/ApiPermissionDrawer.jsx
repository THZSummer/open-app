import React, { useState, useEffect } from 'react';
import { Drawer, Tabs, Table, Button, Tag, Pagination, Input, Select } from 'antd';
import { fetchFilteredApis, fetchApiModules, fetchIdentityModules } from './thunk';
import { mockFeatureFlag } from './mock';
import { AUTH_TYPE } from '../../utils/constants';
import './ApiPermissionDrawer.m.less';

const PAGE_SIZE_OPTIONS = [10, 20, 50];
const NEED_REVIEW_OPTIONS = [
  { value: 'all', label: '全部' },
  { value: 'true', label: '需要审核' },
  { value: 'false', label: '无需审核' },
];

const IDENTITY_TABS = [
  { key: 'BUSINESS_IDENTITY', label: '业务身份权限' },
  { key: 'PERSONAL_IDENTITY', label: '个人身份权限' },
];

const BUSINESS_API_TABS = [
  { key: 'app_type_a', label: 'SOA类型' },
  { key: 'app_type_b', label: 'APIG类型' },
];

const PERSONAL_API_TABS = [
  { key: 'personal_aksk', label: 'AKSK类型' },
];

/**
 * 将分类数据转换为模块列表格式
 */
const transformCategoriesToModules = (categories) => {
  if (!Array.isArray(categories)) return [];
  
  const result = [
    { key: 'all', name: '全部' }
  ];
  
  categories.forEach(cat => {
    result.push({
      key: cat.id,
      name: cat.nameCn
    });
  });
  
  return result;
};

function ApiPermissionDrawer({ open, onClose, onConfirm, appType = 'business' }) {
  const enableIdentityPermission = mockFeatureFlag.enableIdentityPermission;
  const [activeIdentityType, setActiveIdentityType] = useState('BUSINESS_IDENTITY');
  const [activeApiType, setActiveApiType] = useState('app_type_a');
  const [activeModule, setActiveModule] = useState('all');
  const [selectedRowKeys, setSelectedRowKeys] = useState([]);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [apisData, setApisData] = useState([]);
  const [modulesData, setModulesData] = useState([]);
  const [loading, setLoading] = useState(false);
  const [filterName, setFilterName] = useState('');
  const [filterScope, setFilterScope] = useState('');
  const [filterNeedReview, setFilterNeedReview] = useState('all');

  useEffect(() => {
    if (open) {
      if (enableIdentityPermission) {
        if (appType === 'business') {
          setActiveIdentityType('BUSINESS_IDENTITY');
          setActiveApiType('app_type_a');
        } else {
          setActiveIdentityType('PERSONAL_IDENTITY');
          setActiveApiType('personal_aksk');
        }
      } else {
        if (appType === 'business') {
          setActiveApiType('app_type_a');
        } else {
          setActiveApiType('personal_aksk');
        }
        setActiveIdentityType('');
      }
      setActiveModule('all');
      setFilterName('');
      setFilterScope('');
      setFilterNeedReview('all');
      setCurrentPage(1);
      setSelectedRowKeys([]);
    }
  }, [open, appType, enableIdentityPermission]);

  useEffect(() => {
    const loadModules = async () => {
      if (enableIdentityPermission) {
        const identityKey = activeIdentityType;
        if (identityKey) {
          const categories = await fetchIdentityModules(identityKey);
          setModulesData(transformCategoriesToModules(categories));
        }
      } else if (activeApiType) {
        const categories = await fetchApiModules(activeApiType);
        setModulesData(transformCategoriesToModules(categories));
      }
    };
    if (open) {
      loadModules();
    }
  }, [activeApiType, activeIdentityType, enableIdentityPermission, open]);

  useEffect(() => {
    const loadFilteredApis = async () => {
      if (!open) return;
      setLoading(true);
      const identityKey = enableIdentityPermission ? activeIdentityType : null;
      if (identityKey) {
        const apis = await fetchFilteredApis({
          identityType: identityKey,
          name: filterName,
          scope: filterScope,
          needReview: filterNeedReview,
        });
        setApisData(apis);
      } else {
        const apis = await fetchFilteredApis({
          auth: activeApiType,
          name: filterName,
          scope: filterScope,
          needReview: filterNeedReview,
          appType: appType,
        });
        setApisData(apis);
      }
      setLoading(false);
    };
    if (open) {
      loadFilteredApis();
    }
  }, [activeApiType, activeIdentityType, enableIdentityPermission, filterName, filterScope, filterNeedReview, appType, open]);

  const filteredApis = () => {
    if (activeModule === 'all') {
      return apisData;
    }
    return apisData.filter(api => {
      if (api.categoryId) {
        return api.categoryId === activeModule;
      }
      if (api.category?.id) {
        return api.category.id === activeModule;
      }
      return false;
    });
  };

  const paginatedApis = () => {
    const data = filteredApis();
    const start = (currentPage - 1) * pageSize;
    return data.slice(start, start + pageSize);
  };

  const handleModuleClick = ({ key }) => {
    setActiveModule(key);
    setCurrentPage(1);
    setSelectedRowKeys([]);
  };

  const handleIdentityChange = async (identityType) => {
    setActiveIdentityType(identityType);
    setLoading(true);
    
    const categories = await fetchIdentityModules(identityType);
    setModulesData(transformCategoriesToModules(categories));
    
    const apis = await fetchFilteredApis({ identityType });
    setApisData(apis);
    
    setLoading(false);
    setActiveModule('all');
    setFilterName('');
    setFilterScope('');
    setFilterNeedReview('all');
    setCurrentPage(1);
    setSelectedRowKeys([]);
  };

  const handleApiTypeChange = async (type) => {
    setActiveApiType(type);
    setLoading(true);
    
    const categories = await fetchApiModules(type);
    setModulesData(transformCategoriesToModules(categories));
    
    const apis = await fetchFilteredApis({
      auth: type,
      name: '',
      scope: '',
      needReview: 'all',
      appType: appType,
    });
    setApisData(apis);
    
    setActiveModule('all');
    setFilterName('');
    setFilterScope('');
    setFilterNeedReview('all');
    setCurrentPage(1);
    setSelectedRowKeys([]);
    setLoading(false);
  };

  const handlePageChange = (page, newPageSize) => {
    setCurrentPage(page);
    if (newPageSize && newPageSize !== pageSize) {
      setPageSize(newPageSize);
    }
  };

  const handleSelectChange = (keys) => {
    setSelectedRowKeys(keys);
  };

  const handleConfirm = () => {
    const selectedApis = apisData.filter(api => selectedRowKeys.includes(api.id));
    onConfirm(selectedApis);
    setSelectedRowKeys([]);
    setCurrentPage(1);
    onClose();
  };

  const handleFilterChange = (setter) => (e) => {
    setter(e.target.value);
    setCurrentPage(1);
  };

  const handleScopeChange = (e) => {
    setFilterScope(e.target.value);
    setCurrentPage(1);
  };

  const handleNeedReviewChange = (value) => {
    setFilterNeedReview(value);
    setCurrentPage(1);
  };

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

  const rowSelection = {
    selectedRowKeys,
    onChange: handleSelectChange,
  };

  const currentFilteredApis = filteredApis();

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

  const renderSecondLevelTabs = () => {
    if (enableIdentityPermission) {
      if (appType === 'business') {
        return (
          <Tabs
            activeKey={activeApiType}
            onChange={handleApiTypeChange}
            items={BUSINESS_API_TABS}
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
    }
    const apiTabs = appType === 'business' ? BUSINESS_API_TABS : PERSONAL_API_TABS;
    return (
      <Tabs
        activeKey={activeApiType}
        onChange={handleApiTypeChange}
        items={apiTabs}
      />
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
            placeholder="权限名称"
            value={filterName}
            onChange={handleFilterChange(setFilterName)}
            style={{ width: 150 }}
            allowClear
          />
          <Input
            placeholder="Scope"
            value={filterScope}
            onChange={handleScopeChange}
            style={{ width: 150 }}
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
                  onClick={() => handleModuleClick({ key: module.key })}
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
              dataSource={paginatedApis()}
              rowKey="id"
              pagination={false}
              loading={loading}
            />
            <div className="drawer-pagination">
              <span className="pagination-total">共 {currentFilteredApis.length} 条</span>
              <Pagination
                current={currentPage}
                pageSize={pageSize}
                total={currentFilteredApis.length}
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
