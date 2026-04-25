import React, { useState, useEffect } from 'react';
import {
  Card,
  Button,
  Table,
  Tag,
  Space,
  Popconfirm,
  Empty,
  Spin,
  Modal,
  Form,
  Input,
  Select,
  Divider,
  message,
  Pagination,
} from 'antd';
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  UserOutlined,
  MinusCircleOutlined,
} from '@ant-design/icons';
import {
  fetchApprovalFlowList,
  fetchApprovalFlowDetail,
  createApprovalFlow,
  updateApprovalFlow,
  deleteApprovalFlow,
} from './thunk';
import './ApprovalCenter.m.less';
import { LEVEL_MAP } from './constants';

const { Option } = Select;

// 审批类型代码映射
const APPROVAL_TYPE_MAP = {
  'global': '全局审批流程',
  'api_permission_apply': 'API权限申请审批',
  'event_permission_apply': '事件权限申请审批',
  'callback_permission_apply': '回调权限申请审批',
  'api_register': 'API注册审批',
  'event_register': '事件注册审批',
  'callback_register': '回调注册审批',
};

function ApprovalFlowConfig() {
  const [loading, setLoading] = useState(false);
  const [flowList, setFlowList] = useState([]);
  const [total, setTotal] = useState(0);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [modalVisible, setModalVisible] = useState(false);
  const [modalLoading, setModalLoading] = useState(false);
  const [editingFlow, setEditingFlow] = useState(null);
  const [form] = Form.useForm();
  const [keyword, setKeyword] = useState('');
  const [deleteModalVisible, setDeleteModalVisible] = useState(false);
  const [deleteFlowId, setDeleteFlowId] = useState(null);
  const [deleteFlowCode, setDeleteFlowCode] = useState('');
  const [deleteInputCode, setDeleteInputCode] = useState('');
  const [deleteLoading, setDeleteLoading] = useState(false);

  useEffect(() => {
    loadFlowList();
  }, []);

  const loadFlowList = async (page = currentPage, size = pageSize) => {
    setLoading(true);
    const result = await fetchApprovalFlowList({ 
      keyword,
      curPage: page,
      pageSize: size 
    });
    if (result.code === '200') {
      setFlowList(result.data);
      setTotal(result.page?.total || 0);
    }
    setLoading(false);
  };

  const handleSearch = () => {
    setCurrentPage(1);
    loadFlowList(1, pageSize);
  };

  const handlePageChange = (page, size) => {
    setCurrentPage(page);
    setPageSize(size);
    loadFlowList(page, size);
  };

  const handleCreate = () => {
    setEditingFlow(null);
    form.resetFields();
    // 设置默认节点为空
    form.setFieldsValue({
      nodes: []
    });
    setModalVisible(true);
  };

  const handleEdit = async (record) => {
    setLoading(true);
    const result = await fetchApprovalFlowDetail(record.id);
    setLoading(false);
    
    if (result.code === '200' && result.data) {
      setEditingFlow(result.data);
      form.setFieldsValue({
        nameCn: result.data.nameCn,
        nameEn: result.data.nameEn,
        code: result.data.code,
        nodes: result.data.nodes || [],
      });
      setModalVisible(true);
    } else {
      message.error('获取流程详情失败');
    }
  };

  const handleDelete = (record) => {
    setDeleteFlowId(record.id);
    setDeleteFlowCode(record.code);
    setDeleteInputCode('');
    setDeleteModalVisible(true);
  };

  const handleConfirmDelete = async () => {
    // 验证输入的代码是否正确
    if (deleteInputCode !== deleteFlowCode) {
      message.error('输入的流程代码不正确，请重新输入');
      return;
    }

    setDeleteLoading(true);
    const result = await deleteApprovalFlow(deleteFlowId);
    setDeleteLoading(false);

    if (result.code === '200') {
      message.success('删除成功');
      setDeleteModalVisible(false);
      loadFlowList();
    } else {
      message.error(result.messageZh || '删除失败');
    }
  };

  const handleModalOk = async () => {
    try {
      const values = await form.validateFields();
      setModalLoading(true);
      
      // 处理节点数据，确保 order 正确，支持空数组
      const nodes = values.nodes ? values.nodes.map((node, index) => ({
        ...node,
        order: index + 1,
        type: 'approver', // 固定为 approver
      })) : [];
      
      const data = {
        ...values,
        nodes,
      };

      let result;
      if (editingFlow) {
        // 编辑模式：不传 code
        const { code, ...updateData } = data;
        result = await updateApprovalFlow(editingFlow.id, updateData);
      } else {
        // 创建模式
        result = await createApprovalFlow(data);
      }

      setModalLoading(false);
      
      if (result.code === '200') {
        message.success(editingFlow ? '更新成功' : '创建成功');
        setModalVisible(false);
        form.resetFields();
        loadFlowList();
      } else {
        message.error(result.messageZh || '操作失败');
      }
    } catch (error) {
      setModalLoading(false);
      console.error('Validation failed:', error);
    }
  };

  const handleModalCancel = () => {
    setModalVisible(false);
    form.resetFields();
    setEditingFlow(null);
  };

  const columns = [
    {
      title: '流程ID',
      dataIndex: 'id',
      key: 'id',
      width: 80,
    },
    {
      title: '流程名称（中文）',
      dataIndex: 'nameCn',
      key: 'nameCn',
      width: 200,
    },
    {
      title: '流程名称（英文）',
      dataIndex: 'nameEn',
      key: 'nameEn',
      width: 200,
    },
    {
      title: '审批类型',
      dataIndex: 'code',
      key: 'approvalType',
      width: 120,
      render: (code) => (
        <Tag color={code === 'global' ? 'green' : 'blue'}>
          {code === 'global' ? '全局审批' : '场景审批'}
        </Tag>
      ),
    },
    {
      title: '流程代码',
      dataIndex: 'code',
      key: 'code',
      width: 180,
      render: (code) => (
        <Tag color="blue">{code}</Tag>
      ),
    },
    {
      title: '审批节点数',
      dataIndex: 'nodes',
      key: 'nodes',
      width: 100,
      render: (nodes) => nodes?.length || 0,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 80,
      render: (status) => (
        <Tag color={status === 1 ? 'green' : 'default'}>
          {status === 1 ? '启用' : '禁用'}
        </Tag>
      ),
    },
    {
      title: '操作',
      key: 'action',
      width: 150,
      render: (_, record) => (
        <Space>
          <Button
            type="link"
            size="small"
            icon={<EditOutlined />}
            onClick={() => handleEdit(record)}
          >
            编辑
          </Button>
          <Button
            type="link"
            size="small"
            danger
            icon={<DeleteOutlined />}
            onClick={() => handleDelete(record)}
          >
            删除
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <div className="approval-flow-config">
      <div className="page-header" style={{ marginBottom: 16 }}>
        <div className="page-header-left">
          <h4 className="page-title">审批流程配置</h4>
          <span className="page-desc">配置不同审批类型的审批流程模板</span>
        </div>
      </div>

      <Card>
        <div className="table-toolbar" style={{ marginBottom: 16 }}>
          <Space>
            <Input.Search
              placeholder="搜索流程名称或代码"
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              onSearch={handleSearch}
              style={{ width: 300 }}
              enterButton
            />
            <Button
              type="primary"
              icon={<PlusOutlined />}
              onClick={handleCreate}
            >
              新建流程
            </Button>
          </Space>
        </div>

        <Spin spinning={loading}>
          {flowList.length > 0 ? (
            <>
              <Table
                columns={columns}
                dataSource={flowList}
                rowKey="id"
                pagination={false}
              />
              <div style={{ marginTop: 16, textAlign: 'right' }}>
                <Pagination
                  total={total}
                  current={currentPage}
                  pageSize={pageSize}
                  pageSizeOptions={[10, 20, 50]}
                  showSizeChanger
                  showQuickJumper
                  showTotal={(total) => `共 ${total} 条`}
                  onChange={handlePageChange}
                />
              </div>
            </>
          ) : (
            <Empty description="暂无审批流程" />
          )}
        </Spin>
      </Card>

      <Modal
        title={editingFlow ? '编辑审批流程' : '新建审批流程'}
        open={modalVisible}
        onOk={handleModalOk}
        onCancel={handleModalCancel}
        confirmLoading={modalLoading}
        width={700}
        destroyOnClose
      >
        <Form
          form={form}
          layout="vertical"
          initialValues={{
            nodes: []
          }}
        >
          <Form.Item
            label="流程名称（中文）"
            name="nameCn"
            rules={[{ required: true, message: '请输入流程名称（中文）' }]}
          >
            <Input placeholder="请输入流程名称（中文）" />
          </Form.Item>

          <Form.Item
            label="流程名称（英文）"
            name="nameEn"
            rules={[{ required: true, message: '请输入流程名称（英文）' }]}
          >
            <Input placeholder="请输入流程名称（英文）" />
          </Form.Item>

          <Form.Item
            label="流程代码"
            name="code"
            rules={[
              { required: true, message: '请输入流程代码' },
              { pattern: /^[a-z_]+$/, message: '流程代码只能包含小写字母和下划线' }
            ]}
            extra="常用代码：global（全局审批）、api_register、event_register、callback_register、api_permission_apply、event_permission_apply、callback_permission_apply"
          >
            <Input 
              placeholder="请输入流程代码（如：api_register）" 
              disabled={!!editingFlow}
            />
          </Form.Item>

          <Form.Item
            label="审批类型"
          >
            <Form.Item name="code" noStyle>
              {({ getFieldValue }) => {
                const code = getFieldValue('code');
                return (
                  <Tag color={code === 'global' ? 'green' : 'blue'}>
                    {code === 'global' ? '全局审批' : '场景审批'}
                  </Tag>
                );
              }}
            </Form.Item>
          </Form.Item>

          <Divider orientation="left">审批节点配置</Divider>

          <Form.List name="nodes">
            {(fields, { add, remove }) => (
              <>
                {/* 空状态提示 */}
                {fields.length === 0 && (
                  <div style={{ 
                    padding: 24, 
                    textAlign: 'center', 
                    color: '#999',
                    border: '1px dashed #d9d9d9',
                    borderRadius: 4,
                    marginBottom: 16
                  }}>
                    <p style={{ marginBottom: 8 }}>暂无审批节点</p>
                    <p style={{ marginBottom: 0, fontSize: 12 }}>点击下方"添加审批节点"按钮添加审批人</p>
                  </div>
                )}

                {fields.map(({ key, name, ...restField }, index) => (
                  <div
                    key={key}
                    style={{
                      padding: 16,
                      marginBottom: 16,
                      border: '1px solid #d9d9d9',
                      borderRadius: 4,
                      background: '#fafafa',
                    }}
                  >
                    <div style={{ marginBottom: 8, fontWeight: 'bold' }}>
                      <UserOutlined style={{ marginRight: 8 }} />
                      审批节点 {index + 1}
                    </div>

                    <Space style={{ display: 'flex', marginBottom: 8 }} align="baseline">
                      <Form.Item
                        {...restField}
                        name={[name, 'userId']}
                        rules={[{ required: true, message: '请输入审批人ID' }]}
                        style={{ marginBottom: 0, flex: 1 }}
                      >
                        <Input placeholder="审批人ID" />
                      </Form.Item>

                      <Form.Item
                        {...restField}
                        name={[name, 'userName']}
                        rules={[{ required: true, message: '请输入审批人姓名' }]}
                        style={{ marginBottom: 0, flex: 1 }}
                      >
                        <Input placeholder="审批人姓名" />
                      </Form.Item>

                      {/* 删除按钮始终显示，允许删除所有节点 */}
                      <MinusCircleOutlined
                        onClick={() => remove(name)}
                        style={{ fontSize: 20, color: '#ff4d4f' }}
                      />
                    </Space>
                  </div>
                ))}

                <Button
                  type="dashed"
                  onClick={() => add({ type: 'approver', order: fields.length + 1 })}
                  block
                  icon={<PlusOutlined />}
                >
                  添加审批节点
                </Button>
              </>
            )}
          </Form.List>

          <div style={{ marginTop: 16, color: '#666', fontSize: 12 }}>
            <p>提示：</p>
            <ul style={{ paddingLeft: 20 }}>
              <li>审批节点按顺序执行，序号从1开始递增</li>
              <li>code='global' 为全局审批流程，其他为场景审批流程</li>
              <li>权限申请流程的资源审批节点从 permission.resource_nodes 字段获取</li>
              <li>流程代码创建后不可修改</li>
            </ul>
          </div>
        </Form>
      </Modal>

      {/* 删除确认弹窗 */}
      <Modal
        title="删除确认"
        open={deleteModalVisible}
        onCancel={() => setDeleteModalVisible(false)}
        onOk={handleConfirmDelete}
        confirmLoading={deleteLoading}
        okText="确认删除"
        okButtonProps={{ danger: true }}
      >
        <div style={{ marginBottom: 16 }}>
          <p style={{ marginBottom: 8 }}>
            您即将删除审批流程：<strong>{deleteFlowCode}</strong>
          </p>
          <p style={{ marginBottom: 16, color: '#ff4d4f' }}>
            此操作将永久删除该审批流程，无法恢复！
          </p>
          <p style={{ marginBottom: 8 }}>
            请输入流程代码 <strong style={{ color: '#1677ff' }}>{deleteFlowCode}</strong> 以确认删除：
          </p>
          <Input
            placeholder={`请输入 ${deleteFlowCode}`}
            value={deleteInputCode}
            onChange={(e) => setDeleteInputCode(e.target.value)}
            status={deleteInputCode && deleteInputCode !== deleteFlowCode ? 'error' : ''}
          />
          {deleteInputCode && deleteInputCode !== deleteFlowCode && (
            <div style={{ color: '#ff4d4f', marginTop: 4 }}>
              输入的流程代码不正确
            </div>
          )}
        </div>
      </Modal>
    </div>
  );
}

export default ApprovalFlowConfig;
