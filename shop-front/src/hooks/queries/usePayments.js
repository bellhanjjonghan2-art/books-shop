import { useMutation } from '@tanstack/react-query'
import { confirmPayment } from '../../api/payments'

export function useConfirmPayment() {
  return useMutation({
    mutationFn: confirmPayment,
  })
}
