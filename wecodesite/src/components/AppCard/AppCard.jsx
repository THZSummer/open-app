import React from 'react';
import { useNavigate } from 'react-router-dom';
import { ROLE_MAP } from '../../utils/constants';

import './AppCard.m.less';

/**
 * 应用卡片组件
 *
 * 功能：
 * - 展示应用图标、名称、EAMAP 绑定状态
 * - 显示所有者、角色、最新动态信息
 * - 点击跳转应用详情
 *
 * @param {Object} props - 组件属性
 * @param {Object} props.app - 应用数据对象
 * @param {Function} props.onClick - 点击回调
 */
function AppCard(props) {
  const { app, onClick } = props;
  const navigate = useNavigate();

  const roleInfo = ROLE_MAP[app.currentUserRole] || ROLE_MAP[0];
  const roleText = roleInfo.text;

  const ownerName = app.owner?.memberNameCn || 'Unknown';
  const ownerAccount = app.owner?.w3Account || '';

  // 格式化时间：ISO → YYYY-MM-DD HH:mm:ss
  const formatTime = (isoStr) => {
    if (!isoStr) return '-';
    const d = new Date(isoStr);
    if (isNaN(d.getTime())) return isoStr;
    const pad = (n) => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
  };

  // 图标：优先用 icon.url（后端已处理预设/上传图标），否则取首字母
  const iconDisplay = app.icon?.url
    ? <img src={app.icon.url} alt="" className="app-card-icon-img" />
    : <span className="app-card-icon-fallback">{(app.nameCn || app.nameEn || '?')[0]}</span>;

  const handleClick = () => {
    if (onClick) {
      onClick();
    } else {
      navigate(`/basic-info?appId=${app.appId}`);
    }
  };

  return (
    <div className="app-card" onClick={handleClick} data-testid={`app-card-${app.appId}`}>
      <div className="app-card-top">
        <div className="app-card-icon">
          {iconDisplay}
        </div>
        <div className="app-card-info">
          <div className="app-card-name">{app.nameCn || app.nameEn}</div>
          <div className="app-card-eamap">
            {app.eamapBound ? (
              <span className="eamap-status binded">
                已绑定EAMAP{app.eamapAppCode ? `-${app.eamapAppCode}` : ''}
              </span>
            ) : (
              <span className="eamap-status unbinded">
                未绑定EAMAP
              </span>
            )}
          </div>
        </div>
      </div>

      <div className="app-card-bottom">
        <div className="app-card-meta-item">
            <span className="app-card-meta-label">所有者：</span>
          <span className="app-card-meta-value owner">
            {ownerName}{ownerAccount ? ` ${ownerAccount}` : ''}
          </span>
        </div>
        <div className="app-card-meta-item">
            <span className="app-card-meta-label">我的角色：</span>
          <span className="app-card-meta-value role">{roleText}</span>
        </div>
        <div className="app-card-meta-item">
            <span className="app-card-meta-label">最新动态：</span>
          <span className="app-card-meta-value">{formatTime(app.lastUpdateTime)}</span>
        </div>
      </div>
    </div>
  );
}

export default AppCard;
