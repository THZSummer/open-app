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

export const UPLOAD_IMAGE_TYPE = {
  icon: 1, // 图标
  diagram: 2, // 功能示意图
}

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

/**
 * 由后端版本数据构造前端 UI 版本摘要
 * 前端 UI 使用 name 字段展示版本名称，按 `v{versionNumber}` 拼接
 * 兼容连接器（ConnectorEditor）和连接流（FlowEditorV2）两侧版本下拉
 *
 * @param {Object} ver - 后端版本数据
 * 包含以下字段：
 * - versionId: 版本 ID
 * - status: 版本状态
 * - createTime: 创建时间
 * - versionNumber: 版本号（用于拼接 name）
 * - publishedTime: 发布时间（可选）
 * - publishedBy: 发布人（可选）
 *
 * @returns {Object} 含 versionId / status / createTime / name / versionNumber / publishedTime / publishedBy 的摘要
 */
export const buildVersionSummary = (ver) => ({
  versionId: ver.versionId,
  status: ver.status,
  createTime: ver.createTime,
  name: ver.versionNumber != null ? `v${ver.versionNumber}` : ver.versionId,
  versionNumber: ver.versionNumber,
  publishedTime: ver.publishedTime,
  publishedBy: ver.publishedBy,
});

/**
 * 归一化后端 JSON 配置字段
 * @param {string|Object} rawConfig 后端返回的原始配置
 * @returns {Object|null} 解析后的配置对象
 */
export const normalizeJsonConfig = (rawConfig) => {
  // 空值交给调用方走默认配置分支
  if (!rawConfig) return null;

  // 已经是对象时直接返回，避免重复序列化
  if (typeof rawConfig === 'object') return rawConfig;

  // 字符串配置需要兼容 JSON parse 失败场景
  if (typeof rawConfig === 'string') {
    try {
      return JSON.parse(rawConfig);
    } catch {
      return null;
    }
  }

  return null;
};

/**
 * 构建版本摘要列表
 * @param {Array} versions 后端版本列表
 */
export const buildSortedVersionSummaries = (versions = []) => {
  return (versions || [])
    .map(buildVersionSummary)
};

/**
 * 拼接版本对象展示名
 * @param {Object} version 版本对象
 * @returns {string} 版本展示名
 */
export const getVersionObjectName = (version) => {
  // 缺少版本对象时返回空字符串，避免弹窗文案出现 undefined
  if (!version) return '';

  // 兼容后端与前端摘要里的不同版本号字段命名
  const versionNo = version.versionNo || version.versionNumber;
  // 兼容后端与前端摘要里的不同版本名字段命名
  const versionName = version.versionName || version.name;

  if (versionNo && versionName) return `v${versionNo} (${versionName})`;
  if (versionNo) return `v${versionNo}`;
  if (versionName) return versionName;
  return version.versionId || '';
};

/**
 * 从审批链接中提取 eflowId
 * @param {string} url - 审批地址URL
 * @returns {string} eflowId 值或空字符串
 */
export const extractEflowId = (url) => {
  if (!url) return '';
  
  const match = url.match(/eflowId=([^&]+)/);
  return match ? match[1] : '';
};
