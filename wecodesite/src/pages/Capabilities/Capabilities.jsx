import React from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { Typography, Card, Button, Tag, Empty, Modal, message } from 'antd';
import { RobotOutlined, GlobalOutlined, AppstoreOutlined, MailOutlined, CheckOutlined, PlusOutlined } from '@ant-design/icons';
import './Capabilities.m.less';

const { Title, Text } = Typography;

const capabilityTypes = [
  { type: 'bot', name: '机器人', icon: 'RobotOutlined' },
  { type: 'web', name: '网页应用', icon: 'GlobalOutlined' },
  { type: 'miniapp', name: '小程序', icon: 'MailOutlined' },
  { type: 'widget', name: '小组件', icon: 'AppstoreOutlined' },
];

function Capabilities() {
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();
  const appId = searchParams.get('appId') || '1';
  const enabledCapabilities = searchParams.get('caps')?.split(',').filter(Boolean) || ['bot'];
  const [modalVisible, setModalVisible] = React.useState(false);
  const [selectedCapability, setSelectedCapability] = React.useState(null);

  const isEnabled = (type) => enabledCapabilities.includes(type);

  const updateCapabilities = (newCaps) => {
    const capsStr = newCaps.join(',');
    setSearchParams((prev) => {
      const newParams = new URLSearchParams(prev);
      if (capsStr) {
        newParams.set('caps', capsStr);
      } else {
        newParams.delete('caps');
      }
      return newParams;
    }, { replace: true });
  };

  const handleAddClick = (capability) => {
    setSelectedCapability(capability);
    setModalVisible(true);
  };

  const handleConfirmAdd = () => {
    if (selectedCapability && !enabledCapabilities.includes(selectedCapability.type)) {
      const newCaps = [...enabledCapabilities, selectedCapability.type];
      updateCapabilities(newCaps);
      message.success(`已成功添加${selectedCapability.name}能力`);
    }
    setModalVisible(false);
    setSelectedCapability(null);
  };

  const handleCapabilityClick = (capability) => {
    if (isEnabled(capability.type)) {
      navigate(`/capability-detail?appId=${appId}&type=${capability.type}`);
    }
  };

  return (
    <div className="capabilities">
      <div className="capabilities-header">
        <Title level={4} className="page-title">添加应用能力</Title>
        <Text type="secondary" className="page-desc">为应用开启对应的客户端能力，开启后需完成配置并发布应用才能生效</Text>
      </div>

      <div className="capabilities-content">
        <div className="capabilities-grid">
          {capabilityTypes.map((capability) => (
            <Card
              key={capability.type}
              className={`capability-card ${isEnabled(capability.type) ? 'enabled' : ''}`}
              hoverable={isEnabled(capability.type)}
              onClick={() => handleCapabilityClick(capability)}
            >
              <div className="capability-card-inner">
                <div className="capability-icon-wrapper">
                  <span className="capability-icon">
                    {capability.type === 'bot' && <RobotOutlined />}
                    {capability.type === 'web' && <GlobalOutlined />}
                    {capability.type === 'miniapp' && <MailOutlined />}
                    {capability.type === 'widget' && <AppstoreOutlined />}
                  </span>
                  {isEnabled(capability.type) && (
                    <span className="capability-check">
                      <CheckOutlined />
                    </span>
                  )}
                </div>
                <div className="capability-info">
                  <div className="capability-name-row">
                    <span className="capability-name">{capability.name}</span>
                    {capability.type === 'miniapp' && (
                      <Tag color="orange" className="not-recommended-tag">不推荐</Tag>
                    )}
                  </div>
                  <Text type="secondary" className="capability-desc">
                    {capability.type === 'bot' && '通过飞书会话与用户进行消息交互'}
                    {capability.type === 'web' && 'H5 开发，运行在飞书客户端内'}
                    {capability.type === 'miniapp' && '支持在小程序中实现复杂交互'}
                    {capability.type === 'widget' && '将应用嵌入到云文档、多维表格等飞书模块'}
                  </Text>
                  <div className="capability-status">
                    {isEnabled(capability.type) ? (
                      <Text type="success" className="enabled-text">
                        <CheckOutlined /> 已添加{capability.name}
                      </Text>
                    ) : (
                      <Button
                        type="primary"
                        ghost
                        size="small"
                        icon={<PlusOutlined />}
                        onClick={(e) => {
                          e.stopPropagation();
                          handleAddClick(capability);
                        }}
                        className="add-btn"
                      >
                        添加
                      </Button>
                    )}
                  </div>
                </div>
              </div>
            </Card>
          ))}
        </div>

        {enabledCapabilities.length === 0 && (
          <div className="empty-tips">
            <Empty
              description={
                <span className="empty-text">暂无已启用的应用能力，请点击"添加"按钮启用能力</span>
              }
            />
          </div>
        )}

        <div className="capability-notes">
          <Text type="secondary" className="notes-title">说明</Text>
          <ul className="notes-list">
            <li>应用能力开启后，需完成必填项的配置并提交版本发布申请后，才能在线上生效</li>
            <li>不同能力支持的配置项不同，请根据实际需求进行配置</li>
            <li>如需修改已开启的能力，请在对应能力详情页进行操作</li>
          </ul>
        </div>
      </div>

      <Modal
        title={`添加${selectedCapability?.name || ''}能力`}
        open={modalVisible}
        onOk={handleConfirmAdd}
        onCancel={() => setModalVisible(false)}
        okText="确认添加"
        cancelText="取消"
        className="add-capability-modal"
      >
        {selectedCapability && (
          <div className="modal-content">
            <div className="modal-capability-info">
              <span className="modal-icon">
                {selectedCapability.type === 'bot' && <RobotOutlined />}
                {selectedCapability.type === 'web' && <GlobalOutlined />}
                {selectedCapability.type === 'miniapp' && <MailOutlined />}
                {selectedCapability.type === 'widget' && <AppstoreOutlined />}
              </span>
              <div className="modal-text">
                <Text strong>{selectedCapability.name}</Text>
                <br />
                <Text type="secondary">
                  {selectedCapability.type === 'bot' && '通过飞书会话与用户进行消息交互'}
                  {selectedCapability.type === 'web' && 'H5 开发，运行在飞书客户端内'}
                  {selectedCapability.type === 'miniapp' && '支持在小程序中实现复杂交互'}
                  {selectedCapability.type === 'widget' && '将应用嵌入到云文档、多维表格等飞书模块'}
                </Text>
              </div>
            </div>
            <div className="modal-notice">
              <Text>添加能力后，你需要：</Text>
              <ul>
                <li>完成必填项的配置</li>
                <li>提交版本发布申请</li>
                <li>等待审核通过后能力才能生效</li>
              </ul>
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
}

export default Capabilities;