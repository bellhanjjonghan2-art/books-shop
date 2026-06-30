---
name: front-ui-component
description: >
   shop-front에서 새 컴포넌트나 화면을 만들거나 스타일을 작성/수정할 때 반드시 사용한다.
   CSS Module(*.module.css)과 Tailwind CSS v4를 함께 쓰는 규칙, 컴포넌트 폴더 구조, 스타일 작성 컨벤션을 정의한다.
   "컴포넌트 만들기", "화면 추가", "스타일", "버튼/카드/레이아웃", CSS, Tailwind, className 같은 작업이
   나오면 이 스킬을 적용한다.
---

# front-ui-component (컴포넌트 / 스타일 규칙)

프론트는 순수 JavaScript(.jsx)로 작성한다.

## 컴포넌트 폴더 구조 (고정)
컴포넌트 1개 = 폴더 1개. 스타일은 같은 폴더의 `*.module.css`로 분리한다.
```
src/components/Button/
├─ Button.jsx
└─ Button.module.css
```
- 함수형 컴포넌트로 작성한다.
- 페이지 단위 컴포넌트는 `src/pages/<PageName>/`에 동일 구조로 둔다.

## 스타일 규칙 (중요)
1. **인라인 스타일(`style={{}}`) 금지.** styled-components류도 쓰지 않는다.
2. 단순 레이아웃/간격/색은 JSX에서 **Tailwind 유틸리티 className**으로 처리한다.
3. 복잡하거나 재사용되는 스타일, 의사선택자(:hover 등)는 **`*.module.css`** 로 분리한다.
4. `*.module.css` 안에서 Tailwind 유틸리티(`@apply`)를 쓰려면 **파일 최상단에 `@reference` 지시어**를 넣어야 한다.
   (Tailwind v4는 모듈 스코프 파일이 메인 CSS 컨텍스트를 모르기 때문에 필수다.)
5. Tailwind v4는 `tailwind.config.js` 없이 CSS 우선 설정을 쓴다. 설정이 필요하면 메인 CSS의 `@theme` 블록에 둔다.

## 템플릿
`Button.module.css`
```css
@reference "@/styles/index.css";  /* @import "tailwindcss"가 있는 진입 CSS 경로 */

.button {
  @apply inline-flex items-center justify-center rounded-md px-4 py-2 font-medium;
  @apply transition-colors;
}

.button:hover {
  @apply opacity-90;
}
```

`Button.jsx`
```jsx
import styles from './Button.module.css'

export function Button({ children, onClick }) {
   return (
           <button className={styles.button} onClick={onClick}>
              {children}
           </button>
   )
}
```

## 판단 기준 (Tailwind vs CSS Module)
- 한두 줄 유틸리티로 끝나면 → JSX className에 Tailwind.
- 상태별 스타일·여러 셀렉터·재사용 → CSS Module로 분리.
- 둘을 한 요소에서 섞지 말고, 한 요소의 스타일은 한쪽 방식으로 통일한다.