# front-api-client 코드 템플릿 (JavaScript)

새 도메인을 추가할 때 아래 4개 블록을 복제하고 `book`/`books` 부분만 도메인명으로 바꾼다.

## 1) axios 인스턴스 — `src/api/client.js`
파일당 인스턴스는 1개만. JWT는 요청 인터셉터에서 붙인다.

```js
import axios from 'axios'
import { useAuthStore } from '@/stores/useAuthStore'

export const api = axios.create({
  baseURL: '/api',
  timeout: 10_000,
})

// 요청마다 토큰 자동 첨부
api.interceptors.request.use((config) => {
  const token = useAuthStore.getState().token
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 공통 에러 처리 지점 (필요할 때만 확장)
api.interceptors.response.use(
  (res) => res,
  (error) => Promise.reject(error),
)
```

## 2) 요청 함수 — `src/api/books.js`
여기서는 axios만 다루고 React 관련 코드는 넣지 않는다.

```js
import { api } from './client'

export async function fetchBooks() {
  const { data } = await api.get('/books')
  return data
}

export async function fetchBook(id) {
  const { data } = await api.get(`/books/${id}`)
  return data
}

export async function createBook(payload) {
  const { data } = await api.post('/books', payload)
  return data
}
```

## 3) 조회 훅 (useQuery) — `src/hooks/queries/useBooks.js`
컴포넌트는 이 훅만 import 한다.

```js
import { useQuery } from '@tanstack/react-query'
import { fetchBooks, fetchBook } from '@/api/books'

export function useBooks() {
  return useQuery({
    queryKey: ['books'],
    queryFn: fetchBooks,
  })
}

export function useBook(id) {
  return useQuery({
    queryKey: ['books', id],
    queryFn: () => fetchBook(id),
    enabled: Number.isFinite(id), // id가 준비됐을 때만 실행
  })
}
```

## 4) 변경 훅 (useMutation) — `src/hooks/queries/useCreateBook.js`
성공 후 관련 queryKey를 무효화한다.

```js
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { createBook } from '@/api/books'

export function useCreateBook() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload) => createBook(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['books'] })
    },
  })
}
```

## 컴포넌트 사용 예 — `BookList.jsx`
```jsx
import { useBooks } from '@/hooks/queries/useBooks'

function BookList() {
  const { data: books, isPending, isError, error } = useBooks()

  if (isPending) return <p>불러오는 중…</p>
  if (isError) return <p>에러: {error.message}</p>

  return (
    <ul>
      {books.map((b) => (
        <li key={b.id}>{b.title}</li>
      ))}
    </ul>
  )
}
```

## QueryClientProvider (앱 진입부, 최초 1회만) — `src/main.jsx`
앱을 `QueryClientProvider`로 감싼다.

```jsx
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'

const queryClient = new QueryClient()

// <QueryClientProvider client={queryClient}> ... </QueryClientProvider>
```