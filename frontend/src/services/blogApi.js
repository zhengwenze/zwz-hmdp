import { apiRequest } from "./httpClient";

export const blogApi = {
  create(payload, options = {}) {
    return apiRequest(
      "POST /blog",
      { method: "POST", path: "/blog", body: payload },
      options,
    );
  },
  fetchHot(current, options = {}) {
    return apiRequest(
      "GET /blog/hot",
      { method: "GET", path: "/blog/hot", query: { current } },
      options,
    );
  },
  fetchMy(current, options = {}) {
    return apiRequest(
      "GET /blog/of/me",
      { method: "GET", path: "/blog/of/me", query: { current } },
      options,
    );
  },
  fetchDetail(blogId, options = {}) {
    return apiRequest(
      "GET /blog/{id}",
      { method: "GET", path: `/blog/${blogId}` },
      options,
    );
  },
  fetchLikes(blogId, options = {}) {
    return apiRequest(
      "GET /blog/likes/{id}",
      { method: "GET", path: `/blog/likes/${blogId}` },
      options,
    );
  },
  toggleLike(blogId, options = {}) {
    return apiRequest(
      "PUT /blog/like/{id}",
      { method: "PUT", path: `/blog/like/${blogId}` },
      options,
    );
  },
  fetchByUser(userId, current = 1, options = {}) {
    return apiRequest(
      "GET /blog/of/user",
      { method: "GET", path: "/blog/of/user", query: { id: userId, current } },
      options,
    );
  },
  fetchFollowFeed(lastId, offset, options = {}) {
    return apiRequest(
      "GET /blog/of/follow",
      {
        method: "GET",
        path: "/blog/of/follow",
        query: { lastId, offset },
      },
      options,
    );
  },
};
