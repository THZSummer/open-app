/**
 * ========================================
 * 运行管理 - 连接流 Tab 组件
 * ========================================
 *
 * 字段命名对齐 plan-api.md §3.7 #49 响应：
 *   - executionId / flowNameCn / status / triggerType
 */
import React, { useEffect, useState } from 'react';
import { Form, Input, Select, Button, Table, Pagination } from 'antd';
import { SearchOutlined, ReloadOutlined } from '@ant-design/icons';
import RunDetailDrawer from './RunDetailDrawer';
import { fetchFlowRunList } from '../thunk';
import { FLOW_RUN_STATUS, getFlowRunColumns } from '../constants';

const { Option } = Select;

function FlowRunTab() {
  // 连接流表单实例
  const [flowRunForm] = Form.useForm();
  // 加载状态
  const [flowRunLoading, setFlowRunLoading] = useState(false);
  // 表格数据
  const [flowRunData, setFlowRunData] = useState([]);
  // 当前页
  const [flowRunPage, setFlowRunPage] = useState(1);
  // 每页条数
  const [flowRunPageSize, setFlowRunPageSize] = useState(10);
  // 数据总数
  const [flowRunTotal, setFlowRunTotal] = useState(0);

  // 详情抽屉是否打开
  const [drawerOpen, setDrawerOpen] = useState(false);
  // 当前查看的执行 ID
  const [currentExecutionId, setCurrentExecutionId] = useState('');

  /**
   * 加载连接流执行列表
   *
   * @param {Object} params - 参数对象
   * 包含以下字段：
   * - filters: 可选的查询条件（不传则使用当前表单值）
   */
  const loadFlowRunData = async (params = {}) => {
    setFlowRunLoading(true);
    try {
      // 取传入的 filters，否则取表单当前值
      const filters = params.filters || flowRunForm.getFieldsValue();
      const result = await fetchFlowRunList({
        page: flowRunPage,
        pageSize: flowRunPageSize,
        filters,
      });
      setFlowRunData(result.data || []);
      setFlowRunTotal(result.total || 0);
    } finally {
      setFlowRunLoading(false);
    }
  };

  // 分页变化时拉取数据
  useEffect(() => {
    loadFlowRunData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [flowRunPage, flowRunPageSize]);

  /**
   * 连接流搜索
   *
   * @param {Object} values - 表单字段值
   */
  const handleFlowRunSearch = (values) => {
    setFlowRunPage(1);
    loadFlowRunData({ filters: values });
  };

  /**
   * 连接流重置
   */
  const handleFlowRunReset = () => {
    flowRunForm.resetFields();
    setFlowRunPage(1);
    loadFlowRunData({ filters: {} });
  };

  /**
   * 连接流分页变化
   *
   * @param {number} page - 新页码
   * @param {number} size - 每页条数
   */
  const handleFlowRunPageChange = (page, size) => {
    setFlowRunPage(page);
    setFlowRunPageSize(size);
  };

  /**
   * 打开运行详情抽屉
   *
   * @param {string} executionId - 执行 ID
   */
  const handleOpenDetail = (executionId) => {
    setCurrentExecutionId(executionId);
    setDrawerOpen(true);
  };

  /**
   * 关闭运行详情抽屉
   */
  const handleCloseDetail = () => {
    setDrawerOpen(false);
  };

  // 表格列定义
  const flowRunColumns = getFlowRunColumns({ onShowDetail: handleOpenDetail });

  return (
    <>
      <div className="search-area">
        <Form form={flowRunForm} layout="horizontal" onFinish={handleFlowRunSearch}>
          <div className="search-form-row">
            <div className="search-form-left">
              <Form.Item name="executionId" label="执行ID">
                <Input placeholder="请输入执行ID" style={{ width: 180 }} />
              </Form.Item>
              <Form.Item name="flowNameCn" label="名称">
                <Input placeholder="请输入连接流名称" style={{ width: 180 }} />
              </Form.Item>
              <Form.Item name="status" label="执行状态">
                <Select placeholder="请选择" style={{ width: 180 }} allowClear>
                  {FLOW_RUN_STATUS.map((item) => (
                    <Option key={item.value} value={item.value}>{item.label}</Option>
                  ))}
                </Select>
              </Form.Item>
            </div>
            <div className="search-form-right">
              <Button icon={<ReloadOutlined />} onClick={handleFlowRunReset}>重置</Button>
              <Button type="primary" icon={<SearchOutlined />} htmlType="submit">搜索</Button>
            </div>
          </div>
        </Form>
      </div>

      <div className="table-area">
        <Table
          columns={flowRunColumns}
          dataSource={flowRunData}
          rowKey="executionId"
          pagination={false}
          loading={flowRunLoading}
        />
      </div>

      <div className="pagination-area">
        <Pagination
          total={flowRunTotal}
          current={flowRunPage}
          pageSize={flowRunPageSize}
          pageSizeOptions={[10, 20, 50]}
          showSizeChanger
          showQuickJumper
          onChange={handleFlowRunPageChange}
        />
      </div>

      {/* 运行详情抽屉 */}
      <RunDetailDrawer
        open={drawerOpen}
        executionId={currentExecutionId}
        onClose={handleCloseDetail}
      />
    </>
  );
}

export default FlowRunTab;
