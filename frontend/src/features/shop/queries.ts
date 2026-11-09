import { useQuery } from '@tanstack/react-query'
import { client } from '@/api/client'
import { ApiError } from '@/lib/api-error'

export function useShopOfferings() {
  return useQuery({
    queryKey: ['shop', 'offerings'],
    queryFn: async () => {
      const { data, response } = await client.GET('/api/shop/offerings')
      // not `error`: a 401 has an empty body, and openapi-fetch then returns `error: undefined`
      if (data === undefined) {
        throw new ApiError(
          `Loading the offerings failed: ${response.status}`,
          response.status,
        )
      }
      return data
    },
  })
}

export function useShopOffering(id: string) {
  return useQuery({
    queryKey: shopOfferingKey(id),
    queryFn: () => fetchShopOffering(id),
  })
}

export function shopOfferingKey(id: string) {
  return ['shop', 'offerings', id]
}

/** One offering of the shop. The buy dialog also reloads it without the cache (`BuyConsultationDialog`). */
export async function fetchShopOffering(id: string) {
  const { data, response } = await client.GET('/api/shop/offerings/{id}', {
    params: { path: { id } },
  })
  // not `error`: a 401 has an empty body, and openapi-fetch then returns `error: undefined`
  if (data === undefined) {
    throw new ApiError(
      `Loading the offering failed: ${response.status}`,
      response.status,
    )
  }
  return data
}
