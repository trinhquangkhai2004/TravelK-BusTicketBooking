/*
 * seat-race.js — k6 race test cho cơ chế chống double-booking.
 *
 * MỤC TIÊU: đo TÍNH ĐÚNG ĐẮN, không đo hiệu năng.
 * Stack (Spring Boot + MySQL + Redis) và bộ sinh tải chạy chung một laptop, nên
 * mọi con số throughput/RPS ở đây đều vô nghĩa và KHÔNG được trích dẫn.
 * p95 latency có in ra nhưng chỉ để tham khảo môi trường.
 *
 * Kịch bản: VUS người dùng KHÁC NHAU (mỗi VU một JWT riêng) cùng POST /api/booking
 * để đặt CÙNG MỘT ghế trên CÙNG MỘT trip, gần như đồng thời.
 *
 * Contract đã đối chiếu với code hiện tại trong working tree:
 *   - BookingController.java:23-27   -> POST /api/booking, trả 201 CREATED khi thành công
 *   - SecurityConfig.java:44-51      -> /api/booking/** KHÔNG còn permitAll => bắt buộc
 *                                       header Authorization: Bearer
 *   - BookingRequestDto.java         -> busId, stationId, tripId @NotNull; seats @NotEmpty.
 *                                       KHÔNG còn field userId — userId lấy từ token.
 *   - BookingServiceImpl.java:60     -> getCurrentUserId() đọc principal, không đọc body.
 *
 * PHÂN LOẠI KẾT QUẢ — đọc kỹ:
 *   GlobalExceptionHandler chỉ map ResourceNotFoundException->404,
 *   ResourceDuplicateException->409, còn lại Exception->500. BookingServiceImpl ném
 *   `RuntimeException` trần khi bị lock từ chối, nên đường dẫn POST /api/booking trả
 *   HTTP 500 (KHÔNG phải 409) cho trường hợp "bị lock chặn". 409 chỉ xuất hiện ở
 *   POST /api/booking/hold vì controller đó tự catch.
 *   => Script phân loại theo (status code + nội dung message) để tách:
 *        rejected_lock  : bị lock Redis chặn  -> ĐÂY LÀ KẾT QUẢ ĐÚNG MONG ĐỢI
 *        rejected_sold  : lớp phòng thủ 2 (ticket đã tồn tại) -> cũng đúng
 *        other_5xx      : nhiễu môi trường (ví dụ cạn HikariCP pool, mặc định 10)
 *      Tuyệt đối không gộp thành "success/failed".
 */

import http from 'k6/http';
import exec from 'k6/execution';
import { sleep } from 'k6';
import { Counter, Trend } from 'k6/metrics';

/* ----------------------------- cấu hình ----------------------------- */

const BASE_URL = __ENV.BASE_URL || 'http://host.docker.internal:8080';
const TOKENS_FILE = __ENV.TOKENS_FILE || './out/tokens.json';
const TRIP_ID = parseInt(__ENV.TRIP_ID, 10);
const BUS_ID = parseInt(__ENV.BUS_ID || '1', 10);
const STATION_ID = parseInt(__ENV.STATION_ID || '1', 10);
const SEAT = __ENV.SEAT || 'A1';
const VUS = parseInt(__ENV.VUS || '200', 10);
const BARRIER_MS = parseInt(__ENV.BARRIER_MS || '3000', 10);
const REQ_TIMEOUT = __ENV.REQ_TIMEOUT || '120s';

const TOKENS = JSON.parse(open(TOKENS_FILE));

if (!TRIP_ID) {
  throw new Error('Thiếu biến môi trường TRIP_ID. Chạy 01-setup-data.sh trước.');
}
if (TOKENS.length < VUS) {
  throw new Error(
    'Chỉ có ' + TOKENS.length + ' token nhưng VUS=' + VUS + '. ' +
    'Mỗi VU BẮT BUỘC một token riêng, nếu không lock re-entrant theo user ' +
    '(BookingServiceImpl:80-84) sẽ cho nhiều request cùng vượt qua lớp lock. ' +
    'Chạy lại 02-register-users.sh hoặc hạ VUS xuống ' + TOKENS.length + '.'
  );
}

/* ------------------------- metric phân loại ------------------------- */

const cBooked = new Counter('booked_2xx');          // đặt thành công
const cRejLock = new Counter('rejected_lock');       // bị lock Redis chặn  (ĐÚNG)
const cRejSold = new Counter('rejected_sold');       // ghế đã bán          (ĐÚNG)
const cHttp409 = new Counter('http_409');            // 409 thật (nếu app đổi mapping)
const cAuth = new Counter('auth_401_403');           // token sai/thiếu
const cBadReq = new Counter('client_4xx_other');     // 400/404/...
const cOther5xx = new Counter('other_5xx_noise');    // 5xx KHÔNG phải lock/sold
const cTransport = new Counter('transport_or_timeout'); // status 0

