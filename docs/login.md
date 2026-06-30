# 로그인 기능 PRD (Product Requirements Document)

| 항목 | 내용 |
|---|---|
| 문서 버전 | v1.0 |
| 기능명 | JWT 기반 로그인 |
| 대상 프로젝트 | books-shop (React 프론트엔드 + Spring Boot 백엔드, Gradle 멀티모듈) |
| 작성 목적 | 클라이언트 에이전트와 서버 에이전트가 동일한 계약(Contract)을 공유하여 병렬 개발 |

---

## 1. 개요

`users` 테이블을 기반으로 한 ID/비밀번호 로그인 기능을 구현한다. 인증은 **JWT(JSON Web Token)** 방식을 사용하며, 서버는 **Spring Security**로 인증 흐름을 처리하고 비밀번호는 **bcrypt**로 단방향 암호화한다. 클라이언트는 **React + Zustand**로 인증 상태를 관리한다.

본 기능의 범위는 **로그인**과 **로그인 상태 관리**이며, 회원가입은 별도 PRD로 분리한다. (단, 로그인 화면에서 회원가입 페이지로 가는 링크는 포함한다.)

### 1.1 범위 (Scope)

| 포함 (In Scope) | 제외 (Out of Scope) |
|---|---|
| 로그인 화면 (독립 컴포넌트) | 회원가입 기능 구현 (링크만 제공) |
| 로그인 API (`POST /api/auth/login`) | 소셜 로그인 (OAuth) |
| JWT 발급 / 검증 | 비밀번호 재설정 / 찾기 |
| Spring Security 인증 필터 | 이메일 인증 |
| Zustand 인증 상태 저장 | Refresh Token 회전 (3.4 참고, 향후 확장) |
| 인증 상태 기반 라우팅 가드 | |

---

## 2. 기술 스택

### 2.1 프론트엔드
- Vite + React 18
- 순수 JavaScript (TypeScript 미사용)
- react-router v7 (라우팅)
- Zustand (persist + immer 미들웨어, 인증 상태 저장)
- TanStack Query (서버 통신, 선택적)
- Tailwind CSS v4 (스타일링)

### 2.2 백엔드
- Spring Boot 3.x
- Spring Security 6.x
- JWT (예: `io.jsonwebtoken:jjwt` 또는 `nimbus-jose-jwt`)
- BCryptPasswordEncoder (Spring Security 내장)
- PostgreSQL

---

## 3. 인증 설계

### 3.1 인증 흐름

```
[사용자] → 로그인 폼 입력 (userId, passwd)
   → [클라이언트] POST /api/auth/login
      → [서버] users 조회 → bcrypt 비밀번호 검증
         → 성공 시 JWT 발급
   → [클라이언트] accessToken을 Zustand에 저장 (persist)
   → 이후 보호된 요청 시 Authorization: Bearer <token> 헤더 첨부
```

### 3.2 JWT 명세

| 항목 | 값 |
|---|---|
| 알고리즘 | HS256 |
| 토큰 타입 | Access Token (Bearer) |
| 만료 시간 | 1시간 (3600초) — 환경변수로 조정 가능 |
| 시크릿 키 | 환경변수 `JWT_SECRET` (256bit 이상, 코드에 하드코딩 금지) |

**Claims 구조**

```json
{
  "sub": "user_id 값",
  "names": "사용자 이름",
  "roles": "USER",
  "iat": 1700000000,
  "exp": 1700003600
}
```

### 3.3 비밀번호 암호화
- 저장: 회원가입 시 `BCryptPasswordEncoder.encode(rawPassword)`로 해시 후 `passwd` 컬럼에 저장 (strength 10 권장)
- 검증: 로그인 시 `BCryptPasswordEncoder.matches(입력값, 저장된 해시)`로 비교
- 평문 비밀번호는 로그/응답/DB 어디에도 남기지 않는다.

