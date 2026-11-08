import type { components } from '@/api/schema'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { useConsultationOfferings } from './queries'

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

/** UC-16 step 1: every consultation offering with its status and counts. */
export function ConsultationOfferingsPage() {
  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-semibold">Consultations</h1>
      <ConsultationOfferings />
    </div>
  )
}

function ConsultationOfferings() {
  const offerings = useConsultationOfferings()

  if (offerings.isPending) {
    return (
      <p className="text-muted-foreground">Loading consultation offerings…</p>
    )
  }
  if (offerings.isError) {
    return (
      <div role="alert" className="space-y-3">
        <p>The consultation offerings could not be loaded.</p>
        <Button variant="outline" onClick={() => offerings.refetch()}>
          Try again
        </Button>
      </div>
    )
  }
  if (offerings.data.length === 0) {
    return (
      <p className="text-muted-foreground">No consultation offerings yet.</p>
    )
  }
  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>Name</TableHead>
          <TableHead>Status</TableHead>
          <TableHead className="text-right">Packages</TableHead>
          <TableHead className="text-right">Experts</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {offerings.data.map((offering) => (
          <TableRow key={offering.id}>
            <TableCell className="font-medium">{offering.name}</TableCell>
            <TableCell>
              <Badge variant={statusVariants[offering.status]}>
                {statusLabels[offering.status]}
              </Badge>
            </TableCell>
            <TableCell className="text-right">
              {offering.packageCount}
            </TableCell>
            <TableCell className="text-right">
              {offering.eligibleExpertCount}
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  )
}
