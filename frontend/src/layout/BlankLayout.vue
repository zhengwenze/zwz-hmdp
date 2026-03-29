<template>
  <div class="blank-layout">
    <div class="blank-layout__content">
      <ElAlert
        v-if="$route.meta.title === '登录'"
        title="登录后可继续体验用户、关注、秒杀和发笔记能力。"
        type="info"
        :closable="false"
        show-icon
      />
      <router-view />
    </div>
  </div>
</template>
