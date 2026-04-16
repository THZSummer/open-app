import React, { useState, useEffect } from 'react';
import { Typography, Button, message } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import AppCard from '../../components/appcard/AppCard';
import CreateAppModal from '../../components/CreateAppModal/CreateAppModal';
import { fetchAppList, fetchDefaultIcons, fetchEamapOptions, createApp } from './thunk';
import './AppList.m.less';

const { Title } = Typography;

function AppList() {
  const [apps, setApps] = useState([]);
  const [defaultIcons, setDefaultIcons] = useState([]);
  const [eamapOptions, setEamapOptions] = useState([]);
  const [modalVisible, setModalVisible] = useState(false);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    setLoading(true);
    try {
      const [appsData, iconsData, eamapData] = await Promise.all([
        fetchAppList(),
        fetchDefaultIcons(),
        fetchEamapOptions(),
      ]);
      setApps(appsData);
      setDefaultIcons(iconsData);
      setEamapOptions(eamapData);
    } catch (error) {
      message.error('加载数据失败');
    } finally {
      setLoading(false);
    }
  };

  const handleCreateApp = async (values) => {
    try {
      await createApp(values);
      message.success('创建应用成功');
      setModalVisible(false);
      loadData();
    } catch (error) {
      message.error('创建应用失败');
    }
  };

  return (
    <div className="app-list">
      <div className="app-list-header">
        <Title level={2} className="app-list-title">我的应用</Title>
        <Button
          type="primary"
          icon={<PlusOutlined />}
          style={{ display: 'flex', alignItems: 'center', justifyContent: 'center' }}
          onClick={() => setModalVisible(true)}
        >
          创建应用
        </Button>
      </div>
      <div className="app-list-content">
        {apps.map((app) => (
          <AppCard key={app.id} app={app} />
        ))}
      </div>
      <CreateAppModal
        visible={modalVisible}
        onCancel={() => setModalVisible(false)}
        onOk={handleCreateApp}
        defaultIcons={defaultIcons}
        eamapOptions={eamapOptions}
      />
    </div>
  );
}

export default AppList;