import { ImageIcon } from 'lucide-react'
import { cn } from 'cn'

// Stands in for an offering without an image (OfferingImage) and for expert photos until they exist (UC-14).
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
