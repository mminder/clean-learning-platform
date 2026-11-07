import { describe, expect, it } from 'vitest'
import { ApiError, isNotFound, retryUnlessNotFound } from './api-error'

describe('ApiError', () => {
  it('carries the response status', () => {
    const error = new ApiError('Loading the offering failed', 404)

    expect(error).toBeInstanceOf(Error)
    expect(error.status).toBe(404)
    expect(error.message).toBe('Loading the offering failed')
  })
})

describe('isNotFound', () => {
  it('is true only for an API error with status 404', () => {
    expect(isNotFound(new ApiError('Not found', 404))).toBe(true)
    expect(isNotFound(new ApiError('Server error', 500))).toBe(false)
    expect(isNotFound(new Error('Network down'))).toBe(false)
    expect(isNotFound(null)).toBe(false)
  })
})

describe('retryUnlessNotFound', () => {
  it('never retries a 404', () => {
    expect(retryUnlessNotFound(0, new ApiError('Not found', 404))).toBe(false)
  })

  it('retries other errors up to three times', () => {
    const serverError = new ApiError('Server error', 500)
    expect(retryUnlessNotFound(0, serverError)).toBe(true)
    expect(retryUnlessNotFound(2, serverError)).toBe(true)
    expect(retryUnlessNotFound(3, serverError)).toBe(false)
    expect(retryUnlessNotFound(0, new TypeError('Failed to fetch'))).toBe(true)
  })
})
