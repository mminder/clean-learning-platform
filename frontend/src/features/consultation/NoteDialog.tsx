import { useId, useState } from 'react'
import { Controller, useForm } from 'react-hook-form'
import { Field } from '@/components/Field'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group'
import { Textarea } from '@/components/ui/textarea'
import { isNotFound } from '@/lib/api-error'
import { showSaveError } from '@/lib/problem'
import {
  useAddNote,
  useEditNote,
  type ExpertNote,
  type NoteRequest,
} from './expert-queries'

const NOTE_GONE = 'This note no longer exists.'

/**
 * UC-15's form for a new note (`note` undefined) or an edit: **Title**, **Text** and the visibility, **Private** (a new
 * note's default) or **Public**. No rules in the form: a 400's messages show next to the fields. It closes after the
 * save. A note deleted meanwhile, in another tab, answers 404: the dialog then says so with only **Close**, and the
 * case reloads (spec 06, decision 7).
 *
 * The page renders it once and keeps the note it was opened for. The form unmounts when the dialog closes, so each
 * opening starts from the note's saved values.
 */
export function NoteDialog({
  caseId,
  note,
  open,
  onOpenChange,
}: {
  caseId: string
  note: ExpertNote | undefined
  open: boolean
  onOpenChange: (open: boolean) => void
}) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      {/* The form has no description; a note that's gone states it with role="alert". */}
      <DialogContent aria-describedby={undefined}>
        <NoteForm
          key={note?.id ?? 'new'}
          caseId={caseId}
          note={note}
          onSaved={() => onOpenChange(false)}
        />
      </DialogContent>
    </Dialog>
  )
}

function NoteForm({
  caseId,
  note,
  onSaved,
}: {
  caseId: string
  note: ExpertNote | undefined
  onSaved: () => void
}) {
  const addNote = useAddNote(caseId)
  const editNote = useEditNote(caseId)
  const {
    register,
    control,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<NoteRequest>({
    defaultValues:
      note === undefined
        ? { title: '', text: '', visibility: 'PRIVATE' }
        : { title: note.title, text: note.text, visibility: note.visibility },
  })
  const [formError, setFormError] = useState<string>()
  const [gone, setGone] = useState(false)
  const visibilityId = useId()

  async function save(values: NoteRequest) {
    setFormError(undefined)
    try {
      if (note === undefined) {
        await addNote.mutateAsync(values)
      } else {
        await editNote.mutateAsync({ noteId: note.id, note: values })
      }
    } catch (error) {
      if (note !== undefined && isNotFound(error)) {
        setGone(true)
      } else {
        setFormError(showSaveError(error, setError))
      }
      return
    }
    onSaved()
  }

  const title = (
    <DialogTitle>{note === undefined ? 'Add note' : 'Edit note'}</DialogTitle>
  )
  if (gone) {
    return (
      <>
        <DialogHeader>
          {title}
          <DialogDescription role="alert" className="text-destructive">
            {NOTE_GONE}
          </DialogDescription>
        </DialogHeader>
        <DialogFooter>
          <DialogClose asChild>
            <Button variant="outline">Close</Button>
          </DialogClose>
        </DialogFooter>
      </>
    )
  }
  return (
    <form onSubmit={handleSubmit(save)} className="space-y-6">
      <DialogHeader>{title}</DialogHeader>
      {formError !== undefined && (
        <p role="alert" className="text-destructive">
          {formError}
        </p>
      )}
      <Field label="Title" error={errors.title}>
        {(props) => <Input {...props} {...register('title')} />}
      </Field>
      <Field label="Text" error={errors.text}>
        {(props) => <Textarea {...props} rows={8} {...register('text')} />}
      </Field>
      <div className="space-y-2">
        <Label id={visibilityId}>Visibility</Label>
        <Controller
          control={control}
          name="visibility"
          render={({ field }) => (
            <RadioGroup
              aria-labelledby={visibilityId}
              value={field.value}
              onValueChange={field.onChange}
              className="flex gap-6"
            >
              <Label className="font-normal">
                <RadioGroupItem value="PRIVATE" />
                Private
              </Label>
              <Label className="font-normal">
                <RadioGroupItem value="PUBLIC" />
                Public
              </Label>
            </RadioGroup>
          )}
        />
        {errors.visibility !== undefined && (
          <p className="text-sm text-destructive">
            {errors.visibility.message}
          </p>
        )}
      </div>
      <DialogFooter>
        <DialogClose asChild>
          <Button type="button" variant="outline">
            Cancel
          </Button>
        </DialogClose>
        <Button type="submit" disabled={isSubmitting}>
          Save
        </Button>
      </DialogFooter>
    </form>
  )
}
