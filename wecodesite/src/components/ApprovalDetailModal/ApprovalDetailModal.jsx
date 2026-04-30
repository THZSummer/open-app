import React, { useEffect, useState } from 'react';
import {
  Card,
  Button,
  Tag,
  Avatar,
  Progress,
  Statistic,
  Descriptions,
  Divider,
  Steps,
  Collapse,
  Timeline,
  Spin,
  Empty,
  message,
} from 'antd';
import {
  CheckCircleOutlined,
  CloseCircleOutlined,
  ClockCircleOutlined,
  SyncOutlined,
  UserOutlined,
} from '@ant-design/icons';
import { STATUS_MAP, LEVEL_MAP } from '../../pages/Admin/Approval/constants';

function ApprovalDetailModal({
  visible,
  approvalId,
  currentDetail,
  onClose,
  fetchDetail,
}) {
  const [loading, setLoading] = useState(false);
  const [detail, setDetail] = useState(null);

  useEffect(() => {
    if (visible && approvalId) {
      loadDetail();
    } else if (visible && currentDetail) {
      setDetail(currentDetail);
    }
  }, [visible, approvalId, currentDetail]);

  const loadDetail = async () => {
    if (!approvalId) return;

    setLoading(true);
    const result = await fetchDetail(approvalId);
    if (result && result.code === '200') {
      setDetail(result.data);
    } else {
      message.error(result?.message || '加载审批详情失败');
    }
    setLoading(false);
  };

  if (!visible) return null;

  return (
    <div style={{ maxHeight: '70vh', overflowY: 'auto' }}>
      <Spin spinning={loading}>
        {!detail ? (
          <Empty description="暂无详情数据" />
        ) : (
          <div>
            {detail.combinedNodes && detail.combinedNodes.length > 0 && (
              <Card size="small" style={{ marginBottom: 16 }}>
                {renderProgressCard(detail)}
              </Card>
            )}

            <Descriptions bordered column={2} size="small" style={{ marginBottom: 16 }}>
              <Descriptions.Item label="申请编号">{detail.id}</Descriptions.Item>
              <Descriptions.Item label="申请人">{detail.applicantName}</Descriptions.Item>
              <Descriptions.Item label="业务类型">{detail.businessType}</Descriptions.Item>
              <Descriptions.Item label="业务名称">{detail.businessData?.nameCn || '-'}</Descriptions.Item>
              <Descriptions.Item label="业务ID">{detail.businessId}</Descriptions.Item>
              <Descriptions.Item label="状态">
                <Tag color={STATUS_MAP[detail.status]?.color || 'default'}>
                  {STATUS_MAP[detail.status]?.text || '未知'}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="申请时间" span={2}>{detail.createTime}</Descriptions.Item>
            </Descriptions>

            {detail.combinedNodes && detail.combinedNodes.length > 0 && (
              <>
                <Divider orientation="left">审批流程</Divider>
                {renderApprovalFlow(detail)}
              </>
            )}

            {detail.logs && detail.logs.length > 0 && (
              <Collapse
                ghost
                style={{ marginTop: 16 }}
              >
                <Collapse.Panel
                  key="logs"
                  label={
                    <span style={{ fontWeight: 500 }}>
                      操作历史
                      <span style={{ color: '#8c8c8c', fontSize: 12, marginLeft: 8 }}>
                        ({detail.logs.length} 条记录)
                      </span>
                    </span>
                  }
                >
                  {renderOperationHistoryTimeline(detail)}
                </Collapse.Panel>
              </Collapse>
            )}
          </div>
        )}
      </Spin>
    </div>
  );
}

