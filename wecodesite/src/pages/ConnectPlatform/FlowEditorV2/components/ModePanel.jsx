import React from 'react';
import { FLOW_MODE_META } from '../constants';

/**
 * 编排模式选择面板
 *
 * @param {Object} props
 * @param {string} props.flowMode 当前编排模式
 * @param {boolean} props.editable 是否可编辑
 * @param {Object} props.appModeVisibility 应用级编排模式可见性配置
 * @param {Function} props.onSelect 选择模式回调 (mode) => void
 */
const ModePanel = (props) => {
  const { flowMode, editable, appModeVisibility, onSelect } = props;

  /**
   * 获取可见的编排模式列表
   * @returns {Array<{key, title, desc, icon}>} 可见模式列表
   */
  const getVisibleModes = () => {
    return Object.keys(FLOW_MODE_META)
      .filter(key => appModeVisibility?.[key])
      .map(key => ({ key, ...FLOW_MODE_META[key] }));
  };

  /**
   * 处理模式选择
   * @param {string} mode 选择的模式
   */
  const handleSelect = (mode) => {
    if (!editable) return;
    if (mode === flowMode) return;
    onSelect(mode);
  };

  const visibleModes = getVisibleModes();

  return (
    <div className="flow-mode-section">
      <div className="mode-title">选择编排类型</div>

      {visibleModes.length === 0 ? (
        <div style={{ padding: 20, textAlign: 'center', color: '#86909c' }}>
          当前应用未启用任何编排类型，请联系管理员调整接口配置。
        </div>
      ) : (
        <div className="mode-grid">
          {visibleModes.map(mode => (
            <div
              key={mode.key}
              className={`mode-card ${flowMode === mode.key ? 'active' : ''}`}
              onClick={() => handleSelect(mode.key)}
            >
              <div className="mode-card-title">
                <span className="mode-badge">{mode.icon}</span>
                {mode.title}
              </div>
              <div className="mode-card-desc">{mode.desc}</div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default ModePanel;
