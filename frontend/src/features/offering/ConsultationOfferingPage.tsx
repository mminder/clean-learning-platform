import { useParams } from 'react-router'
import { Button } from '@/components/ui/button'
import { isBadRequest, isNotFound } from '@/lib/api-error'
import { BackToConsultations } from './BackToConsultations'
import { ConsultationOfferingForm } from './ConsultationOfferingForm'
import { OfferingImageSection } from './OfferingImageSection'
import { OfferingStatusActions } from './OfferingStatusActions'
import { OfferingStatusBadge } from './OfferingStatusBadge'
import {
  toRequest,
  useConsultationOffering,
  useEditConsultationOffering,
} from './queries'

/**
 * UC-16 steps 2–5: an existing offering, with its status, the status changes it allows, its image and its form. The
 * image changes at once, apart from the form's **Save**. An archived offering is read-only.
 */
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

  const { name, status } = offering.data
  const archived = status === 'ARCHIVED'
  return (
    <div className="space-y-6">
      <BackToConsultations />
      <header className="flex flex-wrap items-center gap-3">
        <h1 className="text-2xl font-semibold">{name}</h1>
        <OfferingStatusBadge status={status} />
      </header>
      <OfferingStatusActions offering={offering.data} />
      {archived && (
        <p className="text-muted-foreground">
          This offering is archived and can't be changed any more.
        </p>
      )}
      <OfferingImageSection offering={offering.data} />
      <ConsultationOfferingForm
        // A new form for another offering; a reload of this one keeps what the admin is typing. Archived, the form
        // starts again from the saved offering: unsaved edits can't be saved any more and mustn't look saved.
        key={archived ? `${id}-archived` : id}
        defaultValues={toRequest(offering.data)}
        readOnly={archived}
        onSave={(changed) => editOffering.mutateAsync(changed)}
      />
    </div>
  )
}
