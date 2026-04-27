import React from 'react';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import CallbackList from '../../../src/pages/Admin/Callback';
import * as thunk from '../../../src/pages/Admin/Callback/thunk';
import * as categoryThunk from '../../../src/pages/Admin/Category/thunk';

jest.mock('../../../src/pages/Admin/Callback/thunk');
jest.mock('../../../src/pages/Admin/Category/thunk');

const mockCallbackList = [
  {
    id: '1',
    nameCn: '用户登录回调',
    nameEn: 'User Login Callback',
    path: '/callback/user/login',
    method: 'POST',
    channelType: 0,
    categoryId: '1-1-1',
    categoryName: '用户管理',
    status: 2,
    permission: {
      nameCn: '用户基本信息',
      nameEn: 'User Basic Info Permission',
      scope: 'callback:user:basic',
    },
    createTime: '2026-04-01 10:00:00',
  },
  {
    id: '2',
    nameCn: '订单创建回调',
    nameEn: 'Order Created Callback',
    path: '/callback/order/create',
    method: 'POST',
    channelType: 1,
    categoryId: '1-1-2',
    categoryName: '订单管理',
    status: 1,
    permission: {
      nameCn: '订单管理权限',
      nameEn: 'Order Management Permission',
      scope: 'callback:order:create',
    },
    createTime: '2026-04-02 10:00:00',
  },
];

const mockCategoryTree = [
  {
    id: '1',
    nameCn: '用户管理',
    children: [
      { id: '1-1', nameCn: '登录相关' },
      { id: '1-2', nameCn: '用户信息' }
    ]
  }
];

describe('Admin/Callback 页面测试', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    thunk.fetchCallbackList.mockResolvedValue({
      code: '200',
      data: mockCallbackList,
      page: { total: 2, curPage: 1, pageSize: 10 }
    });
    thunk.deleteCallback.mockResolvedValue({ code: '200', message: '删除成功' });
    categoryThunk.fetchCategoryTree.mockResolvedValue({
      code: '200',
      data: mockCategoryTree
    });
  });

  test('应该渲染页面标题和描述', async () => {
    await act(async () => {
      render(<CallbackList />);
    });

    await waitFor(() => {
      expect(screen.getByText('回调管理')).toBeInTheDocument();
      expect(screen.getByText(/管理回调接口/)).toBeInTheDocument();
    });
  });

  test('应该渲染注册回调按钮', async () => {
    await act(async () => {
      render(<CallbackList />);
    });

    await waitFor(() => {
      const registerButton = screen.getByRole('button', { name: /注册回调/ });
      expect(registerButton).toBeInTheDocument();
    });
  });

  test('应该渲染搜索框', async () => {
    await act(async () => {
      render(<CallbackList />);
    });

    await waitFor(() => {
      const searchInput = screen.getByPlaceholderText(/搜索回调名称/);
      expect(searchInput).toBeInTheDocument();
    });
  });

  test('应该渲染分类选择器', async () => {
    await act(async () => {
      render(<CallbackList />);
    });

    await waitFor(() => {
      const treeSelect = screen.getByText('选择分类');
      expect(treeSelect).toBeInTheDocument();
    });
  });

  test('应该渲染状态选择器', async () => {
    await act(async () => {
      render(<CallbackList />);
    });

    await waitFor(() => {
      const statusSelect = screen.getByText('选择状态');
      expect(statusSelect).toBeInTheDocument();
    });
  });

  test('应该成功加载回调列表数据', async () => {
    await act(async () => {
      render(<CallbackList />);
    });

    await waitFor(() => {
      expect(thunk.fetchCallbackList).toHaveBeenCalled();
      expect(screen.getByText('用户登录回调')).toBeInTheDocument();
      expect(screen.getByText('订单创建回调')).toBeInTheDocument();
    });
  });

  test('点击注册回调按钮应该打开注册表单', async () => {
    await act(async () => {
      render(<CallbackList />);
    });

    await waitFor(() => {
      const registerButton = screen.getByRole('button', { name: /注册回调/ });
      fireEvent.click(registerButton);
    });

    await waitFor(() => {
      expect(screen.getByText('注册回调')).toBeInTheDocument();
    });
  });

  test('点击编辑按钮应该打开编辑表单', async () => {
    await act(async () => {
      render(<CallbackList />);
    });

    await waitFor(() => {
      expect(screen.getByText('用户登录回调')).toBeInTheDocument();
    });

    const editButtons = screen.getAllByRole('button', { name: /编辑/ });
    await act(async () => {
      fireEvent.click(editButtons[0]);
    });

    await waitFor(() => {
      expect(screen.getByText('编辑回调')).toBeInTheDocument();
    });
  });

  test('点击查看按钮应该打开查看表单', async () => {
    await act(async () => {
      render(<CallbackList />);
    });

    await waitFor(() => {
      expect(screen.getByText('用户登录回调')).toBeInTheDocument();
    });

    const viewButtons = screen.getAllByRole('button', { name: /查看/ });
    await act(async () => {
      fireEvent.click(viewButtons[0]);
    });

    await waitFor(() => {
      expect(screen.getByText('回调详情')).toBeInTheDocument();
    });
  });

  test('搜索功能应该正确工作', async () => {
    await act(async () => {
      render(<CallbackList />);
    });

    await waitFor(() => {
      expect(screen.getByText('用户登录回调')).toBeInTheDocument();
    });

    const searchInput = screen.getByPlaceholderText(/搜索回调名称/);
    await act(async () => {
      fireEvent.change(searchInput, { target: { value: '用户登录' } });
      fireEvent.click(screen.getByRole('button', { name: /搜索/ }));
    });

    await waitFor(() => {
      expect(thunk.fetchCallbackList).toHaveBeenCalledWith(expect.objectContaining({ keyword: '用户登录' }));
    });
  });

  test('分页器应该正确渲染', async () => {
    thunk.fetchCallbackList.mockResolvedValue({
      code: '200',
      data: mockCallbackList,
      page: { total: 50, curPage: 1, pageSize: 10 }
    });

    await act(async () => {
      render(<CallbackList />);
    });

    await waitFor(() => {
      expect(screen.getByText(/共 50 条/)).toBeInTheDocument();
    });
  });

  test('加载失败应该显示错误消息', async () => {
    thunk.fetchCallbackList.mockResolvedValue({
      code: '500',
      message: '加载失败',
      data: []
    });

    await act(async () => {
      render(<CallbackList />);
    });

    await waitFor(() => {
      expect(global.message.error).toHaveBeenCalled();
    });
  });
});
