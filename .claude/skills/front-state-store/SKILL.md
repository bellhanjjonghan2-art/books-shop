---
name: front-state-store
description: >
  shop-front에서 전역 상태(스토어)를 추가하거나 수정할 때 반드시 사용한다.
  zustand 스토어를 persist(새로고침 유지) + immer(불변성 간소화) 미들웨어 조합으로 만드는 표준 패턴을 정의한다.
  "전역 상태", "상태 공유", "로그인 상태", "장바구니", "스토어 추가", zustand, store, persist, immer 같은
  작업이 나오면 이 스킬을 적용한다. 컴포넌트 로컬 상태(useState)로 충분한 경우에는 스토어를 만들지 않는다.
---

# front-state-store (zustand 스토어 규칙)

shop-front의 전역 상태는 모두 zustand 스토어로 만들고, **persist + immer 미들웨어를 함께 적용**한다.
프론트는 순수 JavaScript(.js)로 작성한다.

## 위치/네이밍 (고정)
- 파일: `src/stores/use<이름>Store.js` (예: `useAuthStore.js`, `useCartStore.js`)
- 훅 이름: `use<이름>Store`
- persist의 `name`(저장 키): `<이름>-storage` (예: `auth-storage`)

## 핵심 규칙
1. `create()(...)` 형태로 작성한다(zustand v5 문법).
2. 미들웨어 적용 순서는 바깥부터 `persist(immer(...))`로 고정한다.
3. immer가 적용되므로 `set` 안에서 상태를 **직접 변형(mutate)** 한다. 펼침연산자로 새 객체를 만들 필요가 없다.
4. import 경로: `persist`는 `zustand/middleware`, `immer`는 `zustand/middleware/immer`에서 가져온다.
5. 컴포넌트에서는 **셀렉터로 필요한 값만 구독**한다(`useCartStore((s) => s.items)`). 스토어 전체를 통째로 구독하지 않는다.
6. 민감 정보를 영구 저장할 때 주의한다. 토큰처럼 보안 민감 값은 저장 범위를 `partialize`로 최소화한다.

## 표준 템플릿
```js
import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import { immer } from 'zustand/middleware/immer'

export const useCartStore = create()(
  persist(
    immer((set) => ({
      items: [],
      addItem: (item) =>
        set((state) => {
          state.items.push(item) // immer 덕분에 직접 변형 가능
        }),
      removeItem: (id) =>
        set((state) => {
          state.items = state.items.filter((it) => it.id !== id)
        }),
      clear: () =>
        set((state) => {
          state.items = []
        }),
    })),
    {
      name: 'cart-storage',
      // 일부만 저장하려면 partialize 사용
      // partialize: (state) => ({ items: state.items }),
    },
  ),
)
```

## 컴포넌트 사용 예 (셀렉터 구독)
```jsx
const items = useCartStore((s) => s.items)
const addItem = useCartStore((s) => s.addItem)
```