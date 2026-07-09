-- ============================================
-- books-shop 데이터베이스 스키마
-- DBMS : PostgreSQL
-- ============================================

-- ============================================
-- categories : 카테고리(메뉴) 테이블
-- types는 IT(IT), NOVEL(소설), SELF(자기계발)
-- ============================================
CREATE TABLE categories (
                            id            VARCHAR(36)  NOT NULL,                  -- PK, Java에서 UUID 문자열 생성
                            name          VARCHAR(100) NOT NULL,                  -- 카테고리명
                            description   VARCHAR(500),                           -- 설명
                            display_order INTEGER      NOT NULL DEFAULT 0,         -- 메뉴 노출 순서
                            types         VARCHAR(30)  DEFAULT '',
                            use_yn        CHAR(1)      NOT NULL DEFAULT 'Y',       -- 활성 여부
                            created_at    TIMESTAMP    NOT NULL DEFAULT now(),     -- 생성일
                            updated_at    TIMESTAMP    NOT NULL DEFAULT now(),     -- 수정일
                            CONSTRAINT pk_categories PRIMARY KEY (id),
                            CONSTRAINT uq_categories_name UNIQUE (name),
                            CONSTRAINT ck_categories_use_yn CHECK (use_yn IN ('Y', 'N'))
);

-- ============================================
-- books : 도서 테이블
-- ============================================
CREATE TABLE books (
                       id             VARCHAR(40)  NOT NULL,                  -- PK, 'IBK-' + UUID
                       category_id    VARCHAR(36)  NOT NULL,                  -- FK -> categories(id)
                       title          VARCHAR(200) NOT NULL,                  -- 제목
                       subtitle       VARCHAR(200),                           -- 부제목
                       author         VARCHAR(100),                           -- 저자
                       publisher      VARCHAR(100),                           -- 출판사
                       description    TEXT,                                   -- 설명
                       cover_image    VARCHAR(500),                           -- 대표이미지 URL
                       list_price     INTEGER,                                -- 정가
                       sale_price     INTEGER,                                -- 판매가
                       stocks         INTEGER      NOT NULL DEFAULT 0,         -- 수량
                       page_count     INTEGER,                                -- 페이지 수
                       published_date DATE,                                   -- 출판일
                       edition        VARCHAR(50),                            -- 인쇄 판수
                       best_yn        CHAR(1)      NOT NULL DEFAULT 'N',       -- 베스트셀러 여부
                       new_yn         CHAR(1)      NOT NULL DEFAULT 'N',       -- 신간 여부
                       created_at     TIMESTAMP    NOT NULL DEFAULT now(),     -- 등록일
                       updated_at     TIMESTAMP    NOT NULL DEFAULT now(),     -- 수정일
                       CONSTRAINT pk_books PRIMARY KEY (id),
                       CONSTRAINT fk_books_category FOREIGN KEY (category_id) REFERENCES categories (id),
                       CONSTRAINT ck_books_best_yn CHECK (best_yn IN ('Y', 'N')),
                       CONSTRAINT ck_books_new_yn CHECK (new_yn IN ('Y', 'N'))
);

-- ============================================
-- cart_items : 장바구니 항목 테이블
-- ============================================
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
-- book_id 단독 조회/FK 무결성 검사용 인덱스
CREATE INDEX idx_cart_items_book_id ON cart_items (book_id);


-- 카테고리별 도서 조회가 잦으므로 FK 컬럼에 인덱스
CREATE INDEX idx_books_category_id ON books (category_id);

-- ============================================
-- order_items : 주문 결제 배송 항목 테이블
-- ============================================
-- 1. 주문
CREATE TABLE orders (
                        id            VARCHAR(36)  PRIMARY KEY,              -- UUID, 토스 orderId로 그대로 사용
                        user_id       VARCHAR(100) NOT NULL,                 -- users.user_id
                        order_name    VARCHAR(100) NOT NULL,                 -- 토스 orderName ("클린코드 외 2건")
                        status        VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    -- PENDING(결제대기) / PAID(결제완료) / CANCELED(전체취소)
    -- PARTIAL_CANCELED(부분취소) / FAILED(결제실패)
                        total_amount  INTEGER      NOT NULL,                 -- 주문 총액(KRW), 승인 전 amount 검증 기준값
                        created_at    TIMESTAMP,
                        updated_at    TIMESTAMP
);
CREATE INDEX idx_orders_user_id ON orders (user_id);
CREATE INDEX idx_orders_status  ON orders (status);

-- 2. 주문 상품 (주문 시점 가격·제목 스냅샷 — 이후 책 정보가 바뀌어도 주문 내역 보존)
CREATE TABLE order_items (
                             id          VARCHAR(36)  PRIMARY KEY,
                             order_id    VARCHAR(36)  NOT NULL,                   -- orders.id
                             book_id     VARCHAR(40)  NOT NULL,                   -- books.id
                             title       VARCHAR(200) NOT NULL,                   -- 스냅샷
                             unit_price  INTEGER      NOT NULL,                   -- 주문 시점 sale_price 스냅샷
                             quantity    INTEGER      NOT NULL,
                             amount      INTEGER      NOT NULL,                   -- unit_price * quantity
                             created_at  TIMESTAMP,
                             updated_at  TIMESTAMP
);
CREATE INDEX idx_order_items_order_id ON order_items (order_id);

