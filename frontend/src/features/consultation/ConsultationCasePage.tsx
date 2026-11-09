import type { ReactNode } from 'react'
import { useParams } from 'react-router'
import { OfferingImage } from '@/components/OfferingImage'
import { Button } from '@/components/ui/button'
import { isBadRequest, isNotFound } from '@/lib/api-error'
import { formatDateTime } from '@/lib/date'
import { formatMoney } from '@/lib/money'
import { useCustomerConsultationCase } from './queries'

/**
 * UC-05's case detail, as far as a case has data yet: the offering, the expert (none before the first booking), the
 * credits, the purchase terms and the purchase time. Another customer's case is "Case not found", like an unknown one.
 */
export function ConsultationCasePage() {
  // The route /my/cases/:id always has an id.
  const { id = '' } = useParams()
  const consultationCase = useCustomerConsultationCase(id)

  if (consultationCase.isPending) {
    return <p className="text-muted-foreground">Loading the case…</p>
  }
  if (consultationCase.isError) {
    // A malformed id in the URL (400) is a missing case for the customer, too.
    if (
      isNotFound(consultationCase.error) ||
      isBadRequest(consultationCase.error)
    ) {
      return <h1 className="text-2xl font-semibold">Case not found</h1>
    }
    return (
      <div role="alert" className="space-y-3">
        <p>The case could not be loaded.</p>
        <Button variant="outline" onClick={() => consultationCase.refetch()}>
          Try again
        </Button>
      </div>
    )
  }

  const {
    offeringName,
    offeringImageId,
    purchaseTerms,
    purchasedAt,
    openCredits,
    totalCredits,
  } = consultationCase.data
  return (
    <article className="space-y-8">
      <header className="space-y-3">
        <OfferingImage
          imageId={offeringImageId}
          className="aspect-video w-full max-w-2xl rounded-xl"
        />
        <h1 className="text-3xl font-semibold">{offeringName}</h1>
      </header>
      <dl className="grid gap-4 sm:grid-cols-2">
        <Term label="Expert">Not assigned yet</Term>
        <Term label="Credits">
          {openCredits} of {totalCredits} open
        </Term>
        <Term label="Price paid">{formatMoney(purchaseTerms.pricePaid)}</Term>
        <Term label="Sessions">
          {purchaseTerms.sessionCount} × {purchaseTerms.sessionDurationMinutes}{' '}
          min
        </Term>
        <Term label="Cancellation">
          Free cancellation up to {purchaseTerms.cancellationDeadlineHours} h
          before a session
        </Term>
        <Term label="Bought">
          {formatDateTime(purchasedAt)}{' '}
          <span className="font-normal text-muted-foreground">
            (Zurich time)
          </span>
        </Term>
      </dl>
    </article>
  )
}

function Term({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div>
      <dt className="text-sm text-muted-foreground">{label}</dt>
      <dd className="font-medium">{children}</dd>
    </div>
  )
}
