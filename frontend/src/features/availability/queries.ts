import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { client } from '@/api/client'
import type { components } from '@/api/schema'
import { ApiError } from '@/lib/api-error'
import { problemOf } from '@/lib/problem'

export type AvailabilityRequest = components['schemas']['AvailabilityRequest']

export type Availability = components['schemas']['AvailabilityResponse']

const availabilityKey = ['expert', 'availability']

/** The current expert's own availability: their dates from today on, and their daily time ranges (UC-13). */
export function useAvailability() {
  return useQuery({
    queryKey: availabilityKey,
    queryFn: async () => {
      const { data, response } = await client.GET('/api/expert/availability')
      // not `error`: a 401 has an empty body, and openapi-fetch then returns `error: undefined`
      if (data === undefined) {
        throw new ApiError(
          `Loading the availability failed: ${response.status}`,
          response.status,
        )
      }
      return data
    },
  })
}

/**
 * Saves the current expert's availability (UC-13 step 4). The answer is the saved availability, which goes into the
 * cache as it is: there's nothing else to reload.
 */
export function useSaveAvailability() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (availability: AvailabilityRequest) => {
      const { data, error, response } = await client.PUT(
        '/api/expert/availability',
        { body: availability },
      )
      if (data === undefined) {
        throw new ApiError(
          `Saving the availability failed: ${response.status}`,
          response.status,
          problemOf(response.status, error),
        )
      }
      return data
    },
    onSuccess: (saved) => queryClient.setQueryData(availabilityKey, saved),
  })
}

/**
 * The form's values for a saved availability. A time input takes `HH:mm`, so a time is cut to its first five
 * characters, whether the API writes `09:00` or `09:00:00`.
 */
export function toRequest({
  dates,
  timeRanges,
}: Availability): AvailabilityRequest {
  return {
    dates,
    timeRanges: timeRanges.map(({ start, end }) => ({
      start: start.slice(0, 5),
      end: end.slice(0, 5),
    })),
  }
}
