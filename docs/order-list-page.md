# 주문/배송 조회 페이지 설계

작성: 결제 기능 후속 — 주문/배송 목록
참고 디자인: `docs/resources/orderList.png`
관련 문서: `docs/payment-process.md`(결제·주문 생성), `docs/specs/api-contract.md`(API 계약)

> 이 문서는 주문/배송 조회 페이지와 그에 필요한 API의 설계다.
> 스키마는 `scripts/create_table.sql`의 `orders`/`order_items`/`books`/`deliveries`를 그대로 사용한다(물리 DB 작업 없음).

---

## 0. 확정 사항

| 항목 | 결정 |
|---|---|
| 조회 범위 | **결제완료(PAID) 이후 주문만** 표시 (PENDING·FAILED 제외). 배송 상태가 진행된 주문 포함 |
| 기간 필터 | 1개월 / 3개월 / 6개월 / 전체. **기본 1개월** |
| 페이지네이션 | 서버 페이징. 디자인의 « ‹ 1 2 3 4 › » |
| 주문번호 표기 | `orders.id`(UUID) 그대로 (결제 결과 페이지와 동일) |
| 진입 경로 | ① 사용자 드롭다운 "주문/배송조회" ② 결제 완료 화면 "주문 내역 확인" 버튼 |
| 페이지 경로 | `/orders` |
| 인증 | JWT 필요. **본인 주문만** 조회 |

---

## 1. 화면 구성 (orderList.png)

| 영역 | 구성 | 데이터 소스 |
|---|---|---|
| ① 브레드크럼 | 홈 > 주문/배송 목록 | — |
| ② 타이틀 | "주문/배송 목록" + "최근 주문 내역을 확인하세요" + **기간 필터 버튼(1/3/6개월·전체)** | — |
| ③ 주문 카드(주문 1건) | 헤더: 주문번호 \| 주문일 ··· **배송상태 뱃지** / 본문: 상품행(표지·제목·수량·금액) / 푸터: **합계** | 아래 매핑 |
| ④ 페이지네이션 | « ‹ 1 2 3 4 › » | 서버 페이징 메타 |

### 주문 카드 데이터 매핑

| 화면 요소 | 소스 |
|---|---|
| 주문번호 | `orders.id` (UUID 그대로) |
| 주문일 | `orders.created_at` (`YYYY.MM.DD`) |
| 배송상태 뱃지 | `deliveries.status` → 라벨/색 매핑(§2) |
| 상품 표지 | `books.cover_image` (JOIN, `order_items.book_id`) |
| 상품 제목 | `order_items.title` (스냅샷) |
| 상품 수량 | `order_items.quantity` (`수량: N권`) |
| 상품 금액 | `order_items.amount` |
| 합계 | `orders.total_amount` |

---

## 2. 배송 상태 뱃지 매핑

`deliveries.status` 값 → 화면 라벨/색상:

| status | 라벨 | 색상(디자인) |
|---|---|---|
| `PREPARING` | 배송준비중 | 노랑 |
| `SHIPPING` | 배송중 | 파랑 |
| `DELIVERED` | 배송완료 | 초록 |
| `CANCELED` | 취소 | 회색 |

> `PAID` 직후 배송은 `PREPARING`("배송준비중")으로 시작한다(결제 결과 페이지의 "결제 확인 중"과 동일 status지만, 목록에서는 디자인 문구 "배송준비중"으로 표기). 문구 매핑은 프론트가 처리한다.

---

## 3. 신규 API — `GET /api/orders`

주문/배송 목록 조회. 기존 `ApiResponse`/`ErrorResponse` 규약, 페이징 메타 패턴(`reviewList`/카테고리 목록과 동일)을 따른다.

### 3-1. 엔드포인트

| 항목 | 값 |
|---|---|
| Method | `GET` |
| URL | `/api/orders` |
| 인증 | JWT 필요 (`Authorization: Bearer {accessToken}`) |
| Query | `period`(기간, 기본 `1m`), `page`(0부터, 기본 0), `size`(기본 5) |

`period` 값: `1m`(1개월) / `3m`(3개월) / `6m`(6개월) / `all`(전체). 기준: `orders.created_at >= now - period`.

> 기존 `GET /api/orders/{orderId}`(주문 결과 단건 조회, payment-process.md §3-1)와 경로가 구분된다. 목록은 `/api/orders`, 단건은 `/api/orders/{orderId}`.

### 3-2. 성공 응답 (`200 OK`)

