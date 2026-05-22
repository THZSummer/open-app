import { useState } from 'react';
import { Card, Row, Col } from 'antd';
import { ApiOutlined, ThunderboltOutlined, NotificationOutlined } from '@ant-design/icons';
import ApiPermissionDrawer from './ApiPermissionDrawer';
import EventPermissionDrawer from './EventPermissionDrawer';
import CallbackPermissionDrawer from './CallbackPermissionDrawer';
import styles from './PermissionApply.module.less';

/**
 * 权限申请页面
 */
const PermissionApply: React.FC = () => {
  const [apiDrawerVisible, setApiDrawerVisible] = useState(false);
  const [eventDrawerVisible, setEventDrawerVisible] = useState(false);
  const [callbackDrawerVisible, setCallbackDrawerVisible] = useState(false);

  // TODO: 从状态管理或路由参数获取 appId
  const appId = '10';

  return (
    <div className={styles.container}>
      <Card title="权限申请">
        <Row gutter={[24, 24]}>
          <Col span={8}>
            <Card
              hoverable
              className={styles.card}
              onClick={() => setApiDrawerVisible(true)}
            >
              <div className={styles.cardContent}>
                <ApiOutlined className={styles.icon} />
                <h3>API 权限申请</h3>
                <p>申请 API 调用权限，审批通过后可调用对应的 API</p>
              </div>
            </Card>
          </Col>
          <Col span={8}>
            <Card
              hoverable
              className={styles.card}
              onClick={() => setEventDrawerVisible(true)}
            >
              <div className={styles.cardContent}>
                <ThunderboltOutlined className={styles.icon} />
                <h3>事件权限申请</h3>
                <p>申请事件订阅权限，审批通过后可接收事件推送</p>
              </div>
            </Card>
          </Col>
          <Col span={8}>
            <Card
              hoverable
              className={styles.card}
              onClick={() => setCallbackDrawerVisible(true)}
            >
              <div className={styles.cardContent}>
                <NotificationOutlined className={styles.icon} />
                <h3>回调权限申请</h3>
                <p>申请回调订阅权限，审批通过后可接收回调通知</p>
              </div>
            </Card>
          </Col>
        </Row>
      </Card>

      {/* API 权限申请抽屉 */}
      <ApiPermissionDrawer
        visible={apiDrawerVisible}
        appId={appId}
        onClose={() => setApiDrawerVisible(false)}
      />

      {/* 事件权限申请抽屉 */}
      <EventPermissionDrawer
        visible={eventDrawerVisible}
        appId={appId}
        onClose={() => setEventDrawerVisible(false)}
      />

      {/* 回调权限申请抽屉 */}
      <CallbackPermissionDrawer
        visible={callbackDrawerVisible}
        appId={appId}
        onClose={() => setCallbackDrawerVisible(false)}
      />
    </div>
  );
};

export default PermissionApply;
