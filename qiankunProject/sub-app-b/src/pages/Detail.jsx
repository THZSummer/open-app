import React from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { mergeQueryParams, toQueryString } from '../utils/queryHelper';

// 页面内联样式
const styles = {
  container: { padding: '20px' },
  title: { color: '#1890ff', marginBottom: '16px' },
  info: { lineHeight: '2', fontSize: '14px' },
  btn: { padding: '4px 12px', cursor: 'pointer', border: '1px solid #1890ff', background: '#1890ff', color: '#fff', borderRadius: '4px', marginTop: '16px' }
};

/**
 * 详情页组件
 */
function Detail() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  // 从 URL 获取参数
  const id = searchParams.get('id');
  const source = searchParams.get('source');
  const userId = searchParams.get('userId');

  /**
   * 返回列表页，保留主应用传入的参数
   */
  const goBack = () => {
    const query = toQueryString(mergeQueryParams({}));
    navigate(`/${query}`);
  };

  return (
    <div style={styles.container}>
      <h2 style={styles.title}>子应用 B - 详情页</h2>
      <div style={styles.info}>
        <p>记录 ID：{id}</p>
        <p>来源（主应用参数）：{source}</p>
        <p>用户 ID（主应用参数）：{userId}</p>
      </div>
      <button style={styles.btn} onClick={goBack}>返回列表</button>
    </div>
  );
}

export default Detail;