-- 3. 결제 (토스 Payment 객체 매핑, 승인 시도 단위로 1행)
CREATE TABLE payments (
                          id                   VARCHAR(36)  PRIMARY KEY,
                          order_id             VARCHAR(36)  NOT NULL,          -- orders.id (= 토스 orderId)
                          payment_key          VARCHAR(200) NOT NULL UNIQUE,   -- 토스 paymentKey (취소·조회의 키)
                          status               VARCHAR(30)  NOT NULL,
    -- READY / IN_PROGRESS / WAITING_FOR_DEPOSIT / DONE
    -- / CANCELED / PARTIAL_CANCELED / ABORTED / EXPIRED
                          method               VARCHAR(30),                    -- 카드 / 가상계좌 / 간편결제 / 계좌이체 ...
                          easy_pay_provider    VARCHAR(50),                    -- easyPay.provider (토스페이 등, 없으면 NULL)
                          total_amount         INTEGER      NOT NULL,          -- totalAmount
                          balance_amount       INTEGER      NOT NULL,          -- balanceAmount (취소 가능 잔액)
                          supplied_amount      INTEGER,                        -- suppliedAmount (공급가액)
                          vat                  INTEGER,                        -- vat
                          tax_free_amount      INTEGER      DEFAULT 0,         -- taxFreeAmount
                          currency             VARCHAR(3)   NOT NULL DEFAULT 'KRW',
                          requested_at         TIMESTAMPTZ,                    -- requestedAt (+09:00 오프셋 포함 응답)
                          approved_at          TIMESTAMPTZ,                    -- approvedAt
                          receipt_url          VARCHAR(500),                   -- receipt.url (매출전표)
                          last_transaction_key VARCHAR(64),                    -- lastTransactionKey
                          fail_code            VARCHAR(50),                    -- failure.code (승인 실패 시)
                          fail_message         VARCHAR(500),                   -- failure.message
                          raw_response         JSONB,                          -- Payment 객체 원본 (정산·CS 대비)
                          created_at           TIMESTAMP,
                          updated_at           TIMESTAMP
);
CREATE INDEX idx_payments_order_id ON payments (order_id);
-- 승인 성공(DONE)은 주문당 1건만 허용 (실패 후 재시도 행은 여러 개 가능)
CREATE UNIQUE INDEX uq_payments_order_done ON payments (order_id) WHERE status = 'DONE';

-- 4. 결제 취소 이력 (Payment.cancels[] 매핑 — 부분취소 지원)
CREATE TABLE payment_cancels (
                                 id               VARCHAR(36)  PRIMARY KEY,
                                 payment_id       VARCHAR(36)  NOT NULL,              -- payments.id
                                 transaction_key  VARCHAR(64)  NOT NULL UNIQUE,       -- cancels[].transactionKey
                                 cancel_amount    INTEGER      NOT NULL,
                                 cancel_reason    VARCHAR(200) NOT NULL,
                                 tax_free_amount  INTEGER      DEFAULT 0,
                                 cancel_status    VARCHAR(20)  NOT NULL,              -- DONE 등
                                 canceled_at      TIMESTAMPTZ  NOT NULL,
                                 created_at       TIMESTAMP,
                                 updated_at       TIMESTAMP
);
CREATE INDEX idx_payment_cancels_payment_id ON payment_cancels (payment_id);

-- 5. 배송 (결제 DONE 확정 시 생성, 주문당 1건)
CREATE TABLE deliveries (
                            id              VARCHAR(36)  PRIMARY KEY,
                            order_id        VARCHAR(36)  NOT NULL UNIQUE,        -- orders.id
                            receiver_name   VARCHAR(50)  NOT NULL,               -- 수령인 (주문 시점 스냅샷)
                            receiver_phone  VARCHAR(20)  NOT NULL,
                            post_code       VARCHAR(10)  NOT NULL,
                            address         VARCHAR(300) NOT NULL,
                            addr_detail     VARCHAR(300),
                            delivery_memo   VARCHAR(200),                        -- 배송 요청사항
                            status          VARCHAR(20)  NOT NULL DEFAULT 'PREPARING',
    -- PREPARING(배송준비) / SHIPPING(배송중) / DELIVERED(배송완료) / CANCELED(취소)
                            courier_company VARCHAR(50),                         -- 택배사
                            tracking_number VARCHAR(50),                         -- 운송장 번호
                            shipped_at      TIMESTAMP,                           -- 출고 시각
                            delivered_at    TIMESTAMP,                           -- 배송 완료 시각
                            created_at      TIMESTAMP,
                            updated_at      TIMESTAMP
);
CREATE INDEX idx_deliveries_status ON deliveries (status);