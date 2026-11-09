import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { client } from '@/api/client'
import type { components } from '@/api/schema'
import { ApiError } from '@/lib/api-error'
import { problemOf } from '@/lib/problem'

export type PurchaseConsultationPackageRequest =
  components['schemas']['PurchaseConsultationPackageRequest']

export type CustomerConsultationCase =
  components['schemas']['CustomerConsultationCaseResponse']

// The list's key (task 02) is the detail keys' prefix, so invalidating it invalidates every detail, too.
const customerConsultationCasesKey = ['customer', 'consultation-cases']

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
 * dialog showed a stale offering; the dialog reloads its packages, and the shop when it closes (`BuyConsultationDialog`).
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
  })
}
