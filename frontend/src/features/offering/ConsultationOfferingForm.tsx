import { useId, useState, type ReactNode } from 'react'
import { useForm, type FieldError } from 'react-hook-form'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { showSaveError } from '@/lib/problem'
import type { ConsultationOfferingRequest } from './queries'

/**
 * The offering's form, for a new and an existing offering (UC-16 steps 3–4). It has no rules of its own: the
 * backend's 400 puts each message next to its field. A number field left empty is `NaN`, which JSON sends as `null`.
 */
export function ConsultationOfferingForm({
  defaultValues,
  onSave,
}: {
  defaultValues: ConsultationOfferingRequest
  onSave: (offering: ConsultationOfferingRequest) => Promise<unknown>
}) {
  const {
    register,
    handleSubmit,
    setError,
    reset,
    formState: { errors, isDirty, isSubmitting },
  } = useForm<ConsultationOfferingRequest>({ defaultValues })
  const [formError, setFormError] = useState<string>()
  const [saved, setSaved] = useState(false)

  async function save(offering: ConsultationOfferingRequest) {
    setFormError(undefined)
    setSaved(false)
    try {
      await onSave(offering)
    } catch (error) {
      setFormError(showSaveError(error, setError))
      return
    }
    // The saved values are the form's new starting point, so "Saved" goes away with the next change.
    reset(offering)
    setSaved(true)
  }

  return (
    <form onSubmit={handleSubmit(save)} className="max-w-xl space-y-6">
      {formError !== undefined && (
        <p role="alert" className="text-destructive">
          {formError}
        </p>
      )}
      <Field label="Name" error={errors.name}>
        {(props) => <Input {...props} {...register('name')} />}
      </Field>
      <Field label="Description" error={errors.description}>
        {(props) => (
          <Textarea {...props} rows={6} {...register('description')} />
        )}
      </Field>
      <Field
        label="Session duration (minutes)"
        error={errors.sessionDurationMinutes}
      >
        {(props) => (
          <Input
            {...props}
            type="number"
            className="max-w-40"
            {...register('sessionDurationMinutes', { valueAsNumber: true })}
          />
        )}
      </Field>
      <Field
        label="Cancellation deadline (hours)"
        error={errors.cancellationDeadlineHours}
      >
        {(props) => (
          <Input
            {...props}
            type="number"
            className="max-w-40"
            {...register('cancellationDeadlineHours', { valueAsNumber: true })}
          />
        )}
      </Field>
      <div className="flex items-center gap-4">
        <Button type="submit" disabled={isSubmitting}>
          Save
        </Button>
        {saved && !isDirty && (
          <p role="status" className="text-sm text-muted-foreground">
            Saved
          </p>
        )}
      </div>
    </form>
  )
}

interface ControlProps {
  id: string
  'aria-invalid': boolean
  'aria-describedby': string | undefined
}

/** A label, its control and the backend's message for it. */
function Field({
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
