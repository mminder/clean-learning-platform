import type { ReactNode } from 'react'
import { Link, useParams } from 'react-router'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { isBadRequest, isNotFound } from '@/lib/api-error'
import { formatDateTime } from '@/lib/date'
import { formatDateAndTimes } from './appointment-times'
import { useExpertConsultationCase } from './expert-queries'

/**
 * UC-11's case detail for its assigned expert, following the customer's `ConsultationCasePage`: the customer's name
 * and email, the offering, the purchase terms without the price paid, the credits, and the upcoming, past and cancelled
 * appointments in Zurich time. A cancelled case says when it was cancelled in place of the credits, without its refund
 * outcome (spec 06, decision 4). The server decides the lists and each appointment's status: a running one is among the
 * upcoming ones with "Now" (decision 2), and the cancelled ones are a list of their own (decision 14). Another expert's
 * case is "Case not found", like an unknown one.
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
    </article>
  )
}

function BackToMyCases() {
  return (
    <Link to="/expert/cases" className="inline-block text-sm underline">
      Back to my cases
    </Link>
  )
}

function ZurichTime() {
  return (
    <span className="font-normal text-muted-foreground">(Zurich time)</span>
  )
}

function Term({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div>
      <dt className="text-sm text-muted-foreground">{label}</dt>
      <dd className="font-medium">{children}</dd>
    </div>
  )
}

/** A list of appointments, with "(Zurich time)" once, next to its heading. */
function Appointments({
  title,
  children,
}: {
  title: string
  children: ReactNode
}) {
  return (
    <section className="space-y-4">
      <h2 className="text-xl font-semibold">
        {title}{' '}
        <span className="text-base font-normal text-muted-foreground">
          (Zurich time)
        </span>
      </h2>
      <ul className="space-y-2">{children}</ul>
    </section>
  )
}

function AppointmentItem({
  start,
  end,
  children,
}: {
  start: string
  end: string
  children: ReactNode
}) {
  return (
    <li className="flex items-center gap-3">
      <span className="font-medium">{formatDateAndTimes(start, end)}</span>
      {children}
    </li>
  )
}
