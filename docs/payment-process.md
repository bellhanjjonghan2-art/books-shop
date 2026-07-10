# 결제 프로세스 설계 — 토스페이먼츠(v2 결제위젯)

작성: 결제 기능 연동 설계
연동 방식: **토스페이먼츠 결제위젯 (SDK v2, `@tosspayments/tosspayments-sdk`) — 결제창형(팝업)**
결제 흐름: **연동 키 기반 결제 인증 → 서버 승인(Confirm)** 2단계 흐름

> 이 문서는 구현 전 청사진이다. 실제 코드/커밋은 이 문서 확정 후 진행한다.
> 물리 DB 작업(테이블 생성·데이터 조작)은 하지 않으며, 스키마는 `scripts/create_table.sql`에 이미 설계된 것을 그대로 따른다.

---

## 0. 전제 (이미 준비된 것)

| 구분 | 상태 | 위치 |
|---|---|---|
| DB 스키마 | 완료 (토스 연동 전제 설계) | `scripts/create_table.sql` — `orders` / `order_items` / `payments` / `payment_cancels` / `deliveries` |
| 주문서 화면 | 완료 (필수값 검증까지) | `shop-front/src/pages/Order/Order.jsx` — `handlePayment()`에 "실제 결제는 후속 단계" 주석 |
| 주문 도서 조회 API | 완료 | `GET /api/books/order-items` (`docs/specs/api-contract.md`) |
| axios 공용 인스턴스 | 완료 (JWT 자동 첨부) | `shop-front/src/api/client.js` |

핵심 스키마 설계 의도(그대로 준수):
- `orders.id` = **토스 `orderId`로 그대로 사용** (UUID)
- `orders.total_amount` = **승인 전 금액 위변조 검증의 기준값**
- `payments.payment_key` = 토스 `paymentKey` (UNIQUE, 취소·조회의 키)
- `uq_payments_order_done` = **승인 성공(DONE)은 주문당 1건만** 허용 (부분 유니크 인덱스)
- `deliveries` = **결제 DONE 확정 시 생성**, 주문당 1건

---

## 1. 전체 흐름 (5단계)

```
[1] 주문 생성        프론트 → POST /api/orders → orders/order_items INSERT (status=PENDING), orderId 반환
[2] 결제 요청        프론트 SDK widgets.requestPayment(orderId, amount, successUrl, failUrl)
[3] 결제창 인증      토스 결제창에서 구매자가 결제수단 인증
[4] 리다이렉트       성공/실패 모두 /payments/result 로 이동 (같은 경로)
                     성공 시 ?paymentKey&orderId&amount&paymentType / 실패 시 ?code&message&orderId
                     → 페이지가 쿼리로 성공/실패 분기
[5] 서버 승인        (성공 분기일 때) 프론트 → POST /api/payments/confirm → 서버가 amount 재검증 후 토스 승인 API 호출
                     → 200 OK: payments(DONE)+deliveries 생성, orders=PAID   → 실패: orders=FAILED, payments(ABORTED)
```

**설계 원칙 (토스 공식 가이드 준수):**
1. **금액은 서버가 신뢰의 근원**이다. 클라이언트가 보낸 `amount`는 믿지 않는다. `orders.total_amount`와 비교해 일치할 때만 승인한다.
2. **승인(Confirm)은 반드시 백엔드에서** 시크릿 키로 호출한다. 시크릿 키는 프론트/깃허브에 절대 노출하지 않는다.
3. 결제 요청 완료 후 **10분 이내 승인**해야 한다(토스 세션 만료).
4. `orders`는 결제 요청 **전에** 생성해 `orderId`를 확보한다(금액 검증 기준을 서버가 먼저 확정).

---

## 2. 단계별 상세

### [1] 주문 생성 — `POST /api/orders`

주문서에서 '결제하기'를 누르면(필수값 검증 통과 후) **결제창을 띄우기 전에** 먼저 주문을 만든다.

- 인증: JWT 필요
- 요청 본문:
  ```json
  {
    "items": [{ "bookId": "IT-IT-6FB622", "quantity": 2 }],
    "delivery": {
      "receiverName": "홍길동", "receiverPhone": "010-0000-0000",
      "postCode": "06236", "address": "서울시 ...", "addrDetail": "101동 1001호",
      "deliveryMemo": "부재 시 경비실"
    }
  }
  ```
