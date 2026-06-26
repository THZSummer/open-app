/**
 * ========================================
 * 连接流 V2 公共数据转换工具
 * ========================================
 *
 * 沉淀连接器编辑器与连接流编辑器共用的 jsonObjectDef / SchemaEditorV2 转换逻辑。
 */

/**
 * 判断对象是否存在有效字段
 * @param {Object} obj 待检查对象
 * @returns {boolean} 是否存在字段
 */
export const hasObjectKeys = (obj) => {
  // 只有普通对象且至少包含一个 key 时，才认为存在有效配置
  return !!obj && typeof obj === 'object' && Object.keys(obj).length > 0;
};

/**
 * 将 jsonObjectDef 字段转换为 SchemaEditorV2 参数节点
 * @param {Object} params 参数对象
 * @returns {Object} SchemaEditorV2 参数节点
 */
export const parseJsonFieldToParam = (params) => {
  // params.name / params.field / params.carrier
  const { name, field, carrier } = params;
  const type = field?.type || 'string';
  const childJsonObject = type === 'array' ? field?.items : field;
  const children = parseJsonObjectToParams({ jsonObject: childJsonObject, carrier });

  return {
    name,
    paramName: name,
    type,
    paramType: type,
    desc: field?.description || '',
    description: field?.description || '',
    children,
    carrier,
  };
};

/**
 * 将 jsonObjectDef 转换为 SchemaEditorV2 参数数组
 * @param {Object} params 参数对象
 * @returns {Array} SchemaEditorV2 参数数组
 */
export const parseJsonObjectToParams = (params) => {
  // params.jsonObject / params.carrier
  const { jsonObject, carrier } = params || {};
  if (!jsonObject?.properties) return [];
  return Object.entries(jsonObject.properties).map(([name, field]) => parseJsonFieldToParam({
    name,
    field,
    carrier,
  }));
};

/**
 * 将 SchemaEditorV2 参数节点转换为 jsonObjectDef 字段
 * @param {Object} param 前端参数节点
 * @returns {Object} jsonObjectDef 字段
 */
export const buildJsonFieldFromParam = (param) => {
  // param.paramName / param.name / param.paramType / param.type / param.description / param.desc / param.children
  const type = param.paramType || param.type || 'string';
  const description = param.description || param.desc || '';

  if (type === 'object') {
    const childrenObject = buildJsonObjectFromParams(param.children || []);
    return {
      type: 'object',
      properties: childrenObject?.properties || {},
      ...(description ? { description } : {}),
    };
  }

  if (type === 'array') {
    return {
      type: 'array',
      items: buildJsonObjectFromParams(param.children || []) || { type: 'object', properties: {} },
      ...(description ? { description } : {}),
    };
  }

  return {
    type,
    ...(description ? { description } : {}),
  };
};

/**
 * 将 SchemaEditorV2 参数数组转换为 jsonObjectDef
 * @param {Array} params 前端参数数组
 * @returns {Object|null} jsonObjectDef 对象
 */
export const buildJsonObjectFromParams = (params) => {
  // 空数组返回 null，兼容连接器编辑器原有保存行为
  if (!params || params.length === 0) return null;

  const properties = {};
  (params || []).forEach((param) => {
    const name = param.paramName || param.name || '';
    if (!name) return;
    properties[name] = buildJsonFieldFromParam(param);
  });

  return { type: 'object', properties };
};

/**
 * 将 HTTP 分段参数转换为 carrier 到 jsonObjectDef 的映射
 * @param {Object} params 参数对象
 * @returns {Object} HTTP carrier 参数对象
 */
export const buildHttpCarrierParams = (params) => {
  // params.sourceMap / params.carriers
  const { sourceMap, carriers } = params;
  const result = {};

  (carriers || []).forEach((carrier) => {
    const jsonObject = buildJsonObjectFromParams(sourceMap?.[carrier] || []);
    if (hasObjectKeys(jsonObject?.properties)) {
      result[carrier] = jsonObject;
    }
  });

  return result;
};
