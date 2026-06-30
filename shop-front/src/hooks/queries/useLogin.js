import { useMutation } from '@tanstack/react-query'
import { login } from '../../api/auth'
import { useAuthStore } from '../../stores/useAuthStore'

export function useLogin() {
  const setAuth = useAuthStore((state) => state.setAuth)

  return useMutation({
    mutationFn: (payload) => login(payload),
    onSuccess: (data) => {
      setAuth({ accessToken: data.accessToken, user: data.user })
    },
  })
}
