/**
 * ========================================
 * Schema编辑器通用组件
 * ========================================
 *
 * 功能：
 * - 渲染Schema参数列表（支持无限层级嵌套）
 * - 处理参数的增删改操作
 * - 支持递归渲染子参数
 * - 支持引用选择模式（选择上游节点参数）
 */

import React, { useEffect, useState, useCallback } from 'react';
import { Input, Select, Button, Card } from 'antd';
import ParameterSelector from '../ParameterSelector/ParameterSelector';
import { STYLES, PARAM_DEFAULTS, TYPE_OPTIONS, isComplexType } from './constants';

const { Option } = Select;

/**
 * 创建默认参数对象
 * @param {Object} options - 配置选项
 * @param {string} options.mode - 模式：'default' 或 'reference'
 * @param {boolean} options.showCarrier - 是否显示carrier
 * @param {Array} options.carrierOptions - carrier可选值
 * @returns {Object} 新的参数对象
 */
const createDefaultParam = (options) => {
  // 解构配置选项，设置默认值
  const {
    mode = 'default',
    showCarrier = false,
    carrierOptions = []
  } = options;

  // 基于 PARAM_DEFAULTS 创建基础参数对象
  // 根据 mode 设置 sourceType：reference 模式使用 'reference'，否则使用默认值
  const baseParam = {
    ...PARAM_DEFAULTS,
    sourceType: mode === 'reference' ? 'reference' : PARAM_DEFAULTS.sourceType
  };

  // 如果需要显示 carrier 且有可用的 carrier 选项，设置默认 carrier 值
  if (showCarrier && carrierOptions.length > 0) {
    baseParam.carrier = carrierOptions[0];
  }

  return baseParam;
};

/**
 * Schema参数项组件（支持递归嵌套和引用选择模式）
 * @param {Object} props - 组件属性
 * @param {Object} props.param - 参数对象
 * @param {number} props.index - 参数索引
 * @param {Array} props.path - 参数路径数组，用于追踪嵌套层级
 * @param {Function} props.onUpdate - 更新参数的回调
 * @param {Function} props.onDelete - 删除参数的回调
 * @param {boolean} props.editable - 是否可编辑
 * @param {number} props.depth - 当前层级深度
 * @param {string} props.mode - 渲染模式：'default' | 'reference'
 * @param {Array} props.upstreamParams - 上游节点参数列表（mode='reference' 时使用）
 * @param {boolean} props.showCarrier - 是否显示carrier字段
 * @param {Array} props.carrierOptions - carrier可选值配置
 * @param {string} props.parentCarrier - 父参数的carrier值，用于锁定子参数的carrier
 * @param {Array} props.typeOptions - 可选的参数类型列表，默认包含所有类型
 * @param {Object} props.lockedFields - 锁定字段配置
 * @param {boolean} props.lockedFields.paramName - 是否锁定参数名称
 * @param {boolean} props.lockedFields.paramType - 是否锁定参数类型
 * @param {boolean} props.lockedFields.carrier - 是否锁定carrier字段
 * @param {boolean} props.lockedFields.sourceType - 是否锁定来源类型
 * @param {boolean} props.showActionButtons - 是否显示增删按钮，默认true
 */
