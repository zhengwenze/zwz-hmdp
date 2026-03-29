<script setup>
import { reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { blogFlowState } from "../stores/blogFlow";
import { shopApi } from "../services/shopApi";
import { blogApi } from "../services/blogApi";
import { uploadApi } from "../services/uploadApi";
import { setNotice } from "../stores/appState";
import { toAssetUrl } from "../stores/appState";
import { historyState, rememberShop } from "../stores/historyState";

const router = useRouter();

const selectedUploadFile = ref(null);
const shopSearch = ref("");
const shopCandidates = ref([]);

const form = reactive({
  title: blogFlowState.draft.title,
  content: blogFlowState.draft.content,
});

function onUploadFileChange(event) {
  const [file] = event.target.files || [];
  selectedUploadFile.value = file || null;
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
  }
}

async function removeImage(image) {
  const { success } = await uploadApi.deleteBlogImage(image, {
    successMessage: "图片已删除。",
  });
  if (success) {
    blogFlowState.draft.images = blogFlowState.draft.images.filter((item) => item !== image);
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
}

async function publishBlog() {
  if (!form.title.trim() || !form.content.trim()) {
    setNotice("error", "标题和正文不能为空。");
    return;
  }
  if (!blogFlowState.draft.shopId) {
    setNotice("error", "请先关联一个商铺。");
    return;
  }

  const { success } = await blogApi.create(
    {
      title: form.title,
      content: form.content,
      shopId: Number(blogFlowState.draft.shopId),
      images: blogFlowState.draft.images.join(","),
    },
    {
      successMessage: "笔记发布成功。",
    },
  );

  if (success) {
    form.title = "";
    form.content = "";
    blogFlowState.draft.title = "";
    blogFlowState.draft.content = "";
    blogFlowState.draft.shopId = "";
    blogFlowState.draft.shopName = "";
    blogFlowState.draft.images = [];
    router.push("/me");
  }
}
</script>

<template>
  <section class="consumer-page">
    <article class="panel ue-washi ue-shadow">
      <div class="panel-head">
        <h2>发布笔记</h2>
        <button class="secondary" @click="router.back()">取消</button>
      </div>

      <div class="form-grid single">
        <label>
          <span>标题</span>
          <input
            v-model="form.title"
            placeholder="先写一个别人愿意点开的标题"
            @input="blogFlowState.draft.title = form.title"
          />
        </label>
        <label>
          <span>正文</span>
          <textarea
            v-model="form.content"
            rows="8"
            placeholder="先写亮点，再写环境、味道和是否值得去"
            @input="blogFlowState.draft.content = form.content"
          />
        </label>
      </div>

      <div class="button-row wrap">
        <input type="file" accept="image/*" @change="onUploadFileChange" />
        <button @click="uploadImage">上传图片</button>
      </div>

      <div class="image-strip">
        <article
          v-for="image in blogFlowState.draft.images"
          :key="image"
          class="upload-card"
        >
          <img :src="toAssetUrl(image)" :alt="image" />
          <div class="button-row tight">
            <button class="secondary" @click="removeImage(image)">删除</button>
          </div>
        </article>
      </div>
    </article>

    <article class="panel ue-washi ue-shadow">
      <div class="panel-head">
        <h3>关联商铺</h3>
        <span class="status-pill muted">{{ blogFlowState.draft.shopName || "未选择" }}</span>
      </div>
      <div v-if="historyState.recentShops.value.length" class="history-chip-list">
        <button
          v-for="shop in historyState.recentShops.value.slice(0, 4)"
          :key="shop.id"
          class="secondary history-chip-button"
          @click="selectShop(shop)"
        >
          {{ shop.name }}
        </button>
      </div>
      <div class="button-row wrap">
        <input v-model="shopSearch" placeholder="输入商铺名称搜索" @keyup.enter="searchShops" />
        <button @click="searchShops">搜索</button>
      </div>
      <div class="simple-list">
        <button
          v-for="shop in shopCandidates"
          :key="shop.id"
          class="secondary consumer-list-button"
          @click="selectShop(shop)"
        >
          <strong>{{ shop.name }}</strong>
          <span>{{ shop.area }}</span>
        </button>
      </div>
    </article>

    <div class="button-row wrap">
      <button class="accent" @click="publishBlog">发布笔记</button>
      <span class="helper">草稿会自动保存在本地，刷新页面也不会丢。</span>
    </div>
  </section>
</template>
