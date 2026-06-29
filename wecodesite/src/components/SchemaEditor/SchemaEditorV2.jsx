/**
 * ========================================
 * Schema 编辑器 V2 - 连接器接口配置专用
 * ========================================
 *
 * 功能与设计：
 * - 完全对齐 demo（connector-editor.html）的入参/出参配置交互
 * - 行内紧凑布局（参数名 / 类型 / carrier / 描述 / 添加按钮 / 删除按钮）
 * - 复杂类型（object/array）以左缩进展示子参数，不再用 Card 包裹
 * - 复杂类型行尾"添加"按钮为下拉菜单（添加子节点 / 添加兄弟节点）
 * - array 类型最多只能添加 1 个子参数，object 类型可添加多个子参数
 * - 基础类型行尾"添加"按钮直接添加兄弟节点（不能添加子节点）
 * - Tab 顶部"+ 添加参数"按钮仅当前 carrier 下无参数时显示
 * - 嵌套层级硬编码上限 10
 * - 与 V1 不兼容：仅服务于 ConnectorEditor 场景，移除 reference/upstreamParams 等 FlowEditor 专属能力
 *
 * 组件关系：
 * - SchemaEditorV2：维护完整 schemaData 和路径回写逻辑，是数据中枢
 * - SchemaParamItem：由 SchemaEditorV2 调用，负责渲染单行 + 递归子参数，
 *   通过回调将增删改事件冒泡给 SchemaEditorV2
 */

import React, { useEffect, useState } from 'react';
import { Button } from 'antd';
import SchemaParamItem from './SchemaParamItem';
import {
  isComplexType,
  DEFAULT_TYPE_OPTIONS,
  createDefaultParamV2,
} from './constants';
import './SchemaEditorV2.m.less';

/**
 * Schema 编辑器 V2 组件
 * @param {Object} props 组件属性
 * props.form Ant Design Form 实例
 * props.schemaType form.apiConfig 中的字段名（如 'requestSchema' / 'responseSchema'）
 * props.apiConfig 当未使用 form 时的兜底数据源
 * props.value 受控数据源（优先级最高）
 * props.onChange 数据变化回调
 * props.editable 是否可编辑
 * props.carrierOptions carrier 候选项
 * props.carrierFilter 当前 Tab 锁定的 carrier
 * props.hideCarrier 是否隐藏 carrier 下拉控件
 * props.typeOptions 参数类型候选项
 */
