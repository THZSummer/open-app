import React, { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useSelector, useDispatch } from 'react-redux';
import { fetchAppDetail, clearAppDetail } from '../../../store/appSlice';
import { fetchRole, clearRole } from '../../../store/roleSlice';
import { message } from 'antd';
import { fetchEamapOptions } from '../../../pages/AppList/thunk';
import { bindEamap } from '../../../pages/BasicInfo/thunk';
import BindEamapModal from '../../BindEamapModal/BindEamapModal';

import './AppInfoBar.m.less';

function AppInfoBar() {
  const appDetail = useSelector(state => state.app.appDetail);
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const appId = searchParams.get('appId') || '';

  useEffect(() => {
    if (appId) {
      dispatch(fetchAppDetail(appId));
      dispatch(fetchRole(appId));
    }
    return () => {
      dispatch(clearAppDetail());
      dispatch(clearRole());
    };
  }, [appId, dispatch]);
  const [eamapOptions, setEamapOptions] = useState([]);
  const [bindModalVisible, setBindModalVisible] = useState(false);

  const loadEamapOptions = async () => {
    try {
      const result = await fetchEamapOptions({ curPage: 1, pageSize: 100 });
      if (result?.code === '200') {
        setEamapOptions(result.data || []);
      }
    } catch (error) {
      // ignore
    }
  };

  const handleBindEamapOpen = () => {
    loadEamapOptions();
    setBindModalVisible(true);
  };

  const handleBindEamapSubmit = async (eamapAppCode) => {
    try {
      const result = await bindEamap(appId, { eamapAppCode });
      if (result?.code === '200') {
        message.success('绑定成功，应用已升级为业务应用');
        setBindModalVisible(false);
        // 刷新页面让 Layout 重新加载 appDetail
        window.location.reload();
      } else {
        message.error(result?.messageZh || result?.messageEn || '绑定失败');
      }
    } catch (error) {
      message.error('绑定失败，请稍后重试');
    }
  };

  // 判断应用类型
  const isPersonalApp = appDetail?.appType === 0;
  const isLegacyPersonal = isPersonalApp && appDetail?.appSubType === 0;
  const hasEamap = !!appDetail?.eamapAppCode;

  return (
    <>
      <div className="app-info-bar">
        <div className="app-info-bar-inner">
          <button className="home-btn" onClick={() => navigate('/')} title="返回应用列表">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" />
              <polyline points="9 22 9 12 15 12 15 22" />
            </svg>
          </button>
          <div className="header-divider"></div>
          <div className="header-app-info">
            <span className="header-app-name">
              {appDetail?.nameCn || appDetail?.nameEn || '加载中...'}
            </span>
            {/* 业务应用：显示已绑定信息 */}
            {!isPersonalApp && hasEamap && (
              <span className="header-eamap"><span className="header-eamap-label">已绑定:</span> {appDetail.eamapAppName || ''} {appDetail.eamapAppCode}</span>
            )}
            {/* 存量个人应用：显示 未绑定应用服务: 立即绑定 */}
            {isLegacyPersonal && (
              <span className="header-eamap-unbound">
                未绑定应用服务: <span className="header-eamap-bind" onClick={handleBindEamapOpen}>立即绑定</span>
              </span>
            )}
            {/* 普通个人应用（appType=0, appSubType≠0）：不显示任何额外内容 */}
          </div>
        </div>
      </div>
      <BindEamapModal
        visible={bindModalVisible}
        onCancel={() => setBindModalVisible(false)}
        onOk={handleBindEamapSubmit}
        appId={appId}
        eamapOptions={eamapOptions}
      />
    </>
  );
}

export default AppInfoBar;
