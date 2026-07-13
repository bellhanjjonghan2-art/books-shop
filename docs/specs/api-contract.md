# API 계약 — JWT 로그인 (백엔드 초안)

> 본 문서는 `docs/login.md` 5절("에이전트 간 인터페이스 계약")을 Source of Truth로 하여,
> 백엔드(Spring Boot) 구현 관점에서 검토한 뒤 확정한 API 계약 **초안**이다.
> 작성 주체: 백엔드 에이전트. **프론트 에이전트의 검토 및 합의가 아직 끝나지 않았다.**
> PRD 5절의 엔드포인트·요청/응답 스키마는 그대로 유지했고, **공통 응답/에러 포맷(5.3절)만
> 백엔드 공통 규약(`backend-api-response` 스킬)에 맞춰 봉투(envelope)를 씌우는 조정을 제안한다.**
> 자세한 사유는 "3. 공통 응답 규칙" 및 "검토 상태" 참고.

---

## 1. `POST /api/auth/login`

로그인. 인증 불필요 (`permitAll`).

### Request Body

```json
{
  "userId": "string",
  "passwd": "string"
}
```

| 필드 | 타입 | 필수 | 비고 |
|---|---|---|---|
| userId | string | Y | 로그인 ID |
| passwd | string | Y | 평문 비밀번호 (전송용, 서버 저장/로그 금지) |

### Response 200 (성공)

PRD 5.1 스키마를 `ApiResponse.data` 안에 그대로 보존한다 (필드명·타입 변경 없음).

```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGci...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "user": {
      "userId": "hong123",
      "names": "홍길동",
      "roles": "USER"
    }
  }
}
```

### Response 401 (인증 실패)

```json
{
  "success": false,
  "code": "AUTH_FAILED",
  "message": "아이디 또는 비밀번호가 올바르지 않습니다."
}
```

> 보안상 "아이디 없음"과 "비밀번호 틀림"을 구분하지 않고 동일 메시지로 응답한다. (PRD 5.1 그대로)

### Response 400 (입력값 오류)

```json
{
  "success": false,
  "code": "INVALID_REQUEST",
  "message": "userId와 passwd는 필수입니다."
}
```

> `@Valid` 검증 실패(`MethodArgumentNotValidException`)는 `backend-api-response` 스킬의
> `GlobalExceptionHandler`에서 공통 처리되며, 이때도 위와 동일한 `code: "INVALID_REQUEST"`로
> 응답하도록 `ErrorCode` enum에 정의한다.

---

## 2. `GET /api/auth/me` (인증 필요)

현재 토큰의 사용자 정보를 반환한다. (앱 진입 시 토큰 유효성 확인 용도)

### Request Header

```
Authorization: Bearer <accessToken>
```

### Response 200 (성공)

```json
{
  "success": true,
  "data": {
    "userId": "hong123",
    "names": "홍길동",
    "roles": "USER"
  }
}
```

### Response 401 (토큰 없음/만료/위변조)

```json
{
  "success": false,
  "code": "TOKEN_INVALID",
  "message": "토큰이 유효하지 않습니다."
}
```

---

## 3. 공통 응답 규칙

### 3.1 PRD 5.3절과의 차이 — 백엔드 제안 (프론트 검토 필요)

PRD 5.3절은 다음을 명시한다.

- 모든 에러 응답은 `{ "code": string, "message": string }` 형태.
- 성공 응답은 5.1/5.2 명세의 키 이름/타입을 그대로 지킨다 (봉투 없는 평면 구조).

반면 이 저장소의 백엔드 공통 규약(`backend-api-response` 스킬)은 모든 응답을 다음 두 타입으로
통일하도록 강제한다.

- 성공: `ApiResponse<T> { success: true, data: T }`
- 에러: `ErrorResponse { success: false, code, message }`

이 규약은 로그인 기능뿐 아니라 향후 추가될 모든 API(도서 목록, 주문 등)에 동일하게 적용되는
프로젝트 전역 컨벤션이므로, 로그인 API만 예외로 봉투 없이 응답하면 클라이언트의 axios
인터셉터·에러 처리 로직이 API마다 분기해야 하는 비용이 생긴다.

**백엔드 제안:** PRD 5.1/5.2의 필드 이름·타입은 한 글자도 바꾸지 않고, 다음과 같이
봉투만 추가한다.

| 항목 | PRD 5절 원안 | 백엔드 제안 |
|---|---|---|
| 성공 응답 | 최상위에 `accessToken`, `user` 등 직접 위치 | `success: true`와 함께 `data` 필드 안에 위치 (필드 내부 구조는 PRD와 동일) |
| 에러 응답 | `{ code, message }` | `{ success: false, code, message }` (`success` 필드만 추가, `code`/`message`는 PRD와 동일한 의미·값) |

즉 클라이언트 입장에서 추가로 해야 할 일은 **`response.data.data`로 한 단계 더 접근하는 것**과
에러 시 `success: false` 플래그를 보는 것뿐이며, `code`/`message`/`accessToken`/`user` 등
필드명과 값의 의미는 PRD와 완전히 동일하다.

