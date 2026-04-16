import React, { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Card, Typography, Space } from 'antd';
import { HomeOutlined } from '@ant-design/icons';
import { fetchEamapOptions, fetchAppById, bindEamap } from '../../../pages/AppList/thunk';
import BindEamapModal from '../../BindEamapModal/BindEamapModal';

const { Text } = Typography;

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
        <Space size={12}>
          <HomeOutlined
            style={{ fontSize: 16, cursor: 'pointer' }}
            onClick={() => navigate('/')}
          />
          <div style={{ height: 16, width: 1, background: '#d9d9d9' }} />
          <Space direction="vertical" size={2}>
            <Text strong style={{ fontSize: 14 }}>{appData.name}</Text>
            {appData.eamap ? (
              <Text type="secondary" style={{ fontSize: 12 }}>已绑定：{appData.eamap}</Text>
            ) : (
              <Text type="secondary" style={{ fontSize: 12 }}>
                未绑定应用服务：<Text type="warning" style={{ cursor: 'pointer' }} onClick={() => setBindModalVisible(true)}>立即绑定</Text>
              </Text>
            )}
          </Space>
        </Space>
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