const SchemaEditorV2 = (props) => {
  const {
    form,
    schemaType,
    apiConfig = {},
    value,
    onChange,
    editable = true,
    carrierOptions = [],
    carrierFilter = null,
    hideCarrier = false,
    typeOptions = DEFAULT_TYPE_OPTIONS,
  } = props;

  // 本地维护完整的 schema 数据（含所有 carrier）
  const [schemaData, setSchemaData] = useState([]);
  // 标记是否由组件内部触发的数据变更，防止 useEffect 覆盖
  const [internalChange, setInternalChange] = useState(false);

  /**
   * 监听外部数据源变更，初始化 / 同步 schema 数据
   * 优先级：value > form.apiConfig > apiConfig prop
   */
  useEffect(() => {
    if (internalChange) {
      return;
    }

    // 优先使用 value 受控属性
    if (value !== undefined) {
      setSchemaData(Array.isArray(value) ? value : []);
      return;
    }

    // 兜底从 apiConfig prop 取
    if (apiConfig && apiConfig[schemaType]) {
      const schema = apiConfig[schemaType] || [];
      setSchemaData(Array.isArray(schema) ? schema : []);
    }
  }, [schemaType, value, form, apiConfig]);

  /**
   * 统一处理 schema 数据更新：写本地状态 + 通知父组件 + 同步 form
   * @param {Array} newSchema 更新后的完整 schema 数组
   */
  const commitSchema = (newSchema) => {
    setInternalChange(true);
    setSchemaData(newSchema);

    if (typeof onChange === 'function') {
      onChange(newSchema);
    }

    // 同步到 form.apiConfig，保证后续 getFieldValue 能拿到最新数据
    if (form && form.setFieldsValue) {
      const currentConfig = form.getFieldValue('apiConfig') || {};
      form.setFieldsValue({
        apiConfig: {
          ...currentConfig,
          [schemaType]: newSchema,
        },
      });
    }

    setInternalChange(false);
  };

  /**
   * 按路径深拷贝 schema，并定位到目标节点的父级数组
   * @param {Object} options 配置对象
   * options.path 路径数组
   */
  const cloneAndLocate = (options) => {
    // options.path: 目标路径，至少一层
    const { path } = options;
    const newSchema = JSON.parse(JSON.stringify(schemaData));
    let parentList = newSchema;

    // 沿路径深入到 path 最后一项的父级 children 数组
    for (let i = 0; i < path.length - 1; i++) {
      parentList = parentList[path[i]].children;
    }

    return { newSchema, parentList };
  };

  /**
   * 按路径更新参数字段
   * @param {Array} path 路径数组
   * @param {Object} updates 要合并的字段
   */
  const handleUpdateByPath = (path, updates) => {
    const { newSchema, parentList } = cloneAndLocate({ path });
    const lastIndex = path[path.length - 1];

    parentList[lastIndex] = { ...parentList[lastIndex], ...updates };

    // 若更新了 carrier 且当前节点是复杂类型，子树同步更新 carrier
    if ('carrier' in updates) {
      const updatedParam = parentList[lastIndex];
      if (isComplexType(updatedParam.paramType) && Array.isArray(updatedParam.children) && updatedParam.children.length > 0) {
        const propagate = (children, val) => children.map((child) => {
          const next = { ...child, carrier: val };
          if (Array.isArray(child.children) && child.children.length > 0) {
            next.children = propagate(child.children, val);
          }
          return next;
        });
        parentList[lastIndex].children = propagate(updatedParam.children, updates.carrier);
      }
    }

    commitSchema(newSchema);
  };

  /**
   * 按路径删除参数
   * @param {Array} path 路径数组
   */
  const handleDeleteByPath = (path) => {
    const { newSchema, parentList } = cloneAndLocate({ path });
    parentList.splice(path[path.length - 1], 1);
    commitSchema(newSchema);
  };

  /**
   * 在指定节点下追加子节点（仅复杂类型可用）
   * @param {Array} path 父节点路径
   */
  const handleAddChild = (path) => {
    // 父节点的同层数组 + 父节点在数组中的索引
    const { newSchema, parentList } = cloneAndLocate({ path });
    const lastIndex = path[path.length - 1];
    const target = parentList[lastIndex];

    // 子节点的 carrier 跟随父节点
    const childCarrier = target.carrier || carrierFilter || '';
    const newChild = createDefaultParamV2({ carrier: childCarrier });

    target.children = [...(target.children || []), newChild];
    commitSchema(newSchema);
  };

  /**
   * 获取指定路径节点的直接父节点
   * @param {Object} options 配置对象
   * options.path 当前节点路径
   * options.schema 当前完整 schema 数据
   * @returns {Object|null} 当前节点的直接父节点
   */
  const getParentParamByPath = (options) => {
    // options.path: 当前节点路径；options.schema: 当前完整 schema 数据
    const { path, schema } = options;

    if (!Array.isArray(path) || path.length <= 1) return null;

    // 逐级向下查找，停在当前节点的直接父节点
    let parentParam = null;
    let currentList = schema;
    for (let i = 0; i < path.length - 1; i += 1) {
      parentParam = currentList[path[i]];
      currentList = parentParam?.children || [];
    }

    return parentParam;
  };

  /**
   * 在指定节点同层追加兄弟节点
   * @param {Array} path 当前节点路径
   */
  const handleAddSibling = (path) => {
    const { newSchema, parentList } = cloneAndLocate({ path });
    const lastIndex = path[path.length - 1];
    const parentParam = getParentParamByPath({ path, schema: newSchema });

    // array 类型只能拥有一个子节点，阻止其子节点继续追加兄弟节点
    if (parentParam?.paramType === 'array') return;

    // 兄弟节点 carrier 跟随当前节点
    const siblingCarrier = parentList[lastIndex].carrier || carrierFilter || '';
    const newSibling = createDefaultParamV2({ carrier: siblingCarrier });

    parentList.splice(lastIndex + 1, 0, newSibling);
    commitSchema(newSchema);
  };

  /**
   * 顶部"+ 添加参数"按钮处理（无参数时显示）
   */
  const handleAddTopLevel = () => {
    const newParam = createDefaultParamV2({ carrier: carrierFilter || '' });
    commitSchema([...schemaData, newParam]);
  };

  // ============ 渲染过滤：按 carrierFilter 仅展示当前 Tab 的顶层参数 ============
  // 同时建立"展示 index -> 真实 index"映射，保证路径回写不串 Tab
  const visibleIndexMap = [];
  const visibleSchema = [];
  (schemaData || []).forEach((item, idx) => {
    if (!carrierFilter || item.carrier === carrierFilter) {
      visibleSchema.push(item);
      visibleIndexMap.push(idx);
    }
  });

  /**
   * 将展示态的路径还原成真实路径（仅顶层 index 需要映射）
   * @param {Array} displayPath 由子组件传回的路径
   */
  const resolveRealPath = (displayPath) => {
    if (!carrierFilter) return displayPath;
    if (!displayPath || displayPath.length === 0) return displayPath;
    const [first, ...rest] = displayPath;
    const realFirst = visibleIndexMap[first];
    if (realFirst === undefined) return displayPath;
    return [realFirst, ...rest];
  };

  // 顶部"+ 添加参数"按钮：仅当前 Tab 无参数时显示
  const showTopAddButton = editable && visibleSchema.length === 0;

  return (
    <div className="schema-editor-v2">
      {/* 顶部添加按钮（仅初始空状态显示） */}
      {showTopAddButton && (
        <Button
          type="dashed"
          size="small"
          onClick={handleAddTopLevel}
          className="schema-add-top-btn"
        >
          + 添加参数
        </Button>
      )}

      {/* 渲染当前 Tab 下的参数列表 */}
      {visibleSchema.map((param, displayIdx) => (
        <SchemaParamItem
          key={visibleIndexMap[displayIdx]}
          param={param}
          path={[displayIdx]}
          depth={0}
          editable={editable}
          hideCarrier={hideCarrier}
          lockedCarrier={carrierFilter || null}
          carrierOptions={carrierOptions}
          typeOptions={typeOptions}
          onUpdate={(displayPath, updates) => handleUpdateByPath(resolveRealPath(displayPath), updates)}
          onDelete={(displayPath) => handleDeleteByPath(resolveRealPath(displayPath))}
          onAddChild={(displayPath) => handleAddChild(resolveRealPath(displayPath))}
          onAddSibling={(displayPath) => handleAddSibling(resolveRealPath(displayPath))}
        />
      ))}
    </div>
  );
};

export default SchemaEditorV2;