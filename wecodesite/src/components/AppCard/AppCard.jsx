import React from 'react';
import { useNavigate } from 'react-router-dom';
import { Typography, Tag } from 'antd';

const { Text } = Typography;

function AppCard({ app }) {
  const navigate = useNavigate();

  return (
    <div 
      onClick={() => navigate(`/basic-info?appId=${app.id}`)}
      style={{ 
        height: '100%', 
        borderRadius: 8,
        border: '1px solid #f0f0f0',
        cursor: 'pointer',
        background: '#fff',
        padding: 24
      }}
    >
      <div style={{ display: 'flex', alignItems: 'center' }}>
        <div style={{ width: 54, height: 54, borderRadius: 14, backgroundColor: '#f5f5f5', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 28, marginRight: 16 }}>
          {app.icon}
        </div>
        <div>
          <div style={{ marginBottom: 4 }}>
            <Text strong style={{ fontSize: 16 }}>{app.name}</Text>
          </div>
          <div>
            {app.eamap ? (
              <Tag color="green">已绑定：{app.eamap}</Tag>
            ) : (
              <Tag color="warning">未绑定EAMAP</Tag>
            )}
          </div>
        </div>
      </div>
      
      <div style={{ lineHeight: 2, marginTop: 16, display: 'flex', flexDirection: 'column', gap: 16 }}>
        <div>
          <Text type="secondary" style={{ width: 70, display: 'inline-block' }}>所有者：</Text><Text style={{ color: '#1677ff' }}>{app.owner.split('@')[0]}</Text>
        </div>
        <div>
          <Text type="secondary" style={{ width: 70, display: 'inline-block' }}>我的角色：</Text><Text>{app.role}</Text>
        </div>
        <div>
          <Text type="secondary" style={{ width: 70, display: 'inline-block' }}>最新动态：</Text><Text>{app.updateTime}</Text>
        </div>
      </div>
    </div>
  );
}

export default AppCard;