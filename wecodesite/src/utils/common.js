import { getUserIdCookie } from './cookie';
import { ACTION_CONFIG } from './constants';

export const queryParams = param => {
  const reg = new RegExp("(^|&)" + param + "=([^&]*)(&|$)");
  const r =
    window.location.search.substr(1).match(reg) ||
    window.location.hash
      .substring(window.location.hash.search(/\?/) + 1)
      .match(reg);
  if (r !== null) {
    return decodeURIComponent(r[2]);
  }
  return '';
}

export const openUrl = url => {
  if (!url) return;
  window.open(url, '_blank', 'noopener,noreferrer');
};

export const ADMIN_WHITELIST = [
  'admin001',
  'admin002',
  'perm_admin',
];

export const isInAdminWhitelist = () => {
  const currentUserId = getUserIdCookie();
  return ADMIN_WHITELIST.includes(currentUserId);
};

export const convertToTreeData = (categoryList) => {
  if (!Array.isArray(categoryList)) return [];
  return categoryList.map(cat => ({
    value: cat.id,
    title: cat.nameCn,
    key: cat.id,
    children: convertToTreeData(cat.children)
  }));
};

export const getSecondModalInfo = (params) => {
  /**
   * 二次确认弹窗配置参数
   */
  const {
    action,
    getConfirmText,
    impactText,
    objectName,
  } = params;
  return {
    ...ACTION_CONFIG[action],
    content: {
      confirmText: getConfirmText({ objectName }),
      impactText,
    },
  }
}