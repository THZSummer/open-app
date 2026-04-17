import React, { useState, useEffect } from 'react';
import { Button, Table, Modal, Input, Radio, Tag, message } from 'antd';
import { PlusOutlined, DeleteOutlined } from '@ant-design/icons';
import { fetchMembers, fetchUsers, addMember, removeMember } from './thunk';
import { rolePermissions } from './mock';
import type { Member, User } from '../../types';
import styles from './Members.module.less';

const Members: React.FC = () => {
  const [members, setMembers] = useState<Member[]>([]);
  const [loading, setLoading] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);
  const [searchResults, setSearchResults] = useState<User[]>([]);
  const [selectedUser, setSelectedUser] = useState<User | null>(null);
  const [selectedRole, setSelectedRole] = useState('开发者');
  const [searchKeyword, setSearchKeyword] = useState('');

  useEffect(() => {
    loadMembers();
  }, []);

  const loadMembers = async () => {
    setLoading(true);
    const data = await fetchMembers();
    setMembers(data);
    setLoading(false);
  };

  const handleSearch = async (keyword: string) => {
    setSearchKeyword(keyword);
    if (keyword) {
      const users = await fetchUsers(keyword);
      setSearchResults(users);
    } else {
      setSearchResults([]);
    }
  };

  const handleSelectUser = (user: User) => {
    setSelectedUser(user);
    setSearchResults([]);
    setSearchKeyword(user.name);
  };

  const handleAddMember = async () => {
    if (!selectedUser) {
      message.error('请选择用户');
      return;
    }
    const newMember = await addMember(selectedUser.id, selectedRole);
    setMembers([...members, newMember]);
    setModalVisible(false);
    setSelectedUser(null);
    setSearchKeyword('');
    message.success('添加成功');
  };

  const handleRemoveMember = async (memberId: number) => {
    await removeMember(memberId);
    setMembers(members.filter(m => m.id !== memberId));
    message.success('删除成功');
  };

  const columns = [
    { title: '姓名', dataIndex: 'name', key: 'name' },
    { title: '工号', dataIndex: 'employeeId', key: 'employeeId' },
    {
      title: '角色',
      dataIndex: 'role',
      key: 'role',
      render: (role: string) => <Tag color={role === '管理员' ? 'blue' : 'default'}>{role}</Tag>,
    },
    {
      title: '操作',
      key: 'action',
      render: (_: any, record: Member) => (
        <Button 
          type="text" 
          danger 
          icon={<DeleteOutlined />}
          onClick={() => handleRemoveMember(record.id)}
        >
          删除
        </Button>
      ),
    },
  ];

  return (
    <div className={styles.container}>
      <div className={styles.header}>
        <div>
          <h2 className={styles.title}>成员管理</h2>
          <p className={styles.desc}>管理应用成员和角色权限</p>
        </div>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => setModalVisible(true)}>
          添加成员
        </Button>
      </div>
      <Table columns={columns} dataSource={members} loading={loading} rowKey="id" />

      <Modal
        title="添加成员"
        open={modalVisible}
        onOk={handleAddMember}
        onCancel={() => {
          setModalVisible(false);
          setSelectedUser(null);
          setSearchKeyword('');
          setSearchResults([]);
        }}
        destroyOnClose
      >
        <div className={styles.formSection}>
          <label>人员搜索</label>
          <Input.Search
            placeholder="搜索姓名或工号"
            value={searchKeyword}
            onChange={(e) => handleSearch(e.target.value)}
            allowClear
          />
          {searchResults.length > 0 && (
            <div className={styles.searchResults}>
              {searchResults.map((user) => (
                <div 
                  key={user.id} 
                  className={styles.searchItem}
                  onClick={() => handleSelectUser(user)}
                >
                  <div className={styles.userName}>{user.name}</div>
                  <div className={styles.userMeta}>{user.employeeId} | {user.email}</div>
                </div>
              ))}
            </div>
          )}
        </div>

        <div className={styles.formSection}>
          <label>角色</label>
          <Radio.Group value={selectedRole} onChange={(e) => setSelectedRole(e.target.value)}>
            <Radio value="管理员">管理员</Radio>
            <Radio value="开发者">开发者</Radio>
          </Radio.Group>
        </div>

        <div className={styles.permissionBox}>
          <div className={styles.permissionTitle}>{selectedRole}权限说明</div>
          <div className={styles.permissionTags}>
            {rolePermissions[selectedRole]?.map((perm) => (
              <Tag key={perm} className={styles.permissionTag}>{perm}</Tag>
            ))}
          </div>
        </div>
      </Modal>
    </div>
  );
};

export default Members;
