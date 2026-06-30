import { api } from './client'

export async function login({ userId, passwd }) {
  const { data } = await api.post('/auth/login', { userId, passwd })
  return data.data
}

export async function fetchMe() {
  const { data } = await api.get('/auth/me')
  return data.data
}