### 3.4 토큰 저장 위치 및 보안 고려 (중요)
- 본 PRD에서는 **단순성을 위해 Access Token만** 발급하고, 클라이언트의 Zustand `persist`(localStorage)에 저장한다.
- **트레이드오프:** localStorage 저장은 XSS에 노출될 수 있다. 운영 보안 강화가 필요하면 Access Token은 메모리(zustand persist 제외) + Refresh Token은 httpOnly 쿠키 방식으로 확장한다. → **향후 확장 항목**

---

## 4. 데이터 모델

```sql
create table users (
  user_id     varchar(100)  not null,
  passwd      varchar(200)  not null,   -- bcrypt 해시 저장
  names       varchar(50)   not null,
  birth       varchar(10)   not null,
  gender      varchar(10)   not null,
  phone       varchar(20)   not null,
  email       varchar(100)  not null,
  address     varchar(300)  not null,
  addr_detail varchar(300)  default '',
  postcode    varchar(10)   not null,
  roles       varchar(10)   not null,   -- 예: 'USER', 'ADMIN'
  create_at   timestamp     default null,
  update_at   timestamp     default null,

  primary key (user_id),
  constraint uq_email unique (email)
);
```

**로그인에서 사용하는 필드:** `user_id`(로그인 ID), `passwd`(검증), `names`/`roles`(JWT/응답 구성).

---

## 5. 에이전트 간 인터페이스 계약 (Source of Truth)

> 클라이언트 에이전트와 서버 에이전트는 **이 절을 변경 불가능한 단일 계약**으로 간주한다. 변경이 필요하면 본 PRD를 먼저 갱신한 뒤 양측에 반영한다.

### 5.1 `POST /api/auth/login`

**Request Body**
```json
{
  "userId": "string",
  "passwd": "string"
}
```

**Response 200 (성공)**
```json
{
  "accessToken": "eyJhbGci...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "user": {
    "userId": "hong123",
    "names": "홍길동",
    "roles": "USER"
  }
}
```

**Response 401 (인증 실패)**
```json
{
  "code": "AUTH_FAILED",
  "message": "아이디 또는 비밀번호가 올바르지 않습니다."
}
```

> 보안상 "아이디 없음"과 "비밀번호 틀림"을 구분하지 않고 동일 메시지로 응답한다.

**Response 400 (입력값 오류)**
```json
{
  "code": "INVALID_REQUEST",
  "message": "userId와 passwd는 필수입니다."
}
```

### 5.2 `GET /api/auth/me` (인증 필요)

현재 토큰의 사용자 정보를 반환한다. (앱 진입 시 토큰 유효성 확인 용도)

**Request Header**
```
Authorization: Bearer <accessToken>
```

**Response 200**
```json
{
  "userId": "hong123",
  "names": "홍길동",
  "roles": "USER"
}
```

**Response 401:** 토큰 없음/만료/위변조 시 `{ "code": "TOKEN_INVALID", "message": "..." }`

### 5.3 공통 응답 규칙
- 모든 에러 응답은 `{ "code": string, "message": string }` 형태를 따른다.
- 시간 필드는 ISO-8601(예: `2026-06-24T10:00:00Z`) 사용.
- 성공 응답은 위 명세의 키 이름/타입을 정확히 지킨다.

---

## 6. 서버 (백엔드 에이전트) 요구사항

1. **SecurityFilterChain 설정**
    - 세션 정책: `STATELESS`
    - `POST /api/auth/login` → `permitAll()`
    - `/api/auth/me` 및 그 외 보호 경로 → `authenticated()`
    - CSRF 비활성화 (토큰 기반이므로)
2. **JwtAuthenticationFilter** (OncePerRequestFilter)
    - `Authorization: Bearer` 헤더에서 토큰 추출 → 검증 → SecurityContext에 인증 정보 설정
3. **로그인 서비스**
    - `user_id`로 사용자 조회 → 없으면 401
    - `BCryptPasswordEncoder.matches()`로 비밀번호 검증 → 실패 시 401
    - 성공 시 JWT 발급 후 5.1 형식으로 응답
