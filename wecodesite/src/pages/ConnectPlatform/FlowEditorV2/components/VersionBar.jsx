/**
 * ========================================
 * 版本栏组件
 * ========================================
 *
 * 左侧：版本下拉 + 详情 / 更多配置 / 调试按钮
 * 右侧：其他版本操作按钮由父组件通过 onAction 在 index 中处理
 * 无版本时左侧仅展示"暂无版本"文案，右侧操作区不渲染（添加版本入口由 index.jsx 主体空状态区域承载）
 */

import React from 'react';
import { Select, Button, Tag } from 'antd';
import { VERSION_STATUS, VERSION_STATUS_MAP, VERSION_ACTIONS, VERSION_BUTTON_ORDER } from '../constants';

const { Option } = Select;

/**
 * 版本栏
 *
 * @param {Object} props
 * @param {Array} props.versionList 版本列表
 * @param {Object} props.currentVersion 当前版本
 * @param {boolean} props.actionLoading 操作加载中
 * @param {boolean} props.isEditing 是否处于编辑态（草稿 / 已撤回 / 已驳回 状态下控制 编辑/保存 互斥按钮展示，默认非编辑态）
 * @param {Function} props.onVersionChange 版本切换 (versionId) => void
 * @param {Function} props.onCancelEdit 取消编辑回调
 * @param {Function} props.onAction 操作触发 (action) => void
 */
const VersionBar = (props) => {
  const {
    versionList,
    currentVersion,
    actionLoading,
    isEditing,
    onVersionChange,
    onCancelEdit,
    onAction,
  } = props;

  const hasVersions = versionList && versionList.length > 0;

  /**
   * 左侧版本辅助按钮集合
   * @returns {Array} 详情、更多配置、调试按钮配置
   */
  const getVersionExtraActions = () => {
    // 当前版本状态对应的按钮列表
    const list = VERSION_ACTIONS[currentVersion?.status] || [];
    // 详情固定跟随版本下拉展示；更多配置、调试沿用原状态配置
    return [
      { label: '详情', action: 'detail', type: 'default' },
      ...list.filter(btn => ['moreConfig', 'debug'].includes(btn.action)),
    ];
  };

  /**
   * 根据当前版本状态与编辑态计算实际渲染的右侧按钮列表，并按 VERSION_BUTTON_ORDER 重排
   * 草稿 / 已撤回 / 已驳回 三种状态均支持「编辑/取消编辑/保存」互斥替换；其余状态走原列表。
   * @returns {Array} 按钮配置列表（已重排）
   */
  const getRenderActions = () => {
    // 当前版本状态对应的原始按钮列表，过滤左侧已承载的更多配置 / 调试
    const list = (VERSION_ACTIONS[currentVersion?.status] || [])
      .filter(btn => !['moreConfig', 'debug'].includes(btn.action));
    // 支持编辑态的版本状态集合（草稿 / 已撤回 / 已驳回）
    const editableStatuses = [
      VERSION_STATUS.DRAFT,
      VERSION_STATUS.WITHDRAWN,
      VERSION_STATUS.REJECTED,
    ];
    // 是否允许「编辑 / 发布 / 保存」三者互斥
    const supportEdit = editableStatuses.includes(currentVersion?.status);
    // 支持编辑的状态下：非编辑态把 save 替换为 edit；编辑态隐藏 publish，仅保留 cancelEdit 与 save
    const mapped = supportEdit
      ? list.reduce((actions, btn) => {
        if (btn.action === 'save' && !isEditing) {
          return [...actions, { label: '编辑', action: 'edit', type: 'primary' }];
        }
        if (btn.action === 'save' && isEditing) {
          return [
            ...actions,
            { label: '取消编辑', action: 'cancelEdit', type: 'default' },
            btn,
          ];
        }
        if (btn.action === 'publish' && isEditing) {
          return actions;
        }
        return [...actions, btn];
      }, [])
      : list;
    // 按 VERSION_BUTTON_ORDER 稳定重排：未在顺序表中的按钮排到末尾，并保持其原相对顺序
    const orderIndex = (action) => {
      const idx = VERSION_BUTTON_ORDER.indexOf(action);
      return idx === -1 ? VERSION_BUTTON_ORDER.length : idx;
    };
    return [...mapped].sort((a, b) => orderIndex(a.action) - orderIndex(b.action));
  };

  return (
    <div className="version-bar">
      <div className="version-bar-left">
        <span className="version-bar-label">当前版本</span>
        {!hasVersions ? (
          <span className="version-empty">暂无版本，点击"创建草稿"开始配置</span>
        ) : (
          <>
            <Select
              className="version-select"
              value={currentVersion?.versionId}
              onChange={onVersionChange}
              disabled={actionLoading}
            >
              {versionList.map(v => (
                // label 仅展示版本名，下拉项使用 .version-option 容器渲染：版本名 + 创建时间 + 状态 Tag
                <Option key={v.versionId} value={v.versionId} label={v.name}>
                  <div className="version-option">
                    <span className="version-option-name">{v.name}</span>
                    <span className="version-option-time">{v.createTime}</span>
                    <Tag color={VERSION_STATUS_MAP[v.status]?.color}>
                      {VERSION_STATUS_MAP[v.status]?.text}
                    </Tag>
                  </div>
                </Option>
              ))}
            </Select>
            <div className="version-bar-extra-actions">
              {/* 详情 / 更多配置 / 调试 跟随版本下拉作为左侧整体展示 */}
              {getVersionExtraActions().map((btn) => (
                <Button
                  key={btn.action}
                  type={btn.type}
                  onClick={() => onAction(btn.action)}
                >
                  {btn.label}
                </Button>
              ))}
            </div>
          </>
        )}
      </div>

      {/* 无版本时不再渲染右侧操作区，添加版本入口统一收敛到 index.jsx 主体空状态区域 */}
      {hasVersions && getRenderActions().length > 0 && (
        <div className="version-bar-actions">
          {/* 右侧仅展示版本变更类操作，详情 / 更多配置 / 调试 已移动到版本下拉右侧 */}
          {getRenderActions().map((btn) => (
            <Button
              key={btn.action}
              type={btn.type}
              danger={btn.danger}
              className={btn.danger ? 'danger-btn' : btn.type === 'primary' ? 'primary-btn' : undefined}
              loading={actionLoading && (btn.action === 'save' || btn.action === 'publish' || btn.action === 'newDraft')}
              disabled={btn.action === 'cancelEdit' && actionLoading}
              onClick={() => (btn.action === 'cancelEdit' ? onCancelEdit() : onAction(btn.action))}
            >
              {btn.label}
            </Button>
          ))}
        </div>
      )}
    </div>
  );
};

export default VersionBar;