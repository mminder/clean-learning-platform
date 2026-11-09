import type { components } from '@/api/schema'
import { Badge } from '@/components/ui/badge'

type Status =
  components['schemas']['ConsultationOfferingSummaryResponse']['status']

const statusLabels: Record<Status, string> = {
  DRAFT: 'Draft',
  PUBLISHED: 'Published',
  ARCHIVED: 'Archived',
}

const statusVariants: Record<Status, 'default' | 'secondary' | 'outline'> = {
  DRAFT: 'secondary',
  PUBLISHED: 'default',
  ARCHIVED: 'outline',
}

export function OfferingStatusBadge({ status }: { status: Status }) {
  return <Badge variant={statusVariants[status]}>{statusLabels[status]}</Badge>
}