// Đếm thô theo status code
const codeCounters = {
  '0': new Counter('status_0'),
  '200': new Counter('status_200'),
  '201': new Counter('status_201'),
  '400': new Counter('status_400'),
  '401': new Counter('status_401'),
  '403': new Counter('status_403'),
  '404': new Counter('status_404'),
  '409': new Counter('status_409'),
  '500': new Counter('status_500'),
  '502': new Counter('status_502'),
  '503': new Counter('status_503'),
  '504': new Counter('status_504'),
};
const cStatusOther = new Counter('status_other');
const tLatency = new Trend('booking_call_ms', true);

export const options = {
  scenarios: {
    seat_race: {
      // per-vu-iterations: k6 khởi tạo TOÀN BỘ VU trước, rồi thả cùng lúc, mỗi VU
      // đúng 1 iteration. Chọn nó thay vì shared-iterations để giữ bất biến
      // 1 VU <-> 1 token: với shared-iterations một VU nhanh có thể chạy iteration
      // thứ hai bằng CHÍNH token đó và lọt qua lock re-entrant, làm bẩn số đếm.
      executor: 'per-vu-iterations',
      vus: VUS,
      iterations: 1,
      maxDuration: '5m',
    },
  },
  // Sai số duy nhất không chấp nhận được: nhiều hơn 1 booking thành công.
  thresholds: {
    booked_2xx: ['count<=1'],
  },
  // Không dùng số throughput -> tắt các summary trend không cần thiết là không cần,
  // nhưng ghi rõ ở README rằng RPS không được trích dẫn.
  discardResponseBodies: false,
};

/* --------------------------- barrier khởi động ---------------------- */

export function setup() {
  return { startAtMs: Date.now() + BARRIER_MS, tokenCount: TOKENS.length };
}

/* ------------------------------ kịch bản ---------------------------- */

// Các mảnh chuỗi tiếng Việt trích từ message của BookingServiceImpl.
// File này lưu UTF-8; k6 đọc source và body response đều theo UTF-8.
// Mỗi nhóm có thêm một mảnh dự phòng để không phụ thuộc một chuỗi duy nhất.
// Hai nhóm không giao nhau: message lock không chứa "bán:"/"tiếc",
// message sold không chứa "giữ"/"thao t".
const MSG_LOCK = 'đang có người khác giữ'; // "dang co nguoi khac giu"
const MSG_LOCK_ALT = 'thao t';                                          // "...hoac thao tac!"
const MSG_SOLD = 'bán:';                                           // "...da duoc ban: A1"
const MSG_SOLD_ALT = 'tiếc';                                       // "Rat tiec, ..."

function looksLikeLock(body) {
  return body.indexOf(MSG_LOCK) !== -1 || body.indexOf(MSG_LOCK_ALT) !== -1;
}
function looksLikeSold(body) {
  return body.indexOf(MSG_SOLD) !== -1 || body.indexOf(MSG_SOLD_ALT) !== -1;
}

export default function (data) {
  // Mỗi VU một token riêng — bất biến quan trọng nhất của test này.
  const token = TOKENS[exec.vu.idInTest - 1];

  // Barrier: chờ tới mốc thời gian chung để 200 request bắn gần như đồng thời.
  const wait = data.startAtMs - Date.now();
  if (wait > 0) {
    sleep(wait / 1000);
  }

  const payload = JSON.stringify({
    tripId: TRIP_ID,
    busId: BUS_ID,
    stationId: STATION_ID,
    seats: [SEAT],
  });

  const res = http.post(BASE_URL + '/api/booking', payload, {
    headers: {
      'Content-Type': 'application/json',
      Accept: 'application/json',
      Authorization: 'Bearer ' + token,
    },
    timeout: REQ_TIMEOUT,
    tags: { name: 'POST /api/booking' },
  });

  const status = res.status;
  const body = res.body || '';

  (codeCounters[String(status)] || cStatusOther).add(1);
  if (status !== 0) {
    tLatency.add(res.timings.duration);
  }

  if (status === 0) {
    cTransport.add(1);
  } else if (status >= 200 && status < 300) {
    cBooked.add(1);
    console.log('VU ' + exec.vu.idInTest + ' WON seat ' + SEAT + ' -> HTTP ' + status);
  } else if (status === 409) {
    cHttp409.add(1);
    if (looksLikeLock(body)) cRejLock.add(1);
    else if (looksLikeSold(body)) cRejSold.add(1);
  } else if (status === 401 || status === 403) {
    cAuth.add(1);
  } else if (looksLikeLock(body)) {
    cRejLock.add(1);
  } else if (looksLikeSold(body)) {
    cRejSold.add(1);
  } else if (status >= 500) {
    cOther5xx.add(1);
  } else {
    cBadReq.add(1);
  }
}

