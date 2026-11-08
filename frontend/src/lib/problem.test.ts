import { describe, expect, it, vi } from 'vitest'
import { ApiError } from './api-error'
import {
  fieldErrors,
  problemOf,
  showSaveError,
  toFormPath,
  type ProblemDetail,
} from './problem'

const badRequest: ProblemDetail = {
  title: 'Bad Request',
  status: 400,
  detail: 'Invalid request content.',
  instance: '/api/admin/consultation-offerings',
  errors: [
    { field: 'name', message: 'must not be blank' },
    {
      field: 'packages[1].price',
      message: 'must be an amount in CHF with at most two decimals',
    },
  ],
}

describe('problemOf', () => {
  it('reads the body of a 400 and a 409', () => {
    expect(problemOf(400, badRequest)).toBe(badRequest)

    const conflict = { ...badRequest, status: 409, errors: undefined }
    expect(problemOf(409, conflict)).toBe(conflict)
  })

  it('ignores the body of any other status, which decides by status alone', () => {
    // A 403's body is Boot's error JSON, a 401's is empty (D-34).
    expect(problemOf(403, badRequest)).toBeUndefined()
    expect(problemOf(401, undefined)).toBeUndefined()
    expect(problemOf(404, badRequest)).toBeUndefined()
    expect(problemOf(500, badRequest)).toBeUndefined()
  })

  it('is undefined without a body', () => {
    expect(problemOf(400, undefined)).toBeUndefined()
  })
})

describe('toFormPath', () => {
  it('turns Spring indexes into React Hook Form paths', () => {
    expect(toFormPath('name')).toBe('name')
    expect(toFormPath('packages[1].price')).toBe('packages.1.price')
    expect(toFormPath('runs[0].days[12].start')).toBe('runs.0.days.12.start')
  })
})

describe('fieldErrors', () => {
  it('lists each field with its form path and message', () => {
    expect(fieldErrors(badRequest)).toEqual([
      { path: 'name', message: 'must not be blank' },
      {
        path: 'packages.1.price',
        message: 'must be an amount in CHF with at most two decimals',
      },
    ])
  })

  it('is empty for a problem without errors', () => {
    expect(
      fieldErrors({
        ...badRequest,
        errors: undefined,
        detail: 'An offering name is not blank',
      }),
    ).toEqual([])
  })
})

describe('showSaveError', () => {
  it('sets each field error', () => {
    const setError = vi.fn()

    const message = showSaveError(
      new ApiError('Saving failed', 400, badRequest),
      setError,
    )

    expect(message).toBeUndefined()
    expect(setError.mock.calls).toEqual([
      ['name', { type: 'server', message: 'must not be blank' }],
      [
        'packages.1.price',
        {
          type: 'server',
          message: 'must be an amount in CHF with at most two decimals',
        },
      ],
    ])
  })

  it('returns the detail of a problem without field errors, for above the form', () => {
    const setError = vi.fn()
    const problem = {
      title: 'Bad Request',
      status: 400,
      detail: 'An offering name is not blank',
      instance: '/api/admin/consultation-offerings',
    }

    expect(
      showSaveError(new ApiError('Saving failed', 400, problem), setError),
    ).toBe('An offering name is not blank')
    expect(
      showSaveError(
        new ApiError('Saving failed', 409, { ...problem, detail: undefined }),
        setError,
      ),
    ).toBe('Bad Request')
    expect(setError).not.toHaveBeenCalled()
  })

  it('returns a general message for any other error', () => {
    const setError = vi.fn()

    expect(showSaveError(new ApiError('Saving failed', 500), setError)).toBe(
      'Saving failed. Please try again.',
    )
    expect(showSaveError(new TypeError('Failed to fetch'), setError)).toBe(
      'Saving failed. Please try again.',
    )
    expect(setError).not.toHaveBeenCalled()
  })
})
