<script setup>
import { computed, onMounted, reactive, ref, watch } from "vue";
import {
  asBoolean,
  asNumber,
  cloneWithoutEmpty,
  createApiClient,
  prettify,
  resolveAssetUrl,
} from "./api";

const endpointCatalog = [
  { key: "POST /user/code", module: "用户", label: "发送验证码" },
  { key: "POST /user/login", module: "用户", label: "验证码登录" },
  { key: "POST /user/logout", module: "用户", label: "登出（后端未完成）" },
  { key: "GET /user/me", module: "用户", label: "当前用户" },
  { key: "GET /user/info/{id}", module: "用户", label: "用户详情" },
  { key: "GET /user/{id}", module: "用户", label: "用户基础信息" },
  { key: "POST /user/sign", module: "用户", label: "签到" },
  { key: "GET /user/sign/count", module: "用户", label: "连续签到天数" },
  { key: "GET /shop/{id}", module: "商铺", label: "商铺详情" },
  { key: "POST /shop", module: "商铺", label: "新增商铺" },
  { key: "PUT /shop", module: "商铺", label: "更新商铺" },
  { key: "GET /shop/of/type", module: "商铺", label: "按分类查商铺" },
  { key: "GET /shop/of/name", module: "商铺", label: "按名称查商铺" },
  { key: "GET /shop-type/list", module: "分类", label: "商铺分类列表" },
  { key: "POST /blog", module: "博客", label: "发布博客" },
  { key: "PUT /blog/like/{id}", module: "博客", label: "点赞 / 取消点赞" },
  { key: "GET /blog/of/me", module: "博客", label: "我的博客" },
  { key: "GET /blog/hot", module: "博客", label: "热门博客" },
  { key: "GET /blog/{id}", module: "博客", label: "博客详情" },
  { key: "GET /blog/likes/{id}", module: "博客", label: "点赞用户列表" },
  { key: "GET /blog/of/user", module: "博客", label: "用户博客列表" },
  { key: "GET /blog/of/follow", module: "博客", label: "关注推送流" },
  { key: "PUT /follow/{id}/{isFollow}", module: "关注", label: "关注 / 取关" },
  { key: "GET /follow/or/not/{id}", module: "关注", label: "是否关注" },
  { key: "GET /follow/common/{id}", module: "关注", label: "共同关注" },
  { key: "POST /voucher", module: "优惠券", label: "新增普通券" },
  { key: "POST /voucher/seckill", module: "优惠券", label: "新增秒杀券" },
  { key: "GET /voucher/list/{shopId}", module: "优惠券", label: "店铺券列表" },
  {
    key: "POST /voucher-order/seckill/{id}",
    module: "秒杀",
    label: "秒杀下单",
  },
  { key: "POST /upload/blog", module: "上传", label: "上传博客图片" },
  { key: "GET /upload/blog/delete", module: "上传", label: "删除博客图片" },
];

const navSections = [
  { id: "hero", label: "总览" },
  { id: "user", label: "用户" },
  { id: "shop-types", label: "分类" },
  { id: "shops", label: "商铺" },
  { id: "blogs", label: "博客" },
  { id: "follow", label: "关注" },
  { id: "vouchers", label: "优惠券" },
  { id: "upload", label: "上传" },
  { id: "logs", label: "请求日志" },
];

const apiBaseUrl = ref(
  localStorage.getItem("hmdp-api-base") ||
    import.meta.env.VITE_API_BASE_URL ||
    "",
);
const assetBaseUrl = ref(
  localStorage.getItem("hmdp-asset-base") ||
    import.meta.env.VITE_ASSET_BASE_URL ||
    import.meta.env.VITE_API_BASE_URL ||
    "",
);
const token = ref(localStorage.getItem("hmdp-token") || "");
const currentUser = ref(null);
const lastResponse = ref(null);
const requestLogs = ref([]);
const uploadedImages = ref(
  JSON.parse(localStorage.getItem("hmdp-uploaded-images") || "[]"),
);
const selectedUploadFile = ref(null);
const lastDevCode = ref("");

const notice = reactive({
  type: "info",
  message: "页面已接入全部后端接口。先发送验证码并登录，再验证受保护接口。",
});

const touchedEndpoints = reactive({});
const loadingMap = reactive({});

const shopTypes = ref([]);
const shopsByType = ref([]);
const shopsByName = ref([]);
const selectedShop = ref(null);
const vouchers = ref([]);
const hotBlogs = ref([]);
const myBlogs = ref([]);
const userBlogs = ref([]);
const blogDetail = ref(null);
const blogLikes = ref([]);
const followFeed = ref(null);
const userDetail = ref(null);
const userSummary = ref(null);
const followStatus = ref(null);
const commonFollows = ref([]);
const signCount = ref(null);
const seckillOrderId = ref(null);

const forms = reactive({
  user: {
    phone: "13800138000",
    code: "",
    password: "",
    infoId: "1",
    profileId: "1",
  },
  shopType: {
    current: "1",
  },
  shop: {
    typeId: "1",
    current: "1",
    x: "",
    y: "",
    name: "",
    nameCurrent: "1",
    detailId: "1",
    create: {
      name: "",
      typeId: "1",
      images: "",
      area: "",
      address: "",
      x: "",
      y: "",
      avgPrice: "",
      sold: "",
      comments: "",
      score: "",
      openHours: "10:00-22:00",
    },
    update: {
      id: "",
      name: "",
      typeId: "",
      images: "",
      area: "",
      address: "",
      x: "",
      y: "",
      avgPrice: "",
      sold: "",
      comments: "",
      score: "",
      openHours: "",
    },
  },
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
  voucher: {
    shopId: "1",
    normal: {
      shopId: "1",
      title: "",
      subTitle: "",
      rules: "",
      payValue: "",
      actualValue: "",
      type: "0",
    },
    seckill: {
      shopId: "1",
      title: "",
      subTitle: "",
      rules: "",
      payValue: "",
      actualValue: "",
      type: "1",
      stock: "",
      beginTime: "",
      endTime: "",
    },
    seckillId: "1",
  },
});

const apiClient = createApiClient({
  getBaseUrl: () => apiBaseUrl.value.trim(),
  getToken: () => token.value.trim(),
  onUnauthorized: () => {
    currentUser.value = null;
    setNotice(
      "error",
      "后端返回 401。请重新登录，或检查 authorization token。",
    );
  },
});

const effectiveApiBase = computed(
  () => apiBaseUrl.value.trim() || "同源代理（Vite dev proxy）",
);
const effectiveAssetBase = computed(
  () => assetBaseUrl.value.trim() || apiBaseUrl.value.trim(),
);
const isLoggedIn = computed(() => Boolean(token.value.trim()));
const shortToken = computed(() => {
  if (!token.value) {
    return "未登录";
  }
  if (token.value.length < 16) {
    return token.value;
  }
  return `${token.value.slice(0, 8)}...${token.value.slice(-6)}`;
});
const coverageCount = computed(
  () =>
    endpointCatalog.filter((endpoint) => touchedEndpoints[endpoint.key]).length,
);
const coverageRows = computed(() =>
  endpointCatalog.map((endpoint) => ({
    ...endpoint,
    done: Boolean(touchedEndpoints[endpoint.key]),
  })),
);
const lastResponsePretty = computed(() =>
  prettify(
    lastResponse.value ? lastResponse.value.body : { message: "尚无请求" },
  ),
);
const blogImageList = computed(() => splitImages(forms.blog.create.images));

watch(apiBaseUrl, (value) => localStorage.setItem("hmdp-api-base", value));
watch(assetBaseUrl, (value) => localStorage.setItem("hmdp-asset-base", value));
watch(token, (value) => localStorage.setItem("hmdp-token", value));
watch(
  uploadedImages,
  (value) =>
    localStorage.setItem("hmdp-uploaded-images", JSON.stringify(value)),
  { deep: true },
);

