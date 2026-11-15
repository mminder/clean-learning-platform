import { Link } from 'react-router'
import { Button } from '@/components/ui/button'
import { CaseGroups } from './CaseGroups'
import { ConsultationCaseCard } from './ConsultationCaseCard'
import { useCustomerConsultationCases } from './queries'

/** UC-05's landing page, for cases: the customer's cases in their groups (`CaseGroups`). */
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
  return (
    <CaseGroups
      cases={cases.data}
      renderCard={(consultationCase) => (
        <ConsultationCaseCard consultationCase={consultationCase} />
      )}
    />
  )
}
