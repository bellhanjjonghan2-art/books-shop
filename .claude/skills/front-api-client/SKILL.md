---
name: front-api-client
description: >
  shop-front에서 백엔드 API를 호출하거나 데이터를 가져오는 코드를 작성/수정할 때 반드시 사용한다.
  axios 인스턴스 규칙과 TanStack Query v5(useQuery/useMutation) 작성 패턴을 정의한다.
  "API 연동", "데이터 가져오기", "목록 조회", "등록/수정/삭제 요청", axios, fetch, react-query,
  useQuery, useMutation 같은 작업이 나오면 이 스킬을 적용한다.
---

# front-api-client (프론트 API 연동 규칙)

shop-front의 모든 서버 통신은 아래 규칙을 따른다. **컴포넌트에서 axios를 직접 호출하지 않는다.**
항상 `src/api`의 axios 인스턴스 + `src/hooks/queries`의 React Query 훅을 거친다.
프론트는 순수 JavaScript(.js/.jsx)로 작성한다.

## 레이어 구조 (고정)
- `src/api/client.js` — axios 인스턴스 1개만 둔다. `baseURL: '/api'`. JWT 토큰을 요청 인터셉터에서 붙인다.
- `src/api/<도메인>.js` — 순수 요청 함수(예: `fetchBooks`, `createBook`).
- `src/hooks/queries/use<도메인>.js` — React Query 훅. 컴포넌트는 이 훅만 import 한다.

## 핵심 규칙
1. 컴포넌트 → React Query 훅 → 요청 함수 → axios 인스턴스. 이 순서를 건너뛰지 않는다.
2. 조회는 `useQuery`, 변경(생성/수정/삭제)은 `useMutation`을 쓴다.
3. TanStack Query v5는 **객체 인자 문법만** 허용된다(`useQuery({ queryKey, queryFn })`). 배열 인자 옛 문법 금지.
4. `queryKey`는 배열로 작성하고 도메인명으로 시작한다(예: `['books']`, `['books', id]`).
5. 변경 성공 후에는 관련 `queryKey`를 `invalidateQueries`로 무효화해 캐시를 갱신한다.
6. 로딩/에러 표시는 훅이 돌려주는 `isPending`, `isError`, `error`를 사용한다. 수동 useState 금지.
7. baseURL이 `/api`이므로 요청 경로에 `/api`를 다시 붙이지 않는다(`api.get('/books')`).

## 코드 템플릿
axios 인스턴스, 요청 함수, useQuery/useMutation 훅의 표준 템플릿은 `examples.md`를 읽고 그대로 따른다.
새 도메인을 추가할 때는 examples.md의 4개 블록(client / 요청함수 / query 훅 / mutation 훅)을 복제해 도메인명만 바꾼다.