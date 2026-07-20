/**
 * 能力目录管理 API 接口
 *
 * 提供能力目录列表查询、创建及文件上传等接口调用。
 * 接口基础路径：/service/open/v2/ability/admin
 */
import API_CONFIG from '../../../configs/web.config';
import { fetchApi } from '../../../utils/webFetch';

/**
 * 获取能力目录列表
 *
 * @param {Object} [params={}] - 查询参数
 * @param {number} [params.curPage] - 当前页码
 * @param {number} [params.pageSize] - 每页条数
 * @param {string} [params.keyword] - 搜索关键词
 * @param {string} [params.sortField] - 排序字段
 * @param {string} [params.sortOrder] - 排序方向 asc|desc
 * @returns {Promise<Object>} 能力列表数据
 */
export const getAbilityList = async (params = {}) => {
  try {
    const result = await fetchApi(API_CONFIG.ABILITY_LIST, {
      method: 'GET',
      params,
    });
    return result;
  } catch (err) {
    return {
      code: '500',
      messageZh: '请求失败',
      messageEn: 'Request failed',
      data: [],
      page: { curPage: 1, pageSize: 20, total: 0 },
    };
  }
};

/**
 * 上传文件（使用原生 fetch，FormData 需自行处理 Content-Type）
 *
 * @param {File} file - 上传的文件
 * @param {number} bizType - 业务类型：1=图标，2=示意图
 * @returns {Promise<Object>} 上传结果 { batchId, showUrl }
 */
export const uploadFile = async (file, bizType) => {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('bizType', bizType);
  try {
    const response = await fetch(API_CONFIG.FILE_UPLOAD, {
      method: 'POST',
      credentials: 'include',
      body: formData,
    });
    return response.json();
  } catch (err) {
    return {
      code: '500',
      messageZh: '文件上传失败',
      messageEn: 'File upload failed',
    };
  }
};

/**
 * 创建能力
 *
 * @param {Object} data - 能力数据
 * @returns {Promise<Object>} 创建结果
 */
export const createAbility = async (data) => {
  try {
    const result = await fetchApi(API_CONFIG.ABILITY_CREATE, {
      method: 'POST',
      body: JSON.stringify(data),
    });
    return result;
  } catch (err) {
    return {
      code: '500',
      messageZh: '创建失败',
      messageEn: 'Create failed',
    };
  }
};
