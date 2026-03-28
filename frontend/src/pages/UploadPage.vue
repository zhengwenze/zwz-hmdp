<script setup>
import { ref } from "vue";
import {
  sharedState,
  send,
  isLoading,
  endpointBadgeClass,
  toAssetUrl,
  setNotice,
} from "../stores/sharedState";

const selectedUploadFile = ref(null);

function onUploadFileChange(event) {
  const [file] = event.target.files || [];
  selectedUploadFile.value = file || null;
}

async function uploadBlogImage() {
  if (!selectedUploadFile.value) {
    setNotice("error", "请先选择一个图片文件。");
    return;
  }

  const formData = new FormData();
  formData.append("file", selectedUploadFile.value);

  await send(
    "POST /upload/blog",
    { method: "POST", path: "/upload/blog", formData },
    {
      successMessage: "图片上传成功。",
      onSuccess: (data) => {
        if (data) {
          sharedState.uploadedImages.value = [
            data,
            ...sharedState.uploadedImages.value.filter((item) => item !== data),
          ];
        }
      },
    },
  );
}

async function deleteBlogImage(path) {
  await send(
    "GET /upload/blog/delete",
    { method: "GET", path: "/upload/blog/delete", query: { name: path } },
    {
      successMessage: "图片删除成功。",
      onSuccess: () => {
        sharedState.uploadedImages.value = sharedState.uploadedImages.value.filter(
          (item) => item !== path,
        );
      },
    },
  );
}
</script>

<template>
  <section id="upload" class="module-section">
    <div class="section-title">
      <div>
        <p class="eyebrow">Upload APIs</p>
        <h2>图片上传</h2>
      </div>
      <span class="section-hint">上传返回相对路径，删除时直接传回该路径</span>
    </div>

    <div class="card-grid two">
      <article class="panel ue-washi ue-shadow ukiyo-e-digital-card">
        <div class="panel-head">
          <h3>上传博客图片</h3>
          <span :class="endpointBadgeClass(sharedState.touchedEndpoints['POST /upload/blog'])">POST /upload/blog</span>
        </div>
        <div class="form-grid single">
          <label>
            <span>选择文件</span>
            <input type="file" accept="image/*" @change="onUploadFileChange" />
          </label>
        </div>
        <div class="button-row wrap">
          <button :disabled="isLoading('POST /upload/blog')" @click="uploadBlogImage">上传图片</button>
        </div>
        <p class="helper">接口使用 `multipart/form-data`，字段名固定为 `file`。</p>
      </article>

      <article class="panel ue-washi ue-shadow ukiyo-e-digital-card">
        <div class="panel-head">
          <h3>已上传图片与删除</h3>
          <span :class="endpointBadgeClass(sharedState.touchedEndpoints['GET /upload/blog/delete'])">GET /upload/blog/delete</span>
        </div>
        <div class="upload-list">
          <article v-for="path in sharedState.uploadedImages.value" :key="path" class="upload-card">
            <img :src="toAssetUrl(path)" :alt="path" />
            <div>
              <strong>{{ path }}</strong>
              <div class="button-row tight">
                <button class="secondary" @click="deleteBlogImage(path)">删除图片</button>
              </div>
            </div>
          </article>
        </div>
      </article>
    </div>
  </section>
</template>
