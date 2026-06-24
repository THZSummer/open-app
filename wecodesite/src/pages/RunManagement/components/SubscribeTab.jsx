/**
 * ========================================
 * 运行管理 - 订阅群 Tab 组件
 * ========================================
 *
 * 包含订阅群的搜索表单、表格展示、分页控制及数据加载逻辑。
 */
import React, { useEffect, useState } from 'react';
import { Form, Input, Select, Button, Table, Pagination } from 'antd';
import { SearchOutlined, ReloadOutlined } from '@ant-design/icons';
import { getGroupSubscribe } from '../thunk';
import { SUBSCRIBE_TYPES, getSubscribeColumns } from '../constants';

const { Option } = Select;

function SubscribeTab() {
  // 订阅群表单实例
  const [subscribeForm] = Form.useForm();
  // 加载状态
  const [subscribeLoading, setSubscribeLoading] = useState(false);
  // 表格数据
  const [subscribeData, setSubscribeData] = useState([]);
  // 当前页
  const [subscribePage, setSubscribePage] = useState(1);
  // 每页条数
  const [subscribePageSize, setSubscribePageSize] = useState(10);
  // 数据总数
  const [subscribeTotal, setSubscribeTotal] = useState(0);

  /**
   * 加载订阅群列表
   * 参数对象 params 包含：
   *   filters - 可选的查询条件（不传则使用当前表单值）
   */
  const loadSubscribeData = async (params = {}) => {
    setSubscribeLoading(true);
    try {
      // 取传入的 filters，否则取表单当前值
      const filters = params.filters || subscribeForm.getFieldsValue();
      const result = await getGroupSubscribe({
        page: subscribePage,
        pageSize: subscribePageSize,
        filters,
      });
      setSubscribeData(result.data || []);
      setSubscribeTotal(result.total || 0);
    } finally {
      setSubscribeLoading(false);
    }
  };

  // 分页变化时拉取数据
  useEffect(() => {
    loadSubscribeData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [subscribePage, subscribePageSize]);

  /**
   * 订阅群搜索
   * 参数 values 为表单字段值
   */
  const handleSubscribeSearch = (values) => {
    setSubscribePage(1);
    loadSubscribeData({ filters: values });
  };

  /**
   * 订阅群重置
   */
  const handleSubscribeReset = () => {
    subscribeForm.resetFields();
    setSubscribePage(1);
    loadSubscribeData({ filters: {} });
  };

  /**
   * 订阅群分页变化
   * 参数：
   *   page - 新页码
   *   size - 每页条数
   */
  const handleSubscribePageChange = (page, size) => {
    setSubscribePage(page);
    setSubscribePageSize(size);
  };

  // 表格列定义
  const subscribeColumns = getSubscribeColumns();

  return (
    <>
      <div className="search-area">
        <Form form={subscribeForm} layout="horizontal" onFinish={handleSubscribeSearch}>
          <div className="search-form-row">
            <div className="search-form-left">
              <Form.Item name="groupId" label="群ID">
                <Input placeholder="请输入群ID" style={{ width: 180 }} />
              </Form.Item>
              <Form.Item name="subscribeUser" label="订阅账号">
                <Input placeholder="请输入订阅账号" style={{ width: 180 }} />
              </Form.Item>
              <Form.Item name="subscribeType" label="订阅方式">
                <Select placeholder="请选择" style={{ width: 180 }} allowClear>
                  {SUBSCRIBE_TYPES.map((item) => (
                    <Option key={item.value} value={item.value}>{item.label}</Option>
                  ))}
                </Select>
              </Form.Item>
            </div>
            <div className="search-form-right">
              <Button icon={<ReloadOutlined />} onClick={handleSubscribeReset}>重置</Button>
              <Button type="primary" icon={<SearchOutlined />} htmlType="submit">搜索</Button>
            </div>
          </div>
        </Form>
      </div>

      <div className="table-area">
        <Table
          columns={subscribeColumns}
          dataSource={subscribeData}
          rowKey="key"
          pagination={false}
          loading={subscribeLoading}
        />
      </div>

      <div className="pagination-area">
        <Pagination
          total={subscribeTotal}
          current={subscribePage}
          pageSize={subscribePageSize}
          pageSizeOptions={[10, 20, 50]}
          showSizeChanger
          showQuickJumper
          onChange={handleSubscribePageChange}
        />
      </div>
    </>
  );
}

export default SubscribeTab;
