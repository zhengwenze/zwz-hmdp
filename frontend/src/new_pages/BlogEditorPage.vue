<script setup>
import { ref } from "vue";
import { useRouter } from "vue-router";
import { blogFlowState } from "../stores/blogFlow";
import { shopApi } from "../services/shopApi";
import { blogApi } from "../services/blogApi";
import { uploadApi } from "../services/uploadApi";
import { setNotice, toAssetUrl } from "../stores/appState";
import { historyState, rememberShop } from "../stores/historyState";

const router = useRouter();

const uploadRef = ref(null);
const selectedUploadFile = ref(null);
const shopSearch = ref("");
const shopCandidates = ref([]);
const shopDialogVisible = ref(false);

function handleUploadChange(uploadFile) {
  selectedUploadFile.value = uploadFile.raw || null;
}

function handleUploadRemove() {
  selectedUploadFile.value = null;
}

async function uploadImage() {
  if (!selectedUploadFile.value) {
    setNotice("error", "请先选择一张图片。");
    return;
  }

  const formData = new FormData();
  formData.append("file", selectedUploadFile.value);

  const { data, success } = await uploadApi.uploadBlogImage(formData, {
    successMessage: "图片上传成功。",
  });

  if (success && data) {
    blogFlowState.draft.images = [...blogFlowState.draft.images, data];
    blogFlowState.uploadedImages.value = [
      data,
      ...blogFlowState.uploadedImages.value.filter((item) => item !== data),
    ];
    selectedUploadFile.value = null;
    uploadRef.value?.clearFiles();
  }
}

async function removeImage(image) {
  const { success } = await uploadApi.deleteBlogImage(image, {
    successMessage: "图片已删除。",
  });
  if (success) {
    blogFlowState.draft.images = blogFlowState.draft.images.filter(
      (item) => item !== image,
    );
    blogFlowState.uploadedImages.value = blogFlowState.uploadedImages.value.filter(
      (item) => item !== image,
    );
  }
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
  blogFlowState.draft.shopId = String(shop.id);
  blogFlowState.draft.shopName = shop.name;
  rememberShop(shop);
  shopDialogVisible.value = false;
}

async function publishBlog() {
  if (!blogFlowState.draft.title.trim() || !blogFlowState.draft.content.trim()) {
    setNotice("error", "标题和正文不能为空。");
    return;
  }
  if (!blogFlowState.draft.shopId) {
    setNotice("error", "请先关联一个商铺。");
    return;
  }

  const { success } = await blogApi.create(
    {
      title: blogFlowState.draft.title,
      content: blogFlowState.draft.content,
      shopId: Number(blogFlowState.draft.shopId),
      images: blogFlowState.draft.images.join(","),
    },
    {
      successMessage: "笔记发布成功。",
    },
  );

  if (success) {
    blogFlowState.draft.title = "";
    blogFlowState.draft.content = "";
    blogFlowState.draft.shopId = "";
    blogFlowState.draft.shopName = "";
    blogFlowState.draft.images = [];
    router.push("/blog");
  }
}
</script>

<template>
  <section class="app-page">
    <ElCard class="page-panel">
      <template #header>
        <div class="page-panel__header">
          <div>
            <h2 class="page-panel__title">独立笔记编辑页</h2>
            <p class="page-panel__hint">适合处理图片上传、选店和完整草稿编辑。</p>
          </div>
          <div class="page-actions">
            <ElButton @click="router.push('/blog')">返回笔记页</ElButton>
            <ElButton type="primary" @click="publishBlog">发布笔记</ElButton>
          </div>
        </div>
      </template>

      <ElForm label-position="top">
        <ElFormItem label="标题">
          <ElInput
            v-model="blogFlowState.draft.title"
            placeholder="先写一个别人愿意点开的标题"
          />
        </ElFormItem>
        <ElFormItem label="正文">
          <ElInput
            v-model="blogFlowState.draft.content"
            type="textarea"
            :rows="8"
            placeholder="先写亮点，再写环境、味道和是否值得去"
          />
        </ElFormItem>
      </ElForm>
    </ElCard>

    <div class="page-grid-2">
      <ElCard class="page-panel">
        <template #header>
          <div class="page-panel__header">
            <div>
              <h3 class="page-panel__title">关联商铺</h3>
              <p class="page-panel__hint">先选商铺，再发布笔记。</p>
            </div>
            <ElTag effect="plain">{{ blogFlowState.draft.shopName || "未选择" }}</ElTag>
          </div>
        </template>

        <ElSpace wrap>
          <ElTag
            v-for="shop in historyState.recentShops.value.slice(0, 6)"
            :key="shop.id"
            effect="plain"
            @click="selectShop(shop)"
          >
            {{ shop.name }}
          </ElTag>
        </ElSpace>

        <div class="page-actions" style="margin-top: 16px;">
          <ElButton type="primary" plain @click="shopDialogVisible = true">
            搜索并选择商铺
          </ElButton>
        </div>
      </ElCard>

      <ElCard class="page-panel">
        <template #header>
          <div class="page-panel__header">
            <div>
              <h3 class="page-panel__title">图片上传</h3>
              <p class="page-panel__hint">上传成功后自动进入当前草稿。</p>
            </div>
          </div>
        </template>

        <ElUpload
          ref="uploadRef"
          drag
          :auto-upload="false"
          :limit="1"
          accept="image/*"
          :on-change="handleUploadChange"
          :on-remove="handleUploadRemove"
        >
          <div class="stat-card__desc">
            将图片拖到这里，或点击选择文件
          </div>
        </ElUpload>

        <div class="page-actions" style="margin-top: 16px;">
          <ElButton type="primary" @click="uploadImage">上传到草稿</ElButton>
        </div>
      </ElCard>
    </div>

    <ElCard class="page-panel">
      <template #header>
        <div class="page-panel__header">
          <div>
            <h3 class="page-panel__title">草稿图片</h3>
            <p class="page-panel__hint">当前草稿已关联的所有图片。</p>
          </div>
        </div>
      </template>

      <div v-if="blogFlowState.draft.images.length" class="image-list-grid">
        <div
          v-for="image in blogFlowState.draft.images"
          :key="image"
          class="image-card"
        >
          <div class="detail-cover">
            <ElImage :src="toAssetUrl(image)" fit="cover" />
          </div>
          <div class="image-card__path">{{ image }}</div>
          <ElButton type="danger" plain @click="removeImage(image)">
            删除
          </ElButton>
        </div>
      </div>
      <ElEmpty v-else description="当前草稿还没有图片。" />
    </ElCard>

    <ElDialog
      v-model="shopDialogVisible"
      title="选择商铺"
      width="760px"
    >
      <div class="dialog-search">
        <div class="filter-actions">
          <ElInput
            v-model="shopSearch"
            placeholder="输入商铺名称搜索"
            @keyup.enter="searchShops"
          />
          <ElButton type="primary" @click="searchShops">搜索</ElButton>
        </div>

        <ElTable :data="shopCandidates" border stripe>
          <ElTableColumn prop="id" label="商铺 ID" width="100" />
          <ElTableColumn prop="name" label="商铺名" min-width="200" />
          <ElTableColumn prop="area" label="商圈" min-width="160" />
          <ElTableColumn label="操作" width="100">
            <template #default="{ row }">
              <ElButton link type="primary" @click="selectShop(row)">
                选择
              </ElButton>
            </template>
          </ElTableColumn>
        </ElTable>
      </div>
    </ElDialog>
  </section>
</template>