function renderProgressCard(detail) {
  const totalCount = detail.combinedNodes.length;
  const completedCount = detail.combinedNodes.filter(n => n.status === 1).length;
  const currentNodeIndex = detail.currentNode;
  const currentNode = detail.combinedNodes[currentNodeIndex];
  const pendingNodes = detail.combinedNodes.filter(n => n.status === null && n !== currentNode);

  return (
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
          status={detail.status === 2 ? 'exception' : 'active'}
          strokeColor={{
            '0%': '#108ee9',
            '100%': '#87d068',
          }}
        />
        <div style={{ marginTop: 12 }}>
          {detail.status === 0 && currentNode && (
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
          {pendingNodes.length > 0 && detail.status === 0 && (
            <div style={{ marginTop: 8 }}>
              <span style={{ color: '#8c8c8c' }}>待审批：</span>
              {pendingNodes.map((node, idx) => (
                <Tag key={idx} style={{ marginTop: 4 }}>
                  {node.userName}（{LEVEL_MAP[node.level]?.text}）
                </Tag>
              ))}
            </div>
          )}
          {detail.status === 1 && (
            <span style={{ color: '#52c41a', fontWeight: 'bold' }}>✓ 审批已通过</span>
          )}
          {detail.status === 2 && (
            <span style={{ color: '#ff4d4f', fontWeight: 'bold' }}>✗ 审批已拒绝</span>
          )}
        </div>
      </div>
    </div>
  );
}

function renderApprovalFlow(detail) {
  return (
    <Steps direction="vertical" current={detail.currentNode}>
      {detail.combinedNodes.map((node, index) => {
        const isCompleted = node.status === 1;
        const isRejected = node.status === 2;
        const isCurrent = index === detail.currentNode && detail.status === 0;
        const isPending = node.status === null && !isCurrent;

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

        return (
          <Steps.Step
            key={index}
            icon={icon}
            status={stepStatus}
            title={
              <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <Avatar size="small" icon={<UserOutlined />} />
                <strong>{node.userName}</strong>
                <Tag color={LEVEL_MAP[node.level]?.color || 'default'}>
                  {LEVEL_MAP[node.level]?.text || node.level}
                </Tag>
                {isCurrent && <Tag color="processing">待审批</Tag>}
                {isCompleted && <Tag color="success">已同意</Tag>}
                {isRejected && <Tag color="error">已拒绝</Tag>}
                {isPending && <Tag color="default">等待中</Tag>}
              </div>
            }
            description={
              <div style={{ marginTop: 8, color: '#8c8c8c', fontSize: 12 }}>
                <div>审批人ID：{node.userId} | 节点顺序：第 {node.order} 步</div>
                {node.approveTime && (
                  <div style={{ marginTop: 4 }}>审批时间：{node.approveTime}</div>
                )}
                {node.comment && (
                  <div style={{ marginTop: 4, fontStyle: 'italic' }}>
                    审批意见：{node.comment}
                  </div>
                )}
              </div>
            }
          />
        );
      })}
    </Steps>
  );
}

function renderOperationHistoryTimeline(detail) {
  const actionMap = {
    0: { text: '同意', color: 'success' },
    1: { text: '拒绝', color: 'error' },
    2: { text: '撤销', color: 'warning' },
    3: { text: '转交', color: 'processing' },
  };

  return (
    <Timeline>
      {detail.logs.map((log, idx) => (
        <Timeline.Item
          key={idx}
          color={log.action === 0 ? 'green' : log.action === 1 ? 'red' : 'gray'}
        >
          <div style={{ paddingBottom: 8 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 4 }}>
              <strong>{log.operatorName}</strong>
              <Tag color={actionMap[log.action]?.color || 'default'}>
                {actionMap[log.action]?.text || '未知'}
              </Tag>
              {log.level && (
                <Tag color={LEVEL_MAP[log.level]?.color || 'default'}>
                  {LEVEL_MAP[log.level]?.text || log.level}
                </Tag>
              )}
            </div>
            <div style={{ color: '#8c8c8c', fontSize: 12 }}>
              {log.createTime}
            </div>
            {log.comment && (
              <div style={{
                marginTop: 4,
                padding: '6px 10px',
                background: '#fafafa',
                borderRadius: 4,
                borderLeft: '2px solid #1890ff',
                fontStyle: 'italic',
                fontSize: 12,
                color: '#666'
              }}>
                "{log.comment}"
              </div>
            )}
          </div>
        </Timeline.Item>
      ))}
    </Timeline>
  );
}

export default ApprovalDetailModal;
