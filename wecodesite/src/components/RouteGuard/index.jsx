import React, { useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { message } from 'antd';
import { useDispatch, useSelector } from 'react-redux';
import { fetchAppDetail } from '../../thunks/appThunks';

/**
 * 路由守卫 Hook
 * 用于应用详情页、成员管理、能力管理、版本发布等需要 appId 的页面
 * 对应 TASK-011
 */
export const useAppRouteGuard = () => {
  const { appId } = useParams();
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const appDetail = useSelector((state) => state.app?.appDetail);
  const loading = useSelector((state) => state.app?.loading);

  useEffect(() => {
    if (!appId) {
      message.error('缺少应用ID参数');
      navigate('/app/list');
      return;
    }

    const numericAppId = Number(appId);
    if (isNaN(numericAppId) || numericAppId <= 0) {
      message.error('应用ID格式不正确');
      navigate('/app/list');
      return;
    }

    // 如果 Redux 中没有当前应用详情，则加载
    if (!appDetail || appDetail.appId !== numericAppId) {
      dispatch(fetchAppDetail({ appId: numericAppId }));
    }
  }, [appId, navigate, dispatch, appDetail]);

  return { appId: appId ? Number(appId) : null, appDetail, loading };
};

/**
 * 路由守卫组件（可选包装器）
 */
const RouteGuard = ({ children }) => {
  useAppRouteGuard();
  return children;
};

export default RouteGuard;
