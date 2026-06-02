/**
 * ========================================
 * 连接器和连接流搜索表单组件
 * ========================================
 *
 * 功能：
 * - 提供统一的搜索表单
 * - 支持关键词筛选
 * - 搜索按钮触发搜索，重置按钮清空条件
 */

import React from 'react';
import { Form, Input, Button } from 'antd';
import { SearchOutlined, ReloadOutlined } from '@ant-design/icons';
import './ConnectorSearchForm.less';

/**
 * 连接器/连接流搜索表单组件
 *
 * @param {Object} props - 组件属性
 * @param {string} props.keyword - 关键词值
 * @param {Function} props.onSearch - 搜索回调，接收表单值对象
 * @param {string} [props.placeholder='搜索名称'] - 搜索框占位符
 */
const ConnectorSearchForm = ({
  keyword,
  onSearch,
  placeholder = '搜索名称',
}) => {
  const [form] = Form.useForm();

  /**
   * 处理搜索按钮点击
   */
  const handleSearch = () => {
    const values = form.getFieldsValue();
    if (onSearch) {
      onSearch(values);
    }
  };

  /**
   * 处理重置按钮点击
   */
  const handleReset = () => {
    form.resetFields();
    if (onSearch) {
      onSearch({});
    }
  };

  return (
    <div className="connector-search-form">
      <Form
        form={form}
        layout="inline"
        className="search-form-inline"
      >
        <div className="form-items-left">
          <Form.Item name="keyword" className="form-item-keyword">
            <Input
              placeholder={placeholder}
              defaultValue={keyword}
              style={{ width: 300 }}
              allowClear
            />
          </Form.Item>
        </div>

        <Form.Item className="form-item-buttons">
          <Button
            type="primary"
            icon={<SearchOutlined />}
            onClick={handleSearch}
          >
            搜索
          </Button>
          <Button
            icon={<ReloadOutlined />}
            onClick={handleReset}
            style={{ marginLeft: 8 }}
          >
            重置
          </Button>
        </Form.Item>
      </Form>
    </div>
  );
};

export default ConnectorSearchForm;
