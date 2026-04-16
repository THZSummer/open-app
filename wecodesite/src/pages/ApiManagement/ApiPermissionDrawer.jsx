import React, { useState, useMemo, useEffect } from 'react';
import { Drawer, Tabs, Table, Button, Tag, Pagination } from 'antd';
import { fetchAvailableApis, fetchApiModules } from './thunk';
import './ApiPermissionDrawer.m.less';

const PAGE_SIZE_OPTIONS = [10, 20, 50];

function ApiPermissionDrawer({ open, onClose, onConfirm }) {
  const [activeType, setActiveType] = useState('SOA');
  const [activeModule, setActiveModule] = useState('all');
  const [selectedRowKeys, setSelectedRowKeys] = useState([]);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [availableApisData, setAvailableApisData] = useState({});
  const [modulesData, setModulesData] = useState([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const loadData = async () => {
      setLoading(true);
      const soaData = await fetchAvailableApis('SOA');
      const apigData = await fetchAvailableApis('APIG');
      setAvailableApisData({ SOA: soaData, APIG: apigData });
      setLoading(false);
    };
    loadData();
  }, []);

  useEffect(() => {
    const loadModules = async () => {
      const modules = await fetchApiModules(activeType);
      setModulesData(modules);
    };
    loadModules();
  }, [activeType]);

  const currentData = availableApisData[activeType];
  const modules = modulesData;
  const allApis = currentData?.apis || [];

  const filteredApis = useMemo(() => {
    if (activeModule === 'all') {
      return allApis;
    }
    const moduleItem = modules.find(m => m.key === activeModule);
    if (moduleItem) {
      return allApis.filter(api => api.category === moduleItem.name);
    }
    return allApis;
  }, [allApis, activeModule, modules]);

  const paginatedApis = useMemo(() => {
    const start = (currentPage - 1) * pageSize;
    return filteredApis.slice(start, start + pageSize);
  }, [filteredApis, currentPage, pageSize]);

  const handleModuleClick = ({ key }) => {
    setActiveModule(key);
    setCurrentPage(1);
    setSelectedRowKeys([]);
  };

  const handleTypeChange = (type) => {
    setActiveType(type);
    setActiveModule('all');
    setCurrentPage(1);
    setSelectedRowKeys([]);
  };

  const handlePageChange = (page, size) => {
    setCurrentPage(page);
    setPageSize(size);
  };

  const handleSelectChange = (keys) => {
    setSelectedRowKeys(keys);
  };

  const handleConfirm = () => {
    const selectedApis = allApis.filter(api => selectedRowKeys.includes(api.id));
    onConfirm(selectedApis);
    setSelectedRowKeys([]);
    setCurrentPage(1);
    onClose();
  };

  const handleDocClick = (url) => {
    window.open(url, '_blank');
  };

  const columns = [
    {
      title: '权限名称',
      dataIndex: 'name',
      key: 'name',
    },
    {
      title: '是否需要审核',
      dataIndex: 'needReview',
      key: 'needReview',
      render: (needReview) => (
        needReview 
          ? <Tag color="orange">需要审核</Tag> 
          : <Tag color="green">无需审核</Tag>
      ),
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
        <Button 
          type="link" 
          size="small" 
          onClick={() => handleDocClick(record.docUrl)}
        >
          查看文档
        </Button>
      ),
    },
  ];

  const rowSelection = {
    selectedRowKeys,
    onChange: handleSelectChange,
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
        <Tabs 
          activeKey={activeType} 
          onChange={handleTypeChange}
          items={[
            { key: 'SOA', label: 'SOA类型' },
            { key: 'APIG', label: 'APIG类型' },
          ]}
        />
        <div className="drawer-main">
          <div className="drawer-sidebar">
            <ul className="module-list">
              {modules.map(module => (
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
              dataSource={paginatedApis}
              rowKey="id"
              pagination={false}
              loading={loading}
            />
            <div className="drawer-pagination">
              <span className="pagination-total">共 {filteredApis.length} 条</span>
              <Pagination
                current={currentPage}
                pageSize={pageSize}
                total={filteredApis.length}
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
