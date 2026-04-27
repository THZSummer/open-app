import React from 'react';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import Callbacks from '../../src/pages/Callbacks';
import * as thunk from '../../src/pages/Callbacks/thunk';

jest.mock('../../src/pages/Callbacks/thunk');

const mockCallbacks = [
  {
    id: '401',
    appId: '10',
    permissionId: '301',
    permission: {
      nameCn: '用户基本信息',
      scope: 'callback:user:login'
    },
    callback: {
      docUrl: '/docs/user-login-callback'
    },
    category: {
      id: '1',
      nameCn: '业务应用',
      path: '/1/',
      categoryPath: ['业务应用']
    },
    status: 1,
    channelType: 0,
    channelAddress: '',
    authType: 0,
    approver: {
      userId: 'user001',
      userName: '张三'
    },
    approvalUrl: 'https://approval.example.com/callback/1',
    createTime: '2026-04-20T10:00:00.000Z'
  },
  {
    id: '402',
    appId: '10',
    permissionId: '302',
    permission: {
      nameCn: '订单管理',
      scope: 'callback:order:created'
    },
    callback: {
      docUrl: '/docs/order-created-callback'
    },
    category: {
      id: '1',
      nameCn: '业务应用',
      path: '/1/',
      categoryPath: ['业务应用']
    },
    status: 0,
    channelType: 1,
    channelAddress: 'https://example.com/order-callback',
    authType: 0,
    approver: {
      userId: 'user002',
      userName: '李四'
    },
    approvalUrl: 'https://approval.example.com/callback/2',
    createTime: '2026-04-20T10:00:00.000Z'
  },
];

describe('Callbacks 页面测试', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    thunk.fetchAppCallbacks.mockResolvedValue({
      code: '200',
      data: mockCallbacks,
      page: { total: 2, curPage: 1, pageSize: 10 }
    });
    thunk.subscribeCallbacks.mockResolvedValue({ code: '200', message: '申请已提交' });
    thunk.withdrawApproval.mockResolvedValue({ code: '200', message: '已撤回' });
    thunk.remindApproval.mockResolvedValue({ code: '200', message: '已催办' });
    thunk.deleteCallback.mockResolvedValue({ code: '200', message: '删除成功' });
  });

  test('应该渲染页面标题和描述', async () => {
    await act(async () => {
      render(<Callbacks />);
    });

    expect(screen.getByText('回调配置')).toBeInTheDocument();
    expect(screen.getByText(/配置API回调地址/)).toBeInTheDocument();
  });

  test('应该渲染添加回调按钮', async () => {
    await act(async () => {
      render(<Callbacks />);
    });

    const addButton = screen.getByRole('button', { name: /添加回调/ });
    expect(addButton).toBeInTheDocument();
  });

  test('应该成功加载回调列表数据', async () => {
    await act(async () => {
      render(<Callbacks />);
    });

    await waitFor(() => {
      expect(thunk.fetchAppCallbacks).toHaveBeenCalledWith('test-app-id', { curPage: 1, pageSize: 10 });
    });

    expect(screen.getByText('用户基本信息')).toBeInTheDocument();
    expect(screen.getByText('订单管理')).toBeInTheDocument();
  });

  test('点击添加回调按钮应该打开抽屉', async () => {
    await act(async () => {
      render(<Callbacks />);
    });

    const addButton = screen.getByRole('button', { name: /添加回调/ });
    await act(async () => {
      fireEvent.click(addButton);
    });

    expect(screen.getByText('添加回调权限')).toBeInTheDocument();
  });

  test('点击编辑按钮应该打开编辑抽屉', async () => {
    await act(async () => {
      render(<Callbacks />);
    });

    await waitFor(() => {
      expect(screen.getByText('用户基本信息')).toBeInTheDocument();
    });

    const editButtons = screen.getAllByRole('button', { name: /编辑/ });
    await act(async () => {
      fireEvent.click(editButtons[0]);
    });

    expect(screen.getByText('配置回调')).toBeInTheDocument();
  });

  test('点击删除按钮应该显示删除确认对话框', async () => {
    await act(async () => {
      render(<Callbacks />);
    });

    await waitFor(() => {
      expect(screen.getByText('用户基本信息')).toBeInTheDocument();
    });

    const deleteButton = screen.getByRole('button', { name: /删除/ });
    await act(async () => {
      fireEvent.click(deleteButton);
    });

    expect(screen.getByText('删除确认')).toBeInTheDocument();
  });

  test('点击撤回按钮应该调用撤回API', async () => {
    await act(async () => {
      render(<Callbacks />);
    });

    await waitFor(() => {
      expect(screen.getByText('订单管理')).toBeInTheDocument();
    });

    const withdrawButton = screen.getByRole('button', { name: /撤回/ });
    await act(async () => {
      fireEvent.click(withdrawButton);
    });

    await waitFor(() => {
      expect(thunk.withdrawApproval).toHaveBeenCalledWith('402');
    });
  });

  test('点击复制审批地址应该打开模态框', async () => {
    await act(async () => {
      render(<Callbacks />);
    });

    await waitFor(() => {
      expect(screen.getByText('订单管理')).toBeInTheDocument();
    });

    const copyButton = screen.getByRole('button', { name: /复制/ });
    await act(async () => {
      fireEvent.click(copyButton);
    });

    expect(screen.getByText('审批地址')).toBeInTheDocument();
  });

  test('分页器应该正确渲染', async () => {
    thunk.fetchAppCallbacks.mockResolvedValue({
      code: '200',
      data: mockCallbacks,
      page: { total: 50, curPage: 1, pageSize: 10 }
    });

    await act(async () => {
      render(<Callbacks />);
    });

    await waitFor(() => {
      expect(screen.getByText(/共 50 条/)).toBeInTheDocument();
    });
  });

  test('加载失败应该显示错误消息', async () => {
    thunk.fetchAppCallbacks.mockResolvedValue({
      code: '500',
      message: '加载失败',
      data: []
    });

    await act(async () => {
      render(<Callbacks />);
    });

    await waitFor(() => {
      expect(global.message.error).toHaveBeenCalled();
    });
  });
});
