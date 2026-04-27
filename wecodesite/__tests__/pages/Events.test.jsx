import React from 'react';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import Events from '../../src/pages/Events';
import * as thunk from '../../src/pages/Events/thunk';

jest.mock('../../src/pages/Events/thunk');

const mockEvents = [
  {
    id: '301',
    appId: '10',
    permissionId: '201',
    permission: {
      nameCn: '读取应用信息',
      scope: 'event:app:open'
    },
    event: {
      topic: 'app_open',
      docUrl: '/docs/app-open'
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
    approvalUrl: 'https://approval.example.com/event/1',
    createTime: '2026-04-20T10:00:00.000Z'
  },
  {
    id: '302',
    appId: '10',
    permissionId: '202',
    permission: {
      nameCn: '读取消息记录',
      scope: 'event:message:revoke'
    },
    event: {
      topic: 'message_revoke',
      docUrl: '/docs/message-revoke'
    },
    category: {
      id: '2',
      nameCn: '个人应用',
      path: '/2/',
      categoryPath: ['个人应用']
    },
    status: 0,
    channelType: 0,
    channelAddress: '',
    authType: 0,
    approver: {
      userId: 'user002',
      userName: '李四'
    },
    approvalUrl: 'https://approval.example.com/event/2',
    createTime: '2026-04-20T10:00:00.000Z'
  },
];

describe('Events 页面测试', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    thunk.fetchAppEvents.mockResolvedValue({
      code: '200',
      data: mockEvents,
      page: { total: 2, curPage: 1, pageSize: 10 }
    });
    thunk.subscribeEvents.mockResolvedValue({ code: '200', message: '申请已提交' });
    thunk.withdrawApproval.mockResolvedValue({ code: '200', message: '已撤回' });
    thunk.remindApproval.mockResolvedValue({ code: '200', message: '已催办' });
    thunk.deleteEvent.mockResolvedValue({ code: '200', message: '删除成功' });
  });

  test('应该渲染页面标题和描述', async () => {
    await act(async () => {
      render(<Events />);
    });

    expect(screen.getByText('事件配置')).toBeInTheDocument();
    expect(screen.getByText(/配置事件订阅/)).toBeInTheDocument();
  });

  test('应该渲染添加事件按钮', async () => {
    await act(async () => {
      render(<Events />);
    });

    const addButton = screen.getByRole('button', { name: /添加事件/ });
    expect(addButton).toBeInTheDocument();
  });

  test('应该成功加载事件列表数据', async () => {
    await act(async () => {
      render(<Events />);
    });

    await waitFor(() => {
      expect(thunk.fetchAppEvents).toHaveBeenCalledWith('test-app-id', { curPage: 1, pageSize: 10 });
    });

    expect(screen.getByText('读取应用信息')).toBeInTheDocument();
    expect(screen.getByText('读取消息记录')).toBeInTheDocument();
  });

  test('点击添加事件按钮应该打开抽屉', async () => {
    await act(async () => {
      render(<Events />);
    });

    const addButton = screen.getByRole('button', { name: /添加事件/ });
    await act(async () => {
      fireEvent.click(addButton);
    });

    expect(screen.getByText('添加事件订阅')).toBeInTheDocument();
  });

  test('点击编辑按钮应该打开编辑抽屉', async () => {
    await act(async () => {
      render(<Events />);
    });

    await waitFor(() => {
      expect(screen.getByText('读取应用信息')).toBeInTheDocument();
    });

    const editButtons = screen.getAllByRole('button', { name: /编辑/ });
    await act(async () => {
      fireEvent.click(editButtons[0]);
    });

    expect(screen.getByText('配置事件订阅')).toBeInTheDocument();
  });

  test('点击删除按钮应该显示删除确认对话框', async () => {
    await act(async () => {
      render(<Events />);
    });

    await waitFor(() => {
      expect(screen.getByText('读取应用信息')).toBeInTheDocument();
    });

    const deleteButton = screen.getByRole('button', { name: /删除/ });
    await act(async () => {
      fireEvent.click(deleteButton);
    });

    expect(screen.getByText('删除确认')).toBeInTheDocument();
  });

  test('点击撤回按钮应该调用撤回API', async () => {
    await act(async () => {
      render(<Events />);
    });

    await waitFor(() => {
      expect(screen.getByText('读取消息记录')).toBeInTheDocument();
    });

    const withdrawButton = screen.getByRole('button', { name: /撤回/ });
    await act(async () => {
      fireEvent.click(withdrawButton);
    });

    await waitFor(() => {
      expect(thunk.withdrawApproval).toHaveBeenCalledWith('302');
    });
  });

  test('点击复制审批地址应该打开模态框', async () => {
    await act(async () => {
      render(<Events />);
    });

    await waitFor(() => {
      expect(screen.getByText('读取消息记录')).toBeInTheDocument();
    });

    const copyButton = screen.getByRole('button', { name: /复制/ });
    await act(async () => {
      fireEvent.click(copyButton);
    });

    expect(screen.getByText('审批地址')).toBeInTheDocument();
  });

  test('分页器应该正确渲染', async () => {
    thunk.fetchAppEvents.mockResolvedValue({
      code: '200',
      data: mockEvents,
      page: { total: 50, curPage: 1, pageSize: 10 }
    });

    await act(async () => {
      render(<Events />);
    });

    await waitFor(() => {
      expect(screen.getByText(/共 50 条/)).toBeInTheDocument();
    });
  });

  test('加载失败应该显示错误消息', async () => {
    thunk.fetchAppEvents.mockResolvedValue({
      code: '500',
      message: '加载失败',
      data: []
    });

    await act(async () => {
      render(<Events />);
    });

    await waitFor(() => {
      expect(global.message.error).toHaveBeenCalled();
    });
  });
});