> 이 절충안은 백엔드의 제안이며 최종 확정이 아니다. 프론트 에이전트가 다음 단계에서
> 이 봉투 구조가 axios 인터셉터·TanStack Query 처리에 문제가 없는지 검토해야 한다.
> 만약 프론트가 PRD 5절의 평면 구조를 그대로 요구한다면, 로그인/me API에 한해
> 공통 규약의 예외를 두는 방향도 재논의 가능하다.

#### 3.1.1 프론트 검토 결과 — 승인

`front-api-client` 스킬 기준 axios 인스턴스(`src/api/client.js`)는 응답 인터셉터에서
`res.data`를 그대로 통과시키는 구조이므로, 봉투를 푸는 작업은 도메인 요청 함수
(`src/api/auth.js`) 레벨에서 `const { data } = await api.post('/auth/login', payload); return data.data`
처럼 한 단계만 더 접근하면 끝난다. 인터셉터 자체를 API마다 분기할 필요가 없다.

에러도 마찬가지로 `error.response.data`가 `{ success:false, code, message }` 형태이므로
`useMutation`의 `onError`/컴포넌트에서 `error.response.data.code`,
`error.response.data.message`로 그대로 사용 가능하며, PRD 5.3의 `{ code, message }`와
의미상 동일하다.

TanStack Query(`useQuery`/`useMutation`)와 zustand `authStore`(`setAuth`)는 모두 위에서
이미 한 단계 풀어낸 `{ accessToken, tokenType, expiresIn, user }` 객체를 그대로 받아 쓰면
되므로 추가 구조 변경이 필요 없다.

**결론: 백엔드 제안 봉투 구조를 그대로 승인한다.** 페이징 누락, 필수 필드 누락 등의
문제도 없으며, PRD 5절의 필드명·타입은 그대로 보존되어 있다.

### 3.2 에러 코드 정의

`ErrorCode` enum에 다음 항목을 추가한다 (`backend-api-response` 스킬 5번 규칙).

| code | HTTP Status | message |
|---|---|---|
| AUTH_FAILED | 401 | 아이디 또는 비밀번호가 올바르지 않습니다. |
| TOKEN_INVALID | 401 | 토큰이 유효하지 않습니다. |
| INVALID_REQUEST | 400 | userId와 passwd는 필수입니다. |

### 3.3 기타 공통 규칙

- 시간 필드는 ISO-8601(예: `2026-06-24T10:00:00Z`)을 사용한다. (PRD 5.3 그대로)
- 비밀번호(`passwd`)는 어떤 응답에도 포함하지 않는다.
- 에러 응답 본문은 항상 `ErrorResponse`로 통일하며, 컨트롤러에서 try-catch 하지 않고
  `@RestControllerAdvice`(`GlobalExceptionHandler`)에서 공통 처리한다.

---

## 4. 인증 방식 요약

| 항목 | 값 |
|---|---|
| 방식 | Bearer JWT (`Authorization: Bearer <accessToken>`) |
| 알고리즘 | HS256 |
| 만료 시간 | 1시간 (3600초), 환경변수로 조정 가능 |
| 시크릿 키 | 환경변수 `JWT_SECRET` (256bit 이상, 코드 하드코딩 금지) |
| Claims | `sub`(userId), `names`, `roles`, `iat`, `exp` |
| 세션 정책 | STATELESS (Spring Security) |
| 보호 경로 | `POST /api/auth/login` 제외 전부 `authenticated()` |
| CSRF | 비활성화 (토큰 기반이므로) |

---

## 5. URL 설계에 대한 참고 (RESTful 규약 검토)

`backend-api-conventions` 스킬은 자원을 복수형 명사로, 행위는 HTTP 메서드로 표현할 것을
규약으로 두고 있다. `/api/auth/login`, `/api/auth/me`는 표면적으로 동사형 경로지만,
인증(auth) 도메인은 "자원"이 아니라 "인증 행위/세션 컨텍스트"를 다루는 관용적 예외 영역으로
널리 쓰이는 패턴(`/auth/login`, `/auth/me`, `/auth/logout` 등)이며, PRD 5절이 이미 이
경로를 계약으로 고정했으므로 **변경하지 않고 그대로 채택**한다.

---

## 6. 비고 (다음 구현 단계 참고용 합의 사항)

- **로그인 성공 후 기본 이동 경로**: `"/"` (루트). 라우팅 가드에서 보호 라우트 접근 중
  리다이렉트된 경우 원래 경로 우선, 없으면 `/`로 이동.
- **로그아웃 트리거 위치**: 헤더(MainLayout의 전역 메뉴)에 로그아웃 버튼을 둔다.
  현재 MainLayout이 아직 없으므로 이번 단계에서 직접 만들지 않으며, 로그인/인증 기능
  구현(Phase 3) 시 헤더에 로그아웃 버튼을 추가하는 것으로 합의한다.

