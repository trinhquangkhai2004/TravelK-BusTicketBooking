# loadtest — chứng minh cơ chế chống double-booking

Bộ script này **đo tính đúng đắn (correctness)**, không đo hiệu năng.

Câu hỏi duy nhất nó trả lời: *khi 200 người dùng khác nhau cùng lúc đặt đúng một
ghế trên đúng một chuyến, có bao giờ hai người cùng lấy được ghế đó không?*

> **Không trích dẫn throughput/RPS từ bộ test này.** Backend, MySQL, Redis và bộ
> sinh tải k6 đều chạy trên cùng một laptop, cạnh tranh cùng một CPU. Mọi con số
> RPS đo được ở đây nói về cái laptop, không nói về hệ thống. p95 latency có in ra
> nhưng chỉ để tham khảo môi trường — luôn kèm cảnh báo này khi nhắc tới nó.

Con số duy nhất đáng đưa vào CV là kết quả của `05-verify.sh`:
**0 ghế bị bán trùng qua N vòng chạy, mỗi vòng 200 người dùng tranh 1 ghế.**

---

## 0. Đọc trước khi chạy — mã lỗi thực tế KHÔNG phải 409

Đây là điểm quan trọng nhất và nó khác với giả định thông thường.

`GlobalExceptionHandler` chỉ map:

| Exception | HTTP |
| --- | --- |
| `ResourceNotFoundException` | 404 |
| `ResourceDuplicateException` | 409 |
| `Exception` (mọi thứ còn lại) | **500** |

`BookingServiceImpl.createBooking` ném **`RuntimeException` trần** khi bị lock Redis
từ chối (`"Ghế X đang có người khác giữ hoặc thao tác!"`) và khi ghế đã bán
(`"Rất tiếc, các ghế trên đã đươc bán: ..."`).

⇒ **`POST /api/booking` trả HTTP 500 cho trường hợp "bị lock chặn", không phải 409.**
409 chỉ xuất hiện ở `POST /api/booking/hold`, vì controller đó tự `catch` và đổi
sang `HttpStatus.CONFLICT`.

Vì vậy `seat-race.js` phân loại theo **(status code + nội dung message)** chứ không
chỉ theo status code, và tách rõ:

| Nhóm | Ý nghĩa |
| --- | --- |
| `booked_2xx` | Đặt thành công (HTTP 201). **Phải đúng bằng 1.** |
| `rejected_lock` | Bị lock Redis chặn — **kết quả ĐÚNG mong đợi** (hiện về dưới dạng HTTP 500 + message "đang có người khác giữ") |
| `rejected_sold` | Lớp phòng thủ thứ 2 chặn (ticket đã tồn tại) — cũng đúng |
| `http_409` | Chỉ >0 nếu app được sửa để map đúng 409 |
| `auth_401_403` | Token sai/thiếu/hết hạn — **sai cấu hình test, phải sửa** |
| `client_4xx_other` | Payload sai — **sai cấu hình test, phải sửa** |
| `other_5xx_noise` | 5xx **không** kèm message lock/sold — nhiễu môi trường, điển hình là cạn HikariCP connection pool (mặc định 10 connection, không cấu hình trong `application.properties`) |
| `transport_or_timeout` | k6 không nhận được response |

Kết quả kỳ vọng, diễn đạt đúng theo hành vi hiện tại của app:

```
booked_2xx           = 1
rejected_lock + rejected_sold ≈ 199   (trả về dưới dạng HTTP 500 + message tiếng Việt)
other_5xx_noise + transport_or_timeout  = càng gần 0 càng tốt
duplicate trong MySQL = 0             <-- con số quyết định
```

Nếu bạn muốn phát biểu "1 × 2xx + 199 × 409" thì phải sửa app để ném một exception
map sang 409 — **bộ script này không sửa app**, chỉ đo và báo cáo.

> Hệ quả phụ cùng nguyên nhân: lỗi `@Valid` (thiếu `busId`/`stationId`/`tripId`)
> cũng trả **500** chứ không phải 400, nên nó sẽ rơi nhầm vào ô
> `other_5xx_noise`. `seat-race.js` luôn gửi đủ 4 field bắt buộc nên trường hợp
> này không xảy ra — nhưng nếu bạn sửa payload thì hãy nhớ điều đó.
>
> Request **không kèm** `Authorization` trả **403** (đã kiểm chứng bằng curl trên
> stack đang chạy), rơi vào ô `auth_401_403`.

