import React from 'react';
import { Select } from 'antd';

function BindEamapSelect({ value, onChange, eamapOptions, placeholder }) {
  return (
    <Select
      value={value}
      onChange={onChange}
      options={eamapOptions}
      placeholder={placeholder || '请选择要绑定的EAMAP'}
      showSearch
      optionFilterProp="label"
      style={{ width: '100%' }}
    />
  );
}

export default BindEamapSelect;