---

## 검토 상태

프론트 검토 완료 — 계약 확정

---
---

# API 계약 — 카테고리별 도서 리스트 (백엔드 초안)

> 본 문서는 `docs/specs/features.md` 하단 "기능 명세 — 카테고리별 도서 리스트 화면 API 연동"
> (A0~A6, US-1~US-9)을 요구사항 Source of Truth로 하고, `docs/bookList.md` 초안을
> 백엔드 컨벤션(`backend-api-conventions`, `backend-api-response`, `backend-jpa-persistence`)에
> 맞춰 검토·조정한 API 계약 **초안**이다.
> 작성 주체: 백엔드 에이전트. **프론트 에이전트의 검토 및 합의가 아직 끝나지 않았다.**
> 이 단계는 계약 정의까지만 진행하며, 구현은 프론트 합의 이후 다음 단계에서 진행한다.

---

## 1. URL 설계 검토 — `docs/bookList.md`의 `/api/books/{types}` 안 조정

### 1.1 문제

`docs/bookList.md` 초안은 엔드포인트를 `/api/books/{types}`로 제안했다. 그러나 기존
`BookController`에는 이미 도서 상세 조회용 `GET /api/books/{bookId}`가 있다.

Spring MVC는 URL을 **패턴** 단위로 매칭한다. `/api/books/{bookId}`와 `/api/books/{types}`는
경로 세그먼트 구조가 완전히 동일(`/api/books/` + 변수 1개)하기 때문에, 변수명이 달라도
동일한 패턴으로 취급되어 두 핸들러가 동시에 등록되면 **애플리케이션 구동 시점에
`IllegalStateException`(Ambiguous mapping)이 발생한다.** 즉 `types` 값이 실제로는
`IT`/`NOVEL`/`SELF` 3종 고정값이라 `bookId`(길이 40 문자열 PK)와 값 자체는 겹치지 않지만,
Spring 라우팅은 값이 아니라 패턴으로 등록되므로 이 사실과 무관하게 충돌한다. `docs/bookList.md`
초안을 그대로 쓸 수 없다.

### 1.2 검토한 대안

| 안 | 설명 | 채택 여부 |
|---|---|---|
| A. `/api/books/{types}` (초안 그대로) | 위 사유로 `/api/books/{bookId}`와 패턴 충돌, 구동 불가 | 기각 |
| B. `GET /api/books?category=IT&page=&size=&orderType=` | 쿼리 파라미터로 필터링. 컨벤션상 "필터·정렬·페이징은 쿼리 파라미터로" 원칙엔 부합하나, 기존 `GET /api/books`(메인페이지 top-N 집계 응답)와 URL이 완전히 같아져 **같은 URL이 쿼리 파라미터 유무에 따라 응답 스키마 자체가 달라지는** 문제 발생(메인페이지 응답은 `MainPageBooksResponse`, 카테고리 목록 응답은 페이지네이션 리스트로 구조가 다름). 하나의 리소스 URL이 두 가지 다른 표현을 반환하는 것은 혼란을 유발하므로 기각 | 기각 |
| C. `GET /api/books/category/{types}` | `/api/books/{bookId}`(세그먼트 1개)와 `/api/books/category/{types}`(세그먼트 2개)는 세그먼트 개수가 달라 패턴이 겹치지 않는다. 기존 `BookController`에 그대로 추가 가능, 구조 변경 최소화 | **채택** |
| D. `GET /api/categories/{types}/books` (계층형 자원) | "계층 관계는 경로로 표현" 컨벤션 예시(`/users/{userId}/orders`)에 가장 부합. 다만 이를 위해서는 `Category`를 1급 자원으로 노출하는 `CategoryController`를 새로 만들어야 하고, 현재 이 프로젝트에 카테고리 자체를 조회하는 API가 없어 범위가 커진다 | 이번 단계에서는 과함 — 후보로만 남김 |

### 1.3 결론

**`GET /api/books/category/{types}`** 를 채택한다. 기존 `BookController`에 그대로 추가하며,
`category`는 행위(동사)가 아니라 "도서를 카테고리 기준으로 필터링한 하위 컬렉션"을 가리키는
명사 세그먼트로 컨벤션에 위배되지 않는다고 판단한다. (D안이 더 순수한 REST 계층 구조이지만,
현재 범위와 기존 코드 최소 변경 원칙을 고려해 C안을 우선 제안한다. 프론트/팀 협의에서 D안을
선호하면 재논의 가능.)

> **프론트 확인 필요:** 위 URL 설계 판단(C안 채택)에 대한 동의 여부.

---

## 2. `GET /api/books/category/{types}`

카테고리별 도서 목록을 정렬·페이지네이션하여 조회한다. 인증 불필요 (`permitAll`, 기존
`/api/books`, `/api/books/{bookId}`와 동일한 공개 API).

### 2.1 Path Variable

