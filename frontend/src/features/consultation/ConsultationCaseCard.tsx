import { Link } from 'react-router'
import { OfferingImage } from '@/components/OfferingImage'
import {
  Card,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { formatDate } from '@/lib/date'
import { formatDateAndTimes } from './appointment-times'
import type { CustomerConsultationCaseSummary } from './queries'

/**
 * A case on My purchases (UC-05 step 2): the offering, the expert's name or "Not assigned yet", the next appointment
 * in Zurich time, "Next" before it starts and "Now" while it runs (spec 04, decision 21), the credits and the
 * purchase date. A cancelled case says "Cancelled" in place of the next appointment and the credits (spec 05,
 * decision 6); the API puts it under Past.
 */
export function ConsultationCaseCard({
  consultationCase,
}: {
  consultationCase: CustomerConsultationCaseSummary
}) {
  const next = consultationCase.nextAppointment
  return (
    // The title's link stretches over the whole card, so a click anywhere opens the case.
    <Card className="relative w-full overflow-hidden pt-0 transition-shadow focus-within:ring-2 focus-within:ring-ring hover:shadow-md">
      <OfferingImage
        imageId={consultationCase.offeringImageId}
        className="aspect-video"
      />
      <CardHeader>
        <CardTitle>
          <h3 className="text-lg">
            <Link
              to={`/my/cases/${consultationCase.id}`}
              className="outline-none after:absolute after:inset-0"
            >
              {consultationCase.offeringName}
            </Link>
          </h3>
        </CardTitle>
        <CardDescription>
          {consultationCase.expertDisplayName ?? 'Not assigned yet'}
        </CardDescription>
      </CardHeader>
      <CardFooter className="mt-auto flex-col items-start gap-1 text-sm">
        {consultationCase.cancelledAt ? (
          <p className="font-medium">Cancelled</p>
        ) : (
          <>
            {next && (
              <p className="font-medium">
                {next.status === 'CONSUMED' ? 'Now' : 'Next'}:{' '}
                {formatDateAndTimes(next.start, next.end)}
              </p>
            )}
            <p className="font-medium">
              {consultationCase.openCredits} of {consultationCase.totalCredits}{' '}
              credits open
            </p>
          </>
        )}
        <p className="text-muted-foreground">
          Bought {formatDate(consultationCase.purchasedAt)}
        </p>
      </CardFooter>
    </Card>
  )
}
