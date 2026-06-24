import React from 'react';
import { Select } from 'antd';

import './BindEamapSelect.m.less';

/**
 * 绑定应用服务下拉选择组件
 *
 * @param {Object} props - 组件属性
 * @param {string} props.value - 当前选中值
 * @param {Function} props.onChange - 选择变化回调
 * @param {Array} props.eamapOptions - EAMAP 选项列表
 * @param {string} props.placeholder - 占位文字
 */
function BindEamapSelect(props) {
  const { value, onChange, eamapOptions, placeholder } = props;

  // 后端返回 { eamapAppCode, eamapAppName }，映射为 Ant Design Select 的 { value, label }
  // 展示格式: 名称 code（如 "审批中心 eamap_approval_003"）
  const options = (eamapOptions || []).map(opt => ({
    value: opt.eamapAppCode,
    label: opt.eamapAppName ? `${opt.eamapAppName} ${opt.eamapAppCode}` : opt.eamapAppCode,
  }));

  return (
    <Select
      value={value}
      onChange={onChange}
      options={options}
      placeholder={placeholder || '选择应用服务'}
      showSearch
      optionFilterProp="label"
      className="eamap-select-full"
    />
  );
}

export default BindEamapSelect;