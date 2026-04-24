import React, { useState, useEffect } from 'react';
import {
  Card,
  Button,
  Table,
  Tag,
  Space,
  Tabs,
  Popconfirm,
  Empty,
  Spin,
  Modal,
  Input,
  Steps,
  Descriptions,
  Divider,
  Pagination,
} from 'antd';
import {
  CheckOutlined,
  CloseOutlined,
  EyeOutlined,
  UserOutlined,
} from '@ant-design/icons';
import {
  fetchApprovalList,
  fetchMyApprovals,
  approveApplication,
  rejectApplication,
  LEVEL_MAP,
} from './thunk';
import { TYPE_MAP, LEVEL_MAP as MOCK_LEVEL_MAP } from './mock';
import ApprovalFlowConfig from './ApprovalFlowConfig';
import './ApprovalCenter.m.less';

const { TextArea } = Input;

const STATUS_MAP = {
  0: { text: '待审', color: 'orange' },
  1: { text: '已通过', color: 'green' },
  2: { text: '已拒绝', color: 'red' },
  3: { text: '已撤销', color: 'default' },
};

// 审批节点状态映射
const NODE_STATUS_MAP = {
  null: { text: '待审', color: 'default' },
  0: { text: '已同意', color: 'success' },
  1: { text: '已拒绝', color: 'error' },
};

