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
import { Form, Input, Select, Button, Card } from 'antd';
import ParameterSelector from '../ParameterSelector/ParameterSelector';

const { Option } = Select;

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
}) => {
  const isComplexType = param.paramType === 'object' || param.paramType === 'array';
  const showSourceType = mode === 'reference';
  const sourceType = param.sourceType || 'static';
  const isArrayChild = depth > 0;

  /**
   * 根据模式渲染值输入控件
   * - default模式：显示参数描述输入框
   * - reference模式：根据sourceType显示来源类型下拉框和对应的输入控件
   */
  const renderValueInput = () => {
    if (!showSourceType) {
      return (
        <Input
          value={param.description}
          onChange={(e) => onUpdate(path, { description: e.target.value })}
          placeholder="参数描述"
          size="small"
          style={{ flex: 2 }}
          disabled={!editable}
        />
      );
    }

    return (
      <>
        <Select
          value={sourceType}
          onChange={(val) => onUpdate(path, { sourceType: val, paramValue: '', referencePath: '' })}
          size="small"
          style={{ width: 100 }}
          disabled={!editable}
        >
          <Option value="static">静态值</Option>
          <Option value="reference">引用参数</Option>
        </Select>

        {sourceType === 'static' ? (
          <Input
            value={param.paramValue}
            onChange={(e) => onUpdate(path, { paramValue: e.target.value })}
            placeholder="输入参数值"
            size="small"
            style={{ flex: 2 }}
            disabled={!editable}
          />
        ) : (
          <ParameterSelector
            upstreamParams={upstreamParams}
            value={param.referencePath}
            onChange={(val) => onUpdate(path, { referencePath: val })}
            disabled={!editable}
          />
        )}
      </>
    );
  };

  if (!isComplexType) {
    return (
      <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 8, marginLeft: depth > 0 ? 16 : 0 }}>
        <Input
          value={param.paramName}
          onChange={(e) => onUpdate(path, { paramName: e.target.value })}
          placeholder="参数名称"
          size="small"
          style={{ flex: 2, minWidth: 120 }}
          disabled={!editable}
        />
        <Select
          value={param.paramType}
          onChange={(val) => {
            const isComplex = val === 'object' || val === 'array';
            const updates = {
              paramType: val,
              children: isComplex ? [] : param.children,
            };
            if (isComplex && showSourceType) {
              updates.sourceType = 'reference';
            }
            onUpdate(path, updates);
          }}
          size="small"
          style={{ width: 90 }}
          disabled={!editable}
        >
          {isArrayChild ? (
            <>
              <Option value="object">object</Option>
              <Option value="array">array</Option>
            </>
          ) : (
            <>
              <Option value="string">string</Option>
              <Option value="number">number</Option>
              <Option value="boolean">boolean</Option>
              <Option value="object">object</Option>
              <Option value="array">array</Option>
            </>
          )}
        </Select>
        {renderValueInput()}
        {editable && (
          <Button
            type="text"
            danger
            size="small"
            onClick={() => onDelete(path)}
          >
            删除
          </Button>
        )}
      </div>
    );
  }

  return (
    <Card
      size="small"
      style={{
        marginBottom: 12,
        border: '1px solid #e8e8e8',
        borderRadius: 6,
        padding: 12,
        backgroundColor: '#fafbfc',
      }}
    >
      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
        <Input
          value={param.paramName}
          onChange={(e) => onUpdate(path, { paramName: e.target.value })}
          placeholder="参数名称"
          size="small"
          style={{ flex: 2, minWidth: 120 }}
          disabled={!editable}
        />
        <Select
          value={param.paramType}
          onChange={(val) => {
            const isComplex = val === 'object' || val === 'array';
            const updates = {
              paramType: val,
              children: isComplex ? [] : param.children,
            };
            if (isComplex && showSourceType) {
              updates.sourceType = 'reference';
            }
            onUpdate(path, updates);
          }}
          size="small"
          style={{ width: 90 }}
          disabled={!editable}
        >
          {isArrayChild ? (
            <>
              <Option value="object">object</Option>
              <Option value="array">array</Option>
            </>
          ) : (
            <>
              <Option value="string">string</Option>
              <Option value="number">number</Option>
              <Option value="boolean">boolean</Option>
              <Option value="object">object</Option>
              <Option value="array">array</Option>
            </>
          )}
        </Select>
        {isComplexType && showSourceType && (
          <ParameterSelector
            upstreamParams={upstreamParams}
            value={param.referencePath}
            onChange={(val) => onUpdate(path, { referencePath: val })}
            disabled={!editable}
          />
        )}
        {editable && (
          <Button
            type="text"
            danger
            size="small"
            onClick={() => onDelete(path)}
          >
            删除
          </Button>
        )}
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
          />
        ))}

        {editable && (
          <Button
            type="dashed"
            size="small"
            block
            onClick={() => {
              const newChild = mode === 'reference'
                ? { paramName: '', paramType: 'object', sourceType: 'reference', paramValue: '', referencePath: '', description: '', children: [] }
                : { paramName: '', paramType: 'object', sourceType: 'static', paramValue: '', referencePath: '', description: '', children: [] };
              const newChildren = [
                ...(param.children || []),
                newChild
              ];
              onUpdate(path, { children: newChildren });
            }}
            style={{ marginTop: 4 }}
            disabled={(param.children || []).length >= 1}
          >
            {(param.children || []).length === 0 ? '+ 添加子参数' : '已有子参数（array只能添加一个子参数）'}
          </Button>
        )}
      </div>
    </Card>
  );
};