const SchemaParamItem = ({
  param,
  index,
  path,
  onUpdate,
  onDelete,
  editable,
  depth = 0,
  mode = 'default',
  upstreamParams = [],
  showCarrier = false,
  carrierOptions = [],
  parentCarrier = null,
  typeOptions = TYPE_OPTIONS,  // 使用常量配置的可选类型列表
  valueInputType = 'description',
  lockedFields = {},
  showActionButtons = true,
}) => {
  const paramIsComplex = isComplexType(param.paramType);
  const showSourceType = mode === 'reference';
  const sourceType = param.sourceType || 'static';
  const isCarrierLocked = !!parentCarrier || lockedFields.carrier;
  const effectiveCarrier = parentCarrier || param.carrier;

  /**
   * 根据模式渲染值输入控件
   * - default模式：显示参数描述或字段映射输入框（根据valueInputType）
   * - reference模式：根据sourceType显示来源类型下拉框和对应的输入控件
   */
  const renderValueInput = () => {
    // 非 reference 模式：根据 valueInputType 显示描述或字段映射输入框
    if (!showSourceType) {
      return (
        <Input
          value={param[valueInputType]}  // 根据 valueInputType 读取对应字段值
          onChange={(e) => onUpdate(path, { [valueInputType]: e.target.value })}  // 更新对应字段
          placeholder={valueInputType === 'fieldName' ? '字段映射' : '参数描述'}  // 动态设置提示文本
          size="small"
          style={STYLES.valueInput}
          disabled={!editable}  // 非编辑模式下禁用输入
        />
      );
    }

    // reference 模式：显示来源类型选择和对应的值输入
    return (
      <>
        {/* 来源类型选择：静态值或引用参数 */}
        <Select
          value={sourceType}
          onChange={(val) => onUpdate(path, { sourceType: val, paramValue: '', referencePath: '' })}  // 切换类型时清空值
          size="small"
          style={STYLES.sourceTypeSelect}
          disabled={!editable || lockedFields.sourceType}
        >
          <Option value="static">静态值</Option>
          <Option value="reference">引用参数</Option>
        </Select>

        {/* 根据来源类型显示不同的输入控件 */}
        {sourceType === 'static' ? (
          // 静态值：显示文本输入框
          <Input
            value={param.paramValue}
            onChange={(e) => onUpdate(path, { paramValue: e.target.value })}
            placeholder="输入参数值"
            size="small"
            style={STYLES.complexValueInput}
            disabled={!editable}
          />
        ) : (
          // 引用参数：显示参数选择器
          <ParameterSelector
            upstreamParams={upstreamParams}
            value={param.referencePath}
            onChange={(val) => onUpdate(path, { referencePath: val })}
            disabled={!editable}
            style={STYLES.complexValueInput}
          />
        )}
      </>
    );
  };

  /**
   * 处理参数类型变更
   * @param {string} val - 新的参数类型
   */
  const handleTypeChange = (val) => {
    // 判断新类型是否为复杂类型（object 或 array）
    const isComplex = isComplexType(val);

    // 构建更新对象：更新类型，清空子参数（如果变为复杂类型）
    const updates = {
      paramType: val,
      children: isComplex ? [] : param.children,  // 变为复杂类型时清空子参数，否则保留
    };

    // 如果是复杂类型且为 reference 模式，设置 sourceType 为 'reference'
    if (isComplex && showSourceType) {
      updates.sourceType = 'reference';
    }

    // 执行更新
    onUpdate(path, updates);
  };

  /**
   * 渲染通用参数输入行（参数名称、类型、carrier）
   * @returns {JSX.Element} 参数行组件
   */
  const renderParamRow = () => (
    <>
      {/* 参数名称输入框 */}
      <Input
        value={param.paramName}
        onChange={(e) => onUpdate(path, { paramName: e.target.value })}
        placeholder="参数名称"
        size="small"
        style={STYLES.paramInput}
        disabled={!editable || lockedFields.paramName}  // 根据锁定状态禁用
      />

      {/* 参数类型选择器 */}
      <Select
        value={param.paramType}
        onChange={handleTypeChange}  // 类型变更时调用 handleTypeChange 处理
        size="small"
        style={STYLES.typeSelect}
        disabled={!editable || lockedFields.paramType}
      >
        {typeOptions.map(opt => (
          <Option key={opt} value={opt}>{opt}</Option>
        ))}
      </Select>

      {/* Carrier 选择器（仅当 showCarrier 为 true 时显示） */}
      {showCarrier && (
        <Select
          value={effectiveCarrier}  // 使用 effectiveCarrier，优先使用父级 carrier
          onChange={(val) => onUpdate(path, { carrier: val })}
          size="small"
          style={STYLES.carrierSelect}
          disabled={!editable || isCarrierLocked}  // 根据锁定状态禁用
        >
          {carrierOptions.map(opt => (
            <Option key={opt} value={opt}>
              {opt}
            </Option>
          ))}
        </Select>
      )}
    </>
  );

  /**
   * 渲染删除按钮
   * @returns {JSX.Element|null} 删除按钮组件
   */
  const renderDeleteButton = () => {
    // 仅在可编辑且显示操作按钮时显示删除按钮
    if (editable && showActionButtons) {
      return (
        <Button
          type="text"
          danger  // 使用危险按钮样式（红色）
          size="small"
          onClick={() => onDelete(path)}  // 点击时调用 onDelete 回调
        >
          删除
        </Button>
      );
    }
    // 否则返回 null，不渲染按钮
    return null;
  };

  // 非复杂类型：简单行布局（参数名称、类型、值、删除按钮在一行）
  if (!paramIsComplex) {
    return (
      <div style={{ ...STYLES.paramRow, marginLeft: depth > 0 ? 16 : 0 }}>
        {renderParamRow()}
        {renderValueInput()}
        {renderDeleteButton()}
      </div>
    );
  }

  // 复杂类型（object/array）：使用 Card 包装，支持嵌套子参数
  return (
    <Card
      size="small"
      style={STYLES.cardStyle}
    >
      <div style={STYLES.paramRow}>
        {/* 渲染通用参数行（参数名称、类型、carrier） */}
        {renderParamRow()}

        {/* 仅在复杂类型且为 reference 模式时显示来源类型选择 */}
        {paramIsComplex && showSourceType && (
          <>
            {/* 来源类型选择：静态值或引用参数 */}
            <Select
              value={sourceType}
              onChange={(val) => onUpdate(path, { sourceType: val, paramValue: '', referencePath: '' })}
              size="small"
              style={STYLES.sourceTypeSelect}
              disabled={!editable || lockedFields.sourceType}
            >
              <Option value="static">静态值</Option>
              <Option value="reference">引用参数</Option>
            </Select>

            {/* 根据来源类型显示对应的输入控件 */}
            {sourceType === 'reference' ? (
              // 引用参数：使用参数选择器
              <ParameterSelector
                upstreamParams={upstreamParams}
                value={param.referencePath}
                onChange={(val) => onUpdate(path, { referencePath: val })}
                disabled={!editable}
                style={STYLES.complexValueInput}
              />
            ) : (
              // 静态值：显示 JSON 输入框
              <Input
                value={param.paramValue}
                onChange={(e) => onUpdate(path, { paramValue: e.target.value })}
                placeholder="输入静态对象值（JSON格式）"
                size="small"
                style={STYLES.complexValueInput}
                disabled={!editable}
              />
            )}
          </>
        )}

        {/* 渲染删除按钮 */}
        {renderDeleteButton()}
      </div>

      <div style={{ marginTop: 12 }}>
        {(param.children || []).map((child, childIndex) => (
          <SchemaParamItem
            key={`${index}-${childIndex}`}
            param={child}
            index={childIndex}
            path={[...path, childIndex]}
            onUpdate={onUpdate}
            onDelete={onDelete}
            editable={editable}
            depth={depth + 1}
            mode={mode}
            upstreamParams={upstreamParams}
            showCarrier={showCarrier}
            carrierOptions={carrierOptions}
            parentCarrier={effectiveCarrier}
            valueInputType={valueInputType}
            showActionButtons={showActionButtons}
            lockedFields={lockedFields}
          />
        ))}

        {/* 仅在可编辑且显示操作按钮时显示添加子参数按钮 */}
        {editable && showActionButtons && (() => {
          // 计算子参数相关状态
          const children = param.children || [];
          const hasChildren = children.length > 0;  // 是否有子参数
          const isArrayType = param.paramType === 'array';  // 是否为 array 类型
          const canAddMore = !isArrayType || !hasChildren;  // array 类型只能添加一个

          // 根据状态计算按钮文本（使用对象映射避免嵌套判断）
          const buttonTextMap = {
            initial: '+ 添加子参数',
            arrayLimit: '已有子参数（array只能添加一个子参数）',
            continue: '+ 继续添加子参数'
          };

          let buttonText;
          if (!hasChildren) {
            buttonText = buttonTextMap.initial;  // 首次添加
          } else if (isArrayType) {
            buttonText = buttonTextMap.arrayLimit;  // array 类型已达上限
          } else {
            buttonText = buttonTextMap.continue;  // 继续添加
          }

          return (
            <Button
              type="dashed"
              size="small"
              block
              onClick={() => {
                // 创建新的子参数对象
                const newChild = createDefaultParam({ mode, showCarrier, carrierOptions });
                // 将新子参数添加到现有子参数数组中
                const newChildren = [...children, newChild];
                // 更新当前参数的 children
                onUpdate(path, { children: newChildren });
              }}
              style={{ marginTop: 4 }}
              disabled={!canAddMore}  // 根据状态禁用按钮
            >
              {buttonText}
            </Button>
          );
        })()}
      </div>
    </Card>
  );
};

