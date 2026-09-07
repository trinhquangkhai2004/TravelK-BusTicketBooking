#!/usr/bin/env bash
# Chạy vòng lặp bắt buộc: reset -> race -> verify, lặp ROUNDS lần (mặc định 3).
#
# Điều kiện tiên quyết (chạy MỘT LẦN trước đó):
#   ./01-setup-data.sh
#   ./02-register-users.sh
#
# Ví dụ:
#   ./run-all.sh            # 3 vòng, 200 VU
#   VUS=100 ./run-all.sh    # hạ VU khi tỷ lệ 5xx nhiễu cao
#   ROUNDS=1 ./run-all.sh

set -uo pipefail
. "$(dirname "$0")/lib/common.sh"

ROUNDS="${ROUNDS:-3}"
PASS=0
FAIL=0
INCONCLUSIVE=0

for r in $(seq 1 "$ROUNDS"); do
  printf '\n\n#####################  VÒNG %s/%s  #####################\n' "$r" "$ROUNDS" >&2

  if ! "$LT_DIR/03-reset-state.sh"; then
    log "Reset thất bại ở vòng $r — dừng."
    exit 1
  fi

  # k6 có thể exit != 0 do threshold booked_2xx<=1 bị vi phạm; vẫn phải verify.
  "$LT_DIR/04-run-race.sh" || log "(k6 thoát với mã khác 0 — xem bảng phân loại ở trên)"

  if [ -f "$OUT_DIR/race-summary.json" ]; then
    cp "$OUT_DIR/race-summary.json" "$OUT_DIR/race-summary-round$r.json"
  fi

  "$LT_DIR/05-verify.sh"
  case "$?" in
    0) PASS=$((PASS + 1)) ;;
    1) FAIL=$((FAIL + 1)) ;;
    *) INCONCLUSIVE=$((INCONCLUSIVE + 1)) ;;
  esac
done

printf '\n\n=====================================================================\n' >&2
printf ' TỔNG KẾT %s VÒNG\n' "$ROUNDS" >&2
printf '   PASS          : %s\n' "$PASS" >&2
printf '   FAIL          : %s   (có ghế bán trùng)\n' "$FAIL" >&2
printf '   KHÔNG KẾT LUẬN: %s\n' "$INCONCLUSIVE" >&2
printf ' Summary từng vòng: %s/race-summary-round*.json\n' "$OUT_DIR" >&2
printf '=====================================================================\n' >&2

[ "$FAIL" = "0" ] || exit 1
[ "$INCONCLUSIVE" = "0" ] || exit 2
exit 0
