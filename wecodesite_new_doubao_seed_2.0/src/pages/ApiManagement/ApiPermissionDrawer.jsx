import React, { useState, useEffect } from 'react';
import { Drawer, Tabs, Checkbox, Button, Table, Pagination, message } from 'antd';
import { FileTextOutlined } from '@ant-design/icons';
import './ApiPermissionDrawer.m.less';
import mockData from './mock';

const { TabPane } = Tabs;

const ApiPermissionDrawer = ({ visible, onClose, onConfirm }) => {
  const [activeTab, setActiveTab] = useState('soa');
  const [selectedCategory, setSelectedCategory] = useState('所有');
  const [selectedApis, setSelectedApis] = useState([]);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [filteredApis, setFilteredApis] = useState([]);

  // 分类配置
  const categories = {
    soa: ['所有', '用户管理', '通讯录', '组织架构'],
    apig: ['所有', '云空间', '会议管理', '公众号', '消息推送']
  };

  useEffect(() => {
    // 初始化数据
    if (visible) {
      setActiveTab('soa');
      setSelectedCategory('所有');
      setSelectedApis([]);
      setCurrentPage(1);
      filterApis('soa', '所有');
    }
  }, [visible]);

  useEffect(() => {
    filterApis(activeTab, selectedCategory);
  }, [activeTab, selectedCategory]);

  const filterApis = (tab, category) => {
    const apis = mockData.availableApis[tab === 'soa' ? 'soa' : 'apig'];
    if (category === '所有') {
      setFilteredApis(apis);
    } else {
      setFilteredApis(apis.filter(api => api.category === category));
    }
  };

  const handleTabChange = (key) => {
    setActiveTab(key);
    setSelectedCategory('所有');
    setSelectedApis([]);
    setCurrentPage(1);
    filterApis(key, '所有');
  };

  const handleCategoryChange = (category) => {
    setSelectedCategory(category);
    setSelectedApis([]);
    setCurrentPage(1);
    filterApis(activeTab, category);
  };

  const handleApiCheck = (apiId, checked) => {
    if (checked) {
      setSelectedApis([...selectedApis, apiId]);
    } else {
      setSelectedApis(selectedApis.filter(id => id !== apiId));
    }
  };

  const handleConfirm = () => {
    if (selectedApis.length === 0) {
      message.error('请选择要开通的API');
      return;
    }
    // 获取选中的API详情
    const apis = mockData.availableApis[activeTab === 'soa' ? 'soa' : 'apig'];
    const selectedApiDetails = apis.filter(api => selectedApis.includes(api.id));
    onConfirm(selectedApiDetails);
  };

  const handleViewDoc = (api) => {
    // 打开 API 文档
    window.open(`https://open.feishu.cn/document/${api.codeName}`, '_blank');
  };

  // 分页处理
  const startIndex = (currentPage - 1) * pageSize;
  const endIndex = startIndex + pageSize;
  const paginatedApis = filteredApis.slice(startIndex, endIndex);

  const columns = [
    {
      title: (
        <Checkbox
          indeterminate={selectedApis.length > 0 && selectedApis.length < filteredApis.length}
          checked={selectedApis.length === filteredApis.length && filteredApis.length > 0}
          onChange={(e) => {
            if (e.target.checked) {
              setSelectedApis(filteredApis.map(api => api.id));
            } else {
              setSelectedApis([]);
            }
          }}
        />
      ),
      dataIndex: 'checkbox',
      key: 'checkbox',
      render: (_, record) => (
        <Checkbox
          checked={selectedApis.includes(record.id)}
          onChange={(e) => handleApiCheck(record.id, e.target.checked)}
        />
      )
    },
    {
      title: '权限名称',
      dataIndex: 'name',
      key: 'name'
    },
    {
      title: '是否需要审核',
      dataIndex: 'needApproval',
      key: 'needApproval',
      render: (needApproval) => (
        <span className={needApproval ? 'needApproval' : 'noApproval'}>
          {needApproval ? '需要审核' : '无需审核'}
        </span>
      )
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
        <Button
          type="text"
          icon={<FileTextOutlined />}
          onClick={() => handleViewDoc(record)}
        >
          查看文档
        </Button>
      )
    }
  ];

  return (
    <Drawer
      title="开通权限"
      placement="right"
      width={900}
      open={visible}
      onClose={onClose}
      footer={(
        <div className="drawerFooter">
          <Button onClick={onClose}>取消</Button>
          <Button type="primary" onClick={handleConfirm}>
            确认开通权限
          </Button>
        </div>
      )}
    >
      <Tabs activeKey={activeTab} onChange={handleTabChange}>
        <TabPane tab="SOA类型" key="soa">
          <div className="drawerContent">
            <div className="categoryList">
              {categories.soa.map(category => (
                <Button
                  key={category}
                  type={selectedCategory === category ? 'primary' : 'default'}
                  onClick={() => handleCategoryChange(category)}
                  className="categoryButton"
                >
                  {category}
                </Button>
              ))}
            </div>
            <div className="apiList">
              <Table
                columns={columns}
                dataSource={paginatedApis}
                rowKey="id"
                pagination={false}
                size="small"
              />
              <div className="pagination">
                <span className="total">
                  共 {filteredApis.length} 条
                </span>
                <Pagination
                  current={currentPage}
                  pageSize={pageSize}
                  total={filteredApis.length}
                  onChange={(page, size) => {
                    setCurrentPage(page);
                    setPageSize(size);
                  }}
                  showSizeChanger
                  pageSizeOptions={['10', '20', '50']}
                />
              </div>
            </div>
          </div>
        </TabPane>
        <TabPane tab="APIG类型" key="apig">
          <div className="drawerContent">
            <div className="categoryList">
              {categories.apig.map(category => (
                <Button
                  key={category}
                  type={selectedCategory === category ? 'primary' : 'default'}
                  onClick={() => handleCategoryChange(category)}
                  className="categoryButton"
                >
                  {category}
                </Button>
              ))}
            </div>
            <div className="apiList">
              <Table
                columns={columns}
                dataSource={paginatedApis}
                rowKey="id"
                pagination={false}
                size="small"
              />
              <div className="pagination">
                <span className="total">
                  共 {filteredApis.length} 条
                </span>
                <Pagination
                  current={currentPage}
                  pageSize={pageSize}
                  total={filteredApis.length}
                  onChange={(page, size) => {
                    setCurrentPage(page);
                    setPageSize(size);
                  }}
                  showSizeChanger
                  pageSizeOptions={['10', '20', '50']}
                />
              </div>
            </div>
          </div>
        </TabPane>
      </Tabs>
    </Drawer>
  );
};

export default ApiPermissionDrawer;