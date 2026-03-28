import { createRouter, createWebHistory } from "vue-router";
import HeroPage from "../pages/HeroPage.vue";
import UserPage from "../pages/UserPage.vue";
import ShopTypesPage from "../pages/ShopTypesPage.vue";
import ShopsPage from "../pages/ShopsPage.vue";
import BlogsPage from "../pages/BlogsPage.vue";
import FollowPage from "../pages/FollowPage.vue";
import VouchersPage from "../pages/VouchersPage.vue";
import UploadPage from "../pages/UploadPage.vue";
import LogsPage from "../pages/LogsPage.vue";

const routes = [
  { path: "/", name: "hero", component: HeroPage },
  { path: "/user", name: "user", component: UserPage },
  { path: "/shop-types", name: "shop-types", component: ShopTypesPage },
  { path: "/shops", name: "shops", component: ShopsPage },
  { path: "/blogs", name: "blogs", component: BlogsPage },
  { path: "/follow", name: "follow", component: FollowPage },
  { path: "/vouchers", name: "vouchers", component: VouchersPage },
  { path: "/upload", name: "upload", component: UploadPage },
  { path: "/logs", name: "logs", component: LogsPage },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

export default router;