function ApprovalCenter() {
  const [loading, setLoading] = useState(false);
  const [approvalList, setApprovalList] = useState([]);
  const [myApprovals, setMyApprovals] = useState([]);
  const [activeTab, setActiveTab] = useState('pending');
  const [detailVisible, setDetailVisible] = useState(false);
  const [currentDetail, setCurrentDetail] = useState(null);
  const [total, setTotal] = useState(0);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  useEffect(() => {
    setCurrentPage(1);  // 切换 Tab 时重置页码
    loadData(1, pageSize);
  }, [activeTab]);

  const loadData = async (page = currentPage, size = pageSize) => {
    setLoading(true);
    let result;
    
    if (activeTab === 'pending') {
      // 我的待审：只查询待审状态（status=0）
      result = await fetchApprovalList({ status: 0, curPage: page, pageSize: size });
      if (result.code === '200') {
        setApprovalList(result.data);
        setTotal(result.page?.total || 0);
      }
    } else if (activeTab === 'mine') {
      // 我发起的：查询当前用户发起的所有审批
      result = await fetchMyApprovals({ curPage: page, pageSize: size });
      if (result.code === '200') {
        setMyApprovals(result.data);
        setTotal(result.page?.total || 0);
      }
    } else if (activeTab === 'all') {
      // 全部：查询所有审批记录（不传 status）
      result = await fetchApprovalList({ curPage: page, pageSize: size });
      if (result.code === '200') {
        setApprovalList(result.data);
        setTotal(result.page?.total || 0);
      }
    }
    
    setLoading(false);
  };

  const handlePageChange = (page, size) => {
    setCurrentPage(page);
    setPageSize(size);
    loadData(page, size);
  };

  const handleApprove = async (id) => {
    await approveApplication(id);
    loadData();
  };

  const handleReject = async (id) => {
    await rejectApplication(id);
    loadData();
  };

  const handleViewDetail = (record) => {
    setCurrentDetail(record);
    setDetailVisible(true);
  };

  const columns = [
    {
      title: '申请编号',
      dataIndex: 'id',
      key: 'id',
    },
    {
      title: '申请人',
      dataIndex: 'applicantName',
      key: 'applicantName',
    },
    {
      title: '业务类型',
      dataIndex: 'businessType',
      key: 'businessType',
      render: (businessType) => {
        // v2.8.0: 新增业务类型显示
        const typeMap = {
          'api_register': 'API注册',
          'event_register': '事件注册',
          'callback_register': '回调注册',
          'api_permission_apply': 'API权限申请',
          'event_permission_apply': '事件权限申请',
          'callback_permission_apply': '回调权限申请',
        };
        return typeMap[businessType] || businessType;
      },
    },
    {
      title: '业务名称',
      dataIndex: 'businessName',
      key: 'businessName',
    },
    {
      title: '审批类型',
      dataIndex: 'type',
      key: 'type',
      render: (type) => TYPE_MAP[type] || type,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status) => {
        const { text, color } = STATUS_MAP[status] || STATUS_MAP[0];
        return <Tag color={color}>{text}</Tag>;
      },
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
        <Space>
          <Button
            type="link"
            size="small"
            icon={<EyeOutlined />}
            onClick={() => handleViewDetail(record)}
          >
            详情
          </Button>
          {record.status === 0 && activeTab === 'pending' && (
            <>
              <Popconfirm
                title="确定通过该申请吗？"
                onConfirm={() => handleApprove(record.id)}
                okText="确定"
                cancelText="取消"
              >
                <Button type="link" size="small" icon={<CheckOutlined />}>
                  通过
                </Button>
              </Popconfirm>
              <Popconfirm
                title="确定拒绝该申请吗？"
                onConfirm={() => handleReject(record.id)}
                okText="确定"
                cancelText="取消"
              >
                <Button type="link" size="small" danger icon={<CloseOutlined />}>
                  拒绝
                </Button>
              </Popconfirm>
            </>
          )}
        </Space>
      ),
    },
  ];

  const myColumns = [
    {
      title: '申请编号',
      dataIndex: 'id',
      key: 'id',
    },
    {
      title: '业务类型',
      dataIndex: 'businessType',
      key: 'businessType',
    },
    {
      title: '业务名称',
      dataIndex: 'businessName',
      key: 'businessName',
    },
    {
      title: '审批类型',
      dataIndex: 'type',
      key: 'type',
      render: (type) => {
        const typeMap = {
          'resource_register': '资源注册',
          'permission_apply': '权限申请',
        };
        return typeMap[type] || type;
      },
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status) => {
        const { text, color } = STATUS_MAP[status] || STATUS_MAP[0];
        return <Tag color={color}>{text}</Tag>;
      },
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
        <Button
          type="link"
          size="small"
          icon={<EyeOutlined />}
          onClick={() => handleViewDetail(record)}
        >
          详情
        </Button>
      ),
    },
  ];

  const dataSource = activeTab === 'mine' ? myApprovals : approvalList;
  const cols = activeTab === 'mine' ? myColumns : columns;

  return (
    <div className="approval-center">
      <div className="page-header">
        <div className="page-header-left">
          <h4 className="page-title">审批中心</h4>
          <span className="page-desc">审批权限申请，处理待办事项</span>
        </div>
      </div>

      <Tabs
          activeKey={activeTab}
          onChange={setActiveTab}
          items={[
            { key: 'pending', label: '我的待审' },
            { key: 'mine', label: '我发起的' },
            { key: 'all', label: '全部' },
            { key: 'flowConfig', label: '审批流程配置' },
          ]}
        />

        {activeTab === 'flowConfig' ? (
          <ApprovalFlowConfig />
        ) : (
          <Spin spinning={loading}>
            {dataSource.length > 0 ? (
              <>
                <Table
                  columns={cols}
                  dataSource={dataSource}
                  rowKey="id"
                  pagination={false}
                />
                <div style={{ marginTop: 16, textAlign: 'right' }}>
                  <Pagination
                    total={total}
                    current={currentPage}
                    pageSize={pageSize}
                    pageSizeOptions={[10, 20, 50]}
                    showSizeChanger
                    showQuickJumper
                    showTotal={(total) => `共 ${total} 条`}
                    onChange={handlePageChange}
                  />
                </div>
              </>
            ) : (
              <Empty description="暂无数据" />
            )}
          </Spin>
        )}

      <Modal
        title="申请详情"
        open={detailVisible}
        onCancel={() => setDetailVisible(false)}
        footer={
          <Button onClick={() => setDetailVisible(false)}>关闭</Button>
        }
        width={700}
      >
        {currentDetail && (
          <div>
            {/* 基本信息区域 */}
            <Descriptions bordered column={2} size="small">
              <Descriptions.Item label="申请编号">{currentDetail.id}</Descriptions.Item>
              <Descriptions.Item label="申请人">{currentDetail.applicantName}</Descriptions.Item>
              <Descriptions.Item label="审批类型">
                {TYPE_MAP[currentDetail.type] || currentDetail.type}
              </Descriptions.Item>
              <Descriptions.Item label="业务类型">
                {(() => {
                  const typeMap = {
                    'api_register': 'API注册',
                    'event_register': '事件注册',
                    'callback_register': '回调注册',
                    'api_permission_apply': 'API权限申请',
                    'event_permission_apply': '事件权限申请',
                    'callback_permission_apply': '回调权限申请',
                  };
                  return typeMap[currentDetail.businessType] || currentDetail.businessType;
                })()}
              </Descriptions.Item>
              <Descriptions.Item label="业务名称">{currentDetail.businessName}</Descriptions.Item>
              <Descriptions.Item label="业务ID">{currentDetail.businessId}</Descriptions.Item>
              <Descriptions.Item label="状态">
                <Tag color={STATUS_MAP[currentDetail.status]?.color || 'default'}>
                  {STATUS_MAP[currentDetail.status]?.text || '未知'}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="申请时间">{currentDetail.createTime}</Descriptions.Item>
            </Descriptions>

            {/* 审批流程区域 - v2.8.0: 显示 combinedNodes（组合审批节点） */}
            {currentDetail.combinedNodes && currentDetail.combinedNodes.length > 0 && (
              <>
                <Divider orientation="left">审批流程</Divider>
                <div className="approval-flow-section">
                  <Steps
                    current={currentDetail.currentNode}
                    direction="vertical"
                    items={currentDetail.combinedNodes.map((node, index) => ({
                      title: (
                        <div className="approval-node-title">
                          <span className="approver-name">
                            <UserOutlined style={{ marginRight: 8 }} />
                            {node.userName}
                          </span>
                          <Tag 
                            color={(LEVEL_MAP[node.level] || MOCK_LEVEL_MAP[node.level] || {}).color || 'default'}
                            style={{ marginLeft: 8 }}
                          >
                            {(LEVEL_MAP[node.level] || MOCK_LEVEL_MAP[node.level] || {}).text || node.level}
                          </Tag>
                        </div>
                      ),
                      description: (
                        <div className="approval-node-desc">
                          <div className="node-info">
                            审批人ID: {node.userId} | 节点顺序: {node.order}
                          </div>
                          {node.status !== null && (
                            <Tag 
                              color={NODE_STATUS_MAP[node.status]?.color || 'default'}
                              style={{ marginTop: 4 }}
                            >
                              {NODE_STATUS_MAP[node.status]?.text || '未知'}
                            </Tag>
                          )}
                        </div>
                      ),
                      status: (() => {
                        if (index < currentDetail.currentNode) {
                          return 'finish';  // 已完成
                        } else if (index === currentDetail.currentNode && currentDetail.status === 0) {
                          return 'process';  // 当前节点
                        } else if (currentDetail.status === 2) {
                          return 'error';  // 已拒绝
                        } else if (currentDetail.status === 3) {
                          return 'wait';  // 已撤销
                        }
                        return 'wait';
                      })(),
                    }))}
                  />
                </div>
                
                {/* 审批流程说明 */}
                <div className="approval-flow-legend" style={{ marginTop: 16 }}>
                  <p style={{ color: '#666', fontSize: 12 }}>
                    审批流程说明：
                    <Tag color="blue" style={{ marginLeft: 8 }}>资源审批</Tag>
                    资源提供方审核 → 
                    <Tag color="orange" style={{ marginLeft: 8 }}>场景审批</Tag>
                    业务场景审核 → 
                    <Tag color="green" style={{ marginLeft: 8 }}>全局审批</Tag>
                    平台运营审核
                  </p>
                </div>
              </>
            )}
          </div>
        )}
      </Modal>
    </div>
  );
}

export default ApprovalCenter;