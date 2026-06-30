import { api } from './client'

export const fetchBooks = () => api.get('/books').then((res) => res.data.data)

export const fetchBookDetail = (bookId, page = 0, size = 10) =>
  api.get(`/books/${bookId}`, { params: { page, size } }).then((res) => res.data.data)
