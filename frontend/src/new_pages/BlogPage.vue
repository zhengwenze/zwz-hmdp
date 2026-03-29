<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { blogApi } from "../services/blogApi";
import { shopApi } from "../services/shopApi";
import { blogFlowState } from "../stores/blogFlow";
import { buildRedirectPath, isAuthenticated } from "../stores/session";
import {
  excerpt,
  firstImage,
  formatDateTime,
  renderRichText,
  splitImages,
} from "../utils/view";
import { historyState, rememberBlog, rememberShop } from "../stores/historyState";

const PAGE_SIZE = 10;

const router = useRouter();
const draft = blogFlowState.draft;
const shopSearch = ref("");
const shopCandidates = ref([]);
const activeTab = ref("publish");

const query = reactive({
  hotCurrent: "1",
  myCurrent: "1",
  userId: "1",
  userCurrent: "1",
  detailId: "1",
  likesId: "1",
});

const hotBlogs = ref([]);
const myBlogs = ref([]);
const userBlogs = ref([]);
const detailBlog = ref(null);
const likeUsers = ref([]);
const imageInput = ref("");

const uploadedImages = computed(() => blogFlowState.uploadedImages.value);
const hotPageCount = computed(
  () => Number(query.hotCurrent) + (hotBlogs.value.length === PAGE_SIZE ? 1 : 0),
);
const myPageCount = computed(
  () => Number(query.myCurrent) + (myBlogs.value.length === PAGE_SIZE ? 1 : 0),
);
const userPageCount = computed(
  () => Number(query.userCurrent) + (userBlogs.value.length === PAGE_SIZE ? 1 : 0),
);

function jumpToLogin() {
  router.push(buildRedirectPath("/blog"));
}

function openEditor() {
  router.push("/blog/editor");
}

function attachImage(path) {
  if (!path || draft.images.includes(path)) {
    return;
  }
  draft.images = [...draft.images, path];
}

function addManualImage() {
  if (!imageInput.value.trim()) {
    return;
  }
  attachImage(imageInput.value.trim());
  imageInput.value = "";
}

function removeDraftImage(path) {
  draft.images = draft.images.filter((item) => item !== path);
}

async function searchShops() {
  const { data, success } = await shopApi.fetchByName(
    { name: shopSearch.value.trim(), current: 1 },
    { silentError: true },
  );
  if (success) {
    shopCandidates.value = Array.isArray(data) ? data : [];
  }
}

function selectShop(shop) {
  draft.shopId = String(shop.id);
  draft.shopName = shop.name;
  rememberShop(shop);
}

async function createBlog() {
  if (!isAuthenticated()) {
    jumpToLogin();
    return;
  }

  const { success, data } = await blogApi.create(
    {
      title: draft.title,
      content: draft.content,
      shopId: Number(draft.shopId),
      images: draft.images.join(","),
    },
    { successMessage: "笔记发布成功。" },
  );

  if (success) {
    draft.title = "";
    draft.content = "";
    draft.shopId = "";
    draft.shopName = "";
    draft.images = [];
    query.detailId = String(data || query.detailId);
    activeTab.value = "detail";
    await Promise.all([fetchHotBlogs(), fetchMyBlogs()]);
    if (data) {
      await fetchBlogDetail(data);
    }
  }
}

async function fetchHotBlogs() {
  const { data, success } = await blogApi.fetchHot(query.hotCurrent, {
    silentError: true,
  });
  if (success) {
    hotBlogs.value = Array.isArray(data) ? data : [];
  }
}

async function fetchMyBlogs() {
  if (!isAuthenticated()) {
    jumpToLogin();
    return;
  }
  const { data, success } = await blogApi.fetchMy(query.myCurrent, {
    silentError: true,
  });
  if (success) {
    myBlogs.value = Array.isArray(data) ? data : [];
  }
}

async function fetchUserBlogs() {
  const { data, success } = await blogApi.fetchByUser(
    query.userId,
    query.userCurrent,
    { silentError: true },
  );
  if (success) {
    userBlogs.value = Array.isArray(data) ? data : [];
  }
}

async function fetchBlogDetail(blogId = query.detailId) {
  query.detailId = String(blogId);
  const { data, success } = await blogApi.fetchDetail(blogId, {
    silentError: true,
  });
  if (success) {
    detailBlog.value = data || null;
    rememberBlog(data);
  }
}

async function fetchBlogLikes(blogId = query.likesId) {
  query.likesId = String(blogId);
  const { data, success } = await blogApi.fetchLikes(blogId, {
    silentError: true,
  });
  if (success) {
    likeUsers.value = Array.isArray(data) ? data : [];
  }
}

