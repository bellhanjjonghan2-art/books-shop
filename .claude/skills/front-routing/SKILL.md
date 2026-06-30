---
name: front-routing
description: >
  shop-front에서 새 페이지를 추가하거나 라우팅(페이지 이동) 구조, 레이아웃을 작성/수정할 때 반드시 사용한다.
  react-router v7 기반 라우트 등록, 단일 레이아웃(MainLayout) + Outlet 구조, 페이지 추가 절차를 정의한다.
  "페이지 추가", "새 화면 라우트", "URL 경로", "페이지 이동", "네비게이션", "활성 메뉴 스타일",
  "레이아웃", "헤더 메뉴", "Outlet", router, route, NavLink, Link, navigate 같은 작업이 나오면 이 스킬을 적용한다.
---

# front-routing (react-router v7 규칙)

shop-front는 react-router v7을 **데이터 라우터(`createBrowserRouter`)** 방식으로 쓴다.
import는 모두 `react-router` 패키지에서 한다(v7에서 통합됨, `react-router-dom` 별도 import 금지).
프론트는 순수 JavaScript(.jsx)로 작성한다.

## 레이아웃 구조 (고정)
shop-front는 **단일 루트 레이아웃 `MainLayout`** 을 쓰는 SPA다.
- `MainLayout`은 상단 헤더(전역 메뉴) + `<Outlet/>` 콘텐츠 영역으로 구성된다.
- **모든 페이지는 `MainLayout`의 자식 라우트로 추가**한다. 콘텐츠는 `<Outlet/>` 자리에 갈아끼워진다(SPA).
- 파일 위치: `src/layouts/MainLayout/MainLayout.jsx`

## 라우트 정의 위치 (고정)
- `src/router.jsx` — 라우트 정의를 1곳에 모은다.
- 페이지 컴포넌트는 `src/pages/<PageName>/<PageName>.jsx`.

## 페이지 추가 절차
1. `src/pages/<PageName>/`에 페이지 컴포넌트를 만든다(스타일은 front-ui-component 규칙을 따른다).
2. `src/router.jsx`에서 **MainLayout의 `children` 배열**에 라우트 항목을 추가한다(루트 레벨에 평면으로 추가하지 않는다).
3. 페이지 이동은 아래 링크 규칙대로 한다. `<a href>` 직접 사용 금지.

## 링크 컴포넌트 선택 (중요)
- **`NavLink`를 기본으로 쓴다.** 메뉴/네비게이션처럼 "현재 페이지 여부에 따라 스타일을 다르게 줄 가능성"이 있는 링크는 모두 `NavLink`로 작성한다.
- `Link`는 활성 상태 표시가 전혀 필요 없는 단순 본문 링크에만 쓴다.
- 코드(이벤트 핸들러 등)에서 이동할 때는 `useNavigate()`를 쓴다.
- `NavLink`의 `className`은 **함수 형태**(`({ isActive }) => ...`)로 받아 활성 상태에 따라 클래스를 바꾼다. 스타일은 front-ui-component 규칙(CSS Module + Tailwind v4)을 따른다.
- 홈(`to="/"`) 링크에는 **`end` 속성**을 붙인다. 안 붙이면 모든 하위 경로에서도 활성으로 표시된다.

## 표준 템플릿

`src/router.jsx` — MainLayout(부모) + 페이지(자식) 중첩 구조
```jsx
import { createBrowserRouter } from 'react-router'
import { MainLayout } from '@/layouts/MainLayout/MainLayout'
import { HomePage } from '@/pages/Home/Home'
import { BookListPage } from '@/pages/BookList/BookList'

export const router = createBrowserRouter([
  {
    path: '/',
    element: <MainLayout />,      // 헤더 + <Outlet/> 껍데기
    children: [
      { index: true, element: <HomePage /> },   // 경로 '/'
      { path: 'books', element: <BookListPage /> },
      // 새 페이지는 여기에 자식으로 추가
    ],
  },
])
```

`src/layouts/MainLayout/MainLayout.jsx` — 헤더 메뉴 + Outlet
```jsx
import { NavLink, Outlet } from 'react-router'
import styles from './MainLayout.module.css'

const navClass = ({ isActive }) =>
  `${styles.link} ${isActive ? styles.active : ''}`

export function MainLayout() {
  return (
    <div className={styles.layout}>
      <header className={styles.header}>
        <nav>
          {/* 홈은 end를 붙여 정확히 '/'일 때만 활성 */}
          <NavLink to="/" end className={navClass}>홈</NavLink>
          <NavLink to="/books" className={navClass}>도서 목록</NavLink>
        </nav>
      </header>

      <main className={styles.content}>
        <Outlet />   {/* 자식 라우트(페이지)가 여기로 렌더된다 */}
      </main>
    </div>
  )
}
```

`src/layouts/MainLayout/MainLayout.module.css`
```css
@reference "@/styles/index.css";

.header {
  @apply flex items-center gap-4 border-b px-6 py-3;
}
.link {
  @apply px-2 py-1 text-gray-600;
}
.link.active {
  @apply font-bold text-blue-600;
}
.content {
  @apply p-6;
}
```

`src/main.jsx` (진입부, 최초 1회)
```jsx
import { RouterProvider } from 'react-router'
import { router } from './router'

// <RouterProvider router={router} />
```

## 단순 본문 링크(활성 표시 불필요)
```jsx
import { Link } from 'react-router'

<Link to="/books">도서 목록 보기</Link>
```

## 규칙 요약
- import는 항상 `react-router`에서.
- 라우트는 `src/router.jsx` 한 곳에서만 관리하고, 페이지는 `MainLayout`의 `children`으로 추가한다.
- 레이아웃은 `MainLayout` 하나. 헤더 전역 메뉴 + `<Outlet/>` 콘텐츠 영역 구조를 유지한다.
- 네비게이션 링크는 `NavLink`를 기본으로 쓰고, `className`은 `({ isActive }) => ...` 함수로 활성 스타일을 부여한다(홈은 `end` 필수).
- 활성 표시가 필요 없는 단순 링크에만 `Link`를 쓴다.