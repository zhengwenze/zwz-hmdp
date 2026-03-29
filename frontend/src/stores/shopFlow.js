import { defineStore } from "pinia";
import { reactive, ref, toRef } from "vue";
import { pinia } from "./pinia";

export const useShopFlowStore = defineStore("shopFlow", () => {
  const listMode = ref("type");
  const currentTypeId = ref("");
  const currentTypeName = ref("");
  const keyword = ref("");
  const currentPage = ref(1);
  const hasMore = ref(true);
  const shops = ref([]);
  const selectedShop = ref(null);
  const vouchers = ref([]);
  const detailLoading = ref(false);
  const detailError = ref("");
  const location = reactive({
    x: "",
    y: "",
    enabled: false,
  });

  return {
    listMode,
    currentTypeId,
    currentTypeName,
    keyword,
    currentPage,
    hasMore,
    shops,
    selectedShop,
    vouchers,
    detailLoading,
    detailError,
    location,
  };
});

export function getShopFlowStore() {
  return useShopFlowStore(pinia);
}

const shopFlowStore = getShopFlowStore();

export const shopFlowState = {
  listMode: toRef(shopFlowStore, "listMode"),
  currentTypeId: toRef(shopFlowStore, "currentTypeId"),
  currentTypeName: toRef(shopFlowStore, "currentTypeName"),
  keyword: toRef(shopFlowStore, "keyword"),
  currentPage: toRef(shopFlowStore, "currentPage"),
  hasMore: toRef(shopFlowStore, "hasMore"),
  shops: toRef(shopFlowStore, "shops"),
  selectedShop: toRef(shopFlowStore, "selectedShop"),
  vouchers: toRef(shopFlowStore, "vouchers"),
  detailLoading: toRef(shopFlowStore, "detailLoading"),
  detailError: toRef(shopFlowStore, "detailError"),
  location: shopFlowStore.location,
};
