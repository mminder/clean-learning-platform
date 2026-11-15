import { cn } from 'cn'
import { OfferingImage } from './OfferingImage'

/**
 * An expert's photo (UC-14), round and cropped by the browser, never resized (D-21), or the round placeholder without
 * one: `className` sizes both. The photo is decorative, the expert's name is always next to it.
 */
export function ExpertPhoto({
  photoId,
  className,
}: {
  photoId?: string | null
  className?: string
}) {
  return (
    <OfferingImage
      imageId={photoId}
      className={cn('shrink-0 rounded-full', className)}
    />
  )
}
