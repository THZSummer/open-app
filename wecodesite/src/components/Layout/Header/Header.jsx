import React from 'react';
import { Link } from 'react-router-dom';
import { Layout, Typography, Space } from 'antd';

const { Header: AntHeader } = Layout;
const { Text } = Typography;

function Header() {
  return (
    <AntHeader style={{ 
      background: '#fff', 
      padding: '0 24px',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between',
      borderBottom: '1px solid #f0f0f0'
    }}>
      <Space size={40}>
        <Link to="/" style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <div style={{
            width: 32,
            height: 32,
            borderRadius: 6,
            background: '#0066ff',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center'
          }}>
            <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
              <path d="M4 8L8 4L12 8L8 12L4 8Z" fill="white"/>
            </svg>
          </div>
          <Text strong style={{ fontSize: 16, color: '#1f1f1f' }}>开放平台</Text>
        </Link>
        <a 
          href="https://open.feishu.cn/"
          target="_blank"
          rel="noopener noreferrer"
          style={{ color: '#5e5e5e' }}
        >
          开发文档
        </a>
      </Space>
      <Text type="secondary">开发者</Text>
    </AntHeader>
  );
}

export default Header;