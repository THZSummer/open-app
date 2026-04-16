import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Typography, Form, Input, Button, Table, Pagination, Space, Radio, Row, Col, Popconfirm } from 'antd';
import { EditOutlined, PlusOutlined } from '@ant-design/icons';
import { fetchEventList, fetchSubscriptionConfig } from './thunk';
import EventDrawer from './EventDrawer';
import './Events.m.less';

const { Title, Text, Link } = Typography;

function Events() {
  const navigate = useNavigate();
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(false);
  const [isEditing, setIsEditing] = useState(false);
  const [subscriptionConfig, setSubscriptionConfig] = useState({ method: 'mqs', callbackUrl: '' });
  const [form] = Form.useForm();
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [drawerOpen, setDrawerOpen] = useState(false);

  useEffect(() => {
    const loadData = async () => {
      setLoading(true);
      const [eventsData, configData] = await Promise.all([
        fetchEventList(),
        fetchSubscriptionConfig()
      ]);
      setEvents(eventsData);
      setSubscriptionConfig(configData);
      form.setFieldsValue({
        method: configData.method,
        callbackUrl: configData.callbackUrl,
      });
      setLoading(false);
    };
    loadData();
  }, [form]);

  const handleAddEvent = (selectedEvents) => {
    const newEvents = selectedEvents.map((event, index) => ({
      ...event,
      id: Date.now() + index,
      status: 'enabled',
    }));
    setEvents([...events, ...newEvents]);
  };

  const handleOpenDrawer = () => {
    setDrawerOpen(true);
  };

  const handleCloseDrawer = () => {
    setDrawerOpen(false);
  };

  const handleEdit = () => {
    form.setFieldsValue({
      method: subscriptionConfig.method,
      callbackUrl: subscriptionConfig.callbackUrl,
    });
    setIsEditing(true);
  };

  const handleSave = () => {
    form.validateFields().then((values) => {
      const newConfig = { ...subscriptionConfig, ...values };
      setSubscriptionConfig(newConfig);
      setIsEditing(false);
    });
  };

  const handleCancel = () => {
    setIsEditing(false);
  };

  const handleDelete = (id) => {
    setEvents(events.filter((e) => e.id !== id));
  };

  const handlePageChange = (page, size) => {
    setCurrentPage(page);
    setPageSize(size);
  };

  const columns = [
    {
      title: '事件名称',
      dataIndex: 'name',
      key: 'name',
      render: (text, record) => (
        <div>
          <div>{text}</div>
          <Text type="secondary" style={{ fontSize: 12 }}>{record.event}</Text>
        </div>
      ),
    },
    {
      title: '事件类型',
      dataIndex: 'eventType',
      key: 'eventType',
    },
    {
      title: '所需权限',
      dataIndex: 'permission',
      key: 'permission',
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
        <Popconfirm
          title="确定删除此事件吗？"
          onConfirm={() => handleDelete(record.id)}
          okText="确定"
          cancelText="取消"
        >
          <Button type="link" danger>删除</Button>
        </Popconfirm>
      ),
    },
  ];

  const paginatedData = events.slice((currentPage - 1) * pageSize, currentPage * pageSize);

  return (
    <div className="events">
      <Title level={4} className="page-title">事件配置</Title>
      <Text type="secondary" className="page-desc">
        配置事件订阅和回调地址
        <Link onClick={() => navigate('/events-docs')} style={{ marginLeft: 4 }}>了解更多</Link>
      </Text>

      <div style={{ marginTop: 16 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 16, marginBottom: 16 }}>
          <Text strong style={{ fontSize: 16 }}>订阅方式</Text>
          {!isEditing ? (
            <Button icon={<EditOutlined />} onClick={handleEdit}>编辑</Button>
          ) : null}
        </div>
        <Form form={form} layout="horizontal">
          <Form.Item name="method" label="发送方式" style={{ marginBottom: 16 }}>
            <Radio.Group 
              onChange={(e) => form.setFieldsValue({ method: e.target.value })}
              disabled={!isEditing}
            >
              <Radio value="mqs">MQS平台订阅</Radio>
              <Radio value="business">将事件发送至业务系统</Radio>
            </Radio.Group>
          </Form.Item>
          <Form.Item noStyle shouldUpdate={(prev, curr) => prev.method !== curr.method}>
            {() => (
              <div>
                {form.getFieldValue('method') === 'mqs' && (
                  <Form.Item label="说明文档" style={{ marginBottom: 16 }}>
                    <Link href="https://example.com/mqs-docs" target="_blank">
                      <span style={{ color: '#000' }}>查看</span>
                      <span style={{ color: '#1890ff' }}>MQS内置消息队列使用规范</span>
                    </Link>
                  </Form.Item>
                )}
                {form.getFieldValue('method') === 'business' && (
                  <Form.Item label="请求地址" style={{ marginBottom: 16 }}>
                    {isEditing ? (
                      <Form.Item name="callbackUrl" noStyle>
                        <Input 
                          placeholder="https://your-domain.com/webhook" 
                          style={{ width: '100%' }} 
                        />
                      </Form.Item>
                    ) : (
                      <Text>{subscriptionConfig.callbackUrl || '-'}</Text>
                    )}
                  </Form.Item>
                )}
              </div>
            )}
          </Form.Item>
          {isEditing && (
            <div style={{ marginTop: 16 }}>
              <Button 
                type="primary" 
                onClick={handleSave}
                style={{ borderRadius: 7, width: 88, marginRight: 16 }}
              >
                保存
              </Button>
              <Button 
                onClick={handleCancel}
                style={{ borderRadius: 7, width: 88 }}
              >
                取消
              </Button>
            </div>
          )}
        </Form>
      </div>

      <div style={{ marginTop: 16 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
          <Text strong style={{ fontSize: 16 }}>已添加事件</Text>
          <Button icon={<PlusOutlined />} onClick={handleOpenDrawer}>添加事件</Button>
        </div>
        <Table
          columns={columns}
          dataSource={paginatedData}
          rowKey="id"
          pagination={false}
          loading={loading}
        />
        <div style={{ marginTop: 16, textAlign: 'right' }}>
          <Pagination
            total={events.length}
            current={currentPage}
            pageSize={pageSize}
            pageSizeOptions={[10, 20, 50]}
            showSizeChanger
            showQuickJumper
            showTotal={(total) => `共 ${total} 条`}
            onChange={handlePageChange}
          />
        </div>
      </div>

      <EventDrawer
        open={drawerOpen}
        onClose={handleCloseDrawer}
        onConfirm={handleAddEvent}
        selectedEvents={events}
      />
    </div>
  );
}

export default Events;
