import { useState } from 'react'
import { Controller, useFieldArray, useForm } from 'react-hook-form'
import { Field } from '@/components/Field'
import { Button } from '@/components/ui/button'
import { Calendar } from '@/components/ui/calendar'
import { Input } from '@/components/ui/input'
import {
  formatLocalDate,
  fromCalendarDay,
  toCalendarDay,
  todayInZurich,
} from '@/lib/date'
import { showSaveError } from '@/lib/problem'
import {
  toRequest,
  type Availability,
  type AvailabilityRequest,
} from './queries'

/**
 * The expert's availability (UC-13 steps 2–4): the dates in a month calendar, and one set of daily time ranges for all
 * of them. It has no rules of its own: the backend's 400 puts each message next to its field, and a date before
 * today, which the calendar doesn't offer, shows above the form. `onSave` resolves with the saved availability, which
 * becomes the form's new starting point.
 */
export function AvailabilityForm({
  defaultValues,
  onSave,
}: {
  defaultValues: AvailabilityRequest
  onSave: (availability: AvailabilityRequest) => Promise<Availability>
}) {
  const {
    register,
    control,
    handleSubmit,
    setError,
    reset,
    formState: { errors, isDirty, isSubmitting },
  } = useForm<AvailabilityRequest>({ defaultValues })
  const {
    fields: rangeRows,
    append,
    remove,
  } = useFieldArray({ control, name: 'timeRanges' })
  const [formError, setFormError] = useState<string>()
  const [saved, setSaved] = useState(false)
  // Days before today (Zurich) are past and can't be changed; today can (UC-13).
  const today = toCalendarDay(todayInZurich())

  async function save(availability: AvailabilityRequest) {
    setFormError(undefined)
    setSaved(false)
    let savedAvailability: Availability
    try {
      savedAvailability = await onSave(availability)
    } catch (error) {
      setFormError(showSaveError(error, setError))
      return
    }
    // The saved values are the form's new starting point, so "Saved" goes away with the next change.
    reset(toRequest(savedAvailability))
    setSaved(true)
  }

  return (
    <form onSubmit={handleSubmit(save)} className="max-w-xl space-y-6">
      {formError !== undefined && (
        <p role="alert" className="text-destructive">
          {formError}
        </p>
      )}
      <fieldset className="space-y-3">
        <legend className="mb-2 text-lg font-semibold">Dates</legend>
        <Controller
          control={control}
          name="dates"
          render={({ field }) => (
            <>
              <Calendar
                mode="multiple"
                timeZone="Europe/Zurich"
                className="rounded-md border"
                selected={field.value.map(toCalendarDay)}
                // Sorted, as the saved dates are, so selecting a date and deselecting it again isn't a change.
                onSelect={(days) =>
                  field.onChange((days ?? []).map(fromCalendarDay).sort())
                }
                disabled={{ before: today }}
              />
              <p className="text-sm text-muted-foreground">
                {field.value.length === 0
                  ? 'No dates selected.'
                  : `Selected: ${field.value.map(formatLocalDate).join(', ')}`}
              </p>
            </>
          )}
        />
        {errors.dates?.message !== undefined && (
          <p className="text-sm text-destructive">{errors.dates.message}</p>
        )}
      </fieldset>
      <fieldset className="space-y-4">
        <legend className="mb-2 text-lg font-semibold">
          Daily time ranges
        </legend>
        {rangeRows.length === 0 && (
          <p className="text-sm text-muted-foreground">
            No time ranges yet. Add one so that customers can book you.
          </p>
        )}
        {rangeRows.map((row, index) => (
          <div
            key={row.id}
            className="grid grid-cols-[minmax(0,1fr)_minmax(0,1fr)_auto] items-start gap-4"
          >
            <Field label="Start" error={errors.timeRanges?.[index]?.start}>
              {(props) => (
                <Input
                  {...props}
                  type="time"
                  {...register(`timeRanges.${index}.start`)}
                />
              )}
            </Field>
            <Field label="End" error={errors.timeRanges?.[index]?.end}>
              {(props) => (
                <Input
                  {...props}
                  type="time"
                  {...register(`timeRanges.${index}.end`)}
                />
              )}
            </Field>
            <Button
              type="button"
              variant="outline"
              className="mt-7"
              aria-label={`Remove time range ${index + 1}`}
              onClick={() => remove(index)}
            >
              Remove
            </Button>
          </div>
        ))}
        <Button
          type="button"
          variant="outline"
          // A new row's fields start empty.
          onClick={() => append({ start: '', end: '' })}
        >
          Add time range
        </Button>
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
