import React from 'react';
import { useNavigate } from 'react-router-dom';
import { mergeQueryParams, toQueryString } from '../utils/queryHelper';

// 模拟列表数据
const mockData = [
  { id: 1, name: '项目一', status: '进行中' },
  { id: 2, name: '项目二', status: '已完成' },
  { id: 3, name: '项目三', status: '待开始' }
];

// 页面内联样式
const styles = {
  container: { padding: '20px' },
  title: { color: '#1890ff', marginBottom: '16px' },
  table: { width: '100%', borderCollapse: 'collapse', marginTop: '16px' },
  th: { border: '1px solid #ddd', padding: '8px 12px', background: '#fafafa', textAlign: 'left' },
  td: { border: '1px solid #ddd', padding: '8px 12px' },
  btn: { padding: '4px 12px', marginRight: '8px', cursor: 'pointer', border: '1px solid #1890ff', background: '#1890ff', color: '#fff', borderRadius: '4px' }
};

/**
 * 列表页组件
 */
function List() {
  const navigate = useNavigate();

  /**
   * 跳转到详情页
   * @param {Object} record 当前行数据
   */
  const goDetail = (record) => {
    // 从参数对象中获取记录 ID
    const { id } = record;
    // 合并主应用参数和子页面参数
    const query = toQueryString(mergeQueryParams({ id }));
    navigate(`/detail${query}`);
  };

  /**
   * 跳转到编辑页
   * @param {Object} record 当前行数据
   */
  const goEdit = (record) => {
    // 从参数对象中获取记录 ID
    const { id } = record;
    // 合并主应用参数和子页面参数
    const query = toQueryString(mergeQueryParams({ id }));
    navigate(`/edit${query}`);
  };

  return (
    <div style={styles.container}>
      <h2 style={styles.title}>子应用 B - 列表页</h2>
      <table style={styles.table}>
        <thead>
          <tr>
            <th style={styles.th}>ID</th>
            <th style={styles.th}>名称</th>
            <th style={styles.th}>状态</th>
            <th style={styles.th}>操作</th>
          </tr>
        </thead>
        <tbody>
          {mockData.map((item) => (
            <tr key={item.id}>
              <td style={styles.td}>{item.id}</td>
              <td style={styles.td}>{item.name}</td>
              <td style={styles.td}>{item.status}</td>
              <td style={styles.td}>
                <button style={styles.btn} onClick={() => goDetail(item)}>查看详情</button>
                <button style={styles.btn} onClick={() => goEdit(item)}>编辑</button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export default List;
