import { mockMembers, mockUsers } from './mock';
import type { Member, User } from '../../types';

export const fetchMembers = async (): Promise<Member[]> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve(mockMembers);
    }, 500);
  });
};

export const fetchUsers = async (keyword?: string): Promise<User[]> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      let users = mockUsers;
      if (keyword) {
        users = users.filter(u => 
          u.name.includes(keyword) || u.employeeId.includes(keyword)
        );
      }
      resolve(users);
    }, 300);
  });
};

export const addMember = async (userId: number, role: string): Promise<Member> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      const user = mockUsers.find(u => u.id === userId);
      const newMember: Member = {
        id: userId,
        name: user?.name || '',
        employeeId: user?.employeeId || '',
        role,
        status: 'active',
      };
      resolve(newMember);
    }, 300);
  });
};

export const removeMember = async (_memberId: number): Promise<void> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve();
    }, 300);
  });
};
