<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { blogFlowState } from "../stores/blogFlow";
import { sessionState, isAuthenticated } from "../stores/session";
import { blogApi } from "../services/blogApi";
import { followApi } from "../services/followApi";
import { shopApi } from "../services/shopApi";
import { shopFlowState } from "../stores/shopFlow";
import { splitImages, formatDateTime, renderRichText, excerpt } from "../utils/view";
import { setNotice } from "../stores/appState";
import { rememberBlog, rememberShop } from "../stores/historyState";

const route = useRoute();
const router = useRouter();
const followed = ref(false);
const activeImage = ref(0);

const images = computed(() => splitImages(blogFlowState.detailBlog.value?.images));
const isOwnBlog = computed(
  () => sessionState.currentUser.value?.id === blogFlowState.detailBlog.value?.userId,
);
const richContent = computed(() => renderRichText(blogFlowState.detailBlog.value?.content));

async function loadDetail() {
  const blogId = route.params.id;
  const detailResult = await blogApi.fetchDetail(blogId, { silentError: true });
  if (!detailResult.success) {
    return;
  }

  blogFlowState.detailBlog.value = detailResult.data || null;
  rememberBlog(detailResult.data);
  activeImage.value = 0;

  await Promise.all([
    blogApi.fetchLikes(blogId, {
      silentError: true,
      onSuccess: (data) => {
        blogFlowState.blogLikes.value = Array.isArray(data) ? data : [];
      },
    }),
    shopApi.fetchDetail(detailResult.data?.shopId, {
      silentError: true,
      onSuccess: (data) => {
        shopFlowState.selectedShop.value = data || null;
        rememberShop(data);
      },
    }),
  ]);

  if (isAuthenticated() && !isOwnBlog.value) {
    const followResult = await followApi.check(blogFlowState.detailBlog.value.userId, {
      silentError: true,
    });
    if (followResult.success) {
      followed.value = Boolean(followResult.data);
    }
  }
}

async function toggleLike() {
  if (!isAuthenticated()) {
    router.push({
      path: "/login",
      query: { redirect: route.fullPath },
    });
    return;
  }
  const { success } = await blogApi.toggleLike(route.params.id, {
    successMessage: "点赞状态已切换。",
  });
  if (success) {
    await loadDetail();
  }
}

async function toggleFollow() {
  if (!isAuthenticated()) {
    router.push({
      path: "/login",
      query: { redirect: route.fullPath },
    });
    return;
  }

  const { success } = await followApi.toggle(
    blogFlowState.detailBlog.value.userId,
    !followed.value,
    {
      successMessage: followed.value ? "已取消关注。" : "已关注作者。",
    },
  );

  if (success) {
    followed.value = !followed.value;
  }
}

function jumpToAuthor() {
  if (isOwnBlog.value) {
    router.push("/me");
    return;
  }
  router.push(`/user/${blogFlowState.detailBlog.value.userId}`);
}

function openShop() {
  if (shopFlowState.selectedShop.value?.id) {
    router.push(`/shop/${shopFlowState.selectedShop.value.id}`);
  } else {
    setNotice("info", "当前笔记未关联商铺。");
  }
}

watch(() => route.params.id, loadDetail);
onMounted(loadDetail);
</script>

<template>
  <section class="consumer-page">
    <article class="panel ue-washi ue-shadow">
      <div class="panel-head">
        <h2>{{ blogFlowState.detailBlog.value?.title || "笔记详情" }}</h2>
        <button class="secondary" @click="router.back()">返回</button>
      </div>

      <div class="blog-detail-author">
        <button class="blog-detail-author-button" @click="jumpToAuthor">
          <strong>{{ blogFlowState.detailBlog.value?.name || "匿名作者" }}</strong>
          <small>{{ formatDateTime(blogFlowState.detailBlog.value?.createTime) }}</small>
        </button>
        <button
          v-if="blogFlowState.detailBlog.value && !isOwnBlog"
          class="accent"
          @click="toggleFollow"
        >
          {{ followed ? "取消关注" : "关注作者" }}
        </button>
      </div>

      <div class="blog-detail-gallery">
        <img
          v-if="images[activeImage]"
          :src="images[activeImage]"
          :alt="blogFlowState.detailBlog.value?.title"
          class="blog-detail-main-image"
        />
        <div class="blog-detail-thumbs">
          <button
            v-for="(image, index) in images"
            :key="image"
            class="blog-detail-thumb"
            :class="{ active: activeImage === index }"
            @click="activeImage = index"
          >
            <img :src="image" :alt="`预览 ${index + 1}`" />
          </button>
        </div>
      </div>

      <div class="consumer-rich-copy blog-detail-content">
        <div v-html="richContent || excerpt(blogFlowState.detailBlog.value?.content, 320)" />
      </div>

      <div class="button-row wrap">
        <button class="accent" @click="toggleLike">
          {{ blogFlowState.detailBlog.value?.isLike ? "取消点赞" : "点赞" }}
          {{ blogFlowState.detailBlog.value?.liked ?? 0 }}
        </button>
        <button class="secondary" @click="openShop">
          查看关联商铺
        </button>
      </div>
    </article>

    <article v-if="shopFlowState.selectedShop.value" class="panel ue-washi ue-shadow">
      <div class="panel-head">
        <h3>关联店铺</h3>
        <span class="status-pill muted">从笔记继续转店铺</span>
      </div>
      <div class="consumer-list-button">
        <div>
          <strong>{{ shopFlowState.selectedShop.value.name }}</strong>
          <p class="shop-preview-address">{{ shopFlowState.selectedShop.value.address || "地址待补充" }}</p>
        </div>
        <button class="accent" @click="openShop">去看店铺详情</button>
      </div>
    </article>

    <article class="panel ue-washi ue-shadow">
      <div class="panel-head">
        <h3>点赞席位</h3>
        <span class="status-pill muted">{{ blogFlowState.blogLikes.value.length }} 人</span>
      </div>
      <div class="avatar-strip">
        <div
          v-for="user in blogFlowState.blogLikes.value"
          :key="user.id"
          class="avatar-token"
        >
          <strong>{{ user.nickName || `用户 ${user.id}` }}</strong>
        </div>
      </div>
    </article>

    <article class="panel ue-washi ue-shadow">
      <div class="panel-head">
        <h3>当前限制</h3>
        <span class="status-pill muted">显式降级</span>
      </div>
      <p class="helper">
        原型里的评论列表和回复交互依赖评论接口。当前后端未实现 `/blog-comments`
        读写能力，所以本页保留点赞、关注作者和关联商铺三条主链路。
      </p>
    </article>
  </section>
</template>
