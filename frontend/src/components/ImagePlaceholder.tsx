import { ImageIcon } from 'lucide-react'
import { cn } from 'cn'

// Stands in for an offering without an image (OfferingImage) and an expert without a photo (ExpertPhoto).
export function ImagePlaceholder({ className }: { className?: string }) {
  return (
    <div
      aria-hidden="true"
      className={cn(
        'flex items-center justify-center bg-muted text-muted-foreground',
        className,
      )}
    >
      <ImageIcon className="size-10" />
    </div>
  )
}
