import { api } from './client'

export const createOrder = ({ items, delivery }) =>
  api.post('/orders', { items, delivery }).then((res) => res.data.data)

export const fetchOrderResult = (orderId) =>
  api.get(`/orders/${orderId}`).then((res) => res.data.data)
