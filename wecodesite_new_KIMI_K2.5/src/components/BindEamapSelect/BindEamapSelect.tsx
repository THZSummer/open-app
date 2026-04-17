import React from 'react';
import { Select } from 'antd';

interface BindEamapSelectProps {
  value?: string;
  onChange?: (value: string) => void;
  options: { value: string; label: string }[];
  placeholder?: string;
}

const BindEamapSelect: React.FC<BindEamapSelectProps> = ({ value, onChange, options, placeholder = '请选择应用服务' }) => {
  return <Select value={value} onChange={onChange} placeholder={placeholder} options={options} allowClear style={{ width: '100%' }} />;
};

export default BindEamapSelect;
