import { useQuery } from '@tanstack/react-query'
import { client } from '@/api/client'

export function useShopOfferings() {
  return useQuery({
    queryKey: ['shop', 'offerings'],
    queryFn: async () => {
      const { data, response } = await client.GET('/api/shop/offerings')
      if (data === undefined) {
        throw new Error(`Loading the offerings failed: ${response.status}`)
      }
      return data
    },
  })
}
