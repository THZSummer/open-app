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
} from 'antd';
import {
  CheckOutlined,
  CloseOutlined,
  EyeOutlined,
} from '@ant-design/icons';
import {
  fetchApprovalList,
  fetchMyApprovals,
  approveApplication,
  rejectApplication,
} from './thunk';
import './ApprovalCenter.m.less';

const { TextArea } = Input;

const STATUS_MAP = {
  0: { text: '待审', color: 'orange' },
  1: { text: '已通过', color: 'green' },
  2: { text: '已拒绝', color: 'red' },
};

function ApprovalCenter() {
  const [loading, setLoading] = useState(false);
  const [approvalList, setApprovalList] = useState([]);
  const [myApprovals, setMyApprovals] = useState([]);
  const [activeTab, setActiveTab] = useState('pending');
  const [detailVisible, setDetailVisible] = useState(false);
  const [currentDetail, setCurrentDetail] = useState(null);

  useEffect(() => {
    loadData();
  }, [activeTab]);

  const loadData = async () => {
    setLoading(true);
    let result;
    if (activeTab === 'pending' || activeTab === 'all') {
      result = await fetchApprovalList(
        activeTab === 'pending' ? { status: 0 } : {}
      );
      if (result.code === '200') {
        setApprovalList(result.data);
      }
    } else if (activeTab === 'mine') {
      result = await fetchMyApprovals();
      if (result.code === '200') {
        setMyApprovals(result.data);
      }
    }
    setLoading(false);
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
      dataIndex: 'applicationNo',
      key: 'applicationNo',
    },
    {
      title: '申请人',
      dataIndex: 'applicant',
      key: 'applicant',
    },
    {
      title: '申请类型',
      dataIndex: 'type',
      key: 'type',
    },
    {
      title: '申请内容',
      dataIndex: 'content',
      key: 'content',
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
      dataIndex: 'applicationNo',
      key: 'applicationNo',
    },
    {
      title: '申请类型',
      dataIndex: 'type',
      key: 'type',
    },
    {
      title: '申请内容',
      dataIndex: 'content',
      key: 'content',
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
          ]}
        />

        <Spin spinning={loading}>
          {dataSource.length > 0 ? (
            <Table
              columns={cols}
              dataSource={dataSource}
              rowKey="id"
              pagination={{ pageSize: 20 }}
            />
          ) : (
            <Empty description="暂无数据" />
          )}
        </Spin>

      <Modal
        title="申请详情"
        open={detailVisible}
        onCancel={() => setDetailVisible(false)}
        footer={
          <Button onClick={() => setDetailVisible(false)}>关闭</Button>
        }
      >
        {currentDetail && (
          <div>
            <p><strong>申请编号：</strong>{currentDetail.applicationNo}</p>
            <p><strong>申请人：</strong>{currentDetail.applicant}</p>
            <p><strong>申请类型：</strong>{currentDetail.type}</p>
            <p><strong>申请内容：</strong>{currentDetail.content}</p>
            <p><strong>申请时间：</strong>{currentDetail.createTime}</p>
          </div>
        )}
      </Modal>
    </div>
  );
}

export default ApprovalCenter;