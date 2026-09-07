#!/usr/bin/env bash
# Bước 5 — VERIFY. Chạy SAU MỖI lần race.
#
# Đây là con số quyết định, không phải bảng đếm HTTP của k6: hỏi thẳng MySQL xem
# có ghế nào bị bán hai lần không.
#
#   SELECT seat_number, COUNT(*) FROM tickets
#   WHERE trip_id = ? GROUP BY seat_number HAVING COUNT(*) > 1;
#
# Số dòng trả về PHẢI bằng 0.
# (tickets.trip_id và tickets.seat_number lấy đúng từ entity/Ticket.java)

set -euo pipefail
. "$(dirname "$0")/lib/common.sh"

require_cmd docker
load_trip_env

step "Kiểm tra vé trùng cho TRIP_ID=$TRIP_ID"

DUP_SQL="SELECT seat_number, COUNT(*) AS so_ve
         FROM tickets
         WHERE trip_id = $TRIP_ID
         GROUP BY seat_number
         HAVING COUNT(*) > 1;"

DUP_ROWS="$(mysql_q "$DUP_SQL" | sed '/^$/d' | wc -l | tr -d ' \r')"
TOTAL_TICKETS="$(mysql_q "SELECT COUNT(*) FROM tickets WHERE trip_id = $TRIP_ID;" | tr -d '\r')"
TOTAL_BOOKINGS="$(mysql_q "SELECT COUNT(*) FROM booking_trip WHERE trip_id = $TRIP_ID;" | tr -d '\r')"
DISTINCT_SEATS="$(mysql_q "SELECT COUNT(DISTINCT seat_number) FROM tickets WHERE trip_id = $TRIP_ID;" | tr -d '\r')"

log ""
log "  tổng vé (tickets)        : $TOTAL_TICKETS"
log "  ghế phân biệt            : $DISTINCT_SEATS"
log "  tổng booking             : $TOTAL_BOOKINGS"
log "  SỐ GHẾ BỊ BÁN TRÙNG      : $DUP_ROWS"
log ""

if [ "$DUP_ROWS" != "0" ]; then
  log "Chi tiết ghế trùng:"
  mysql_q_table "$DUP_SQL" >&2
fi

log "Vé hiện có của trip này:"
mysql_q_table "SELECT t.id, t.seat_number, t.booking_id, b.user_id, b.status
               FROM tickets t JOIN booking_trip b ON b.id = t.booking_id
               WHERE t.trip_id = $TRIP_ID
               ORDER BY t.id;" >&2

log ""
log "====================================================================="
if [ "$DUP_ROWS" = "0" ] && [ "$TOTAL_TICKETS" = "1" ]; then
  log " PASS — 0 ghế bị bán trùng, đúng 1 vé được tạo."
  log " Chống double-booking hoạt động đúng dưới tranh chấp."
  RC=0
elif [ "$DUP_ROWS" = "0" ] && [ "$TOTAL_TICKETS" = "0" ]; then
  log " KHÔNG KẾT LUẬN — 0 duplicate nhưng cũng 0 vé."
  log " Không có request nào thành công => race chưa thực sự diễn ra."
  log " Kiểm tra: đã chạy 03-reset-state.sh chưa? token còn hạn không?"
  log " Xem lại out/race-summary.json (auth_401_403 / client_4xx_other)."
  RC=2
elif [ "$DUP_ROWS" = "0" ]; then
  log " PASS (có lưu ý) — 0 ghế bị bán trùng, nhưng có $TOTAL_TICKETS vé."
  log " Nếu race chỉ nhắm 1 ghế thì con số này lẽ ra là 1;"
  log " có thể state chưa được reset sạch trước khi chạy."
  RC=0
else
  log " FAIL — $DUP_ROWS ghế bị bán trùng. DOUBLE BOOKING đã xảy ra."
  RC=1
fi
log "====================================================================="

exit "$RC"
