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
import type { CustomerConsultationCaseSummary } from './queries'

/** A case on My purchases (UC-05 step 2), as far as a case has data yet: no appointment before the first booking. */
export function ConsultationCaseCard({
  consultationCase,
}: {
  consultationCase: CustomerConsultationCaseSummary
}) {
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
        <CardDescription>Not assigned yet</CardDescription>
      </CardHeader>
      <CardFooter className="mt-auto flex-col items-start gap-1 text-sm">
        <p className="font-medium">
          {consultationCase.openCredits} of {consultationCase.totalCredits}{' '}
          credits open
        </p>
        <p className="text-muted-foreground">
          Bought {formatDate(consultationCase.purchasedAt)}
        </p>
      </CardFooter>
    </Card>
  )
}
