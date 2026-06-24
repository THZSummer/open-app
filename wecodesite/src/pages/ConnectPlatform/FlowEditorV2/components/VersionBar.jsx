/**
 * ========================================
 * 版本栏组件
 * ========================================
 *
 * 左侧：版本下拉 + 详情按钮
 * 右铡：版本操作按钮由父组件通过 onAction 在 index 中渲染
 * 无版本时展示"暂无版本"文案 + "添加版本"按钮
 */

import React from 'react';
import { Select, Button, Tag, Space } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { VERSION_STATUS_MAP, VERSION_ACTIONS } from '../constants';

const { Option } = Select;

/**
 * 按钮渲染顺序（从左到右）
 * 详情按钮独立渲染在最左侧；其余按钮按下方优先级稳定重排。
 * 顺序：更多配置 / 调试 → 新增草稿 / 编辑 / 保存 → 发布 / 撤回 → 失效 / 删除
 */
const BUTTON_ORDER = [
  'moreConfig',
  'debug',
  'newDraft',
  'edit',
  'save',
  'publish',
  'withdraw',
  'expire',
  'delete',
];

/**
 * 版本栏
 *
 * @param {Object} props
 * @param {Array} props.versionList 版本列表
 * @param {Object} props.currentVersion 当前版本
 * @param {boolean} props.actionLoading 操作加载中
 * @param {boolean} props.isEditing 草稿是否处于编辑态（控制 编辑/保存 互斥按钮展示）
 * @param {Function} props.onVersionChange 版本切换 (versionId) => void
 * @param {Function} props.onAction 操作触发 (action) => void
 */
const VersionBar = (props) => {
  const {
    versionList,
    currentVersion,
    actionLoading,
    isEditing,
    onVersionChange,
    onAction,
  } = props;

  const hasVersions = versionList && versionList.length > 0;

  /**
   * 渲染版本选项内容
   * @param {Object} version 版本对象
   * @returns {React.ReactNode}
   */
  const renderVersionOption = (version) => {
    const statusInfo = VERSION_STATUS_MAP[version.status] || { text: version.status, color: 'default' };
    return (
      <Space size={8}>
        <span className="version-option-name">{version.name}</span>
        <span className="version-option-time">{version.createTime}</span>
        <Tag color={statusInfo.color} style={{ marginRight: 0 }}>{statusInfo.text}</Tag>
      </Space>
    );
  };

  /**
   * 根据当前版本状态与编辑态计算实际渲染的按钮列表，并按 BUTTON_ORDER 重排
   * 仅草稿状态需要做"编辑/保存"互斥替换；非草稿状态走原列表。
   * @returns {Array} 按钮配置列表（已重排）
   */
  const getRenderActions = () => {
    const list = VERSION_ACTIONS[currentVersion?.status] || [];
    // 草稿态下根据 isEditing 把 save 替换成 edit
    const mapped = currentVersion?.status === 'DRAFT'
      ? list.map((btn) => {
        if (btn.action === 'save' && !isEditing) {
          return { label: '编辑', action: 'edit', type: 'default' };
        }
        return btn;
      })
      : list;
    // 按 BUTTON_ORDER 稳定重排：未在顺序表中的按钮排到末尾，并保持其原相对顺序
    const orderIndex = (action) => {
      const idx = BUTTON_ORDER.indexOf(action);
      return idx === -1 ? BUTTON_ORDER.length : idx;
    };
    return [...mapped].sort((a, b) => orderIndex(a.action) - orderIndex(b.action));
  };

  return (
    <div className="version-bar">
      <div className="version-bar-left">
        <span className="version-bar-label">当前版本</span>
        {!hasVersions ? (
          <span className="version-empty">暂无版本</span>
        ) : (
          <Select
            className="version-select"
            value={currentVersion?.versionId}
            onChange={onVersionChange}
            disabled={actionLoading}
            optionLabelProp="label"
          >
            {versionList.map(v => (
              // label 与下拉项使用同一份 JSX，确保选中态展示与下拉一致：版本名 + 创建时间 + 状态 Tag
              <Option key={v.versionId} value={v.versionId} label={renderVersionOption(v)}>
                {renderVersionOption(v)}
              </Option>
            ))}
          </Select>
        )}
      </div>

      <div className="version-bar-actions">
        {!hasVersions ? (
          <Button
            type="primary"
            icon={<PlusOutlined />}
            loading={actionLoading}
            onClick={() => onAction('addVersion')}
          >
            添加版本
          </Button>
        ) : (
          <>
            <Button onClick={() => onAction('detail')}>
              详情
            </Button>
            {/* 按当前版本状态从 VERSION_ACTIONS 常量驱动按钮渲染，确保与文档 6.2.2 节按钮规则一致；草稿态下 编辑/保存 互斥渲染 */}
            {getRenderActions().map((btn) => (
              <Button
                key={btn.action}
                type={btn.type}
                danger={btn.danger}
                className={btn.danger ? 'danger-btn' : btn.type === 'primary' ? 'primary-btn' : undefined}
                loading={actionLoading && (btn.action === 'save' || btn.action === 'publish' || btn.action === 'newDraft')}
                onClick={() => onAction(btn.action)}
              >
                {btn.label}
              </Button>
            ))}
          </>
        )}
      </div>
    </div>
  );
};

export default VersionBar;