/**
 * Schema编辑器通用组件
 * @param {Object} props - 组件属性
 * @param {Object} props.form - Ant Design Form实例
 * @param {string} props.schemaType - Schema类型（requestSchema/responseSchema）
 * @param {boolean} props.editable - 是否可编辑
 * @param {string} props.title - 标题（可选）
 * @param {string} props.mode - 渲染模式：'default' | 'reference'
 * @param {Array} props.upstreamParams - 上游节点参数列表（mode='reference' 时使用）
 * @param {boolean} props.showCarrier - 是否显示carrier字段
 * @param {Array} props.carrierOptions - carrier可选值配置
 * @param {Function} props.onChange - 数据变化回调函数，接收新的schema数组作为参数
 * @param {Array} props.typeOptions - 可选的参数类型列表，默认包含所有类型
 * @param {Object} props.lockedFields - 锁定字段配置
 * @param {boolean} props.lockedFields.paramName - 是否锁定参数名称
 * @param {boolean} props.lockedFields.paramType - 是否锁定参数类型
 * @param {boolean} props.lockedFields.carrier - 是否锁定carrier字段
 * @param {boolean} props.lockedFields.sourceType - 是否锁定来源类型
 * @param {boolean} props.showActionButtons - 是否显示增删按钮，默认true
 */
