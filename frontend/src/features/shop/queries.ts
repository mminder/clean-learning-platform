import { useQuery } from '@tanstack/react-query'
import { client } from '@/api/client'
import { ApiError } from '@/lib/api-error'

export function useShopOfferings() {
  return useQuery({
    queryKey: ['shop', 'offerings'],
    queryFn: async () => {
      const { data, response } = await client.GET('/api/shop/offerings')
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
    queryKey: ['shop', 'offerings', id],
    queryFn: async () => {
      const { data, response } = await client.GET('/api/shop/offerings/{id}', {
        params: { path: { id } },
      })
      if (data === undefined) {
        throw new ApiError(
          `Loading the offering failed: ${response.status}`,
          response.status,
        )
      }
      return data
    },
  })
}
