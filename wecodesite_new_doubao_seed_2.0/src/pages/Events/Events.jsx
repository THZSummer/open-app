import React, { useState } from 'react';
import { Button, Radio, Input, Table, Pagination, message } from 'antd';
import { PlusOutlined, DeleteOutlined } from '@ant-design/icons';
import './Events.m.less';
import mockData from './mock';
import EventDrawer from './EventDrawer';

const Events = () => {
  const [events, setEvents] = useState(mockData.events);
  const [isDrawerVisible, setIsDrawerVisible] = useState(false);
  const [editMode, setEditMode] = useState(false);
  const [subscribeMethod, setSubscribeMethod] = useState('mqs');
  const [callbackUrl, setCallbackUrl] = useState('');

  const handleAddEvent = () => {
    setIsDrawerVisible(true);
  };

  const handleCloseDrawer = () => {
    setIsDrawerVisible(false);
  };

  const handleConfirmAddEvent = (newEvent) => {
    setEvents([...events, newEvent]);
    setIsDrawerVisible(false);
    message.success('事件添加成功');
  };

  const handleDeleteEvent = (eventId) => {
    setEvents(events.filter(event => event.id !== eventId));
    message.success('事件删除成功');
  };

  const handleEdit = () => {
    setEditMode(true);
  };

  const handleSave = () => {
    setEditMode(false);
    message.success('保存成功');
  };

  const handleCancel = () => {
    setEditMode(false);
  };

  const columns = [
    {
      title: '事件名称',
      dataIndex: 'name',
      key: 'name'
    },
    {
      title: '事件类型',
      dataIndex: 'type',
      key: 'type'
    },
    {
      title: '所需权限',
      dataIndex: 'permission',
      key: 'permission'
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
        <Button
          type="text"
          danger
          icon={<DeleteOutlined />}
          onClick={() => handleDeleteEvent(record.id)}
        >
          删除
        </Button>
      )
    }
  ];

  return (
    <div className="events">
      <h1 className="title">事件配置</h1>
      <p className="description">
        配置事件订阅和回调地址
        <a href="https://open.feishu.cn/document" target="_blank" rel="noopener noreferrer" className="helpLink">
          了解更多
        </a>
      </p>

      {/* 订阅方式 */}
      <div className="section">
        <div className="sectionHeader">
          <h2 className="sectionTitle">订阅方式</h2>
          {!editMode && (
            <Button type="link" onClick={handleEdit}>
              编辑
            </Button>
          )}
        </div>
        <div className="card">
          <Radio.Group
            value={subscribeMethod}
            disabled={!editMode}
            onChange={(e) => setSubscribeMethod(e.target.value)}
          >
            <Radio value="mqs">MQS平台订阅</Radio>
            <Radio value="callback">将事件发送至业务系统</Radio>
          </Radio.Group>
          {subscribeMethod === 'mqs' && (
            <div className="mqsInfo">
              <a href="https://open.feishu.cn/document" target="_blank" rel="noopener noreferrer">
                查看 MQS内置消息队列使用规范
              </a>
            </div>
          )}
          {subscribeMethod === 'callback' && (
            <div className="callbackInfo">
              <Input
                placeholder="请输入请求地址"
                value={callbackUrl}
                onChange={(e) => setCallbackUrl(e.target.value)}
                disabled={!editMode}
                style={{ marginTop: 16, width: 400 }}
              />
            </div>
          )}
          {editMode && (
            <div className="formActions">
              <Button onClick={handleSave}>保存</Button>
              <Button onClick={handleCancel}>取消</Button>
            </div>
          )}
        </div>
      </div>

      {/* 已添加事件 */}
      <div className="section">
        <div className="sectionHeader">
          <h2 className="sectionTitle">已添加事件</h2>
          <Button type="primary" icon={<PlusOutlined />} onClick={handleAddEvent}>
            添加事件
          </Button>
        </div>
        <div className="card">
          <Table
            columns={columns}
            dataSource={events}
            rowKey="id"
            pagination={{ pageSize: 10 }}
          />
        </div>
      </div>

      {/* 添加事件 Drawer */}
      <EventDrawer
        visible={isDrawerVisible}
        onClose={handleCloseDrawer}
        onConfirm={handleConfirmAddEvent}
      />
    </div>
  );
};

export default Events;