- 서버 처리:
  1. `bookId`들의 현재 `sale_price`·`title`·재고를 DB에서 조회(**가격은 서버가 확정** — 프론트가 보낸 금액 신뢰 안 함).
  2. `orders` INSERT: `id`=UUID, `user_id`, `order_name`(예: `"클린 코드 외 2건"`), `status='PENDING'`, `total_amount`=서버 계산 합계.
  3. `order_items` INSERT: 도서별 `unit_price`(스냅샷), `quantity`, `amount`.
  4. **배송 입력값은 주문 생성 시 서버에 저장·보관한다(확정)**. 스키마상 `deliveries`는 결제 성공 시 생성하므로, 이 시점에는 `deliveries` 행을 만들지 않고 배송 정보를 서버가 보관해 둔다.
     - 재고는 이 시점에 차감하지 않는다(차감은 승인 성공 시 — §2-[5]).
     - 보관 위치는 구현 단계 결정(§5-(a) 참조): 배송정보 전용 임시 테이블/컬럼 추가 없이 처리하려면, `deliveries` 행을 주문 생성 시 미리 INSERT하고 `status`를 별도 상태로 두는 방안도 검토(스키마상 `deliveries.order_id`는 UNIQUE·NOT NULL). 단, "결제 성공 시 저장" 요구에 맞추려면 **주문 생성 시 배송정보를 서버 보관 → 승인 성공 시 `deliveries` INSERT** 가 기본 방향이다.
- 응답:
  ```json
  { "success": true, "data": { "orderId": "uuid", "orderName": "클린 코드 외 2건", "amount": 59400 } }
  ```

### [2]~[3] 결제 요청 & 인증 — 프론트 SDK (**결제창형/팝업** 확정)

`Order.jsx`의 `handlePayment()`가 [1]로 `orderId`/`amount`를 받은 뒤 SDK로 **팝업 결제창**을 연다.
현재 Order 화면의 자체 요약 카드·결제하기 버튼 UI를 그대로 유지하고, 버튼 클릭 시 결제창을 띄운다(화면에 결제수단 UI를 임베드하지 않음).

```js
import { loadTossPayments } from '@tosspayments/tosspayments-sdk'

const clientKey = import.meta.env.VITE_TOSS_CLIENT_KEY // 테스트 클라이언트 키
const tossPayments = await loadTossPayments(clientKey)
// 회원 결제: customerKey는 로그인 사용자 id 사용 (useAuthStore.user.userId)
const customerKey = useAuthStore.getState().user.userId
const widgets = tossPayments.widgets({ customerKey })

// 1) 금액 설정
await widgets.setAmount({ currency: 'KRW', value: amount })

// 2) 결제창(팝업) 렌더링 — 결제창형은 renderPaymentWindow 사용
const paymentWindow = await widgets.renderPaymentWindow({
  variantKey: { paymentMethod: 'DEFAULT', agreement: 'AGREEMENT' },
})

// 3) 구매자가 결제수단을 선택하면 paymentRequest 이벤트 → 결제 요청
paymentWindow.on('paymentRequest', async ({ paymentMethod }) => {
  try {
    await widgets.requestPayment({
      orderId,                     // = orders.id (서버가 발급)
      orderName,                   // = orders.order_name
      // 성공/실패를 한 페이지에서 처리: 같은 경로를 양쪽에 지정한다.
      // 토스가 성공 시엔 ?paymentKey&orderId&amount&paymentType,
      //         실패 시엔 ?code&message&orderId 를 붙여 이동시킨다.
      successUrl: window.location.origin + '/payments/result',
      failUrl:    window.location.origin + '/payments/result',
      customerName: receiver,
      customerMobilePhone: phone,  // 형식은 토스 요구사항 확인
    })
  } catch (error) {
    console.error(error)
  }
})
```

> **확정: 결제창형(팝업)**. 주문서형(`renderPaymentMethods` 임베드)은 사용하지 않는다.
> 다만 `renderPaymentWindow`를 언제 호출할지(버튼 클릭 시 vs 마운트 시)는 SDK 동작에 맞춰 구현 단계에서 확정한다.

### [4] 리다이렉트 처리 — **단일 결과 페이지**로 성공/실패 통합

**확정: 성공/실패를 한 페이지에서 처리한다.** `successUrl`·`failUrl`에 같은 경로(`/payments/result`)를 지정하고,
페이지가 마운트될 때 **넘어온 결과 데이터(쿼리 파라미터)로 성공/실패를 분기**한다.

라우터(`router.jsx`)의 `MainLayout` 자식으로 1개만 추가(프로젝트 레이아웃 규약 준수).