| 이름 | 타입 | 필수 | 값 | 비고 |
|---|---|---|---|---|
| types | string | Y | `IT` \| `NOVEL` \| `SELF` | `Category.types`와 동일한 고정 3종 값. 대소문자 구분(정확히 대문자 일치만 허용) |

### 2.2 Query Parameters

| 이름 | 타입 | 필수 | 기본값 | 값 | 비고 |
|---|---|---|---|---|---|
| page | int | N | `0` | 0 이상 정수 | 0-based 페이지 번호 |
| size | int | N | `10` | 1 이상 정수 | 페이지당 건수 (US-4: 기본 10 고정 사용 예정이나 파라미터 자체는 가변 허용) |
| orderType | string | N | `new` | `new` \| `lower` \| `high` \| `reviewCnt` | 정렬 기준. `docs/bookList.md` 3절 값 그대로 채택 |

`orderType` 값 의미:

| 값 | 의미 (프론트 버튼 라벨) |
|---|---|
| new | 최신순 (기본값) |
| lower | 낮은가격순 |
| high | 높은가격순 |
| reviewCnt | 리뷰많은순 |

> "인기순"은 값 목록에 없다 (US-3, 이미 제거된 상태 유지).

### 2.3 정렬 규칙 상세

모든 정렬은 **동률 시 `title` 오름차순**을 2차 정렬 기준으로 고정한다(US-2, 안정적 페이지네이션을
위해 모든 orderType에 공통 적용).

| orderType | 기준 필드/집계 | ORDER BY (개념적 SQL) |
|---|---|---|
| new (기본) | `Book.publishedDate` | `ORDER BY published_date DESC, title ASC` |
| lower | `Book.salePrice` | `ORDER BY sale_price ASC, title ASC` |
| high | `Book.salePrice` | `ORDER BY sale_price DESC, title ASC` |
| reviewCnt | `COUNT(Review.id)` (book_id 기준 집계, 리뷰 0건 포함) | `LEFT JOIN reviews ON reviews.book_id = books.id ... GROUP BY books.id ORDER BY COUNT(reviews.id) DESC, title ASC` |

- 카테고리 필터: `WHERE books.category_id = (해당 types의 Category.id)` (또는 `category.types = :types`로 JPQL 조인).
- `reviewCnt` 정렬은 리뷰가 0건인 도서도 `LEFT JOIN`으로 포함되어야 한다(US-1 마지막 항목, 리뷰 0건 도서도 응답 포함 요건과 동일한 이유).
- `Review` 엔티티는 현재 `Book`과 JPA 연관관계(`@ManyToOne`/`@OneToMany`)가 아니라 평범한 `bookId` 문자열 컬럼으로만 연결되어 있다(`Review.java` 확인). 구현 시 JPQL의 명시적 `ON` 절을 이용한 ad-hoc join 또는 별도 집계 쿼리(리뷰 통계를 먼저 구해 Map으로 병합) 중 하나를 선택해야 한다 — **이번 계약 문서에는 방식을 못박지 않으며, 구현 단계에서 백엔드가 정한다.** (참고: `backend-jpa-persistence` 스킬상 join이 복잡하면 JPQL/QueryDSL 사용 가능.)

