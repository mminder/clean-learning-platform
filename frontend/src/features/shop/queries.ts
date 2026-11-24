import { useQuery } from '@tanstack/react-query'
import { client, dataOrThrow } from '@/api/client'

/** The prefix of all shop queries. */
export const shopKey = ['shop']

export function useShopOfferings() {
  return useQuery({
    queryKey: [...shopKey, 'offerings'],
    queryFn: async () =>
      dataOrThrow(
        await client.GET('/api/shop/offerings'),
        'Loading the offerings',
      ),
  })
}

export function useShopOffering(id: string) {
  return useQuery({
    queryKey: shopOfferingKey(id),
    queryFn: () => fetchShopOffering(id),
  })
}

export function shopOfferingKey(id: string) {
  return [...shopKey, 'offerings', id]
}

/** One offering of the shop. The buy dialog also reloads it without the cache (`BuyConsultationDialog`). */
export async function fetchShopOffering(id: string) {
  return dataOrThrow(
    await client.GET('/api/shop/offerings/{id}', {
      params: { path: { id } },
    }),
    'Loading the offering',
  )
}
