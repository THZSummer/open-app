import React from 'react';
import { Link } from 'react-router-dom';
import './Header.m.less';

const Header = () => {
  return (
    <div className="header">
      <div className="logo">
        <Link to="/" className="logoLink">
          <div className="logoIcon"></div>
          <span className="logoText">开放平台</span>
        </Link>
      </div>
      <div className="nav">
        <a href="https://open.feishu.cn/" target="_blank" rel="noopener noreferrer" className="navLink">
          开发文档
        </a>
        <span className="navUser">开发者</span>
      </div>
    </div>
  );
};

export default Header;