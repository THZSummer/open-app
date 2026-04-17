export interface App {
  id: string;
  name: string;
  icon: string;
  owner: string;
  role: string;
  updateTime: string;
  eamap: string | null;
}

export interface Member {
  id: number;
  name: string;
  employeeId: string;
  role: string;
  status: string;
}

export interface User {
  id: number;
  name: string;
  employeeId: string;
  email: string;
}

export interface Version {
  id: number;
  version: string;
  status: '已发布' | '审核中' | '审核未通过';
  publisher: string;
  approvedTime: string;
}

export interface ApiPermission {
  id: string;
  name: string;
  codeName: string;
  authType: string;
  category: string;
  status: '已审核' | '审核中' | '已中止';
}

export interface Event {
  id: string;
  name: string;
  type: string;
  permission: string;
}

export interface Capability {
  type: string;
  name: string;
  icon: string;
  enabled: boolean;
}
