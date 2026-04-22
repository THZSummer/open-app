import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import { Layout as AntLayout, Menu, Button, Avatar, Dropdown, Space } from 'antd';
import {
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  AppstoreOutlined,
  ApiOutlined,
  ThunderboltOutlined,
  NotificationOutlined,
  SafetyOutlined,
  CheckSquareOutlined,
  UserOutlined,
} from '@ant-design/icons';
import useGlobalStore from '@/stores/global.store';
import styles from './index.module.less';

const { Header, Sider, Content } = AntLayout;

/**
 * 菜单项配置
 */
const menuItems = [
  {
    key: '/',
    icon: <AppstoreOutlined />,
    label: '首页',
  },
  {
    key: '/categories',
    icon: <AppstoreOutlined />,
    label: '分类管理',
  },
  {
    key: '/apis',
    icon: <ApiOutlined />,
    label: 'API 管理',
  },
  {
    key: '/events',
    icon: <ThunderboltOutlined />,
    label: '事件管理',
  },
  {
    key: '/callbacks',
    icon: <NotificationOutlined />,
    label: '回调管理',
  },
  {
    key: '/permissions',
    icon: <SafetyOutlined />,
    label: '权限申请',
  },
  {
    key: '/approvals',
    icon: <CheckSquareOutlined />,
    label: '审批中心',
  },
];

/**
 * 布局组件
 * 
 * 包含：
 * - 侧边栏导航
 * - 顶部导航栏
 * - 内容区域
 */
const Layout = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { collapsed, setCollapsed, userInfo, logout } = useGlobalStore();

  /**
   * 用户下拉菜单
   */
  const userMenuItems = [
    {
      key: 'profile',
      label: '个人信息',
    },
    {
      key: 'settings',
      label: '设置',
    },
    {
      type: 'divider' as const,
    },
    {
      key: 'logout',
      label: '退出登录',
      onClick: logout,
    },
  ];

  /**
   * 菜单点击处理
   */
  const handleMenuClick = ({ key }: { key: string }) => {
    navigate(key);
  };

  // 根据当前路径设置选中的菜单项
  const selectedKeys = [location.pathname];

  return (
    <AntLayout className={styles.layout}>
      {/* 侧边栏 */}
      <Sider
        trigger={null}
        collapsible
        collapsed={collapsed}
        className={styles.sider}
        width={240}
      >
        <div className={styles.logo}>
          <img src="/vite.svg" alt="Logo" />
          {!collapsed && <span>能力开放平台</span>}
        </div>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={selectedKeys}
          items={menuItems}
          onClick={handleMenuClick}
        />
      </Sider>

      <AntLayout>
        {/* 顶部导航栏 */}
        <Header className={styles.header}>
          <Button
            type="text"
            icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
            onClick={() => setCollapsed(!collapsed)}
            className={styles.trigger}
          />

          <div className={styles.headerRight}>
            <Space>
              <Dropdown menu={{ items: userMenuItems }} placement="bottomRight">
                <Space className={styles.userInfo}>
                  <Avatar icon={<UserOutlined />} />
                  <span>{userInfo?.userName || '未登录'}</span>
                </Space>
              </Dropdown>
            </Space>
          </div>
        </Header>

        {/* 内容区域 */}
        <Content className={styles.content}>
          <Outlet />
        </Content>
      </AntLayout>
    </AntLayout>
  );
};

export default Layout;
