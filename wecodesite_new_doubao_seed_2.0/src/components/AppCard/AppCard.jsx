import React from 'react';
import { Link } from 'react-router-dom';
import './AppCard.m.less';

const AppCard = ({ app }) => {
  return (
    <Link to={`/basic-info?appId=${app.id}`} className="appCard">
      <div className="cardHeader">
        <div className="appIcon"></div>
        <div className="appInfo">
          <div className="appName">{app.name}</div>
          <div className="appStatus">{app.status}</div>
        </div>
      </div>
      <div className="cardBody">
        <div className="infoItem">
          <span className="label">所有者：</span>
          <span className="value">{app.owner}</span>
        </div>
        <div className="infoItem">
          <span className="label">我的角色：</span>
          <span className="value">{app.role}</span>
        </div>
        <div className="infoItem">
          <span className="label">最新动态：</span>
          <span className="value">{app.lastActivity}</span>
        </div>
      </div>
    </Link>
  );
};

export default AppCard;