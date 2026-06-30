# books-shop — Agent Guide

이 파일은 books-shop 저장소 작업 시 기본 컨텍스트다. 작업 전 항상 읽고 따른다.

## Why
books-shop은 도서 판매 SPA다. 하나의 저장소에 프론트(shop-front)와 백엔드(shop-back)를 멀티 모듈로 둔다.

## 프로젝트 구조
- `shop-front/` — React 프론트엔드(Vite). 화면·상태·API 연동.
- `shop-back/`  — Spring Boot 백엔드(Gradle, Groovy DSL). REST API 제공.
- 통합 빌드: 루트에서 `./gradlew build` (node-gradle로 프론트까지 빌드).

## 기술 스택
- 백엔드: Java 21, Spring Boot, Spring Security, JWT(jjwt), org.json
- 프론트: React(JavaScript, .js/.jsx), Vite, react-router v7, Tailwind CSS v4 + CSS Module
- 프론트 API 연동: axios + TanStack Query(react query)
- 전역 상태: zustand(persist+immer) — 특히 인증/인가 상태 저장에 사용
- 모든 라이브러리는 최신 안정 버전 사용(추측·임의 고정 금지)

## 핵심 규약
- 언어: 백엔드 Java, 프론트 React(JavaScript). 사용자와의 대화는 한국어.
- 인증/인가: JWT 기반. 토큰은 프론트에서 zustand로 보관한다.
- API: 모든 엔드포인트는 RESTful 규약을 따른다. 기본 경로는 `/api`, 개발 시 Vite proxy로 백엔드에 전달.
- 레이아웃: shop-front는 단일 루트 레이아웃 MainLayout(헤더 전역 메뉴 + `<Outlet/>` 콘텐츠 영역) SPA다.
  모든 페이지는 MainLayout의 자식 라우트로 추가한다.

## 항상 지킬 행동 규칙
- 추측해서 설정/구현하지 않는다. 불확실하면 멈추고 확인받는다.
- 요청·문서에 명시된 범위만 구현한다. 임의로 기능·파일·라이브러리를 늘리지 않는다.
- Git 작업(커밋·푸시·브랜치 등)은 명시적으로 요청받았을 때만 한다.
- 물리적 DB 작업(실제 생성·연결·스키마 적용·데이터 조작)은 하지 않는다.

## 작업별 규칙은 스킬을 따른다
세부 절차는 `.claude/skills/`의 스킬에 있다. 해당 작업 시 그 스킬을 따른다.
- API 연동(axios / react query) → front-api-client
- 전역 상태(zustand) → front-state-store
- 컴포넌트 / 스타일(CSS Module + Tailwind) → front-ui-component
- 라우팅 / 페이지 / 레이아웃·Outlet(react-router) → front-routing
- API 엔드포인트 설계(컨트롤러, RESTful URL, HTTP 메서드, 수정 시 PUT) → backend-api-conventions
- 공통 응답·에러 응답 포맷, 전역 예외 처리 → backend-api-response
- 백엔드 코드 작성 스타일(주석 등) → backend-coding-style
- JPA 영속성 계층(엔티티·리포지토리·쿼리·DTO, Auditing) → backend-jpa-persistence