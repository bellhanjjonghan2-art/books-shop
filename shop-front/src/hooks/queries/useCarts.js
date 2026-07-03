import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { addCartItem, deleteCartItems, fetchCarts, updateCartQuantity } from '../../api/carts'

export function useCarts() {
  return useQuery({
    queryKey: ['carts'],
    queryFn: fetchCarts,
  })
}

export function useUpdateCartQuantity() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: updateCartQuantity,
    onSuccess: (data) => {
      if (data.code !== 200) {
        alert(data.message)
        return
      }
      queryClient.invalidateQueries({ queryKey: ['carts'] })
    },
  })
}

export function useAddCartItem() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: addCartItem,
    onSuccess: (data) => {
      if (data.code !== 200) {
        alert(data.message)
        return
      }
      queryClient.invalidateQueries({ queryKey: ['carts'] })
    },
  })
}

export function useDeleteCartItems() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: deleteCartItems,
    onSuccess: (data) => {
      if (data.code !== 200) {
        alert(data.message)
        return
      }
      queryClient.invalidateQueries({ queryKey: ['carts'] })
    },
  })
}
