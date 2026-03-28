<script setup>
import { reactive, computed } from "vue";
import {
  sharedState,
  send,
  isLoading,
  endpointBadgeClass,
  toAssetUrl,
  splitImages,
  buildBlogPayload,
} from "../stores/sharedState";
import { cloneWithoutEmpty } from "../api";

const forms = reactive({
  blog: {
    create: {
      shopId: "1",
      title: "",
      images: "",
      content: "",
    },
    hotCurrent: "1",
    myCurrent: "1",
    detailId: "1",
    likesId: "1",
    userId: "1",
    userCurrent: "1",
    followLastId: String(Date.now()),
    followOffset: "0",
  },
  follow: {
    targetUserId: "1",
    isFollow: "true",
    checkUserId: "1",
    commonUserId: "1",
  },
});

const blogImageList = computed(() => splitImages(forms.blog.create.images));

async function createBlog() {
  await send(
    "POST /blog",
    { method: "POST", path: "/blog", body: buildBlogPayload(forms) },
    {
      successMessage: "博客发布成功。",
      onSuccess: async (data) => {
        if (data) {
          forms.blog.detailId = String(data);
          await Promise.all([fetchHotBlogs(), fetchMyBlogs(), fetchBlogDetail(data)]);
        }
      },
    },
  );
}

async function fetchHotBlogs() {
  await send(
    "GET /blog/hot",
    { method: "GET", path: "/blog/hot", query: { current: forms.blog.hotCurrent } },
    {
      successMessage: "热门博客已刷新。",
      onSuccess: (data) => {
        sharedState.hotBlogs.value = Array.isArray(data) ? data : [];
      },
    },
  );
}

async function fetchMyBlogs() {
  await send(
    "GET /blog/of/me",
    { method: "GET", path: "/blog/of/me", query: { current: forms.blog.myCurrent } },
    {
      successMessage: "我的博客列表已刷新。",
      onSuccess: (data) => {
        sharedState.myBlogs.value = Array.isArray(data) ? data : [];
      },
    },
  );
}

async function fetchBlogDetail(blogId = forms.blog.detailId) {
  forms.blog.detailId = String(blogId);
  await send(
    "GET /blog/{id}",
    { method: "GET", path: `/blog/${blogId}` },
    {
      successMessage: "博客详情已更新。",
      onSuccess: (data) => {
        sharedState.blogDetail.value = data || null;
      },
    },
  );
}

async function fetchBlogLikes(blogId = forms.blog.likesId) {
  forms.blog.likesId = String(blogId);
  await send(
    "GET /blog/likes/{id}",
    { method: "GET", path: `/blog/likes/${blogId}` },
    {
      successMessage: "点赞用户列表已更新。",
      onSuccess: (data) => {
        sharedState.blogLikes.value = Array.isArray(data) ? data : [];
      },
    },
  );
}

async function toggleBlogLike(blogId) {
  await send(
    "PUT /blog/like/{id}",
    { method: "PUT", path: `/blog/like/${blogId}` },
    {
      successMessage: "点赞状态已切换。",
      onSuccess: async () => {
        await Promise.all([fetchHotBlogs(), fetchBlogDetail(blogId), fetchBlogLikes(blogId)]);
      },
    },
  );
}

async function fetchUserBlogs() {
  await send(
    "GET /blog/of/user",
    { method: "GET", path: "/blog/of/user", query: { id: forms.blog.userId, current: forms.blog.userCurrent } },
    {
      successMessage: "指定用户博客列表已更新。",
      onSuccess: (data) => {
        sharedState.userBlogs.value = Array.isArray(data) ? data : [];
      },
    },
  );
}

async function fetchFollowFeed() {
  await send(
    "GET /blog/of/follow",
    { method: "GET", path: "/blog/of/follow", query: { lastId: forms.blog.followLastId, offset: forms.blog.followOffset } },
    {
      successMessage: "关注推送流已更新。",
      onSuccess: (data) => {
        sharedState.followFeed.value = data || null;
        if (data?.minTime) {
          forms.blog.followLastId = String(data.minTime);
        }
        if (data?.offset !== undefined && data?.offset !== null) {
          forms.blog.followOffset = String(data.offset);
        }
      },
    },
  );
}

