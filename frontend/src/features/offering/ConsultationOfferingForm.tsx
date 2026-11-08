import { useId, useState, type ReactNode } from 'react'
import {
  Controller,
  useFieldArray,
  useForm,
  type FieldError,
} from 'react-hook-form'
import { Button } from '@/components/ui/button'
import { Checkbox } from '@/components/ui/checkbox'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { showSaveError } from '@/lib/problem'
import { useExperts } from '@/features/user/queries'
import {
  toRequest,
  type ConsultationOffering,
  type ConsultationOfferingRequest,
} from './queries'

/**
 * The offering's form, for a new and an existing offering (UC-16 steps 3–4). It has no rules of its own: the
 * backend's 400 puts each message next to its field. A number field left empty is `NaN`, which JSON sends as `null`.
 * The eligible experts are one checkbox per expert. `onSave` resolves with the saved offering, whose packages (with
 * the ids of the new ones) become the form's new starting point.
 */
export function ConsultationOfferingForm({
  defaultValues,
  onSave,
}: {
  defaultValues: ConsultationOfferingRequest
  onSave: (
    offering: ConsultationOfferingRequest,
  ) => Promise<ConsultationOffering>
}) {
  const {
    register,
    control,
    handleSubmit,
    setError,
    reset,
    formState: { errors, isDirty, isSubmitting },
  } = useForm<ConsultationOfferingRequest>({ defaultValues })
  // A package row's `id` stays in the form's values, which useFieldArray keeps; its own `fields[i].id` is only a key.
  const {
    fields: packageRows,
    append,
    remove,
  } = useFieldArray({ control, name: 'packages' })
  const experts = useExperts()
  const [formError, setFormError] = useState<string>()
  const [saved, setSaved] = useState(false)

  async function save(offering: ConsultationOfferingRequest) {
    setFormError(undefined)
    setSaved(false)
    let savedOffering: ConsultationOffering
    try {
      savedOffering = await onSave(offering)
    } catch (error) {
      setFormError(showSaveError(error, setError))
      return
    }
    // The saved values are the form's new starting point, so "Saved" goes away with the next change.
    reset(toRequest(savedOffering))
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
      <fieldset className="space-y-4">
        <legend className="mb-2 text-lg font-semibold">Packages</legend>
        {packageRows.length === 0 && (
          <p className="text-sm text-muted-foreground">
            No packages. An offering without packages is not in the shop.
          </p>
        )}
        {packageRows.map((row, index) => (
          <div
            key={row.id}
            className="grid grid-cols-[6rem_minmax(0,1fr)_auto] items-start gap-4"
          >
            <Field
              label="Sessions"
              error={errors.packages?.[index]?.sessionCount}
            >
              {(props) => (
                <Input
                  {...props}
                  type="number"
                  {...register(`packages.${index}.sessionCount`, {
                    valueAsNumber: true,
                  })}
                />
              )}
            </Field>
            <Field label="Price (CHF)" error={errors.packages?.[index]?.price}>
              {(props) => (
                <Input
                  {...props}
                  inputMode="decimal"
                  {...register(`packages.${index}.price`)}
                />
              )}
            </Field>
            <Button
              type="button"
              variant="outline"
              className="mt-7"
              aria-label={`Remove package ${index + 1}`}
              onClick={() => remove(index)}
            >
              Remove
            </Button>
          </div>
        ))}
        <Button
          type="button"
          variant="outline"
          // A new row's fields start empty, not at 0.
          onClick={() => append({ sessionCount: NaN, price: '' })}
        >
          Add package
        </Button>
      </fieldset>
      <fieldset className="space-y-3">
        <legend className="mb-2 text-lg font-semibold">Eligible experts</legend>
        {experts.isPending && (
          <p className="text-sm text-muted-foreground">Loading the experts…</p>
        )}
        {experts.isError && (
          <p role="alert" className="text-sm text-destructive">
            The experts could not be loaded.
          </p>
        )}
        {experts.isSuccess && experts.data.length === 0 && (
          <p className="text-sm text-muted-foreground">No experts.</p>
        )}
        {experts.isSuccess && (
          <Controller
            control={control}
            name="eligibleExpertIds"
            render={({ field }) => (
              <>
                {experts.data.map((expert) => (
                  <ExpertCheckbox
                    key={expert.id}
                    label={expert.displayName}
                    checked={field.value.includes(expert.id)}
                    onCheckedChange={(checked) =>
                      field.onChange(
                        checked
                          ? [...field.value, expert.id]
                          : field.value.filter((id) => id !== expert.id),
                      )
                    }
                  />
                ))}
              </>
            )}
          />
        )}
        {errors.eligibleExpertIds?.message !== undefined && (
          <p className="text-sm text-destructive">
            {errors.eligibleExpertIds.message}
          </p>
        )}
      </fieldset>
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

/** A checkbox with its label. */
function ExpertCheckbox({
  label,
  checked,
  onCheckedChange,
}: {
  label: string
  checked: boolean
  onCheckedChange: (checked: boolean) => void
}) {
  const id = useId()
  return (
    <div className="flex items-center gap-2">
      <Checkbox
        id={id}
        checked={checked}
        onCheckedChange={(state) => onCheckedChange(state === true)}
      />
      <Label htmlFor={id}>{label}</Label>
    </div>
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
