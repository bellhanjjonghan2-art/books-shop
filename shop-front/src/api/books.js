import { api } from './client'

export const fetchBooks = () => api.get('/books').then((res) => res.data.data)

export const fetchBookDetail = (bookId, page = 0, size = 10) =>
  api.get(`/books/${bookId}`, { params: { page, size } }).then((res) => res.data.data)

export const fetchBooksByCategory = (types, { page = 0, size = 10, orderType = 'new' } = {}) =>
  api
    .get(`/books/category/${types}`, { params: { page, size, orderType } })
    .then((res) => res.data.data)

export const fetchBooksByIds = (bookIds) =>
  api.get('/books', { params: { bookIds: bookIds.join(',') } }).then((res) => res.data.data)
