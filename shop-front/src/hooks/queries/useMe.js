import { useQuery } from '@tanstack/react-query'
import { fetchMe } from '../../api/auth'
import { useAuthStore } from '../../stores/useAuthStore'

export function useMe() {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated)

  return useQuery({
    queryKey: ['auth', 'me'],
    queryFn: fetchMe,
    enabled: isAuthenticated,
    staleTime: Infinity,
    retry: false,
  })
}
