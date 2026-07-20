import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import { Layout as AntLayout, Menu, Button, Avatar, Dropdown, Space } from 'antd';
import {
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  AppstoreOutlined,
  BookOutlined,
  FileTextOutlined,
  AuditOutlined,
  AppstoreAddOutlined,
  UserOutlined,
} from '@ant-design/icons';
import useGlobalStore from '@/stores/global.store';
import less from './index.module.less';

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
    key: '/lookup-classify',
    icon: <BookOutlined />,
    label: 'LookUp管理',
  },
  {
    key: '/dictionary',
    icon: <FileTextOutlined />,
    label: '数据字典',
  },
  {
    key: '/approval',
    icon: <AuditOutlined />,
    label: '审批管理',
  },
  {
    key: '/ability-admin',
    icon: <AppstoreAddOutlined />,
    label: '能力管理',
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
      type: 'divider',
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
  const handleMenuClick = (e) => {
    navigate(e.key);
  };

  // 根据当前路径设置选中的菜单项
  const selectedKeys = [location.pathname];

  return (
    <AntLayout className={less.layout}>
      {/* 侧边栏 */}
      <Sider
        trigger={null}
        collapsible
        collapsed={collapsed}
        className={less.sider}
        width={240}
      >
        <div className={less.logo}>
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
        <Header className={less.header}>
          <Button
            type="text"
            icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
            onClick={() => setCollapsed(!collapsed)}
            className={less.trigger}
          />

          <div className={less.headerRight}>
            <Space>
              <Dropdown menu={{ items: userMenuItems }} placement="bottomRight">
                <Space className={less.userInfo}>
                  <Avatar icon={<UserOutlined />} />
                  <span>{userInfo?.userName || '未登录'}</span>
                </Space>
              </Dropdown>
            </Space>
          </div>
        </Header>

        {/* 内容区域 */}
        <Content className={less.content}>
          <Outlet />
        </Content>
      </AntLayout>
    </AntLayout>
  );
};

export default Layout;
