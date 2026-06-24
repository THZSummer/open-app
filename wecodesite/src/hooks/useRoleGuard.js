import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useSelector } from 'react-redux';

/**
 * 页面级权限守卫 Hook
 * 从 Redux store 读取 role，如果当前用户不是应用成员则跳转到应用列表
 *
 * @param {string} appId - 应用 ID
 * @returns {{ role: number|null, loading: boolean }}
 */
export function useRoleGuard(appId) {
  const navigate = useNavigate();
  const { role, roleLoading } = useSelector(state => state.role);

  useEffect(() => {
    if (!appId) {
      navigate('/');
      return;
    }
    if (roleLoading) return;
    if (role == null) {
      navigate('/');
    }
  }, [appId, role, roleLoading, navigate]);

  return { role, loading: roleLoading };
}
