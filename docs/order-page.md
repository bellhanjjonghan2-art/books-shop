# 주문하기(Order) 페이지 PRD

# 1. 목적
- Pencil `book-shop03.pen`의 `주문/배송목록`(id `aagfi`, 화면 타이틀 "주문하기") 프레임 기준으로 주문서 작성 화면 구현.
- 진입 3곳: 메뉴별 도서리스트 바로구매 / 도서 상세 바로구매 / 장바구니 결제.
- 이번 범위: **주문서 작성 화면 + 도서 조회 API**. 결제 승인/주문 생성 연동은 범위 밖(8장).

# 2. 디자인 대비 변경 사항
디자인 예시 텍스트(홍길동, 클린 코드 등)는 무시하고 실제 데이터로 바인딩. 아래만 다르게 구현:

| 항목 | 변경 내용 |
| --- | --- |
| 쿠폰 할인 / 포인트 사용 행 | **제거** |
| 배송 메모 (`Ioouz`) | 드롭다운 → **자유 텍스트(textarea)** |
| 우편번호(`Xq1nK`) / 주소(`S0qFVM`) | **readonly**, 다음 주소 검색 결과로만 채움 (직접 타이핑 불가) |
| 상세주소(`WdMSz`) | 그대로 유지, 수동 입력 |
| 수량 표시 | 정적 텍스트 유지, 이 화면에서 변경 UI 없음 (변경은 장바구니에서) |

# 3. 진입 라우팅 (3곳)
신규 라우트 `path: 'order'`(`/order`)를 [router.jsx](../shop-front/src/router.jsx)에 `MainLayout` 자식으로 추가. bookId 배열은 URL이 아닌 `navigate(path, { state })`로 전달:

```js
navigate('/order', { state: { items: [{ bookId, quantity }, ...] } })
```

| 진입 위치 | 파일 | quantity |
| --- | --- | --- |
| 도서리스트 바로구매 | [BookListPage.jsx:198](../shop-front/src/components/BookListPage/BookListPage.jsx#L198) `handleAuthAction` | 무조건 1 |
| 도서 상세 바로구매 | [BookDetailPage.jsx:96](../shop-front/src/pages/BookDetail/BookDetailPage.jsx#L96) `handleBuy` | 무조건 1 |
| 장바구니 결제 | [CartPage.jsx:208](../shop-front/src/pages/Cart/CartPage.jsx#L208) "선택 상품 구매하기" | 선택 항목 각각의 장바구니 수량 그대로 (여러 건 가능) |

- 세 곳 모두 현재는 no-op placeholder(로그인 체크만) → 로그인 상태면 위 `navigate` 호출로 교체.
- 비로그인 시 동작은 기존과 동일(변경 없음). 장바구니는 선택 0건이면 클릭 방어.
- `/order` 새로고침·직접 접근 등 `location.state` 없는 경우: 안내 후 `/cart`로 리다이렉트.

# 4. 화면 구성
`Page Header`(홈>장바구니>주문하기) → `Page Title`("주문하기") → `Body Row`(왼쪽: 배송지+주문상품 / 오른쪽: 결제금액).

# 5. 배송지 정보 카드
| 필드 | 타입 | 비고 |
| --- | --- | --- |
| 수령인 / 연락처 / 상세주소 | text input | 직접 입력 |
| 우편번호 / 주소 | text input, readonly | 다음 주소 검색 결과로만 채움 |
| 배송 메모 | textarea | 직접 입력, 선택 |

**다음(Daum) 주소 검색**: "우편번호 검색" 버튼(`Dywwg`) 클릭 시 공식 임베드 스크립트를 동적 로드(`//t1.daumcdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js`, 별도 npm 패키지 미사용)해서 팝업 오픈:

```js
new window.daum.Postcode({
  oncomplete: (data) => { setZonecode(data.zonecode); setAddress(data.roadAddress) },
}).open()
```
우편번호/주소는 이 콜백으로만 채워지고 입력창은 readOnly. 상세주소만 수동 입력.

# 6. 주문 상품 목록 + 도서 조회 API
`location.state.items`의 `bookId`로 도서 정보를 조회해 카드로 표시. 수량/금액은 서버 응답이 아니라 `items`의 `quantity`로 계산.

**API**: `GET /api/books?bookIds=id1,id2,id3` (콤마 구분, 장바구니 삭제 API와 동일 컨벤션). 기존 `GET /api/books`(메인페이지)와는 `@GetMapping(params = "bookIds")`로 분리.

```json
{ "code": 200, "message": "OK",
  "data": [{ "bookId": "IBK-abc123", "title": "클린 코드", "author": "로버트 C. 마틴",
             "coverImage": "https://...", "categoryName": "IT", "salePrice": 22000 }] }
```
- 순서 보장 불필요(프론트에서 `items` 순서대로 매핑). 존재하지 않는 bookId는 실패 처리하지 않고 결과에서 제외.
- 응답/에러 포맷은 `backend-api-response` Skill 그대로.
- 카드 표시: 표지, 카테고리 뱃지, 제목, 저자, 수량("N권"), 금액(`salePrice*quantity`), 하단 요약("총 N개 상품"/합계).

# 7. 결제 금액 영역 (Right Column)
- 상품 금액 합계 + 배송비(장바구니 페이지와 동일하게 무료 고정) = 총 결제금액. 쿠폰/포인트 행 없음.
- 동의 체크박스 필수, "OO원 결제하기" 버튼은 배송지 필수값+동의 체크 완료 시에만 활성화 (클릭 동작은 8장 참조, no-op).
- Safety Row(SSL/환불보장/고객지원 뱃지)는 디자인 그대로 정적 표시.

# 8. 이번 범위가 아닌 것
- "결제하기" 클릭 시 주문 생성 API 호출, 토스페이먼츠 SDK 연동, 결제 승인/취소 — 폼 유효성 검증까지만 동작, 클릭은 no-op placeholder.
- `orders`/`payments`/`deliveries` 테이블 INSERT 로직 (테이블 정의는 `scripts/create_table.sql`에 이미 존재).
- 배송비 정책 확정 (현재는 무료 고정 가정).

# 9. 확인 필요
- 배송비 무료 고정 유지 여부 (정책 도입 시 별도 확인).
- 라우트 경로명 `order` vs `checkout`.
