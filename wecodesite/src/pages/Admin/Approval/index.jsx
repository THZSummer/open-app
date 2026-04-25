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
  Progress, Statistic, Avatar, Badge, Timeline, Collapse,
  message
} from 'antd';
import {
  CheckOutlined,
  CloseOutlined,
  EyeOutlined,
  UserOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  ClockCircleOutlined,
  SyncOutlined,
} from '@ant-design/icons';
import {
  fetchApprovalList,
  fetchMyApprovals,
  fetchApprovalDetail,
  approveApplication,
  rejectApplication,
} from './thunk';
import { STATUS_MAP, APPROVAL_TYPE_MAP, APPROVAL_TABS, getApprovalColumns, getMyApprovalColumns, NODE_STATUS_MAP, LEVEL_MAP } from './constants';
import { TYPE_MAP, LEVEL_MAP as MOCK_LEVEL_MAP } from './mock';
import ApprovalFlowConfig from './ApprovalFlowConfig';
import './ApprovalCenter.m.less';
import { INIT_PAGECONFIG, PAGE_SIZE_OPTIONS } from '../../../utils/constants';

function ApprovalCenter() {
  const [loading, setLoading] = useState(false); 
  const [approvalList, setApprovalList] = useState([]);
  const [myApprovals, setMyApprovals] = useState([]);
  const [activeTab, setActiveTab] = useState('pending');
  const [detailVisible, setDetailVisible] = useState(false);
  const [currentDetail, setCurrentDetail] = useState(null);
  const [pagination, setPagination] = useState(INIT_PAGECONFIG);
  const [rejectModalVisible, setRejectModalVisible] = useState(false);
  const [rejectingId, setRejectingId] = useState(null);
  const [rejectComment, setRejectComment] = useState('');
  const [approveModalVisible, setApproveModalVisible] = useState(false);
  const [approvingId, setApprovingId] = useState(null);
  const [approveComment, setApproveComment] = useState('');

  useEffect(() => {
    loadData(INIT_PAGECONFIG);
  }, [activeTab]);

  const loadData = async (params = {}) => {
    setLoading(true);
    const finalPage = 'curPage' in params ? params.curPage : pagination.curPage;
    const finalSize = 'pageSize' in params ? params.pageSize : pagination.pageSize;
    let result;

    if (activeTab === 'pending') {
      result = await fetchApprovalList({ status: 0, ...params });
      if (result.code === '200') {
        setApprovalList(result.data);
        setPagination(prev => ({ ...prev, total: result.page?.total || 0, curPage: finalPage, pageSize: finalSize }));
      }
    } else if (activeTab === 'mine') {
      result = await fetchMyApprovals(params);
      if (result.code === '200') {
        setMyApprovals(result.data);
        setPagination(prev => ({ ...prev, total: result.page?.total || 0, curPage: finalPage, pageSize: finalSize }));
      }
    } else if (activeTab === 'all') {
      result = await fetchApprovalList(params);
      if (result.code === '200') {
        setApprovalList(result.data);
        setPagination(prev => ({ ...prev, total: result.page?.total || 0, curPage: finalPage, pageSize: finalSize }));
      }
    }

    setLoading(false);
  };

  const handlePageChange = (page, size) => {
    const params = {
      curPage: page,
      pageSize: size
    }
    loadData(params);
  };

  const handleApprove = (id) => {
    setApprovingId(id);
    setApproveComment('');
    setApproveModalVisible(true);
  };

  const handleConfirmApprove = async () => {
    const res = await approveApplication(approvingId, { comment: approveComment });
    if (res && res.code === '200') {
      message.success('审批通过');
      setApproveModalVisible(false);
      loadData();
    } else {
      message.error(res?.messageZh || res?.message || '审批失败');
    }
  };

  const handleReject = (id) => {
    setRejectingId(id);
    setRejectComment('');
    setRejectModalVisible(true);
  };

  const handleConfirmReject = async () => {
    if (!rejectComment.trim()) {
      message.warning('请输入审批意见');
      return;
    }
    const res = await rejectApplication(rejectingId, { comment: rejectComment });
    if (res && res.code === '200') {
      message.success('审批已拒绝');
      setRejectModalVisible(false);
      loadData();
    } else {
      message.error(res?.messageZh || res?.message || '审批失败');
    }
  };

  const handleViewDetail = async (record) => {
    setLoading(true);
    try {
      const result = await fetchApprovalDetail(record.id);
      if (result.code === '200') {
        setCurrentDetail(result.data);
        setDetailVisible(true);
      }
    } finally {
      setLoading(false);
    }
  };

  const columns = getApprovalColumns({
    handleViewDetail,
    handleApprove,
    handleReject,
  });

  const myColumns = getMyApprovalColumns({
    handleViewDetail,
  });

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
                  total={pagination.total}
                  current={pagination.curPage}
                  pageSize={pagination.pageSize}
                  pageSizeOptions={PAGE_SIZE_OPTIONS}
                  showSizeChanger
                  showQuickJumper
                  showTotal={(total) => `共 ${pagination.total} 条`}
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
            {/* 进度概览卡片 */}
            {currentDetail.combinedNodes && currentDetail.combinedNodes.length > 0 && (() => {
              const totalCount = currentDetail.combinedNodes.length;
              const completedCount = currentDetail.combinedNodes.filter(n => n.status === 1).length;
              const currentNodeIndex = currentDetail.currentNode;
              const currentNode = currentDetail.combinedNodes[currentNodeIndex];
              const pendingNodes = currentDetail.combinedNodes.filter(n => n.status === null && n !== currentNode);

              return (
                <Card size="small" className="approval-progress-card" style={{ marginBottom: 16 }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
                    <div style={{ flex: '0 0 120px' }}>
                      <Statistic
                        title="审批进度"
                        value={completedCount}
                        suffix={`/ ${totalCount} 节点`}
                        valueStyle={{ fontSize: 20 }}
                      />
                    </div>
                    <div style={{ flex: 1 }}>
                      <Progress
                        percent={Math.round(completedCount / totalCount * 100)}
                        status={currentDetail.status === 2 ? 'exception' : 'active'}
                        strokeColor={{
                          '0%': '#108ee9',
                          '100%': '#87d068',
                        }}
                      />
                      <div style={{ marginTop: 12 }}>
                        {currentDetail.status === 0 && currentNode && (
                          <>
                            <span style={{ color: '#8c8c8c' }}>当前节点：</span>
                            <Tag color="processing" icon={<SyncOutlined spin />}>
                              {LEVEL_MAP[currentNode.level]?.text || currentNode.level}
                            </Tag>
                            <strong>{currentNode.userName}</strong>
                            <span style={{ color: '#8c8c8c', marginLeft: 8 }}>
                              (第 {currentNode.order} 步)
                            </span>
                          </>
                        )}
                        {pendingNodes.length > 0 && currentDetail.status === 0 && (
                          <div style={{ marginTop: 8 }}>
                            <span style={{ color: '#8c8c8c' }}>待审批：</span>
                            {pendingNodes.map((node, idx) => (
                              <Tag key={idx} style={{ marginTop: 4 }}>
                                {node.userName}（{LEVEL_MAP[node.level]?.text}）
                              </Tag>
                            ))}
                          </div>
                        )}
                        {currentDetail.status === 1 && (
                          <span style={{ color: '#52c41a', fontWeight: 'bold' }}>✓ 审批已通过</span>
                        )}
                        {currentDetail.status === 2 && (
                          <span style={{ color: '#ff4d4f', fontWeight: 'bold' }}>✗ 审批已拒绝</span>
                        )}
                      </div>
                    </div>
                  </div>
                </Card>
              );
            })()}

            {/* 基本信息区域 */}
            <Descriptions bordered column={2} size="small">
              <Descriptions.Item label="申请编号">{currentDetail.id}</Descriptions.Item>
              <Descriptions.Item label="申请人">{currentDetail.applicantName}</Descriptions.Item>
              <Descriptions.Item label="业务类型">{currentDetail.businessType}</Descriptions.Item>
              <Descriptions.Item label="业务名称">{currentDetail.businessData?.nameCn || '-'}</Descriptions.Item>
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
                    items={currentDetail.combinedNodes.map((node, index) => {
                      const isCompleted = node.status === 1;  // 1 = 已通过
                      const isRejected = node.status === 2;   // 2 = 已拒绝
                      const isCurrent = index === currentDetail.currentNode && currentDetail.status === 0;
                      const isPending = node.status === null && !isCurrent;

                      // 根据状态选择图标
                      let icon;
                      let stepStatus;

                      if (isCompleted) {
                        icon = <CheckCircleOutlined style={{ color: '#52c41a' }} />;
                        stepStatus = 'finish';
                      } else if (isRejected) {
                        icon = <CloseCircleOutlined style={{ color: '#ff4d4f' }} />;
                        stepStatus = 'error';
                      } else if (isCurrent) {
                        icon = <SyncOutlined spin style={{ color: '#1890ff' }} />;
                        stepStatus = 'process';
                      } else {
                        icon = <ClockCircleOutlined style={{ color: '#999' }} />;
                        stepStatus = 'wait';
                      }

                      return {
                        icon,
                        status: stepStatus,
                        title: (
                          <div className="approval-node-title">
                            <div className="approver-info">
                              <Avatar size="small" icon={<UserOutlined />} style={{ marginRight: 8 }} />
                              <span className="approver-name">{node.userName}</span>
                            </div>
                            <div className="node-badges">
                              <Tag color={LEVEL_MAP[node.level]?.color || 'default'}>
                                {LEVEL_MAP[node.level]?.text || node.level}
                              </Tag>
                              {isCurrent && (
                                <Badge status="processing" text="待审批" />
                              )}
                              {isCompleted && (
                                <Tag color="success" icon={<CheckCircleOutlined />}>已同意</Tag>
                              )}
                              {isRejected && (
                                <Tag color="error" icon={<CloseCircleOutlined />}>已拒绝</Tag>
                              )}
                              {isPending && (
                                <Tag color="default" icon={<ClockCircleOutlined />}>等待中</Tag>
                              )}
                            </div>
                          </div>
                        ),
                        description: (
                          <div className="approval-node-desc">
                            <div className="node-info">
                              <span style={{ color: '#8c8c8c', fontSize: 12 }}>
                                审批人ID：{node.userId} | 节点顺序：第 {node.order} 步
                              </span>
                            </div>
                            {node.approveTime && (
                              <div className="approve-time" style={{ marginTop: 4 }}>
                                <span style={{ color: '#8c8c8c', fontSize: 12 }}>
                                  审批时间：{node.approveTime}
                                </span>
                              </div>
                            )}
                            {node.comment && (
                              <div style={{ marginTop: 4 }}>
                                <span style={{ color: '#8c8c8c', fontSize: 12 }}>
                                  审批意见：{node.comment}
                                </span>
                              </div>
                            )}
                          </div>
                        ),
                      };
                    })}
                  />
                </div>



                {/* 操作历史（审计追溯） */}
                {currentDetail.logs && currentDetail.logs.length > 0 && (
                  <Collapse
                    ghost
                    style={{ marginTop: 16 }}
                    items={[{
                      key: 'logs',
                      label: (
                        <span style={{ fontWeight: 500 }}>
                          操作历史
                          <span style={{ color: '#8c8c8c', fontSize: 12, marginLeft: 8 }}>
                            ({currentDetail.logs.length} 条记录)
                          </span>
                        </span>
                      ),
                      children: (
                        <Timeline
                          items={currentDetail.logs.map((log, idx) => {
                            // 操作类型映射
                            const actionMap = {
                              0: { text: '同意', color: 'success' },
                              1: { text: '拒绝', color: 'error' },
                              2: { text: '撤销', color: 'warning' },
                              3: { text: '转交', color: 'processing' },
                            };
                            const actionInfo = actionMap[log.action] || { text: '未知', color: 'default' };

                            return {
                              color: log.action === 0 ? 'green' : log.action === 1 ? 'red' : 'gray',
                              children: (
                                <div key={idx} style={{ paddingBottom: 8 }}>
                                  <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 4 }}>
                                    <strong>{log.operatorName}</strong>
                                    <Tag color={actionInfo.color}>{actionInfo.text}</Tag>
                                    {log.level && (
                                      <Tag color={LEVEL_MAP[log.level]?.color || 'default'}>
                                        {LEVEL_MAP[log.level]?.text || log.level}
                                      </Tag>
                                    )}
                                  </div>
                                  <span style={{ color: '#8c8c8c', fontSize: 12 }}>
                                    {log.createTime}
                                  </span>
                                  {log.comment && (
                                    <div style={{ marginTop: 4, padding: '6px 10px', background: '#fafafa', borderRadius: 4, borderLeft: '2px solid #1890ff' }}>
                                      <span style={{ fontStyle: 'italic', fontSize: 12, color: '#666' }}>
                                        "{log.comment}"
                                      </span>
                                    </div>
                                  )}
                                </div>
                              ),
                            };
                          })}
                        />
                      ),
                    }]}
                  />
                )}
              </>
            )}
          </div>
        )}
      </Modal>

      {/* 审批通过模态框 */}
      <Modal
        title="审批通过"
        open={approveModalVisible}
        onCancel={() => setApproveModalVisible(false)}
        onOk={handleConfirmApprove}
        okText="确认通过"
      >
        <div style={{ marginBottom: 8 }}>
          <Text type="secondary">审批意见（可选）：</Text>
        </div>
        <TextArea
          rows={4}
          value={approveComment}
          onChange={(e) => setApproveComment(e.target.value)}
          placeholder="请输入审批意见（可选）"
          maxLength={500}
          showCount
        />
      </Modal>

      {/* 驳回审批意见模态框 */}
      <Modal
        title="驳回审批意见"
        open={rejectModalVisible}
        onCancel={() => setRejectModalVisible(false)}
        onOk={handleConfirmReject}
        okText="确认驳回"
        okButtonProps={{ danger: true }}
      >
        <div style={{ marginBottom: 8 }}>
          <Text type="secondary">请输入审批意见：</Text>
        </div>
        <TextArea
          rows={4}
          value={rejectComment}
          onChange={(e) => setRejectComment(e.target.value)}
          placeholder="请输入审批意见"
          maxLength={500}
          showCount
        />
      </Modal>
    </div>
  );
}

export default ApprovalCenter;
