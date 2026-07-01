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
