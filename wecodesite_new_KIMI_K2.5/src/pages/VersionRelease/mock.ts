import type { Version } from '../../types';

export const mockVersions: Version[] = [
  { id: 1, version: '1.0.0', status: '已发布', publisher: '张三', approvedTime: '2024-01-15 10:00' },
  { id: 2, version: '0.9.0', status: '审核中', publisher: '李四', approvedTime: '-' },
  { id: 3, version: '0.8.0', status: '审核未通过', publisher: '王五', approvedTime: '2024-01-10 14:30' },
  { id: 4, version: '0.7.0', status: '已发布', publisher: '张三', approvedTime: '2024-01-05 09:00' },
];
