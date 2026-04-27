import React from 'react';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import ApprovalCenter from '../../../src/pages/Admin/Approval';
import * as thunk from '../../../src/pages/Admin/Approval/thunk';

jest.mock('../../../src/pages/Admin/Approval/thunk');

const mockApprovalList = [
  {
    id: '1',
    applicantName: '张三',
    businessType: 'API权限申请',
    businessData: { nameCn: '发送消息' },
    status: 0,
    createTime: '2026-04-20 10:00:00'
  },
  {
    id: '2',
    applicantName: '李四',
    businessType: '回调权限申请',
    businessData: { nameCn: '订单回调' },
    status: 0,
    createTime: '2026-04-20 11:00:00'
  }
];

const mockMyApprovals = [
  {
    id: '3',
    applicantName: '王五',
    businessType: '事件权限申请',
    businessData: { nameCn: '会议开始' },
    status: 1,
    createTime: '2026-04-19 10:00:00'
  }
];

const mockApprovalDetail = {
  id: '1',
  applicantName: '张三',
  businessType: 'API权限申请',
  businessData: { nameCn: '发送消息' },
  status: 0,
  createTime: '2026-04-20 10:00:00',
  combinedNodes: [
    { userId: 'user001', userName: '审批人1', level: 1, order: 1, status: 1, approveTime: '2026-04-20 11:00:00' },
    { userId: 'user002', userName: '审批人2', level: 2, order: 2, status: null }
  ],
  logs: [
    { operatorName: '审批人1', action: 0, level: 1, createTime: '2026-04-20 11:00:00', comment: '同意' }
  ]
};

describe('Admin/Approval 页面测试', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    thunk.fetchApprovalList.mockResolvedValue({
      code: '200',
      data: mockApprovalList,
      page: { total: 2, curPage: 1, pageSize: 10 }
    });
    thunk.fetchMyApprovals.mockResolvedValue({
      code: '200',
      data: mockMyApprovals,
      page: { total: 1, curPage: 1, pageSize: 10 }
    });
    thunk.fetchApprovalDetail.mockResolvedValue({
      code: '200',
      data: mockApprovalDetail
    });
    thunk.approveApplication.mockResolvedValue({ code: '200', message: '审批通过' });
    thunk.rejectApplication.mockResolvedValue({ code: '200', message: '审批已拒绝' });
  });

  test('应该渲染页面标题和描述', async () => {
    await act(async () => {
      render(<ApprovalCenter />);
    });

    await waitFor(() => {
      expect(screen.getByText('审批中心')).toBeInTheDocument();
      expect(screen.getByText(/审批权限申请/)).toBeInTheDocument();
    });
  });

  test('应该渲染Tabs', async () => {
    await act(async () => {
      render(<ApprovalCenter />);
    });

    await waitFor(() => {
      expect(screen.getByText('我的待审')).toBeInTheDocument();
      expect(screen.getByText('我发起的')).toBeInTheDocument();
      expect(screen.getByText('全部')).toBeInTheDocument();
      expect(screen.getByText('审批流程配置')).toBeInTheDocument();
    });
  });

  test('默认显示我的待审列表', async () => {
    await act(async () => {
      render(<ApprovalCenter />);
    });

    await waitFor(() => {
      expect(thunk.fetchApprovalList).toHaveBeenCalledWith(expect.objectContaining({ status: 0 }));
      expect(screen.getByText('张三')).toBeInTheDocument();
    });
  });

  test('切换到我发起的Tab', async () => {
    await act(async () => {
      render(<ApprovalCenter />);
    });

    await waitFor(() => {
      expect(screen.getByText('我的待审')).toBeInTheDocument();
    });

    await act(async () => {
      fireEvent.click(screen.getByText('我发起的'));
    });

    await waitFor(() => {
      expect(thunk.fetchMyApprovals).toHaveBeenCalled();
    });
  });

  test('切换到全部Tab', async () => {
    await act(async () => {
      render(<ApprovalCenter />);
    });

    await waitFor(() => {
      expect(screen.getByText('我的待审')).toBeInTheDocument();
    });

    await act(async () => {
      fireEvent.click(screen.getByText('全部'));
    });

    await waitFor(() => {
      expect(thunk.fetchApprovalList).toHaveBeenCalledWith(expect.not.objectContaining({ status: 0 }));
    });
  });

  test('点击审批通过按钮应该显示通过模态框', async () => {
    await act(async () => {
      render(<ApprovalCenter />);
    });

    await waitFor(() => {
      expect(screen.getByText('张三')).toBeInTheDocument();
    });

    const approveButtons = screen.getAllByRole('button', { name: /通过/ });
    await act(async () => {
      fireEvent.click(approveButtons[0]);
    });

    await waitFor(() => {
      expect(screen.getByText('审批通过')).toBeInTheDocument();
    });
  });

  test('点击驳回按钮应该显示驳回模态框', async () => {
    await act(async () => {
      render(<ApprovalCenter />);
    });

    await waitFor(() => {
      expect(screen.getByText('张三')).toBeInTheDocument();
    });

    const rejectButtons = screen.getAllByRole('button', { name: /驳回/ });
    await act(async () => {
      fireEvent.click(rejectButtons[0]);
    });

    await waitFor(() => {
      expect(screen.getByText('驳回审批意见')).toBeInTheDocument();
    });
  });

  test('点击查看详情应该显示详情模态框', async () => {
    await act(async () => {
      render(<ApprovalCenter />);
    });

    await waitFor(() => {
      expect(screen.getByText('张三')).toBeInTheDocument();
    });

    const viewButtons = screen.getAllByRole('button', { name: /查看/ });
    await act(async () => {
      fireEvent.click(viewButtons[0]);
    });

    await waitFor(() => {
      expect(screen.getByText('申请详情')).toBeInTheDocument();
    });
  });

  test('分页器应该正确渲染', async () => {
    thunk.fetchApprovalList.mockResolvedValue({
      code: '200',
      data: mockApprovalList,
      page: { total: 50, curPage: 1, pageSize: 10 }
    });

    await act(async () => {
      render(<ApprovalCenter />);
    });

    await waitFor(() => {
      expect(screen.getByText(/共 50 条/)).toBeInTheDocument();
    });
  });

  test('加载失败应该显示错误消息', async () => {
    thunk.fetchApprovalList.mockResolvedValue({
      code: '500',
      message: '加载失败',
      data: []
    });

    await act(async () => {
      render(<ApprovalCenter />);
    });

    await waitFor(() => {
      expect(global.message.error).toHaveBeenCalled();
    });
  });
});
