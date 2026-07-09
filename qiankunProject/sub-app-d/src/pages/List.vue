<template>
  <!-- 列表页 -->
  <div class="list-page">
    <h2>子应用 D - 列表页</h2>
    <table>
      <thead>
        <tr>
          <th>ID</th>
          <th>名称</th>
          <th>状态</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in mockData" :key="item.id">
          <td>{{ item.id }}</td>
          <td>{{ item.name }}</td>
          <td>{{ item.status }}</td>
          <td>
            <button class="btn" @click="goDetail(item)">查看详情</button>
            <button class="btn" @click="goEdit(item)">编辑</button>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script>
import { useRouter } from 'vue-router';
import { mergeQueryParams, toQueryString } from '../utils/queryHelper';

// 模拟列表数据
const mockData = [
  { id: 1, name: '工单一', status: '进行中' },
  { id: 2, name: '工单二', status: '已完成' },
  { id: 3, name: '工单三', status: '待开始' }
];

export default {
  name: 'List',
  setup() {
    const router = useRouter();

    /**
     * 跳转到详情页
     * @param {Object} record 当前行数据
     */
    const goDetail = (record) => {
      // 从参数对象中获取记录 ID
      const { id } = record;
      // 合并主应用参数和子页面参数
      const query = toQueryString(mergeQueryParams({ id }));
      router.push(`/detail${query}`);
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
      router.push(`/edit${query}`);
    };

    return { mockData, goDetail, goEdit };
  }
};
</script>

<style scoped>
/* 列表页样式 */
.list-page {
  padding: 20px;
}

.list-page h2 {
  color: #ff7a45;
  margin-bottom: 16px;
}

.list-page table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 16px;
}

.list-page th,
.list-page td {
  border: 1px solid #ddd;
  padding: 8px 12px;
  text-align: left;
}

.list-page th {
  background: #fafafa;
}

.btn {
  padding: 4px 12px;
  margin-right: 8px;
  cursor: pointer;
  border: 1px solid #ff7a45;
  background: #ff7a45;
  color: #fff;
  border-radius: 4px;
}
</style>
