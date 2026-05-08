<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { shopApi } from "../services/shopApi";
import { firstImage } from "../utils/view";

const router = useRouter();

const form = reactive({
  name: "",
  typeId: "",
  images: "",
  area: "",
  address: "",
  x: "",
  y: "",
  avgPrice: undefined,
  score: 5,
  openHours: "",
});

const formRef = ref(null);
const shopTypes = ref([]);
const loading = ref(false);
const previewImage = computed(() => firstImage(normalizeImageUrls(form.images)));

const rules = {
  name: [{ required: true, message: "请输入商铺名称", trigger: "blur" }],
  typeId: [{ required: true, message: "请选择商铺分类", trigger: "change" }],
  images: [
    { required: true, message: "请输入图片链接", trigger: "blur" },
    {
      validator: validateImageUrls,
      trigger: "blur",
    },
  ],
  address: [{ required: true, message: "请输入商铺地址", trigger: "blur" }],
  x: [{ required: true, message: "请输入经度", trigger: "blur" }],
  y: [{ required: true, message: "请输入纬度", trigger: "blur" }],
};

function normalizeImageUrls(value) {
  return value
    .split(",")
    .map((image) => image.trim())
    .filter(Boolean)
    .map((url) => {
      if (url.startsWith("//")) {
        return `https:${url}`;
      }
      if (url.startsWith("http://") || url.startsWith("https://")) {
        return url;
      }
      return `https://${url}`;
    })
    .join(",");
}

function validateImageUrls(_rule, value, callback) {
  const urls = value
    .split(",")
    .map((image) => image.trim())
    .filter(Boolean);

  if (!urls.length) {
    callback(new Error("请输入图片链接"));
    return;
  }

  const invalid = urls.some((url) => {
    const normalized = url.startsWith("//") ? `https:${url}` : url;
    if (normalized.startsWith("http://") || normalized.startsWith("https://")) {
      return false;
    }
    return normalized.startsWith("/") || !normalized.includes(".");
  });

  if (invalid) {
    callback(new Error("请输入网络图片链接，例如 https://cdn.example.com/shop.jpg"));
    return;
  }

  callback();
}

async function loadShopTypes() {
  const { data, success } = await shopApi.fetchTypes({ silentError: true });
  if (success) {
    shopTypes.value = Array.isArray(data) ? data : [];
    if (!form.typeId && shopTypes.value.length) {
      form.typeId = String(shopTypes.value[0].id);
    }
  }
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) {
    return;
  }

  loading.value = true;

  const payload = {
    name: form.name.trim(),
    typeId: Number(form.typeId),
    images: normalizeImageUrls(form.images),
    area: form.area.trim() || undefined,
    address: form.address.trim(),
    x: Number(form.x),
    y: Number(form.y),
    avgPrice: form.avgPrice === undefined ? undefined : Number(form.avgPrice),
    score: Math.round(Number(form.score || 0) * 10),
    openHours: form.openHours.trim() || undefined,
  };

  const { success, data } = await shopApi.create(payload, {
    successMessage: "商铺创建成功！",
  });

  loading.value = false;

  if (success) {
    router.push({
      name: "shop-list",
      query: {
        tab: "name",
        name: data?.name || payload.name,
        nameCurrent: "1",
      },
    });
  }
}

onMounted(loadShopTypes);
</script>

<template>
  <section class="app-page">
    <ElCard class="page-panel">
      <template #header>
        <div class="page-panel__header">
          <div>
            <h2 class="page-panel__title">创建商铺</h2>
          </div>
        </div>
      </template>

      <ElForm
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
        label-position="left"
        style="max-width: 600px"
      >
        <ElFormItem
          label="商铺名称"
          prop="name"
        >
          <ElInput
            v-model="form.name"
            placeholder="请输入商铺名称"
            clearable
            maxlength="100"
            show-word-limit
          />
        </ElFormItem>

        <ElFormItem
          label="商铺分类"
          prop="typeId"
        >
          <ElSelect
            v-model="form.typeId"
            placeholder="请选择商铺分类"
            style="width: 100%"
          >
            <ElOption
              v-for="type in shopTypes"
              :key="type.id"
              :label="type.name"
              :value="type.id"
            />
          </ElSelect>
        </ElFormItem>

        <ElFormItem label="图片链接" prop="images">
          <ElInput
            v-model="form.images"
            placeholder="https://cdn.example.com/shop.jpg"
            clearable
            maxlength="1024"
          />
        </ElFormItem>

        <ElFormItem v-if="previewImage" label="封面预览">
          <ElImage
            class="cover-preview"
            :src="previewImage"
            fit="cover"
          />
        </ElFormItem>

        <ElFormItem label="商圈">
          <ElInput
            v-model="form.area"
            placeholder="例如：运河上街"
            clearable
            maxlength="128"
          />
        </ElFormItem>

        <ElFormItem label="商铺地址" prop="address">
          <ElInput
            v-model="form.address"
            placeholder="请输入商铺地址"
            clearable
            maxlength="255"
          />
        </ElFormItem>

        <ElFormItem label="经度" prop="x">
          <ElInput
            v-model="form.x"
            placeholder="例如：120.149192"
            clearable
            type="number"
          />
        </ElFormItem>

        <ElFormItem label="纬度" prop="y">
          <ElInput
            v-model="form.y"
            placeholder="例如：30.316078"
            clearable
            type="number"
          />
        </ElFormItem>

        <ElFormItem label="人均价格">
          <ElInputNumber
            v-model="form.avgPrice"
            :min="0"
            :precision="0"
            :controls="false"
            style="width: 100%"
          />
        </ElFormItem>

        <ElFormItem label="评分">
          <ElInputNumber
            v-model="form.score"
            :min="0"
            :max="5"
            :step="0.1"
            :precision="1"
            style="width: 100%"
          />
        </ElFormItem>

        <ElFormItem label="营业时间">
          <ElInput
            v-model="form.openHours"
            placeholder="例如：10:00-22:00"
            clearable
            maxlength="32"
          />
        </ElFormItem>

        <ElFormItem>
          <ElButton type="primary" @click="handleSubmit" :loading="loading">
            创建商铺
          </ElButton>
          <ElButton @click="router.back()"> 取消 </ElButton>
        </ElFormItem>
      </ElForm>
    </ElCard>
  </section>
</template>

<style scoped>
.app-page {
  padding: 24px;
}

.page-panel {
  max-width: 800px;
}

.page-panel__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.page-panel__title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}

:deep(.el-form-item__label) {
  font-weight: 500;
}

.cover-preview {
  width: 160px;
  height: 104px;
  border-radius: 6px;
}
</style>
