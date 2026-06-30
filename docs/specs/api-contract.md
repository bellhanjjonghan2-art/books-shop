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
