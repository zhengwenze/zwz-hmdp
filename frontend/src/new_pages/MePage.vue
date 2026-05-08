<script setup>
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { userApi } from "../services/userApi";
import { voucherApi } from "../services/voucherApi";
import { setNotice } from "../stores/appState";
import {
  buildRedirectPath,
  clearSession,
  isAuthenticated,
  sessionState,
} from "../stores/session";
import { formatDateTime, formatPrice, formatVoucherWindow } from "../utils/view";

const router = useRouter();
const signCount = ref("--");
const signCalendar = ref(null);
const nicknameDialogVisible = ref(false);
const newNickname = ref("");
const myVouchers = ref([]);
const myVouchersLoading = ref(false);

const userId = computed(() => sessionState.currentUser.value?.id);
const myVoucherCount = computed(() => myVouchers.value.length);

function voucherExpireText(voucher) {
  return voucher.endTime ? formatDateTime(voucher.endTime) : "长期有效";
}

function voucherStatusText(voucher) {
  if (voucher.useTime) {
    return "已核销";
  }
  if (voucher.endTime && new Date(voucher.endTime).getTime() < Date.now()) {
    return "已过期";
  }
  return "可使用";
}

function normalizeSignCalendar(data) {
  if (!data) {
    return null;
  }

  const daysInMonth = Number(data.daysInMonth);
  if (!Number.isInteger(daysInMonth) || daysInMonth <= 0) {
    return null;
  }

  return {
    year: data.year,
    month: data.month,
    daysInMonth,
    signedDays: Array.isArray(data.signedDays) ? data.signedDays : [],
  };
}

async function loadMe() {
  if (!isAuthenticated()) {
    signCount.value = "--";
    signCalendar.value = null;
    myVouchers.value = [];
    sessionState.currentUser.value = null;
    return;
  }

  const meResult = await userApi.fetchMe({ silentError: true });
  if (!meResult.success || !meResult.data?.id) {
    return;
  }

  sessionState.currentUser.value = meResult.data;

  await Promise.all([
    userApi.fetchSignCount({
      silentError: true,
      onSuccess: (data) => {
        signCount.value = data ?? "--";
      },
    }),
    userApi.fetchSignCalendar({
      silentError: true,
      onSuccess: (data) => {
        signCalendar.value = normalizeSignCalendar(data);
      },
    }),
    loadMyVouchers(),
  ]);
}

async function loadMyVouchers(showMessage = false) {
  if (!isAuthenticated()) {
    myVouchers.value = [];
    return;
  }
  myVouchersLoading.value = true;
  const { data, success } = await voucherApi.fetchMine({
    silentError: !showMessage,
    successMessage: showMessage ? "我的优惠券已刷新。" : "",
  });
  myVouchersLoading.value = false;

  if (success) {
    myVouchers.value = Array.isArray(data) ? data : [];
  }
}

function jumpToLogin() {
  router.push(buildRedirectPath("/me"));
}

async function signToday() {
  if (!isAuthenticated()) {
    jumpToLogin();
    return;
  }
  const { success } = await userApi.sign({ successMessage: "签到成功。" });
  if (success) {
    await loadMe();
  }
}

function openNicknameEditor() {
  newNickname.value = sessionState.currentUser.value?.nickName || "";
  nicknameDialogVisible.value = true;
}

async function submitNicknameUpdate() {
  if (!isAuthenticated()) {
    jumpToLogin();
    return;
  }

  const trimmedNickname = newNickname.value.trim();
  const currentNickname = (
    sessionState.currentUser.value?.nickName || ""
  ).trim();

  if (!trimmedNickname) {
    setNotice("error", "昵称不能为空");
    return;
  }
  if (trimmedNickname.length < 2 || trimmedNickname.length > 20) {
    setNotice("error", "昵称长度应为 2-20 个字符");
    return;
  }
  if (trimmedNickname === currentNickname) {
    setNotice("error", "新昵称不能与当前昵称相同");
    return;
  }

  const { success } = await userApi.updateNickName(trimmedNickname, {
    successMessage: "昵称修改成功。",
  });
  if (success) {
    sessionState.currentUser.value = {
      ...sessionState.currentUser.value,
      nickName: trimmedNickname,
    };
    nicknameDialogVisible.value = false;
    await loadMe();
  }
}

async function handleLogout() {
  await userApi.logout({ silentError: true });
  clearSession("已退出");
  signCount.value = "--";
  signCalendar.value = null;
  myVouchers.value = [];
  router.push("/login");
}

onMounted(loadMe);
</script>