---

## 1. Yêu cầu môi trường

- Docker Desktop đang chạy, stack đã lên: `docker-compose up -d --build`
- `curl`, `python` (>= 3.8) trong PATH — Git Bash trên Windows đã có sẵn `curl`
- k6: **không cần cài**. Nếu không có `k6` trong PATH, `04-run-race.sh` tự chạy
  bằng `docker run --rm grafana/k6` (lần đầu sẽ tải image).
- `backend/.env` phải có `MAIL_USERNAME`, `MAIL_PASSWORD`, `GEMINI_API_KEY`,
  `VNPAY_HASH_SECRET`, `JWT_SECRET` — nếu không app không khởi động được.

Kiểm tra nhanh backend đã sống:

```sh
curl -s -o /dev/null -w '%{http_code}\n' http://localhost:8080/api/trips
# mong đợi 200
```

Tên container lấy đúng từ `docker-compose.yml`: `travelK_mysql`, `travelK-redis`.
Override được bằng env `MYSQL_CONTAINER`, `REDIS_CONTAINER`.

---

## 2. Chạy theo đúng thứ tự

Từ Git Bash, đứng trong `loadtest/`:

```sh
cd loadtest

# --- Một lần duy nhất ---
./01-setup-data.sh          # station -> bus (40 ghế A1..D10) -> trip; in ra TRIP_ID
./02-register-users.sh      # 200 user + 200 JWT riêng biệt -> out/tokens.json

# --- Lặp 3 vòng: reset -> race -> verify ---
./run-all.sh
```

Hoặc chạy tay từng vòng (mỗi vòng đủ 3 bước, **không được bỏ bước reset**):

```sh
./03-reset-state.sh   # xoá hold:trip:* trong Redis + tickets/bookings của TRIP_ID
./04-run-race.sh      # 200 VU, 200 token, cùng đặt ghế A1
./05-verify.sh        # đếm vé trùng trong MySQL -> PASS/FAIL
```

### Vì sao bước reset là bắt buộc

Trong `BookingServiceImpl.createBooking`, nhánh "ghế đã được bán" `throw` mà
**không release lock Redis** đã giữ ở vòng lặp phía trên. TTL của lock là 10 phút.

Nếu không reset, từ vòng chạy thứ 2 trở đi mọi request đều bị lock **cũ** chặn
ngay lập tức. Kết quả sẽ là "0 duplicate" trông rất hoàn hảo — nhưng nó chỉ chứng
minh TTL còn hiệu lực, chứ không chứng minh lock chống được tranh chấp. Đó là
số liệu rác. `03-reset-state.sh` xoá cả Redis lẫn MySQL để mỗi vòng bắt đầu sạch.

### Vì sao bắt buộc 200 token khác nhau

`BookingServiceImpl.java` dòng 80–84: khi `setIfAbsent` thất bại, code đọc lại giá
trị lock; nếu người đang giữ **chính là** `userId` của request thì coi như giữ được
và đi tiếp (lock re-entrant theo user). Dùng chung một token cho 200 request sẽ
khiến cả 200 vượt qua lớp lock và bài test mất hết ý nghĩa.

`02-register-users.sh` khử trùng lặp token và **thoát với lỗi** nếu số token duy
nhất < `USER_COUNT`. `04-run-race.sh` cũng chặn nếu số token < `VUS`.

---

## 3. Đọc kết quả

### Bảng của k6 (tầng HTTP — chỉ là chỉ dấu)

```
 PHAN LOAI THEO KET QUA (khong gop success/failed)
   booked_2xx                      1   <- phai dung bang 1
   rejected_lock                 191   <- ket qua DUNG (lock Redis chan)
   rejected_sold                   3   <- ket qua DUNG (lop phong thu 2)
   other_5xx_noise                 5   <- NHIEU moi truong
   ...
```

k6 cũng đặt threshold `booked_2xx: count<=1`, nên k6 exit code khác 0 nghĩa là đã
có nhiều hơn một request đặt được ghế.

### Kết quả của `05-verify.sh` (con số quyết định)

Câu truy vấn:

```sql
SELECT seat_number, COUNT(*) FROM tickets
WHERE trip_id = ? GROUP BY seat_number HAVING COUNT(*) > 1;
```

