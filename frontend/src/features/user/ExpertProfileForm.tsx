import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Field } from '@/components/Field'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { showSaveError } from '@/lib/problem'
import {
  toRequest,
  type ExpertProfile,
  type ExpertProfileRequest,
} from './queries'

/**
 * The expert's profile form (UC-14 steps 2–4): display name, bio and Teams link. It has no rules of its own: the
 * backend's 400 puts each message next to its field. An empty Teams link is sent as `""`, which removes the link.
 * `onSave` resolves with the saved profile, which becomes the form's new starting point.
 */
export function ExpertProfileForm({
  defaultValues,
  onSave,
}: {
  defaultValues: ExpertProfileRequest
  onSave: (profile: ExpertProfileRequest) => Promise<ExpertProfile>
}) {
  const {
    register,
    handleSubmit,
    setError,
    reset,
    formState: { errors, isDirty, isSubmitting },
  } = useForm<ExpertProfileRequest>({ defaultValues })
  const [formError, setFormError] = useState<string>()
  const [saved, setSaved] = useState(false)

  async function save(profile: ExpertProfileRequest) {
    setFormError(undefined)
    setSaved(false)
    let savedProfile: ExpertProfile
    try {
      savedProfile = await onSave(profile)
    } catch (error) {
      setFormError(showSaveError(error, setError))
      return
    }
    // The saved values are the form's new starting point, so "Saved" goes away with the next change.
    reset(toRequest(savedProfile))
    setSaved(true)
  }

  return (
    <form onSubmit={handleSubmit(save)} className="max-w-xl space-y-6">
      {formError !== undefined && (
        <p role="alert" className="text-destructive">
          {formError}
        </p>
      )}
      <Field label="Display name" error={errors.displayName}>
        {(props) => <Input {...props} {...register('displayName')} />}
      </Field>
      <Field label="Bio" error={errors.bio}>
        {(props) => <Textarea {...props} rows={6} {...register('bio')} />}
      </Field>
      <Field label="Teams link" error={errors.teamsLink}>
        {/* Not `type="url"`: the browser would refuse a link before the backend's message could show. */}
        {(props) => <Input {...props} {...register('teamsLink')} />}
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
