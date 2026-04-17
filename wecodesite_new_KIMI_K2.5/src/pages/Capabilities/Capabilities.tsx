import React, { useState, useEffect } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { Card, Button, Modal, message } from 'antd';
import { CheckOutlined, PlusOutlined } from '@ant-design/icons';
import { fetchCapabilities, enableCapability } from './thunk';
import type { Capability } from '../../types';
import styles from './Capabilities.module.less';

const capabilityDesc: Record<string, string> = {
  bot: '通过会话与用户进行交互，支持消息推送和自动回复',
  web: 'H5 开发能力，支持在客户端内打开网页应用',
  miniapp: '支持在小程序容器中运行轻量级应用',
  widget: '将应用嵌入到工作台、聊天等场景',
};

const Capabilities: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const appId = searchParams.get('appId') || '';
  const caps = searchParams.get('caps') || '';
  const enabledCaps = caps.split(',').filter(Boolean);

  const [capabilities, setCapabilities] = useState<Capability[]>([]);
  const [loading, setLoading] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);
  const [selectedCap, setSelectedCap] = useState<Capability | null>(null);

  useEffect(() => {
    loadCapabilities();
  }, [caps]);

  const loadCapabilities = async () => {
    setLoading(true);
    const data = await fetchCapabilities(enabledCaps);
    setCapabilities(data);
    setLoading(false);
  };

  const handleEnable = (cap: Capability) => {
    if (cap.type === 'miniapp') {
      message.info('小程序能力暂不可用');
      return;
    }
    setSelectedCap(cap);
    setModalVisible(true);
  };

  const confirmEnable = async () => {
    if (!selectedCap) return;
    await enableCapability(selectedCap.type);
    const newCaps = [...enabledCaps, selectedCap.type].join(',');
    navigate(`/capabilities?appId=${appId}&caps=${newCaps}`);
    setModalVisible(false);
    message.success(`已添加${selectedCap.name}能力`);
  };

  const handleCardClick = (cap: Capability) => {
    if (cap.enabled) {
      navigate(`/capability-detail?appId=${appId}&caps=${caps}&type=${cap.type}`);
    }
  };

  return (
    <div className={styles.container}>
      <h2 className={styles.title}>添加应用能力</h2>
      <p className={styles.desc}>为应用开启对应的客户端能力，开启后需完成配置并发布应用才能生效</p>

      <div className={styles.grid}>
        {capabilities.map((cap) => (
          <Card 
            key={cap.type} 
            className={styles.card}
            onClick={() => handleCardClick(cap)}
          >
            <div className={styles.cardContent}>
              <div className={styles.iconWrapper}>
                <span className={styles.icon}>{cap.icon}</span>
              </div>
              <div className={styles.info}>
                <div className={styles.name}>
                  {cap.name}
                  {cap.type === 'miniapp' && <span className={styles.tag}>不推荐</span>}
                </div>
                <div className={styles.description}>{capabilityDesc[cap.type]}</div>
              </div>
              {cap.enabled ? (
                <div className={styles.enabled}>
                  <CheckOutlined /> 已添加
                </div>
              ) : (
                <Button 
                  type="primary" 
                  icon={<PlusOutlined />}
                  onClick={(e) => {
                    e.stopPropagation();
                    handleEnable(cap);
                  }}
                  disabled={cap.type === 'miniapp'}
                >
                  添加
                </Button>
              )}
            </div>
            {cap.enabled && (
              <div className={styles.footer} onClick={() => handleCardClick(cap)}>
                前往配置 →
              </div>
            )}
          </Card>
        ))}
      </div>

      <div className={styles.notice}>
        <h4>说明</h4>
        <ul>
          <li>应用能力开启后，需完成必填项的配置并提交版本发布申请后，才能在线上生效</li>
          <li>不同能力支持的配置项不同，请根据实际需求进行配置</li>
          <li>如需修改已开启的能力，请在对应能力详情页进行操作</li>
        </ul>
      </div>

      <Modal
        title="确认添加"
        open={modalVisible}
        onOk={confirmEnable}
        onCancel={() => setModalVisible(false)}
      >
        <p>确定要添加 {selectedCap?.name} 能力吗？</p>
      </Modal>
    </div>
  );
};

export default Capabilities;
