import React, { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Card } from 'antd';
import { HomeOutlined } from '@ant-design/icons';
import { fetchEamapOptions, fetchAppById, bindEamap } from '../../../pages/AppList/thunk';
import BindEamapModal from '../../BindEamapModal/BindEamapModal';

function AppInfoBar() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const appId = searchParams.get('appId') || '1';
  const [appData, setAppData] = useState({ name: '未知应用', icon: '📦', status: '未知', eamap: null });
  const [eamapOptions, setEamapOptions] = useState([]);
  const [bindModalVisible, setBindModalVisible] = useState(false);

  useEffect(() => {
    loadAppData();
  }, [appId]);

  useEffect(() => {
    loadEamapOptions();
  }, []);

  const loadAppData = async () => {
    try {
      const app = await fetchAppById(appId);
      if (app) {
        setAppData(app);
      }
    } catch (error) {
      console.error('Failed to load app data:', error);
    }
  };

  const loadEamapOptions = async () => {
    try {
      const options = await fetchEamapOptions();
      setEamapOptions(options);
    } catch (error) {
      console.error('Failed to load EAMAP options:', error);
    }
  };

  const handleBindEamap = async (eamap) => {
    try {
      await bindEamap(appId, eamap);
      setAppData((prev) => ({ ...prev, eamap }));
      setBindModalVisible(false);
    } catch (error) {
      console.error('Failed to bind EAMAP:', error);
    }
  };

  return (
    <>
      <Card size="small" style={{ marginBottom: 1, borderRadius: 0 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <HomeOutlined
            style={{ fontSize: 16, cursor: 'pointer' }}
            onClick={() => navigate('/')}
          />
          <div style={{ height: 16, width: 1, background: '#d9d9d9' }} />
          <div style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
            <strong style={{ fontSize: 14 }}>{appData.name}</strong>
            {appData.eamap ? (
              <span style={{ fontSize: 12, color: '#8c8c8c' }}>已绑定：{appData.eamap}</span>
            ) : (
              <span style={{ fontSize: 12, color: '#8c8c8c' }}>
                未绑定应用服务：<span style={{ cursor: 'pointer', color: '#faad14' }} onClick={() => setBindModalVisible(true)}>立即绑定</span>
              </span>
            )}
          </div>
        </div>
      </Card>
      <BindEamapModal
        visible={bindModalVisible}
        onCancel={() => setBindModalVisible(false)}
        onOk={handleBindEamap}
        appId={appId}
        eamapOptions={eamapOptions}
        currentEamap={appData.eamap}
      />
    </>
  );
}

export default AppInfoBar;