const SchemaEditor = ({
  form,
  schemaType,
  editable,
  title = 'Schema配置',
  mode = 'default',
  upstreamParams = [],
  showCarrier = false,
  carrierOptions = [],
  onChange,
  typeOptions = TYPE_OPTIONS,  // 使用常量配置的可选类型列表
  valueInputType = 'description',
  apiConfig = {},
  value,
  lockedFields = {},
  showActionButtons = true,
}) => {
  const [schemaData, setSchemaData] = useState([]);
  const [internalChange, setInternalChange] = useState(false);
  const [carrierDefaultSet, setCarrierDefaultSet] = useState(false);
  const [dataSource, setDataSource] = useState('apiConfig');

  /**
   * 监听数据源变化，初始化 schema 数据
   * 优先级：value prop > form.apiConfig > apiConfig prop
   */
  useEffect(() => {
    // 如果是内部状态变化，不覆盖数据（防止循环更新）
    if (internalChange) {
      return;
    }

    // 优先使用传入的 value prop
    if (value !== undefined) {
      setSchemaData(Array.isArray(value) ? value : []);
      setDataSource('value');  // 标记数据来源为 value prop
      setCarrierDefaultSet(true);  // value 来源不需要自动设置 carrier
    } else {
      // 否则从 form 的 apiConfig 中读取
      if (form && form.getFieldValue) {
        const currentConfig = form.getFieldValue('apiConfig') || {};
        const schema = currentConfig[schemaType] || [];
        setSchemaData(Array.isArray(schema) ? schema : []);
        setDataSource('apiConfig');
        setCarrierDefaultSet(false);  // apiConfig 来源需要检查并设置 carrier
      } else if (apiConfig && apiConfig[schemaType]) {
        // 尝试从 apiConfig prop 中读取
        const schema = apiConfig[schemaType] || [];
        setSchemaData(Array.isArray(schema) ? schema : []);
        setDataSource('apiConfig');
        setCarrierDefaultSet(false);
      }
    }
  }, [schemaType, value, form, apiConfig]);

  /**
   * 为所有参数设置默认 carrier 值
   * 仅在数据来自 apiConfig 且可编辑时执行
   */
  useEffect(() => {
    // 检查是否需要执行：仅在 apiConfig 来源、可编辑、未设置默认值、有数据、有 carrier 选项时执行
    if (dataSource !== 'apiConfig' || !editable || carrierDefaultSet || schemaData.length === 0 || carrierOptions.length === 0) {
      return;
    }

    const defaultCarrier = carrierOptions[0];  // 获取第一个 carrier 作为默认值
    const hasUndefinedCarrier = schemaData.some(param => !param.carrier);  // 检查是否有未设置 carrier 的参数

    // 仅在有未设置 carrier 的参数时才更新
    if (hasUndefinedCarrier) {
      // 递归更新所有参数的 carrier 值
      const updateCarrierDefaults = (params) => {
        return params.map(param => {
          const updated = { ...param };
          // 如果当前参数未设置 carrier，使用默认值
          if (!updated.carrier) {
            updated.carrier = defaultCarrier;
          }
          // 如果有子参数，递归处理
          if (updated.children && updated.children.length > 0) {
            updated.children = updateCarrierDefaults(updated.children);
          }
          return updated;
        });
      };

      const updatedSchema = updateCarrierDefaults(schemaData);
      setSchemaData(updatedSchema);
      setCarrierDefaultSet(true);  // 标记已设置默认值，防止重复执行
    }
  }, [schemaData, carrierOptions, editable, dataSource]);

  /**
   * 处理 schema 数据变化
   * @param {Array} newSchema - 新的 schema 数据
   */
  const handleSchemaChange = useCallback((newSchema) => {
    // 标记为内部状态变化，防止 useEffect 重新覆盖数据
    setInternalChange(true);

    // 更新本地状态
    setSchemaData(newSchema);

    // 如果有 onChange 回调，通知父组件数据变化
    if (onChange && typeof onChange === 'function') {
      onChange(newSchema);
    }

    // 如果有 form 实例，同步更新 form 的 apiConfig 字段
    if (form && form.setFieldValue) {
      const currentConfig = form.getFieldValue('apiConfig') || {};
      const updatedConfig = {
        ...currentConfig,
        [schemaType]: newSchema,  // 根据 schemaType 更新对应的 schema 数据
      };
      form.setFieldValue('apiConfig', updatedConfig);
    }

    // 重置内部状态变化标记
    setInternalChange(false);
  }, [form, schemaType, onChange]);

  /**
   * 根据路径更新 schema 中的参数
   * @param {Array} path - 参数路径数组，如 [0, 1] 表示 schemaData[0].children[1]
   * @param {Object} updates - 要更新的字段和值
   */
  const handleUpdateByPath = useCallback((path, updates) => {
    // 深拷贝 schema 数据，避免直接修改状态
    const newSchema = JSON.parse(JSON.stringify(schemaData));

    // 沿着路径找到要更新的参数的父级
    let current = newSchema;
    for (let i = 0; i < path.length - 1; i++) {
      current = current[path[i]].children;  // 逐层深入到 children 数组
    }

    // 获取路径最后一项作为索引，更新对应的参数
    const lastIndex = path[path.length - 1];
    current[lastIndex] = { ...current[lastIndex], ...updates };

    // 如果更新的是 carrier 字段且当前参数是复杂类型，需要递归更新所有子参数的 carrier
    if ('carrier' in updates) {
      const updatedParam = current[lastIndex];
      const paramIsComplex = isComplexType(updatedParam.paramType);

      // 仅对复杂类型参数进行递归更新
      if (paramIsComplex && updatedParam.children && updatedParam.children.length > 0) {
        // 递归更新子参数的 carrier 值
        const updateChildrenCarrier = (children, carrierValue) => {
          return children.map(child => {
            const updatedChild = { ...child, carrier: carrierValue };
            // 如果子参数也是复杂类型，继续递归更新其子参数
            if (child.children && child.children.length > 0) {
              updatedChild.children = updateChildrenCarrier(child.children, carrierValue);
            }
            return updatedChild;
          });
        };
        current[lastIndex].children = updateChildrenCarrier(updatedParam.children, updates.carrier);
      }
    }

    // 触发 schema 变化处理
    handleSchemaChange(newSchema);
  }, [schemaData, handleSchemaChange]);

  /**
   * 根据路径删除 schema 中的参数
   * @param {Array} path - 参数路径数组
   */
  const handleDeleteByPath = useCallback((path) => {
    // 深拷贝 schema 数据，避免直接修改状态
    const newSchema = JSON.parse(JSON.stringify(schemaData));
    let current = newSchema;

    // 路径长度为 1 表示要删除顶层参数
    if (path.length === 1) {
      // 直接在顶层数组中删除
      newSchema.splice(path[0], 1);
    } else {
      // 否则需要沿着路径找到要删除参数的父级数组
      for (let i = 0; i < path.length - 1; i++) {
        current = current[path[i]].children;  // 逐层深入到 children 数组
      }
      // 在父级数组中删除对应的参数
      current.splice(path[path.length - 1], 1);
    }

    // 触发 schema 变化处理
    handleSchemaChange(newSchema);
  }, [schemaData, handleSchemaChange]);

  /**
   * 添加新参数
   */
  const handleAddParam = useCallback(() => {
    // 创建新的默认参数对象
    const newParam = createDefaultParam({ mode, showCarrier, carrierOptions });

    // 将新参数添加到 schema 数组末尾
    const newSchema = [...schemaData, newParam];

    // 触发 schema 变化处理
    handleSchemaChange(newSchema);
  }, [schemaData, handleSchemaChange, mode, showCarrier, carrierOptions]);

  return (
    <div className="schema-editor">
      {/* 遍历渲染所有顶层参数 */}
      {schemaData.map((param, index) => (
        <SchemaParamItem
          key={index}
          param={param}
          index={index}
          path={[index]}  // 顶层参数路径为 [index]
          onUpdate={handleUpdateByPath}  // 更新参数的处理函数
          onDelete={handleDeleteByPath}  // 删除参数的处理函数
          editable={editable}
          depth={0}  // 顶层参数深度为 0
          mode={mode}
          upstreamParams={upstreamParams}
          showCarrier={showCarrier}
          carrierOptions={carrierOptions}
          typeOptions={typeOptions}
          valueInputType={valueInputType}
          lockedFields={lockedFields}
          showActionButtons={showActionButtons}
        />
      ))}

      {/* 仅在可编辑且显示操作按钮时显示添加按钮 */}
      {editable && showActionButtons && (
        <Button
          type="dashed"
          block
          onClick={handleAddParam}
        >
          + 添加参数
        </Button>
      )}
    </div>
  );
};

export default SchemaEditor;
