#!/usr/bin/env bash
# Bước 3 — RESET. Chạy TRƯỚC MỖI lần race, không có ngoại lệ.
#
# Tại sao bắt buộc:
#   BookingServiceImpl.createBooking, nhánh "ghế đã được bán" (dòng ~93-96) `throw`
#   mà KHÔNG release lock Redis đã giữ ở vòng lặp phía trên. TTL của lock là 10 phút.
#   Nếu không xoá, từ lần chạy thứ 2 trở đi mọi request đều bị lock cũ chặn ngay,
#   ra kết quả "0 duplicate" trông rất đẹp nhưng hoàn toàn vô nghĩa — nó chứng minh
#   TTL còn hiệu lực, không chứng minh lock chống được tranh chấp.
#
# Xoá 2 thứ:
#   1. Mọi key Redis khớp hold:trip:*  (key là String thuần — RedisConfig dùng
#      StringRedisSerializer cho key, nên KEYS/SCAN bằng redis-cli đọc được)
#   2. tickets + booking_trip của TRIP_ID trong MySQL (tickets trước vì có FK booking_id)

set -euo pipefail
. "$(dirname "$0")/lib/common.sh"

require_cmd docker "$PY"
load_trip_env

step "Reset Redis — xoá key hold:trip:*"
BEFORE="$(redis_cli --scan --pattern 'hold:trip:*' | wc -l | tr -d ' \r')"
log "số key hold:trip:* trước khi xoá: $BEFORE"
if [ "$BEFORE" != "0" ]; then
  docker exec -i "$REDIS_CONTAINER" sh -c \
    "redis-cli --scan --pattern 'hold:trip:*' | xargs -r redis-cli DEL" >/dev/null
fi
AFTER="$(redis_cli --scan --pattern 'hold:trip:*' | wc -l | tr -d ' \r')"
log "còn lại sau khi xoá: $AFTER"
[ "$AFTER" = "0" ] || die "vẫn còn $AFTER key hold:trip:* — kiểm tra lại container $REDIS_CONTAINER"

step "Reset MySQL — xoá tickets/bookings của TRIP_ID=$TRIP_ID"
T_BEFORE="$(mysql_q "SELECT COUNT(*) FROM tickets WHERE trip_id = $TRIP_ID;" | tr -d '\r')"
B_BEFORE="$(mysql_q "SELECT COUNT(*) FROM booking_trip WHERE trip_id = $TRIP_ID;" | tr -d '\r')"
log "trước: tickets=$T_BEFORE bookings=$B_BEFORE"

mysql_q "DELETE FROM tickets WHERE trip_id = $TRIP_ID;" >/dev/null
mysql_q "DELETE FROM tickets WHERE booking_id IN (SELECT id FROM booking_trip WHERE trip_id = $TRIP_ID);" >/dev/null
mysql_q "DELETE FROM booking_trip WHERE trip_id = $TRIP_ID;" >/dev/null

T_AFTER="$(mysql_q "SELECT COUNT(*) FROM tickets WHERE trip_id = $TRIP_ID;" | tr -d '\r')"
B_AFTER="$(mysql_q "SELECT COUNT(*) FROM booking_trip WHERE trip_id = $TRIP_ID;" | tr -d '\r')"
log "sau  : tickets=$T_AFTER bookings=$B_AFTER"

if [ "$T_AFTER" != "0" ] || [ "$B_AFTER" != "0" ]; then
  die "reset MySQL chưa sạch (tickets=$T_AFTER bookings=$B_AFTER)"
fi

step "RESET OK — trạng thái đã sạch, sẵn sàng chạy race"