### 2.4 Response 200 (성공)

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "bookId": "b3f1a2c4-1234-4a11-9c8e-000000000001",
        "title": "이펙티브 자바",
        "subtitle": "자바 개발자를 위한 프로그래밍 가이드",
        "author": "조슈아 블로크",
        "publisher": "인사이트",
        "publishedDate": "2024-11-01",
        "totalReviewCnt": 12,
        "reviewRating": 9.2,
        "listPrice": 36000,
        "salePrice": 32400,
        "coverImage": "https://cdn.example.com/covers/effective-java.jpg",
        "bestYn": "Y",
        "newYn": "N"
      },
      {
        "bookId": "b3f1a2c4-1234-4a11-9c8e-000000000002",
        "title": "이번 주 신간 도서",
        "subtitle": null,
        "author": "김철수",
        "publisher": "한빛미디어",
        "publishedDate": "2026-06-30",
        "totalReviewCnt": 0,
        "reviewRating": null,
        "listPrice": 22000,
        "salePrice": 22000,
        "coverImage": "",
        "bestYn": "N",
        "newYn": "Y"
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 37,
    "totalPages": 4
  }
}
```

### 2.5 응답 필드 스키마

`data` (`BookListResponse`)

| 필드 | 타입 | nullable | 비고 |
|---|---|---|---|
| content | array | N (빈 배열 가능) | 아래 항목 DTO 리스트. 0건이면 `[]` |
| page | int | N | 요청한(또는 보정된) 0-based 현재 페이지 |
| size | int | N | 요청한(또는 보정된) 페이지당 건수 |
| totalElements | long | N | 해당 카테고리의 전체 도서 수 (US-4, "총 N권" 표시용) |
| totalPages | int | N | `ceil(totalElements / size)` |

`content[]` 항목 (`BookListItemDto`)

| 필드 | 타입 | nullable | 비고 |
|---|---|---|---|
| bookId | string | N | `Book.id` (PK) |
| title | string | N | |
| subtitle | string | Y | `Book.subtitle`이 없으면 `null` |
| author | string | Y | |
| publisher | string | Y | |
| publishedDate | string (`YYYY-MM-DD`, ISO-8601 date) | N | `Book.publishedDate`(LocalDate) 그대로 직렬화. 화면 표시 포맷(`YYYY.MM` 등)은 프론트 책임(A5 Q4 — 확인 필요 항목, 우선 데이터는 일자까지 유지) |
| totalReviewCnt | long | N | 리뷰 0건이면 `0` (null 아님) |
| reviewRating | number (double) | **Y** | `Review.rating`(1~5) 평균값 × 2, 10점 만점. 리뷰 0건이면 `null`(US-6, A5 Q3 — "리뷰 없음" 프론트 표시용) |
| listPrice | int | N | |
| salePrice | int | N | |
| coverImage | string | Y (빈 문자열 가능) | 빈 값/로드 실패 시 대체 처리는 프론트 책임(US-7) |
| bestYn | string(`"Y"`\|`"N"`) | N | 배지 표시는 프론트 책임(US-5, A4.3) |
| newYn | string(`"Y"`\|`"N"`) | N | 배지 표시는 프론트 책임(US-5, A4.3) |

> mock의 `badge`, `coverColor` 필드는 응답에 포함하지 않는다(A4.1, 폐기 확정).

### 2.6 Response 400 — 잘못된 category

```json
{
  "success": false,
  "code": "INVALID_CATEGORY",
  "message": "유효하지 않은 카테고리입니다."
}
```

`types`가 `IT`/`NOVEL`/`SELF` 중 하나가 아니면 400을 반환한다. 존재하지 않는 카테고리를
"빈 목록"(200)이 아니라 "잘못된 요청"(400)으로 처리하는 이유는, `types`가 유한하고 고정된
값 집합(사실상 enum)이기 때문에 그 범위를 벗어난 값은 클라이언트 입력 오류로 보는 것이
합리적이라고 판단했기 때문이다. (구현 시 `types`를 Java enum `PathVariable`로 받고,
`MethodArgumentTypeMismatchException`을 `GlobalExceptionHandler`에서 `INVALID_CATEGORY`로
매핑하는 방식을 제안 — 이 역시 다음 구현 단계에서 확정.)

### 2.7 Response 400 — 잘못된 orderType

```json
{
  "success": false,
  "code": "INVALID_ORDER_TYPE",
  "message": "유효하지 않은 정렬 기준입니다."
}
```

`orderType`이 `new`/`lower`/`high`/`reviewCnt` 중 하나가 아니면 400을 반환한다.

### 2.8 에러 코드 추가분

`ErrorCode` enum에 다음 항목을 추가한다.

| code | HTTP Status | message |
|---|---|---|
| INVALID_CATEGORY | 400 | 유효하지 않은 카테고리입니다. |
| INVALID_ORDER_TYPE | 400 | 유효하지 않은 정렬 기준입니다. |

### 2.9 확인 필요 (프론트/팀 협의 대상)

1. **1.3절 URL 설계(C안 `/api/books/category/{types}` vs D안 `/api/categories/{types}/books`)**
   에 대한 동의 여부.
2. **`page`/`size` 파라미터 자체가 비정상 값(음수, `size=0` 등)일 때의 처리**: 이번 계약에는
   명시하지 않았다. 기본값으로 보정(clamp)할지, 400으로 거부할지 다음 단계에서 확정 필요.
   (제안: 400 `INVALID_REQUEST`로 거부하되, 확정은 보류.)
3. **`totalPages`가 0건일 때 값**: `totalElements=0`이면 `totalPages=0`으로 내려줄지 `1`(빈 페이지
   1개로 취급)로 내려줄지 — 이번 문서는 `0`으로 가정했으나 프론트 페이지네이션 UI 구현 방식에
   따라 재확인 필요.
4. **`publishedDate` 표시 포맷**(A5 Q4)은 기존 features.md 문서에서도 미확정 상태이며, 이 계약은
   데이터 자체를 ISO 날짜 문자열로 내려주는 것까지만 확정한다. 화면 표시 포맷은 프론트 협의 대상.

### 2.10 프론트 검토 결과 — 승인

`front-api-client`, `front-ui-component` 스킬 기준으로 기존 코드
(`shop-front/src/components/BookListPage/BookListPage.jsx`,
`shop-front/src/pages/{ITBooks,Novel,SelfDev}/*.jsx`,
`shop-front/src/api/books.js`, `shop-front/src/hooks/queries/useBooks.js`)를 검토한 결과를
아래에 남긴다. **구현은 이번 단계에서 하지 않는다.**

**필드 완전성**: `content[]`의 bookId/title/subtitle/author/publisher/publishedDate/
totalReviewCnt/reviewRating/listPrice/salePrice/coverImage/bestYn/newYn 12개 필드는
기존 `BookListPage`가 화면에 쓰는 모든 값(id, title, subtitle, author, publisher,
publishedDate, reviewCount, rating, listPrice, salePrice, 그리고 badge/coverColor를
대체하는 bestYn/newYn/coverImage)을 빠짐없이 커버한다. 추가로 필요한 필드는 없다.

**페이지네이션 방식**: `content`/`page`/`size`/`totalElements`/`totalPages` 조합은 기존 UI의
처음/이전/숫자(±2)/다음/마지막 이동과 "총 N권" 표시(A4.2)를 구현하기에 충분하다. 다만
현재 `BookListPage`는 `books` 배열 전체를 props로 받아 내부에서
`useMemo(sortBooks) → slice`로 클라이언트 사이드 정렬/페이지네이션을 하는 구조이므로, 다음
구현 단계에서는 (1) `books` 배열 대신 `categoryTypes`를 props로 받아 컴포넌트 내부에서
`useQuery(['books', 'category', categoryTypes, page, orderType])`로 직접 조회하거나, (2) 페이지
컴포넌트(`ITBooksPage` 등)가 훅을 호출해 `{ content, page, size, totalElements, totalPages }`를
props로 내려주는 방식 중 하나로 전환해야 한다(A4.2 권고와 동일, 정렬 변경 시 1페이지로
초기화하는 기존 동작은 유지). 이는 계약 승인과 무관하게 다음 단계 구현 이슈로 남긴다.

**정렬 파라미터명**: `orderType`(`new`/`lower`/`high`/`reviewCnt`)은 프론트 내부 정렬 키
(`newest`/`priceLow`/`priceHigh`/`reviews`, `BookListPage`의 `SORT_OPTIONS`)와 이름이
다르지만, 버튼 클릭 시 내부 키 → `orderType` 값으로 매핑하는 작은 상수 테이블 하나면
충분하므로 문제 없다. API 값 이름을 프론트 내부 키에 맞춰 바꿔달라고 요청할 실익이 없다.

**2.9절 확인 필요 항목에 대한 결론**

1. **URL 설계(C안 vs D안)** — **C안(`GET /api/books/category/{types}`) 채택에 동의한다.**
   기존 `/api/books/{bookId}`와의 패턴 충돌을 피하면서 컨트롤러 변경을 최소화하는 합리적
   판단이다. D안(`/api/categories/{types}/books`)은 카테고리 자체를 1급 자원으로 노출하는
   `CategoryController` 신설이 전제되는데, 이번 기능 범위(A2)에 카테고리 조회 자체는 없으므로
   과한 확장이다. 프론트 쪽에서 D안을 선호할 특별한 이유도 없다(`src/api/books.js`에 함수
   하나 추가하는 수준이라 URL 형태가 호출부 복잡도에 영향을 주지 않는다).
2. **page/size 비정상값 처리** — **clamp(보정) 방식을 제안한다.** 이번 기능에서 page/size는
   사용자가 URL을 직접 조작하지 않는 한 프론트 페이지네이션 버튼(처음/이전/다음/마지막,
   `disabled` 처리로 범위 밖 이동 자체를 막음)을 통해서만 서버로 전달되므로, 정상 플로우에서는
   비정상 값이 발생하지 않는다. 그럼에도 방어적으로 값을 받는다면, 400 `INVALID_REQUEST`로
   거부하는 것보다 `page<0 → 0`, `size<1 → 10`(또는 기본값)으로 보정해 항상 200을 반환하는
   편이 프론트 입장에서 별도 에러 분기를 추가하지 않아도 되어 더 실용적이다. 다만 이 항목은
   백엔드 구현 편의가 우선이므로, 백엔드가 400 방식을 택해도 프론트 구현 난이도에 큰 차이는
   없다(어차피 `isError` 공통 에러 처리 경로를 타면 된다). **최종 판단은 백엔드에 위임**하되,
   프론트는 clamp를 선호한다는 의견만 남긴다.
3. **totalElements=0일 때 totalPages 값** — **`totalPages=0`(현재 문서 가정) 그대로 승인한다.**
   `BookListPage`의 페이지네이션 바는 `totalPages > 1`일 때만 렌더링되므로 `0`이든 `1`이든
   페이지네이션 바 노출 여부에는 차이가 없다. "총 0권" 표시(US-1 빈 상태, A5 Q5)와 개념적으로도
   `totalPages=0`이 더 직관적이다.
4. **publishedDate 표시 포맷(A5 Q4)** — **목록 화면은 `YYYY.MM`(일 단위 생략)으로 확정한다.**
   기존 mock 포맷과 동일하게 유지해 디자인 변경을 최소화한다. API는 계약대로 ISO
   `YYYY-MM-DD` 문자열을 그대로 내려주고, 목록 컴포넌트(`BookListPage`)에서 표시 시점에
   `YYYY.MM`으로 잘라 포맷한다(정렬은 서버가 `Book.publishedDate`(LocalDate) 원본 일자 기준으로
   수행하므로 화면 표시 축약과 무관하게 정확하다). 도서 상세 페이지 등 다른 화면에서 더 상세한
   일자 표시가 필요하면 그 화면에서 별도로 포맷하면 되므로 이번 결정과 충돌하지 않는다.

**추가 제안 없음**: 로딩/에러 상태는 TanStack Query의 `isPending`/`isError`로 충분히
처리 가능하므로(front-api-client 규칙 6), 계약에 별도 로딩/에러 필드를 추가할 필요는 없다.

**결론: 백엔드 초안(2절 전체, C안 URL 설계 포함)을 그대로 승인한다.** 2.9절 확인 필요
항목 중 (2) page/size 비정상값 처리만 프론트 선호(clamp)를 의견으로 남기고 최종 결정은
백엔드에 위임하며, 그 외 항목은 위 결론대로 확정한다.

---

## 검토 상태 (카테고리별 도서 리스트)

프론트 검토 완료 — 계약 확정

---
---

# API 계약 — 주문서 도서 다건 조회 (백엔드 초안)

> 본 문서는 `docs/order-page.md` 6절("주문 상품 목록 카드 + 도서 조회 API")을 Source of Truth로
> 하고, 백엔드 공통 규약(`backend-api-response`, `backend-jpa-persistence`)에 맞춰 확정한
> API 계약이다. 작성 주체: 백엔드 에이전트.

---

## 1. `GET /api/books?bookIds=id1,id2,id3`

주문서 작성 화면에서 `location.state.items`의 `bookId` 목록으로 도서 정보를 한 번에 조회한다.
인증 불필요(`permitAll`, 기존 `/api/books`, `/api/books/{bookId}`와 동일한 공개 API).

기존 `GET /api/books`(파라미터 없음, 메인페이지 추천도서 top-N 응답)와 URL이 동일하지만,
`@GetMapping(params = "bookIds")`로 분기해 파라미터 유무로 두 핸들러를 구분한다. 세그먼트 수가
같은 `/api/books/{bookId}`와도 겹치지 않는다(그쪽은 path variable 세그먼트, 이쪽은 쿼리 파라미터).

### 1.1 Query Parameters

| 이름 | 타입 | 필수 | 값 | 비고 |
|---|---|---|---|---|
| bookIds | string | Y | 콤마(`,`)로 구분된 bookId 목록 | 예: `bookIds=IBK-abc,IBK-def`. 장바구니 삭제 API와 동일 컨벤션 |

### 1.2 Response 200 (성공)

```json
{
  "success": true,
  "data": [
    {
      "bookId": "IBK-abc123",
      "title": "클린 코드",
      "author": "로버트 C. 마틴",
      "coverImage": "https://...",
      "categoryName": "IT",
      "salePrice": 22000
    }
  ]
}
```

`data[]` 항목 (`BookOrderItemDto`)

| 필드 | 타입 | nullable | 비고 |
|---|---|---|---|
| bookId | string | N | `Book.id` (PK) |
| title | string | N | |
| author | string | Y | |
| coverImage | string | Y | |
| categoryName | string | N | `Book.category.name` (조인) |
| salePrice | int | Y | |

### 1.3 존재하지 않는 bookId 처리

요청에 존재하지 않는 bookId가 섞여 있어도 요청 전체를 실패시키지 않는다. 해당 항목만 조용히
결과에서 제외하고 나머지는 200으로 정상 반환한다(404 아님, 별도 에러 코드 없음). 응답 배열의
순서는 보장하지 않는다 — 프론트가 원래 `items` 순서대로 매핑한다(PRD 6절).

### 1.4 구현 메모

- Repository: `Book.category`(`ManyToOne`)를 조인해 `category.name → categoryName`으로
  projection하는 JPQL `@Query`(`SELECT new ...BookOrderItemDto(...) WHERE b.id IN :bookIds`).
- 기존 `GET /api/books`(`getMainPageBooks()`) 동작·시그니처는 변경하지 않았다.

### 1.5 확인 필요

- 없음. PRD 6절에 스키마·에러 처리 방침이 이미 확정되어 있어 별도 협의 없이 그대로 구현했다.

---

## 검토 상태 (주문서 도서 다건 조회)

백엔드 구현 완료 — 프론트 검토 완료 (필드명 `bookId/title/author/coverImage/categoryName/salePrice` 일치 확인, 추가 조정 없음). 계약 확정, 프론트/백 구현 완료.

---
---

# API 계약 — 주문/배송 목록 조회 (백엔드 구현)

> 본 문서는 `docs/order-list-page.md` §3("신규 API — `GET /api/orders`")을 Source of Truth로 하여
> 그대로 구현한 API 계약이다. 필드명·에러코드 등 스펙 변경 없이 PRD를 그대로 따랐다.
> 작성 주체: 백엔드 에이전트. 백엔드 구현 완료.

---

## 1. `GET /api/orders`

로그인한 사용자 본인의 결제완료(PAID) 주문을 기간 필터·페이지네이션으로 조회한다.
기존 `GET /api/orders/{orderId}`(주문 결과 단건 조회)와 세그먼트 구조가 달라 라우팅 충돌은 없다.

### 1.1 Request

| 항목 | 값 |
|---|---|
| 인증 | JWT 필요 (`Authorization: Bearer {accessToken}`) |

| Query | 타입 | 필수 | 기본값 | 값 |
|---|---|---|---|---|
| period | string | N | `1m` | `1m`(1개월) \| `3m`(3개월) \| `6m`(6개월) \| `all`(전체) |
| page | int | N | `0` | 0-based 페이지 번호 |
| size | int | N | `5` | 페이지당 건수 |

`period`에 따른 조회 기준: `orders.created_at >= LocalDateTime.now().minusMonths(N)` (`all`이면 기간 조건 생략).

### 1.2 Response 200 (성공)

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
            "coverImage": "https://...", "quantity": 1, "amount": 23400 }
        ]
      }
    ]
  }
}
```

`data` (`OrderListResponse`)

| 필드 | 타입 | 비고 |
|---|---|---|
| totalCount | Long | 조건(본인·PAID·기간) 전체 주문 수 |
| page / size / totalPages | int | 페이징 메타 |
| items[] | array | 주문별 목록. `orders.created_at` DESC 정렬. 0건이면 `[]` |

`items[]` 항목 (`OrderSummaryDto`)

| 필드 | 타입 | 소스 |
|---|---|---|
| orderId | string | `orders.id`(UUID) |
| orderDate | string (`YYYY-MM-DD`) | `orders.created_at` |
| deliveryStatus | string | `deliveries.status` (배송 행 없으면 방어적으로 `PREPARING` 기본값) |
| totalAmount | Integer | `orders.total_amount` |
| items[] | array | 주문 상품 목록(아래) |

`items[].items[]` 항목 (`OrderListItemDto`)

| 필드 | 타입 | 소스 |
|---|---|---|
| bookId | string | `order_items.book_id` |
| title | string | `order_items.title`(스냅샷) |
| coverImage | string | `books.cover_image`(JOIN) |
| quantity | Integer | `order_items.quantity` |
| amount | Integer | `order_items.amount` |

### 1.3 처리 규칙

- 대상: `orders.user_id == 토큰 userId` AND `orders.status = 'PAID'` AND 기간 조건(all이면 생략).
- 정렬: `orders.created_at` DESC.
- 빈 목록은 오류가 아니다 — `totalCount:0`, `totalPages:0`, `items:[]`.
- N+1 방지: 페이지에 포함된 주문들의 상품·배송을 각각 `order_id IN (...)`으로 배치 조회한 뒤
  자바에서 `orderId` 기준으로 그룹핑해 조립한다(`OrderItemRepository.findListItemsByOrderIdIn`,
  `DeliveryRepository.findByOrder_IdIn`).

### 1.4 Response 400 — 잘못된 period

```json
{
  "success": false,
  "code": "INVALID_ORDER_PERIOD",
  "message": "유효하지 않은 조회 기간입니다."
}
```

`period`가 `1m`/`3m`/`6m`/`all` 중 하나가 아니면 400을 반환한다. 기존 `INVALID_INPUT`을
재사용하지 않고 신규 코드로 추가했다(PRD §3-4 지침).

### 1.5 에러 코드 추가분

`ErrorCode` enum에 다음 항목을 추가했다.

| code | HTTP Status | message |
|---|---|---|
| INVALID_ORDER_PERIOD | 400 | 유효하지 않은 조회 기간입니다. |

### 1.6 구현 메모

- `OrderRepository`: `findByUserIdAndStatus`, `findByUserIdAndStatusAndCreateAtGreaterThanEqual`
  파생 쿼리 메서드로 페이징 조회(`BookService.getBooksByCategory()` 페이징 보정 패턴과 동일하게
  `page<0→0`, `size<1→기본값(5)` 보정).
- 상품 배치 조회는 내부 전용 프로젝션 `OrderListItemBatchDto`(orderId 포함)로 받은 뒤
  `orderId`별로 그룹핑해 응답 DTO `OrderListItemDto`(orderId 제외)로 변환한다.
- 배송 상태는 `DeliveryRepository.findByOrder_IdIn`으로 배치 조회한 `Delivery` 엔티티에서
  `orderId → status` Map으로 변환해 조립한다(서비스 계층 내부에서만 엔티티 사용, 응답은 DTO).

### 1.7 확인 필요

- 없음. PRD(`docs/order-list-page.md`) §0/§3에 스키마·에러 처리 방침이 이미 확정되어 있어
  별도 협의 없이 그대로 구현했다.

---

## 검토 상태 (주문/배송 목록 조회)

백엔드 구현 완료 — `./gradlew :shop-back:build` 컴파일 성공 확인. 프론트 검토 대기.
