import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { client } from '@/api/client'
import type { components } from '@/api/schema'
import { ApiError } from '@/lib/api-error'
import { problemOf } from '@/lib/problem'

export type ExpertProfileRequest = components['schemas']['ExpertProfileRequest']

export type ExpertProfile = components['schemas']['ExpertProfileResponse']

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

/**
 * Ends the session and loads the shop as a new page, so nothing of the user stays in memory. Not a reset and a
 * navigation inside the SPA: the reset would empty `['me']` while a protected page is still on the screen, and its
 * `RequireRole` would send the visitor to the login instead of the shop.
 */
export function useLogout() {
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
    onSuccess: () => window.location.assign('/'),
  })
}

/** The experts an admin can make eligible for an offering (UC-16), sorted by display name. */
export function useExperts() {
  return useQuery({
    queryKey: ['admin', 'experts'],
    queryFn: async () => {
      const { data, response } = await client.GET('/api/admin/experts')
      if (data === undefined) {
        throw new ApiError(
          `Loading the experts failed: ${response.status}`,
          response.status,
        )
      }
      return data
    },
  })
}

/** The current expert's own profile, with their Teams link (UC-14). */
export function useExpertProfile() {
  return useQuery({
    queryKey: ['expert', 'profile'],
    queryFn: async () => {
      const { data, response } = await client.GET('/api/expert/profile')
      // not `error`: a 401 has an empty body, and openapi-fetch then returns `error: undefined`
      if (data === undefined) {
        throw new ApiError(
          `Loading the profile failed: ${response.status}`,
          response.status,
        )
      }
      return data
    },
  })
}

/**
 * Saves the current expert's profile (UC-14 step 4). It invalidates every query: the name and the bio show in the
 * shop, in the expert picker, on cases and in the admin's list of experts. It stays pending until the queries on the
 * screen have reloaded.
 */
export function useSaveExpertProfile() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (profile: ExpertProfileRequest) => {
      const { data, error, response } = await client.PUT(
        '/api/expert/profile',
        { body: profile },
      )
      if (data === undefined) {
        throw new ApiError(
          `Saving the profile failed: ${response.status}`,
          response.status,
          problemOf(response.status, error),
        )
      }
      return data
    },
    onSuccess: () => queryClient.invalidateQueries(),
  })
}

/** The form's values for a saved profile: no link is an empty field, which the form sends as `""`. */
export function toRequest({
  displayName,
  bio,
  teamsLink,
}: ExpertProfile): ExpertProfileRequest {
  return { displayName, bio, teamsLink: teamsLink ?? '' }
}
