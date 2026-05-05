<script setup>
import { useRouter } from "vue-router";

const router = useRouter();

const moduleCards = [
  {
    title: "用户中心",
    path: "/me",
    description: "管理当前用户、签到、昵称修改和退出登录。",
  },
  {
    title: "商铺管理",
    path: "/shop",
    description: "使用统一筛选区、表格区和分页区查询商铺与详情。",
  },
  {
    title: "笔记管理",
    path: "/blog",
    description: "按标签页集中处理发布、热门、个人笔记和详情。",
  },
  {
    title: "关注关系",
    path: "/follow",
    description: "查看关注状态、共同关注和关注流。",
  },
  {
    title: "优惠券管理",
    path: "/voucher",
    description: "查询店铺券、创建普通券和秒杀券。",
  },
  {
    title: "图片上传",
    path: "/upload",
    description: "统一上传、预览和删除博客图片。",
  },
  {
    title: "智能客服",
    path: "/rag",
    description: "面向文档知识库的 RAG 对话与索引状态管理。",
  },
];
</script>

<template>
  <section class="app-page">
    <ElCard class="page-panel">
      <template #header>
        <div class="page-panel__header">
          <div>
            <h3 class="page-panel__title">模块入口</h3>
            <p class="page-panel__hint">后续新增页面直接挂到统一路由和统一壳子中。</p>
          </div>
        </div>
      </template>

      <div class="page-grid-3">
        <ElCard
          v-for="item in moduleCards"
          :key="item.path"
          shadow="hover"
          class="module-card"
          @click="router.push(item.path)"
        >
          <template #header>
            <div class="page-panel__header">
              <div>
                <h4 class="page-panel__title">{{ item.title }}</h4>
                <p class="page-panel__hint">{{ item.path }}</p>
              </div>
              <ElTag effect="plain">模块页</ElTag>
            </div>
          </template>
          <p class="stat-card__desc">{{ item.description }}</p>
          <div class="page-actions" style="margin-top: 16px;">
            <ElButton type="primary" plain @click.stop="router.push(item.path)">
              打开页面
            </ElButton>
          </div>
        </ElCard>
      </div>
    </ElCard>
  </section>
</template>
