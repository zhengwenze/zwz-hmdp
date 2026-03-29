import { defineConfig, loadEnv } from "vite";
import vue from "@vitejs/plugin-vue";
import AutoImport from "unplugin-auto-import/vite";
import Components from "unplugin-vue-components/vite";
import { ElementPlusResolver } from "unplugin-vue-components/resolvers";

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "");
  const target =
    env.VITE_API_PROXY_TARGET ||
    env.VITE_API_BASE_URL ||
    "http://localhost:8081";

  return {
    plugins: [
      vue(),
      AutoImport({
        resolvers: [ElementPlusResolver()],
        imports: ["vue", "vue-router"],
        dts: false,
      }),
      Components({
        resolvers: [ElementPlusResolver({ importStyle: "css" })],
        dts: false,
      }),
    ],
    server: {
      proxy: {
        "/user": target,
        "/shop": target,
        "/shop-type": target,
        "/blog": target,
        "/follow": target,
        "/voucher": target,
        "/voucher-order": target,
        "/upload": target,
        "/blogs": target,
      },
    },
  };
});
