import React, { useEffect } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { Spin } from 'antd';
import { useSelector } from 'react-redux';
import AuthMethodCard from '../../components/AuthMethodCard/AuthMethodCard';
import AppCredentials from '../../components/AppCredentials/AppCredentials';
import BasicInfoCard from '../../components/BasicInfoCard/BasicInfoCard';
import CardSettings from '../../components/CardSettings/CardSettings';

import './BasicInfo.m.less';

function BasicInfo() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { appBaseInfo } = useSelector(state => state.app);
  const appId = searchParams.get('appId');

  useEffect(() => {
    if (!appId) {
      navigate('/');
    }
  }, [appId, navigate]);

  if (!appBaseInfo) {
    return <Spin spinning={true} className="basic-info-loading" />;
  }

  return (
    <div className="basic-info-page">
      <AppCredentials />
      <BasicInfoCard />
      {appBaseInfo?.appType !== 0 && <><AuthMethodCard /><CardSettings /></>}
    </div>
  );
}

export default BasicInfo;
