import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { client } from '@/api/client'
import type { components } from '@/api/schema'
import { ApiError } from '@/lib/api-error'
import { problemOf } from '@/lib/problem'

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
    queryFn: async () => {
      const { data, response } = await client.GET(
        '/api/admin/consultation-offerings',
      )
      // not `error`: a 401 has an empty body, and openapi-fetch then returns `error: undefined`
      if (data === undefined) {
        throw new ApiError(
          `Loading the consultation offerings failed: ${response.status}`,
          response.status,
        )
      }
      return data
    },
  })
}

export function useConsultationOffering(id: string) {
  return useQuery({
    queryKey: [...consultationOfferingsKey, id],
    queryFn: async () => {
      const { data, response } = await client.GET(
        '/api/admin/consultation-offerings/{id}',
        { params: { path: { id } } },
      )
      if (data === undefined) {
        throw new ApiError(
          `Loading the consultation offering failed: ${response.status}`,
          response.status,
        )
      }
      return data
    },
  })
}

export function useCreateConsultationOffering() {
  const invalidateOfferings = useInvalidateOfferings()
  return useMutation({
    mutationFn: async (offering: ConsultationOfferingRequest) => {
      const { data, error, response } = await client.POST(
        '/api/admin/consultation-offerings',
        { body: offering },
      )
      if (data === undefined) {
        throw new ApiError(
          `Creating the consultation offering failed: ${response.status}`,
          response.status,
          problemOf(response.status, error),
        )
      }
      return data
    },
    onSuccess: invalidateOfferings,
  })
}

export function useEditConsultationOffering(id: string) {
  const invalidateOfferings = useInvalidateOfferings()
  return useMutation({
    mutationFn: async (offering: ConsultationOfferingRequest) => {
      const { data, error, response } = await client.PUT(
        '/api/admin/consultation-offerings/{id}',
        { params: { path: { id } }, body: offering },
      )
      if (data === undefined) {
        throw new ApiError(
          `Saving the consultation offering failed: ${response.status}`,
          response.status,
          problemOf(response.status, error),
        )
      }
      return data
    },
    onSuccess: invalidateOfferings,
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
      queryClient.invalidateQueries({ queryKey: ['shop'] }),
    ])
}