| 경로 | 컴포넌트 | 역할 |
|---|---|---|
| `/payments/result` | `PaymentResultPage` | 쿼리 파라미터로 성공/실패 분기 처리 |

**분기 규칙** (마운트 시 쿼리 파라미터 판정):

| 조건 | 판정 | 처리 |
|---|---|---|
| `paymentKey` + `orderId` + `amount` 존재 | **성공(인증)** | [5] 승인 API(`POST /api/payments/confirm`) 호출 → 응답으로 성공/실패 최종 확정 |
| `code` + `message` 존재 (`paymentKey` 없음) | **실패(인증)** | 승인 API 호출하지 않고, `code`/`message`로 실패 안내·재시도 유도 |

```jsx
// PaymentResultPage 개요
const [params] = useSearchParams()
const paymentKey = params.get('paymentKey')
const code = params.get('code')

useEffect(() => {
  if (paymentKey) {
    // 성공(인증) → 서버 승인 요청 (마운트 시 1회, 중복 승인 방지 가드)
    confirm({ paymentKey, orderId: params.get('orderId'), amount: Number(params.get('amount')) })
  }
  // 실패(인증)면 code/message로 화면 상태만 실패로 세팅
}, [])
```

- 화면 상태(state machine): `PENDING`(승인 요청 중) → `SUCCESS`(confirm 200) / `FAILED`(인증 실패 or confirm 실패).
- 결과 화면 디자인은 이미 존재하므로, 성공/실패 두 상태를 이 한 페이지 안에서 조건부 렌더한다.
- **중복 승인 방지**: 승인 API 호출은 마운트 시 1회만(가드). React StrictMode/새로고침 대비.

#### [4-A] 성공 화면 (참고 디자인: `docs/resources/order-complete.png`)

`SUCCESS` 상태일 때 아래 구성으로 렌더한다. 데이터는 **주문 결과 조회 API `GET /api/orders/{orderId}`**(§3, 신규 구현)로 받는다.
승인(confirm) 성공 후 그 응답만으로도 1차 표시가 가능하지만, 완전한 화면(배송 정보·주문자 정보 포함)을 위해 결과 조회 API를 호출해 채운다.

| 영역 | 구성 | 데이터 소스 |
|---|---|---|
| ① 완료 헤더 | 체크 아이콘 + "주문이 완료되었습니다" + **주문번호 뱃지** + 안내문구 2줄("주문하신 상품은 결제 확인 후 배송이 시작됩니다." / "배송 현황은 마이페이지에서 확인하실 수 있습니다.") | 주문번호 = `orders.id`(**UUID 그대로 표시**) |
| ② 주문 상품 | "주문 상품" + "총 N종 M권", 상품별 [표지·제목·저자·출판사·수량·금액] | `order_items` 스냅샷(제목·수량·금액) + `books`(표지·저자·출판사) |
| ③ 금액 요약 | 상품 금액 + 배송비(무료) = 최종 결제금액 | `orders.total_amount`, 배송비 0(무료) |
| ④ 주문자 정보 | 이름 / 연락처 / 이메일 / **결제수단** / 주문일 | `users`(이름·연락처·이메일) + **결제수단 = `payments.method` 실제값** + 주문일 = `orders.created_at` |
| ⑤ 배송 정보 | 수령인 / 연락처 / 주소 / 요청사항 / **배송 상태 뱃지("결제 확인 중")** | `deliveries`(수령인·연락처·주소·메모·status) |
| ⑥ 하단 버튼 | "홈으로 돌아가기"(→ `/`) + **"주문 내역 확인"(비활성/준비중)** | — |

- **주문번호**: `orders.id`(UUID)를 그대로 표시(별도 날짜형 가공 안 함).
- **결제수단**: `payments.method` 실제값(카드/간편결제 등)을 표시. 결제수단 아이콘은 유지.
- **주문 내역 확인 버튼**: 레이아웃 유지하되 **비활성(disabled)/준비중** 처리. 마이페이지/주문내역 페이지 생기면 연결(차후).
- 배송 상태 뱃지: `deliveries.status='PREPARING'`을 "결제 확인 중"으로 표기(문구 매핑).

#### [4-B] 실패 화면 (요구사항: 최소 구성)

`FAILED` 상태일 때는 **주문 상품·주문자·배송 정보 등 상세를 일절 렌더하지 않는다.** 아래만 표시한다.

