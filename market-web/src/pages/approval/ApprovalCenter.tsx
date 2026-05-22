import { useEffect, useState } from 'react';
import {
  Card,
  Table,
  Space,
  Tag,
  Button,
  Modal,
  Input,
  Popconfirm,
  Badge,
  Tabs,
  message,
  Drawer,
  Descriptions,
  Timeline,
} from 'antd';
import {
  CheckOutlined,
  CloseOutlined,
  UndoOutlined,
  EyeOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { useApproval } from '@/hooks';
import { Approval, ApprovalLog } from '@/services/approval.service';
import styles from './ApprovalCenter.module.less';

const { TextArea } = Input;
const { TabPane } = Tabs;

/**
 * 审批状态映射
 */
const statusMap: Record<number, { color: string; text: string }> = {
  0: { color: 'processing', text: '待审' },
  1: { color: 'success', text: '已通过' },
  2: { color: 'error', text: '已拒绝' },
  3: { color: 'default', text: '已撤销' },
};

/**
 * 审批类型映射
 */
const typeMap: Record<string, { color: string; text: string }> = {
  api: { color: 'blue', text: 'API 权限' },
  event: { color: 'purple', text: '事件权限' },
  callback: { color: 'cyan', text: '回调权限' },
};

/**
 * 审批中心页面
 */
const ApprovalCenter: React.FC = () => {
  const {
    loading,
    approvalList,
    total,
    fetchPendingApprovals,
    handleApprove,
    handleReject,
    handleCancel,
    handleBatchApprove,
    handleBatchReject,
  } = useApproval();

  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);
  const [detailVisible, setDetailVisible] = useState(false);
  const [currentApproval, setCurrentApproval] = useState<Approval | null>(null);
  const [rejectModalVisible, setRejectModalVisible] = useState(false);
  const [rejectReason, setRejectReason] = useState('');
  const [activeTab, setActiveTab] = useState('pending');
  const [searchParams, setSearchParams] = useState({
    status: 0, // 默认查询待审
    curPage: 1,
    pageSize: 20,
  });

  useEffect(() => {
    fetchPendingApprovals(searchParams);
  }, [searchParams]);

  /**
   * 查看审批详情
   */
  const handleViewDetail = (record: Approval) => {
    setCurrentApproval(record);
    setDetailVisible(true);
  };

  /**
   * 同意审批
   */
  const handleApproveClick = async (id: string) => {
    const result = await handleApprove(id);
    if (result) {
      fetchPendingApprovals(searchParams);
    }
  };

  /**
   * 驳回审批
   */
  const handleRejectClick = async () => {
    if (!rejectReason.trim()) {
      message.warning('请输入驳回原因');
      return;
    }
    if (currentApproval) {
      const result = await handleReject(currentApproval.id, rejectReason);
      if (result) {
        setRejectModalVisible(false);
        setRejectReason('');
        setCurrentApproval(null);
        fetchPendingApprovals(searchParams);
      }
    }
  };

  /**
   * 撤销审批
   */
  const handleCancelClick = async (id: string) => {
    const result = await handleCancel(id);
    if (result) {
      fetchPendingApprovals(searchParams);
    }
  };

  /**
   * 批量同意
   */
  const handleBatchApproveClick = async () => {
    if (selectedRowKeys.length === 0) {
      message.warning('请选择要审批的记录');
      return;
    }
    const result = await handleBatchApprove(selectedRowKeys as string[]);
    if (result) {
      setSelectedRowKeys([]);
      fetchPendingApprovals(searchParams);
    }
  };

  /**
   * 批量驳回
   */
  const handleBatchRejectClick = () => {
    if (selectedRowKeys.length === 0) {
      message.warning('请选择要驳回的记录');
      return;
    }
    Modal.confirm({
      title: '批量驳回确认',
      content: (
        <div>
          <p>确定要驳回选中的 {selectedRowKeys.length} 条审批吗？</p>
          <TextArea
            rows={4}
            placeholder="请输入驳回原因"
            onChange={(e) => setRejectReason(e.target.value)}
          />
        </div>
      ),
      onOk: async () => {
        if (!rejectReason.trim()) {
          message.warning('请输入驳回原因');
          return;
        }
        const result = await handleBatchReject(selectedRowKeys as string[], rejectReason);
        if (result) {
          setSelectedRowKeys([]);
          setRejectReason('');
          fetchPendingApprovals(searchParams);
        }
      },
    });
  };

  /**
   * 表格列定义
   */
  const columns: ColumnsType<Approval> = [
    {
      title: '申请编号',
      dataIndex: 'approvalNo',
      key: 'approvalNo',
      width: 150,
    },
    {
      title: '申请人',
      dataIndex: 'applicantName',
      key: 'applicantName',
      width: 120,
    },
    {
      title: '申请类型',
      dataIndex: 'type',
      key: 'type',
      width: 120,
      render: (type: string) => {
        const { color, text } = typeMap[type] || typeMap.api;
        return <Tag color={color}>{text}</Tag>;
      },
    },
    {
      title: '权限名称',
      dataIndex: 'permissionName',
      key: 'permissionName',
      width: 200,
      render: (text: string, record: Approval) => (
        <div>
          <div>{text}</div>
          <div style={{ color: '#999', fontSize: 12 }}>{record.permissionScope}</div>
        </div>
      ),
    },
    {
      title: '应用',
      dataIndex: 'appName',
      key: 'appName',
      width: 150,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: number) => {
        const { color, text } = statusMap[status] || statusMap[0];
        return <Badge status={color as any} text={text} />;
      },
    },
    {
      title: '申请时间',
      dataIndex: 'createTime',
      key: 'createTime',
      width: 180,
      render: (time: string) => (time ? new Date(time).toLocaleString() : '-'),
    },
    {
      title: '操作',
      key: 'action',
      width: 200,
      fixed: 'right',
      render: (_, record: Approval) => (
        <Space size="small">
          <Button
            type="link"
            size="small"
            icon={<EyeOutlined />}
            onClick={() => handleViewDetail(record)}
          >
            详情
          </Button>
          {record.status === 0 && (
            <>
              <Button
                type="link"
                size="small"
                icon={<CheckOutlined />}
                onClick={() => handleApproveClick(record.id)}
              >
                同意
              </Button>
              <Button
                type="link"
                size="small"
                danger
                icon={<CloseOutlined />}
                onClick={() => {
                  setCurrentApproval(record);
                  setRejectModalVisible(true);
                }}
              >
                驳回
              </Button>
            </>
          )}
          {record.status === 0 && (
            <Popconfirm
              title="确定撤销该审批吗？"
              onConfirm={() => handleCancelClick(record.id)}
              okText="确定"
              cancelText="取消"
            >
              <Button type="link" size="small" icon={<UndoOutlined />}>
                撤销
              </Button>
            </Popconfirm>
          )}
        </Space>
      ),
    },
  ];

  /**
   * 行选择配置
   */
  const rowSelection = {
    selectedRowKeys,
    onChange: (keys: React.Key[]) => setSelectedRowKeys(keys),
    getCheckboxProps: (record: Approval) => ({
      disabled: record.status !== 0, // 只有待审状态可选择
    }),
  };

  return (
    <div className={styles.container}>
      <Card title="审批中心">
        <Tabs activeKey={activeTab} onChange={setActiveTab}>
          <TabPane tab="待审批" key="pending">
            <div className={styles.toolbar}>
              <Space>
                <Button
                  type="primary"
                  icon={<CheckOutlined />}
                  onClick={handleBatchApproveClick}
                  disabled={selectedRowKeys.length === 0}
                >
                  批量同意 ({selectedRowKeys.length})
                </Button>
                <Button
                  danger
                  icon={<CloseOutlined />}
                  onClick={handleBatchRejectClick}
                  disabled={selectedRowKeys.length === 0}
                >
                  批量驳回 ({selectedRowKeys.length})
                </Button>
              </Space>
            </div>
            <Table
              columns={columns}
              dataSource={approvalList}
              rowKey="id"
              loading={loading}
              rowSelection={rowSelection}
              pagination={{
                current: searchParams.curPage,
                pageSize: searchParams.pageSize,
                total,
                showSizeChanger: true,
                showTotal: (total) => `共 ${total} 条`,
                onChange: (curPage, pageSize) => {
                  setSearchParams({ ...searchParams, curPage, pageSize });
                },
              }}
              scroll={{ x: 1400 }}
            />
          </TabPane>
          <TabPane tab="已审批" key="approved">
            <Table
              columns={columns}
              dataSource={[]}
              rowKey="id"
              pagination={false}
            />
          </TabPane>
          <TabPane tab="全部" key="all">
            <Table
              columns={columns}
              dataSource={[]}
              rowKey="id"
              pagination={false}
            />
          </TabPane>
        </Tabs>
      </Card>

      {/* 审批详情抽屉 */}
      <Drawer
        title="审批详情"
        placement="right"
        width={600}
        onClose={() => setDetailVisible(false)}
        open={detailVisible}
      >
        {currentApproval && (
          <div>
            <Descriptions title="基本信息" column={1} bordered>
              <Descriptions.Item label="申请编号">{currentApproval.approvalNo}</Descriptions.Item>
              <Descriptions.Item label="申请人">{currentApproval.applicantName}</Descriptions.Item>
              <Descriptions.Item label="申请类型">
                <Tag color={typeMap[currentApproval.type]?.color}>
                  {typeMap[currentApproval.type]?.text}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="权限名称">{currentApproval.permissionName}</Descriptions.Item>
              <Descriptions.Item label="Scope">{currentApproval.permissionScope}</Descriptions.Item>
              <Descriptions.Item label="应用">{currentApproval.appName}</Descriptions.Item>
              <Descriptions.Item label="状态">
                <Badge
                  status={statusMap[currentApproval.status]?.color as any}
                  text={statusMap[currentApproval.status]?.text}
                />
              </Descriptions.Item>
              <Descriptions.Item label="申请时间">{currentApproval.createTime}</Descriptions.Item>
            </Descriptions>

            {currentApproval.logs && currentApproval.logs.length > 0 && (
              <div style={{ marginTop: 24 }}>
                <h4>操作记录</h4>
                <Timeline>
                  {currentApproval.logs.map((log: ApprovalLog) => (
                    <Timeline.Item
                      key={log.id}
                      color={log.action === 'approve' ? 'green' : log.action === 'reject' ? 'red' : 'gray'}
                    >
                      <div>
                        <strong>{log.operatorName}</strong> - {log.action === 'approve' ? '同意' : log.action === 'reject' ? '驳回' : '撤销'}
                      </div>
                      {log.comment && <div style={{ color: '#666' }}>{log.comment}</div>}
                      <div style={{ color: '#999', fontSize: 12 }}>{log.createTime}</div>
                    </Timeline.Item>
                  ))}
                </Timeline>
              </div>
            )}
          </div>
        )}
      </Drawer>

      {/* 驳回原因模态框 */}
      <Modal
        title="驳回原因"
        open={rejectModalVisible}
        onOk={handleRejectClick}
        onCancel={() => {
          setRejectModalVisible(false);
          setRejectReason('');
          setCurrentApproval(null);
        }}
      >
        <TextArea
          rows={4}
          placeholder="请输入驳回原因"
          value={rejectReason}
          onChange={(e) => setRejectReason(e.target.value)}
        />
      </Modal>
    </div>
  );
};

export default ApprovalCenter;
