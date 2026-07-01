import { api } from './client'

export const fetchCarts = () => api.get('/carts').then((res) => res.data.data)

export const updateCartQuantity = ({ itemId, quantity }) =>
  api.put('/carts', { itemId, quantity }).then((res) => res.data)

export const deleteCartItems = (itemIds) =>
  api.delete('/carts', { params: { items: itemIds.join(',') } }).then((res) => res.data)
