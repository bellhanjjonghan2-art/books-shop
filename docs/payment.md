# 결제(토스페이먼츠) 연동 프로세스 (초안)

> `docs/order-page.md` 8장에서 범위 밖으로 명시했던 "결제하기 클릭 시 주문 생성 API 호출,
> 토스페이먼츠 SDK 연동, 결제 승인" 부분을 다루는 다음 단계 문서다. DB 스키마(`orders`,
> `order_items`, `payments`, `payment_cancels`, `deliveries`)는 `scripts/create_table.sql`에
> 이미 정의되어 있다. 이 문서는 토스페이먼츠 MCP(`tosspayments-integration-guide`) v2 가이드를
> 기준으로 이 프로젝트에 맞춘 연동 프로세스 **초안**이며, 실제 코드는 아직 작성하지 않았다.
> "확인 필요" 항목은 구현 전 반드시 확인받는다 (CLAUDE.md "추측해서 구현하지 않는다").

---

## 1. 목적

`OrderPage.jsx`의 "OO원 결제하기" 버튼([OrderPage.jsx:260-267](../shop-front/src/pages/Order/OrderPage.jsx#L260-L267))을
placeholder(`console.log`)에서 실제 토스페이먼츠 결제로 교체한다. 범위:

- 주문 생성 API (`orders`/`order_items` INSERT)
- 토스페이먼츠 결제위젯 SDK 연동 (클라이언트)
- 결제 승인 API 프록시 (`payments` INSERT, `orders` 상태 갱신, `deliveries` 생성)
- 성공/실패 리다이렉트 화면

범위 밖(별도 문서/단계): 결제 취소·부분취소(`payment_cancels`), 웹훅, 가상계좌 등 비동기
결제수단, 마이페이지 주문내역 조회. 9장에 정리.

---

## 2. 연동 방식 선택 — 결제위젯(결제창형)

토스 MCP 정책상 신규 연동은 결제위젯을 우선 권장하며, 그 안에서도 두 방식이 있다.

| 방식 | 설명 | 이 프로젝트 적합성 |
| --- | --- | --- |
| **결제창형** (`renderPaymentWindow`) | 결제수단 선택 UI를 팝업/오버레이로 띄움. 주문서 페이지에는 "결제하기" 버튼만 있으면 됨 | `OrderPage.jsx`가 이미 자체 배송지/주문상품 UI를 완성했고, 결제수단 선택 영역(`div#payment-method`, `div#agreement`)이 없다 → **현재 구조에 그대로 얹을 수 있음** |
| 주문서형 (`renderPaymentMethods` + `renderAgreement`) | 결제수단·약관 UI를 페이지 안에 직접 렌더링 | `OrderPage.jsx`에 위젯 마운트용 컨테이너 2개를 새로 추가해야 함 (레이아웃 변경 필요) |

**제안: 결제창형 채택.** `OrderPage.jsx`의 기존 동의 체크박스 + 결제하기 버튼 구조를 그대로
유지하면서, 버튼 클릭 시 결제창(팝업)만 띄우면 되기 때문이다.

> **확인 필요:** 결제창형(제안) vs 주문서형(UI 변경 필요) 채택 여부.

---

## 3. 전체 흐름

```
[OrderPage] 결제하기 클릭
   │
   ├─ 1. POST /api/orders  (주문 생성 — 서버가 orderId/총액 확정)
   │       └─ orders(PENDING) + order_items INSERT
   │
   ├─ 2. widgets.setAmount() → renderPaymentWindow() → paymentRequest 이벤트
   │       └─ widgets.requestPayment({ orderId, orderName, successUrl, failUrl, ... })
   │
   ├─ 3. 구매자 인증(토스 결제창) 완료
   │       ├─ 성공 → /payment/success?paymentKey&orderId&amount&paymentType
   │       └─ 실패 → /payment/fail?code&message&orderId
   │
   └─ 4. [PaymentSuccessPage] amount 검증 후 POST /api/payments/confirm
           └─ 서버: 저장된 orders.total_amount와 재검증 → 토스 결제 승인 API 호출
               ├─ 성공 → payments(DONE) INSERT, orders.status=PAID, deliveries INSERT
               └─ 실패 → payments(실패기록) INSERT, orders.status=FAILED, 에러 응답
```

핵심 원칙(토스 가이드 공통 규칙):
- **금액은 서버가 최초 주문 생성 시 확정한 값을 신뢰**하고, 리다이렉트 쿼리의 `amount`나
  승인 요청 시점의 `amount`가 이 값과 다르면 승인 API를 호출하지 않고 거부한다.
- `paymentKey`/`orderId`/`amount`는 서버(`payments` 테이블)에 반드시 저장한다.
- 결제 승인은 요청 완료 후 10분 이내에 호출해야 한다(`NOT_FOUND_PAYMENT_SESSION` 방지).

---

## 4. API 설계 (초안)

### 4.1 `POST /api/orders` — 주문 생성

**요청**

```json
{
  "items": [{ "bookId": "IBK-abc123", "quantity": 2 }],
  "delivery": {
    "recipientName": "홍길동",
    "recipientPhone": "010-0000-0000",
    "postCode": "12345",
    "address": "서울시 ...",
    "addressDetail": "101동 202호",
    "memo": "문 앞에 놔주세요"
  }
}
```

**서버 처리**
- 인증 필요(`authenticated()`) — `orders.user_id`는 JWT의 `sub`(userId) 사용.
- `items`의 `bookId`로 `books.sale_price`를 **서버에서 다시 조회**해 `order_items.unit_price`,
  `amount`, `orders.total_amount`를 계산한다(클라이언트 금액 미신뢰, `docs/order-page.md` 6절
  방식과 동일하게 `BookOrderItemDto` 조회 로직 재사용 가능).
- `orders.id`(UUID) = 토스 `orderId`로 그대로 사용. `orders.order_name`은 `제목 외 N건` 형식.
- `orders.status = 'PENDING'`으로 INSERT, `order_items` 함께 INSERT.

**응답**

```json
{ "success": true, "data": { "orderId": "uuid...", "orderName": "클린 코드 외 1건", "amount": 44000 } }
```

### 4.2 `POST /api/payments/confirm` — 결제 승인

**요청** (성공 리다이렉트 쿼리 파라미터 그대로 전달)

```json
{ "paymentKey": "...", "orderId": "uuid...", "amount": 44000 }
```

**서버 처리**
1. `orders`에서 `orderId` 조회. 없으면 `404 ORDER_NOT_FOUND`.
2. `order.status != PENDING`이면 `409 ORDER_ALREADY_PROCESSED` (재승인/중복 클릭 방지).
3. `order.total_amount != amount`이면 `400 ORDER_AMOUNT_MISMATCH` (승인 API 호출 안 함, 위변조 의심).
4. 토스 결제 승인 API 호출:
   ```
   POST https://api.tosspayments.com/v1/payments/confirm
   Authorization: Basic base64(secretKey + ":")
   Idempotency-Key: <서버 생성 UUID>
   { "paymentKey", "orderId", "amount" }
   ```
5. 성공(200) → `payments` INSERT(status=DONE, 응답 전체를 `raw_response` JSONB에 저장) →
   `orders.status = 'PAID'` → `deliveries` INSERT(요청 시 저장해둔 배송지 스냅샷 사용, status=PREPARING).
6. 실패(4xx/5xx) → `payments` INSERT(status=ABORTED 등, `fail_code`/`fail_message` 기록) →
   `orders.status = 'FAILED'` → 클라이언트에는 `502 PAYMENT_CONFIRM_FAILED` + 토스 원본 메시지 전달.

**응답(성공)**

```json
{ "success": true, "data": { "orderId": "uuid...", "status": "DONE", "approvedAt": "..." } }
```

### 4.3 에러 코드 추가분 (`ErrorCode` enum)

| code | HTTP Status | message |
| --- | --- | --- |
| ORDER_NOT_FOUND | 404 | 주문을 찾을 수 없습니다. |
| ORDER_ALREADY_PROCESSED | 409 | 이미 처리된 주문입니다. |
| ORDER_AMOUNT_MISMATCH | 400 | 결제 금액이 일치하지 않습니다. |
| PAYMENT_CONFIRM_FAILED | 502 | 결제 승인에 실패했습니다. |

---

## 5. 프론트 구현 지점

- **환경변수**: `.env`에 `VITE_TOSS_CLIENT_KEY` 추가 (시크릿 키는 절대 프론트에 두지 않음).
- **패키지**: `@tosspayments/tosspayments-sdk` 설치.
- **`src/api/orders.js`**, **`src/api/payments.js`**: axios 함수 (`front-api-client` 스킬 규약).
- **`hooks/queries/useOrders.js`**(`useCreateOrder`), **`hooks/queries/usePayments.js`**(`useConfirmPayment`):
  TanStack Query `useMutation`.
- **`OrderPage.jsx` `handlePayment`** 교체:
  1. `useCreateOrder().mutateAsync({ items, delivery })` → `{ orderId, orderName, amount }`
  2. `widgets.setAmount({ value: amount, currency: 'KRW' })` → `renderPaymentWindow()` →
     `paymentRequest` 이벤트에서 `widgets.requestPayment({ orderId, orderName, successUrl: origin + '/payment/success', failUrl: origin + '/payment/fail', customerEmail, customerName, customerMobilePhone })`
  3. `customerKey`: 로그인 사용자의 `userId`(authStore) 사용 — 비회원 결제 없음(주문 전체가
     `authenticated()` 전제이므로 `ANONYMOUS` 불필요).
- **신규 페이지**(`front-routing` 스킬, `MainLayout` 자식 라우트로 추가):
  - `/payment/success`: 쿼리 파라미터 `paymentKey`/`orderId`/`amount` 파싱 → 클라이언트에서도
    `amount` 재검증(선택) → `useConfirmPayment` 호출 → 성공 시 완료 화면, 실패 시 에러 메시지 +
    "주문 내역으로" 등 안내.
  - `/payment/fail`: 쿼리 파라미터 `code`/`message`/`orderId` 표시, 승인 API 호출하지 않음,
    재시도 유도(예: `/cart` 또는 원래 주문 화면으로).

---

## 6. 백엔드 구현 지점

- 신규 도메인 패키지 `order`, `payment` (기존 `cart`/`book` 패키지와 동일 레이어 구조:
  `Controller`/`Service`/`Repository`/`Entity`/`dto`, `backend-jpa-persistence` 스킬 규약).
- 시크릿 키: `application.yml`에 `toss.secret-key` 형태로 두되 **환경변수 주입**
  (`jwt.secret`과 동일한 패턴, 코드/커밋에 실제 값 하드코딩 금지).
- 외부 HTTP 호출(토스 승인 API): Spring 6.1+ `RestClient` 또는 `WebClient` 중 선택 필요.

> **확인 필요:** 토스 승인 API 호출에 `RestClient`/`WebClient`/`RestTemplate` 중 무엇을 쓸지
> (신규 의존성 여부와 결부되므로 임의로 정하지 않음).

- `Idempotency-Key`는 서버에서 요청마다 UUID 생성(재시도 시 같은 승인 요청이 중복 처리되지
  않도록). `payments` 테이블의 `uq_payments_order_done`(부분 유니크 인덱스, `status='DONE'`)이
  주문당 승인 성공 1건만 허용하는 안전장치와 함께 동작.

---

## 7. 결제 실패/취소 시 상태 전이

| 시점 | orders.status | payments | deliveries |
| --- | --- | --- | --- |
| 주문 생성 직후 | PENDING | (없음) | (없음) |
| 결제창에서 구매자 취소(`PAY_PROCESS_CANCELED`) | PENDING 유지 | (없음, `failUrl`에 orderId도 안 옴) | (없음) |
| 인증 실패(`failUrl`, orderId 있음) | PENDING → **확인 필요** (FAILED로 바꿀지, 재시도 가능하게 PENDING 유지할지) | (없음) | (없음) |
| 승인 API 실패 | FAILED | 실패 이력 INSERT | (없음) |
| 승인 API 성공 | PAID | DONE INSERT | INSERT(PREPARING) |

> **확인 필요:** 인증 실패(`/payment/fail`)만 겪고 승인 시도 자체를 안 한 주문을 그대로
> `PENDING`(재시도 가능)으로 둘지, `FAILED`로 확정할지. 재시도를 허용하면 같은 `orderId`로
> 다시 결제창을 열 수 있어야 하는데, 이 경우 프론트 재시도 진입점(주문 화면 재방문 시 기존
> `orderId` 재사용 여부)도 함께 정해야 한다.

---

## 8. 이번 범위가 아닌 것

- 결제 취소/부분취소 (`POST /v1/payments/{paymentKey}/cancel`, `payment_cancels` 테이블).
- 웹훅(`PAYMENT_STATUS_CHANGED`) — 신용카드/간편결제(즉시 승인)만 다루는 한 불필요. 가상계좌 등
  비동기 결제수단을 추가하면 그때 별도 연동.
- 마이페이지 주문내역/배송조회 화면.
- 정산·매출전표(`receipt.url`) 노출 화면.

## 9. 확인 필요 (요약)

1. 결제창형(제안) vs 주문서형 — 2장.
2. 토스 승인 API 호출용 HTTP 클라이언트(`RestClient`/`WebClient`) 선택 — 6장.
3. 인증 실패 시 `orders.status` 전이·재시도 정책 — 7장.
4. `customerKey`로 `userId`를 그대로 노출할지, 별도 값으로 매핑할지.