- "결제에 실패했습니다" 문구(실패 아이콘 + 안내). 필요 시 토스 `message`를 보조 안내로 노출.
- **"홈으로 돌아가기" 버튼만** 표시(→ `/`). 재시도/주문내역 등 다른 버튼 없음.

> 실패는 인증 실패(쿼리 `code`/`message`)와 승인 실패(confirm 4XX/5XX) 모두 동일한 실패 화면으로 처리한다.

### [5] 서버 승인 — `POST /api/payments/confirm`

프론트 success 페이지가 호출. **여기서 실제 결제가 확정된다.**

- 인증: JWT 필요
- 요청 본문: `{ "paymentKey": "...", "orderId": "...", "amount": 59400 }`
- 서버 처리(트랜잭션):
  1. `orderId`로 `orders` 조회. 없으면 `ORDER_NOT_FOUND`, 이미 `PAID`면 `ORDER_ALREADY_PAID`(멱등 처리).
  2. **금액 검증**: `orders.total_amount == 요청 amount` 아니면 승인 중단 → `PAYMENT_AMOUNT_MISMATCH`.
  3. 토스 **결제 승인 API** 호출:
     - `POST https://api.tosspayments.com/v1/payments/confirm`
     - 헤더: `Authorization: Basic base64(SECRET_KEY + ":")` (시크릿 키 뒤 콜론, base64) — **시크릿 키는 서버 환경변수**.
     - 본문: `{ paymentKey, orderId, amount }`
  4. **200 OK (Payment 객체)** → 아래를 하나의 트랜잭션으로 처리(확정):
     - **(4-1) `payments` INSERT**: `payment_key`, `status='DONE'`, `method`, `easy_pay_provider`, `total_amount`, `balance_amount`, `supplied_amount`, `vat`, `tax_free_amount`, `currency`, `requested_at`, `approved_at`, `receipt_url`, `last_transaction_key`, `raw_response`(JSONB 원본).
     - **(4-2) `orders.status='PAID'`**.
     - **(4-3) 재고 차감 + 재고 부족 방어(확정)**: `order_items`의 각 항목에 대해 `books.stocks -= quantity`.
       - 차감 전 `stocks >= quantity` 검증. **조건부 UPDATE**로 원자적 차감 권장:
         `UPDATE books SET stocks = stocks - :qty WHERE id = :bookId AND stocks >= :qty` → 영향 행 수가 0이면 재고 부족.
       - **재고 부족 시**: 이미 토스 결제가 승인(DONE)된 상태이므로 되돌릴 수 없다. → **토스 결제 취소 API 호출로 자동 환불**한다.
         - `POST https://api.tosspayments.com/v1/payments/{paymentKey}/cancel`, 본문 `{ "cancelReason": "재고 부족" }` (동일 시크릿 키 인증).
         - 트랜잭션 롤백(재고·orders·payments 미확정), `orders.status='CANCELED'` 또는 `FAILED`로 기록, 취소 이력은 `payment_cancels`에 반영.
         - 프론트에는 `OUT_OF_STOCK` 에러로 응답(자동 환불 안내).
     - **(4-4) `deliveries` INSERT**: [1]에서 서버가 보관한 배송 정보로 생성, `status='PREPARING'`.
     - **(4-5) 장바구니 유지(확정)**: 주문 항목을 `cart_items`에서 삭제하지 않는다(§5-(d)).
  5. **4XX/5XX (승인 실패, 에러 객체)** → `orders.status='FAILED'`, 필요 시 `payments`에 `status='ABORTED'`+`fail_code`/`fail_message` 기록. 재고·배송은 손대지 않음.
- 응답(성공):
  ```json
  { "success": true, "data": { "orderId": "uuid", "orderName": "클린 코드 외 2건",
    "method": "카드", "totalAmount": 59400, "approvedAt": "2026-...", "receiptUrl": "https://..." } }
  ```

---

## 3. 신규 API 요약 (backend-api-conventions / backend-api-response 규약 적용)

| Method | URL | 인증 | 역할 |
|---|---|---|---|
| `POST` | `/api/orders` | JWT | 주문 생성(PENDING), `orderId`/`amount` 발급 |
| `POST` | `/api/payments/confirm` | JWT | 금액 재검증 + 토스 승인 + DONE 확정 |
| `POST` | `/api/payments/webhook` | 없음(서명검증) | (권장·보강) 가상계좌 입금·상태변경 수신 |
| `GET`  | `/api/orders/{orderId}` | JWT | **주문 결과 조회** — 성공 화면(§[4-A]) 렌더용. 이번 구현 대상 |

