import { ImageIcon } from 'lucide-react'
import { cn } from 'cn'

// Stands in for offering images and expert photos until images exist (UC-14, UC-16).
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
