import { useMutation, useQuery } from '@tanstack/react-query'
import { createOrder, fetchOrderResult } from '../../api/orders'

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
