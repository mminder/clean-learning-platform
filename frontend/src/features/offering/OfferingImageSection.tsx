import { useId, useRef, useState, type ChangeEvent } from 'react'
import { OfferingImage } from '@/components/OfferingImage'
import { Button } from '@/components/ui/button'
import { problemMessage } from '@/lib/problem'
import {
  useRemoveConsultationOfferingImage,
  useUploadConsultationOfferingImage,
  type ConsultationOffering,
} from './queries'

/**
 * UC-16 step 3: the offering's image, uploaded, replaced or removed at once, apart from the form's **Save** (D-21).
 * The server decides what it accepts: a file it refuses (400) or a change of an archived offering (409) shows its
 * `detail` here. An archived offering's image can't be changed.
 */
export function OfferingImageSection({
  offering,
}: {
  offering: ConsultationOffering
}) {
  const { id, imageId, status } = offering
  const uploadImage = useUploadConsultationOfferingImage(id)
  const removeImage = useRemoveConsultationOfferingImage(id)
  const fileInput = useRef<HTMLInputElement>(null)
  const headingId = useId()
  const [error, setError] = useState<string>()
  const hasImage = imageId !== undefined && imageId !== null
  const disabled =
    status === 'ARCHIVED' || uploadImage.isPending || removeImage.isPending

  async function upload(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0]
    // empty again, so choosing the same file once more uploads it again
    event.target.value = ''
    if (file === undefined) {
      return
    }
    setError(undefined)
    try {
      await uploadImage.mutateAsync(file)
    } catch (failure) {
      setError(
        problemMessage(
          failure,
          'Uploading the image failed. Please try again.',
        ),
      )
    }
  }

  async function remove() {
    setError(undefined)
    try {
      await removeImage.mutateAsync()
    } catch (failure) {
      setError(
        problemMessage(failure, 'Removing the image failed. Please try again.'),
      )
    }
  }

  return (
    <section aria-labelledby={headingId} className="max-w-xl space-y-3">
      <h2 id={headingId} className="text-lg font-semibold">
        Image
      </h2>
      <OfferingImage
        imageId={imageId}
        className="aspect-video w-full rounded-lg"
      />
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
          {hasImage ? 'Replace image' : 'Upload image'}
        </Button>
        {hasImage && (
          <Button
            type="button"
            variant="outline"
            disabled={disabled}
            onClick={() => void remove()}
          >
            Remove image
          </Button>
        )}
      </div>
      {status !== 'ARCHIVED' && (
        <p className="text-sm text-muted-foreground">
          JPEG, PNG or WebP, up to 2 MB. A change takes effect at once.
        </p>
      )}
      {error !== undefined && (
        <p role="alert" className="text-destructive">
          {error}
        </p>
      )}
    </section>
  )
}
