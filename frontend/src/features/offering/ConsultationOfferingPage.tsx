import { useParams } from 'react-router'
import { Button } from '@/components/ui/button'
import { isBadRequest, isNotFound } from '@/lib/api-error'
import { BackToConsultations } from './BackToConsultations'
import { ConsultationOfferingForm } from './ConsultationOfferingForm'
import { OfferingStatusBadge } from './OfferingStatusBadge'
import { useConsultationOffering, useEditConsultationOffering } from './queries'

/** UC-16 steps 2–4: an existing offering, with its status and its form. */
export function ConsultationOfferingPage() {
  // The route /admin/consultations/:id always has an id.
  const { id = '' } = useParams()
  const offering = useConsultationOffering(id)
  const editOffering = useEditConsultationOffering(id)

  if (offering.isPending) {
    return <p className="text-muted-foreground">Loading the offering…</p>
  }
  if (offering.isError) {
    // A malformed id in the URL (400) is a missing offering for the admin, too.
    if (isNotFound(offering.error) || isBadRequest(offering.error)) {
      return (
        <div className="space-y-4">
          <h1 className="text-2xl font-semibold">Offering not found</h1>
          <BackToConsultations />
        </div>
      )
    }
    return (
      <div role="alert" className="space-y-3">
        <p>The offering could not be loaded.</p>
        <Button variant="outline" onClick={() => offering.refetch()}>
          Try again
        </Button>
      </div>
    )
  }

  const {
    name,
    status,
    description,
    sessionDurationMinutes,
    cancellationDeadlineHours,
  } = offering.data
  return (
    <div className="space-y-6">
      <BackToConsultations />
      <header className="flex flex-wrap items-center gap-3">
        <h1 className="text-2xl font-semibold">{name}</h1>
        <OfferingStatusBadge status={status} />
      </header>
      <ConsultationOfferingForm
        // A new form for another offering; a reload of this one keeps what the admin is typing.
        key={id}
        defaultValues={{
          name,
          description,
          sessionDurationMinutes,
          cancellationDeadlineHours,
        }}
        onSave={(changed) => editOffering.mutateAsync(changed)}
      />
    </div>
  )
}
