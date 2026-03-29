<script setup>
import { computed, ref } from "vue";
import { uploadApi } from "../services/uploadApi";
import { blogFlowState } from "../stores/blogFlow";
import { setNotice, toAssetUrl } from "../stores/appState";

const uploadRef = ref(null);
const selectedFile = ref(null);
const currentPage = ref(1);
const pageSize = 8;

const pagedImages = computed(() => {
  const start = (currentPage.value - 1) * pageSize;
  return blogFlowState.uploadedImages.value.slice(start, start + pageSize);
});

function handleFileChange(uploadFile) {
  selectedFile.value = uploadFile.raw || null;
}

function handleFileRemove() {
  selectedFile.value = null;
}

async function uploadBlogImage() {
  if (!selectedFile.value) {
    setNotice("error", "请先选择图片。");
    return;
  }

  const formData = new FormData();
  formData.append("file", selectedFile.value);

  const { data, success } = await uploadApi.uploadBlogImage(formData, {
    successMessage: "图片上传成功。",
  });
  if (success && data) {
    blogFlowState.uploadedImages.value = [
      data,
      ...blogFlowState.uploadedImages.value.filter((item) => item !== data),
    ];
    currentPage.value = 1;
    selectedFile.value = null;
    uploadRef.value?.clearFiles();
  }
}

async function deleteBlogImage(path) {
  const { success } = await uploadApi.deleteBlogImage(path, {
    successMessage: "图片已删除。",
  });
  if (success) {
    blogFlowState.uploadedImages.value =
      blogFlowState.uploadedImages.value.filter((item) => item !== path);
  }
}
</script>

<template>
  <section class="app-page">
    <ElCard class="page-panel">
      <template #header>
        <div class="page-panel__header">
          <div>
            <h2 class="page-panel__title">图片上传工作台</h2>
            <p class="page-panel__hint">上传、预览和删除统一收口，不再散落在各页内部。</p>
          </div>
          <ElTag effect="plain">共 {{ blogFlowState.uploadedImages.value.length }} 张</ElTag>
        </div>
      </template>

      <ElUpload
        ref="uploadRef"
        drag
        :auto-upload="false"
        :limit="1"
        accept="image/*"
        :on-change="handleFileChange"
        :on-remove="handleFileRemove"
      >
        <div class="stat-card__desc">
          将图片拖到这里，或点击选择文件
        </div>
      </ElUpload>

      <div class="page-actions" style="margin-top: 16px;">
        <ElButton type="primary" @click="uploadBlogImage">上传图片</ElButton>
      </div>
    </ElCard>

    <ElCard class="page-panel">
      <template #header>
        <div class="page-panel__header">
          <div>
            <h3 class="page-panel__title">已上传图片</h3>
            <p class="page-panel__hint">表格和缩略图统一展示上传结果。</p>
          </div>
        </div>
      </template>

      <ElTable :data="pagedImages" border stripe>
        <ElTableColumn label="缩略图" width="110">
          <template #default="{ row }">
            <div class="thumb-image">
              <ElImage :src="toAssetUrl(row)" fit="cover" />
            </div>
          </template>
        </ElTableColumn>
        <ElTableColumn label="路径" min-width="360">
          <template #default="{ row }">
            {{ row }}
          </template>
        </ElTableColumn>
        <ElTableColumn label="操作" width="120">
          <template #default="{ row }">
            <ElButton link type="danger" @click="deleteBlogImage(row)">
              删除
            </ElButton>
          </template>
        </ElTableColumn>
        <template #empty>
          <ElEmpty description="暂无上传图片记录" />
        </template>
      </ElTable>

      <div class="page-table-footer">
        <ElPagination
          :current-page="currentPage"
          :page-size="pageSize"
          :total="blogFlowState.uploadedImages.value.length"
          layout="prev, pager, next"
          @current-change="currentPage = $event"
        />
      </div>
    </ElCard>
  </section>
</template>
