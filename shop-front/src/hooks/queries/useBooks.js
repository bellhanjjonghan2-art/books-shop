import { useQuery } from '@tanstack/react-query'
import { fetchBooks, fetchBooksByCategory } from '../../api/books'

export function useBooks() {
  return useQuery({
    queryKey: ['books'],
    queryFn: fetchBooks,
  })
}

export function useBooksByCategory(types, { page = 0, size = 10, orderType = 'new' } = {}) {
  return useQuery({
    queryKey: ['books', 'category', types, page, orderType],
    queryFn: () => fetchBooksByCategory(types, { page, size, orderType }),
  })
}
