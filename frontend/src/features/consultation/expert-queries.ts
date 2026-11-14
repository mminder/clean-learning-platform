import { useQuery } from '@tanstack/react-query'
import { client } from '@/api/client'
import type { components } from '@/api/schema'
import { ApiError } from '@/lib/api-error'

export type ExpertConsultationCaseSummary =
  components['schemas']['ExpertConsultationCaseSummaryResponse']

// The list's key is the prefix of the detail keys, as the customer's is.
const expertConsultationCasesKey = ['expert', 'consultation-cases']

/** The cases the current user is the assigned expert of, in the order of My cases, each with its group (UC-11). */
export function useExpertConsultationCases() {
  return useQuery({
    queryKey: expertConsultationCasesKey,
    queryFn: async () => {
      const { data, response } = await client.GET(
        '/api/expert/consultation-cases',
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

/** One case of the current user as its assigned expert (UC-11 step 4). Another expert's case answers 404, like an unknown one. */
export function useExpertConsultationCase(id: string) {
  return useQuery({
    queryKey: [...expertConsultationCasesKey, id],
    queryFn: async () => {
      const { data, response } = await client.GET(
        '/api/expert/consultation-cases/{id}',
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
