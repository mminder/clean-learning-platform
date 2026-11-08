import { describe, expect, it, vi } from 'vitest'
import { ApiError } from '@/lib/api-error'
import { createQueryClient } from './queryClient'

describe('createQueryClient', () => {
  function setUp() {
    const queryClient = createQueryClient()
    const fetchMe = vi.fn(() => Promise.resolve({ name: 'Admin 1' }))
    return { queryClient, fetchMe }
  }

  it('refetches the current user after a 401 from a query', async () => {
    const { queryClient, fetchMe } = setUp()
    await queryClient.fetchQuery({ queryKey: ['me'], queryFn: fetchMe })
    fetchMe.mockClear()

    await queryClient
      .fetchQuery({
        queryKey: ['admin', 'consultation-offerings'],
        queryFn: () => Promise.reject(new ApiError('expired', 401)),
      })
      .catch(() => undefined)

    await vi.waitFor(() => expect(fetchMe).toHaveBeenCalledTimes(1))
  })

  it('refetches the current user after a 401 from a mutation', async () => {
    const { queryClient, fetchMe } = setUp()
    await queryClient.fetchQuery({ queryKey: ['me'], queryFn: fetchMe })
    fetchMe.mockClear()

    await queryClient
      .getMutationCache()
      .build(queryClient, {
        mutationFn: () => Promise.reject(new ApiError('expired', 401)),
      })
      .execute(undefined)
      .catch(() => undefined)

    await vi.waitFor(() => expect(fetchMe).toHaveBeenCalledTimes(1))
  })

  it('leaves other errors and the current user query alone', async () => {
    const { queryClient, fetchMe } = setUp()
    await queryClient.fetchQuery({ queryKey: ['me'], queryFn: fetchMe })
    fetchMe.mockClear()

    await queryClient
      .fetchQuery({
        queryKey: ['admin', 'consultation-offerings'],
        queryFn: () => Promise.reject(new ApiError('forbidden', 403)),
      })
      .catch(() => undefined)
    await queryClient
      .fetchQuery({
        queryKey: ['me'],
        queryFn: () => Promise.reject(new ApiError('expired', 401)),
        staleTime: 0,
      })
      .catch(() => undefined)

    expect(fetchMe).not.toHaveBeenCalled()
  })
})