function fillBlogDetail(blog) {
  forms.blog.detailId = String(blog.id);
  forms.blog.likesId = String(blog.id);
  fetchBlogDetail(blog.id);
}

function fillFollowTarget(userId) {
  const value = String(userId);
  forms.follow.targetUserId = value;
  forms.follow.checkUserId = value;
  forms.follow.commonUserId = value;
  forms.blog.userId = value;
}
</script>

<template>
  <section id="blogs" class="module-section">
    <div class="section-title">
      <div>
        <p class="eyebrow">Blog APIs</p>
        <h2>博客模块</h2>
      </div>
      <span class="section-hint">发布、热榜、我的笔记、详情、点赞、关注流</span>
    </div>

    <div class="card-grid two">
      <article class="panel ue-washi ue-shadow ukiyo-e-digital-card">
        <div class="panel-head">
          <h3>发布博客</h3>
          <span :class="endpointBadgeClass(sharedState.touchedEndpoints['POST /blog'])">POST /blog</span>
        </div>
        <div class="form-grid single">
          <label><span>商铺 ID</span><input v-model="forms.blog.create.shopId" /></label>
          <label><span>标题</span><input v-model="forms.blog.create.title" /></label>
          <label><span>图片路径（逗号分隔）</span><input v-model="forms.blog.create.images" /></label>
          <label><span>内容</span><textarea v-model="forms.blog.create.content" rows="6" /></label>
        </div>
        <div class="button-row wrap">
          <button :disabled="isLoading('POST /blog')" class="accent" @click="createBlog">发布博客</button>
        </div>
        <div class="image-strip">
          <img v-for="image in blogImageList" :key="image" :src="toAssetUrl(image)" :alt="image" />
        </div>
      </article>

      <article class="panel ue-washi ue-shadow ukiyo-e-digital-card">
        <div class="panel-head">
          <h3>热门博客</h3>
          <span :class="endpointBadgeClass(sharedState.touchedEndpoints['GET /blog/hot'])">/blog/hot</span>
        </div>
        <div class="form-grid double">
          <label><span>页码 current</span><input v-model="forms.blog.hotCurrent" /></label>
          <label><span>说明</span><input value="公开接口，登录后会补充 isLike" disabled /></label>
        </div>
        <div class="button-row">
          <button :disabled="isLoading('GET /blog/hot')" @click="fetchHotBlogs">刷新热门博客</button>
        </div>
        <div class="blog-grid">
          <article v-for="blog in sharedState.hotBlogs.value" :key="`hot-${blog.id}`" class="blog-card">
            <div class="blog-card-head">
              <strong>{{ blog.title }}</strong>
              <span class="ue-stamp">赞 {{ blog.liked }}</span>
            </div>
            <p>{{ blog.name || "匿名作者" }} · 店铺 {{ blog.shopId }}</p>
            <div class="image-strip">
              <img v-for="image in splitImages(blog.images).slice(0, 3)" :key="`${blog.id}-${image}`" :src="toAssetUrl(image)" :alt="blog.title" />
            </div>
            <div class="button-row tight">
              <button class="secondary" @click="fillBlogDetail(blog)">详情</button>
              <button class="secondary" @click="fetchBlogLikes(blog.id)">点赞列表</button>
              <button class="secondary" @click="toggleBlogLike(blog.id)">切换点赞</button>
              <button class="secondary" @click="fillFollowTarget(blog.userId)">设为关注对象</button>
            </div>
          </article>
        </div>
      </article>

      <article class="panel ue-washi ue-shadow ukiyo-e-digital-card">
        <div class="panel-head">
          <h3>我的博客</h3>
          <span :class="endpointBadgeClass(sharedState.touchedEndpoints['GET /blog/of/me'])">/blog/of/me</span>
        </div>
        <div class="form-grid single">
          <label><span>页码 current</span><input v-model="forms.blog.myCurrent" /></label>
        </div>
        <div class="button-row">
          <button :disabled="isLoading('GET /blog/of/me')" @click="fetchMyBlogs">查询我的博客</button>
        </div>
        <div class="simple-list">
          <div v-for="blog in sharedState.myBlogs.value" :key="`my-${blog.id}`" class="simple-row">
            <span>#{{ blog.id }} {{ blog.title }}</span>
            <div class="button-row tight">
              <button class="secondary" @click="fillBlogDetail(blog)">详情</button>
              <button class="secondary" @click="toggleBlogLike(blog.id)">点赞</button>
            </div>
          </div>
        </div>
      </article>

      <article class="panel ue-washi ue-shadow ukiyo-e-digital-card">
        <div class="panel-head">
          <h3>博客详情与点赞列表</h3>
          <span :class="endpointBadgeClass(sharedState.touchedEndpoints['GET /blog/{id}'])">详情 / likes</span>
        </div>
        <div class="form-grid double">
          <label><span>博客 ID</span><input v-model="forms.blog.detailId" /></label>
          <label><span>点赞列表博客 ID</span><input v-model="forms.blog.likesId" /></label>
        </div>
        <div class="button-row wrap">
          <button :disabled="isLoading('GET /blog/{id}')" @click="fetchBlogDetail()">查询博客详情</button>
          <button :disabled="isLoading('GET /blog/likes/{id}')" @click="fetchBlogLikes()">查询点赞列表</button>
          <button :disabled="isLoading('PUT /blog/like/{id}')" @click="toggleBlogLike(forms.blog.detailId)">切换点赞</button>
        </div>
        <pre class="json-box">{{ JSON.stringify(sharedState.blogDetail.value || { message: "尚未查询博客详情" }, null, 2) }}</pre>
        <pre class="json-box small">{{ JSON.stringify(sharedState.blogLikes.value, null, 2) }}</pre>
      </article>

      <article class="panel ue-washi ue-shadow ukiyo-e-digital-card">
        <div class="panel-head">
          <h3>指定用户博客</h3>
          <span :class="endpointBadgeClass(sharedState.touchedEndpoints['GET /blog/of/user'])">/blog/of/user</span>
        </div>
        <div class="form-grid double">
          <label><span>用户 ID</span><input v-model="forms.blog.userId" /></label>
          <label><span>页码 current</span><input v-model="forms.blog.userCurrent" /></label>
        </div>
        <div class="button-row">
          <button :disabled="isLoading('GET /blog/of/user')" @click="fetchUserBlogs">查询指定用户博客</button>
        </div>
        <div class="simple-list">
          <div v-for="blog in sharedState.userBlogs.value" :key="`user-${blog.id}`" class="simple-row">
            <span>#{{ blog.id }} {{ blog.title }}</span>
            <div class="button-row tight">
              <button class="secondary" @click="fillBlogDetail(blog)">详情</button>
              <button class="secondary" @click="fillFollowTarget(blog.userId)">设为关注对象</button>
            </div>
          </div>
        </div>
      </article>

      <article class="panel ue-washi ue-shadow ukiyo-e-digital-card">
        <div class="panel-head">
          <h3>关注推送流</h3>
          <span :class="endpointBadgeClass(sharedState.touchedEndpoints['GET /blog/of/follow'])">/blog/of/follow</span>
        </div>
        <div class="form-grid double">
          <label><span>lastId</span><input v-model="forms.blog.followLastId" /></label>
          <label><span>offset</span><input v-model="forms.blog.followOffset" /></label>
        </div>
        <div class="button-row">
          <button :disabled="isLoading('GET /blog/of/follow')" @click="fetchFollowFeed">拉取关注流</button>
        </div>
        <pre class="json-box">{{ JSON.stringify(sharedState.followFeed.value || { message: "尚未拉取关注流" }, null, 2) }}</pre>
      </article>
    </div>
  </section>
</template>