| Số dòng trả về | Kết luận |
| --- | --- |
| 0 dòng và có đúng 1 vé | **PASS** — không double-booking |
| 0 dòng nhưng 0 vé | **KHÔNG KẾT LUẬN** — không request nào thành công, race chưa thực sự diễn ra (thường là quên reset, hoặc token hết hạn) |
| ≥ 1 dòng | **FAIL** — đã xảy ra double-booking |

Exit code: `0` = PASS, `1` = FAIL, `2` = không kết luận.

### Nếu tỷ lệ 5xx nhiễu cao

Nếu `other_5xx_noise + transport_or_timeout` vượt ~20% tổng số request (k6 sẽ tự
cảnh báo), **hạ VU xuống 100 và chạy lại**:

```sh
VUS=100 ./run-all.sh
```

100 VU sạch có giá trị hơn 200 VU lẫn nhiễu: một bài test 200 VU mà 60 request
chết vì hết connection pool không chứng minh được gì về lock, chỉ chứng minh
laptop hết hơi. Ghi trong CV con số nào bạn thực sự chạy sạch được.

Muốn tiến tới 200 VU sạch thì phải nới HikariCP
(`spring.datasource.hikari.maximum-pool-size`) — đó là **thay đổi cấu hình app,
nằm ngoài phạm vi thư mục này** và không được thực hiện ở đây.

---

## 4. Biến môi trường

| Biến | Mặc định | Ghi chú |
| --- | --- | --- |
| `BASE_URL` | `http://localhost:8080` | backend nhìn từ host |
| `K6_BASE_URL` | `http://host.docker.internal:8080` | backend nhìn từ container k6 |
| `USER_COUNT` | `200` | số user tạo ở bước 02 |
| `VUS` | `200` | số VU khi race |
| `SEAT` | `A1` | ghế bị tranh |
| `ROUNDS` | `3` | số vòng của `run-all.sh` |
| `BARRIER_MS` | `3000` | mọi VU chờ tới một mốc thời gian chung rồi cùng bắn |
| `PARALLEL` | `10` | số curl song song khi tạo user |
| `MYSQL_CONTAINER` / `REDIS_CONTAINER` | `travelK_mysql` / `travelK-redis` | |
| `MYSQL_USER` / `MYSQL_PASSWORD` / `MYSQL_DB` | `root` / `1234` / `bus_reservation_db` | khớp `docker-compose.yml` |
| `ADMIN_LOGIN` / `ADMIN_PASSWORD` | `admin` / `admin` | do `DataInitializer` seed |
| `SEED_ROLE_USER` | `1` | xem mục 5 |

---

## 5. Hai điểm phải biết trước khi tin số liệu

**(a) `ROLE_USER` không được seed.**
`UserServiceImpl.createUser` yêu cầu role `ROLE_USER` tồn tại trong bảng `roles`,
nhưng `DataInitializer` chỉ tạo `ROLE_ADMIN`. Trên DB mới, `POST /api/user` sẽ trả
404 `Role not found`. `02-register-users.sh` chèn thẳng dòng `ROLE_USER` vào bảng
`roles` (chỉ là dữ liệu tham chiếu, **không** sửa code app). Tắt bằng
`SEED_ROLE_USER=0` nếu bạn đã seed bằng cách khác.

**(b) Script này ghi vào database thật.**
Nó tạo station/bus/trip, tạo 200 user `lt_user_0001..0200`, và ở mỗi vòng reset nó
`DELETE` tickets/bookings **của đúng `TRIP_ID` đó** cùng **toàn bộ key Redis
`hold:trip:*`**. Chạy trên DB dev, đừng chạy trên dữ liệu bạn cần giữ.

---

## 6. Cấu trúc thư mục

```
loadtest/
  README.md
  lib/common.sh          # cấu hình + helper (curl/mysql/redis/json)
  01-setup-data.sh       # station -> bus -> trip, in TRIP_ID, ghi out/trip.env
  02-register-users.sh   # 200 user + 200 JWT riêng -> out/tokens.json
  03-reset-state.sh      # xoá hold:trip:* + tickets/bookings của TRIP_ID
  seat-race.js           # k6: 200 VU / 200 token / 1 ghế, phân loại theo status
  04-run-race.sh         # chạy k6 (bản cài trên máy, hoặc qua Docker)
  05-verify.sh           # đếm vé trùng trong MySQL -> PASS/FAIL
  run-all.sh             # lặp 3 vòng: reset -> race -> verify
  out/                   # sinh ra khi chạy (trip.env, tokens.json, race-summary*.json)
```

`out/` chứa JWT thật của các user test — không commit thư mục này.
