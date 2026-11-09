import { Link } from 'react-router'
import { Button } from '@/components/ui/button'
import { ConsultationCaseCard } from './ConsultationCaseCard'
import { useCustomerConsultationCases } from './queries'

/**
 * UC-05's landing page, for cases. Without appointments and cancellations, every case needs booking, so there is one
 * group (spec 03, decision 1); the API's order, the oldest purchase first, is the group's order.
 */
export function MyPurchasesPage() {
  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-semibold">My purchases</h1>
      <NeedsBooking />
    </div>
  )
}

function NeedsBooking() {
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
  return (
    <section className="space-y-3">
      <h2 className="text-lg font-medium">Needs booking</h2>
      <ul className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
        {cases.data.map((consultationCase) => (
          <li key={consultationCase.id} className="flex">
            <ConsultationCaseCard consultationCase={consultationCase} />
          </li>
        ))}
      </ul>
    </section>
  )
}
