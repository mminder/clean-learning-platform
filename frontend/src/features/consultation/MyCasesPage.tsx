import { Button } from '@/components/ui/button'
import { CaseGroups } from './CaseGroups'
import { ExpertConsultationCaseCard } from './ExpertConsultationCaseCard'
import { useExpertConsultationCases } from './expert-queries'

/** UC-11's landing page for experts: the cases they're assigned to, in the groups of My purchases (`CaseGroups`). */
export function MyCasesPage() {
  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-semibold">My cases</h1>
      <Groups />
    </div>
  )
}

function Groups() {
  const cases = useExpertConsultationCases()

  if (cases.isPending) {
    return <p className="text-muted-foreground">Loading your cases…</p>
  }
  if (cases.isError) {
    return (
      <div role="alert" className="space-y-3">
        <p>Your cases could not be loaded.</p>
        <Button variant="outline" onClick={() => cases.refetch()}>
          Try again
        </Button>
      </div>
    )
  }
  if (cases.data.length === 0) {
    return (
      <p className="text-muted-foreground">
        No cases yet. A case appears here once a customer books you.
      </p>
    )
  }
  return (
    <CaseGroups
      cases={cases.data}
      renderCard={(consultationCase) => (
        <ExpertConsultationCaseCard consultationCase={consultationCase} />
      )}
    />
  )
}
