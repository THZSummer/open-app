import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useSelector } from 'react-redux';

export function useRoleGuard(appId) {
  const navigate = useNavigate();
  const { roleType, roleTypeLoading } = useSelector(state => state.role);

  useEffect(() => {
    if (!appId) {
      navigate('/');
      return;
    }
    if (roleTypeLoading) return;
    if (roleType == null) {
      navigate('/');
    }
  }, [appId, roleType, roleTypeLoading, navigate]);

  return { roleType, loading: roleTypeLoading };
}
