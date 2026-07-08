<template>
  <!-- 子应用 E 根组件 -->
  <div class="sub-app-e">
    <h2>子应用 E（Vue 3 + Vite）</h2>
    <p>这是通过 qiankun 嵌入到主应用中的子应用页面。</p>
    <p>技术栈：Vue 3 + Vite 5</p>
  </div>
</template>

<script>
/**
 * 子应用 E 根组件
 */
export default {
  name: 'App'
};
</script>

<style scoped>
/* 子应用 E 样式 */
.sub-app-e {
  padding: 20px;
}

.sub-app-e h2 {
  color: #42b883;
  margin-bottom: 12px;
}

.sub-app-e p {
  color: #666;
  line-height: 1.8;
}
</style>
