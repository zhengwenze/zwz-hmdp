#!/usr/bin/env bash

set -u

BASE_URL="${BASE_URL:-http://localhost:8081}"
PHONE="${PHONE:-13800138000}"
TIMEOUT_SECONDS="${TIMEOUT_SECONDS:-15}"
RUN_STATEFUL="${RUN_STATEFUL:-0}"

PASS_COUNT=0
FAIL_COUNT=0
LAST_BODY=""
LAST_STATUS=""

green() {
  printf '\033[32m%s\033[0m\n' "$1"
}

red() {
  printf '\033[31m%s\033[0m\n' "$1"
}

note() {
  printf '\n%s\n' "$1"
}

request() {
  local method="$1"
  local path="$2"
  local token="${3:-}"
  local body="${4:-}"
  local args
  local response

  args=(-sS --connect-timeout 3 --max-time "$TIMEOUT_SECONDS" \
    -w '\n%{http_code}' \
    -X "$method" "${BASE_URL}${path}")

  if [ -n "$token" ]; then
    args+=(-H "authorization: $token")
  fi

  if [ -n "$body" ]; then
    args+=(-H 'Content-Type: application/json' -d "$body")
  fi

  response=$(curl "${args[@]}")

  LAST_STATUS="${response##*$'\n'}"
  LAST_BODY="${response%$'\n'*}"
}

extract_json_string_field() {
  local field="$1"
  printf '%s' "$LAST_BODY" | sed -n "s/.*\"${field}\":\"\\([^\"]*\\)\".*/\\1/p"
}

extract_first_json_number_field() {
  local field="$1"
  printf '%s' "$LAST_BODY" | sed -n "s/.*\"${field}\":\\([0-9][0-9]*\\).*/\\1/p"
}

assert_success() {
  local name="$1"
  local method="$2"
  local path="$3"
  local token="${4:-}"
  local body="${5:-}"

  request "$method" "$path" "$token" "$body"

  if [ "$LAST_STATUS" = "200" ] && printf '%s' "$LAST_BODY" | grep -q '"success":true'; then
    PASS_COUNT=$((PASS_COUNT + 1))
    green "PASS $name"
  else
    FAIL_COUNT=$((FAIL_COUNT + 1))
    red "FAIL $name"
    printf '  %s %s\n' "$method" "$path"
    printf '  HTTP %s\n' "$LAST_STATUS"
    printf '  %s\n' "$LAST_BODY"
  fi
}

assert_status() {
  local name="$1"
  local expected_status="$2"
  local method="$3"
  local path="$4"
  local token="${5:-}"
  local body="${6:-}"

  request "$method" "$path" "$token" "$body"

  if [ "$LAST_STATUS" = "$expected_status" ]; then
    PASS_COUNT=$((PASS_COUNT + 1))
    green "PASS $name"
  else
    FAIL_COUNT=$((FAIL_COUNT + 1))
    red "FAIL $name"
    printf '  %s %s\n' "$method" "$path"
    printf '  expected HTTP %s, got HTTP %s\n' "$expected_status" "$LAST_STATUS"
    printf '  %s\n' "$LAST_BODY"
  fi
}

require_curl() {
  if ! command -v curl >/dev/null 2>&1; then
    red "curl is required but was not found"
    exit 1
  fi
}

print_summary_and_exit() {
  note "Summary"
  printf '  passed: %s\n' "$PASS_COUNT"
  printf '  failed: %s\n' "$FAIL_COUNT"

  if [ "$FAIL_COUNT" -eq 0 ]; then
    green "Backend smoke test passed"
    exit 0
  fi

  red "Backend smoke test failed"
  exit 1
}

require_curl

note "Backend smoke test"
printf '  BASE_URL=%s\n' "$BASE_URL"
printf '  PHONE=%s\n' "$PHONE"
printf '  RUN_STATEFUL=%s\n' "$RUN_STATEFUL"

note "Public endpoints"
assert_success "shop type list" "GET" "/shop-type/list"
assert_success "shop list by type" "GET" "/shop/of/type?typeId=1&current=1"
SHOP_ID="$(extract_first_json_number_field id)"
if [ -n "$SHOP_ID" ]; then
  assert_success "shop detail" "GET" "/shop/${SHOP_ID}"
else
  red "FAIL extract shop id"
  printf '  /shop/of/type did not return a shop id.\n'
  FAIL_COUNT=$((FAIL_COUNT + 1))
fi
assert_success "shop list by name" "GET" "/shop/of/name?current=1"
assert_success "hot blogs" "GET" "/blog/hot?current=1"
assert_success "shop vouchers" "GET" "/voucher/list/1"

note "Auth guard"
assert_status "user me requires token" "401" "GET" "/user/me"

note "Login flow"
assert_success "send login code" "POST" "/user/code?phone=${PHONE}"
LOGIN_CODE="${LOGIN_CODE:-$(extract_json_string_field data)}"

if [ -z "$LOGIN_CODE" ]; then
  red "FAIL extract login code"
  printf '  /user/code did not return data. The app usually returns code only in dev profile.\n'
  printf '  You can retry with LOGIN_CODE=123456 if you already know the code.\n'
  FAIL_COUNT=$((FAIL_COUNT + 1))
  print_summary_and_exit
fi

assert_success "login" "POST" "/user/login" "" "{\"phone\":\"${PHONE}\",\"code\":\"${LOGIN_CODE}\"}"
TOKEN="$(extract_json_string_field data)"

if [ -z "$TOKEN" ]; then
  red "FAIL extract token"
  printf '  /user/login did not return token data.\n'
  FAIL_COUNT=$((FAIL_COUNT + 1))
  print_summary_and_exit
fi

note "Authenticated endpoints"
assert_success "current user" "GET" "/user/me" "$TOKEN"
assert_success "user info" "GET" "/user/info/1" "$TOKEN"
assert_success "user profile" "GET" "/user/1" "$TOKEN"
assert_success "blog detail" "GET" "/blog/4" "$TOKEN"
assert_success "blog likes" "GET" "/blog/likes/4" "$TOKEN"
assert_success "my blogs" "GET" "/blog/of/me?current=1" "$TOKEN"
assert_success "user blogs" "GET" "/blog/of/user?id=1&current=1" "$TOKEN"
assert_success "follow feed" "GET" "/blog/of/follow?lastId=9999999999999&offset=0" "$TOKEN"
assert_success "is following user 2" "GET" "/follow/or/not/2" "$TOKEN"
assert_success "common follows with user 2" "GET" "/follow/common/2" "$TOKEN"
assert_success "sign count" "GET" "/user/sign/count" "$TOKEN"

if [ "$RUN_STATEFUL" = "1" ]; then
  note "Stateful endpoints"
  assert_success "sign today" "POST" "/user/sign" "$TOKEN"
  assert_success "follow user 2" "PUT" "/follow/2/true" "$TOKEN"
  assert_success "check follow user 2" "GET" "/follow/or/not/2" "$TOKEN"
  assert_success "unfollow user 2" "PUT" "/follow/2/false" "$TOKEN"
else
  note "Stateful endpoints skipped"
  printf '  Run with RUN_STATEFUL=1 to test sign and follow/unfollow writes.\n'
fi

note "Logout"
assert_success "logout" "POST" "/user/logout" "$TOKEN"
assert_status "token invalid after logout" "401" "GET" "/user/me" "$TOKEN"

print_summary_and_exit
