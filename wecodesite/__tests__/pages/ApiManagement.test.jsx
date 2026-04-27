import React from 'react';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import ApiManagement from '../../src/pages/ApiManagement';
import * as thunk from '../../src/pages/ApiManagement/thunk';
import * as basicInfoThunk from '../../src/pages/BasicInfo/thunk';

jest.mock('../../src/pages/ApiManagement/thunk');
jest.mock('../../src/pages/BasicInfo/thunk');

const mockApis = [
  {
    id: '300',
    appId: '10',
    permissionId: '200',
    permission: {
      nameCn: '获取用户信息',
      scope: 'user.get'
    },
    api: {
      path: '/api/v1/users',
      method: 'GET',
      docUrl: '/docs/user-get'
    },
    category: {
      id: '1',
      nameCn: '用户管理',
      path: '/1/',
      categoryPath: ['用户管理']
    },
    approver: {
      userId: 'user001',
      userName: '李四'
    },
    status: 1,
    authType: 0,
    approvalUrl: 'https://approval.example.com/api/1',
    createTime: '2026-04-20T10:00:00.000Z'
  },
  {
    id: '301',
    appId: '10',
    permissionId: '201',
    permission: {
      nameCn: '发送消息',
      scope: 'message.send'
    },
    api: {
      path: '/api/v1/messages',
      method: 'POST',
      docUrl: '/docs/message-send'
    },
    category: {
      id: '2',
      nameCn: '消息推送',
      path: '/2/',
      categoryPath: ['消息推送']
    },
    approver: {
      userId: 'user002',
      userName: '王五'
    },
    status: 0,
    authType: 1,
    approvalUrl: 'https://approval.example.com/api/2',
    createTime: '2026-04-20T10:00:00.000Z'
  },
];

describe('ApiManagement 页面测试', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    thunk.fetchAppApis.mockResolvedValue({
      code: '200',
      data: mockApis,
      page: { total: 2, curPage: 1, pageSize: 10 }
    });
    thunk.subscribeApis.mockResolvedValue({ code: '200', message: '申请已提交' });
    thunk.withdrawApiApplication.mockResolvedValue({ code: '200', message: '已撤回' });
    thunk.remindApproval.mockResolvedValue({ code: '200', message: '已催办' });
    basicInfoThunk.fetchAppInfo.mockResolvedValue({
      eamap: 'test-eamap'
    });
  });

  test('应该渲染页面标题和描述', async () => {
    await act(async () => {
      render(<ApiManagement />);
    });

    expect(screen.getByText('API管理')).toBeInTheDocument();
    expect(screen.getByText(/管理应用接口/)).toBeInTheDocument();
  });

  test('应该渲染添加API按钮', async () => {
    await act(async () => {
      render(<ApiManagement />);
    });

    const addButton = screen.getByRole('button', { name: /添加API/ });
    expect(addButton).toBeInTheDocument();
  });

  test('应该显示加载状态', async () => {
    thunk.fetchAppApis.mockImplementation(() => new Promise(() => {}));
    
    await act(async () => {
      render(<ApiManagement />);
    });

    const table = screen.getByRole('table');
    expect(table).toBeInTheDocument();
  });

  test('应该成功加载API列表数据', async () => {
    await act(async () => {
      render(<ApiManagement />);
    });

    await waitFor(() => {
      expect(thunk.fetchAppApis).toHaveBeenCalledWith('test-app-id', { curPage: 1, pageSize: 10 });
    });

    expect(screen.getByText('获取用户信息')).toBeInTheDocument();
    expect(screen.getByText('发送消息')).toBeInTheDocument();
  });

  test('点击添加API按钮应该打开抽屉', async () => {
    await act(async () => {
      render(<ApiManagement />);
    });

    const addButton = screen.getByRole('button', { name: /添加API/ });
    await act(async () => {
      fireEvent.click(addButton);
    });

    expect(screen.getByText('添加API权限')).toBeInTheDocument();
  });

  test('点击撤回按钮应该调用撤回API', async () => {
    await act(async () => {
      render(<ApiManagement />);
    });

    await waitFor(() => {
      expect(screen.getByText('获取用户信息')).toBeInTheDocument();
    });

    const withdrawButton = screen.getByRole('button', { name: /撤回/ });
    await act(async () => {
      fireEvent.click(withdrawButton);
    });

    await waitFor(() => {
      expect(thunk.withdrawApiApplication).toHaveBeenCalledWith('test-app-id', '300');
    });
  });

  test('点击复制审批地址应该打开模态框', async () => {
    await act(async () => {
      render(<ApiManagement />);
    });

    await waitFor(() => {
      expect(screen.getByText('获取用户信息')).toBeInTheDocument();
    });

    const copyButton = screen.getByRole('button', { name: /复制/ });
    await act(async () => {
      fireEvent.click(copyButton);
    });

    expect(screen.getByText('审批地址')).toBeInTheDocument();
  });

  test('分页器应该正确渲染', async () => {
    thunk.fetchAppApis.mockResolvedValue({
      code: '200',
      data: mockApis,
      page: { total: 50, curPage: 1, pageSize: 10 }
    });

    await act(async () => {
      render(<ApiManagement />);
    });

    await waitFor(() => {
      expect(screen.getByText('获取用户信息')).toBeInTheDocument();
    });

    expect(screen.getByText(/共 50 条/)).toBeInTheDocument();
  });

  test('分页器页码变化应该重新加载数据', async () => {
    thunk.fetchAppApis.mockResolvedValue({
      code: '200',
      data: mockApis,
      page: { total: 50, curPage: 2, pageSize: 10 }
    });

    await act(async () => {
      render(<ApiManagement />);
    });

    await waitFor(() => {
      expect(screen.getByText(/共 50 条/)).toBeInTheDocument();
    });

    const page2Button = screen.getByRole('button', { name: '2' });
    await act(async () => {
      fireEvent.click(page2Button);
    });

    await waitFor(() => {
      expect(thunk.fetchAppApis).toHaveBeenCalledWith('test-app-id', { curPage: 2, pageSize: 10 });
    });
  });

  test('加载失败应该显示错误消息', async () => {
    thunk.fetchAppApis.mockResolvedValue({
      code: '500',
      message: '加载失败',
      data: []
    });

    await act(async () => {
      render(<ApiManagement />);
    });

    await waitFor(() => {
      expect(global.message.error).toHaveBeenCalled();
    });
  });
});
