<template>
  <!-- 子应用 D 根组件 -->
  <div class="sub-app-d">
    <h2>子应用 D（Vue 3 + Webpack）</h2>
    <p>这是通过 qiankun 嵌入到主应用中的子应用页面。</p>
    <p>技术栈：Vue 3 + Webpack 5</p>
  </div>
</template>

<script>
/**
 * 子应用 D 根组件
 */
export default {
  name: 'App'
};
</script>

<style scoped>
/* 子应用 D 样式 */
.sub-app-d {
  padding: 20px;
}

.sub-app-d h2 {
  color: #ff7a45;
  margin-bottom: 12px;
}

.sub-app-d p {
  color: #666;
  line-height: 1.8;
}
</style>
