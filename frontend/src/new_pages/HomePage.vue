<script setup>
import { computed } from "vue";
import { useRouter } from "vue-router";
import { isAuthenticated, sessionState } from "../stores/session";

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
];

const rescueSteps = [
  "统一布局与路由挂载，只让内容区切换。",
  "统一 Element Plus 表单、表格、弹窗和上传能力。",
  "保留现有 API 和 store，页面结构全部后台化。",
  "将复杂页面拆成卡片与标签页，后续新增页面直接复用。",
];

const backendFacts = [
  "用户、商铺、分类、笔记、关注、优惠券、秒杀、上传接口都已可用。",
  "评论接口仍未纳入本次改造范围，避免扩大施工面。",
  "当前路由只承载 new_pages，旧 pages 保留参考但不参与运行。",
];

const sessionText = computed(() =>
  isAuthenticated()
    ? `当前登录用户：${sessionState.currentUser.value?.nickName || "已持有 token"}`
    : "当前未登录，先去登录页获取 token。",
);
</script>

<template>
  <section class="app-page">
    <ElCard class="page-panel">
      <template #header>
        <div class="page-panel__header">
          <div>
            <h2 class="page-panel__title">new_pages 后台化总览</h2>
            <p class="page-panel__hint">
              所有页面已收敛到统一布局、统一风格、统一组件体系。
            </p>
          </div>
          <ElTag type="primary" effect="plain">Element Plus Ready</ElTag>
        </div>
      </template>

      <div class="stats-grid">
        <ElCard shadow="never" class="stat-card">
          <span class="stat-card__label">当前会话</span>
          <p class="stat-card__value">{{ isAuthenticated() ? "已登录" : "未登录" }}</p>
          <p class="stat-card__desc">{{ sessionText }}</p>
        </ElCard>
        <ElCard shadow="never" class="stat-card">
          <span class="stat-card__label">页面数量</span>
          <p class="stat-card__value">9</p>
          <p class="stat-card__desc">Home、Login、Me、Shop、Blog、Follow、Voucher、Upload、BlogEditor。</p>
        </ElCard>
        <ElCard shadow="never" class="stat-card">
          <span class="stat-card__label">统一壳子</span>
          <p class="stat-card__value">2 套</p>
          <p class="stat-card__desc">业务页走 MainLayout，登录页走 BlankLayout。</p>
        </ElCard>
        <ElCard shadow="never" class="stat-card">
          <span class="stat-card__label">视觉基线</span>
          <p class="stat-card__value">黑白灰蓝</p>
          <p class="stat-card__desc">白底、黑字、灰线、蓝色激活态，适合长期维护。</p>
        </ElCard>
      </div>
    </ElCard>

    <div class="page-grid-2">
      <ElCard class="page-panel">
        <template #header>
          <div class="page-panel__header">
            <div>
              <h3 class="page-panel__title">改造顺序</h3>
              <p class="page-panel__hint">按风险和依赖顺序推进。</p>
            </div>
          </div>
        </template>
        <ElTimeline>
          <ElTimelineItem
            v-for="step in rescueSteps"
            :key="step"
            type="primary"
            hollow
          >
            {{ step }}
          </ElTimelineItem>
        </ElTimeline>
      </ElCard>

      <ElCard class="page-panel">
        <template #header>
          <div class="page-panel__header">
            <div>
              <h3 class="page-panel__title">后端边界</h3>
              <p class="page-panel__hint">页面改造不改 API 契约。</p>
            </div>
          </div>
        </template>
        <ElSpace direction="vertical" fill size="large">
          <ElAlert
            v-for="fact in backendFacts"
            :key="fact"
            :title="fact"
            type="info"
            :closable="false"
            show-icon
          />
        </ElSpace>
      </ElCard>
    </div>

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
