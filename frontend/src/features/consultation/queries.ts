import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { client } from '@/api/client'
import type { components } from '@/api/schema'
import { ApiError, isConflict, isNotFound } from '@/lib/api-error'
import { problemOf } from '@/lib/problem'

export type PurchaseConsultationPackageRequest =
  components['schemas']['PurchaseConsultationPackageRequest']

export type CustomerConsultationCase =
  components['schemas']['CustomerConsultationCaseResponse']

export type CustomerConsultationCaseSummary =
  components['schemas']['CustomerConsultationCaseSummaryResponse']

// The list's key is the detail keys' prefix, so invalidating it invalidates every detail, too.
const customerConsultationCasesKey = ['customer', 'consultation-cases']

/** The customer's own cases, the oldest purchase first (UC-05's landing page). */
export function useCustomerConsultationCases() {
  return useQuery({
    queryKey: customerConsultationCasesKey,
    queryFn: async () => {
      const { data, response } = await client.GET(
        '/api/customer/consultation-cases',
      )
      // not `error`: a 401 has an empty body, and openapi-fetch then returns `error: undefined`
      if (data === undefined) {
        throw new ApiError(
          `Loading the cases failed: ${response.status}`,
          response.status,
        )
      }
      return data
    },
  })
}

/** One of the customer's cases (UC-05). Another customer's case answers 404, like an unknown one. */
export function useCustomerConsultationCase(id: string) {
  return useQuery({
    queryKey: [...customerConsultationCasesKey, id],
    queryFn: async () => {
      const { data, response } = await client.GET(
        '/api/customer/consultation-cases/{id}',
        { params: { path: { id } } },
      )
      // not `error`: a 401 has an empty body, and openapi-fetch then returns `error: undefined`
      if (data === undefined) {
        throw new ApiError(
          `Loading the case failed: ${response.status}`,
          response.status,
        )
      }
      return data
    },
  })
}

/**
 * Buys a package at the price and session count the dialog's confirmation stated (UC-03). A 409 or 404 means the
 * dialog showed a stale offering, so the shop reloads, the dialog's packages included; the mutation fails only after
 * that, so the dialog then shows the current packages.
 */
export function usePurchaseConsultationPackage() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (purchase: PurchaseConsultationPackageRequest) => {
      const { data, error, response } = await client.POST(
        '/api/customer/consultation-cases',
        { body: purchase },
      )
      if (data === undefined) {
        throw new ApiError(
          `Buying the package failed: ${response.status}`,
          response.status,
          problemOf(response.status, error),
        )
      }
      return data
    },
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: customerConsultationCasesKey }),
    onError: (error) =>
      isConflict(error) || isNotFound(error)
        ? queryClient.invalidateQueries({ queryKey: ['shop'] })
        : undefined,
  })
}
