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

export const getSecondModalInfo = (type, action, isAdminWeb) => {
  const actionStr = action === 'delete' ? '删除' : '撤回';
  let modalTitle = `确认${actionStr}${type}`;
  let modalContent = `${actionStr}后将无法恢复，确认要${actionStr}这个${type}`;
  let finnalStr = '';
  if (action === 'delete') {
    finnalStr = isAdminWeb ? '吗？' : '订阅吗？'
  } else {
    finnalStr = '申请吗？';
  }
  modalTitle += finnalStr;
  modalContent += finnalStr;
  return {
    ...ACTION_CONFIG[action],
    title: modalTitle,
    content: modalContent,
  }
}