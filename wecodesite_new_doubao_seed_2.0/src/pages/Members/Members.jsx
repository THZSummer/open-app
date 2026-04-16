import React, { useState } from 'react';
import { Button, Table, Modal, Input, Radio, message } from 'antd';
import { UserAddOutlined, DeleteOutlined } from '@ant-design/icons';
import './Members.m.less';
import mockData from './mock';

const { Search } = Input;

const Members = () => {
  const [members, setMembers] = useState(mockData.members);
  const [isModalVisible, setIsModalVisible] = useState(false);
  const [selectedUser, setSelectedUser] = useState(null);
  const [role, setRole] = useState('developer');
  const [searchText, setSearchText] = useState('');

  const handleAddMember = () => {
    setIsModalVisible(true);
  };

  const handleDeleteMember = (id) => {
    Modal.confirm({
      title: '确认删除',
      content: '确定要删除该成员吗？',
      onOk: () => {
        setMembers(members.filter(member => member.id !== id));
        message.success('删除成功');
      }
    });
  };

  const handleModalOk = () => {
    if (!selectedUser) {
      message.error('请选择用户');
      return;
    }
    const newMember = {
      id: Date.now().toString(),
      name: selectedUser.name,
      employeeId: selectedUser.employeeId,
      role: role
    };
    setMembers([...members, newMember]);
    setIsModalVisible(false);
    setSelectedUser(null);
    setRole('developer');
    setSearchText('');
    message.success('添加成功');
  };

  const handleModalCancel = () => {
    setIsModalVisible(false);
    setSelectedUser(null);
    setRole('developer');
    setSearchText('');
  };

  const columns = [
    {
      title: '姓名',
      dataIndex: 'name',
      key: 'name'
    },
    {
      title: '工号',
      dataIndex: 'employeeId',
      key: 'employeeId'
    },
    {
      title: '角色',
      dataIndex: 'role',
      key: 'role',
      render: (role) => (
        <span className={`roleTag ${role === 'admin' ? 'adminTag' : 'developerTag'}`}>
          {role === 'admin' ? '管理员' : '开发者'}
        </span>
      )
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
        <Button
          type="text"
          danger
          icon={<DeleteOutlined />}
          onClick={() => handleDeleteMember(record.id)}
        >
          删除
        </Button>
      )
    }
  ];

  // 模拟用户搜索结果
  const mockUsers = [
    { id: '1', name: '张三', employeeId: 'E001', email: 'zhangsan@company.com' },
    { id: '2', name: '李四', employeeId: 'E002', email: 'lisi@company.com' },
    { id: '3', name: '王五', employeeId: 'E003', email: 'wangwu@company.com' }
  ];

  const filteredUsers = mockUsers.filter(user => 
    user.name.includes(searchText) || user.employeeId.includes(searchText)
  );

  return (
    <div className="members">
      <div className="header">
        <h1 className="title">成员管理</h1>
        <p className="description">管理应用成员和角色权限</p>
        <Button type="primary" icon={<UserAddOutlined />} onClick={handleAddMember}>
          添加成员
        </Button>
      </div>
      <div className="card">
        <Table
          columns={columns}
          dataSource={members}
          rowKey="id"
          pagination={false}
        />
      </div>

      {/* 添加成员 Modal */}
      <Modal
        title="添加成员"
        open={isModalVisible}
        onOk={handleModalOk}
        onCancel={handleModalCancel}
        okText="确认添加"
        cancelText="取消"
      >
        <div className="modalContent">
          <div className="modalSection">
            <h3 className="modalSectionTitle">人员搜索</h3>
            <Search
              placeholder="搜索姓名或工号"
              value={searchText}
              onChange={(e) => setSearchText(e.target.value)}
              style={{ marginBottom: 16 }}
            />
            {filteredUsers.map(user => (
              <div
                key={user.id}
                className={`userItem ${selectedUser?.id === user.id ? 'selectedUser' : ''}`}
                onClick={() => setSelectedUser(user)}
              >
                <div className="userName">{user.name}</div>
                <div className="userInfo">{user.employeeId} | {user.email}</div>
              </div>
            ))}
            {selectedUser && (
              <div className="selectedInfo">
                已选择：{selectedUser.name} ({selectedUser.employeeId})
              </div>
            )}
          </div>
          <div className="modalSection">
            <h3 className="modalSectionTitle">角色</h3>
            <Radio.Group value={role} onChange={(e) => setRole(e.target.value)}>
              <Radio value="admin">管理员</Radio>
              <Radio value="developer">开发者</Radio>
            </Radio.Group>
          </div>
          <div className="modalSection">
            <h3 className="modalSectionTitle">角色权限说明</h3>
            <div className="permissionTags">
              <span className="permissionTag">查看应用信息</span>
              <span className="permissionTag">调用API</span>
              {role === 'admin' && (
                <>
                  <span className="permissionTag">管理成员</span>
                  <span className="permissionTag">发布版本</span>
                </>
              )}
            </div>
          </div>
        </div>
      </Modal>
    </div>
  );
};

export default Members;