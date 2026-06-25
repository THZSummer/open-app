import React, { useState, useMemo } from 'react';
import { Tabs, Input, Select, Button, Dropdown } from 'antd';
import { OUTPUT_CARRIER_TABS } from '../../constants';
import { collectUpstreamRefs } from '../../utils';
import { DEFAULT_TYPE_OPTIONS, isComplexType, MAX_SCHEMA_DEPTH } from '../../../../../components/SchemaEditor/constants';
import './NodeCards.m.less';

/**
 * 输出参数取值来源选项
 */
const VALUE_SOURCE_OPTIONS = [
  { value: 'static', label: '静态值' },
  { value: 'ref', label: '引用上游参数' },
];

/**
 * 创建默认输出参数
 * @returns {Object} 默认输出参数
 */
const createDefaultOutputParam = () => ({
  paramName: '',
  paramType: 'string',
  sourceType: 'static',
  paramValue: '',
  children: [],
});

/**
 * 把上游 ref 列表转换为分组 Select options
 * @param {Array} refs 上游引用参数列表
 * @returns {Array} 分组下拉选项
 */
const buildRefOptions = (refs) => {
  // 按节点分组，组内展示完整参数路径表达式
  const groupMap = new Map();
  refs.forEach((item) => {
    const groupLabel = item.groupLabel || item.nodeId || '上游参数';
    const groupOptions = groupMap.get(groupLabel) || [];
    groupOptions.push({
      value: item.value,
      label: item.label || item.path || item.value,
    });
    groupMap.set(groupLabel, groupOptions);
  });
  return Array.from(groupMap.entries()).map(([label, options]) => ({ label, options }));
};

/**
 * 数据输出节点卡片
 *
 * @param {Object} props
 *   props.node      输出节点数据
 *   props.editable  是否可编辑
 *   props.flowData  整个连接流数据
 *   props.appLimits 应用级上限（保留接口一致性）
 *   props.onChange  (updatedNode) => void
 */
