import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { client, dataOrThrow, okOrThrow } from '@/api/client'
import type { components } from '@/api/schema'
import { shopKey } from '@/features/shop/queries'
import { isConflict } from '@/lib/api-error'

export type ConsultationOfferingRequest =
  components['schemas']['ConsultationOfferingRequest']

export type ConsultationOffering =
  components['schemas']['ConsultationOfferingResponse']

/**
 * The form's values for a saved offering. A package keeps its `id`, which the form never shows: it travels with the
 * row, so a save names the packages it keeps (UC-16 step 3). The eligible experts are the ids of the checked boxes.
 */
export function toRequest({
  name,
  description,
  sessionDurationMinutes,
  cancellationDeadlineHours,
  packages,
  eligibleExpertIds,
}: ConsultationOffering): ConsultationOfferingRequest {
  return {
    name,
    description,
    sessionDurationMinutes,
    cancellationDeadlineHours,
    packages: packages.map(({ id, sessionCount, price }) => ({
      id,
      sessionCount,
      price: price.amount,
    })),
    eligibleExpertIds,
  }
}

// The list's key is the detail keys' prefix, so invalidating it invalidates every detail, too.
const consultationOfferingsKey = ['admin', 'consultation-offerings']

export function useConsultationOfferings() {
  return useQuery({
    queryKey: consultationOfferingsKey,
    queryFn: async () =>
      dataOrThrow(
        await client.GET('/api/admin/consultation-offerings'),
        'Loading the consultation offerings',
      ),
  })
}

export function useConsultationOffering(id: string) {
  return useQuery({
    queryKey: [...consultationOfferingsKey, id],
    queryFn: async () =>
      dataOrThrow(
        await client.GET('/api/admin/consultation-offerings/{id}', {
          params: { path: { id } },
        }),
        'Loading the consultation offering',
      ),
  })
}

export function useCreateConsultationOffering() {
  const invalidateOfferings = useInvalidateOfferings()
  return useMutation({
    mutationFn: async (offering: ConsultationOfferingRequest) =>
      dataOrThrow(
        await client.POST('/api/admin/consultation-offerings', {
          body: offering,
        }),
        'Creating the consultation offering',
      ),
    onSuccess: invalidateOfferings,
  })
}

export function useEditConsultationOffering(id: string) {
  const invalidateOfferings = useInvalidateOfferings()
  return useMutation({
    mutationFn: async (offering: ConsultationOfferingRequest) =>
      dataOrThrow(
        await client.PUT('/api/admin/consultation-offerings/{id}', {
          params: { path: { id } },
          body: offering,
        }),
        'Saving the consultation offering',
      ),
    onSuccess: invalidateOfferings,
    // A 409 comes from a stale page, such as an edit of an offering archived meanwhile: reload it.
    onError: (error) => (isConflict(error) ? invalidateOfferings() : undefined),
  })
}

/** A change of an offering's status (UC-16 step 5); delete is separate. */
export type StatusChange = 'publish' | 'unpublish' | 'archive'

const statusChangePaths = {
  publish: '/api/admin/consultation-offerings/{id}/publish',
  unpublish: '/api/admin/consultation-offerings/{id}/unpublish',
  archive: '/api/admin/consultation-offerings/{id}/archive',
} as const

export function useChangeConsultationOfferingStatus(id: string) {
  const invalidateOfferings = useInvalidateOfferings()
  return useMutation({
    mutationFn: async (change: StatusChange) =>
      dataOrThrow(
        await client.POST(statusChangePaths[change], {
          params: { path: { id } },
        }),
        `Changing the consultation offering's status (${change})`,
      ),
    onSuccess: invalidateOfferings,
    // A 409 means the page showed another status than the server has: reload the offering.
    onError: (error) => (isConflict(error) ? invalidateOfferings() : undefined),
  })
}

/**
 * Uploads the offering's image, or replaces it, at once and apart from the form's save (UC-16 step 3). The server
 * detects the type from the file's first bytes; a file it refuses answers 400 with a `detail` for the user.
 */
export function useUploadConsultationOfferingImage(id: string) {
  const invalidateOfferings = useInvalidateOfferings()
  return useMutation({
    mutationFn: async (file: File) => {
      const body = new FormData()
      body.append('file', file)
      return dataOrThrow(
        await client.PUT('/api/admin/consultation-offerings/{id}/image', {
          params: { path: { id } },
          // The contract types the part as a string (format binary), but the browser sends a File in a FormData.
          // openapi-fetch passes a FormData on as it is and sets no JSON content type, so the browser sets the
          // multipart one with its boundary; the CSRF middleware adds its header as on any PUT.
          body: body as unknown as { file: string },
        }),
        "Uploading the offering's image",
      )
    },
    onSuccess: invalidateOfferings,
    // A 409 means the offering was archived meanwhile: reload it.
    onError: (error) => (isConflict(error) ? invalidateOfferings() : undefined),
  })
}

/** Removes the offering's image, at once and apart from the form's save (UC-16 step 3). */
export function useRemoveConsultationOfferingImage(id: string) {
  const invalidateOfferings = useInvalidateOfferings()
  return useMutation({
    mutationFn: async () =>
      dataOrThrow(
        await client.DELETE('/api/admin/consultation-offerings/{id}/image', {
          params: { path: { id } },
        }),
        "Removing the offering's image",
      ),
    onSuccess: invalidateOfferings,
    onError: (error) => (isConflict(error) ? invalidateOfferings() : undefined),
  })
}

/**
 * Deletes a draft. Its page closes afterwards, so its detail isn't fetched again, which would answer 404: it's only
 * marked stale. The list is dropped from the cache, so it opens freshly loaded, without the deleted offering.
 */
export function useDeleteConsultationOffering(id: string) {
  const queryClient = useQueryClient()
  const invalidateOfferings = useInvalidateOfferings()
  return useMutation({
    mutationFn: async () =>
      okOrThrow(
        await client.DELETE('/api/admin/consultation-offerings/{id}', {
          params: { path: { id } },
        }),
        'Deleting the consultation offering',
      ),
    onSuccess: () => {
      queryClient.removeQueries({
        queryKey: consultationOfferingsKey,
        exact: true,
      })
      return Promise.all([
        queryClient.invalidateQueries({
          queryKey: consultationOfferingsKey,
          refetchType: 'none',
        }),
        queryClient.invalidateQueries({ queryKey: shopKey }),
      ])
    },
    onError: (error) => (isConflict(error) ? invalidateOfferings() : undefined),
  })
}

/**
 * Every offering mutation refreshes the admin's offerings, every detail included, and the shop, which shows a
 * published offering's change at once. The mutation stays pending until the queries on the screen have reloaded.
 */
function useInvalidateOfferings() {
  const queryClient = useQueryClient()
  return () =>
    Promise.all([
      queryClient.invalidateQueries({ queryKey: consultationOfferingsKey }),
      queryClient.invalidateQueries({ queryKey: shopKey }),
    ])
}
