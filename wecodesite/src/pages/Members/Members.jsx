import React, { useState, useMemo } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Table, Button, Tag, Modal, Input, Radio } from 'antd';
import { PlusOutlined, DeleteOutlined, SearchOutlined } from '@ant-design/icons';
import { mockMembers, mockUsers, rolePermissions } from './mock';
import './Members.m.less';

function Members() {
  const [searchParams] = useSearchParams();
  const [members, setMembers] = useState(mockMembers);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [searchValue, setSearchValue] = useState('');
  const [selectedUser, setSelectedUser] = useState(null);
  const [selectedRole, setSelectedRole] = useState('管理员');

  const filteredUsers = useMemo(() => {
    if (!searchValue) return [];
    const value = searchValue.toLowerCase();
    return mockUsers.filter(
      (user) =>
        user.name.toLowerCase().includes(value) ||
        user.employeeId.toLowerCase().includes(value)
    );
  }, [searchValue]);

  const columns = [
    {
      title: '姓名',
      dataIndex: 'name',
      key: 'name',
    },
    {
      title: '工号',
      dataIndex: 'employeeId',
      key: 'employeeId',
    },
    {
      title: '角色',
      dataIndex: 'role',
      key: 'role',
      render: (role) => (
        <Tag color={role === '管理员' ? 'blue' : 'default'}>{role}</Tag>
      ),
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
        <div style={{ display: 'flex', gap: 8 }}>
          <Button
            type="link"
            size="small"
            danger
            icon={<DeleteOutlined />}
            onClick={() => handleDelete(record.id)}
          >
            删除
          </Button>
        </div>
      ),
    },
  ];

  const handleDelete = (id) => {
    setMembers(members.filter((m) => m.id !== id));
  };

  const handleAddMember = () => {
    setIsModalOpen(true);
    setSearchValue('');
    setSelectedUser(null);
    setSelectedRole('管理员');
  };

  const handleModalOk = () => {
    if (selectedUser) {
      const newMember = {
        id: Date.now(),
        name: selectedUser.name,
        employeeId: selectedUser.employeeId,
        role: selectedRole,
        status: '正常',
      };
      setMembers([...members, newMember]);
      setIsModalOpen(false);
      setSearchValue('');
      setSelectedUser(null);
      setSelectedRole('管理员');
    }
  };

  const handleModalCancel = () => {
    setIsModalOpen(false);
    setSearchValue('');
    setSelectedUser(null);
    setSelectedRole('管理员');
  };

  const handleUserSelect = (user) => {
    setSelectedUser(user);
    setSearchValue(user.name);
  };

  return (
    <div className="members">
      <div className="page-header">
        <div className="page-header-left">
          <h4 className="page-title">成员管理</h4>
          <span className="page-desc">管理应用成员和角色权限</span>
        </div>
        <Button
          type="primary"
          icon={<PlusOutlined />}
          className="btn-primary"
          onClick={handleAddMember}
        >
          添加成员
        </Button>
      </div>
      <Table
        columns={columns}
        dataSource={members}
        rowKey="id"
        pagination={false}
      />
      <Modal
        title="添加成员"
        open={isModalOpen}
        onOk={handleModalOk}
        onCancel={handleModalCancel}
        okText="确定"
        cancelText="取消"
        destroyOnClose
      >
        <div className="add-member-modal">
          <div className="form-item">
            <label className="form-label">人员搜索</label>
            <div className="user-search-wrapper">
              <Input
                placeholder="搜索姓名或工号"
                prefix={<SearchOutlined />}
                value={searchValue}
                onChange={(e) => {
                  setSearchValue(e.target.value);
                  setSelectedUser(null);
                }}
              />
              {searchValue && !selectedUser && filteredUsers.length > 0 && (
                <div className="search-results">
                  {filteredUsers.map((user) => (
                    <div
                      key={user.id}
                      className="search-result-item"
                      onClick={() => handleUserSelect(user)}
                    >
                      <span className="user-name">{user.name}</span>
                      <span className="user-info">
                        {user.employeeId} | {user.email}
                      </span>
                    </div>
                  ))}
                </div>
              )}
              {searchValue && filteredUsers.length === 0 && (
                <div className="search-results">
                  <div className="search-no-result">未找到匹配的人员</div>
                </div>
              )}
            </div>
            {selectedUser && (
              <div className="selected-user">
                已选择：{selectedUser.name} ({selectedUser.employeeId})
              </div>
            )}
          </div>
          <div className="form-item">
            <label className="form-label">角色</label>
            <Radio.Group
              value={selectedRole}
              onChange={(e) => setSelectedRole(e.target.value)}
              className="role-radio-group"
            >
              <Radio value="管理员">
                <span className="role-option">
                  <span className="role-name">管理员</span>
                  <span className="role-desc">拥有全部管理权限</span>
                </span>
              </Radio>
              <Radio value="开发者">
                <span className="role-option">
                  <span className="role-name">开发者</span>
                  <span className="role-desc">基础查看和调用权限</span>
                </span>
              </Radio>
            </Radio.Group>
          </div>
          <div className="form-item">
            <label className="form-label">
              {selectedRole}权限说明
            </label>
            <div className="permissions-list">
              {rolePermissions[selectedRole]?.map((permission, index) => (
                <Tag key={index} className="permission-tag">
                  {permission}
                </Tag>
              ))}
            </div>
          </div>
        </div>
      </Modal>
    </div>
  );
}

export default Members;