const OutputCard = (props) => {
  // props.node / props.editable / props.flowData / props.appLimits / props.onChange
  const { node, editable, flowData, onChange } = props;

  // 当前激活的载体 Tab，默认从 body 开始（与配置顺序一致）
  const [activeCarrier, setActiveCarrier] = useState(OUTPUT_CARRIER_TABS[0].carrier);

  /**
   * 上游可引用参数列表
   */
  const upstreamRefs = useMemo(
    () => collectUpstreamRefs({ flowData, currentNodeId: node.id }),
    [flowData, node.id]
  );

  /**
   * 引用上游参数下拉选项
   */
  const refOptions = useMemo(() => buildRefOptions(upstreamRefs), [upstreamRefs]);

  /**
   * 更新某 carrier 下的参数列表
   *
   * @param {Object} params 配置对象
   *   params.carrier 载体
   *   params.list    更新后的参数列表
   */
  const updateCarrierList = (params) => {
    // params.carrier / params.list
    const { carrier, list } = params;
    const assembleParams = node.assembleParams || { body: [], header: [] };
    onChange({
      ...node,
      assembleParams: { ...assembleParams, [carrier]: list || [] },
    });
  };

  /**
   * 按路径深拷贝参数列表，并定位目标节点的父级数组
   *
   * @param {Object} params 配置对象
   *   params.list 参数列表
   *   params.path 路径数组
   * @returns {Object} 定位结果
   */
  const cloneAndLocate = (params) => {
    // params.list / params.path
    const { list, path } = params;
    const nextList = JSON.parse(JSON.stringify(list || []));
    let parentList = nextList;
    for (let i = 0; i < path.length - 1; i++) {
      parentList = parentList[path[i]].children;
    }
    return { nextList, parentList };
  };

  /**
   * 更新参数字段
   *
   * @param {Object} params 配置对象
   *   params.carrier 载体
   *   params.path    参数路径
   *   params.updates 字段更新值
   */
  const handleUpdateParam = (params) => {
    // params.carrier / params.path / params.updates
    const { carrier, path, updates } = params;
    const list = node.assembleParams?.[carrier] || [];
    const { nextList, parentList } = cloneAndLocate({ list, path });
    const lastIndex = path[path.length - 1];
    const current = parentList[lastIndex];
    const nextUpdates = { ...updates };

    // 切换值来源时清空输入值，避免静态值和引用变量串用
    if ('sourceType' in nextUpdates) {
      nextUpdates.paramValue = '';
    }
    parentList[lastIndex] = { ...current, ...nextUpdates };

    // 切换到非复杂类型时清空子参数
    if ('paramType' in nextUpdates && !isComplexType(nextUpdates.paramType)) {
      parentList[lastIndex].children = [];
    }
    updateCarrierList({ carrier, list: nextList });
  };

  /**
   * 删除参数
   *
   * @param {Object} params 配置对象
   *   params.carrier 载体
   *   params.path    参数路径
   */
  const handleDeleteParam = (params) => {
    // params.carrier / params.path
    const { carrier, path } = params;
    const list = node.assembleParams?.[carrier] || [];
    const { nextList, parentList } = cloneAndLocate({ list, path });
    parentList.splice(path[path.length - 1], 1);
    updateCarrierList({ carrier, list: nextList });
  };

  /**
   * 添加顶层参数
   * @param {string} carrier 载体
   */
  const handleAddTopLevel = (carrier) => {
    const list = node.assembleParams?.[carrier] || [];
    updateCarrierList({ carrier, list: [...list, createDefaultOutputParam()] });
  };

  /**
   * 添加子参数
   *
   * @param {Object} params 配置对象
   *   params.carrier 载体
   *   params.path    父参数路径
   */
  const handleAddChild = (params) => {
    // params.carrier / params.path
    const { carrier, path } = params;
    const list = node.assembleParams?.[carrier] || [];
    const { nextList, parentList } = cloneAndLocate({ list, path });
    const target = parentList[path[path.length - 1]];
    target.children = [...(target.children || []), createDefaultOutputParam()];
    updateCarrierList({ carrier, list: nextList });
  };

  /**
   * 添加兄弟参数
   *
   * @param {Object} params 配置对象
   *   params.carrier 载体
   *   params.path    当前参数路径
   */
  const handleAddSibling = (params) => {
    // params.carrier / params.path
    const { carrier, path } = params;
    const list = node.assembleParams?.[carrier] || [];
    const { nextList, parentList } = cloneAndLocate({ list, path });
    parentList.splice(path[path.length - 1] + 1, 0, createDefaultOutputParam());
    updateCarrierList({ carrier, list: nextList });
  };

  /**
   * 渲染输出参数取值输入框
   *
   * @param {Object} params 配置对象
   *   params.param   参数对象
   *   params.carrier 载体
   *   params.path    参数路径
   * @returns {JSX.Element} 取值输入控件
   */
  const renderValueInput = (params) => {
    // params.param / params.carrier / params.path
    const { param, carrier, path } = params;
    if (param.sourceType === 'ref') {
      return (
        <Select
          showSearch
          allowClear
          size="middle"
          className="schema-param-value"
          placeholder="选择上游参数"
          value={param.paramValue || undefined}
          disabled={!editable}
          options={refOptions}
          filterOption={(input, option) => {
            // 分组选项本身不参与过滤，实际过滤由子选项完成
            if (Array.isArray(option?.options)) return false;
            return (option?.label || option?.value || '').toLowerCase().includes((input || '').toLowerCase());
          }}
          onChange={(value) => handleUpdateParam({
            carrier,
            path,
            updates: { paramValue: value || '' },
          })}
        />
      );
    }

    return (
      <Input
        className="schema-param-value"
        placeholder="请输入静态值"
        value={param.paramValue || ''}
        disabled={!editable}
        onChange={(e) => handleUpdateParam({
          carrier,
          path,
          updates: { paramValue: e.target.value },
        })}
      />
    );
  };

  /**
   * 渲染单个输出参数行
   *
   * @param {Object} params 配置对象
   *   params.param   参数对象
   *   params.carrier 载体
   *   params.path    参数路径
   *   params.depth   当前层级
   * @returns {JSX.Element} 参数行
   */
  const renderParamItem = (params) => {
    // params.param / params.carrier / params.path / params.depth
    const { param, carrier, path, depth } = params;
    const complex = isComplexType(param.paramType);
    const isArrayType = param.paramType === 'array';
    const canAddChild = depth < MAX_SCHEMA_DEPTH - 1 && !(isArrayType && (param.children || []).length >= 1);
    const addMenuItems = [
      ...(canAddChild ? [{ key: 'child', label: '添加子节点' }] : []),
      { key: 'sibling', label: '添加兄弟节点' },
    ];

    return (
      <div key={path.join('-')} data-depth={depth}>
        <div className="schema-param-row output-param-row">
          <Input
            value={param.paramName}
            onChange={(e) => handleUpdateParam({ carrier, path, updates: { paramName: e.target.value } })}
            placeholder="参数名称"
            size="small"
            className="schema-param-name"
            disabled={!editable}
          />
          <Select
            value={param.paramType}
            onChange={(value) => handleUpdateParam({ carrier, path, updates: { paramType: value } })}
            size="small"
            className="schema-param-type"
            disabled={!editable}
            options={DEFAULT_TYPE_OPTIONS.map((item) => ({ value: item, label: item }))}
          />
          <Select
            value={param.sourceType || 'static'}
            onChange={(value) => handleUpdateParam({ carrier, path, updates: { sourceType: value } })}
            size="middle"
            className="schema-param-source"
            disabled={!editable}
            options={VALUE_SOURCE_OPTIONS}
          />
          {renderValueInput({ param, carrier, path })}
          {editable && (
            complex ? (
              <Dropdown
                menu={{
                  items: addMenuItems,
                  onClick: (info) => {
                    if (info.key === 'child') {
                      handleAddChild({ carrier, path });
                    } else {
                      handleAddSibling({ carrier, path });
                    }
                  },
                }}
                trigger={['click']}
                overlayClassName="schema-editor-v2-dropdown"
              >
                <Button type="text" size="small" className="schema-add-btn">添加</Button>
              </Dropdown>
            ) : (
              <Button
                type="text"
                size="small"
                className="schema-add-btn"
                onClick={() => handleAddSibling({ carrier, path })}
              >
                添加
              </Button>
            )
          )}
          {editable && (
            <Button
              type="text"
              size="middle"
              className="schema-delete-btn"
              onClick={() => handleDeleteParam({ carrier, path })}
            >
              删除
            </Button>
          )}
        </div>
        {complex && (
          <div className="schema-children-container">
            {(param.children || []).map((child, index) => renderParamItem({
              param: child,
              carrier,
              path: [...path, index],
              depth: depth + 1,
            }))}
          </div>
        )}
      </div>
    );
  };

  /**
   * 渲染指定 carrier 下的输出参数配置
   * @param {string} carrier 载体类型
   * @returns {JSX.Element} 输出参数配置区域
   */
  const renderOutputParams = (carrier) => {
    const list = node.assembleParams?.[carrier] || [];
    return (
      <div className="schema-editor-v2 output-schema-editor">
        {editable && list.length === 0 && (
          <Button
            type="dashed"
            size="small"
            onClick={() => handleAddTopLevel(carrier)}
            className="schema-add-top-btn"
          >
            + 添加参数
          </Button>
        )}
        {list.map((param, index) => renderParamItem({
          param,
          carrier,
          path: [index],
          depth: 0,
        }))}
      </div>
    );
  };

  return (
    <div>
      <div className="node-card-section">
        <div className="section-title">响应参数组装</div>
        <Tabs
          activeKey={activeCarrier}
          onChange={setActiveCarrier}
          items={OUTPUT_CARRIER_TABS.map((tab) => ({
            key: tab.key,
            label: tab.label,
            children: renderOutputParams(tab.carrier),
          }))}
        />
      </div>
    </div>
  );
};

export default OutputCard;
