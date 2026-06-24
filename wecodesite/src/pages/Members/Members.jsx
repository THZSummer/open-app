import React, { useState, useEffect } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { Table, Button, Tag, Modal, Radio, Select, message, Spin, Tooltip, Pagination } from 'antd';
import { PlusOutlined, DeleteOutlined, SwapOutlined } from '@ant-design/icons';
import DeleteConfirmModal from '../../components/DeleteConfirmModal/DeleteConfirmModal';
import { useAppDetail } from '../../contexts/AppContext';
import { useRoleGuard } from '../../hooks/useRoleGuard';
import { fetchMemberList, searchUsers, addMembers, deleteMember, transferOwner } from './thunk';
import { ROLE_MAP, INIT_PAGECONFIG, PAGE_SIZE_OPTIONS } from '../../utils/constants';
import { debounce } from '../../utils/common';

import './Members.m.less';

const { Option } = Select;

/**
 * 成员管理页
 */
function Members() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { appDetail } = useAppDetail();
  const appId = searchParams.get('appId');

  // 页面级权限守卫：不是成员则跳回列表
  const { role, loading: roleLoading } = useRoleGuard(appId);

  const [members, setMembers] = useState([]);
  const [pagination, setPagination] = useState(INIT_PAGECONFIG);
  const [loading, setLoading] = useState(false);
  const [appData, setAppData] = useState(null);

  // 添加成员弹窗
  const [addModalVisible, setAddModalVisible] = useState(false);
  const [searchValue, setSearchValue] = useState('');
  const [searchResults, setSearchResults] = useState([]);
  const [searchFetching, setSearchFetching] = useState(false);
  const [selectedUsers, setSelectedUsers] = useState([]);
  const [selectedRole, setSelectedRole] = useState(null);

  // 转移 Owner 弹窗
  const [transferModalVisible, setTransferModalVisible] = useState(false);
  const [transferTarget, setTransferTarget] = useState(null);
  const [transferSearchValue, setTransferSearchValue] = useState('');
  const [transferSearchResults, setTransferSearchResults] = useState([]);
  const [transferSearchFetching, setTransferSearchFetching] = useState(false);
  const [transferConfirmVisible, setTransferConfirmVisible] = useState(false);

  // 删除确认弹窗
  const [deleteModalVisible, setDeleteModalVisible] = useState(false);
  const [deletingMember, setDeletingMember] = useState(null);

  // 合并搜索结果和已选用户，确保已选用户始终有 Option 可渲染
  const allOptions = (() => {
    const map = new Map();
    [...searchResults, ...selectedUsers].forEach((u) => map.set(u.welinkId, u));
    return Array.from(map.values());
  })();

  // 权限校验通过后（role 已拿到），从 Context 拿 appDetail 加载数据
  useEffect(() => {
    if (!appId || roleLoading || role === null) return;
    if (!appDetail) return;
    if (appDetail.appType !== 1) {
      navigate(`/basic-info?appId=${appId}`);
      return;
    }
    setAppData(appDetail);
    // 根据 role 设置添加成员时的默认角色
    if (role === 1) setSelectedRole(2);
    else if (role === 2) setSelectedRole(0);
    else setSelectedRole(0);
    loadMembers();
  }, [appId, appDetail, role, roleLoading]);

  const loadMembers = async (params = { curPage: 1, pageSize: 10 }) => {
    setLoading(true);
    try {
      const membersRes = await fetchMemberList(appId, params);
      if (membersRes?.code === '200') {
        setMembers(membersRes.data || []);
        setPagination(membersRes.page ? {
          ...membersRes.page,
          total: parseInt(membersRes.page.total, 10) || 0,
        } : INIT_PAGECONFIG);
      }
    } catch (error) {
      message.error('加载成员列表失败');
    } finally {
      setLoading(false);
    }
  };

  // 分页变化
  const handlePageChange = (page, pageSize) => {
    loadMembers({ curPage: page, pageSize });
  };

  // 搜索用户（添加成员用）— 模糊搜索
  const handleSearch = debounce(async (keyword) => {
    if (!keyword || keyword.trim().length === 0) {
      setSearchResults([]);
      return;
    }
    setSearchFetching(true);
    try {
      const result = await searchUsers(appId, keyword.trim());
      if (result?.code === '200') {
        setSearchResults(result.data || []);
      } else {
        setSearchResults([]);
      }
    } catch {
      setSearchResults([]);
    } finally {
      setSearchFetching(false);
    }
  }, 300);

  // 搜索用户（转移Owner用）
  const handleTransferSearch = debounce(async (keyword) => {
    if (!keyword || keyword.trim().length === 0) {
      setTransferSearchResults([]);
      return;
    }
    setTransferSearchFetching(true);
    try {
      const result = await searchUsers(appId, keyword.trim());
      if (result?.code === '200') {
        setTransferSearchResults(result.data || []);
      } else {
        setTransferSearchResults([]);
      }
    } catch {
      setTransferSearchResults([]);
    } finally {
      setTransferSearchFetching(false);
    }
  }, 300);

  // 添加成员
  const handleAddMembers = async () => {
    if (selectedUsers.length === 0) {
      message.error('请选择要添加的成员');
      return;
    }
    const result = await addMembers(appId, {
      accountIds: selectedUsers.map((u) => u.welinkId),
      role: selectedRole,
    });
    if (result?.code === '200') {
      message.success(`已添加 ${selectedUsers.length} 位成员`);
      setAddModalVisible(false);
      setSelectedUsers([]);
      setSearchValue('');
      setSearchResults([]);
      loadMembers();
    } else {
      message.error(result?.messageZh || '添加失败');
    }
  };

  // 删除成员
  const handleDelete = () => {
    if (!deletingMember) return;
    deleteMember(appId, deletingMember.id).then((result) => {
      if (result?.code === '200') {
        message.success('已删除');
        loadMembers();
      } else {
        message.error(result?.messageZh || '删除失败');
      }
    });
    setDeleteModalVisible(false);
    setDeletingMember(null);
  };

  // 转移 Owner — 第一步：点击"转移"后弹出二次确认
  const handleTransfer = () => {
    if (!transferTarget) {
      message.error('请选择新 Owner');
      return;
    }
    setTransferConfirmVisible(true);
  };

  // 转移 Owner — 第二步：确认后执行
  const handleTransferConfirm = async () => {
    const result = await transferOwner(appId, { toAccountId: transferTarget.welinkId });
    if (result?.code === '200') {
      message.success('转移Owner成功');
      setTransferConfirmVisible(false);
      setTransferModalVisible(false);
      setTransferTarget(null);
      // 当前用户已不再是 Owner，跳转首页
      navigate('/');
    } else {
      message.error(result?.messageZh || '转移失败');
    }
  };

  // 当前用户权限
  const isOwner = role === 1;
  const isAdmin = role === 2;
  const isDeveloper = role === 0;
  const canAdd = isOwner || isAdmin;

  // 权限矩阵：判断按钮是否可执行 / 置灰 / 不展示
  // 转移按钮：Owner行才显示；Owner可执行，Admin/Developer置灰
  // 删除按钮：Owner行不显示；Owner可删Admin+Developer，Admin可删Developer，Developer置灰
  const NO_PERMISSION_TIP = '无操作权限';

  const columns = [
    { title: '姓名', dataIndex: 'memberNameCn', key: 'memberNameCn' },
    { title: '工号', dataIndex: 'w3Account', key: 'w3Account' },
    {
      title: '角色',
      dataIndex: 'memberType',
      key: 'memberType',
      render: (memberType) => (
        <Tag color={ROLE_MAP[memberType]?.color || 'default'}>{ROLE_MAP[memberType]?.text || memberType}</Tag>
      ),
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => {
        const isOwnerRow = record.memberType === 1;
        const isAdminRow = record.memberType === 2;
        const isDevRow = record.memberType === 0;

        // 转移按钮：Owner 行显示；Owner 可执行，Admin/Developer 置灰
        const showTransfer = isOwnerRow;
        const transferDisabled = !isOwner;

        // 删除按钮：Owner 行不显示；非管理权限不显示
        const showDelete = !isOwnerRow;
        let deleteDisabled = false;
        if (isDeveloper) deleteDisabled = true; // Developer 所有删除置灰
        else if (isAdmin && isAdminRow) deleteDisabled = true; // Admin 删 Admin 置灰
        else if (isAdmin && isDevRow) deleteDisabled = false; // Admin 删 Developer 可执行

        return (
          <div className="action-btns">
            {showTransfer && (
              <Tooltip title={transferDisabled ? '仅 Owner 可转移' : ''}>
                <Button
                  type="link"
                  icon={<SwapOutlined />}
                  disabled={transferDisabled}
                  onClick={() => { setTransferTarget(null); setTransferSearchValue(''); setTransferSearchResults([]); setTransferModalVisible(true); }}
                >
                  转移
                </Button>
              </Tooltip>
            )}
            {showDelete && (
              <Tooltip title={deleteDisabled ? NO_PERMISSION_TIP : ''}>
                <Button
                  type="link"
                  danger
                  icon={<DeleteOutlined />}
                  disabled={deleteDisabled}
                  onClick={() => { setDeletingMember(record); setDeleteModalVisible(true); }}
                >
                  删除
                </Button>
              </Tooltip>
            )}
          </div>
        );
      },
    },
  ];

  return (
    <div className="members-page">
      <div className="content-card">
        <div className="page-header">
          <div>
            <h2>成员管理</h2>
            <p className="page-desc">可添加企业内的人员为协作人员，并根据权限为成员设置不同角色。</p>
          </div>
          <Tooltip title={!canAdd ? '无操作权限' : ''}>
            <Button type="primary" icon={<PlusOutlined />} disabled={!canAdd} onClick={() => setAddModalVisible(true)}>
              添加成员
            </Button>
          </Tooltip>
        </div>

        <Spin spinning={loading}>
          <Table columns={columns} dataSource={members} rowKey={(r) => `${r.accountId}_${r.memberType}`} pagination={false} />
          <div style={{ marginTop: 16 }}>
            <div className="members-pagination">
              <Pagination
                current={pagination.curPage}
                pageSize={pagination.pageSize}
                total={pagination.total}
                onChange={handlePageChange}
                showSizeChanger
                pageSizeOptions={PAGE_SIZE_OPTIONS}
                showQuickJumper
                showTotal={(total) => `共 ${total} 条`}
              />
            </div>
          </div>
        </Spin>
      </div>

      {/* 添加成员弹窗 */}
      <Modal
        title="添加人员"
        open={addModalVisible}
        onOk={handleAddMembers}
        onCancel={() => { setAddModalVisible(false); setSelectedUsers([]); setSearchValue(''); setSearchResults([]); }}
      okText="确认添加"
        cancelText="取消"
      >
        <div className="add-member-modal">
          <div className="form-item">
            <label className="form-label"><span style={{ color: '#f54a45' }}>* </span>人员：</label>
            <Select
              mode="multiple"
              placeholder="请输入姓名或工号搜索成员，可同时添加多个成员"
              value={selectedUsers.map((u) => u.welinkId)}
              onSearch={(val) => { setSearchValue(val); handleSearch(val); }}
              onChange={(values) => {
                const map = new Map(allOptions.map((u) => [u.welinkId, u]));
                const newSelected = values.map((id) => map.get(id)).filter(Boolean);
                setSelectedUsers(newSelected);
              }}
              filterOption={false}
              notFoundContent={searchFetching ? '搜索中...' : (searchValue ? '未找到匹配人员' : '请输入姓名或工号搜索成员')}
              style={{ width: '100%' }}
              dropdownClassName="member-select-dropdown"
              optionLabelProp="label"
            >
              {allOptions.map((user) => (
                  <Option key={user.welinkId} value={user.welinkId} label={`${user.memberNameCn} ${user.w3Account} ${user.deptName || ''}`}>
                    <div className="member-option">
                      <span className="member-option-name">{user.memberNameCn}</span>
                      <span className="member-option-id">{user.w3Account}</span>
                      <span className="member-option-dept">{user.deptName}</span>
                    </div>
                  </Option>
                ))}
            </Select>
          </div>

          <div className="role-select">
            <label className="form-label"><span style={{ color: '#f54a45' }}>* </span>角色：</label>
            <Radio.Group value={selectedRole} onChange={(e) => setSelectedRole(e.target.value)}>
              {isOwner && <Radio value={2}>管理员</Radio>}
              <Radio value={0}>开发者</Radio>
            </Radio.Group>
            {selectedRole === 2 && (
              <div style={{ marginTop: 12, padding: '12px 16px', background: '#f7f8fa', borderRadius: 6 }}>
                <div style={{ fontSize: 13, fontWeight: 500, color: '#1f2329', marginBottom: 8 }}>管理员权限</div>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '4px 24px', fontSize: 12, color: '#646a73', lineHeight: 1.8 }}>
                  <span>· 可添加删除应用开发者</span><span>· 可以添加、删除各种开放服务</span>
                  <span>· 可以申请API权限</span><span>· 可以查看应用基本信息</span>
                  <span>· 可以查看申请后的服务</span><span>· 可以查看运营报表等</span>
                </div>
              </div>
            )}
            {selectedRole === 0 && (
              <div style={{ marginTop: 12, padding: '12px 16px', background: '#f7f8fa', borderRadius: 6 }}>
                <div style={{ fontSize: 13, fontWeight: 500, color: '#1f2329', marginBottom: 8 }}>开发权限</div>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '4px 24px', fontSize: 12, color: '#646a73', lineHeight: 1.8 }}>
                  <span>· 可以查看应用基本信息</span><span>· 可以添加、删除各种服务</span>
                  <span>· 可以申请API权限</span><span>· 可以查看申请后的服务</span>
                  <span>· 可以查看运营报表等</span>
                </div>
              </div>
            )}
          </div>
        </div>
      </Modal>

      {/* 转移 Owner 弹窗 — 第一步：选择新 Owner */}
      <Modal
        title="转移 Owner"
        open={transferModalVisible}
        onOk={handleTransfer}
        okText="确认"
        cancelText="取消"
        onCancel={() => { setTransferModalVisible(false); setTransferTarget(null); }}
      >
        <div className="transfer-modal">
          <Select
            showSearch
            placeholder="请输入姓名或工号搜索成员"
            style={{ width: '100%' }}
            value={transferTarget?.welinkId || undefined}
            onSearch={(val) => { setTransferSearchValue(val); handleTransferSearch(val); }}
            onChange={(value) => {
              const user = transferSearchResults.find((u) => u.welinkId === value);
              setTransferTarget(user || null);
            }}
            filterOption={false}
            notFoundContent={transferSearchFetching ? '搜索中...' : (transferSearchValue ? '未找到匹配人员' : '请输入姓名或工号搜索成员')}
            dropdownClassName="member-select-dropdown"
            optionLabelProp="label"
          >
            {transferSearchResults.map((user) => (
              <Option key={user.welinkId} value={user.welinkId} label={`${user.memberNameCn} ${user.w3Account} ${user.deptName || ''}`}>
                <div className="member-option">
                  <span className="member-option-name">{user.memberNameCn}</span>
                  <span className="member-option-id">{user.w3Account}</span>
                  <span className="member-option-dept">{user.deptName}</span>
                </div>
              </Option>
            ))}
          </Select>
        </div>
      </Modal>

      {/* 删除成员确认 */}
      <DeleteConfirmModal
        open={deleteModalVisible}
        onClose={() => { setDeleteModalVisible(false); setDeletingMember(null); }}
        onConfirm={handleDelete}
        modalInfo={{
          title: '确认删除',
          content: `确认将 ${deletingMember?.memberNameCn || ''} 从应用中移除？该操作不可撤销`,
          confirmButtonText: '确认删除',
          loadingText: '删除中...',
          dangerColor: '#ff4d4f',
        }}
      />

      {/* 转移 Owner 二次确认 */}
      <DeleteConfirmModal
        open={transferConfirmVisible}
        onClose={() => setTransferConfirmVisible(false)}
        onConfirm={handleTransferConfirm}
        modalInfo={{
          title: '确认转移 Owner',
          content: `确认将 Owner 转移给 ${transferTarget?.memberNameCn || ''}？`,
          confirmButtonText: '确认转移',
          loadingText: '转移中...',
          dangerColor: '#faad14',
        }}
      />

      {/* 转移 Owner 二次确认 */}
      <DeleteConfirmModal
        open={transferConfirmVisible}
        onClose={() => setTransferConfirmVisible(false)}
        onConfirm={handleTransferConfirm}
        modalInfo={{
          title: '确认转移 Owner',
          content: `确认将 Owner 转移给 ${transferTarget?.memberNameCn || ''}？`,
          confirmButtonText: '确认转移',
          loadingText: '转移中...',
          dangerColor: '#faad14',
        }}
      />
    </div>
  );
}

export default Members;