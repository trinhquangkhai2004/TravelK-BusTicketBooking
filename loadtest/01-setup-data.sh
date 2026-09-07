#!/usr/bin/env bash
# Bước 1 — Tạo dữ liệu nền cho race test: station -> bus -> trip.
# In ra TRIP_ID và ghi vào out/trip.env để các bước sau dùng lại.
#
# Endpoint đã đối chiếu với code hiện tại:
#   POST /api/auth/login              (permitAll)
#   POST /api/station                 (@PreAuthorize ADMIN)  -> StationResponseDto.stationId
#   POST /api/buses/station/{id}      (@PreAuthorize ADMIN)  -> BusResponseDto.id
#   POST /api/trips                   (permitAll qua /api/trips/**) -> TripResponseDto.id

set -euo pipefail
. "$(dirname "$0")/lib/common.sh"

require_cmd curl docker "$PY"

FORCE=0
if [ "${1:-}" = "--force" ]; then FORCE=1; fi

if [ -f "$TRIP_ENV_FILE" ] && [ "$FORCE" -eq 0 ]; then
  # shellcheck disable=SC1090
  . "$TRIP_ENV_FILE"
  log "Đã có $TRIP_ENV_FILE — dùng lại TRIP_ID=$TRIP_ID (chạy lại với --force để tạo mới)."
  printf 'TRIP_ID=%s\n' "$TRIP_ID"
  exit 0
fi

step "Đăng nhập admin"
TOKEN="$(admin_token)"
log "OK"

SUFFIX="$(date +%s)"
STATION_NAME="LoadTest Origin $SUFFIX"
ARRIVAL_STATION_NAME="LoadTest Destination $SUFFIX"
BUS_NUMBER="LT-$SUFFIX"

step "Tạo station: $STATION_NAME"
BODY="$("$PY" -c '
import json, sys
print(json.dumps({"name": sys.argv[1], "address": "loadtest"}))
' "$STATION_NAME")"
api POST /api/station "$BODY" "$TOKEN"
case "$RESP_CODE" in
  200|201) : ;;
  403|401) die "HTTP $RESP_CODE — tài khoản $ADMIN_LOGIN không có ROLE_ADMIN. Body: $RESP_BODY" ;;
  *) die "tạo station thất bại (HTTP $RESP_CODE): $RESP_BODY" ;;
esac
STATION_ID="$(printf '%s' "$RESP_BODY" | json_field stationId)" \
  || die "không đọc được stationId: $RESP_BODY"
log "STATION_ID=$STATION_ID"

step "Tạo bus 40 ghế (sinh sẵn ghế A1..D10)"
BODY="$("$PY" -c '
import json, sys
print(json.dumps({"number": sys.argv[1], "busType": "LOADTEST", "seats": 40}))
' "$BUS_NUMBER")"
api POST "/api/buses/station/$STATION_ID" "$BODY" "$TOKEN"
case "$RESP_CODE" in
  200|201) : ;;
  *) die "tạo bus thất bại (HTTP $RESP_CODE): $RESP_BODY" ;;
esac
BUS_ID="$(printf '%s' "$RESP_BODY" | json_field id)" \
  || die "không đọc được bus id: $RESP_BODY"
log "BUS_ID=$BUS_ID"

step "Tạo trip"
# Ngày/giờ sinh theo timestamp để không đụng ràng buộc existsOverLap khi chạy lại.
BODY="$("$PY" -c '
import json, sys, datetime
suffix = int(sys.argv[2])
d = datetime.date.today() + datetime.timedelta(days=30 + suffix % 300)
hour = 6 + (suffix // 7) % 12
print(json.dumps({
    "departureStationName": sys.argv[1],
    "arrivalStationName": sys.argv[3],
    "busId": int(sys.argv[4]),
    "price": 250000,
    "departureDate": d.isoformat(),
    "departureTime": "%02d:00:00" % hour,
    "arrivalDate": d.isoformat(),
    "arrivalTime": "%02d:30:00" % (hour + 4),
}))
' "$STATION_NAME" "$SUFFIX" "$ARRIVAL_STATION_NAME" "$BUS_ID")"
api POST /api/trips "$BODY" "$TOKEN"
case "$RESP_CODE" in
  200|201) : ;;
  *) die "tạo trip thất bại (HTTP $RESP_CODE): $RESP_BODY" ;;
esac
TRIP_ID="$(printf '%s' "$RESP_BODY" | json_field id)" \
  || die "không đọc được trip id: $RESP_BODY"

cat > "$TRIP_ENV_FILE" <<EOF
# Sinh bởi 01-setup-data.sh lúc $(date -Iseconds)
TRIP_ID=$TRIP_ID
BUS_ID=$BUS_ID
STATION_ID=$STATION_ID
EOF

step "Xong"
log "Đã ghi $TRIP_ENV_FILE"
printf 'TRIP_ID=%s\n' "$TRIP_ID"
printf 'BUS_ID=%s\n' "$BUS_ID"
printf 'STATION_ID=%s\n' "$STATION_ID"