응답은 모두 `ApiResponse`(`{success, data}`) / `ErrorResponse`(`{success, code, message}`) 포맷.

### 3-1. `GET /api/orders/{orderId}` — 주문 결과 조회 (성공 화면용)

- 인증: JWT 필요. **본인 주문만 조회 가능**(`orders.user_id == 토큰의 userId` 아니면 `FORBIDDEN`/404 처리).
- Path: `orderId` = `orders.id`(UUID)
- 성공 응답(`200 OK`):
  ```json
  {
    "success": true,
    "data": {
      "orderId": "uuid",
      "orderName": "어둠 속의 빛 외 1건",
      "status": "PAID",
      "createdAt": "2026-07-09",
      "items": [
        { "bookId": "NOVEL-NV-1234", "title": "어둠 속의 빛", "author": "김민준",
          "publisher": "문학과지성사", "coverImage": "https://...", "quantity": 1, "amount": 18000 }
      ],
      "productAmount": 48000,
      "deliveryFee": 0,
      "totalAmount": 48000,
      "orderer": {
        "name": "신동열", "phone": "010-4096-7353", "email": "dongnyeol@email.com",
        "method": "카드"
      },
      "delivery": {
        "receiverName": "신동열", "receiverPhone": "010-4096-7353",
        "postCode": "13642", "address": "경기도 성남시 수정구 위례중앙로 190",
        "addrDetail": "(창곡동, 위례자이더시티) 5017동 1103호",
        "deliveryMemo": "문 앞에 놔주세요", "status": "PREPARING"
      }
    }
  }
  ```

#### data 필드 매핑

| 필드 | 소스 | 비고 |
|---|---|---|
| `orderId` | `orders.id` | UUID 그대로(화면 주문번호로 표시) |
| `orderName` | `orders.order_name` | "○○ 외 N건" |
| `status` | `orders.status` | `PAID` 등 |
| `createdAt` | `orders.created_at` | 주문일(ISO `YYYY-MM-DD`) |
| `items[].title` / `quantity` / `amount` | `order_items`(스냅샷) | 주문 시점 값 |
| `items[].author` / `publisher` / `coverImage` | `books`(JOIN, `book_id`) | 표시용 |
| `productAmount` | `order_items.amount` 합 | = 상품 금액 |
| `deliveryFee` | 0(무료 고정, 현 정책) | |
| `totalAmount` | `orders.total_amount` | 최종 결제금액 |
| `orderer.name/phone/email` | `users`(JOIN, `orders.user_id`) | 주문자 정보 |
| `orderer.method` | `payments.method`(해당 주문의 DONE 결제) | **실제 결제수단** |
| `delivery.*` | `deliveries`(`order_id`) | 수령인·주소·메모·상태 |

#### 에러 응답

| 상황 | HTTP | code |
|---|---|---|
| 존재하지 않는 주문 | 404 | `ORDER_NOT_FOUND` |
| 타인 주문 조회 | 404 | `ORDER_NOT_FOUND`(존재 노출 방지 위해 404 통일) |
| 서버 오류 | 500 | `INTERNAL_ERROR` |

> 실패 화면(§[4-B])은 주문 상세를 렌더하지 않으므로 이 API를 호출하지 않는다. 성공(`SUCCESS`) 상태에서만 호출한다.

신규 에러코드(기존 `INVALID_INPUT` 재사용 금지, 대상_상태 네이밍 컨벤션):
- `PAYMENT_AMOUNT_MISMATCH` — 금액 불일치
- `PAYMENT_CONFIRM_FAILED` — 토스 승인 실패(원본 code/message 함께)
- `ORDER_NOT_FOUND` — 존재하지 않는 주문
- `ORDER_ALREADY_PAID` — 이미 결제 완료된 주문(멱등)
- `OUT_OF_STOCK` — 승인 성공 후 재고 부족 → 자동 환불(결제 취소) 처리됨

---

## 4. 보안·안정성 체크리스트 (토스 가이드 기반)

