#!/usr/bin/env bash
# Cấu hình dùng chung + helper cho toàn bộ script trong loadtest/.
# KHÔNG chạy trực tiếp file này — các script khác `source` nó.

set -euo pipefail

LT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT_DIR="${OUT_DIR:-$LT_DIR/out}"
mkdir -p "$OUT_DIR"

# ---------------------------------------------------------------------------
# Cấu hình (mọi biến đều override được bằng env)
# ---------------------------------------------------------------------------

# URL backend nhìn từ Git Bash trên host
BASE_URL="${BASE_URL:-http://localhost:8080}"
# URL backend nhìn từ trong container k6 (Docker Desktop trên Windows)
K6_BASE_URL="${K6_BASE_URL:-http://host.docker.internal:8080}"

# Container name lấy đúng từ docker-compose.yml ở gốc repo
MYSQL_CONTAINER="${MYSQL_CONTAINER:-travelK_mysql}"
REDIS_CONTAINER="${REDIS_CONTAINER:-travelK-redis}"

MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-1234}"
MYSQL_DB="${MYSQL_DB:-bus_reservation_db}"

# Admin mặc định do DataInitializer seed (userName=admin / password=admin)
ADMIN_LOGIN="${ADMIN_LOGIN:-admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-admin}"

# Prefix + mật khẩu của user load test
LT_USER_PREFIX="${LT_USER_PREFIX:-lt_user_}"
LT_USER_DOMAIN="${LT_USER_DOMAIN:-loadtest.local}"
LT_USER_PASSWORD="${LT_USER_PASSWORD:-Loadtest@123}"

USER_COUNT="${USER_COUNT:-200}"
VUS="${VUS:-200}"
SEAT="${SEAT:-A1}"

K6_IMAGE="${K6_IMAGE:-grafana/k6:latest}"

TRIP_ENV_FILE="$OUT_DIR/trip.env"
TOKENS_FILE="$OUT_DIR/tokens.json"

PY="${PY:-python}"

# ---------------------------------------------------------------------------
# Helper
# ---------------------------------------------------------------------------

log()  { printf '%s\n' "$*" >&2; }
step() { printf '\n=== %s\n' "$*" >&2; }
die()  { printf 'LỖI: %s\n' "$*" >&2; exit 1; }

require_cmd() {
  local c
  for c in "$@"; do
    command -v "$c" >/dev/null 2>&1 || die "thiếu lệnh '$c' trong PATH"
  done
}

# Đọc 1 field top-level của JSON từ stdin. Trả về exit code != 0 nếu không có.
json_field() {
  "$PY" -c '
import sys, json
key = sys.argv[1]
try:
    d = json.load(sys.stdin)
except Exception:
    sys.exit(1)
if not isinstance(d, dict) or d.get(key) is None:
    sys.exit(1)
print(d[key])
' "$1"
}

# api <METHOD> <PATH> [BODY_JSON] [TOKEN]
# Kết quả đặt vào biến toàn cục RESP_CODE và RESP_BODY.
api() {
  local method="$1" path="$2" body="${3:-}" token="${4:-}"
  local args out
  args=(-sS --max-time 60 -X "$method" "$BASE_URL$path"
        -H 'Content-Type: application/json'
        -H 'Accept: application/json'
        -w $'\n%{http_code}')
  if [ -n "$token" ]; then
    args+=(-H "Authorization: Bearer $token")
  fi
  if [ -n "$body" ]; then
    args+=(--data-binary "$body")
  fi
  out="$(curl "${args[@]}" 2>/dev/null || printf '\n000')"
  RESP_CODE="${out##*$'\n'}"
  RESP_BODY="${out%$'\n'*}"
}

# Chạy 1 câu SQL, trả kết quả dạng tab-separated không header.
mysql_q() {
  docker exec -e MYSQL_PWD="$MYSQL_PASSWORD" -i "$MYSQL_CONTAINER" \
    mysql -u"$MYSQL_USER" -N -B "$MYSQL_DB" -e "$1"
}

# Chạy 1 câu SQL, in kèm header (dùng cho phần verify cho dễ đọc).
mysql_q_table() {
  docker exec -e MYSQL_PWD="$MYSQL_PASSWORD" -i "$MYSQL_CONTAINER" \
    mysql -u"$MYSQL_USER" -t "$MYSQL_DB" -e "$1"
}

redis_cli() {
  docker exec -i "$REDIS_CONTAINER" redis-cli "$@"
}

# Lấy access token của admin
admin_token() {
  local body
  body="$("$PY" -c '
import json, sys
print(json.dumps({"email": sys.argv[1], "password": sys.argv[2]}))
' "$ADMIN_LOGIN" "$ADMIN_PASSWORD")"
  api POST /api/auth/login "$body"
  if [ "$RESP_CODE" != "200" ]; then
    die "đăng nhập admin thất bại (HTTP $RESP_CODE): $RESP_BODY"
  fi
  printf '%s' "$RESP_BODY" | json_field accessToken \
    || die "không đọc được accessToken từ response login admin"
}

load_trip_env() {
  [ -f "$TRIP_ENV_FILE" ] || die "chưa có $TRIP_ENV_FILE — chạy ./01-setup-data.sh trước"
  # shellcheck disable=SC1090
  . "$TRIP_ENV_FILE"
  [ -n "${TRIP_ID:-}" ] || die "TRIP_ID rỗng trong $TRIP_ENV_FILE"
}
