import { api } from './client'

export const confirmPayment = ({ paymentKey, orderId, amount }) =>
  api.post('/payments/confirm', { paymentKey, orderId, amount }).then((res) => res.data.data)
