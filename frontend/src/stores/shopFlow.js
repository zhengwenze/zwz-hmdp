import { reactive, ref } from "vue";

export const shopFlowState = {
  listMode: ref("type"),
  currentTypeId: ref(""),
  currentTypeName: ref(""),
  keyword: ref(""),
  currentPage: ref(1),
  hasMore: ref(true),
  shops: ref([]),
  selectedShop: ref(null),
  vouchers: ref([]),
  detailLoading: ref(false),
  detailError: ref(""),
  location: reactive({
    x: "",
    y: "",
    enabled: false,
  }),
};
