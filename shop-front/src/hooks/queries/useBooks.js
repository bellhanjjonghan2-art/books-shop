import { useQuery } from '@tanstack/react-query'
import { fetchBooks, fetchBooksByCategory, fetchBooksByIds } from '../../api/books'

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

export function useBooksByIds(bookIds) {
  return useQuery({
    queryKey: ['books', 'byIds', bookIds],
    queryFn: () => fetchBooksByIds(bookIds),
    enabled: bookIds.length > 0,
  })
}
