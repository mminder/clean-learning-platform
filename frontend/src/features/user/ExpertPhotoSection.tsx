import { useId, useRef, useState, type ChangeEvent } from 'react'
import { ExpertPhoto } from '@/components/ExpertPhoto'
import { Button } from '@/components/ui/button'
import { problemMessage } from '@/lib/problem'
import { useRemoveExpertPhoto, useUploadExpertPhoto } from './queries'

/**
 * UC-14: the expert's photo, uploaded, replaced or removed at once, apart from the form's **Save** (D-21). The server
 * decides what it accepts: a file it refuses (400) shows its `detail` here.
 */
export function ExpertPhotoSection({ photoId }: { photoId?: string | null }) {
  const uploadPhoto = useUploadExpertPhoto()
  const removePhoto = useRemoveExpertPhoto()
  const fileInput = useRef<HTMLInputElement>(null)
  const headingId = useId()
  const [error, setError] = useState<string>()
  const hasPhoto = photoId !== undefined && photoId !== null
  const disabled = uploadPhoto.isPending || removePhoto.isPending

  async function upload(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0]
    // empty again, so choosing the same file once more uploads it again
    event.target.value = ''
    if (file === undefined) {
      return
    }
    setError(undefined)
    try {
      await uploadPhoto.mutateAsync(file)
    } catch (failure) {
      setError(
        problemMessage(
          failure,
          'Uploading the photo failed. Please try again.',
        ),
      )
    }
  }

  async function remove() {
    setError(undefined)
    try {
      await removePhoto.mutateAsync()
    } catch (failure) {
      setError(
        problemMessage(failure, 'Removing the photo failed. Please try again.'),
      )
    }
  }

  return (
    <section aria-labelledby={headingId} className="max-w-xl space-y-3">
      <h2 id={headingId} className="text-lg font-semibold">
        Photo
      </h2>
      <ExpertPhoto photoId={photoId} className="size-32" />
      <div className="flex flex-wrap gap-3">
        <input
          ref={fileInput}
          type="file"
          accept="image/jpeg,image/png,image/webp"
          className="hidden"
          disabled={disabled}
          onChange={(event) => void upload(event)}
        />
        <Button
          type="button"
          variant="outline"
          disabled={disabled}
          onClick={() => fileInput.current?.click()}
        >
          {hasPhoto ? 'Replace photo' : 'Upload photo'}
        </Button>
        {hasPhoto && (
          <Button
            type="button"
            variant="outline"
            disabled={disabled}
            onClick={() => void remove()}
          >
            Remove photo
          </Button>
        )}
      </div>
      <p className="text-sm text-muted-foreground">
        JPEG, PNG or WebP, up to 2 MB. A change takes effect at once.
      </p>
      {error !== undefined && (
        <p role="alert" className="text-destructive">
          {error}
        </p>
      )}
    </section>
  )
}
