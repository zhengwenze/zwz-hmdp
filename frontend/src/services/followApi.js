import { apiRequest } from "./httpClient";

export const followApi = {
  toggle(userId, isFollow, options = {}) {
    return apiRequest(
      "PUT /follow/{id}/{isFollow}",
      { method: "PUT", path: `/follow/${userId}/${isFollow}` },
      options,
    );
  },
  check(userId, options = {}) {
    return apiRequest(
      "GET /follow/or/not/{id}",
      { method: "GET", path: `/follow/or/not/${userId}` },
      options,
    );
  },
  common(userId, options = {}) {
    return apiRequest(
      "GET /follow/common/{id}",
      { method: "GET", path: `/follow/common/${userId}` },
      options,
    );
  },
};
