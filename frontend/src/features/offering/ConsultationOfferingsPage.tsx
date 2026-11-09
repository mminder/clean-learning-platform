import { Link } from 'react-router'
import { Button } from '@/components/ui/button'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { OfferingStatusBadge } from './OfferingStatusBadge'
import { useConsultationOfferings } from './queries'

/** UC-16 step 1: every consultation offering with its status and counts; each name opens the offering. */
export function ConsultationOfferingsPage() {
  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <h1 className="text-2xl font-semibold">Consultations</h1>
        <Button asChild>
          <Link to="/admin/consultations/new">New offering</Link>
        </Button>
      </div>
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
            <TableCell className="font-medium">
              <Link
                to={`/admin/consultations/${offering.id}`}
                className="hover:underline"
              >
                {offering.name}
              </Link>
            </TableCell>
            <TableCell>
              <OfferingStatusBadge status={offering.status} />
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
