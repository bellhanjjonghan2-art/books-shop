# 주문/배송조회(Order List) 페이지 PRD + API 설계 (초안)

> `docs/resources/orderList.png` 디자인을 기준으로 한다. 이 문서는 화면 설계 + API 설계까지만
> 다루고, 실제 코드 구현은 이 문서 확정 후 별도 단계에서 진행한다 (`docs/order-page.md`,
> `docs/payment-process.md`와 동일한 절차).

---

## 1. 목적

헤더 사용자 드롭다운의 "주문/배송조회" 메뉴([UserDropdown.jsx](../shop-front/src/components/UserDropdown/UserDropdown.jsx))와
결제 완료 화면의 "주문 내역 확인" 버튼([PaymentResultPage.jsx:230-232](../shop-front/src/pages/PaymentResult/PaymentResultPage.jsx#L230-L232),
현재 `disabled`)이 이동할 실제 페이지를 만든다. 로그인한 사용자의 주문 목록을 기간별로 조회하는
화면 + 데이터 API를 설계한다.

## 2. 디자인 대비 변경 사항

디자인의 예시 텍스트(`ORD-2024-001`, 홍길동님 등)는 무시하고 실제 데이터로 바인딩한다
(`docs/order-page.md` 2절과 동일 원칙). 구체적으로:

| 항목 | 디자인 | 실제 구현 |
| --- | --- | --- |
| 주문번호 표기 | `ORD-2024-001` 형식 | `orders.id`(UUID) 그대로 표시 — 결제완료 화면(`payment-process.md` §2-[4-A])에서 이미 UUID를 그대로 쓰기로 확정했으므로 동일하게 맞춘다 |
| 상품 썸네일 | 색상 블록 | `books.cover_image` 있으면 이미지, 없거나 로드 실패 시 색상 블록 fallback (`OrderPage.jsx`의 `BookCover` 컴포넌트와 동일 패턴 재사용) |

나머지(카드 레이아웃, 상태 뱃지, 기간 필터, 페이지네이션)는 디자인 그대로 따른다.

## 3. 진입 라우팅 (2곳)

새 라우트 `path: 'orders'`(`/orders`)를 [router.jsx](../shop-front/src/router.jsx)에 `MainLayout`
자식으로 추가한다. 기존 `/order`(주문서 작성, 단수)와 겹치지 않는 별도 경로다.

| 진입 위치 | 파일 | 현재 상태 |
| --- | --- | --- |
| 헤더 사용자 드롭다운 "주문/배송조회" | [UserDropdown.jsx:26-29](../shop-front/src/components/UserDropdown/UserDropdown.jsx#L26-L29) `handleProfile` | 현재 `/profile`로 이동(빈 라우트, 404) → `/orders`로 교체 |
| 결제 완료 화면 "주문 내역 확인" 버튼 | [PaymentResultPage.jsx:230-232](../shop-front/src/pages/PaymentResult/PaymentResultPage.jsx#L230-L232) | 현재 `disabled` 정적 버튼 → `/orders`로 이동하는 링크로 활성화 |

이 두 곳의 라우팅 연결이 이번 작업의 유일한 "기존 화면 수정" 범위다. 그 외 화면(장바구니, 주문서 등)은
변경하지 않는다.

## 4. 화면 구성

`Page Header`(홈 > 주문/배송 목록) → `Page Title`("주문/배송 목록" + "최근 주문 내역을 확인하세요")
→ 우측 상단 `기간 필터`(1개월/3개월/6개월/전체, 기본값 1개월) → `주문 카드 리스트` → `페이지네이션`.

### 4.1 주문 카드 (주문 1건 = 카드 1개)

| 영역 | 내용 | 데이터 소스 |
| --- | --- | --- |
| 헤더 행 | 주문번호 \| 주문일자 ........ 배송상태 뱃지 | `orders.id`, `orders.created_at`, `deliveries.status` |
| 상품 행(N개) | 표지 · 도서명 · "수량: N권" ........ 금액 | `order_items`(title, quantity, amount 스냅샷) + `books.cover_image`(join) |
| 푸터 행 | "합계" ........ 주문 총액 | `orders.total_amount` |

### 4.2 배송상태 뱃지 매핑

`deliveries.status`(`scripts/create_table.sql` 기존 enum) 그대로 사용, 4번째 값은 디자인에는
없지만 이번 결제 기능(재고부족 자동환불)에서 이미 발생 가능한 상태라 함께 정의한다.

| `deliveries.status` | 뱃지 라벨 | 색상(디자인 기준) |
| --- | --- | --- |
| PREPARING | 배송준비중 | 노랑/amber |
| SHIPPING | 배송중 | 파랑 |
| DELIVERED | 배송완료 | 초록 |
| CANCELED | 주문취소 | 회색(디자인에 없어 임의 제안 — 확인 필요) |

### 4.3 기간 필터 → 페이지네이션 초기화

필터 변경 시 1페이지로 리셋(`BookListPage.jsx`의 정렬 변경 시 동작과 동일 원칙).

## 5. API 설계 — `GET /api/orders`

목록 조회. 기존 `POST /api/orders`(주문 생성), `GET /api/orders/{orderId}`(단건 조회, 결제완료
화면용)와 세그먼트 구조가 달라 패턴이 겹치지 않는다(`api-contract.md`의 "세그먼트 개수가 다르면
패턴 충돌 없음" 판단 방식과 동일).

인증 필요(`authenticated()`, 별도 SecurityConfig 변경 불필요 — `/api/orders`가 이미 permitAll
목록에 없어 기본적으로 인증 대상). 본인 주문만 반환(`orders.user_id` = 토큰의 userId).

### 5.1 Query Parameters

| 이름 | 타입 | 필수 | 기본값 | 값 | 비고 |
| --- | --- | --- | --- | --- | --- |
| period | string | N | `1m` | `1m` \| `3m` \| `6m` \| `all` | `orders.created_at >= now - N개월` 필터. `all`은 필터 없음 |
| page | int | N | `0` | 0 이상 | 0-based |
| size | int | N | `5` | 1 이상 | 디자인 밀도 기준 제안값 — **확인 필요**(카테고리 목록은 10 사용 중) |

### 5.2 상태 필터링 (고정, 파라미터 아님)

`orders.status IN ('PAID', 'PARTIAL_CANCELED', 'CANCELED')`만 반환한다. `PENDING`(결제 시도 전
이탈)과 `FAILED`(승인 실패)는 실제로 돈이 오가지 않은 미완료 주문이라 "주문 내역"에 노출하지
않는다. — **확인 필요**(단, PENDING·FAILED를 제외하는 게 맞는지 최종 확인).

### 5.3 정렬

`orders.created_at DESC` 고정(최신 주문이 먼저, 디자인 예시 순서와 일치).

### 5.4 Response 200

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "orderId": "b3f1a2c4-1234-4a11-9c8e-000000000001",
        "orderDate": "2024-06-15",
        "deliveryStatus": "DELIVERED",
        "items": [
          {
            "bookId": "IBK-abc123",
            "title": "혼자 공부하는 파이썬",
            "author": "윤인성",
            "publisher": "한빛미디어",
            "coverImage": "https://...",
            "quantity": 1,
            "amount": 23400
          }
        ],
        "totalAmount": 57600
      }
    ],
    "page": 0,
    "size": 5,
    "totalElements": 18,
    "totalPages": 4
  }
}
```

`content[]` 항목의 `items[]`는 기존 `GET /api/orders/{orderId}`가 쓰는 `OrderItemResultDto`
(`bookId`/`title`/`author`/`publisher`/`coverImage`/`quantity`/`amount`)를 그대로 재사용한다
(신규 DTO 만들지 않음 — `author`/`publisher`는 이 화면에서 안 쓰지만 재사용 편의상 그대로 둔다).

`content[]` 상위 필드:

| 필드 | 타입 | nullable | 비고 |
| --- | --- | --- | --- |
| orderId | string | N | `Order.id`(UUID) |
| orderDate | string(`YYYY-MM-DD`) | N | `Order.createAt`을 날짜만 추출. 화면 표시(`YYYY.MM.DD`)는 프론트가 포맷 |
| deliveryStatus | string | N | `Delivery.status`(4.2절 enum 값 그대로) |
| items | array | N | 최소 1개(빈 주문 없음) |
| totalAmount | int | N | `Order.totalAmount` |

`page`/`size`/`totalElements`/`totalPages`는 `BookListResponse`(카테고리별 도서 리스트)와
동일한 페이지네이션 포맷 — 프론트에서 `BookListPage.jsx`의 기존 페이지네이션 UI(처음/이전/
숫자±2/다음/마지막)를 그대로 재사용할 수 있다.

### 5.5 빈 목록

주문이 0건이면 `content: []`, `totalElements: 0`, `totalPages: 0` (카테고리 목록과 동일 규칙).
빈 상태 UI(디자인에 없음) — **확인 필요**.

### 5.6 에러

없음(인증 실패는 공통 401만 발생, 별도 도메인 에러코드 불필요 — path variable이 없어
`ORDER_NOT_FOUND` 같은 케이스 자체가 없음).

## 6. 구현 지점 (참고용, 다음 단계)

### 백엔드
- `OrderController`에 `GET`(경로 없음, `/api/orders`) 매핑 추가.
- `OrderService`에 목록 조회 메서드 추가: `orders` + `deliveries` 조인, `period` → 날짜 범위 변환,
  페이지네이션(Spring Data `Pageable` 또는 기존 카테고리 목록과 동일한 수동 page/size 처리 방식
  중 택 1 — 기존 코드 컨벤션 확인 후 결정).
- `order_items`는 기존 `OrderItemResultDto` 프로젝션 쿼리(`OrderRepository`/`OrderItemRepository`에
  이미 있는 패턴) 재사용.

### 프론트
- `src/api/orders.js`에 `fetchOrders({ period, page, size })` 추가.
- `hooks/queries/useOrders.js`에 `useOrders(...)` 쿼리 훅 추가.
- 신규 페이지 `src/pages/OrderList/OrderListPage.jsx` + `.module.css`.
- `router.jsx`에 `/orders` 라우트 추가.
- `UserDropdown.jsx`의 `handleProfile` → `/orders`로 경로 변경(함수명은 `handleOrderList` 등으로
  같이 정리 권장).
- `PaymentResultPage.jsx`의 "주문 내역 확인" 버튼 `disabled` 제거, `Link to="/orders"`로 교체.

## 7. 이번 범위가 아닌 것

- 주문 카드 클릭 시 상세 페이지 이동(디자인에 없음, 클릭 인터랙션 없음).
- 배송 조회(택배사 추적) 연동 — `deliveries.tracking_number`/`courier_company` 표시는 이번
  범위 밖(디자인에도 없음).
- 주문 취소/환불 요청 액션 — 조회 전용.

## 8. 확인 필요

1. `size` 기본값 5 vs 10(5.1절).
2. `PENDING`/`FAILED` 주문 제외 여부(5.2절).
3. `CANCELED` 배송상태 뱃지 색상/라벨(4.2절, 디자인에 없는 상태).
4. 빈 목록 상태 UI(5.5절).
