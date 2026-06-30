import { useQuery } from '@tanstack/react-query'
import { fetchBookDetail } from '../../api/books'

export function useBookDetail(bookId, page = 0) {
  return useQuery({
    queryKey: ['bookDetail', bookId, page],
    queryFn: () => fetchBookDetail(bookId, page, 10),
    enabled: !!bookId,
  })
}
