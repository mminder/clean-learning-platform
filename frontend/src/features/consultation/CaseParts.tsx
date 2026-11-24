import type { ReactNode } from 'react'
import { cn } from 'cn'
import type { components } from '@/api/schema'
import { formatDateAndTimes } from './appointment-times'

type CaseSummary =
  components['schemas']['CustomerConsultationCaseSummaryResponse']

/** The parts the customer's and the expert's case pages and cards share. */

/** The "(Zurich time)" hint next to a time or a heading. */
export function ZurichTime({ className }: { className?: string }) {
  return (
    <span className={cn('font-normal text-muted-foreground', className)}>
      (Zurich time)
    </span>
  )
}

export function Term({
  label,
  children,
}: {
  label: string
  children: ReactNode
}) {
  return (
    <div>
      <dt className="text-sm text-muted-foreground">{label}</dt>
      <dd className="font-medium">{children}</dd>
    </div>
  )
}

/** A list of appointments, with "(Zurich time)" once, next to its heading. */
export function Appointments({
  title,
  children,
}: {
  title: string
  children: ReactNode
}) {
  return (
    <section className="space-y-4">
      <h2 className="text-xl font-semibold">
        {title} <ZurichTime className="text-base" />
      </h2>
      <ul className="space-y-2">{children}</ul>
    </section>
  )
}

export function AppointmentItem({
  start,
  end,
  children,
}: {
  start: string
  end: string
  children: ReactNode
}) {
  return (
    <li className="flex flex-wrap items-center gap-3">
      <span className="font-medium">{formatDateAndTimes(start, end)}</span>
      {children}
    </li>
  )
}

/**
 * A case card's state: "Cancelled", or the next appointment, "Next" before it starts and "Now" while it runs, and the
 * credits. The expert's case summary has the same fields.
 */
export function CaseCardState({
  consultationCase,
}: {
  consultationCase: Pick<
    CaseSummary,
    'cancelledAt' | 'nextAppointment' | 'openCredits' | 'totalCredits'
  >
}) {
  const next = consultationCase.nextAppointment
  if (consultationCase.cancelledAt) {
    return <p className="font-medium">Cancelled</p>
  }
  return (
    <>
      {next && (
        <p className="font-medium">
          {next.status === 'CONSUMED' ? 'Now' : 'Next'}:{' '}
          {formatDateAndTimes(next.start, next.end)}
        </p>
      )}
      <p className="font-medium">
        {consultationCase.openCredits} of {consultationCase.totalCredits}{' '}
        credits open
      </p>
    </>
  )
}