- [ ] 시크릿 키는 서버 환경변수. 프론트/저장소에 노출 금지.
- [ ] 승인 API는 **백엔드에서만** 호출.
- [ ] 승인 전 `amount == orders.total_amount` 검증.
- [ ] 결제 요청 후 **10분 내 승인**(초과 시 `NOT_FOUND_PAYMENT_SESSION`).
- [ ] 승인 성공은 주문당 1건(`uq_payments_order_done`) — 중복 승인 멱등 처리.
- [ ] `raw_response`(Payment 객체 원본) 저장 → 정산·CS 대비.
- [ ] **재고 차감은 원자적 조건부 UPDATE**(`... WHERE stocks >= :qty`)로 동시성 안전 확보.
- [ ] **승인 후 재고 부족 시 토스 결제 취소 API로 자동 환불** — 결제만 되고 재고 없는 상태 방지.
- [ ] 재고 차감·`deliveries` 생성·`orders`/`payments` 확정은 **단일 트랜잭션**.
- [ ] (권장) 웹훅으로 가상계좌 입금 등 비동기 상태 반영.

---

## 5. 확정 필요 항목 (추측 금지 — 구현 전 결정)

- (a) **배송 정보 저장/보관** → **확정**: 주문 생성 시 서버에 배송정보 보관, **결제 성공(승인 DONE) 시 `deliveries` INSERT**. 남은 세부 결정은 "주문 생성~승인 사이 배송정보를 어디에 담을지"(전용 임시 저장 vs `deliveries` 선(先)생성 후 상태 전환) — 구현 시 스키마 최소 변경 원칙으로 확정.
- ~~(b) 결제위젯 렌더 방식~~ → **확정: 결제창형(팝업)**. §2-[2]~[3] 참조.
- **(h) 재고 차감** → **확정**: 결제 성공 시 `order_items` 수량만큼 `books.stocks` 차감, 차감 전 `stocks >= qty` 검증 + 부족 시 결제 취소(자동 환불). §2-[5] (4-3) 참조.
- (c) **`customerKey` 소스** → **확정**: 우선 로그인 사용자 id(`useAuthStore.user.userId`)를 그대로 사용. (토스는 유추 가능 값 회피를 권고하므로, 추후 임의 키 발급·저장 방식으로 개선 여지 남김.)
- (d) **결제 완료 후 장바구니 처리** → **확정: 유지**. 주문 항목을 `cart_items`에서 삭제하지 않는다. §2-[5] (4-5) 참조.
- (e) **토스 키 세팅** → **확정·완료**:
  - 프론트: `shop-front/.env`의 `VITE_TOSS_CLIENT_KEY`(공개 테스트 클라이언트 키). `.env.example` 동봉, `.env`는 `.gitignore` 처리.
  - 백엔드: `application.yml`의 `toss.secret-key`(환경변수 `TOSS_SECRET_KEY` 주입, 미지정 시 공개 테스트 시크릿 키). `toss.api-base-url`도 함께 정의.
  - 운영 전환 시 각각 라이브 키로 교체.
- (f) **SDK 도입 방식**: npm 패키지(`@tosspayments/tosspayments-sdk`) 설치.
- (g) **주문 결과 조회 API** → **확정: 이번 구현 대상**. 성공 화면(`docs/resources/order-compleate.png`)을 위해 `GET /api/orders/{orderId}` 구현. 스펙은 §3-1 참조.
- (i) **성공/실패 화면 구성** → **확정**:
  - 성공: `order-compleate.png`와 동일 구성(§[4-A]). 주문번호=`orders.id`(UUID), 결제수단=`payments.method` 실제값, "주문 내역 확인" 버튼은 비활성/준비중.
  - 실패: 상세 미표시, "결제에 실패했습니다"만 + "홈으로 돌아가기" 버튼만(§[4-B]).

---

## 6. 구현 순서 (제안)

1. (결정) §5 항목 확정 + 토스 테스트 키 준비 — **완료**(키 세팅 §5-(e))
2. BE: `POST /api/orders` (주문 생성) + 엔티티(`Order`/`OrderItem`/`Payment`/`Delivery`)·리포지토리
3. BE: `POST /api/payments/confirm` (승인·검증·재고차감·배송저장·DONE 확정)
4. BE: `GET /api/orders/{orderId}` (주문 결과 조회 — 성공 화면용, §3-1)
5. FE: SDK 설치(`@tosspayments/tosspayments-sdk`) + `Order.jsx` `handlePayment()` → 주문 생성 + 결제창 연결
6. FE: `/payments/result` 단일 페이지 + 라우트 등록
   - 성공 상태: `order-compleate.png` 동일 구성(§[4-A]), 결과 조회 API 연동
   - 실패 상태: 최소 구성(§[4-B])
7. (보강) BE: 웹훅 엔드포인트
8. 테스트 결제로 성공/실패/금액불일치/재고부족 시나리오 검증
