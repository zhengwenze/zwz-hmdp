<script setup>
import zhCn from "element-plus/es/locale/lang/zh-cn";
</script>

<template>
  <ElConfigProvider
    :locale="zhCn"
    size="default"
    :button="{ autoInsertSpace: true }"
  >
    <router-view />
  </ElConfigProvider>
</template>
