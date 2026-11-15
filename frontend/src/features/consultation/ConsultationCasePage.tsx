import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router'
import { cn } from 'cn'
import { ExpertPhoto } from '@/components/ExpertPhoto'
import { OfferingImage } from '@/components/OfferingImage'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { isBadRequest, isNotFound } from '@/lib/api-error'
import { formatDateTime, formatTime } from '@/lib/date'
import { formatMoney } from '@/lib/money'
import { BookAppointmentDialog } from './BookAppointmentDialog'
import {
  CancelAppointmentDialog,
  type AppointmentToCancel,
} from './CancelAppointmentDialog'
import { CancelCaseDialog } from './CancelCaseDialog'
import { AppointmentItem, Appointments, Term, ZurichTime } from './CaseParts'
import { JoinButton } from './JoinButton'
import { msUntilLinkWindowReload } from './link-window-reload'
import { NoteCard } from './NoteCard'
import {
  useCustomerConsultationCase,
  type CustomerUpcomingAppointment,
} from './queries'
import { cancelledCaseRefundText } from './refund-texts'

/**
 * UC-05's case detail, as far as a case has data yet: the offering, the expert ("Not assigned yet" before the first
 * booking), the credits, the purchase terms and the purchase time; **Book appointment** (UC-06), disabled without an
 * open credit, and **Cancel case** (UC-08) while the case can be cancelled; the expert's profile; and the upcoming and
 * past appointments, in Zurich time, each upcoming one with **Cancel** while it can be cancelled (UC-07), but no
 * free-cancellation time (spec 05, decision 1). A cancelled appointment isn't listed (decision 3). A cancelled case
 * says when it was cancelled and its refund outcome in place of the credits, and has no actions and no hint (decision
 * 6). Below the appointments, the expert's public notes under "Notes from your expert", hidden while there's none (spec
 * 06, decision 6): the server sends no private note. Each upcoming appointment has **Join** inside its link window, or
 * says when the link will be available (UC-10); the page reloads the case when the next window opens (decision 11). The
 * server decides `canBook`, `canCancel`, each appointment's status and `canCancel`, and the link window. Another
 * customer's case is "Case not found", like an unknown one.
 */
