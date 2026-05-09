<script setup>
import { onMounted, ref } from "vue";

const loading = ref(false);
const nearbyShops = ref([]);

// TODO: 后续实现基于 GEO 的附近商铺查询
// 1. 获取当前用户位置（浏览器 Geolocation API 或手动输入坐标）
// 2. 调用后端 /shop/nearby 接口查询附近商铺
// 3. 展示商铺列表（距离、名称、评分、图片等）
// 4. 支持按距离排序、筛选

async function loadNearbyShops() {
  loading.value = true;
  try {
    // TODO: 实现附近商铺查询逻辑
    // const { data, success } = await shopApi.fetchNearby({ x, y, distance });
    // if (success) {
    //   nearbyShops.value = data || [];
    // }
    console.log("待实现：附近商铺查询功能");
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  loadNearbyShops();
});
</script>

<template>
  <div class="page-container">
    <header class="page-header">
      <h1 class="page-title">附近商铺</h1>
      <p class="page-description">
        基于地理位置查询附近的商铺，支持按距离排序和筛选。
      </p>
    </header>

    <main class="page-main">
      <!-- 占位提示 -->
      <ElCard class="placeholder-card" shadow="hover">
        <ElEmpty description="功能开发中，敬请期待">
          <template #image>
            <ElIcon :size="64" color="var(--el-color-info)">
              <Location />
            </ElIcon>
          </template>
          <div class="placeholder-tips">
            <p>
              <ElIcon><Location /></ElIcon>
              即将支持的功能：
            </p>
            <ul>
              <li>基于 GEO 地理位置查询附近商铺</li>
              <li>按距离排序和范围筛选</li>
              <li>显示商铺距离、评分、人均消费等信息</li>
              <li>支持一键导航到商铺</li>
            </ul>
          </div>
        </ElEmpty>
      </ElCard>

      <!-- 后续实现的商铺列表区域 -->
      <!-- <ElCard v-loading="loading" class="shops-card" shadow="never">
        <template #header>
          <div class="card-header">
            <span>附近商铺</span>
            <ElInput
              v-model="searchKeyword"
              placeholder="搜索商铺名称"
              clearable
              style="width: 200px"
            >
              <template #prefix>
                <ElIcon><Search /></ElIcon>
              </template>
            </ElInput>
          </div>
        </template>

        <ElRow :gutter="16">
          <ElCol
            v-for="shop in nearbyShops"
            :key="shop.id"
            :xs="24"
            :sm="12"
            :md="8"
            :lg="6"
          >
            <ElCard :body-style="{ padding: '0px' }" shadow="hover">
              <ElImage
                :src="shop.icon || '/default-shop.png'"
                fit="cover"
                style="width: 100%; height: 160px"
              />
              <div class="shop-info">
                <h3 class="shop-name">{{ shop.name }}</h3>
                <p class="shop-distance">
                  <ElIcon><Location /></ElIcon>
                  {{ shop.distance }}m
                </p>
                <div class="shop-meta">
                  <span class="shop-score">
                    <ElIcon><Star /></ElIcon>
                    {{ shop.score }}
                  </span>
                  <span class="shop-price">￥{{ shop.avgPrice }}/人</span>
                </div>
              </div>
            </ElCard>
          </ElCol>
        </ElRow>

        <ElPagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          :page-sizes="[10, 20, 50]"
          style="margin-top: 24px; justify-content: center"
        />
      </ElCard> -->
    </main>
  </div>
</template>

<style scoped>
.page-container {
  padding: 24px;
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 24px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin: 0 0 8px 0;
}

.page-description {
  font-size: 14px;
  color: var(--el-text-color-secondary);
  margin: 0;
}

.page-main {
  min-height: 500px;
}

.placeholder-card {
  text-align: center;
}

.placeholder-tips {
  margin-top: 24px;
  text-align: left;
  max-width: 500px;
  margin-left: auto;
  margin-right: auto;
}

.placeholder-tips p {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
  margin-bottom: 12px;
  color: var(--el-color-primary);
}

.placeholder-tips ul {
  margin: 0;
  padding-left: 32px;
}

.placeholder-tips li {
  margin: 8px 0;
  color: var(--el-text-color-regular);
  line-height: 1.6;
}

.shops-card {
  margin-top: 24px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.shop-info {
  padding: 12px;
}

.shop-name {
  font-size: 16px;
  font-weight: 500;
  color: var(--el-text-color-primary);
  margin: 0 0 8px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.shop-distance {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--el-color-primary);
  margin: 0 0 8px 0;
}

.shop-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
}

.shop-score {
  display: flex;
  align-items: center;
  gap: 4px;
  color: var(--el-color-warning);
}

.shop-price {
  color: var(--el-text-color-secondary);
}
</style>
