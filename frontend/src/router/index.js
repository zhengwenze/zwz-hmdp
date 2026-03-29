import { createRouter, createWebHashHistory } from "vue-router";
import AppShell from "../layout/AppShell.vue";
import HeroPage from "../pages/HeroPage.vue";
import UserPage from "../pages/UserPage.vue";
import ShopTypesPage from "../pages/ShopTypesPage.vue";
import ShopsPage from "../pages/ShopsPage.vue";
import BlogsPage from "../pages/BlogsPage.vue";
import FollowPage from "../pages/FollowPage.vue";
import VouchersPage from "../pages/VouchersPage.vue";
import UploadPage from "../pages/UploadPage.vue";
import LogsPage from "../pages/LogsPage.vue";
import { moduleMeta } from "../config/moduleMeta";

const pageComponents = {
  hero: HeroPage,
  user: UserPage,
  "shop-types": ShopTypesPage,
  shops: ShopsPage,
  blogs: BlogsPage,
  follow: FollowPage,
  vouchers: VouchersPage,
  upload: UploadPage,
  logs: LogsPage,
};

const routes = [
  {
    path: "/",
    component: AppShell,
    children: moduleMeta.map((item) => ({
      path: item.routePath,
      name: item.id,
      component: pageComponents[item.id],
      meta: {
        title: item.title,
        description: item.description,
      },
    })),
  },
];

const router = createRouter({
  history: createWebHashHistory(),
  routes,
});

export default router;
