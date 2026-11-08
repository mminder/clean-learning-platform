import type { FieldValues, Path, UseFormSetError } from 'react-hook-form'
import type { components } from '@/api/schema'
import { ApiError } from './api-error'

/** An error body of the API (RFC 9457), as the generated contract describes it (D-34). */
export type ProblemDetail = components['schemas']['ProblemDetail']

/** A field error of a 400, with the field's path in a React Hook Form form. */
export interface FieldError {
  path: string
  message: string
}

/**
 * The problem in an error answer, if the frontend reads it: only a 400's and a 409's body (D-34). For any other
 * status the body isn't read, and the page decides by the status alone.
 */
export function problemOf(
  status: number,
  body: ProblemDetail | undefined,
): ProblemDetail | undefined {
  return status === 400 || status === 409 ? body : undefined
}

/** A field's path as Spring reports it, as React Hook Form names it: `packages[1].price` → `packages.1.price`. */
export function toFormPath(field: string): string {
  return field.replace(/\[(\d+)\]/g, '.$1')
}

/** The 400's invalid fields, for `setError` on each. A problem without `errors` shows its `detail` instead. */
export function fieldErrors(problem: ProblemDetail): FieldError[] {
  return (problem.errors ?? []).map(({ field, message }) => ({
    path: toFormPath(field),
    message,
  }))
}

/**
 * Shows why a save failed, in a React Hook Form form. A 400 with `errors` puts each message next to its field; then
 * there's nothing to show above the form. Otherwise it returns the message for above the form: the `detail` of a 400
 * or 409, or a general message.
 */
export function showSaveError<T extends FieldValues>(
  error: unknown,
  setError: UseFormSetError<T>,
): string | undefined {
  const problem = error instanceof ApiError ? error.problem : undefined
  if (problem === undefined) {
    return 'Saving failed. Please try again.'
  }
  const errors = fieldErrors(problem)
  if (errors.length === 0) {
    return problem.detail ?? problem.title
  }
  for (const { path, message } of errors) {
    // The backend names the field; a path the form doesn't have isn't shown.
    setError(path as Path<T>, { type: 'server', message })
  }
  return undefined
}
