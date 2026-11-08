import type { ProblemDetail } from './problem'

/**
 * A non-2xx answer from the API, with its HTTP status, so a page can tell a 404 apart from other errors. A 400's or
 * 409's body comes with it as `problem` (`problemOf` in `problem.ts`).
 */
export class ApiError extends Error {
  readonly status: number
  readonly problem: ProblemDetail | undefined

  constructor(message: string, status: number, problem?: ProblemDetail) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.problem = problem
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
