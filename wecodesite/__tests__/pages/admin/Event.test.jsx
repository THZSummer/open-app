import React from 'react';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import EventList from '../../../src/pages/Admin/Event';
import * as thunk from '../../../src/pages/Admin/Event/thunk';
import * as categoryThunk from '../../../src/pages/Admin/Category/thunk';

jest.mock('../../../src/pages/Admin/Event/thunk');
jest.mock('../../../src/pages/Admin/Category/thunk');

const mockEventList = [
  {
    id: '1',
    nameCn: '用户进入应用',
    nameEn: 'User Enter App',
    topic: 'app_open',
    channelType: 0,
    categoryId: '1-1-1',
    categoryName: '应用事件',
    status: 2,
    permission: {
      nameCn: '读取应用信息',
      nameEn: 'Read App Info Permission',
      scope: 'event:app:open',
    },
    createTime: '2026-04-01 10:00:00',
  },
  {
    id: '2',
    nameCn: '消息被撤回',
    nameEn: 'Message Revoked',
    topic: 'message_revoke',
    channelType: 1,
    categoryId: '1-1-2',
    categoryName: '消息事件',
    status: 1,
    permission: {
      nameCn: '读取消息记录',
      nameEn: 'Read Message Permission',
      scope: 'event:message:revoke',
    },
    createTime: '2026-04-02 10:00:00',
  },
];

const mockCategoryTree = [
  {
    id: '1',
    nameCn: '应用事件',
    children: [
      { id: '1-1', nameCn: '应用生命周期' },
      { id: '1-2', nameCn: '消息事件' }
    ]
  }
];

describe('Admin/Event 页面测试', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    thunk.fetchEventList.mockResolvedValue({
      code: '200',
      data: mockEventList,
      page: { total: 2, curPage: 1, pageSize: 10 }
    });
    thunk.deleteEvent.mockResolvedValue({ code: '200', message: '删除成功' });
    categoryThunk.fetchCategoryTree.mockResolvedValue({
      code: '200',
      data: mockCategoryTree
    });
  });

  test('应该渲染页面标题和描述', async () => {
    await act(async () => {
      render(<EventList />);
    });

    await waitFor(() => {
      expect(screen.getByText('事件管理')).toBeInTheDocument();
      expect(screen.getByText(/管理事件定义/)).toBeInTheDocument();
    });
  });

  test('应该渲染注册事件按钮', async () => {
    await act(async () => {
      render(<EventList />);
    });

    await waitFor(() => {
      const registerButton = screen.getByRole('button', { name: /注册事件/ });
      expect(registerButton).toBeInTheDocument();
    });
  });

  test('应该渲染搜索框', async () => {
    await act(async () => {
      render(<EventList />);
    });

    await waitFor(() => {
      const searchInput = screen.getByPlaceholderText(/搜索事件名称/);
      expect(searchInput).toBeInTheDocument();
    });
  });

  test('应该渲染分类选择器', async () => {
    await act(async () => {
      render(<EventList />);
    });

    await waitFor(() => {
      const treeSelect = screen.getByText('选择分类');
      expect(treeSelect).toBeInTheDocument();
    });
  });

  test('应该渲染状态选择器', async () => {
    await act(async () => {
      render(<EventList />);
    });

    await waitFor(() => {
      const statusSelect = screen.getByText('选择状态');
      expect(statusSelect).toBeInTheDocument();
    });
  });

  test('应该成功加载事件列表数据', async () => {
    await act(async () => {
      render(<EventList />);
    });

    await waitFor(() => {
      expect(thunk.fetchEventList).toHaveBeenCalled();
      expect(screen.getByText('用户进入应用')).toBeInTheDocument();
      expect(screen.getByText('消息被撤回')).toBeInTheDocument();
    });
  });

  test('点击注册事件按钮应该打开注册表单', async () => {
    await act(async () => {
      render(<EventList />);
    });

    await waitFor(() => {
      const registerButton = screen.getByRole('button', { name: /注册事件/ });
      fireEvent.click(registerButton);
    });

    await waitFor(() => {
      expect(screen.getByText('注册事件')).toBeInTheDocument();
    });
  });

  test('点击编辑按钮应该打开编辑表单', async () => {
    await act(async () => {
      render(<EventList />);
    });

    await waitFor(() => {
      expect(screen.getByText('用户进入应用')).toBeInTheDocument();
    });

    const editButtons = screen.getAllByRole('button', { name: /编辑/ });
    await act(async () => {
      fireEvent.click(editButtons[0]);
    });

    await waitFor(() => {
      expect(screen.getByText('编辑事件')).toBeInTheDocument();
    });
  });

  test('点击查看按钮应该打开查看表单', async () => {
    await act(async () => {
      render(<EventList />);
    });

    await waitFor(() => {
      expect(screen.getByText('用户进入应用')).toBeInTheDocument();
    });

    const viewButtons = screen.getAllByRole('button', { name: /查看/ });
    await act(async () => {
      fireEvent.click(viewButtons[0]);
    });

    await waitFor(() => {
      expect(screen.getByText('事件详情')).toBeInTheDocument();
    });
  });

  test('搜索功能应该正确工作', async () => {
    await act(async () => {
      render(<EventList />);
    });

    await waitFor(() => {
      expect(screen.getByText('用户进入应用')).toBeInTheDocument();
    });

    const searchInput = screen.getByPlaceholderText(/搜索事件名称/);
    await act(async () => {
      fireEvent.change(searchInput, { target: { value: '用户' } });
      fireEvent.click(screen.getByRole('button', { name: /搜索/ }));
    });

    await waitFor(() => {
      expect(thunk.fetchEventList).toHaveBeenCalledWith(expect.objectContaining({ keyword: '用户' }));
    });
  });

  test('分页器应该正确渲染', async () => {
    thunk.fetchEventList.mockResolvedValue({
      code: '200',
      data: mockEventList,
      page: { total: 50, curPage: 1, pageSize: 10 }
    });

    await act(async () => {
      render(<EventList />);
    });

    await waitFor(() => {
      expect(screen.getByText(/共 50 条/)).toBeInTheDocument();
    });
  });

  test('加载失败应该显示错误消息', async () => {
    thunk.fetchEventList.mockResolvedValue({
      code: '500',
      message: '加载失败',
      data: []
    });

    await act(async () => {
      render(<EventList />);
    });

    await waitFor(() => {
      expect(global.message.error).toHaveBeenCalled();
    });
  });
});