/* ------------------------------ báo cáo ----------------------------- */

function count(data, name) {
  const m = data.metrics[name];
  return m && m.values && m.values.count ? m.values.count : 0;
}

function row(label, value, note) {
  const pad = (label + '                          ').slice(0, 26);
  const v = ('        ' + value).slice(-7);
  return '  ' + pad + v + (note ? '   ' + note : '') + '\n';
}

export function handleSummary(data) {
  const booked = count(data, 'booked_2xx');
  const lock = count(data, 'rejected_lock');
  const sold = count(data, 'rejected_sold');
  const auth = count(data, 'auth_401_403');
  const bad = count(data, 'client_4xx_other');
  const noise = count(data, 'other_5xx_noise');
  const trans = count(data, 'transport_or_timeout');
  const total = booked + lock + sold + auth + bad + noise + trans;

  const p95 =
    data.metrics.booking_call_ms && data.metrics.booking_call_ms.values
      ? Math.round(data.metrics.booking_call_ms.values['p(95)'])
      : 'n/a';

  let s = '';
  s += '\n';
  s += '=====================================================================\n';
  s += ' RACE TEST — 1 ghe, ' + VUS + ' user khac nhau, ' + VUS + ' token khac nhau\n';
  s += ' TRIP_ID=' + TRIP_ID + '  SEAT=' + SEAT + '  BASE_URL=' + BASE_URL + '\n';
  s += '=====================================================================\n\n';

  s += ' PHAN LOAI THEO KET QUA (khong gop success/failed)\n';
  s += row('booked_2xx', booked, '<- phai dung bang 1');
  s += row('rejected_lock', lock, '<- ket qua DUNG (lock Redis chan)');
  s += row('rejected_sold', sold, '<- ket qua DUNG (lop phong thu 2)');
  s += row('auth_401_403', auth, auth ? '<- token sai/het han, KIEM TRA LAI' : '');
  s += row('client_4xx_other', bad, bad ? '<- payload sai, KIEM TRA LAI' : '');
  s += row('other_5xx_noise', noise, '<- NHIEU moi truong (vd het HikariCP pool)');
  s += row('transport_or_timeout', trans, '<- NHIEU moi truong');
  s += row('TONG', total, '');
  s += '\n';

  s += ' DEM THO THEO HTTP STATUS\n';
  const codes = ['0', '200', '201', '400', '401', '403', '404', '409', '500', '502', '503', '504'];
  for (let i = 0; i < codes.length; i++) {
    const c = count(data, 'status_' + codes[i]);
    if (c > 0) s += row('HTTP ' + codes[i], c, '');
  }
  const other = count(data, 'status_other');
  if (other > 0) s += row('HTTP khac', other, '');
  s += '\n';

  s += ' KET LUAN TAM THOI (o tang HTTP)\n';
  if (booked === 1) {
    s += '   PASS(HTTP): dung 1 request dat duoc ghe.\n';
  } else if (booked === 0) {
    s += '   FAIL(HTTP): 0 request nao dat duoc ghe — race chua dien ra dung\n';
    s += '               (rat co the chua chay 03-reset-state.sh, lock cu con TTL).\n';
  } else {
    s += '   FAIL(HTTP): ' + booked + ' request cung dat duoc ghe -> DOUBLE BOOKING.\n';
  }
  const noiseTotal = noise + trans;
  if (noiseTotal > VUS * 0.2) {
    s += '   CANH BAO: nhieu moi truong = ' + noiseTotal + '/' + VUS +
      ' (>20%). Ha VUS xuong 100 va chay lai;\n' +
      '             100 VU sach co gia tri hon 200 VU lan nhieu.\n';
  }
  s += '\n';

  s += ' LATENCY (chi de tham khao)\n';
  s += '   p95 = ' + p95 + ' ms\n';
  s += '   CANH BAO MOI TRUONG: app + MySQL + Redis + k6 chay chung MOT laptop.\n';
  s += '   KHONG trich dan throughput/RPS tu lan chay nay — con so do vo nghia.\n';
  s += '\n';
  s += ' BUOC BAT BUOC TIEP THEO: chay ./05-verify.sh — so duplicate trong MySQL\n';
  s += ' moi la con so quyet dinh, khong phai bang tren.\n';
  s += '=====================================================================\n';

  return {
    stdout: s,
    './out/race-summary.json': JSON.stringify(
      {
        tripId: TRIP_ID,
        seat: SEAT,
        vus: VUS,
        buckets: {
          booked_2xx: booked,
          rejected_lock: lock,
          rejected_sold: sold,
          auth_401_403: auth,
          client_4xx_other: bad,
          other_5xx_noise: noise,
          transport_or_timeout: trans,
        },
        p95_ms: p95,
        note: 'Correctness test only. Throughput/RPS from this run is meaningless.',
      },
      null,
      2
    ),
  };
}