```json
{
  "success": true,
  "data": {
    "totalCount": 23,
    "page": 0,
    "size": 5,
    "totalPages": 5,
    "items": [
      {
        "orderId": "b1f2...uuid",
        "orderDate": "2024-06-15",
        "deliveryStatus": "DELIVERED",
        "totalAmount": 57600,
        "items": [
          { "bookId": "IT-IT-6FB622", "title": "혼자 공부하는 파이썬",
            "coverImage": "https://...", "quantity": 1, "amount": 23400 },
          { "bookId": "IT-IT-1A2B3C", "title": "클린 코드",
            "coverImage": "https://...", "quantity": 1, "amount": 34200 }
        ]
      }
    ]
  }
}
```

#### data 필드

| 필드 | 타입 | 소스 | 설명 |
|---|---|---|---|
| `totalCount` | Long | `orders` 집계 | 조건(본인·PAID·기간) 전체 주문 수 |
| `page` / `size` / `totalPages` | Integer | 페이징 메타 | |
| `items[]` | Array | 주문별 | 최신 주문일 내림차순 정렬 |

#### items[] (주문 1건)

| 필드 | 타입 | 소스 |
|---|---|---|
| `orderId` | String | `orders.id`(UUID) |
| `orderDate` | String | `orders.created_at` (ISO `YYYY-MM-DD`) |
| `deliveryStatus` | String | `deliveries.status` (배송 행 없으면 `PREPARING` 기본) |
| `totalAmount` | Integer | `orders.total_amount` |
| `items[]` | Array | 주문 상품 목록(아래) |

#### items[].items[] (주문 상품)

| 필드 | 타입 | 소스 |
|---|---|---|
| `bookId` | String | `order_items.book_id` |
| `title` | String | `order_items.title` (스냅샷) |
| `coverImage` | String | `books.cover_image` (JOIN) |
| `quantity` | Integer | `order_items.quantity` |
| `amount` | Integer | `order_items.amount` |

### 3-3. 처리 규칙

- 대상: `orders.user_id == 토큰 userId` **AND** `orders.status = 'PAID'` **AND** 기간 조건.
- 정렬: `orders.created_at` DESC (최신순).
- 빈 목록: 오류 아님. `totalCount:0`, `totalPages:0`, `items:[]`.
- N+1 주의: 주문별 상품·표지 조회는 배치(fetch join 또는 in-절)로 처리(backend-jpa-persistence 규칙).

### 3-4. 에러 응답

| 상황 | HTTP | code |
|---|---|---|
| 잘못된 `period` 값 | 400 | `INVALID_INPUT` 재사용 금지 → `INVALID_ORDER_PERIOD` 신설 |
| 인증 실패 | 401 | (기존 시큐리티 처리) |
| 서버 오류 | 500 | `INTERNAL_ERROR` |

---

## 4. 프론트 구현 범위 (shop-front/)

| 파일 | 내용 |
|---|---|
| `api/orders.js` | `fetchOrders({ period, page, size })` 추가 (기존 `createOrder`/`fetchOrderResult` 옆에) |
| `hooks/queries/useOrders.js` | `useOrderList({ period, page })` (useQuery) 추가 |
| `pages/OrderList/OrderList.jsx` + `.module.css` | 목록 화면(orderList.png 재현): 기간 필터, 주문 카드, 페이지네이션 |
| `router.jsx` | `/orders` → `OrderListPage`를 MainLayout 자식으로 등록 |

### 진입점 연결

- **드롭다운**: `UserMenu.jsx`의 "주문/배송조회"가 이미 `/orders`로 navigate (구현됨). 라우트만 추가되면 동작.
- **결제 완료 화면**: `PaymentResult.jsx`의 "주문 내역 확인" 버튼(현재 `disabled`)을 활성화 → `/orders`로 이동.

---

## 5. 구현 순서 (제안)

1. BE: `GET /api/orders` (목록 조회) — 리포지토리 쿼리(본인·PAID·기간·페이징) + 서비스 + 컨트롤러 + `INVALID_ORDER_PERIOD` 에러코드 + `api-contract.md` 갱신
2. FE: `api/orders.js` `fetchOrders` + `useOrders.js` `useOrderList`
3. FE: `OrderList` 페이지(기간 필터·카드·페이지네이션) + `/orders` 라우트
4. FE: `PaymentResult.jsx` "주문 내역 확인" 버튼 활성화 → `/orders`
5. (검증) 결제 완료 → 목록 표시 → 기간 필터 → 페이지 이동
