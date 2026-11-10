import { useId, type ReactNode } from 'react'
import type { FieldError } from 'react-hook-form'
import { Label } from '@/components/ui/label'

interface ControlProps {
  id: string
  'aria-invalid': boolean
  'aria-describedby': string | undefined
}

/** A label, its control and the backend's message for it, in a React Hook Form form. */
export function Field({
  label,
  error,
  children,
}: {
  label: string
  error: FieldError | undefined
  children: (props: ControlProps) => ReactNode
}) {
  const id = useId()
  const errorId = `${id}-error`
  return (
    <div className="space-y-2">
      <Label htmlFor={id}>{label}</Label>
      {children({
        id,
        'aria-invalid': error !== undefined,
        'aria-describedby': error === undefined ? undefined : errorId,
      })}
      {error !== undefined && (
        <p id={errorId} className="text-sm text-destructive">
          {error.message}
        </p>
      )}
    </div>
  )
}