function setNotice(type, message) {
  notice.type = type;
  notice.message = message;
}

function setLoading(key, value) {
  loadingMap[key] = value;
}

function isLoading(key) {
  return Boolean(loadingMap[key]);
}

function markTouched(key) {
  touchedEndpoints[key] = true;
}

function rememberResponse(entry, endpointKey) {
  const enriched = {
    ...entry,
    logId: `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
    endpointKey,
    displayTime: new Date().toLocaleTimeString("zh-CN", { hour12: false }),
  };
  lastResponse.value = enriched;
  requestLogs.value = [enriched, ...requestLogs.value].slice(0, 40);
}

async function send(endpointKey, config, options = {}) {
  setLoading(endpointKey, true);
  const entry = await apiClient.request(config);
  rememberResponse(entry, endpointKey);
  setLoading(endpointKey, false);

  const body = entry.body;
  const businessSuccess =
    entry.ok &&
    (typeof body !== "object" ||
    body === null ||
    !Object.prototype.hasOwnProperty.call(body, "success")
      ? true
      : body.success !== false);

  if (businessSuccess) {
    markTouched(endpointKey);
    setNotice("success", options.successMessage || `${endpointKey} 调用成功`);
    if (options.onSuccess) {
      await options.onSuccess(body?.data, body, entry);
    }
  } else {
    const message = body?.errorMsg || entry.error || `HTTP ${entry.status}`;
    setNotice("error", `${endpointKey} 调用失败：${message}`);
    if (options.onError) {
      await options.onError(body, entry);
    }
  }

  return { entry, body, data: body?.data, success: businessSuccess };
}

function splitImages(rawValue) {
  if (!rawValue) {
    return [];
  }
  return rawValue
    .split(",")
    .map((item) => item.trim())
    .filter(Boolean);
}

function toAssetUrl(path) {
  return resolveAssetUrl(effectiveAssetBase.value, path);
}

function normalizeDateTime(value) {
  if (!value) {
    return undefined;
  }
  return value.length === 16 ? `${value}:00` : value;
}

function buildShopPayload(source) {
  return cloneWithoutEmpty({
    id: asNumber(source.id),
    name: source.name,
    typeId: asNumber(source.typeId),
    images: source.images,
    area: source.area,
    address: source.address,
    x: asNumber(source.x),
    y: asNumber(source.y),
    avgPrice: asNumber(source.avgPrice),
    sold: asNumber(source.sold),
    comments: asNumber(source.comments),
    score: asNumber(source.score),
    openHours: source.openHours,
  });
}

function buildVoucherPayload(source, includeSeckillFields = false) {
  const payload = cloneWithoutEmpty({
    shopId: asNumber(source.shopId),
    title: source.title,
    subTitle: source.subTitle,
    rules: source.rules,
    payValue: asNumber(source.payValue),
    actualValue: asNumber(source.actualValue),
    type: asNumber(source.type),
  });

  if (includeSeckillFields) {
    Object.assign(
      payload,
      cloneWithoutEmpty({
        stock: asNumber(source.stock),
        beginTime: normalizeDateTime(source.beginTime),
        endTime: normalizeDateTime(source.endTime),
      }),
    );
  }

  return payload;
}

function buildBlogPayload() {
  return cloneWithoutEmpty({
    shopId: asNumber(forms.blog.create.shopId),
    title: forms.blog.create.title,
    images: forms.blog.create.images,
    content: forms.blog.create.content,
  });
}

function hydrateShopUpdateForm(shop) {
  forms.shop.update.id = shop?.id ? String(shop.id) : "";
  forms.shop.update.name = shop?.name || "";
  forms.shop.update.typeId = shop?.typeId ? String(shop.typeId) : "";
  forms.shop.update.images = shop?.images || "";
  forms.shop.update.area = shop?.area || "";
  forms.shop.update.address = shop?.address || "";
  forms.shop.update.x = shop?.x ?? "";
  forms.shop.update.y = shop?.y ?? "";
  forms.shop.update.avgPrice = shop?.avgPrice ?? "";
  forms.shop.update.sold = shop?.sold ?? "";
  forms.shop.update.comments = shop?.comments ?? "";
  forms.shop.update.score = shop?.score ?? "";
  forms.shop.update.openHours = shop?.openHours || "";
}

function useShopAsContext(shop) {
  const shopId = shop?.id ? String(shop.id) : "";
  if (!shopId) {
    return;
  }
  forms.blog.create.shopId = shopId;
  forms.voucher.shopId = shopId;
  forms.voucher.normal.shopId = shopId;
  forms.voucher.seckill.shopId = shopId;
  forms.shop.detailId = shopId;
  hydrateShopUpdateForm(shop);
  setNotice("info", `已将店铺 #${shopId} 设为博客和优惠券的默认上下文。`);
}

function useUploadImagesAsBlogImages() {
  forms.blog.create.images = uploadedImages.value.join(",");
  setNotice("info", "已把已上传图片路径写回博客发布表单。");
}

async function fetchMe() {
  return send(
    "GET /user/me",
    {
      method: "GET",
      path: "/user/me",
    },
    {
      successMessage: "已刷新当前登录用户",
      onSuccess: (data) => {
        currentUser.value = data || null;
      },
    },
  );
}

async function sendCode() {
  await send(
    "POST /user/code",
    {
      method: "POST",
      path: "/user/code",
      query: { phone: forms.user.phone },
    },
    {
      successMessage: "验证码请求已发送。",
      onSuccess: (data) => {
        if (typeof data === "string" && data) {
          const code = String(data);
          forms.user.code = code;
          lastDevCode.value = code;
          setNotice("success", `验证码已自动回填到表单：${code}`);
        } else {
          lastDevCode.value = "";
          setNotice(
            "success",
            "验证码请求已发送。当前环境未开启自动回填，请查看短信或后端日志。",
          );
        }
      },
    },
  );
}

async function login() {
  const result = await send(
    "POST /user/login",
    {
      method: "POST",
      path: "/user/login",
      body: cloneWithoutEmpty({
        phone: forms.user.phone,
        code: forms.user.code,
        password: forms.user.password,
      }),
    },
    {
      successMessage: "登录成功，token 已写入本地存储。",
      onSuccess: async (data) => {
        token.value = data || "";
        await fetchMe();
      },
    },
  );

  return result;
}

async function logout() {
  await send(
    "POST /user/logout",
    {
      method: "POST",
      path: "/user/logout",
    },
    {
      successMessage: "后端 logout 已调用。",
    },
  );
}

function clearLocalToken() {
  token.value = "";
  currentUser.value = null;
  setNotice(
    "info",
    "本地 token 已清空。后端 logout 接口仍保留单独按钮供你验证。",
  );
}

async function fetchUserInfo() {
  await send(
    "GET /user/info/{id}",
    {
      method: "GET",
      path: `/user/info/${forms.user.infoId}`,
    },
    {
      successMessage: "用户详情已更新。",
      onSuccess: (data) => {
        userDetail.value = data || null;
      },
    },
  );
}

async function fetchUserSummary() {
  await send(
    "GET /user/{id}",
    {
      method: "GET",
      path: `/user/${forms.user.profileId}`,
    },
    {
      successMessage: "用户基础信息已更新。",
      onSuccess: (data) => {
        userSummary.value = data || null;
      },
    },
  );
}

async function signToday() {
  await send(
    "POST /user/sign",
    {
      method: "POST",
      path: "/user/sign",
    },
    {
      successMessage: "签到成功。",
    },
  );
}

async function fetchSignCount() {
  await send(
    "GET /user/sign/count",
    {
      method: "GET",
      path: "/user/sign/count",
    },
    {
      successMessage: "已查询连续签到天数。",
      onSuccess: (data) => {
        signCount.value = data ?? null;
      },
    },
  );
}

async function fetchShopTypes() {
  await send(
    "GET /shop-type/list",
    {
      method: "GET",
      path: "/shop-type/list",
    },
    {
      successMessage: "分类列表已刷新。",
      onSuccess: (data) => {
        shopTypes.value = Array.isArray(data) ? data : [];
      },
    },
  );
}

async function queryShopsByType() {
  await send(
    "GET /shop/of/type",
    {
      method: "GET",
      path: "/shop/of/type",
      query: {
        typeId: forms.shop.typeId,
        current: forms.shop.current,
        x: forms.shop.x,
        y: forms.shop.y,
      },
    },
    {
      successMessage: "分类商铺列表已更新。",
      onSuccess: (data) => {
        shopsByType.value = Array.isArray(data) ? data : [];
      },
    },
  );
}

async function queryShopsByName() {
  await send(
    "GET /shop/of/name",
    {
      method: "GET",
      path: "/shop/of/name",
      query: {
        name: forms.shop.name,
        current: forms.shop.nameCurrent,
      },
    },
    {
      successMessage: "名称搜索结果已更新。",
      onSuccess: (data) => {
        shopsByName.value = Array.isArray(data) ? data : [];
      },
    },
  );
}

async function fetchShopDetail(shopId = forms.shop.detailId) {
  forms.shop.detailId = String(shopId);
  await send(
    "GET /shop/{id}",
    {
      method: "GET",
      path: `/shop/${shopId}`,
    },
    {
      successMessage: "商铺详情已更新。",
      onSuccess: (data) => {
        selectedShop.value = data || null;
        if (data) {
          useShopAsContext(data);
        }
      },
    },
  );
}

async function createShop() {
  await send(
    "POST /shop",
    {
      method: "POST",
      path: "/shop",
      body: buildShopPayload(forms.shop.create),
    },
    {
      successMessage: "新增商铺请求已发送。后端不会返回新商铺 ID。",
    },
  );
}

async function updateShop() {
  await send(
    "PUT /shop",
    {
      method: "PUT",
      path: "/shop",
      body: buildShopPayload(forms.shop.update),
    },
    {
      successMessage: "商铺更新成功。",
      onSuccess: async () => {
        if (forms.shop.update.id) {
          await fetchShopDetail(forms.shop.update.id);
        }
      },
    },
  );
}

async function fetchHotBlogs() {
  await send(
    "GET /blog/hot",
    {
      method: "GET",
      path: "/blog/hot",
      query: {
        current: forms.blog.hotCurrent,
      },
    },
    {
      successMessage: "热门博客已刷新。",
      onSuccess: (data) => {
        hotBlogs.value = Array.isArray(data) ? data : [];
      },
    },
  );
}

async function createBlog() {
  await send(
    "POST /blog",
    {
      method: "POST",
      path: "/blog",
      body: buildBlogPayload(),
    },
    {
      successMessage: "博客发布成功。",
      onSuccess: async (data) => {
        if (data) {
          forms.blog.detailId = String(data);
          await Promise.all([
            fetchHotBlogs(),
            fetchMyBlogs(),
            fetchBlogDetail(data),
          ]);
        }
      },
    },
  );
}

async function fetchMyBlogs() {
  await send(
    "GET /blog/of/me",
    {
      method: "GET",
      path: "/blog/of/me",
      query: {
        current: forms.blog.myCurrent,
      },
    },
    {
      successMessage: "我的博客列表已刷新。",
      onSuccess: (data) => {
        myBlogs.value = Array.isArray(data) ? data : [];
      },
    },
  );
}

async function fetchBlogDetail(blogId = forms.blog.detailId) {
  forms.blog.detailId = String(blogId);
  await send(
    "GET /blog/{id}",
    {
      method: "GET",
      path: `/blog/${blogId}`,
    },
    {
      successMessage: "博客详情已更新。",
      onSuccess: (data) => {
        blogDetail.value = data || null;
      },
    },
  );
}

async function fetchBlogLikes(blogId = forms.blog.likesId) {
  forms.blog.likesId = String(blogId);
  await send(
    "GET /blog/likes/{id}",
    {
      method: "GET",
      path: `/blog/likes/${blogId}`,
    },
    {
      successMessage: "点赞用户列表已更新。",
      onSuccess: (data) => {
        blogLikes.value = Array.isArray(data) ? data : [];
      },
    },
  );
}

async function toggleBlogLike(blogId) {
  await send(
    "PUT /blog/like/{id}",
    {
      method: "PUT",
      path: `/blog/like/${blogId}`,
    },
    {
      successMessage: "点赞状态已切换。",
      onSuccess: async () => {
        await Promise.all([
          fetchHotBlogs(),
          fetchBlogDetail(blogId),
          fetchBlogLikes(blogId),
        ]);
      },
    },
  );
}

async function fetchUserBlogs() {
  await send(
    "GET /blog/of/user",
    {
      method: "GET",
      path: "/blog/of/user",
      query: {
        id: forms.blog.userId,
        current: forms.blog.userCurrent,
      },
    },
    {
      successMessage: "指定用户博客列表已更新。",
      onSuccess: (data) => {
        userBlogs.value = Array.isArray(data) ? data : [];
      },
    },
  );
}

async function fetchFollowFeed() {
  await send(
    "GET /blog/of/follow",
    {
      method: "GET",
      path: "/blog/of/follow",
      query: {
        lastId: forms.blog.followLastId,
        offset: forms.blog.followOffset,
      },
    },
    {
      successMessage: "关注推送流已更新。",
      onSuccess: (data) => {
        followFeed.value = data || null;
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

async function updateFollowState(value) {
  const boolValue = asBoolean(value);
  await send(
    "PUT /follow/{id}/{isFollow}",
    {
      method: "PUT",
      path: `/follow/${forms.follow.targetUserId}/${boolValue}`,
    },
    {
      successMessage: boolValue ? "已发送关注请求。" : "已发送取关请求。",
    },
  );
}

async function checkFollowStatus() {
  await send(
    "GET /follow/or/not/{id}",
    {
      method: "GET",
      path: `/follow/or/not/${forms.follow.checkUserId}`,
    },
    {
      successMessage: "关注状态已更新。",
      onSuccess: (data) => {
        followStatus.value = data;
      },
    },
  );
}

async function fetchCommonFollows() {
  await send(
    "GET /follow/common/{id}",
    {
      method: "GET",
      path: `/follow/common/${forms.follow.commonUserId}`,
    },
    {
      successMessage: "共同关注列表已更新。",
      onSuccess: (data) => {
        commonFollows.value = Array.isArray(data) ? data : [];
      },
    },
  );
}

async function createNormalVoucher() {
  await send(
    "POST /voucher",
    {
      method: "POST",
      path: "/voucher",
      body: buildVoucherPayload(forms.voucher.normal),
    },
    {
      successMessage: "普通券创建成功。",
    },
  );
}

async function createSeckillVoucher() {
  await send(
    "POST /voucher/seckill",
    {
      method: "POST",
      path: "/voucher/seckill",
      body: buildVoucherPayload(forms.voucher.seckill, true),
    },
    {
      successMessage: "秒杀券创建成功。",
    },
  );
}

async function fetchVoucherList(shopId = forms.voucher.shopId) {
  forms.voucher.shopId = String(shopId);
  await send(
    "GET /voucher/list/{shopId}",
    {
      method: "GET",
      path: `/voucher/list/${shopId}`,
    },
    {
      successMessage: "优惠券列表已更新。",
      onSuccess: (data) => {
        vouchers.value = Array.isArray(data) ? data : [];
      },
    },
  );
}

async function seckillVoucher(voucherId = forms.voucher.seckillId) {
  forms.voucher.seckillId = String(voucherId);
  await send(
    "POST /voucher-order/seckill/{id}",
    {
      method: "POST",
      path: `/voucher-order/seckill/${voucherId}`,
    },
    {
      successMessage: "秒杀下单请求已发送。",
      onSuccess: (data) => {
        seckillOrderId.value = data ?? null;
      },
    },
  );
}

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
    {
      method: "POST",
      path: "/upload/blog",
      formData,
    },
    {
      successMessage: "图片上传成功。",
      onSuccess: (data) => {
        if (data) {
          uploadedImages.value = [
            data,
            ...uploadedImages.value.filter((item) => item !== data),
          ];
          useUploadImagesAsBlogImages();
        }
      },
    },
  );
}

async function deleteBlogImage(path) {
  await send(
    "GET /upload/blog/delete",
    {
      method: "GET",
      path: "/upload/blog/delete",
      query: {
        name: path,
      },
    },
    {
      successMessage: "图片删除成功。",
      onSuccess: () => {
        uploadedImages.value = uploadedImages.value.filter(
          (item) => item !== path,
        );
        forms.blog.create.images = splitImages(forms.blog.create.images)
          .filter((item) => item !== path)
          .join(",");
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
  forms.user.profileId = value;
  forms.user.infoId = value;
  forms.blog.userId = value;
}

function endpointBadgeClass(done) {
  return done ? "status-pill success" : "status-pill muted";
}

onMounted(async () => {
  await Promise.all([fetchShopTypes(), fetchHotBlogs()]);
  if (token.value) {
    await fetchMe();
  }
});
</script>

<template>
  <div class="shell">
    <aside class="sidebar ue-shadow ue-washi">
      <div class="sidebar-head">
        <div class="ue-stamp">浮世绘联调台</div>
        <p>全部后端接口都在这个单页里直连验证。</p>
      </div>
      <nav class="sidebar-nav">
        <a
          v-for="item in navSections"
          :key="item.id"
          class="nav-link"
          :href="`#${item.id}`"
        >
          {{ item.label }}
        </a>
      </nav>
      <div class="sidebar-foot">
        <p class="meta-label">接口覆盖</p>
        <strong>{{ coverageCount }} / {{ endpointCatalog.length }}</strong>
        <p class="meta-label">Blog Comments</p>
        <span class="empty-note">后端仅有空控制器，无可调用方法</span>
      </div>
    </aside>

    <main class="main">
      <section
        id="hero"
        class="hero-card ue-wave-bg ue-washi ue-shadow ukiyo-e-digital-accent-corner"
      >
        <div class="hero-copy">
          <p class="eyebrow">HMDP Frontend Console</p>
          <h1>Ukiyo-e Digital 全接口前端</h1>
          <p class="hero-text">
            以和纸底、靛蓝边、朱印强调组织一个可直接联调的前端工作台。接口状态、请求日志、业务数据和错误信息全部在页面内可见。
          </p>
        </div>
        <div class="hero-grid">
          <div class="info-card ukiyo-e-digital-card">
            <span class="label">API Base URL</span>
            <input v-model="apiBaseUrl" placeholder="留空则使用同源代理" />
            <small>当前：{{ effectiveApiBase }}</small>
          </div>
          <div class="info-card ukiyo-e-digital-card">
            <span class="label">静态资源基址</span>
            <input v-model="assetBaseUrl" placeholder="图片预览基址，可留空" />
            <small>上传图片预览时使用</small>
          </div>
          <div class="info-card ukiyo-e-digital-card">
            <span class="label">Token 状态</span>
            <strong>{{ shortToken }}</strong>
            <small>{{
              isLoggedIn
                ? "已注入 authorization 头"
                : "未登录，仅可调用公开接口"
            }}</small>
          </div>
          <div class="info-card ukiyo-e-digital-card">
            <span class="label">当前用户</span>
            <strong>{{ currentUser?.nickName || "匿名访客" }}</strong>
            <small>ID：{{ currentUser?.id ?? "--" }}</small>
          </div>
        </div>
        <div class="notice-bar" :class="notice.type">
          <span class="ue-stamp">{{
            notice.type === "error"
              ? "警示"
              : notice.type === "success"
                ? "已响应"
                : "提示"
          }}</span>
          <span>{{ notice.message }}</span>
        </div>
      </section>

      <section id="user" class="module-section">
        <div class="section-title">
          <div>
            <p class="eyebrow">User APIs</p>
            <h2>用户模块</h2>
          </div>
          <span class="section-hint">8 个接口，登录后可验证受保护接口</span>
        </div>

        <div class="card-grid two">
          <article
            class="panel ue-washi ue-shadow ukiyo-e-digital-card ukiyo-e-digital-animate-in"
          >
            <div class="panel-head">
              <h3>验证码与登录</h3>
              <span
                :class="endpointBadgeClass(touchedEndpoints['POST /user/code'])"
                >/user/code</span
              >
            </div>
            <div class="form-grid">
              <label>
                <span>手机号</span>
                <input v-model="forms.user.phone" placeholder="13800138000" />
              </label>
              <label>
                <span>验证码</span>
                <input
                  v-model="forms.user.code"
                  placeholder="后端日志中的 6 位验证码"
                />
              </label>
              <p v-if="lastDevCode" class="helper dev-code">
                开发环境验证码已回填：{{ lastDevCode }}
              </p>
              <label>
                <span>密码</span>
                <input
                  v-model="forms.user.password"
                  placeholder="后端当前不会使用该字段"
                />
              </label>
            </div>
            <div class="button-row">
              <button
                :disabled="isLoading('POST /user/code')"
                @click="sendCode"
              >
                发送验证码
              </button>
              <button
                :disabled="isLoading('POST /user/login')"
                class="accent"
                @click="login"
              >
                验证码登录
              </button>
            </div>
            <p class="helper">
              登录成功后 token 会写入本地存储，并自动刷新 `/user/me`。
            </p>
          </article>

          <article
            class="panel ue-washi ue-shadow ukiyo-e-digital-card ukiyo-e-digital-animate-in"
          >
            <div class="panel-head">
              <h3>会话与签到</h3>
              <span
                :class="endpointBadgeClass(touchedEndpoints['GET /user/me'])"
                >受保护接口</span
              >
            </div>
            <div class="button-row wrap">
              <button :disabled="isLoading('GET /user/me')" @click="fetchMe">
                刷新当前用户
              </button>
              <button
                :disabled="isLoading('POST /user/logout')"
                @click="logout"
              >
                调用 logout
              </button>
              <button class="secondary" @click="clearLocalToken">
                仅清空本地 token
              </button>
              <button
                :disabled="isLoading('POST /user/sign')"
                @click="signToday"
              >
                今日签到
              </button>
              <button
                :disabled="isLoading('GET /user/sign/count')"
                @click="fetchSignCount"
              >
                查询连续签到
              </button>
            </div>
            <div class="inline-stats">
              <div>
                <span class="label">当前用户</span>
                <strong>{{ currentUser?.nickName || "--" }}</strong>
              </div>
              <div>
                <span class="label">连续签到</span>
                <strong>{{ signCount ?? "--" }}</strong>
              </div>
            </div>
            <p class="helper">
              `/user/logout`
              当前由后端固定返回“功能未完成”，保留该按钮用于接口验证。
            </p>
          </article>

          <article class="panel ue-washi ue-shadow ukiyo-e-digital-card">
            <div class="panel-head">
              <h3>查询用户详情</h3>
              <span
                :class="
                  endpointBadgeClass(touchedEndpoints['GET /user/info/{id}'])
                "
                >/user/info/{id}</span
              >
            </div>
            <div class="form-grid single">
              <label>
                <span>用户 ID</span>
                <input v-model="forms.user.infoId" />
              </label>
            </div>
            <div class="button-row">
              <button
                :disabled="isLoading('GET /user/info/{id}')"
                @click="fetchUserInfo"
              >
                查询详情
              </button>
            </div>
            <pre class="json-box">{{
              prettify(userDetail || { message: "尚未查询" })
            }}</pre>
          </article>

          <article class="panel ue-washi ue-shadow ukiyo-e-digital-card">
            <div class="panel-head">
              <h3>查询用户基础信息</h3>
              <span
                :class="endpointBadgeClass(touchedEndpoints['GET /user/{id}'])"
                >/user/{id}</span
              >
            </div>
            <div class="form-grid single">
              <label>
                <span>用户 ID</span>
                <input v-model="forms.user.profileId" />
              </label>
            </div>
            <div class="button-row">
              <button
                :disabled="isLoading('GET /user/{id}')"
                @click="fetchUserSummary"
              >
                查询基础信息
              </button>
            </div>
            <pre class="json-box">{{
              prettify(userSummary || { message: "尚未查询" })
            }}</pre>
          </article>
        </div>
      </section>

      <section id="shop-types" class="module-section">
        <div class="section-title">
          <div>
            <p class="eyebrow">Shop Type APIs</p>
            <h2>商铺分类</h2>
          </div>
          <span class="section-hint"
            >公开接口，可作为商铺和博客的入口上下文</span
          >
        </div>

        <article class="panel ue-washi ue-shadow ukiyo-e-digital-card">
          <div class="panel-head">
            <h3>分类列表</h3>
            <span
              :class="
                endpointBadgeClass(touchedEndpoints['GET /shop-type/list'])
              "
              >/shop-type/list</span
            >
          </div>
          <div class="button-row">
            <button
              :disabled="isLoading('GET /shop-type/list')"
              @click="fetchShopTypes"
            >
              刷新分类
            </button>
          </div>
          <div class="type-grid">
            <button
              v-for="type in shopTypes"
              :key="type.id"
              class="type-chip"
              @click="
                forms.shop.typeId = String(type.id);
                forms.shop.create.typeId = String(type.id);
                forms.shop.update.typeId = String(type.id);
                queryShopsByType();
              "
            >
              <img
                v-if="type.icon"
                :src="toAssetUrl(type.icon)"
                :alt="type.name"
              />
              <span>{{ type.name }}</span>
            </button>
          </div>
        </article>
      </section>

      <section id="shops" class="module-section">
        <div class="section-title">
          <div>
            <p class="eyebrow">Shop APIs</p>
            <h2>商铺模块</h2>
          </div>
          <span class="section-hint">查询、创建、更新、选中店铺上下文</span>
        </div>

        <div class="card-grid two">
          <article class="panel ue-washi ue-shadow ukiyo-e-digital-card">
            <div class="panel-head">
              <h3>按分类查询商铺</h3>
              <span
                :class="
                  endpointBadgeClass(touchedEndpoints['GET /shop/of/type'])
                "
                >/shop/of/type</span
              >
            </div>
            <div class="form-grid quad">
              <label>
                <span>分类 ID</span>
                <input v-model="forms.shop.typeId" />
              </label>
              <label>
                <span>页码 current</span>
                <input v-model="forms.shop.current" />
              </label>
              <label>
                <span>经度 x</span>
                <input v-model="forms.shop.x" placeholder="可选" />
              </label>
              <label>
                <span>纬度 y</span>
                <input v-model="forms.shop.y" placeholder="可选" />
              </label>
            </div>
            <div class="button-row">
              <button
                :disabled="isLoading('GET /shop/of/type')"
                @click="queryShopsByType"
              >
                查询分类商铺
              </button>
            </div>
            <div class="shop-grid">
              <article
                v-for="shop in shopsByType"
                :key="`type-${shop.id}`"
                class="shop-card"
              >
                <img
                  v-if="splitImages(shop.images)[0]"
                  :src="toAssetUrl(splitImages(shop.images)[0])"
                  :alt="shop.name"
                />
                <div>
                  <strong>{{ shop.name }}</strong>
                  <p>{{ shop.area }} · {{ shop.openHours }}</p>
                  <p>
                    评分 {{ shop.score }} · 距离 {{ shop.distance ?? "--" }}
                  </p>
                </div>
                <div class="button-row tight">
                  <button class="secondary" @click="fetchShopDetail(shop.id)">
                    详情
                  </button>
                  <button class="secondary" @click="fetchVoucherList(shop.id)">
                    店铺券
                  </button>
                  <button class="secondary" @click="useShopAsContext(shop)">
                    设为上下文
                  </button>
                </div>
              </article>
            </div>
          </article>

          <article class="panel ue-washi ue-shadow ukiyo-e-digital-card">
            <div class="panel-head">
              <h3>按名称查询商铺</h3>
              <span
                :class="
                  endpointBadgeClass(touchedEndpoints['GET /shop/of/name'])
                "
                >/shop/of/name</span
              >
            </div>
            <div class="form-grid double">
              <label>
                <span>名称关键词</span>
                <input v-model="forms.shop.name" placeholder="例如 茶餐厅" />
              </label>
              <label>
                <span>页码 current</span>
                <input v-model="forms.shop.nameCurrent" />
              </label>
            </div>
            <div class="button-row">
              <button
                :disabled="isLoading('GET /shop/of/name')"
                @click="queryShopsByName"
              >
                搜索商铺
              </button>
            </div>
            <div class="shop-grid compact">
              <article
                v-for="shop in shopsByName"
                :key="`name-${shop.id}`"
                class="shop-card compact"
              >
                <div>
                  <strong>{{ shop.name }}</strong>
                  <p>{{ shop.address }}</p>
                </div>
                <div class="button-row tight">
                  <button class="secondary" @click="fetchShopDetail(shop.id)">
                    详情
                  </button>
                  <button class="secondary" @click="useShopAsContext(shop)">
                    设为上下文
                  </button>
                </div>
              </article>
            </div>
          </article>

          <article class="panel ue-washi ue-shadow ukiyo-e-digital-card">
            <div class="panel-head">
              <h3>商铺详情</h3>
              <span
                :class="endpointBadgeClass(touchedEndpoints['GET /shop/{id}'])"
                >/shop/{id}</span
              >
            </div>
            <div class="form-grid single">
              <label>
                <span>商铺 ID</span>
                <input v-model="forms.shop.detailId" />
              </label>
            </div>
            <div class="button-row">
              <button
                :disabled="isLoading('GET /shop/{id}')"
                @click="fetchShopDetail()"
              >
                查询详情
              </button>
            </div>
            <pre class="json-box">{{
              prettify(selectedShop || { message: "尚未查询商铺详情" })
            }}</pre>
          </article>

          <article class="panel ue-washi ue-shadow ukiyo-e-digital-card">
            <div class="panel-head">
              <h3>新增商铺</h3>
              <span :class="endpointBadgeClass(touchedEndpoints['POST /shop'])"
                >POST /shop</span
              >
            </div>
            <div class="form-grid quad">
              <label
                ><span>名称</span><input v-model="forms.shop.create.name"
              /></label>
              <label
                ><span>分类 ID</span><input v-model="forms.shop.create.typeId"
              /></label>
              <label
                ><span>商圈</span><input v-model="forms.shop.create.area"
              /></label>
              <label
                ><span>营业时间</span
                ><input v-model="forms.shop.create.openHours"
              /></label>
              <label
                ><span>地址</span><input v-model="forms.shop.create.address"
              /></label>
              <label
                ><span>图片（逗号分隔）</span
                ><input v-model="forms.shop.create.images"
              /></label>
              <label
                ><span>经度 x</span><input v-model="forms.shop.create.x"
              /></label>
              <label
                ><span>纬度 y</span><input v-model="forms.shop.create.y"
              /></label>
              <label
                ><span>均价</span><input v-model="forms.shop.create.avgPrice"
              /></label>
              <label
                ><span>销量</span><input v-model="forms.shop.create.sold"
              /></label>
              <label
                ><span>评论数</span><input v-model="forms.shop.create.comments"
              /></label>
              <label
                ><span>评分</span><input v-model="forms.shop.create.score"
              /></label>
            </div>
            <div class="button-row">
              <button :disabled="isLoading('POST /shop')" @click="createShop">
                新增商铺
              </button>
            </div>
          </article>

          <article class="panel ue-washi ue-shadow ukiyo-e-digital-card full">
            <div class="panel-head">
              <h3>更新商铺</h3>
              <span :class="endpointBadgeClass(touchedEndpoints['PUT /shop'])"
                >PUT /shop</span
              >
            </div>
            <div class="form-grid quad">
              <label
                ><span>ID</span><input v-model="forms.shop.update.id"
              /></label>
              <label
                ><span>名称</span><input v-model="forms.shop.update.name"
              /></label>
              <label
                ><span>分类 ID</span><input v-model="forms.shop.update.typeId"
              /></label>
              <label
                ><span>营业时间</span
                ><input v-model="forms.shop.update.openHours"
              /></label>
              <label
                ><span>商圈</span><input v-model="forms.shop.update.area"
              /></label>
              <label
                ><span>地址</span><input v-model="forms.shop.update.address"
              /></label>
              <label
                ><span>图片</span><input v-model="forms.shop.update.images"
              /></label>
              <label
                ><span>经度 x</span><input v-model="forms.shop.update.x"
              /></label>
              <label
                ><span>纬度 y</span><input v-model="forms.shop.update.y"
              /></label>
              <label
                ><span>均价</span><input v-model="forms.shop.update.avgPrice"
              /></label>
              <label
                ><span>销量</span><input v-model="forms.shop.update.sold"
              /></label>
              <label
                ><span>评论数</span><input v-model="forms.shop.update.comments"
              /></label>
              <label
                ><span>评分</span><input v-model="forms.shop.update.score"
              /></label>
            </div>
            <div class="button-row">
              <button :disabled="isLoading('PUT /shop')" @click="updateShop">
                更新商铺
              </button>
            </div>
          </article>
        </div>
      </section>

      <section id="blogs" class="module-section">
        <div class="section-title">
          <div>
            <p class="eyebrow">Blog APIs</p>
            <h2>博客模块</h2>
          </div>
          <span class="section-hint"
            >发布、热榜、我的笔记、详情、点赞、关注流</span
          >
        </div>

        <div class="card-grid two">
          <article class="panel ue-washi ue-shadow ukiyo-e-digital-card">
            <div class="panel-head">
              <h3>发布博客</h3>
              <span :class="endpointBadgeClass(touchedEndpoints['POST /blog'])"
                >POST /blog</span
              >
            </div>
            <div class="form-grid single">
              <label
                ><span>商铺 ID</span><input v-model="forms.blog.create.shopId"
              /></label>
              <label
                ><span>标题</span><input v-model="forms.blog.create.title"
              /></label>
              <label
                ><span>图片路径（逗号分隔）</span
                ><input v-model="forms.blog.create.images"
              /></label>
              <label
                ><span>内容</span
                ><textarea v-model="forms.blog.create.content" rows="6" />
              </label>
            </div>
            <div class="button-row wrap">
              <button
                :disabled="isLoading('POST /blog')"
                class="accent"
                @click="createBlog"
              >
                发布博客
              </button>
              <button class="secondary" @click="useUploadImagesAsBlogImages">
                使用已上传图片
              </button>
            </div>
            <div class="image-strip">
              <img
                v-for="image in blogImageList"
                :key="image"
                :src="toAssetUrl(image)"
                :alt="image"
              />
            </div>
          </article>

          <article class="panel ue-washi ue-shadow ukiyo-e-digital-card">
            <div class="panel-head">
              <h3>热门博客</h3>
              <span
                :class="endpointBadgeClass(touchedEndpoints['GET /blog/hot'])"
                >/blog/hot</span
              >
            </div>
            <div class="form-grid double">
              <label
                ><span>页码 current</span
                ><input v-model="forms.blog.hotCurrent"
              /></label>
              <label
                ><span>说明</span
                ><input value="公开接口，登录后会补充 isLike" disabled
              /></label>
            </div>
            <div class="button-row">
              <button
                :disabled="isLoading('GET /blog/hot')"
                @click="fetchHotBlogs"
              >
                刷新热门博客
              </button>
            </div>
            <div class="blog-grid">
              <article
                v-for="blog in hotBlogs"
                :key="`hot-${blog.id}`"
                class="blog-card"
              >
                <div class="blog-card-head">
                  <strong>{{ blog.title }}</strong>
                  <span class="ue-stamp">赞 {{ blog.liked }}</span>
                </div>
                <p>{{ blog.name || "匿名作者" }} · 店铺 {{ blog.shopId }}</p>
                <div class="image-strip">
                  <img
                    v-for="image in splitImages(blog.images).slice(0, 3)"
                    :key="`${blog.id}-${image}`"
                    :src="toAssetUrl(image)"
                    :alt="blog.title"
                  />
                </div>
                <div class="button-row tight">
                  <button class="secondary" @click="fillBlogDetail(blog)">
                    详情
                  </button>
                  <button class="secondary" @click="fetchBlogLikes(blog.id)">
                    点赞列表
                  </button>
                  <button class="secondary" @click="toggleBlogLike(blog.id)">
                    切换点赞
                  </button>
                  <button
                    class="secondary"
                    @click="fillFollowTarget(blog.userId)"
                  >
                    设为关注对象
                  </button>
                </div>
              </article>
            </div>
          </article>

          <article class="panel ue-washi ue-shadow ukiyo-e-digital-card">
            <div class="panel-head">
              <h3>我的博客</h3>
              <span
                :class="endpointBadgeClass(touchedEndpoints['GET /blog/of/me'])"
                >/blog/of/me</span
              >
            </div>
            <div class="form-grid single">
              <label
                ><span>页码 current</span><input v-model="forms.blog.myCurrent"
              /></label>
            </div>
            <div class="button-row">
              <button
                :disabled="isLoading('GET /blog/of/me')"
                @click="fetchMyBlogs"
              >
                查询我的博客
              </button>
            </div>
            <div class="simple-list">
              <div
                v-for="blog in myBlogs"
                :key="`my-${blog.id}`"
                class="simple-row"
              >
                <span>#{{ blog.id }} {{ blog.title }}</span>
                <div class="button-row tight">
                  <button class="secondary" @click="fillBlogDetail(blog)">
                    详情
                  </button>
                  <button class="secondary" @click="toggleBlogLike(blog.id)">
                    点赞
                  </button>
                </div>
              </div>
            </div>
          </article>

          <article class="panel ue-washi ue-shadow ukiyo-e-digital-card">
            <div class="panel-head">
              <h3>博客详情与点赞列表</h3>
              <span
                :class="endpointBadgeClass(touchedEndpoints['GET /blog/{id}'])"
                >详情 / likes</span
              >
            </div>
            <div class="form-grid double">
              <label
                ><span>博客 ID</span><input v-model="forms.blog.detailId"
              /></label>
              <label
                ><span>点赞列表博客 ID</span
                ><input v-model="forms.blog.likesId"
              /></label>
            </div>
            <div class="button-row wrap">
              <button
                :disabled="isLoading('GET /blog/{id}')"
                @click="fetchBlogDetail()"
              >
                查询博客详情
              </button>
              <button
                :disabled="isLoading('GET /blog/likes/{id}')"
                @click="fetchBlogLikes()"
              >
                查询点赞列表
              </button>
              <button
                :disabled="isLoading('PUT /blog/like/{id}')"
                @click="toggleBlogLike(forms.blog.detailId)"
              >
                切换点赞
              </button>
            </div>
            <pre class="json-box">{{
              prettify(blogDetail || { message: "尚未查询博客详情" })
            }}</pre>
            <pre class="json-box small">{{ prettify(blogLikes) }}</pre>
          </article>

          <article class="panel ue-washi ue-shadow ukiyo-e-digital-card">
            <div class="panel-head">
              <h3>指定用户博客</h3>
              <span
                :class="
                  endpointBadgeClass(touchedEndpoints['GET /blog/of/user'])
                "
                >/blog/of/user</span
              >
            </div>
            <div class="form-grid double">
              <label
                ><span>用户 ID</span><input v-model="forms.blog.userId"
              /></label>
              <label
                ><span>页码 current</span
                ><input v-model="forms.blog.userCurrent"
              /></label>
            </div>
            <div class="button-row">
              <button
                :disabled="isLoading('GET /blog/of/user')"
                @click="fetchUserBlogs"
              >
                查询指定用户博客
              </button>
            </div>
            <div class="simple-list">
              <div
                v-for="blog in userBlogs"
                :key="`user-${blog.id}`"
                class="simple-row"
              >
                <span>#{{ blog.id }} {{ blog.title }}</span>
                <div class="button-row tight">
                  <button class="secondary" @click="fillBlogDetail(blog)">
                    详情
                  </button>
                  <button
                    class="secondary"
                    @click="fillFollowTarget(blog.userId)"
                  >
                    设为关注对象
                  </button>
                </div>
              </div>
            </div>
          </article>

          <article class="panel ue-washi ue-shadow ukiyo-e-digital-card">
            <div class="panel-head">
              <h3>关注推送流</h3>
              <span
                :class="
                  endpointBadgeClass(touchedEndpoints['GET /blog/of/follow'])
                "
                >/blog/of/follow</span
              >
            </div>
            <div class="form-grid double">
              <label
                ><span>lastId</span><input v-model="forms.blog.followLastId"
              /></label>
              <label
                ><span>offset</span><input v-model="forms.blog.followOffset"
              /></label>
            </div>
            <div class="button-row">
              <button
                :disabled="isLoading('GET /blog/of/follow')"
                @click="fetchFollowFeed"
              >
                拉取关注流
              </button>
            </div>
            <pre class="json-box">{{
              prettify(followFeed || { message: "尚未拉取关注流" })
            }}</pre>
          </article>
        </div>
      </section>

      <section id="follow" class="module-section">
        <div class="section-title">
          <div>
            <p class="eyebrow">Follow APIs</p>
            <h2>关注模块</h2>
          </div>
          <span class="section-hint"
            >选择用户后可直接关注、查状态、查共同关注</span
          >
        </div>

        <div class="card-grid three">
          <article class="panel ue-washi ue-shadow ukiyo-e-digital-card">
            <div class="panel-head">
              <h3>关注 / 取关</h3>
              <span
                :class="
                  endpointBadgeClass(
                    touchedEndpoints['PUT /follow/{id}/{isFollow}'],
                  )
                "
                >PUT /follow</span
              >
            </div>
            <div class="form-grid single">
              <label
                ><span>目标用户 ID</span
                ><input v-model="forms.follow.targetUserId"
              /></label>
            </div>
            <div class="button-row">
              <button
                :disabled="isLoading('PUT /follow/{id}/{isFollow}')"
                @click="updateFollowState(true)"
              >
                关注
              </button>
              <button
                :disabled="isLoading('PUT /follow/{id}/{isFollow}')"
                class="secondary"
                @click="updateFollowState(false)"
              >
                取关
              </button>
            </div>
          </article>

          <article class="panel ue-washi ue-shadow ukiyo-e-digital-card">
            <div class="panel-head">
              <h3>是否已关注</h3>
              <span
                :class="
                  endpointBadgeClass(
                    touchedEndpoints['GET /follow/or/not/{id}'],
                  )
                "
                >/follow/or/not/{id}</span
              >
            </div>
            <div class="form-grid single">
              <label
                ><span>目标用户 ID</span
                ><input v-model="forms.follow.checkUserId"
              /></label>
            </div>
            <div class="button-row">
              <button
                :disabled="isLoading('GET /follow/or/not/{id}')"
                @click="checkFollowStatus"
              >
                查询关注状态
              </button>
            </div>
            <div class="inline-stats">
              <div>
                <span class="label">结果</span>
                <strong>{{
                  followStatus === null
                    ? "--"
                    : followStatus
                      ? "已关注"
                      : "未关注"
                }}</strong>
              </div>
            </div>
          </article>

          <article class="panel ue-washi ue-shadow ukiyo-e-digital-card">
            <div class="panel-head">
              <h3>共同关注</h3>
              <span
                :class="
                  endpointBadgeClass(
                    touchedEndpoints['GET /follow/common/{id}'],
                  )
                "
                >/follow/common/{id}</span
              >
            </div>
            <div class="form-grid single">
              <label
                ><span>对方用户 ID</span
                ><input v-model="forms.follow.commonUserId"
              /></label>
            </div>
            <div class="button-row">
              <button
                :disabled="isLoading('GET /follow/common/{id}')"
                @click="fetchCommonFollows"
              >
                查询共同关注
              </button>
            </div>
            <pre class="json-box small">{{ prettify(commonFollows) }}</pre>
          </article>
        </div>
      </section>

      <section id="vouchers" class="module-section">
        <div class="section-title">
          <div>
            <p class="eyebrow">Voucher APIs</p>
            <h2>优惠券与秒杀</h2>
          </div>
          <span class="section-hint"
            >普通券、秒杀券、店铺券列表、秒杀下单全接入</span
          >
        </div>

        <div class="card-grid two">
          <article class="panel ue-washi ue-shadow ukiyo-e-digital-card">
            <div class="panel-head">
              <h3>店铺券列表</h3>
              <span
                :class="
                  endpointBadgeClass(
                    touchedEndpoints['GET /voucher/list/{shopId}'],
                  )
                "
                >/voucher/list/{shopId}</span
              >
            </div>
            <div class="form-grid single">
              <label
                ><span>店铺 ID</span><input v-model="forms.voucher.shopId"
              /></label>
            </div>
            <div class="button-row">
              <button
                :disabled="isLoading('GET /voucher/list/{shopId}')"
                @click="fetchVoucherList()"
              >
                查询店铺券
              </button>
            </div>
            <div class="voucher-grid">
              <article
                v-for="voucher in vouchers"
                :key="voucher.id"
                class="voucher-card"
              >
                <div class="blog-card-head">
                  <strong>{{ voucher.title }}</strong>
                  <span class="ue-stamp">{{
                    voucher.type === 1 ? "秒杀券" : "普通券"
                  }}</span>
                </div>
                <p>
                  支付 {{ voucher.payValue }} / 抵扣 {{ voucher.actualValue }}
                </p>
                <p>
                  库存 {{ voucher.stock ?? "--" }} · 生效
                  {{ voucher.beginTime || "--" }}
                </p>
                <div class="button-row tight">
                  <button
                    class="secondary"
                    @click="forms.voucher.seckillId = String(voucher.id)"
                  >
                    设为秒杀 ID
                  </button>
                  <button class="secondary" @click="seckillVoucher(voucher.id)">
                    立即秒杀
                  </button>
                </div>
              </article>
            </div>
          </article>

          <article class="panel ue-washi ue-shadow ukiyo-e-digital-card">
            <div class="panel-head">
              <h3>新增普通券</h3>
              <span
                :class="endpointBadgeClass(touchedEndpoints['POST /voucher'])"
                >POST /voucher</span
              >
            </div>
            <div class="form-grid double">
              <label
                ><span>店铺 ID</span
                ><input v-model="forms.voucher.normal.shopId"
              /></label>
              <label
                ><span>标题</span><input v-model="forms.voucher.normal.title"
              /></label>
              <label
                ><span>副标题</span
                ><input v-model="forms.voucher.normal.subTitle"
              /></label>
              <label
                ><span>规则</span><input v-model="forms.voucher.normal.rules"
              /></label>
              <label
                ><span>支付金额</span
                ><input v-model="forms.voucher.normal.payValue"
              /></label>
              <label
                ><span>抵扣金额</span
                ><input v-model="forms.voucher.normal.actualValue"
              /></label>
            </div>
            <div class="button-row">
              <button
                :disabled="isLoading('POST /voucher')"
                @click="createNormalVoucher"
              >
                创建普通券
              </button>
            </div>
          </article>

          <article class="panel ue-washi ue-shadow ukiyo-e-digital-card full">
            <div class="panel-head">
              <h3>新增秒杀券 + 秒杀下单</h3>
              <span
                :class="
                  endpointBadgeClass(touchedEndpoints['POST /voucher/seckill'])
                "
                >POST /voucher/seckill</span
              >
            </div>
            <div class="form-grid quad">
              <label
                ><span>店铺 ID</span
                ><input v-model="forms.voucher.seckill.shopId"
              /></label>
              <label
                ><span>标题</span><input v-model="forms.voucher.seckill.title"
              /></label>
              <label
                ><span>副标题</span
                ><input v-model="forms.voucher.seckill.subTitle"
              /></label>
              <label
                ><span>规则</span><input v-model="forms.voucher.seckill.rules"
              /></label>
              <label
                ><span>支付金额</span
                ><input v-model="forms.voucher.seckill.payValue"
              /></label>
              <label
                ><span>抵扣金额</span
                ><input v-model="forms.voucher.seckill.actualValue"
              /></label>
              <label
                ><span>库存</span><input v-model="forms.voucher.seckill.stock"
              /></label>
              <label
                ><span>开始时间</span
                ><input
                  v-model="forms.voucher.seckill.beginTime"
                  type="datetime-local"
              /></label>
              <label
                ><span>结束时间</span
                ><input
                  v-model="forms.voucher.seckill.endTime"
                  type="datetime-local"
              /></label>
              <label
                ><span>秒杀券 ID</span><input v-model="forms.voucher.seckillId"
              /></label>
            </div>
            <div class="button-row wrap">
              <button
                :disabled="isLoading('POST /voucher/seckill')"
                @click="createSeckillVoucher"
              >
                创建秒杀券
              </button>
              <button
                :disabled="isLoading('POST /voucher-order/seckill/{id}')"
                class="accent"
                @click="seckillVoucher()"
              >
                发起秒杀下单
              </button>
            </div>
            <div class="inline-stats">
              <div>
                <span class="label">最近秒杀订单 ID</span>
                <strong>{{ seckillOrderId ?? "--" }}</strong>
              </div>
            </div>
          </article>
        </div>
      </section>

      <section id="upload" class="module-section">
        <div class="section-title">
          <div>
            <p class="eyebrow">Upload APIs</p>
            <h2>图片上传</h2>
          </div>
          <span class="section-hint"
            >上传返回相对路径，删除时直接传回该路径</span
          >
        </div>

        <div class="card-grid two">
          <article class="panel ue-washi ue-shadow ukiyo-e-digital-card">
            <div class="panel-head">
              <h3>上传博客图片</h3>
              <span
                :class="
                  endpointBadgeClass(touchedEndpoints['POST /upload/blog'])
                "
                >POST /upload/blog</span
              >
            </div>
            <div class="form-grid single">
              <label>
                <span>选择文件</span>
                <input
                  type="file"
                  accept="image/*"
                  @change="onUploadFileChange"
                />
              </label>
            </div>
            <div class="button-row wrap">
              <button
                :disabled="isLoading('POST /upload/blog')"
                @click="uploadBlogImage"
              >
                上传图片
              </button>
              <button class="secondary" @click="useUploadImagesAsBlogImages">
                写回博客表单
              </button>
            </div>
            <p class="helper">
              接口使用 `multipart/form-data`，字段名固定为 `file`。
            </p>
          </article>

          <article class="panel ue-washi ue-shadow ukiyo-e-digital-card">
            <div class="panel-head">
              <h3>已上传图片与删除</h3>
              <span
                :class="
                  endpointBadgeClass(
                    touchedEndpoints['GET /upload/blog/delete'],
                  )
                "
                >GET /upload/blog/delete</span
              >
            </div>
            <div class="upload-list">
              <article
                v-for="path in uploadedImages"
                :key="path"
                class="upload-card"
              >
                <img :src="toAssetUrl(path)" :alt="path" />
                <div>
                  <strong>{{ path }}</strong>
                  <div class="button-row tight">
                    <button
                      class="secondary"
                      @click="forms.blog.create.images = path"
                    >
                      仅填单张路径
                    </button>
                    <button class="secondary" @click="deleteBlogImage(path)">
                      删除图片
                    </button>
                  </div>
                </div>
              </article>
            </div>
          </article>
        </div>
      </section>

      <section id="logs" class="module-section">
        <div class="section-title">
          <div>
            <p class="eyebrow">Coverage & Logs</p>
            <h2>接口覆盖矩阵与请求日志</h2>
          </div>
          <span class="section-hint">覆盖情况按“HTTP 方法 + 路径”统计</span>
        </div>

        <div class="card-grid two">
          <article class="panel ue-washi ue-shadow ukiyo-e-digital-card">
            <div class="panel-head">
              <h3>接口覆盖矩阵</h3>
              <span class="ue-stamp"
                >{{ coverageCount }} / {{ endpointCatalog.length }}</span
              >
            </div>
            <div class="coverage-grid">
              <div
                v-for="row in coverageRows"
                :key="row.key"
                class="coverage-item"
              >
                <div>
                  <strong>{{ row.key }}</strong>
                  <p>{{ row.module }} · {{ row.label }}</p>
                </div>
                <span :class="endpointBadgeClass(row.done)">{{
                  row.done ? "已验证" : "待触发"
                }}</span>
              </div>
            </div>
          </article>

          <article class="panel dark-panel ue-shadow ukiyo-e-digital-card">
            <div class="panel-head">
              <h3>最近请求日志</h3>
              <span class="status-pill muted">最多保留 40 条</span>
            </div>
            <div class="log-list">
              <article
                v-for="entry in requestLogs"
                :key="entry.logId"
                class="log-item"
              >
                <div class="log-top">
                  <strong>{{ entry.endpointKey }}</strong>
                  <span>{{ entry.displayTime }}</span>
                </div>
                <p>{{ entry.method }} {{ entry.path }}</p>
                <p>状态 {{ entry.status }} · {{ entry.durationMs }}ms</p>
              </article>
            </div>
          </article>
        </div>
      </section>
    </main>

    <aside class="response-panel ue-shadow">
      <div class="panel-head">
        <h3>最后一次响应</h3>
        <span class="ue-stamp">{{ lastResponse?.status ?? "--" }}</span>
      </div>
      <div class="response-meta">
        <div>
          <span class="label">Endpoint</span>
          <strong>{{ lastResponse?.endpointKey || "--" }}</strong>
        </div>
        <div>
          <span class="label">URL</span>
          <strong class="mono">{{ lastResponse?.url || "--" }}</strong>
        </div>
        <div>
          <span class="label">耗时</span>
          <strong>{{ lastResponse?.durationMs ?? "--" }} ms</strong>
        </div>
      </div>
      <pre class="json-box dark">{{ lastResponsePretty }}</pre>
    </aside>
  </div>
</template>
