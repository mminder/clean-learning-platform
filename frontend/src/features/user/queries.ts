import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { client, dataOrThrow, okOrThrow } from '@/api/client'
import type { components } from '@/api/schema'

export type ExpertProfileRequest = components['schemas']['ExpertProfileRequest']

export type ExpertProfile = components['schemas']['ExpertProfileResponse']

/** The logged-in user, or `null` for a visitor: a 401 from `GET /api/me` is no error. */
export function useMe() {
  return useQuery({
    queryKey: ['me'],
    queryFn: async () => {
      const result = await client.GET('/api/me')
      if (result.response.status === 401) {
        return null
      }
      return dataOrThrow(result, 'Loading the current user')
    },
  })
}

export function usePocUsers() {
  return useQuery({
    queryKey: ['poc', 'users'],
    queryFn: async () =>
      dataOrThrow(await client.GET('/api/poc/users'), 'Loading the users'),
  })
}

export function useLogin() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (userId: string) =>
      dataOrThrow(
        await client.POST('/api/poc/login', {
          body: { userId },
        }),
        'Logging in',
      ),
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
    mutationFn: async () =>
      okOrThrow(await client.POST('/api/logout'), 'Logging out'),
    onSuccess: () => window.location.assign('/'),
  })
}

/** The experts an admin can make eligible for an offering (UC-16), sorted by display name. */
export function useExperts() {
  return useQuery({
    queryKey: ['admin', 'experts'],
    queryFn: async () =>
      dataOrThrow(
        await client.GET('/api/admin/experts'),
        'Loading the experts',
      ),
  })
}

/** The current expert's own profile, with their Teams link (UC-14). */
export function useExpertProfile() {
  return useQuery({
    queryKey: ['expert', 'profile'],
    queryFn: async () =>
      dataOrThrow(
        await client.GET('/api/expert/profile'),
        'Loading the profile',
      ),
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
    mutationFn: async (profile: ExpertProfileRequest) =>
      dataOrThrow(
        await client.PUT('/api/expert/profile', { body: profile }),
        'Saving the profile',
      ),
    onSuccess: () => queryClient.invalidateQueries(),
  })
}

/**
 * Uploads the current expert's photo, or replaces it, at once and apart from the form's save (UC-14). The server
 * detects the type from the file's first bytes; a file it refuses answers 400 with a `detail` for the user. It
 * invalidates every query, as a save does: the photo shows in the shop, in the expert picker and on cases.
 */
export function useUploadExpertPhoto() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (file: File) => {
      const body = new FormData()
      body.append('file', file)
      return dataOrThrow(
        await client.PUT(
          '/api/expert/profile/photo',
          // The contract types the part as a string (format binary), but the browser sends a File in a FormData, as
          // for the offering's image (useUploadConsultationOfferingImage).
          { body: body as unknown as { file: string } },
        ),
        'Uploading the photo',
      )
    },
    onSuccess: () => queryClient.invalidateQueries(),
  })
}

/** Removes the current expert's photo, at once and apart from the form's save (UC-14). */
export function useRemoveExpertPhoto() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async () =>
      dataOrThrow(
        await client.DELETE('/api/expert/profile/photo'),
        'Removing the photo',
      ),
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
