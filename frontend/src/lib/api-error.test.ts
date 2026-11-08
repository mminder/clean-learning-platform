import { describe, expect, it } from 'vitest'
import {
  ApiError,
  isBadRequest,
  isClientError,
  isConflict,
  isNotFound,
  retryUnlessClientError,
} from './api-error'

describe('ApiError', () => {
  it('carries the response status', () => {
    const error = new ApiError('Loading the offering failed', 404)

    expect(error).toBeInstanceOf(Error)
    expect(error.status).toBe(404)
    expect(error.message).toBe('Loading the offering failed')
    expect(error.problem).toBeUndefined()
  })

  it('carries the problem of a 400', () => {
    const problem = {
      title: 'Bad Request',
      status: 400,
      instance: '/api/admin/consultation-offerings',
      errors: [{ field: 'name', message: 'must not be blank' }],
    }

    expect(new ApiError('Saving failed', 400, problem).problem).toBe(problem)
  })
})

describe('isNotFound', () => {
  it('is true only for an API error with status 404', () => {
    expect(isNotFound(new ApiError('Not found', 404))).toBe(true)
    expect(isNotFound(new ApiError('Bad request', 400))).toBe(false)
    expect(isNotFound(new ApiError('Server error', 500))).toBe(false)
    expect(isNotFound(new Error('Network down'))).toBe(false)
    expect(isNotFound(null)).toBe(false)
  })
})

describe('isBadRequest', () => {
  it('is true only for an API error with status 400', () => {
    expect(isBadRequest(new ApiError('Bad request', 400))).toBe(true)
    expect(isBadRequest(new ApiError('Not found', 404))).toBe(false)
    expect(isBadRequest(new Error('Network down'))).toBe(false)
  })
})

describe('isConflict', () => {
  it('is true only for an API error with status 409', () => {
    expect(isConflict(new ApiError('Conflict', 409))).toBe(true)
    expect(isConflict(new ApiError('Bad request', 400))).toBe(false)
    expect(isConflict(new Error('Network down'))).toBe(false)
  })
})

describe('isClientError', () => {
  it('is true only for an API error with a 4xx status', () => {
    expect(isClientError(new ApiError('Bad request', 400))).toBe(true)
    expect(isClientError(new ApiError('Not found', 404))).toBe(true)
    expect(isClientError(new ApiError('Conflict', 499))).toBe(true)
    expect(isClientError(new ApiError('Redirect', 399))).toBe(false)
    expect(isClientError(new ApiError('Server error', 500))).toBe(false)
    expect(isClientError(new Error('Network down'))).toBe(false)
  })
})

describe('retryUnlessClientError', () => {
  it('never retries a client error', () => {
    expect(retryUnlessClientError(0, new ApiError('Not found', 404))).toBe(
      false,
    )
    expect(retryUnlessClientError(0, new ApiError('Bad request', 400))).toBe(
      false,
    )
  })

  it('retries other errors up to three times', () => {
    const serverError = new ApiError('Server error', 500)
    expect(retryUnlessClientError(0, serverError)).toBe(true)
    expect(retryUnlessClientError(2, serverError)).toBe(true)
    expect(retryUnlessClientError(3, serverError)).toBe(false)
    expect(retryUnlessClientError(0, new TypeError('Failed to fetch'))).toBe(
      true,
    )
  })
})
