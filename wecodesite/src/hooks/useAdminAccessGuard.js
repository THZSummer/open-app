import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { isInAdminWhitelist } from '../utils/common';

/**
 * 管理端访问权限守卫
 */
export const useAdminAccessGuard = () => {
  const navigate = useNavigate();

  useEffect(() => {
    /**
     * 校验当前用户是否具备管理端访问权限
     */
    const checkAdminAccess = async () => {
      const hasAccess = await isInAdminWhitelist();
      if (!hasAccess) {
        navigate('/apps');
      }
    };

    checkAdminAccess();
  }, []);
};