<template>
  <section class="app-page">
    <ElCard class="page-panel">
      <template #header>
        <div class="page-panel__header">
          <div>
            <h2 class="page-panel__title">用户信息</h2>
          </div>
          <div class="page-actions">
            <ElButton
              v-if="!isAuthenticated()"
              type="primary"
              @click="jumpToLogin"
            >
              去登录
            </ElButton>
            <template v-else>
              <ElButton type="primary" @click="signToday">今日签到</ElButton>
              <ElButton type="info" plain @click="openNicknameEditor">
                修改昵称
              </ElButton>
              <ElButton type="info" plain @click="handleLogout"
                >退出登录</ElButton
              >
            </template>
          </div>
        </div>
      </template>

      <ElEmpty
        v-if="!isAuthenticated()"
        description="当前未登录，请先登录后再查看用户信息。"
      />

      <ElDescriptions v-else :column="2" border>
        <ElDescriptionsItem label="用户 ID">
          {{ userId ?? "--" }}
        </ElDescriptionsItem>
        <ElDescriptionsItem label="昵称">
          {{ sessionState.currentUser.value?.nickName || "--" }}
        </ElDescriptionsItem>
        <ElDescriptionsItem label="手机号">
          {{ sessionState.currentUser.value?.phone || "--" }}
        </ElDescriptionsItem>
        <ElDescriptionsItem label="连续签到">
          {{ signCount }}
        </ElDescriptionsItem>
      </ElDescriptions>
    </ElCard>

    <ElCard class="page-panel">
      <template #header>
        <div class="page-panel__header">
          <div>
            <h3 class="page-panel__title">我的优惠卷</h3>
            <p class="page-panel__hint">
              展示当前账号已拥有的全部优惠券和所属店铺。
            </p>
          </div>
          <div class="page-actions">
            <ElTag effect="plain">{{ myVoucherCount }} 张</ElTag>
            <ElButton
              v-if="isAuthenticated()"
              type="primary"
              plain
              :loading="myVouchersLoading"
              @click="loadMyVouchers(true)"
            >
              刷新
            </ElButton>
          </div>
        </div>
      </template>

      <ElEmpty
        v-if="!isAuthenticated()"
        description="当前未登录，请先登录后查看我的优惠券。"
      />

      <ElTable
        v-else
        v-loading="myVouchersLoading"
        :data="myVouchers"
        border
        stripe
      >
        <ElTableColumn prop="voucherId" label="券 ID" width="96" />
        <ElTableColumn prop="title" label="优惠券" min-width="150" />
        <ElTableColumn label="类型" width="100">
          <template #default="{ row }">
            <ElTag :type="row.type === 1 ? 'danger' : 'info'" effect="plain">
              {{ row.type === 1 ? "秒杀券" : "普通券" }}
            </ElTag>
          </template>
        </ElTableColumn>
        <ElTableColumn label="所属店铺" min-width="160">
          <template #default="{ row }">
            {{ row.shopName || `店铺 ${row.shopId || "--"}` }}
          </template>
        </ElTableColumn>
        <ElTableColumn label="支付 / 抵扣" min-width="150">
          <template #default="{ row }">
            {{ formatPrice(row.payValue) }} / {{ formatPrice(row.actualValue) }}
          </template>
        </ElTableColumn>
        <ElTableColumn label="有效期" min-width="220">
          <template #default="{ row }">
            {{ formatVoucherWindow(row) }}
          </template>
        </ElTableColumn>
        <ElTableColumn label="过期时间" min-width="150">
          <template #default="{ row }">
            {{ voucherExpireText(row) }}
          </template>
        </ElTableColumn>
        <ElTableColumn label="获得时间" min-width="150">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </ElTableColumn>
        <ElTableColumn label="状态" width="100">
          <template #default="{ row }">
            <ElTag
              :type="voucherStatusText(row) === '可使用' ? 'success' : 'info'"
              effect="plain"
            >
              {{ voucherStatusText(row) }}
            </ElTag>
          </template>
        </ElTableColumn>
        <template #empty>
          <ElEmpty description="暂无已拥有的优惠券。" />
        </template>
      </ElTable>
    </ElCard>

    <ElCard v-if="signCalendar" class="page-panel">
      <template #header>
        <div class="page-panel__header">
          <div>
            <h3 class="page-panel__title">
              {{ signCalendar.year }} 年 {{ signCalendar.month }} 月签到日历
            </h3>
          </div>
        </div>
      </template>

      <div class="sign-calendar">
        <div class="sign-calendar__header">
          <span
            v-for="day in signCalendar.daysInMonth"
            :key="day"
            class="sign-calendar__day"
            :class="{ 'is-signed': signCalendar.signedDays.includes(day) }"
          >
            {{ day }}
          </span>
        </div>
        <div class="sign-calendar__legend">
          <span class="legend-item"
            ><span class="legend-dot"></span> 未签到</span
          >
          <span class="legend-item"
            ><span class="legend-dot is-signed"></span> 已签到</span
          >
        </div>
      </div>
    </ElCard>

    <ElDialog v-model="nicknameDialogVisible" title="修改昵称" width="420px">
      <ElForm label-position="top">
        <ElFormItem label="新昵称">
          <ElInput
            v-model="newNickname"
            maxlength="32"
            placeholder="请输入新昵称"
            @keyup.enter="submitNicknameUpdate"
          />
        </ElFormItem>
      </ElForm>

      <template #footer>
        <div class="page-actions">
          <ElButton @click="nicknameDialogVisible = false">取消</ElButton>
          <ElButton type="primary" @click="submitNicknameUpdate">
            保存昵称
          </ElButton>
        </div>
      </template>
    </ElDialog>
  </section>
</template>

<style scoped>
.sign-calendar {
  padding: 8px 0;
}

.sign-calendar__header {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.sign-calendar__day {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: #f4f4f5;
  color: #909399;
  font-size: 14px;
}

.sign-calendar__day.is-signed {
  background: #67c23a;
  color: #ffffff;
}

.sign-calendar__legend {
  display: flex;
  gap: 16px;
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid #f4f4f5;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #606266;
}

.legend-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #f4f4f5;
}

.legend-dot.is-signed {
  background: #67c23a;
}
</style>
