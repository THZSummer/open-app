import React, { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Card, Button, message, Spin } from 'antd';
import { EyeInvisibleOutlined, EyeOutlined, CopyOutlined } from '@ant-design/icons';
import { useSelector } from 'react-redux';
import { copyToClipboard } from '../../utils/common';
import { fetchAppIdentity } from './thunk';
import './AppCredentials.m.less';

function AppCredentials() {
  const [searchParams] = useSearchParams();
  const appId = searchParams.get('appId');
  const { appBaseInfo } = useSelector(state => state.app);
  const [showSecret, setShowSecret] = useState(false);
  const [identity, setIdentity] = useState(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!appId) return;
    setLoading(true);
    fetchAppIdentity(appId)
      .then((result) => {
        if (result?.code === '200') {
          setIdentity(result.data);
        } else {
          message.error(result?.messageZh || '获取应用凭证失败');
        }
      })
      .finally(() => setLoading(false));
  }, [appId]);

  const handleCopy = async (text) => {
    const success = await copyToClipboard(text);
    if (success) {
      message.success('已复制');
    } else {
      message.error('复制失败');
    }
  };

  return (
    <Card title="应用凭证" className="info-card">
      <div className="credential-item">
        <span className="credential-label">APP ID</span>
        <span className="credential-value">{appBaseInfo.appId}</span>
      </div>
      <Spin spinning={loading}>
        {identity && (
          <>
            <div className="credential-item">
              <span className="credential-label">APP Key</span>
              <span className="credential-value">{identity.ak}</span>
            </div>
            <div className="credential-item">
              <span className="credential-label">APP Secret</span>
              <span className="credential-value">
                <span className="credential-text">
                  {showSecret ? identity.sk : '********'}
                </span>
                <span className="credential-actions">
                  <Button
                    type="link"
                    size="small"
                    icon={<CopyOutlined />}
                    onClick={() => handleCopy(identity.sk)}
                  />
                  <Button
                    type="link"
                    size="small"
                    icon={showSecret ? <EyeInvisibleOutlined /> : <EyeOutlined />}
                    onClick={() => setShowSecret(prev => !prev)}
                  />
                </span>
              </span>
            </div>
          </>
        )}
      </Spin>
    </Card>
  );
}

export default AppCredentials;
