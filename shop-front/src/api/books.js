import { api } from './client'

export const fetchBooks = () => api.get('/books').then((res) => res.data.data)
