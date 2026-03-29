import { createRouter, createWebHashHistory } from "vue-router";
import MainLayout from "../layout/MainLayout.vue";
import BlankLayout from "../layout/BlankLayout.vue";
import HomePage from "../new_pages/HomePage.vue";
import LoginPage from "../new_pages/LoginPage.vue";
import MePage from "../new_pages/MePage.vue";
import ShopPage from "../new_pages/ShopPage.vue";
import BlogPage from "../new_pages/BlogPage.vue";
import FollowPage from "../new_pages/FollowPage.vue";
import VoucherPage from "../new_pages/VoucherPage.vue";
import UploadPage from "../new_pages/UploadPage.vue";
import BlogEditorPage from "../new_pages/BlogEditorPage.vue";

const routes = [
  {
    path: "/auth",
    redirect: "/login",
  },
  {
    path: "/login",
    component: BlankLayout,
    children: [
      {
        path: "",
        name: "login",
        component: LoginPage,
        meta: {
          title: "登录",
        },
      },
    ],
  },
  {
    path: "/",
    component: MainLayout,
    children: [
      {
        path: "",
        name: "home",
        component: HomePage,
        meta: {
          title: "工作台",
          description: "查看模块入口、当前会话与改造后的页面体系。",
          menu: true,
        },
      },
      {
        path: "me",
        name: "me",
        component: MePage,
        meta: {
          title: "用户中心",
          description: "管理当前登录用户、签到、昵称修改和登出。",
          menu: true,
        },
      },
      {
        path: "shop",
        name: "shop",
        component: ShopPage,
        meta: {
          title: "商铺管理",
          description: "统一以筛选区、表格区和分页区承载商铺查询。",
          menu: true,
        },
      },
      {
        path: "blog",
        name: "blog",
        component: BlogPage,
        meta: {
          title: "笔记管理",
          description: "集中管理发布、热门、个人笔记和详情查询。",
          menu: true,
        },
      },
      {
        path: "blog/editor",
        name: "blog-editor",
        component: BlogEditorPage,
        meta: {
          title: "发布笔记",
          description: "使用标准表单、上传和选店对话框完成笔记发布。",
          hidden: true,
        },
      },
      {
        path: "follow",
        name: "follow",
        component: FollowPage,
        meta: {
          title: "关注关系",
          description: "查看关注状态、共同关注和关注流。",
          menu: true,
        },
      },
      {
        path: "voucher",
        name: "voucher",
        component: VoucherPage,
        meta: {
          title: "优惠券管理",
          description: "查询店铺券、创建普通券和秒杀券。",
          menu: true,
        },
      },
      {
        path: "upload",
        name: "upload",
        component: UploadPage,
        meta: {
          title: "图片上传",
          description: "统一管理博客图片上传、预览和删除。",
          menu: true,
        },
      },
    ],
  },
  {
    path: "/:pathMatch(.*)*",
    redirect: "/",
  },
];

const router = createRouter({
  history: createWebHashHistory(),
  routes,
});

router.afterEach((to) => {
  document.title = to.meta?.title ? `${to.meta.title} - HMDP` : "HMDP";
});

export default router;
