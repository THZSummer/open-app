import React, { useState, useEffect } from 'react';
import { Card, Button, Table, Radio, Tag, Pagination, message } from 'antd';
import { EditOutlined, PlusOutlined, DeleteOutlined } from '@ant-design/icons';
import EventDrawer from './EventDrawer';
import { fetchEventList, fetchSubscriptionConfig, addEvents, removeEvent } from './thunk';
import type { Event } from '../../types';
import styles from './Events.module.less';

const Events: React.FC = () => {
  const [events, setEvents] = useState<Event[]>([]);
  const [loading, setLoading] = useState(false);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [total, setTotal] = useState(0);
  const [subscriptionMethod, setSubscriptionMethod] = useState('mqs');
  const [editingSubscription, setEditingSubscription] = useState(false);
  const [drawerVisible, setDrawerVisible] = useState(false);

  useEffect(() => {
    loadEvents();
    loadSubscriptionConfig();
  }, [currentPage, pageSize]);

  const loadEvents = async () => {
    setLoading(true);
    const result = await fetchEventList(currentPage, pageSize);
    setEvents(result.list);
    setTotal(result.total);
    setLoading(false);
  };

  const loadSubscriptionConfig = async () => {
    const config = await fetchSubscriptionConfig();
    setSubscriptionMethod(config.method);
  };

  const handleSaveSubscription = () => {
    setEditingSubscription(false);
    message.success('保存成功');
  };

  const handleAddEvents = async (selectedEvents: string[]) => {
    await addEvents(selectedEvents);
    setDrawerVisible(false);
    message.success(`已添加 ${selectedEvents.length} 个事件`);
    loadEvents();
  };

  const handleRemoveEvent = async (eventId: string) => {
    await removeEvent(eventId);
    message.success('删除成功');
    loadEvents();
  };

  const columns = [
    { title: '事件名称', dataIndex: 'name', key: 'name' },
    {
      title: '事件类型',
      dataIndex: 'type',
      key: 'type',
      render: (type: string) => <Tag>{type}</Tag>,
    },
    { title: '所需权限', dataIndex: 'permission', key: 'permission' },
    {
      title: '操作',
      key: 'action',
      render: (_: any, record: Event) => (
        <Button 
          type="text" 
          danger 
          icon={<DeleteOutlined />}
          onClick={() => handleRemoveEvent(record.id)}
        >
          删除
        </Button>
      ),
    },
  ];

  return (
    <div className={styles.container}>
      <h2 className={styles.title}>事件配置</h2>
      <p className={styles.desc}>配置事件订阅和回调地址</p>

      <Card className={styles.card}>
        <div className={styles.cardHeader}>
          <div className={styles.cardTitle}>订阅方式</div>
          {!editingSubscription && (
            <Button type="link" icon={<EditOutlined />} onClick={() => setEditingSubscription(true)}>
              编辑
            </Button>
          )}
        </div>
        {editingSubscription ? (
          <>
            <Radio.Group 
              value={subscriptionMethod} 
              onChange={(e) => setSubscriptionMethod(e.target.value)}
            >
              <Radio value="mqs">MQS平台订阅</Radio>
              <Radio value="business">将事件发送至业务系统</Radio>
            </Radio.Group>
            <div className={styles.actions}>
              <Button onClick={() => setEditingSubscription(false)}>取消</Button>
              <Button type="primary" onClick={handleSaveSubscription} style={{ marginLeft: 8 }}>
                保存
              </Button>
            </div>
          </>
        ) : (
          <div>
            发送方式: {subscriptionMethod === 'mqs' ? 'MQS平台订阅' : '将事件发送至业务系统'}
            <div className={styles.notice}>
              说明文档: <a href="#">查看MQS内置消息队列使用规范</a>
            </div>
          </div>
        )}
      </Card>

      <Card className={styles.card}>
        <div className={styles.cardHeader}>
          <div className={styles.cardTitle}>已添加事件</div>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => setDrawerVisible(true)}>
            添加事件
          </Button>
        </div>
        <Table
          columns={columns}
          dataSource={events}
          loading={loading}
          rowKey="id"
          pagination={false}
          size="small"
        />
        <div className={styles.pagination}>
          <Pagination
            total={total}
            current={currentPage}
            pageSize={pageSize}
            pageSizeOptions={[10, 20, 50]}
            showSizeChanger
            showQuickJumper
            showTotal={(t) => `共 ${t} 条`}
            onChange={(page, size) => {
              setCurrentPage(page);
              setPageSize(size || 10);
            }}
          />
        </div>
      </Card>

      <EventDrawer
        visible={drawerVisible}
        onClose={() => setDrawerVisible(false)}
        onConfirm={handleAddEvents}
      />
    </div>
  );
};

export default Events;