/**
 * Schema编辑器组件
 * @param {Object} props - 组件属性
 * @param {Object} props.form - Ant Design Form实例
 * @param {string} props.schemaType - Schema类型（requestSchema/responseSchema）
 * @param {boolean} props.editable - 是否可编辑
 * @param {string} props.title - 标题（可选）
 * @param {string} props.mode - 渲染模式：'default' | 'reference'
 * @param {Array} props.upstreamParams - 上游节点参数列表（mode='reference' 时使用）
 */
const SchemaEditor = ({
  form,
  schemaType,
  editable,
  title = 'Schema配置',
  mode = 'default',
  upstreamParams = [],
}) => {
  const [schemaData, setSchemaData] = useState([]);

  useEffect(() => {
    const currentConfig = form.getFieldValue('apiConfig') || {};
    const schema = currentConfig[schemaType] || [];
    setSchemaData(schema);
  }, [form, schemaType]);

  const handleSchemaChange = useCallback((newSchema) => {
    const currentConfig = form.getFieldValue('apiConfig') || {};
    const updatedConfig = {
      ...currentConfig,
      [schemaType]: newSchema,
    };
    form.setFieldValue('apiConfig', updatedConfig);
    setSchemaData(newSchema);
  }, [form, schemaType]);

  const handleUpdateByPath = useCallback((path, updates) => {
    const newSchema = JSON.parse(JSON.stringify(schemaData));
    let current = newSchema;

    for (let i = 0; i < path.length - 1; i++) {
      current = current[path[i]].children;
    }

    const lastIndex = path[path.length - 1];
    current[lastIndex] = { ...current[lastIndex], ...updates };

    handleSchemaChange(newSchema);
  }, [schemaData, handleSchemaChange]);

  const handleDeleteByPath = useCallback((path) => {
    const newSchema = JSON.parse(JSON.stringify(schemaData));
    let current = newSchema;

    if (path.length === 1) {
      newSchema.splice(path[0], 1);
    } else {
      for (let i = 0; i < path.length - 1; i++) {
        current = current[path[i]].children;
      }
      current.splice(path[path.length - 1], 1);
    }

    handleSchemaChange(newSchema);
  }, [schemaData, handleSchemaChange]);

  const handleAddParam = useCallback(() => {
    const newParam = mode === 'reference'
      ? { paramName: '', paramType: 'string', sourceType: 'static', paramValue: '', referencePath: '', description: '', children: [] }
      : { paramName: '', paramType: 'string', description: '', children: [] };
    const newSchema = [...schemaData, newParam];
    handleSchemaChange(newSchema);
  }, [schemaData, handleSchemaChange, mode]);

  return (
    <div className="schema-editor">
      {schemaData.map((param, index) => (
        <SchemaParamItem
          key={index}
          param={param}
          index={index}
          path={[index]}
          onUpdate={handleUpdateByPath}
          onDelete={handleDeleteByPath}
          editable={editable}
          depth={0}
          mode={mode}
          upstreamParams={upstreamParams}
        />
      ))}
      {editable && (
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
