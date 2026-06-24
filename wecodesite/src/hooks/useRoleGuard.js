import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { fetchApi, API_CONFIG } from '../configs/web.config';

/**
 * 页面级权限守卫 Hook
 * 调用 current-role 接口，如果当前用户不是应用成员则跳转到应用列表
 *
 * @param {string} appId - 应用 ID
 * @returns {{ role: number|null, loading: boolean }}
 */
export function useRoleGuard(appId) {
  const navigate = useNavigate();
  const [role, setRole] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!appId) {
      navigate('/');
      return;
    }

    const checkAccess = async () => {
      try {
        const result = await fetchApi(API_CONFIG.APP.CURRENT_ROLE, { params: { appId } });
        if (result?.code !== '200' || result.data?.role == null) {
          navigate('/');
          return;
        }
        setRole(result.data.role);
      } catch {
        navigate('/');
      } finally {
        setLoading(false);
      }
    };
    checkAccess();
  }, [appId]);

  return { role, loading };
}
