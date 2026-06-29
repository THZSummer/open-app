/**
 * ========================================
 * 连接流部署版本选择弹窗
 * ========================================
 *
 * 功能：
 * - 打开后查询当前连接流的可部署版本（仅展示已发布版本）
 * - 单选目标版本；若连接流已有部署版本，默认选中当前部署版本
 * - 确认后调用部署接口，仅更新连接流-版本绑定，连接流状态保持不变
 *
 * 整改依据：连接流列表需求设计说明书 V1.3 - §7.8
 */

import React, { useEffect, useState } from 'react';
import { Modal, Spin, Empty, Tag, Table } from 'antd';
import { fetchPublishedVersions } from '../../pages/ConnectPlatform/Flow/thunk';
import { VERSION_STATUS_MAP } from '../../pages/ConnectPlatform/FlowEditorV2/constants';
import './DeployFlowModal.m.less';

/**
 * 部署版本选择弹窗
 *
 * @param {Object} props - 组件属性
 * 包含以下字段：
 * - open: 是否打开
 * - flow: 当前连接流记录（包含 id 与 deployedVersionId 字段）
 * - loading: 确认按钮 loading 状态
 * - onCancel: 取消回调
 * - onOk: 确认部署回调，参数为所选 versionId
 */
function DeployFlowModal(props) {
  // 解构传入对象中需要使用的参数
  const { open, flow, loading, onCancel, onOk } = props;

  // 可部署版本列表（仅已发布）
  const [versions, setVersions] = useState([]);

  // 版本列表加载状态
  const [versionsLoading, setVersionsLoading] = useState(false);

  // 当前选中的版本ID
  const [selectedVersionId, setSelectedVersionId] = useState(null);

  /**
   * 弹窗打开时加载已发布版本列表
   * 若连接流已有部署版本，默认选中当前部署版本
   */
  useEffect(() => {
    // 获取当前连接流 ID，列表记录中使用 id 字段。
    const flowId = flow?.id;
    if (!open || !flowId) return;

    // 重置选中状态
    setSelectedVersionId(null);

    // 拉取已发布版本（status=5 已发布）
    const loadVersions = async () => {
      setVersionsLoading(true);
      const res = await fetchPublishedVersions(flowId);
      if (res && res.code === '200') {
        const list = res.data || [];
        setVersions(list);

        // 当前部署版本如仍在可部署列表中，则默认选中
        const currentDeployedId = flow.deployedVersionId;
        const found = list.find((v) => v.versionId === currentDeployedId);
        if (found) {
          setSelectedVersionId(currentDeployedId);
        }
      } else {
        setVersions([]);
      }
      setVersionsLoading(false);
    };

    loadVersions();
  }, [open, flow]);

  /**
   * 确认部署
   */
  const handleOk = () => {
    if (!selectedVersionId) return;
    onOk(selectedVersionId);
  };

  // 是否禁用"确认部署"按钮：无已发布版本 或 未选中版本
  const okDisabled = versions.length === 0 || !selectedVersionId;

  // 部署版本表格列配置
  const columns = [
    {
      title: '版本',
      dataIndex: 'versionNumber',
      key: 'version',
      render: (value, record) => (
        <span className="deploy-flow-modal__version-name">
          {value != null ? `v${value}` : record.versionId}
        </span>
      ),
    },
    {
      title: '时间',
      dataIndex: 'publishedTime',
      key: 'time',
      render: (value, record) => value || record.createTime || '-',
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (value) => {
        // 当前版本状态展示信息
        const statusInfo = VERSION_STATUS_MAP[value] || {};
        return (
          <Tag color={statusInfo.color || 'default'} className="deploy-flow-modal__status-tag">
            {statusInfo.text || value || '-'}
          </Tag>
        );
      },
    },
    {
      title: '当前部署',
      dataIndex: 'versionId',
      key: 'current',
      render: (value) => (
        value === flow?.deployedVersionId ? (
          <Tag color="blue" className="deploy-flow-modal__current-tag">
            当前部署
          </Tag>
        ) : '-'
      ),
    },
  ];

  return (
    <Modal
      title="选择部署版本"
      open={open}
      onCancel={onCancel}
      onOk={handleOk}
      okText="确认部署"
      cancelText="取消"
      width={520}
      centered
      okButtonProps={{ disabled: okDisabled, loading }}
      destroyOnClose
    >
      <Spin spinning={versionsLoading}>
        <div className="deploy-flow-modal">
          {versions.length === 0 && !versionsLoading ? (
            // 无可部署版本：提示并禁用确认按钮
            <Empty description="暂无已发布的版本" />
          ) : (
            <Table
              className="deploy-flow-modal__table"
              columns={columns}
              dataSource={versions}
              rowKey="versionId"
              pagination={false}
              size="small"
              rowSelection={{
                type: 'radio',
                selectedRowKeys: selectedVersionId ? [selectedVersionId] : [],
                onChange: (keys) => setSelectedVersionId(keys[0]),
              }}
              onRow={(record) => ({
                // 点击整行时选中当前版本
                onClick: () => setSelectedVersionId(record.versionId),
              })}
            />
          )}
        </div>
      </Spin>
    </Modal>
  );
}

export default DeployFlowModal;