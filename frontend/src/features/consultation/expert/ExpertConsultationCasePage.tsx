import { useState } from 'react'
import { Link, useParams } from 'react-router'
import { ConfirmDialog } from '@/components/ConfirmDialog'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import {
  AppointmentItem,
  Appointments,
  Term,
  ZurichTime,
} from '@/features/consultation/CaseParts'
import { JoinButton } from '@/features/consultation/JoinButton'
import { NoteCard } from '@/features/consultation/NoteCard'
import { isBadRequest, isNotFound } from '@/lib/api-error'
import { formatDateTime } from '@/lib/date'
import {
  useDeleteNote,
  useExpertConsultationCase,
  type ExpertNote,
} from './queries'
import { NoteDialog } from './NoteDialog'

/**
 * UC-11's case detail for its assigned expert, following the customer's `ConsultationCasePage`: the customer's name
 * and email, the offering, the purchase terms without the price paid, the credits, and the upcoming, past and cancelled
 * appointments in Zurich time. A cancelled case says when it was cancelled in place of the credits, without its refund
 * outcome (spec 06, decision 4). The server decides the lists and each appointment's status: a running one is among the
 * upcoming ones with "Now" (decision 2), and the cancelled ones are a list of their own (decision 14). Another expert's
 * case is "Case not found", like an unknown one. Each upcoming appointment has **Join** with the expert's own Teams
 * link, at any time, or asks for one on **Profile** (UC-10, spec 06, decision 10). Below them, the notes, newest first,
 * each with its visibility, **Edit** and **Delete**, and **Add note** (UC-15).
 */
export function ExpertConsultationCasePage() {
  // The route /expert/cases/:id always has an id.
  const { id = '' } = useParams()
  const consultationCase = useExpertConsultationCase(id)

  if (consultationCase.isPending) {
    return <p className="text-muted-foreground">Loading the case…</p>
  }
  if (consultationCase.isError) {
    // A malformed id in the URL (400) is a missing case for the expert, too.
    if (
      isNotFound(consultationCase.error) ||
      isBadRequest(consultationCase.error)
    ) {
      return (
        <div className="space-y-4">
          <h1 className="text-2xl font-semibold">Case not found</h1>
          <BackToMyCases />
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
    customerName,
    customerEmail,
    offeringName,
    sessionCount,
    sessionDurationMinutes,
    cancellationDeadlineHours,
    purchasedAt,
    openCredits,
    totalCredits,
    cancelledAt,
    upcomingAppointments,
    pastAppointments,
    cancelledAppointments,
    notes,
    teamsLink,
  } = consultationCase.data
  return (
    <article className="space-y-8">
      <BackToMyCases />
      <header className="space-y-1">
        <h1 className="text-3xl font-semibold">{customerName}</h1>
        <p>
          <a href={`mailto:${customerEmail}`} className="underline">
            {customerEmail}
          </a>
        </p>
        <p className="text-muted-foreground">{offeringName}</p>
      </header>
      <dl className="grid gap-4 sm:grid-cols-2">
        {cancelledAt ? (
          <Term label="Status">
            Cancelled on {formatDateTime(cancelledAt)} <ZurichTime />
          </Term>
        ) : (
          <Term label="Credits">
            {openCredits} of {totalCredits} credits open
          </Term>
        )}
        <Term label="Sessions">
          {sessionCount} {sessionCount === 1 ? 'session' : 'sessions'}
        </Term>
        <Term label="Session duration">
          {sessionDurationMinutes} minutes per session
        </Term>
        <Term label="Cancellation">
          Free cancellation up to {cancellationDeadlineHours} h before a session
        </Term>
        <Term label="Bought">
          {formatDateTime(purchasedAt)} <ZurichTime />
        </Term>
      </dl>
      {upcomingAppointments.length > 0 && (
        <Appointments title="Upcoming appointments">
          {upcomingAppointments.map((appointment) => (
            <AppointmentItem key={appointment.id} {...appointment}>
              {appointment.status === 'CONSUMED' && <Badge>Now</Badge>}
              {teamsLink ? (
                <JoinButton teamsLink={teamsLink} />
              ) : (
                <span className="text-muted-foreground">
                  Add your Teams link in{' '}
                  <Link to="/expert/profile" className="underline">
                    your profile
                  </Link>
                  .
                </span>
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
      {cancelledAppointments.length > 0 && (
        <Appointments title="Cancelled appointments">
          {cancelledAppointments.map((appointment) => (
            <AppointmentItem key={appointment.id} {...appointment}>
              <Badge variant="outline">
                {appointment.creditRefunded
                  ? 'Cancelled, credit refunded'
                  : 'Cancelled, credit forfeited'}
              </Badge>
            </AppointmentItem>
          ))}
        </Appointments>
      )}
      <Notes caseId={consultationCase.data.id} notes={notes} />
    </article>
  )
}

/**
 * The notes with **Add note**, **Edit** and **Delete**. The page renders one `NoteDialog` and keeps the note it was
 * opened for. A delete asks first; a note deleted meanwhile, in another tab, says so above the notes, which reload.
 */
function Notes({ caseId, notes }: { caseId: string; notes: ExpertNote[] }) {
  const deleteNote = useDeleteNote(caseId)
  const [dialog, setDialog] = useState<{
    open: boolean
    note?: ExpertNote
  }>({ open: false })
  const [deleteError, setDeleteError] = useState<string>()
  const pending = deleteNote.isPending

  function openDialog(note?: ExpertNote) {
    setDeleteError(undefined)
    setDialog({ open: true, note })
  }

  async function remove(noteId: string) {
    setDeleteError(undefined)
    try {
      await deleteNote.mutateAsync(noteId)
    } catch (error) {
      setDeleteError(
        isNotFound(error)
          ? 'This note no longer exists.'
          : 'Deleting failed. Please try again.',
      )
    }
  }

  return (
    <section className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h2 className="text-xl font-semibold">
          Notes <ZurichTime className="text-base" />
        </h2>
        <Button disabled={pending} onClick={() => openDialog()}>
          Add note
        </Button>
      </div>
      {deleteError !== undefined && (
        <p role="alert" className="text-destructive">
          {deleteError}
        </p>
      )}
      <ul className="space-y-3">
        {notes.map((note) => (
          <NoteCard key={note.id} note={note}>
            <Badge
              variant={note.visibility === 'PUBLIC' ? 'default' : 'outline'}
            >
              {note.visibility === 'PUBLIC' ? 'Public' : 'Private'}
            </Badge>
            <span className="ml-auto flex gap-2">
              <Button
                variant="outline"
                size="sm"
                disabled={pending}
                onClick={() => openDialog(note)}
              >
                Edit
              </Button>
              <ConfirmDialog
                title={`Delete the note "${note.title}"?`}
                description="This can't be undone."
                confirmLabel="Delete"
                destructive
                onConfirm={() => void remove(note.id)}
              >
                <Button variant="outline" size="sm" disabled={pending}>
                  Delete
                </Button>
              </ConfirmDialog>
            </span>
          </NoteCard>
        ))}
      </ul>
      <NoteDialog
        caseId={caseId}
        note={dialog.note}
        open={dialog.open}
        onOpenChange={(open) => setDialog((current) => ({ ...current, open }))}
      />
    </section>
  )
}

function BackToMyCases() {
  return (
    <Link to="/expert/cases" className="inline-block text-sm underline">
      Back to my cases
    </Link>
  )
}
