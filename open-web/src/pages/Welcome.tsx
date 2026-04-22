import { Typography, Card, Row, Col, Statistic } from 'antd';
import {
  ApiOutlined,
  ThunderboltOutlined,
  NotificationOutlined,
  TeamOutlined,
} from '@ant-design/icons';

const { Title, Paragraph } = Typography;

/**
 * 欢迎页面
 * 
 * 显示系统概览和统计数据
 */
const Welcome = () => {
  return (
    <div>
      <Title level={2}>欢迎使用能力开放平台</Title>
      <Paragraph>
        能力开放平台提供统一的 API、事件、回调资源管理和权限申请能力，
        帮助企业构建开放的生态体系。
      </Paragraph>

      <Row gutter={16} style={{ marginTop: 24 }}>
        <Col span={6}>
          <Card>
            <Statistic
              title="API 数量"
              value={0}
              prefix={<ApiOutlined />}
              valueStyle={{ color: '#3f8600' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="事件数量"
              value={0}
              prefix={<ThunderboltOutlined />}
              valueStyle={{ color: '#1890ff' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="回调数量"
              value={0}
              prefix={<NotificationOutlined />}
              valueStyle={{ color: '#722ed1' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="应用数量"
              value={0}
              prefix={<TeamOutlined />}
              valueStyle={{ color: '#cf1322' }}
            />
          </Card>
        </Col>
      </Row>

      <Card style={{ marginTop: 24 }}>
        <Title level={4}>快速开始</Title>
        <Paragraph>
          <ul>
            <li>📝 <a>注册 API 资源</a> - 将您的 API 能力开放给其他应用</li>
            <li>⚡ <a>注册事件资源</a> - 发布事件供其他应用订阅</li>
            <li>🔔 <a>注册回调资源</a> - 提供回调能力</li>
            <li>🔑 <a>申请权限</a> - 申请使用其他应用的开放能力</li>
          </ul>
        </Paragraph>
      </Card>
    </div>
  );
};

export default Welcome;
