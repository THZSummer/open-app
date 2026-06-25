import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useSelector } from 'react-redux';

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
