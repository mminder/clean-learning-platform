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
import { problemMessage } from '@/lib/problem'
import { formatDateAndTimes } from './appointment-times'
import { cancellationFailure } from './cancellation-failure'
import {
  useCancelConsultationCase,
  type CustomerConsultationCase,
} from './queries'
import { nonRefundableReason, refundedText } from './refund-texts'

const TITLE = 'Cancel this case?'
const CHECKING = 'Checking the cancellation…'
const NOT_CHECKED =
  'The cancellation could not be checked. Please close the dialog and try again.'
const NOT_CANCELLABLE = 'This case can no longer be cancelled.'
const NOT_FOUND = 'This case was not found.'
const CANCELLING_FAILED = 'Cancelling failed. Please try again.'

/**
 * UC-08 steps 1–5, the plan's cancellation dialog: it reloads the case when it opens (spec 05, decision 12), and until
 * the answer is back says "Checking the cancellation…". Then it states the outcome from the reloaded case's
 * `refundIfCancelledNow`: the purchase price would be refunded, or would **not** be, and why; and it lists the
 * appointments cancelled with the case, the upcoming ones with `canCancel`. **Cancel case** sends that outcome
 * (decision 4); **Keep case** and Escape change nothing. After the cancellation the dialog closes, and the case detail
 * shows the cancelled case.
 *
 * The page renders it once, outside what a reload changes, so a reload that removes **Cancel case** doesn't close it.
 * It reads the page's case and reloads it with the case query's `refetch` (`reloadCase`), so an opening sends one
 * request. Errors follow the plan's error table: a 409 shows its `detail` and what's current, and the dialog stays
 * open.
 */
export function CancelCaseDialog({
  consultationCase,
  reloadCase,
  open,
  onOpenChange,
}: {
  consultationCase: CustomerConsultationCase
  reloadCase: () => Promise<{ isSuccess: boolean }>
  open: boolean
  onOpenChange: (open: boolean) => void
}) {
  return (
    <AlertDialog open={open} onOpenChange={onOpenChange}>
      <AlertDialogContent>
        {/* The content unmounts when the dialog closes, so each opening reloads the preview. */}
        <CancelCaseContent
          consultationCase={consultationCase}
          reloadCase={reloadCase}
          onCancelled={() => onOpenChange(false)}
        />
      </AlertDialogContent>
    </AlertDialog>
  )
}

type Preview = 'checking' | 'checked' | 'failed'

interface Failure {
  message: string
  closeOnly: boolean
}

function CancelCaseContent({
  consultationCase,
  reloadCase,
  onCancelled,
}: {
  consultationCase: CustomerConsultationCase
  reloadCase: () => Promise<{ isSuccess: boolean }>
  onCancelled: () => void
}) {
  const cancel = useCancelConsultationCase(consultationCase.id)
  const [preview, setPreview] = useState<Preview>('checking')
  const [failure, setFailure] = useState<Failure>()
  // The case as the request sent its outcome, while it runs and after it succeeded: the reload after the cancellation
  // has no `canCancel`, and the closing dialog keeps what it showed instead of saying it can no longer be cancelled.
  const [sent, setSent] = useState<CustomerConsultationCase>()

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

  const shown = sent ?? consultationCase
  const refund = shown.refundIfCancelledNow
  const expert = shown.expert?.displayName
  // Exactly the ones the cancellation cancels: the upcoming appointments that haven't started.
  const cancelledWithTheCase = shown.upcomingAppointments.filter(
    ({ canCancel }) => canCancel,
  )

  async function confirm(confirmed: CustomerConsultationCase) {
    if (!confirmed.refundIfCancelledNow) {
      return
    }
    setFailure(undefined)
    setSent(confirmed)
    try {
      // The mutation reloads the case before it settles: after a 409 the dialog shows what's current.
      await cancel.mutateAsync(confirmed.refundIfCancelledNow.refundEligible)
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

  const closeOnly = (message: string) => (
    <>
      <AlertDialogHeader>
        <AlertDialogTitle>{TITLE}</AlertDialogTitle>
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
  if (preview === 'checked' && !(shown.canCancel && refund)) {
    return closeOnly(NOT_CANCELLABLE)
  }
  const checking = preview === 'checking' || !refund
  return (
    <>
      <AlertDialogHeader>
        <AlertDialogTitle>{TITLE}</AlertDialogTitle>
        <AlertDialogDescription>
          {checking ? (
            CHECKING
          ) : refund.refundEligible ? (
            refundedText(shown.purchaseTerms.pricePaid)
          ) : (
            <>
              The purchase price would <strong>not</strong> be refunded because{' '}
              {nonRefundableReason(refund)} Cancel anyway?
            </>
          )}
        </AlertDialogDescription>
      </AlertDialogHeader>
      {!checking && cancelledWithTheCase.length > 0 && (
        <div className="space-y-2 text-sm">
          <p>
            These appointments are cancelled with the case{' '}
            <span className="text-muted-foreground">(Zurich time)</span>:
          </p>
          <ul className="list-disc space-y-1 pl-5">
            {cancelledWithTheCase.map((appointment) => (
              <li key={appointment.id}>
                {formatDateAndTimes(appointment.start, appointment.end)}
                {expert === undefined ? '' : ` with ${expert}`}
              </li>
            ))}
          </ul>
        </div>
      )}
      {failure && (
        <p role="alert" className="text-destructive">
          {failure.message}
        </p>
      )}
      <AlertDialogFooter>
        <AlertDialogCancel>Keep case</AlertDialogCancel>
        <Button
          variant="destructive"
          disabled={checking || cancel.isPending}
          onClick={() => void confirm(shown)}
        >
          Cancel case
        </Button>
      </AlertDialogFooter>
    </>
  )
}
