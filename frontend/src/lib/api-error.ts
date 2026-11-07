/** A non-2xx answer from the API, with its HTTP status, so a page can tell a 404 apart from other errors. */
export class ApiError extends Error {
  readonly status: number

  constructor(message: string, status: number) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

export function isNotFound(error: unknown): boolean {
  return error instanceof ApiError && error.status === 404
}

export function isBadRequest(error: unknown): boolean {
  return error instanceof ApiError && error.status === 400
}

export function isClientError(error: unknown): boolean {
  return error instanceof ApiError && error.status >= 400 && error.status < 500
}

/**
 * The QueryClient's default `retry`: a client error (4xx) won't change by asking again, so it isn't retried.
 * Other errors are retried three times, as TanStack Query does by default.
 */
export function retryUnlessClientError(
  failureCount: number,
  error: Error,
): boolean {
  return !isClientError(error) && failureCount < 3
}
