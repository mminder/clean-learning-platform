import { Link } from 'react-router'
import { Button } from '@/components/ui/button'
import { ConsultationCaseCard } from './ConsultationCaseCard'
import {
  type CustomerConsultationCaseSummary,
  useCustomerConsultationCases,
} from './queries'

type Group = CustomerConsultationCaseSummary['group']

/**
 * UC-05's landing page, for cases: Upcoming, Needs booking and Past, collapsed (spec 04, decision 6). The server
 * decides each case's group and sends the cases in the page's order (decision 18), so the page only splits the list by
 * `group`, keeping the order, and hides an empty group.
 */
export function MyPurchasesPage() {
  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-semibold">My purchases</h1>
      <Groups />
    </div>
  )
}

function Groups() {
  const cases = useCustomerConsultationCases()

  if (cases.isPending) {
    return <p className="text-muted-foreground">Loading your purchases…</p>
  }
  if (cases.isError) {
    return (
      <div role="alert" className="space-y-3">
        <p>Your purchases could not be loaded.</p>
        <Button variant="outline" onClick={() => cases.refetch()}>
          Try again
        </Button>
      </div>
    )
  }
  if (cases.data.length === 0) {
    return (
      <div className="space-y-3">
        <p className="text-muted-foreground">
          You haven&apos;t bought anything yet.
        </p>
        <Link to="/" className="inline-block text-sm underline">
          Go to the shop
        </Link>
      </div>
    )
  }
  const ofGroup = (group: Group) =>
    cases.data.filter((consultationCase) => consultationCase.group === group)
  const upcoming = ofGroup('UPCOMING')
  const needsBooking = ofGroup('NEEDS_BOOKING')
  const past = ofGroup('PAST')
  return (
    <div className="space-y-8">
      {upcoming.length > 0 && (
        <section className="space-y-3">
          {/* Only an Upcoming card shows a time, so the hint sits next to this heading (frontend.md). */}
          <h2 className="text-lg font-medium">
            Upcoming{' '}
            <span className="text-sm font-normal text-muted-foreground">
              (Zurich time)
            </span>
          </h2>
          <Cards cases={upcoming} />
        </section>
      )}
      {needsBooking.length > 0 && (
        <section className="space-y-3">
          <h2 className="text-lg font-medium">Needs booking</h2>
          <Cards cases={needsBooking} />
        </section>
      )}
      {past.length > 0 && (
        <details className="space-y-3">
          <summary className="cursor-pointer text-lg font-medium">
            Past ({past.length})
          </summary>
          <div className="mt-3">
            <Cards cases={past} />
          </div>
        </details>
      )}
    </div>
  )
}

function Cards({ cases }: { cases: CustomerConsultationCaseSummary[] }) {
  return (
    <ul className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
      {cases.map((consultationCase) => (
        <li key={consultationCase.id} className="flex">
          <ConsultationCaseCard consultationCase={consultationCase} />
        </li>
      ))}
    </ul>
  )
}
