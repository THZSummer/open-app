import React, { useState, useEffect } from 'react';
import { Button, Modal, message } from 'antd';
import { useLocation, useNavigate } from 'react-router-dom';
import './Capabilities.m.less';

const Capabilities = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const searchParams = new URLSearchParams(location.search);
  const appId = searchParams.get('appId');
  const capsParam = searchParams.get('caps') || '';
  const [addedCaps, setAddedCaps] = useState(capsParam.split(',').filter(cap => cap));
  const [isModalVisible, setIsModalVisible] = useState(false);
  const [selectedCap, setSelectedCap] = useState(null);

  // 能力配置
  const capabilities = [
    {
      type: 'bot',
      name: '机器人',
      icon: '🤖',
      description: '通过飞书会话与用户进行消息交互',
      added: addedCaps.includes('bot')
    },
    {
      type: 'web',
      name: '网页应用',
      icon: '🌐',
      description: 'H5开发，运行在飞书客户端内',
      added: addedCaps.includes('web')
    },
    {
      type: 'miniapp',
      name: '小程序[不推荐]',
      icon: '📧',
      description: '支持在小程序中实现复杂交互',
      added: addedCaps.includes('miniapp')
    },
    {
      type: 'widget',
      name: '小组件',
      icon: '📱',
      description: '将应用嵌入到云文档等飞书模块',
      added: addedCaps.includes('widget')
    }
  ];

  useEffect(() => {
    // 更新URL参数
    const newSearchParams = new URLSearchParams();
    if (appId) newSearchParams.append('appId', appId);
    if (addedCaps.length > 0) newSearchParams.append('caps', addedCaps.join(','));
    const newSearch = newSearchParams.toString() ? `?${newSearchParams.toString()}` : '';
    navigate(`${location.pathname}${newSearch}`, { replace: true });
  }, [addedCaps, appId, location.pathname, navigate]);

  const handleAddCapability = (cap) => {
    setSelectedCap(cap);
    setIsModalVisible(true);
  };

  const handleModalOk = () => {
    if (selectedCap && !addedCaps.includes(selectedCap.type)) {
      setAddedCaps([...addedCaps, selectedCap.type]);
      message.success(`已添加${selectedCap.name}`);
    }
    setIsModalVisible(false);
    setSelectedCap(null);
  };

  const handleModalCancel = () => {
    setIsModalVisible(false);
    setSelectedCap(null);
  };

  const handleCapClick = (cap) => {
    if (cap.added) {
      // 跳转到能力详情页
      const params = new URLSearchParams();
      if (appId) params.append('appId', appId);
      if (addedCaps.length > 0) params.append('caps', addedCaps.join(','));
      params.append('type', cap.type);
      navigate(`/capability-detail?${params.toString()}`);
    } else {
      handleAddCapability(cap);
    }
  };

  return (
    <div className="capabilities">
      <h1 className="title">添加应用能力</h1>
      <p className="description">为应用开启对应的客户端能力，开启后需完成配置并发布应用才能生效</p>
      <div className="capGrid">
        {capabilities.map((cap) => (
          <div
            key={cap.type}
            className={`capCard ${cap.added ? 'added' : ''}`}
            onClick={() => handleCapClick(cap)}
          >
            <div className="capIcon">{cap.icon}</div>
            <div className="capName">{cap.name}</div>
            <div className="capDescription">{cap.description}</div>
            {cap.added ? (
              <div className="capStatus">✓ 已添加</div>
            ) : (
              <Button type="primary" size="small" className="addButton">
                添加
              </Button>
            )}
          </div>
        ))}
      </div>
      <div className="note">
        <h3 className="noteTitle">说明</h3>
        <ul className="noteList">
          <li>应用能力开启后，需完成必填项的配置并提交版本发布申请...</li>
          <li>不同能力支持的配置项不同...</li>
          <li>如需修改已开启的能力，请在对应能力详情页进行操作</li>
        </ul>
      </div>

      {/* 添加能力确认 Modal */}
      <Modal
        title={`添加${selectedCap?.name}`}
        open={isModalVisible}
        onOk={handleModalOk}
        onCancel={handleModalCancel}
        okText="确认添加"
        cancelText="取消"
      >
        <p>确定要为应用添加{selectedCap?.name}能力吗？</p>
        <p className="modalNote">添加后需要在能力详情页完成配置。</p>
      </Modal>
    </div>
  );
};

export default Capabilities;