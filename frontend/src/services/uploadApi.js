import { apiRequest } from "./httpClient";

export const uploadApi = {
  uploadBlogImage(formData, options = {}) {
    return apiRequest(
      "POST /upload/blog",
      { method: "POST", path: "/upload/blog", formData },
      options,
    );
  },
  deleteBlogImage(path, options = {}) {
    return apiRequest(
      "GET /upload/blog/delete",
      { method: "GET", path: "/upload/blog/delete", query: { name: path } },
      options,
    );
  },
};
