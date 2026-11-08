import { MutationCache, QueryCache, QueryClient } from '@tanstack/react-query'
import { ApiError, retryUnlessClientError } from '@/lib/api-error'

/**
 * The app's QueryClient. A 401 from any query or mutation other than `useMe` means the session has ended, so it
 * refetches the current user: `RequireRole` then sends a visitor on a protected page to the login. The backend
 * decides who is logged in; the SPA only asks again.
 */
export function createQueryClient(): QueryClient {
  const queryClient: QueryClient = new QueryClient({
    defaultOptions: { queries: { retry: retryUnlessClientError } },
    queryCache: new QueryCache({
      onError: (error, query) => {
        if (query.queryKey[0] !== 'me') {
          refetchMeOnUnauthorized(queryClient, error)
        }
      },
    }),
    mutationCache: new MutationCache({
      onError: (error) => refetchMeOnUnauthorized(queryClient, error),
    }),
  })
  return queryClient
}

function refetchMeOnUnauthorized(queryClient: QueryClient, error: unknown) {
  if (error instanceof ApiError && error.status === 401) {
    void queryClient.refetchQueries({ queryKey: ['me'] })
  }
}
