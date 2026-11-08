import { useState } from 'react'
import { useNavigate } from 'react-router'
import { ConfirmDialog } from '@/components/ConfirmDialog'
import { Button } from '@/components/ui/button'
import { problemMessage } from '@/lib/problem'
import {
  useChangeConsultationOfferingStatus,
  useDeleteConsultationOffering,
  type ConsultationOffering,
  type StatusChange,
} from './queries'

/**
 * UC-16 step 5: the status changes the offering's status allows, each after a confirmation that states the
 * consequence. A draft can be published or deleted, a published offering unpublished or archived, and an archived
 * offering nothing. When the server says no (409), the message shows here, above the form, and the offering reloads.
 */
export function OfferingStatusActions({
  offering,
}: {
  offering: ConsultationOffering
}) {
  const { id, name, status } = offering
  const changeStatus = useChangeConsultationOfferingStatus(id)
  const deleteOffering = useDeleteConsultationOffering(id)
  const navigate = useNavigate()
  const [error, setError] = useState<string>()
  const pending = changeStatus.isPending || deleteOffering.isPending

  async function change(statusChange: StatusChange) {
    setError(undefined)
    try {
      await changeStatus.mutateAsync(statusChange)
    } catch (failure) {
      setError(
        problemMessage(
          failure,
          'Changing the status failed. Please try again.',
        ),
      )
    }
  }

  async function remove() {
    setError(undefined)
    try {
      await deleteOffering.mutateAsync()
    } catch (failure) {
      setError(problemMessage(failure, 'Deleting failed. Please try again.'))
      return
    }
    navigate('/admin/consultations')
  }

  // A message stays when the reload shows the offering archived meanwhile: it says why nothing changed.
  if (status === 'ARCHIVED' && error === undefined) {
    return null
  }
  return (
    <div className="space-y-3">
      {status !== 'ARCHIVED' && (
        <div className="flex flex-wrap gap-3">
          {status === 'DRAFT' && (
            <>
              <ConfirmDialog
                title={`Publish ${name}?`}
                description="It appears in the shop, and customers can buy it."
                confirmLabel="Publish"
                onConfirm={() => void change('publish')}
              >
                <Button disabled={pending}>Publish</Button>
              </ConfirmDialog>
              <ConfirmDialog
                title={`Delete ${name}?`}
                description="The offering is deleted for good. This can't be undone."
                confirmLabel="Delete"
                destructive
                onConfirm={() => void remove()}
              >
                <Button variant="outline" disabled={pending}>
                  Delete
                </Button>
              </ConfirmDialog>
            </>
          )}
          {status === 'PUBLISHED' && (
            <>
              <ConfirmDialog
                title={`Unpublish ${name}?`}
                description="It disappears from the shop and becomes a draft again."
                confirmLabel="Unpublish"
                onConfirm={() => void change('unpublish')}
              >
                <Button variant="outline" disabled={pending}>
                  Unpublish
                </Button>
              </ConfirmDialog>
              <ConfirmDialog
                title={`Archive ${name}?`}
                description="It disappears from the shop for good and can't be changed any more. Customers who bought it keep their cases."
                confirmLabel="Archive"
                destructive
                onConfirm={() => void change('archive')}
              >
                <Button variant="outline" disabled={pending}>
                  Archive
                </Button>
              </ConfirmDialog>
            </>
          )}
        </div>
      )}
      {error !== undefined && (
        <p role="alert" className="text-destructive">
          {error}
        </p>
      )}
    </div>
  )
}
