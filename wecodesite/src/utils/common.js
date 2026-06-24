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

// ==================== 应用管理公共工具函数 (APP-MGMT-001) ====================
/**
 * 校验文件类型和大小
 * @param {File} file - 文件对象
 * @param {Object} config - 校验配置 { types, maxSize, typeMessage, sizeMessage }
 * @returns {Object} { valid: boolean, message: string }
 */
export const validateFile = (file, config) => {
  if (!config.types.includes(file.type)) {
    return { valid: false, message: config.typeMessage };
  }
  if (file.size > config.maxSize) {
    return { valid: false, message: config.sizeMessage };
  }
  return { valid: true, message: '' };
};

/**
 * 校验图片尺寸
 * @param {File} file - 文件对象
 * @param {number} expectWidth - 期望宽度
 * @param {number} expectHeight - 期望高度
 * @returns {Promise<Object>} { valid: boolean, message: string }
 */
export const validateImageDimensions = (file, expectWidth, expectHeight) => {
  return new Promise((resolve) => {
    const img = new Image();
    img.onload = () => {
      if (img.width !== expectWidth || img.height !== expectHeight) {
        resolve({
          valid: false,
          message: `图片尺寸必须为${expectWidth}x${expectHeight}px`,
        });
      } else {
        resolve({ valid: true, message: '' });
      }
      URL.revokeObjectURL(img.src);
    };
    img.onerror = () => {
      resolve({ valid: false, message: '图片加载失败' });
      URL.revokeObjectURL(img.src);
    };
    img.src = URL.createObjectURL(file);
  });
};

/**
 * 复制文本到剪贴板
 * @param {string} text - 要复制的文本
 */
export const copyToClipboard = async (text) => {
  try {
    await navigator.clipboard.writeText(text);
    return true;
  } catch (err) {
    // 降级方案
    const textarea = document.createElement('textarea');
    textarea.value = text;
    textarea.style.position = 'fixed';
    textarea.style.opacity = '0';
    document.body.appendChild(textarea);
    textarea.select();
    const success = document.execCommand('copy');
    document.body.removeChild(textarea);
    return success;
  }
};

/**
 * 防抖函数
 * @param {Function} fn - 原函数
 * @param {number} delay - 延迟毫秒
 * @returns {Function}
 */
export const debounce = (fn, delay = 300) => {
  let timer = null;
  return (...args) => {
    if (timer) clearTimeout(timer);
    timer = setTimeout(() => fn(...args), delay);
  };
};

/**
 * 构建带查询参数的 URL
 * @param {string} path - 路径
 * @param {Object} params - 查询参数
 * @returns {string}
 */
export const buildUrlWithParams = (path, params = {}) => {
  const query = Object.entries(params)
    .filter(([, v]) => v !== undefined && v !== null)
    .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(v)}`)
    .join('&');
  return query ? `${path}?${query}` : path;
};

/**
 * 获取当前用户ID（灰度发布使用）
 * - 当前实现：从 cookie `user_id` 读取
 * - 后续如需切换到其他方式（SSO token、localStorage、用户中心接口等），只需修改此方法
 *
 * @returns {string} 当前用户ID，未登录返回空字符串
 */
export const getCurrentUserId = () => {
  return getUserIdCookie() || '';
};
