import { useQuery } from '@tanstack/react-query'
import { client } from '@/api/client'
import { ApiError } from '@/lib/api-error'

export function useConsultationOfferings() {
  return useQuery({
    queryKey: ['admin', 'consultation-offerings'],
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
