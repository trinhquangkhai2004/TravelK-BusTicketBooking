#!/usr/bin/env bash
# Bước 4 — Chạy seat-race.js bằng k6.
#
# Ưu tiên k6 cài sẵn trên máy; nếu không có thì chạy qua Docker
# (image grafana/k6) và mount thư mục loadtest/ vào /loadtest.
# Không sửa docker-compose.yml — k6 chạy như container rời.

set -euo pipefail
. "$(dirname "$0")/lib/common.sh"

require_cmd "$PY"
load_trip_env

[ -f "$TOKENS_FILE" ] || die "chưa có $TOKENS_FILE — chạy ./02-register-users.sh trước"

TOKEN_N="$("$PY" -c 'import json,sys;print(len(json.load(open(sys.argv[1],encoding="utf-8"))))' "$TOKENS_FILE")"
if [ "$TOKEN_N" -lt "$VUS" ]; then
  die "chỉ có $TOKEN_N token nhưng VUS=$VUS. Mỗi VU bắt buộc một token riêng.
     Chạy lại 02-register-users.sh, hoặc: VUS=$TOKEN_N ./04-run-race.sh"
fi

log "TRIP_ID=$TRIP_ID  SEAT=$SEAT  VUS=$VUS  token khả dụng=$TOKEN_N"

if command -v k6 >/dev/null 2>&1; then
  step "Chạy k6 (bản cài trên máy) — BASE_URL=$BASE_URL"
  cd "$LT_DIR"
  exec k6 run \
    -e "BASE_URL=$BASE_URL" \
    -e "TRIP_ID=$TRIP_ID" \
    -e "BUS_ID=${BUS_ID:-1}" \
    -e "STATION_ID=${STATION_ID:-1}" \
    -e "SEAT=$SEAT" \
    -e "VUS=$VUS" \
    -e "BARRIER_MS=${BARRIER_MS:-3000}" \
    -e "TOKENS_FILE=./out/tokens.json" \
    seat-race.js
fi

require_cmd docker
step "Không thấy k6 trên máy — chạy qua Docker ($K6_IMAGE), BASE_URL=$K6_BASE_URL"

# pwd -W trả về đường dẫn kiểu D:/... mà Docker Desktop hiểu được;
# MSYS_NO_PATHCONV=1 chặn Git Bash tự đổi /loadtest thành C:/Program Files/Git/loadtest.
HOST_DIR="$(cd "$LT_DIR" && pwd -W 2>/dev/null || printf '%s' "$LT_DIR")"

# Chú ý: `-e` phải là cờ CỦA K6 (đặt sau `run`), không phải cờ của `docker run`.
# k6 inspect/run không chắc chắn kế thừa biến môi trường của container.
MSYS_NO_PATHCONV=1 exec docker run --rm -i \
  -v "$HOST_DIR:/loadtest" \
  -w /loadtest \
  "$K6_IMAGE" run \
  -e "BASE_URL=$K6_BASE_URL" \
  -e "TRIP_ID=$TRIP_ID" \
  -e "BUS_ID=${BUS_ID:-1}" \
  -e "STATION_ID=${STATION_ID:-1}" \
  -e "SEAT=$SEAT" \
  -e "VUS=$VUS" \
  -e "BARRIER_MS=${BARRIER_MS:-3000}" \
  -e "TOKENS_FILE=./out/tokens.json" \
  seat-race.js
