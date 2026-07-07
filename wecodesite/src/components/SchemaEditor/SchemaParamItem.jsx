/**
 * ========================================
 * SchemaParamItem - 参数行组件（V2）
 * ========================================
 *
 * 由 SchemaEditorV2 调用，负责渲染单个参数行（含递归子参数）。
 * 与 SchemaEditorV2 的关系：
 * - SchemaEditorV2 维护完整 schemaData 和路径回写逻辑
 * - SchemaParamItem 仅负责"渲染一行 + 递归渲染 children"，并通过回调把
 *   增删改事件冒泡给 SchemaEditorV2
 */

import React from 'react';
import { Input, Select, Button, Dropdown, Menu } from 'antd';
import { isComplexType, MAX_SCHEMA_DEPTH } from './constants';

const { Option } = Select;

/**
 * 渲染单个参数行（含递归子参数）
 * @param {Object} props 组件属性
 * props.param 当前参数对象
 * props.path 路径数组，用于回写到 schemaData
 * props.depth 当前层级深度（顶层 = 0）
 * props.editable 是否可编辑
 * props.hideCarrier 是否隐藏 carrier 下拉
 * props.lockedCarrier 锁定的 carrier 值（来自 carrierFilter 或父参数）
 * props.parentParamType 父级参数类型，用于控制 array 子节点的兄弟新增入口
 * props.carrierOptions carrier 候选项
 * props.typeOptions 类型候选项
 * props.onUpdate 路径更新回调
 * props.onDelete 路径删除回调
 * props.onAddChild 添加子节点回调
 * props.onAddSibling 添加兄弟节点回调
 */
const SchemaParamItem = (props) => {
  const {
    param,
    path,
    depth,
    editable,
    hideCarrier,
    lockedCarrier,
    parentParamType,
    carrierOptions,
    typeOptions,
    onUpdate,
    onDelete,
    onAddChild,
    onAddSibling,
  } = props;

  // 是否复杂类型，决定是否渲染子参数容器与添加按钮形态
  const complex = isComplexType(param.paramType);
  // array 类型最多 1 个子参数；object 不限制
  const isArrayType = param.paramType === 'array';
  const currentChildrenCount = (param.children || []).length;
  // 是否还允许向下增加子节点：未达层级上限，且 array 类型尚未拥有子参数
  const canAddChild = depth < MAX_SCHEMA_DEPTH - 1
    && !(isArrayType && currentChildrenCount >= 1);
  // carrier 是否被锁定（顶层受 carrierFilter 锁定，子层级跟随父级）
  const carrierLocked = !!lockedCarrier;
  // 当前节点是否允许添加兄弟节点；array 的子节点不能继续添加兄弟节点
  const canAddSibling = parentParamType !== 'array';
  // 当前节点是否存在可用添加动作，用于避免展示空菜单
  const canShowAddButton = canAddChild || canAddSibling;
  // 实际展示的 carrier 值
  const effectiveCarrier = lockedCarrier || param.carrier || '';

  /**
   * 处理参数类型变更
   * @param {string} val 新类型
   */
  const handleTypeChange = (val) => {
    // 切换到非复杂类型时清空 children，避免残留数据
    const updates = {
      paramType: val,
      children: isComplexType(val) ? (param.children || []) : [],
    };
    onUpdate(path, updates);
  };

  /**
   * 复杂类型行尾添加按钮的下拉菜单项
   */
  const addMenuItems = [
    // 仅在允许添加子节点时提供"添加子节点"（含层级上限与 array 单子限制）
    ...(canAddChild
      ? [{ key: 'child', label: '添加子节点' }]
      : []),
    // 仅在当前节点允许添加兄弟节点时提供“添加兄弟节点”
    ...(canAddSibling
      ? [{ key: 'sibling', label: '添加兄弟节点' }]
      : []),
  ];

  /**
   * 处理下拉菜单点击
   * @param {Object} info antd Dropdown 点击事件参数
   */
  const handleAddMenuClick = (info) => {
    if (info.key === 'child') {
      onAddChild(path);
    } else {
      onAddSibling(path);
    }
  };

  /**
   * 渲染复杂类型添加菜单
   * @returns {React.ReactNode} 添加菜单节点
   */
  const renderAddMenu = () => (
    <Menu onClick={handleAddMenuClick}>
      {addMenuItems.map((item) => (
        <Menu.Item key={item.key}>{item.label}</Menu.Item>
      ))}
    </Menu>
  );

  return (
    <div data-depth={depth}>
      {/* 参数行 */}
      <div className="schema-param-row">
        {/* 参数名 */}
        <Input
          value={param.paramName}
          onChange={(e) => onUpdate(path, { paramName: e.target.value })}
          placeholder="参数名称"
          className="schema-param-name"
          disabled={!editable}
        />

        {/* 参数类型 */}
        <Select
          value={param.paramType}
          onChange={handleTypeChange}
          className="schema-param-type"
          disabled={!editable}
        >
          {typeOptions.map((opt) => (
            <Option key={opt} value={opt}>{opt}</Option>
          ))}
        </Select>

        {/* carrier 下拉（出参显示，入参隐藏） */}
        {!hideCarrier && (
          <Select
            value={effectiveCarrier}
            onChange={(val) => onUpdate(path, { carrier: val })}
            className="schema-param-carrier"
            disabled={!editable || carrierLocked}
          >
            {carrierOptions.map((opt) => (
              <Option key={opt} value={opt}>{opt}</Option>
            ))}
          </Select>
        )}

        {/* 参数描述 */}
        <Input
          value={param.description}
          onChange={(e) => onUpdate(path, { description: e.target.value })}
          placeholder="描述"
          className="schema-param-desc"
          disabled={!editable}
        />

        {/* 添加按钮：复杂类型为下拉菜单，基础类型为直接添加兄弟 */}
        {editable && (
          complex ? (
            canShowAddButton ? (
              <Dropdown
                overlay={renderAddMenu()}
                trigger={['click']}
                overlayClassName="schema-editor-v2-dropdown"
              >
                <Button type="text" className="schema-add-btn">添加</Button>
              </Dropdown>
            ) : null
          ) : canAddSibling ? (
            <Button
              type="text"
              className="schema-add-btn"
              onClick={() => onAddSibling(path)}
            >
              添加
            </Button>
          ) : null
        )}

        {/* 删除按钮 */}
        {editable && (
          <Button
            type="text"
            className="schema-delete-btn"
            onClick={() => onDelete(path)}
          >
            删除
          </Button>
        )}
      </div>

      {/* 子参数容器（仅复杂类型展示） */}
      {complex && (
        <div className="schema-children-container">
          {(param.children || []).map((child, idx) => (
            <SchemaParamItem
              key={`${param.paramName}_${path.join('_')}`}
              param={child}
              path={[...path, idx]}
              depth={depth + 1}
              editable={editable}
              hideCarrier={hideCarrier}
              // 子参数 carrier 跟随父级，整树锁定到顶层 carrier
              lockedCarrier={effectiveCarrier || lockedCarrier}
              // 子参数记录父级类型，用于限制 array 下只能存在一个子节点
              parentParamType={param.paramType}
              carrierOptions={carrierOptions}
              typeOptions={typeOptions}
              onUpdate={onUpdate}
              onDelete={onDelete}
              onAddChild={onAddChild}
              onAddSibling={onAddSibling}
            />
          ))}
        </div>
      )}
    </div>
  );
};

export default SchemaParamItem;