export function ConsultationCasePage() {
  // The route /my/cases/:id always has an id.
  const { id = '' } = useParams()
  const consultationCase = useCustomerConsultationCase(id)
  // The cancel dialog's state stays here, outside the list a reload changes: the appointment it was opened for stays
  // after it closes, so the closing dialog keeps its title.
  const [cancelDialog, setCancelDialog] = useState<{
    open: boolean
    appointment?: AppointmentToCancel
  }>({ open: false })
  const [cancelCaseOpen, setCancelCaseOpen] = useState(false)
  // Reloads the case when the next link window opens, so that Join appears without a reload by the customer. Each
  // answer starts a new wait, also one equal to the last, which keeps its data but not its `dataUpdatedAt`.
  const upcoming = consultationCase.data?.upcomingAppointments
  const { dataUpdatedAt, refetch } = consultationCase
  useEffect(() => {
    if (upcoming === undefined) {
      return
    }
    const wait = msUntilLinkWindowReload(upcoming, new Date())
    if (wait === undefined) {
      return
    }
    const timer = setTimeout(() => void refetch(), wait)
    return () => clearTimeout(timer)
  }, [upcoming, dataUpdatedAt, refetch])

  if (consultationCase.isPending) {
    return <p className="text-muted-foreground">Loading the case…</p>
  }
  if (consultationCase.isError) {
    // A malformed id in the URL (400) is a missing case for the customer, too.
    if (
      isNotFound(consultationCase.error) ||
      isBadRequest(consultationCase.error)
    ) {
      return (
        <div className="space-y-4">
          <h1 className="text-2xl font-semibold">Case not found</h1>
          <BackToMyPurchases />
        </div>
      )
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
    expert,
    upcomingAppointments,
    pastAppointments,
    cancellation,
    canCancel,
    canBook,
    notes,
  } = consultationCase.data
  const hasAppointment =
    upcomingAppointments.length > 0 || pastAppointments.length > 0
  return (
    <article className="space-y-8">
      <BackToMyPurchases />
      <header className="space-y-3">
        <OfferingImage
          imageId={offeringImageId}
          className="aspect-video w-full max-w-2xl rounded-xl"
        />
        <h1 className="text-3xl font-semibold">{offeringName}</h1>
      </header>
      <dl className="grid gap-4 sm:grid-cols-2">
        <Term label="Expert">{expert?.displayName ?? 'Not assigned yet'}</Term>
        {cancellation ? (
          <Term label="Status">
            Cancelled on {formatDateTime(cancellation.cancelledAt)}{' '}
            <ZurichTime />
            <span className="block font-normal">
              {cancelledCaseRefundText(cancellation, purchaseTerms.pricePaid)}
            </span>
          </Term>
        ) : (
          <Term label="Credits">
            {openCredits} of {totalCredits} open
          </Term>
        )}
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
          {formatDateTime(purchasedAt)} <ZurichTime />
        </Term>
      </dl>
      {/* A cancelled case has no actions, but the booking dialog stays mounted: a tab that booked on it shows
          CASE_CANCELLED while the reload turns the page into the cancelled case. Its content is in a portal. */}
      <div
        className={cn(
          'flex flex-wrap items-center gap-4',
          cancellation && 'hidden',
        )}
      >
        <BookAppointmentDialog consultationCase={consultationCase.data}>
          {!cancellation && (
            <Button disabled={!canBook}>Book appointment</Button>
          )}
        </BookAppointmentDialog>
        {canCancel && (
          <Button variant="outline" onClick={() => setCancelCaseOpen(true)}>
            Cancel case
          </Button>
        )}
        {/* Not after late cancellations forfeited every credit, nor on a cancelled case: nothing is left to book. */}
        {!hasAppointment && canBook && (
          <p className="text-muted-foreground">Book your first session</p>
        )}
      </div>
      {expert && (
        <section className="space-y-4">
          <h2 className="text-xl font-semibold">Your expert</h2>
          <div className="flex gap-4">
            <ExpertPhoto photoId={expert.photoId} className="size-20" />
            <div className="space-y-1">
              <h3 className="font-medium">{expert.displayName}</h3>
              <p className="max-w-prose whitespace-pre-line text-muted-foreground">
                {expert.bio}
              </p>
            </div>
          </div>
        </section>
      )}
      {upcomingAppointments.length > 0 && (
        <Appointments title="Upcoming appointments">
          {upcomingAppointments.map((appointment) => (
            <AppointmentItem key={appointment.id} {...appointment}>
              {appointment.status === 'CONSUMED' && <Badge>Now</Badge>}
              <CallLink appointment={appointment} />
              {appointment.canCancel && (
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() =>
                    setCancelDialog({
                      open: true,
                      appointment: {
                        id: appointment.id,
                        start: appointment.start,
                        end: appointment.end,
                      },
                    })
                  }
                >
                  Cancel
                </Button>
              )}
            </AppointmentItem>
          ))}
        </Appointments>
      )}
      {pastAppointments.length > 0 && (
        <Appointments title="Past appointments">
          {pastAppointments.map((appointment) => (
            <AppointmentItem key={appointment.id} {...appointment}>
              <Badge variant="secondary">Consumed</Badge>
            </AppointmentItem>
          ))}
        </Appointments>
      )}
      {notes.length > 0 && (
        <section className="space-y-4">
          <h2 className="text-xl font-semibold">
            Notes from your expert <ZurichTime className="text-base" />
          </h2>
          <ul className="space-y-3">
            {notes.map((note) => (
              <NoteCard key={note.id} note={note} />
            ))}
          </ul>
        </section>
      )}
      <CancelAppointmentDialog
        consultationCase={consultationCase.data}
        reloadCase={consultationCase.refetch}
        appointment={cancelDialog.appointment}
        open={cancelDialog.open}
        onOpenChange={(open) =>
          setCancelDialog((dialog) => ({ ...dialog, open }))
        }
      />
      <CancelCaseDialog
        consultationCase={consultationCase.data}
        reloadCase={consultationCase.refetch}
        open={cancelCaseOpen}
        onOpenChange={setCancelCaseOpen}
      />
    </article>
  )
}

function BackToMyPurchases() {
  return (
    <Link to="/my" className="inline-block text-sm underline">
      Back to my purchases
    </Link>
  )
}

/**
 * UC-10 for the customer: **Join** inside the link window, otherwise when it opens, with or without a link at the
 * expert; inside it without a link, that there's none yet (spec 06, decision 10).
 */
function CallLink({
  appointment,
}: {
  appointment: CustomerUpcomingAppointment
}) {
  if (!appointment.linkWindowOpen) {
    return (
      <span className="text-muted-foreground">
        The link will be available from{' '}
        {formatTime(appointment.linkAvailableFrom)} (Zurich time).
      </span>
    )
  }
  if (!appointment.teamsLink) {
    return (
      <span className="text-muted-foreground">
        The meeting link has not been provided yet.
      </span>
    )
  }
  return <JoinButton teamsLink={appointment.teamsLink} />
}
