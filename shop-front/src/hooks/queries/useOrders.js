import { useMutation, useQuery } from '@tanstack/react-query'
import { createOrder, fetchOrderResult, fetchOrders } from '../../api/orders'

export function useCreateOrder() {
  return useMutation({
    mutationFn: createOrder,
  })
}

export function useOrderResult(orderId) {
  return useQuery({
    queryKey: ['orders', orderId],
    queryFn: () => fetchOrderResult(orderId),
    enabled: !!orderId,
  })
}

export function useOrderList({ period, page, size = 5 }) {
  return useQuery({
    queryKey: ['orders', 'list', period, page, size],
    queryFn: () => fetchOrders({ period, page, size }),
  })
}
