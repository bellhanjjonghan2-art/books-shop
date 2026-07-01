
# 1. 목적
- 장바구니 화면 API 만들기

# 2. DB 구조
- 테이블은 물리적으로 이미 생성되어 있음
- ID는 item- 접두사를 사용하여 임의의 난수 8자리 만들어서 사용(예) Item12345678
- DB 테이블 구조
```
CREATE TABLE cart_items (
  id         VARCHAR(36) NOT NULL,                  -- PK, Java에서 UUID 문자열 생성
  user_id    VARCHAR(36) NOT NULL,                  -- FK -> users(id), 회원
  book_id    VARCHAR(40) NOT NULL,                  -- FK -> books(id)
  quantity   INTEGER     NOT NULL DEFAULT 1,        -- 담은 수량
  created_at TIMESTAMP   NOT NULL DEFAULT now(),    -- 담은 시각
  updated_at TIMESTAMP   default null,    -- 수량 변경 시각
  CONSTRAINT pk_cart_items PRIMARY KEY (id),
  CONSTRAINT fk_cart_items_user FOREIGN KEY (user_id) REFERENCES users (user_id),
  CONSTRAINT fk_cart_items_book FOREIGN KEY (book_id) REFERENCES books (id),
  CONSTRAINT uq_cart_items_user_book UNIQUE (user_id, book_id),
  CONSTRAINT ck_cart_items_quantity CHECK (quantity > 0)
);
```

# 3. 필요 기능
- 장바구니 list
- 수량 변경
- 아이템 삭제

## 3.1 API 기능 설명
- API 설명은 @docs/cartItem.md 에 있으니 참조
- 삭제시 id값이 콤마로 이어붙어서 넘어오는데 잘라서 사용( eX)1,2,3)
- 장바구니 관련 파일은 Cart- 접두사 붙여서 만들 것
- userId 는 현재 로그인한 사용자 ID를 JWT 토큰에서 추출하여 사용할 것
- 불확실한 것은 물어볼 것

## 3.2 추가 기능
- 장바구니 등록 API도 필요
- API 설명
  - POST 방식
  - 매개변수: {"bookId": "책ID", "quantity" : "1"
  - 수량은 1 로 고정
  - end-point : /api/carts
  - userId 는 현재 로그인한 사용자 Id를 입력
  - 만약 동일한 bookId 가 존재하면 수량만 1 증가하여 업데이트
  - 동일 bookId 가 없다면 insert
  - 
