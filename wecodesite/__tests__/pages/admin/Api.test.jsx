import React from 'react';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import ApiList from '../../../src/pages/Admin/Api';
import * as thunk from '../../../src/pages/Admin/Api/thunk';
import * as categoryThunk from '../../../src/pages/Admin/Category/thunk';

jest.mock('../../../src/pages/Admin/Api/thunk');
jest.mock('../../../src/pages/Admin/Category/thunk');

const mockApiList = [
  {
    id: '1',
    nameCn: '发送消息',
    nameEn: 'Send Message',
    path: '/im/send',
    method: 'POST',
    authType: 1,
    categoryId: '1-1-1',
    categoryName: '发送消息',
    status: 2,
    permission: {
      nameCn: '发送消息权限',
      nameEn: 'Send Message Permission',
      scope: 'api:im:send-message',
    },
    createTime: '2026-04-01 10:00:00',
  },
  {
    id: '2',
    nameCn: '获取用户信息',
    nameEn: 'Get User Info',
    path: '/user/info',
    method: 'GET',
    authType: 5,
    categoryId: '1-1-2',
    categoryName: '用户信息',
    status: 1,
    permission: {
      nameCn: '用户信息权限',
      nameEn: 'User Info Permission',
      scope: 'api:user:info',
    },
    createTime: '2026-04-02 10:00:00',
  },
];

const mockCategoryTree = [
  {
    id: '1',
    nameCn: '消息管理',
    children: [
      { id: '1-1', nameCn: '发送消息' },
      { id: '1-2', nameCn: '用户信息' }
    ]
  }
];

describe('Admin/Api 页面测试', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    thunk.fetchApiList.mockResolvedValue({
      code: '200',
      data: mockApiList,
      page: { total: 2, curPage: 1, pageSize: 10 }
    });
    thunk.deleteApi.mockResolvedValue({ code: '200', message: '删除成功' });
    categoryThunk.fetchCategoryTree.mockResolvedValue({
      code: '200',
      data: mockCategoryTree
    });
  });

  test('应该渲染页面标题和描述', async () => {
    await act(async () => {
      render(<ApiList />);
    });

    await waitFor(() => {
      expect(screen.getByText('API管理')).toBeInTheDocument();
      expect(screen.getByText(/管理API接口/)).toBeInTheDocument();
    });
  });

  test('应该渲染注册API按钮', async () => {
    await act(async () => {
      render(<ApiList />);
    });

    await waitFor(() => {
      const registerButton = screen.getByRole('button', { name: /注册API/ });
      expect(registerButton).toBeInTheDocument();
    });
  });

  test('应该渲染搜索框', async () => {
    await act(async () => {
      render(<ApiList />);
    });

    await waitFor(() => {
      const searchInput = screen.getByPlaceholderText(/搜索API名称/);
      expect(searchInput).toBeInTheDocument();
    });
  });

  test('应该渲染分类选择器', async () => {
    await act(async () => {
      render(<ApiList />);
    });

    await waitFor(() => {
      const treeSelect = screen.getByText('选择分类');
      expect(treeSelect).toBeInTheDocument();
    });
  });

  test('应该渲染状态选择器', async () => {
    await act(async () => {
      render(<ApiList />);
    });

    await waitFor(() => {
      const statusSelect = screen.getByText('选择状态');
      expect(statusSelect).toBeInTheDocument();
    });
  });

  test('应该成功加载API列表数据', async () => {
    await act(async () => {
      render(<ApiList />);
    });

    await waitFor(() => {
      expect(thunk.fetchApiList).toHaveBeenCalled();
      expect(screen.getByText('发送消息')).toBeInTheDocument();
      expect(screen.getByText('获取用户信息')).toBeInTheDocument();
    });
  });

  test('点击注册API按钮应该打开注册表单', async () => {
    await act(async () => {
      render(<ApiList />);
    });

    await waitFor(() => {
      const registerButton = screen.getByRole('button', { name: /注册API/ });
      fireEvent.click(registerButton);
    });

    await waitFor(() => {
      expect(screen.getByText('注册API')).toBeInTheDocument();
    });
  });

  test('点击编辑按钮应该打开编辑表单', async () => {
    await act(async () => {
      render(<ApiList />);
    });

    await waitFor(() => {
      expect(screen.getByText('发送消息')).toBeInTheDocument();
    });

    const editButtons = screen.getAllByRole('button', { name: /编辑/ });
    await act(async () => {
      fireEvent.click(editButtons[0]);
    });

    await waitFor(() => {
      expect(screen.getByText('编辑API')).toBeInTheDocument();
    });
  });

  test('点击查看按钮应该打开查看表单', async () => {
    await act(async () => {
      render(<ApiList />);
    });

    await waitFor(() => {
      expect(screen.getByText('发送消息')).toBeInTheDocument();
    });

    const viewButtons = screen.getAllByRole('button', { name: /查看/ });
    await act(async () => {
      fireEvent.click(viewButtons[0]);
    });

    await waitFor(() => {
      expect(screen.getByText('API详情')).toBeInTheDocument();
    });
  });

  test('点击删除按钮应该显示确认', async () => {
    await act(async () => {
      render(<ApiList />);
    });

    await waitFor(() => {
      expect(screen.getAllByText('发送消息').length).toBeGreaterThan(0);
    });

    const deleteButtons = screen.getAllByRole('button', { name: /删除/ });
    await act(async () => {
      fireEvent.click(deleteButtons[0]);
    });

    await waitFor(() => {
      expect(screen.getByText('确认删除')).toBeInTheDocument();
    });
  });

  test('搜索功能应该正确工作', async () => {
    await act(async () => {
      render(<ApiList />);
    });

    await waitFor(() => {
      expect(screen.getAllByText('发送消息').length).toBeGreaterThan(0);
    });

    const searchInput = screen.getByPlaceholderText(/搜索API名称/);
    await act(async () => {
      fireEvent.change(searchInput, { target: { value: '发送消息' } });
      fireEvent.click(screen.getByRole('button', { name: /搜索/ }));
    });

    await waitFor(() => {
      expect(thunk.fetchApiList).toHaveBeenCalledWith(expect.objectContaining({ keyword: '发送消息' }));
    });
  });

  test('分页器应该正确渲染', async () => {
    thunk.fetchApiList.mockResolvedValue({
      code: '200',
      data: mockApiList,
      page: { total: 50, curPage: 1, pageSize: 10 }
    });

    await act(async () => {
      render(<ApiList />);
    });

    await waitFor(() => {
      expect(screen.getByText(/共 50 条/)).toBeInTheDocument();
    });
  });

  test('加载失败应该显示错误消息', async () => {
    thunk.fetchApiList.mockResolvedValue({
      code: '500',
      message: '加载失败',
      data: []
    });

    await act(async () => {
      render(<ApiList />);
    });

    await waitFor(() => {
      expect(global.message.error).toHaveBeenCalled();
    });
  });
});