async function toggleLike(blogId = query.detailId) {
  if (!isAuthenticated()) {
    jumpToLogin();
    return;
  }
  const { success } = await blogApi.toggleLike(blogId, {
    successMessage: "点赞状态已切换。",
  });
  if (success) {
    await Promise.all([
      fetchHotBlogs(),
      fetchBlogDetail(blogId),
      fetchBlogLikes(blogId),
    ]);
  }
}

function fillDetail(blog) {
  query.detailId = String(blog.id);
  query.likesId = String(blog.id);
  activeTab.value = "detail";
  fetchBlogDetail(blog.id);
  fetchBlogLikes(blog.id);
}

function changeHotPage(page) {
  query.hotCurrent = String(page);
  fetchHotBlogs();
}

function changeMyPage(page) {
  query.myCurrent = String(page);
  fetchMyBlogs();
}

function changeUserPage(page) {
  query.userCurrent = String(page);
  fetchUserBlogs();
}

onMounted(fetchHotBlogs);
</script>

<template>
  <section class="app-page">
    <ElCard class="page-panel">
      <template #header>
        <div class="page-panel__header">
          <div>
            <h2 class="page-panel__title">笔记管理</h2>
            <p class="page-panel__hint">
              通过标签页收口发布、热门、个人笔记与详情查询，避免页面失控扩张。
            </p>
          </div>
          <div class="page-actions">
            <ElButton @click="fetchHotBlogs">刷新热门</ElButton>
            <ElButton type="primary" plain @click="openEditor">
              打开独立编辑页
            </ElButton>
          </div>
        </div>
      </template>

      <ElTabs v-model="activeTab">
        <ElTabPane label="发布笔记" name="publish">
          <ElCard shadow="never" class="page-panel">
            <template #header>
              <div class="page-panel__header">
                <div>
                  <h3 class="page-panel__title">发布表单</h3>
                  <p class="page-panel__hint">快速发布走当前页，复杂编辑走独立编辑页。</p>
                </div>
                <ElTag effect="plain">{{ draft.shopName || "未选择商铺" }}</ElTag>
              </div>
            </template>

            <ElForm label-position="top">
              <ElFormItem label="标题">
                <ElInput v-model="draft.title" placeholder="输入标题" />
              </ElFormItem>
              <ElFormItem label="正文">
                <ElInput
                  v-model="draft.content"
                  type="textarea"
                  :rows="6"
                  placeholder="输入笔记内容"
                />
              </ElFormItem>
            </ElForm>

            <div class="page-grid-2">
              <ElCard shadow="never" class="page-panel">
                <template #header>
                  <div class="page-panel__header">
                    <div>
                      <h4 class="page-panel__title">图片草稿</h4>
                      <p class="page-panel__hint">支持手动路径和上传历史复用。</p>
                    </div>
                  </div>
                </template>
                <div class="filter-actions">
                  <ElInput
                    v-model="imageInput"
                    placeholder="输入图片路径"
                    @keyup.enter="addManualImage"
                  />
                  <ElButton @click="addManualImage">添加图片</ElButton>
                </div>
                <ElSpace wrap>
                  <ElTag
                    v-for="image in draft.images"
                    :key="image"
                    closable
                    @close="removeDraftImage(image)"
                  >
                    {{ image }}
                  </ElTag>
                </ElSpace>
                <ElDivider />
                <ElSpace wrap>
                  <ElTag
                    v-for="image in uploadedImages.slice(0, 8)"
                    :key="image"
                    type="info"
                    effect="plain"
                    @click="attachImage(image)"
                  >
                    复用 {{ image }}
                  </ElTag>
                </ElSpace>
              </ElCard>

              <ElCard shadow="never" class="page-panel">
                <template #header>
                  <div class="page-panel__header">
                    <div>
                      <h4 class="page-panel__title">关联商铺</h4>
                      <p class="page-panel__hint">发布前需先选择一个商铺。</p>
                    </div>
                  </div>
                </template>
                <ElSpace wrap>
                  <ElTag
                    v-for="shop in historyState.recentShops.value.slice(0, 4)"
                    :key="shop.id"
                    effect="plain"
                    @click="selectShop(shop)"
                  >
                    {{ shop.name }}
                  </ElTag>
                </ElSpace>
                <div class="filter-actions" style="margin-top: 16px;">
                  <ElInput
                    v-model="shopSearch"
                    placeholder="输入商铺名搜索"
                    @keyup.enter="searchShops"
                  />
                  <ElButton @click="searchShops">搜索商铺</ElButton>
                </div>
                <ElTable :data="shopCandidates" border stripe style="margin-top: 16px;">
                  <ElTableColumn prop="id" label="商铺 ID" width="100" />
                  <ElTableColumn prop="name" label="商铺名" min-width="160" />
                  <ElTableColumn prop="area" label="商圈" min-width="120" />
                  <ElTableColumn label="操作" width="100">
                    <template #default="{ row }">
                      <ElButton link type="primary" @click="selectShop(row)">
                        选择
                      </ElButton>
                    </template>
                  </ElTableColumn>
                </ElTable>
              </ElCard>
            </div>

            <div class="page-actions">
              <ElButton type="primary" @click="createBlog">发布笔记</ElButton>
              <ElButton @click="fetchMyBlogs">刷新我的笔记</ElButton>
              <ElButton type="info" plain @click="openEditor">进入独立编辑页</ElButton>
            </div>
          </ElCard>
        </ElTabPane>

        <ElTabPane label="热门笔记" name="hot">
          <ElCard shadow="never" class="page-panel">
            <template #header>
              <div class="page-panel__header">
                <div>
                  <h3 class="page-panel__title">热门列表</h3>
                  <p class="page-panel__hint">列表结果统一用表格展示，并按后端页码拉取。</p>
                </div>
              </div>
            </template>

            <ElForm inline label-position="top">
              <ElFormItem label="页码">
                <ElInput v-model="query.hotCurrent" style="width: 120px;" />
              </ElFormItem>
            </ElForm>

            <div class="filter-actions">
              <ElButton type="primary" @click="fetchHotBlogs">查询</ElButton>
            </div>

            <ElTable :data="hotBlogs" border stripe>
              <ElTableColumn label="封面" width="100">
                <template #default="{ row }">
                  <div class="thumb-image">
                    <ElImage v-if="firstImage(row.images)" :src="firstImage(row.images)" fit="cover" />
                  </div>
                </template>
              </ElTableColumn>
              <ElTableColumn prop="id" label="笔记 ID" width="96" />
              <ElTableColumn prop="title" label="标题" min-width="180" />
              <ElTableColumn label="作者" min-width="140">
                <template #default="{ row }">
                  {{ row.name || "匿名作者" }}
                </template>
              </ElTableColumn>
              <ElTableColumn label="摘要" min-width="220">
                <template #default="{ row }">
                  {{ excerpt(row.content) }}
                </template>
              </ElTableColumn>
              <ElTableColumn label="发布时间" min-width="180">
                <template #default="{ row }">
                  {{ formatDateTime(row.createTime) }}
                </template>
              </ElTableColumn>
              <ElTableColumn label="操作" width="160" fixed="right">
                <template #default="{ row }">
                  <ElButton link type="primary" @click="fillDetail(row)">
                    详情
                  </ElButton>
                  <ElButton link type="danger" @click="toggleLike(row.id)">
                    点赞
                  </ElButton>
                </template>
              </ElTableColumn>
              <template #empty>
                <ElEmpty description="暂无热门笔记数据" />
              </template>
            </ElTable>

            <div class="page-table-footer">
              <ElPagination
                :current-page="Number(query.hotCurrent)"
                :page-size="PAGE_SIZE"
                :page-count="Math.max(hotPageCount, 1)"
                layout="prev, pager, next"
                @current-change="changeHotPage"
              />
            </div>
          </ElCard>
        </ElTabPane>

        <ElTabPane label="我的 / 用户笔记" name="mine">
          <div class="page-grid-2">
            <ElCard shadow="never" class="page-panel">
              <template #header>
                <div class="page-panel__header">
                  <div>
                    <h3 class="page-panel__title">我的笔记</h3>
                    <p class="page-panel__hint">登录后按页查询个人发布记录。</p>
                  </div>
                </div>
              </template>

              <ElForm inline label-position="top">
                <ElFormItem label="页码">
                  <ElInput v-model="query.myCurrent" style="width: 120px;" />
                </ElFormItem>
              </ElForm>

              <div class="filter-actions">
                <ElButton type="primary" @click="fetchMyBlogs">查询我的笔记</ElButton>
              </div>

              <ElTable :data="myBlogs" border stripe>
                <ElTableColumn prop="id" label="笔记 ID" width="96" />
                <ElTableColumn prop="title" label="标题" min-width="180" />
                <ElTableColumn label="发布时间" min-width="180">
                  <template #default="{ row }">
                    {{ formatDateTime(row.createTime) }}
                  </template>
                </ElTableColumn>
                <ElTableColumn label="操作" width="100">
                  <template #default="{ row }">
                    <ElButton link type="primary" @click="fillDetail(row)">
                      详情
                    </ElButton>
                  </template>
                </ElTableColumn>
                <template #empty>
                  <ElEmpty description="暂无我的笔记数据" />
                </template>
              </ElTable>

              <div class="page-table-footer">
                <ElPagination
                  :current-page="Number(query.myCurrent)"
                  :page-size="PAGE_SIZE"
                  :page-count="Math.max(myPageCount, 1)"
                  layout="prev, pager, next"
                  @current-change="changeMyPage"
                />
              </div>
            </ElCard>

            <ElCard shadow="never" class="page-panel">
              <template #header>
                <div class="page-panel__header">
                  <div>
                    <h3 class="page-panel__title">指定用户笔记</h3>
                    <p class="page-panel__hint">按用户 ID 和页码查询公开笔记。</p>
                  </div>
                </div>
              </template>

              <ElForm inline label-position="top">
                <ElFormItem label="用户 ID">
                  <ElInput v-model="query.userId" style="width: 140px;" />
                </ElFormItem>
                <ElFormItem label="页码">
                  <ElInput v-model="query.userCurrent" style="width: 120px;" />
                </ElFormItem>
              </ElForm>

              <div class="filter-actions">
                <ElButton type="primary" @click="fetchUserBlogs">查询指定用户笔记</ElButton>
              </div>

              <ElTable :data="userBlogs" border stripe>
                <ElTableColumn prop="id" label="笔记 ID" width="96" />
                <ElTableColumn prop="title" label="标题" min-width="180" />
                <ElTableColumn label="发布时间" min-width="180">
                  <template #default="{ row }">
                    {{ formatDateTime(row.createTime) }}
                  </template>
                </ElTableColumn>
                <ElTableColumn label="操作" width="100">
                  <template #default="{ row }">
                    <ElButton link type="primary" @click="fillDetail(row)">
                      详情
                    </ElButton>
                  </template>
                </ElTableColumn>
                <template #empty>
                  <ElEmpty description="暂无指定用户笔记数据" />
                </template>
              </ElTable>

              <div class="page-table-footer">
                <ElPagination
                  :current-page="Number(query.userCurrent)"
                  :page-size="PAGE_SIZE"
                  :page-count="Math.max(userPageCount, 1)"
                  layout="prev, pager, next"
                  @current-change="changeUserPage"
                />
              </div>
            </ElCard>
          </div>
        </ElTabPane>

        <ElTabPane label="详情 / 点赞" name="detail">
          <ElCard shadow="never" class="page-panel">
            <template #header>
              <div class="page-panel__header">
                <div>
                  <h3 class="page-panel__title">详情查询</h3>
                  <p class="page-panel__hint">详情内容和点赞列表拆成统一详情区。</p>
                </div>
              </div>
            </template>

            <ElForm inline label-position="top">
              <ElFormItem label="笔记 ID">
                <ElInput v-model="query.detailId" style="width: 140px;" />
              </ElFormItem>
              <ElFormItem label="点赞列表笔记 ID">
                <ElInput v-model="query.likesId" style="width: 180px;" />
              </ElFormItem>
            </ElForm>

            <div class="filter-actions">
              <ElButton type="primary" @click="fetchBlogDetail()">查询详情</ElButton>
              <ElButton @click="fetchBlogLikes()">查询点赞列表</ElButton>
              <ElButton type="danger" plain @click="toggleLike()">切换点赞</ElButton>
            </div>

            <div v-if="detailBlog" class="app-page" style="margin-top: 20px;">
              <ElDescriptions :column="2" border>
                <ElDescriptionsItem label="标题">
                  {{ detailBlog.title }}
                </ElDescriptionsItem>
                <ElDescriptionsItem label="作者">
                  {{ detailBlog.name || `用户 ${detailBlog.userId}` }}
                </ElDescriptionsItem>
                <ElDescriptionsItem label="发布时间">
                  {{ formatDateTime(detailBlog.createTime) }}
                </ElDescriptionsItem>
                <ElDescriptionsItem label="点赞数">
                  {{ detailBlog.liked ?? 0 }}
                </ElDescriptionsItem>
              </ElDescriptions>

              <div
                v-if="splitImages(detailBlog.images).length"
                class="image-list-grid"
              >
                <div
                  v-for="image in splitImages(detailBlog.images)"
                  :key="image"
                  class="detail-cover"
                >
                  <ElImage :src="image" fit="cover" />
                </div>
              </div>

              <div class="rich-copy" v-html="renderRichText(detailBlog.content)" />
            </div>
            <ElEmpty v-else description="先查询一篇笔记详情。" />

            <ElDivider />

            <ElTable :data="likeUsers" border stripe>
              <ElTableColumn prop="id" label="用户 ID" width="100" />
              <ElTableColumn prop="nickName" label="昵称" min-width="180" />
              <template #empty>
                <ElEmpty description="暂无点赞用户数据" />
              </template>
            </ElTable>
          </ElCard>
        </ElTabPane>
      </ElTabs>
    </ElCard>
  </section>
</template>
