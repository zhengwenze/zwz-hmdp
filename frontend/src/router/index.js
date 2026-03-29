import { createRouter, createWebHashHistory } from "vue-router";
import ConsumerShell from "../layout/ConsumerShell.vue";
import AppShell from "../layout/AppShell.vue";
import HeroPage from "../pages/HeroPage.vue";
import LoginPage from "../pages/LoginPage.vue";
import UserPage from "../pages/UserPage.vue";
import ShopTypesPage from "../pages/ShopTypesPage.vue";
import ShopsPage from "../pages/ShopsPage.vue";
import BlogsPage from "../pages/BlogsPage.vue";
import FollowPage from "../pages/FollowPage.vue";
import VouchersPage from "../pages/VouchersPage.vue";
import UploadPage from "../pages/UploadPage.vue";
import LogsPage from "../pages/LogsPage.vue";
import HomePage from "../pages/HomePage.vue";
import ShopListPage from "../pages/ShopListPage.vue";
import ShopDetailPage from "../pages/ShopDetailPage.vue";
import BlogDetailPage from "../pages/BlogDetailPage.vue";
import BlogEditorPage from "../pages/BlogEditorPage.vue";
import MePage from "../pages/MePage.vue";
import UserProfilePage from "../pages/UserProfilePage.vue";
import { moduleMeta } from "../config/moduleMeta";
import { buildRedirectPath, isAuthenticated } from "../stores/session";

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
    path: "/auth",
    redirect: "/login",
  },
  {
    path: "/login",
    name: "login",
    component: LoginPage,
  },
  {
    path: "/",
    component: ConsumerShell,
    children: [
      {
        path: "",
        name: "home",
        component: HomePage,
      },
      {
        path: "shop-list/:typeId",
        name: "shop-list",
        component: ShopListPage,
      },
      {
        path: "shop/:id",
        name: "shop-detail",
        component: ShopDetailPage,
      },
      {
        path: "blog/:id",
        name: "blog-detail",
        component: BlogDetailPage,
      },
      {
        path: "blog/new",
        name: "blog-new",
        component: BlogEditorPage,
        meta: {
          requiresAuth: true,
        },
      },
      {
        path: "me",
        name: "me",
        component: MePage,
        meta: {
          requiresAuth: true,
        },
      },
      {
        path: "user/:id",
        name: "user-profile",
        component: UserProfilePage,
      },
    ],
  },
  {
    path: "/lab",
    component: AppShell,
    children: moduleMeta.map((item) => ({
      path: item.routePath,
      name: `lab-${item.id}`,
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

router.beforeEach((to) => {
  if (to.meta.requiresAuth && !isAuthenticated()) {
    return buildRedirectPath(to.fullPath);
  }
  return true;
});

export default router;
