import { getUserIdCookie } from './cookie';

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
}

export const ADMIN_WHITELIST = [
  'admin001',
  'admin002',
  'perm_admin',
];

export const isInAdminWhitelist = () => {
  const currentUserId = getUserIdCookie();
  return ADMIN_WHITELIST.includes(currentUserId);
};