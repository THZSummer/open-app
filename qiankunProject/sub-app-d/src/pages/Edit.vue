<template>
  <!-- 编辑页 -->
  <div class="edit-page">
    <h2>子应用 D - 编辑页</h2>
    <div class="info">
      <p>编辑记录 ID：{{ id }}</p>
      <p>来源（主应用参数）：{{ source }}</p>
      <p>用户 ID（主应用参数）：{{ userId }}</p>
    </div>
    <button class="btn" @click="goBack">返回列表</button>
  </div>
</template>

<script>
import { useRoute, useRouter } from 'vue-router';
import { mergeQueryParams, toQueryString } from '../utils/queryHelper';

export default {
  name: 'Edit',
  setup() {
    const route = useRoute();
    const router = useRouter();

    // 从 URL 获取参数
    const id = route.query.id;
    const source = route.query.source;
    const userId = route.query.userId;

    /**
     * 返回列表页，保留主应用传入的参数
     */
    const goBack = () => {
      const query = toQueryString(mergeQueryParams({}));
      router.push(`/${query}`);
    };

    return { id, source, userId, goBack };
  }
};
</script>

<style scoped>
/* 编辑页样式 */
.edit-page {
  padding: 20px;
}

.edit-page h2 {
  color: #ff7a45;
  margin-bottom: 16px;
}

.info {
  line-height: 2;
  font-size: 14px;
}

.btn {
  padding: 4px 12px;
  cursor: pointer;
  border: 1px solid #ff7a45;
  background: #ff7a45;
  color: #fff;
  border-radius: 4px;
  margin-top: 16px;
}
</style>
