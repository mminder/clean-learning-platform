import { useEffect, useState } from 'react'
import {
  AlertDialog,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog'
import { Button } from '@/components/ui/button'
import { formatDateTime } from '@/lib/date'
import { problemMessage } from '@/lib/problem'
import { formatDateAndTimes } from './appointment-times'
import { cancellationFailure } from './cancellation-failure'
import {
  useCancelConsultationAppointment,
  type CustomerAppointment,
  type CustomerConsultationCase,
  type CustomerUpcomingAppointment,
} from './queries'

/** The appointment the dialog was opened for, as the page kept it: a reload that removes it doesn't change it. */
export type AppointmentToCancel = Pick<
  CustomerAppointment,
  'id' | 'start' | 'end'
>

const CHECKING = 'Checking the cancellation…'
const NOT_CHECKED =
  'The cancellation could not be checked. Please close the dialog and try again.'
const NOT_CANCELLABLE = 'This appointment can no longer be cancelled.'
const NOT_FOUND = 'This appointment was not found.'
const CANCELLING_FAILED = 'Cancelling failed. Please try again.'
const CREDIT_RETURNED = 'The session credit will be returned to your case.'

/**
 * UC-07 steps 1–5, the plan's cancellation dialog: it names the appointment, reloads the case when it opens (spec 05,
 * decision 12), and until the answer is back says "Checking the cancellation…". Then it states the outcome from the
 * reloaded case's upcoming appointment with the same id: the credit is returned, or, late, the deadline that passed and
 * that the credit is **not** refunded. **Cancel appointment** sends that outcome (decision 4); **Keep appointment** and
 * Escape change nothing. After the cancellation the dialog closes, and the case detail shows the new credits without
 * the appointment.
 *
 * The page renders it once, outside the appointment list, and keeps the appointment it was opened for, so a reload
 * that removes the appointment doesn't close it. It reads the page's case and reloads it with the case query's
 * `refetch` (`reloadCase`), so an opening sends one request. Errors follow the plan's error table: a 409 shows its
 * `detail` and what's current, and the dialog stays open.
 */
export function CancelAppointmentDialog({
  consultationCase,
  reloadCase,
  appointment,
  open,
  onOpenChange,
}: {
  consultationCase: CustomerConsultationCase
  reloadCase: () => Promise<{ isSuccess: boolean }>
  appointment: AppointmentToCancel | undefined
  open: boolean
  onOpenChange: (open: boolean) => void
}) {
  return (
    <AlertDialog open={open} onOpenChange={onOpenChange}>
      <AlertDialogContent>
        {/* The content unmounts when the dialog closes, so each opening reloads the preview. */}
        {appointment && (
          <CancelAppointmentContent
            key={appointment.id}
            consultationCase={consultationCase}
            reloadCase={reloadCase}
            appointment={appointment}
            onCancelled={() => onOpenChange(false)}
          />
        )}
      </AlertDialogContent>
    </AlertDialog>
  )
}

type Preview = 'checking' | 'checked' | 'failed'

interface Failure {
  message: string
  closeOnly: boolean
}

function CancelAppointmentContent({
  consultationCase,
  reloadCase,
  appointment,
  onCancelled,
}: {
  consultationCase: CustomerConsultationCase
  reloadCase: () => Promise<{ isSuccess: boolean }>
  appointment: AppointmentToCancel
  onCancelled: () => void
}) {
  const cancel = useCancelConsultationAppointment(consultationCase.id)
  const [preview, setPreview] = useState<Preview>('checking')
  const [failure, setFailure] = useState<Failure>()
  // The preview the request sent, while it runs and after it succeeded: the reload after the cancellation removes the
  // appointment, and the closing dialog keeps what it showed instead of saying it can no longer be cancelled.
  const [sent, setSent] = useState<CustomerUpcomingAppointment>()

  // The preview reloads when the dialog opens; an answer of a reload the dialog no longer waits for is ignored.
  useEffect(() => {
    let current = true
    void reloadCase().then((result) => {
      if (current) {
        setPreview(result.isSuccess ? 'checked' : 'failed')
      }
    })
    return () => {
      current = false
    }
  }, [reloadCase])

  const expert = consultationCase.expert?.displayName
  const upcoming =
    sent ??
    consultationCase.upcomingAppointments.find(
      ({ id }) => id === appointment.id,
    )

  async function confirm(shown: CustomerUpcomingAppointment) {
    setFailure(undefined)
    setSent(shown)
    try {
      // The mutation reloads the case before it settles: after a 409 the dialog shows what's current.
      await cancel.mutateAsync({
        appointmentId: appointment.id,
        creditRefunded: shown.creditRefundedIfCancelledNow,
      })
    } catch (error) {
      setSent(undefined)
      switch (cancellationFailure(error)) {
        case 'outcomeChanged':
          setFailure({
            message: problemMessage(error, CANCELLING_FAILED),
            closeOnly: false,
          })
          return
        case 'notCancellable':
          setFailure({
            message: problemMessage(error, CANCELLING_FAILED),
            closeOnly: true,
          })
          return
        case 'notFound':
          setFailure({ message: NOT_FOUND, closeOnly: true })
          return
        case 'failed':
          setFailure({ message: CANCELLING_FAILED, closeOnly: false })
          return
      }
    }
    onCancelled()
  }

  const title = (
    <AlertDialogTitle>
      Cancel the appointment{expert === undefined ? '' : ` with ${expert}`} on{' '}
      {formatDateAndTimes(appointment.start, appointment.end)} (Zurich time)?
    </AlertDialogTitle>
  )
  const closeOnly = (message: string) => (
    <>
      <AlertDialogHeader>
        {title}
        <AlertDialogDescription role="alert" className="text-destructive">
          {message}
        </AlertDialogDescription>
      </AlertDialogHeader>
      <AlertDialogFooter>
        <AlertDialogCancel>Close</AlertDialogCancel>
      </AlertDialogFooter>
    </>
  )

  if (failure?.closeOnly) {
    return closeOnly(failure.message)
  }
  if (preview === 'failed') {
    return closeOnly(NOT_CHECKED)
  }
  if (preview === 'checked' && !upcoming?.canCancel) {
    return closeOnly(NOT_CANCELLABLE)
  }
  return (
    <>
      <AlertDialogHeader>
        {title}
        <AlertDialogDescription>
          {preview === 'checking' || upcoming === undefined ? (
            CHECKING
          ) : upcoming.creditRefundedIfCancelledNow ? (
            CREDIT_RETURNED
          ) : (
            <>
              The free cancellation deadline passed on{' '}
              {formatDateTime(upcoming.freeCancellationUntil)}. If you cancel,
              the session credit is <strong>not</strong> refunded. Cancel
              anyway?
            </>
          )}
        </AlertDialogDescription>
      </AlertDialogHeader>
      {failure && (
        <p role="alert" className="text-destructive">
          {failure.message}
        </p>
      )}
      <AlertDialogFooter>
        <AlertDialogCancel>Keep appointment</AlertDialogCancel>
        <Button
          variant="destructive"
          disabled={
            preview === 'checking' || upcoming === undefined || cancel.isPending
          }
          onClick={() => {
            if (upcoming !== undefined) {
              void confirm(upcoming)
            }
          }}
        >
          Cancel appointment
        </Button>
      </AlertDialogFooter>
    </>
  )
}
