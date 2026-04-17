import React, { useState, useEffect } from 'react';
import { Button } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import AppCard from '../../components/AppCard/AppCard';
import CreateAppModal from '../../components/CreateAppModal/CreateAppModal';
import { fetchAppList, fetchDefaultIcons, fetchEamapOptions, createApp } from './thunk';
import type { App } from '../../types';
import styles from './AppList.module.less';

const AppList: React.FC = () => {
  const [apps, setApps] = useState<App[]>([]);
  const [, setLoading] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);
  const [defaultIcons, setDefaultIcons] = useState<string[]>([]);
  const [eamapOptions, setEamapOptions] = useState<{ value: string; label: string }[]>([]);

  useEffect(() => {
    loadApps();
    loadIcons();
    loadEamapOptions();
  }, []);

  const loadApps = async () => {
    setLoading(true);
    const data = await fetchAppList();
    setApps(data);
    setLoading(false);
  };

  const loadIcons = async () => {
    const icons = await fetchDefaultIcons();
    setDefaultIcons(icons);
  };

  const loadEamapOptions = async () => {
    const options = await fetchEamapOptions();
    setEamapOptions(options);
  };

  const handleCreateApp = async (values: any) => {
    const newApp = await createApp(values);
    setApps([...apps, newApp]);
    setModalVisible(false);
  };

  return (
    <div className={styles.container}>
      <div className={styles.header}>
        <h1 className={styles.title}>我的应用</h1>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => setModalVisible(true)}>创建应用</Button>
      </div>
      <div className={styles.grid}>
        {apps.map((app) => (<AppCard key={app.id} app={app} />))}
      </div>
      <CreateAppModal visible={modalVisible} onCancel={() => setModalVisible(false)} onSubmit={handleCreateApp} defaultIcons={defaultIcons} eamapOptions={eamapOptions} />
    </div>
  );
};

export default AppList;
