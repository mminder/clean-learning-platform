import { cn } from 'cn'
import { ImagePlaceholder } from './ImagePlaceholder'

/**
 * An offering's image (D-21), or the placeholder without one, in the same frame: `className` sizes both. The image is
 * decorative, the offering's name is always next to it. Its URL never changes its bytes, so the browser caches it.
 */
export function OfferingImage({
  imageId,
  className,
}: {
  imageId?: string | null
  className?: string
}) {
  if (imageId === undefined || imageId === null) {
    return <ImagePlaceholder className={className} />
  }
  return (
    <img
      src={`/api/images/${imageId}`}
      alt=""
      className={cn('bg-muted object-cover', className)}
    />
  )
}