4. **CORS 설정**
    - 프론트엔드 origin(예: `http://localhost:5173`, 운영 도메인) 허용
    - 허용 헤더에 `Authorization` 포함
5. **예외 처리**
    - 인증 실패/토큰 오류를 공통 에러 포맷(5.3)으로 변환하는 핸들러
6. **민감정보 보호**
    - `passwd`는 응답/로그에 절대 포함 금지
    - `JWT_SECRET`은 환경변수로 주입

---

## 7. 프론트엔드 (클라이언트 에이전트) 요구사항

### 7.1 LoginForm 컴포넌트 (독립 컴포넌트)
- 위치 예시: `src/features/auth/LoginForm.jsx`
- 입력 필드: **userId**, **passwd**(type=password)
- 로그인 버튼 + 하단에 **회원가입 링크** (`react-router` `<Link to="/signup">`)
- 디자인: 심플 (중앙 정렬 카드, Tailwind 기본 톤, 과한 장식 없음)
- 상태 표시: 로딩 중 버튼 비활성화, 에러 메시지 영역

**기본 검증 (클라이언트)**
- userId/passwd 빈 값 → 제출 차단 및 안내 메시지
- 실제 인증 판단은 서버 응답(5.1)에 의존

### 7.2 인증 상태 스토어 (Zustand)
- 위치 예시: `src/stores/authStore.js`
- `persist` + `immer` 미들웨어 사용

```javascript
// 스토어 형태 (가이드)
{
  accessToken: null,
  user: null,            // { userId, names, roles }
  isAuthenticated: false,

  setAuth: ({ accessToken, user }) => { ... },  // 로그인 성공 시
  logout: () => { ... },                        // 토큰/유저 초기화
}
```

- `persist` 저장 키 예: `auth-storage`
- 로그인 성공 → `setAuth` 호출 → `isAuthenticated = true`

### 7.3 API 클라이언트
- 로그인 요청: `POST /api/auth/login` (5.1 계약 준수)
- 공통 요청 인터셉터: `accessToken`이 있으면 `Authorization: Bearer` 헤더 자동 첨부
- 401 응답 시: `logout()` 호출 후 로그인 페이지로 리다이렉트

### 7.4 라우팅 가드
- 보호 라우트는 `isAuthenticated`가 false면 `/login`으로 리다이렉트
- 로그인 성공 후 원래 가려던 경로 또는 기본 페이지로 이동

---

## 8. 수용 기준 (Acceptance Criteria)

### 공통
- [ ] 올바른 ID/PW 입력 시 로그인 성공하고 5.1 형식의 응답을 받는다.
- [ ] 틀린 PW / 없는 ID 입력 시 동일한 401 메시지를 받는다.
- [ ] 발급된 토큰으로 `/api/auth/me` 호출 시 사용자 정보를 받는다.
- [ ] 만료/위변조 토큰으로 보호 API 호출 시 401을 받는다.

### 서버
- [ ] 비밀번호는 bcrypt 해시로만 비교하며 평문 비교가 없다.
- [ ] `/api/auth/login`은 미인증 상태에서 접근 가능하다.
- [ ] 응답/로그 어디에도 `passwd`가 노출되지 않는다.
- [ ] CORS로 프론트 origin에서의 요청이 정상 처리된다.

### 프론트
- [ ] LoginForm이 독립 컴포넌트로 분리되어 있다.
- [ ] userId/passwd 입력 필드와 회원가입 링크가 존재한다.
- [ ] 로그인 성공 시 토큰/유저가 Zustand에 저장되고 persist된다.
- [ ] 새로고침 후에도 로그인 상태가 유지된다.
- [ ] 로그아웃 시 토큰/유저 상태가 초기화된다.

---

## 9. 향후 확장 (Backlog)
- Refresh Token 도입 및 토큰 회전 (httpOnly 쿠키 방식)
- 로그인 실패 횟수 제한 / 계정 잠금
- 회원가입 기능 PRD 연동
- `roles` 기반 권한 분기 (ADMIN 등)
- `update_at` 기록 (마지막 로그인 시각 등 운영 정책에 따라)