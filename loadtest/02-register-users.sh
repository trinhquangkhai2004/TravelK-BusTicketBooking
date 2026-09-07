#!/usr/bin/env bash
# Bước 2 — Tạo USER_COUNT user riêng biệt và lấy USER_COUNT JWT riêng biệt.
#
# BẮT BUỘC mỗi VU một token riêng. Lý do nằm ở
# backend/src/main/java/com/khaiquang/service/impl/BookingServiceImpl.java:80-84 :
# khi setIfAbsent thất bại, code đọc lại giá trị lock; nếu người đang giữ chính là
# userId của request thì coi như giữ được và đi tiếp (lock re-entrant theo user).
# Dùng chung 1 token cho 200 request => cả 200 vượt qua lớp lock, test vô nghĩa.
#
# Endpoint đã đối chiếu với code hiện tại:
#   POST /api/user        (permitAll, UserRegisterRequest{userName,email,password,phoneNumber})
#   POST /api/auth/login  (permitAll, LoginRequest{email,password}) -> {accessToken,...}
#
# Lưu ý: UserServiceImpl.createUser yêu cầu role "ROLE_USER" tồn tại trong DB,
# nhưng DataInitializer chỉ seed "ROLE_ADMIN". Script chèn thẳng dòng ROLE_USER
# vào bảng `roles` nếu thiếu (chỉ là dữ liệu tham chiếu, KHÔNG sửa code app).
# Tắt bằng: SEED_ROLE_USER=0

set -euo pipefail
. "$(dirname "$0")/lib/common.sh"

require_cmd curl docker "$PY"

SEED_ROLE_USER="${SEED_ROLE_USER:-1}"
PARALLEL="${PARALLEL:-10}"

TOKENS_DIR="$OUT_DIR/tokens.d"
rm -rf "$TOKENS_DIR"
mkdir -p "$TOKENS_DIR"

if [ "$SEED_ROLE_USER" = "1" ]; then
  step "Đảm bảo role ROLE_USER tồn tại (workaround cho BUG: DataInitializer chỉ seed ROLE_ADMIN)"
  mysql_q "INSERT INTO roles (role_name)
           SELECT 'ROLE_USER' FROM DUAL
           WHERE NOT EXISTS (SELECT 1 FROM roles WHERE role_name = 'ROLE_USER');" >/dev/null
  log "roles hiện có: $(mysql_q 'SELECT GROUP_CONCAT(role_name) FROM roles;')"
fi

step "Tạo + đăng nhập $USER_COUNT user (song song $PARALLEL)"

one_user() {
  local i="$1"
  local idx name email phone body code resp token
  idx="$("$PY" -c 'print("%04d" % int(__import__("sys").argv[1]))' "$i")"
  name="${LT_USER_PREFIX}${idx}"
  email="${name}@${LT_USER_DOMAIN}"
  phone="$("$PY" -c 'print("09%08d" % int(__import__("sys").argv[1]))' "$i")"

  body="$("$PY" -c '
import json, sys
print(json.dumps({
    "userName": sys.argv[1], "email": sys.argv[2],
    "password": sys.argv[3], "phoneNumber": sys.argv[4],
}))
' "$name" "$email" "$LT_USER_PASSWORD" "$phone")"

  # 201 = tạo mới, 409 = đã tồn tại từ lần chạy trước (ResourceDuplicateException)
  api POST /api/user "$body"
  code="$RESP_CODE"
  if [ "$code" != "201" ] && [ "$code" != "200" ] && [ "$code" != "409" ]; then
    printf 'ERR user=%s register HTTP %s %s\n' "$name" "$code" "$RESP_BODY" \
      > "$TOKENS_DIR/$idx.err"
    return 0
  fi

  body="$("$PY" -c '
import json, sys
print(json.dumps({"email": sys.argv[1], "password": sys.argv[2]}))
' "$email" "$LT_USER_PASSWORD")"
  api POST /api/auth/login "$body"
  if [ "$RESP_CODE" != "200" ]; then
    printf 'ERR user=%s login HTTP %s %s\n' "$name" "$RESP_CODE" "$RESP_BODY" \
      > "$TOKENS_DIR/$idx.err"
    return 0
  fi

  token="$(printf '%s' "$RESP_BODY" | json_field accessToken)" || {
    printf 'ERR user=%s no accessToken: %s\n' "$name" "$RESP_BODY" \
      > "$TOKENS_DIR/$idx.err"
    return 0
  }
  printf '%s\n' "$token" > "$TOKENS_DIR/$idx.token"
}

# Chạy theo lô PARALLEL rồi `wait` — đơn giản và không phụ thuộc `wait -n`.
running=0
for i in $(seq 1 "$USER_COUNT"); do
  one_user "$i" &
  running=$((running + 1))
  if [ "$running" -ge "$PARALLEL" ]; then
    wait
    running=0
    printf '.' >&2
  fi
done
wait
printf '\n' >&2

step "Gom token"
"$PY" - "$TOKENS_DIR" "$TOKENS_FILE" <<'PYEOF'
import json, os, sys
d, out = sys.argv[1], sys.argv[2]
tokens, seen = [], set()
for fn in sorted(os.listdir(d)):
    if not fn.endswith(".token"):
        continue
    with open(os.path.join(d, fn), encoding="utf-8") as f:
        t = f.read().strip()
    if t and t not in seen:
        seen.add(t)
        tokens.append(t)
with open(out, "w", encoding="utf-8") as f:
    json.dump(tokens, f)
print(len(tokens))
PYEOF

TOTAL="$(ls -1 "$TOKENS_DIR"/*.token 2>/dev/null | wc -l | tr -d ' ')"
ERRORS="$(ls -1 "$TOKENS_DIR"/*.err 2>/dev/null | wc -l | tr -d ' ')"
UNIQUE="$("$PY" -c 'import json,sys;print(len(json.load(open(sys.argv[1],encoding="utf-8"))))' "$TOKENS_FILE")"

log "token lấy được : $TOTAL / $USER_COUNT"
log "token DUY NHẤT : $UNIQUE"
log "lỗi            : $ERRORS  (chi tiết: $TOKENS_DIR/*.err)"
log "đã ghi         : $TOKENS_FILE"

if [ "$ERRORS" != "0" ]; then
  log ""
  log "Ví dụ lỗi đầu tiên:"
  head -n 3 "$(ls -1 "$TOKENS_DIR"/*.err | head -n 1)" >&2 || true
fi

if [ "$UNIQUE" -lt "$USER_COUNT" ]; then
  log ""
  log "CẢNH BÁO: số token duy nhất ($UNIQUE) < USER_COUNT ($USER_COUNT)."
  log "Hãy chạy race với VUS=$UNIQUE, nếu không nhiều VU sẽ dùng lại cùng một token"
  log "và lock re-entrant sẽ làm sai kết quả."
  exit 1
fi

log ""
log "OK — $UNIQUE token duy nhất, đủ cho VUS=$USER_COUNT."
