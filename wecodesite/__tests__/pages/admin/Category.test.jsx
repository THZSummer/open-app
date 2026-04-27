import React from 'react';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import CategoryList from '../../../src/pages/Admin/Category';
import * as thunk from '../../../src/pages/Admin/Category/thunk';

jest.mock('../../../src/pages/Admin/Category/thunk');

const mockCategoryTree = [
  {
    id: '1',
    nameCn: '消息管理',
    nameEn: 'Message Management',
    categoryAlias: 'message',
    sortOrder: 1,
    children: [
      {
        id: '1-1',
        nameCn: '发送消息',
        nameEn: 'Send Message',
        children: []
      },
      {
        id: '1-2',
        nameCn: '接收消息',
        nameEn: 'Receive Message',
        children: []
      }
    ]
  },
  {
    id: '2',
    nameCn: '用户管理',
    nameEn: 'User Management',
    categoryAlias: 'user',
    sortOrder: 2,
    children: [
      {
        id: '2-1',
        nameCn: '用户信息',
        nameEn: 'User Info',
        children: []
      }
    ]
  }
];

const mockOwners = [
  { id: '1', userId: 'user001', userName: '张三' },
  { id: '2', userId: 'user002', userName: '李四' }
];

describe('Admin/Category 页面测试', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    thunk.fetchCategoryTree.mockResolvedValue({
      code: '200',
      data: mockCategoryTree
    });
    thunk.fetchCategoryOwners.mockResolvedValue({
      code: '200',
      data: mockOwners
    });
    thunk.createCategory.mockResolvedValue({ code: '200', message: '创建成功' });
    thunk.updateCategory.mockResolvedValue({ code: '200', message: '更新成功' });
    thunk.deleteCategory.mockResolvedValue({ code: '200', message: '删除成功' });
    thunk.addCategoryOwner.mockResolvedValue({ code: '200', message: '添加成功' });
    thunk.removeCategoryOwner.mockResolvedValue({ code: '200', message: '移除成功' });
  });

  test('应该渲染页面标题和描述', async () => {
    await act(async () => {
      render(<CategoryList />);
    });

    await waitFor(() => {
      expect(screen.getByText('分类管理')).toBeInTheDocument();
      expect(screen.getByText(/管理分类树结构/)).toBeInTheDocument();
    });
  });

  test('应该渲染新增一级分类按钮', async () => {
    await act(async () => {
      render(<CategoryList />);
    });

    await waitFor(() => {
      const addButton = screen.getByRole('button', { name: /新增一级分类/ });
      expect(addButton).toBeInTheDocument();
    });
  });

  test('应该渲染搜索框', async () => {
    await act(async () => {
      render(<CategoryList />);
    });

    await waitFor(() => {
      const searchInput = screen.getByPlaceholderText(/搜索分类名称/);
      expect(searchInput).toBeInTheDocument();
    });
  });

  test('应该成功加载分类树数据', async () => {
    await act(async () => {
      render(<CategoryList />);
    });

    await waitFor(() => {
      expect(thunk.fetchCategoryTree).toHaveBeenCalled();
      expect(screen.getByText('消息管理')).toBeInTheDocument();
      expect(screen.getByText('用户管理')).toBeInTheDocument();
    });
  });

  test('点击新增一级分类按钮应该打开新增模态框', async () => {
    await act(async () => {
      render(<CategoryList />);
    });

    await waitFor(() => {
      const addButton = screen.getByRole('button', { name: /新增一级分类/ });
      fireEvent.click(addButton);
    });

    await waitFor(() => {
      expect(screen.getByText('新增一级分类')).toBeInTheDocument();
    });
  });

  test('点击添加子分类应该打开新增子分类模态框', async () => {
    await act(async () => {
      render(<CategoryList />);
    });

    await waitFor(() => {
      expect(screen.getByText('消息管理')).toBeInTheDocument();
    });

    const addChildButtons = screen.getAllByRole('button', { name: /添加子分类/ });
    await act(async () => {
      fireEvent.click(addChildButtons[0]);
    });

    await waitFor(() => {
      expect(screen.getByText('新增子分类')).toBeInTheDocument();
    });
  });

  test('点击编辑分类应该打开编辑模态框', async () => {
    await act(async () => {
      render(<CategoryList />);
    });

    await waitFor(() => {
      expect(screen.getByText('消息管理')).toBeInTheDocument();
    });

    const editButtons = screen.getAllByRole('button', { name: /编辑/ });
    await act(async () => {
      fireEvent.click(editButtons[0]);
    });

    await waitFor(() => {
      expect(screen.getByText('编辑分类')).toBeInTheDocument();
    });
  });

  test('点击管理责任人应该打开责任人管理模态框', async () => {
    await act(async () => {
      render(<CategoryList />);
    });

    await waitFor(() => {
      expect(screen.getByText('消息管理')).toBeInTheDocument();
    });

    const ownerButtons = screen.getAllByRole('button', { name: /责任人/ });
    await act(async () => {
      fireEvent.click(ownerButtons[0]);
    });

    await waitFor(() => {
      expect(screen.getByText('管理责任人')).toBeInTheDocument();
      expect(screen.getByText('张三')).toBeInTheDocument();
      expect(screen.getByText('李四')).toBeInTheDocument();
    });
  });

  test('点击删除分类应该显示删除确认对话框', async () => {
    await act(async () => {
      render(<CategoryList />);
    });

    await waitFor(() => {
      expect(screen.getByText('消息管理')).toBeInTheDocument();
    });

    const deleteButtons = screen.getAllByRole('button', { name: /删除/ });
    await act(async () => {
      fireEvent.click(deleteButtons[0]);
    });

    await waitFor(() => {
      expect(screen.getByText('删除确认')).toBeInTheDocument();
    });
  });

  test('搜索功能应该正确工作', async () => {
    await act(async () => {
      render(<CategoryList />);
    });

    await waitFor(() => {
      expect(screen.getByText('消息管理')).toBeInTheDocument();
    });

    const searchInput = screen.getByPlaceholderText(/搜索分类名称/);
    await act(async () => {
      fireEvent.change(searchInput, { target: { value: '用户' } });
    });

    await waitFor(() => {
      expect(screen.getByText('用户管理')).toBeInTheDocument();
    });
  });

  test('加载失败应该显示错误消息', async () => {
    thunk.fetchCategoryTree.mockResolvedValue({
      code: '500',
      message: '加载失败',
      data: []
    });

    await act(async () => {
      render(<CategoryList />);
    });

    await waitFor(() => {
      expect(global.message.error).toHaveBeenCalled();
    });
  });
});
