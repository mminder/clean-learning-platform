import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { client } from '@/api/client'
import { ApiError } from '@/lib/api-error'

/** The logged-in user, or `null` for a visitor: a 401 from `GET /api/me` is no error. */
export function useMe() {
  return useQuery({
    queryKey: ['me'],
    queryFn: async () => {
      const { data, response } = await client.GET('/api/me')
      if (response.status === 401) {
        return null
      }
      // not `error`: a 401 has an empty body, and openapi-fetch then returns `error: undefined`
      if (data === undefined) {
        throw new ApiError(
          `Loading the current user failed: ${response.status}`,
          response.status,
        )
      }
      return data
    },
  })
}

export function usePocUsers() {
  return useQuery({
    queryKey: ['poc', 'users'],
    queryFn: async () => {
      const { data, response } = await client.GET('/api/poc/users')
      if (data === undefined) {
        throw new ApiError(
          `Loading the users failed: ${response.status}`,
          response.status,
        )
      }
      return data
    },
  })
}

export function useLogin() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (userId: string) => {
      const { data, response } = await client.POST('/api/poc/login', {
        body: { userId },
      })
      if (data === undefined) {
        throw new ApiError(
          `Logging in failed: ${response.status}`,
          response.status,
        )
      }
      return data
    },
    // No data of the visitor or the previous user stays; what's on the screen loads again for the new user.
    onSuccess: () => queryClient.resetQueries(),
  })
}

export function useLogout() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async () => {
      const { response } = await client.POST('/api/logout')
      if (!response.ok) {
        throw new ApiError(
          `Logging out failed: ${response.status}`,
          response.status,
        )
      }
    },
    // No data of the user stays; what's on the screen loads again for the visitor.
    onSuccess: () => queryClient.resetQueries(),
  })
}
