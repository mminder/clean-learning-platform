import type { ReactNode } from 'react'
import type { CustomerConsultationCaseSummary } from './queries'

/** The server's `PurchaseGroup`, the same on both lists. */
type Group = CustomerConsultationCaseSummary['group']

/**
 * The groups of My purchases and My cases: Upcoming, Needs booking and Past, collapsed (spec 04, decision 6; spec 06,
 * decision 18). The server decides each case's group and sends the cases in the page's order, so this only splits the
 * list by `group`, keeping the order, and hides an empty group. Both pages use it, so their groups can't drift apart.
 */
export function CaseGroups<Case extends { id: string; group: Group }>({
  cases,
  renderCard,
}: {
  cases: Case[]
  renderCard: (consultationCase: Case) => ReactNode
}) {
  const ofGroup = (group: Group) =>
    cases.filter((consultationCase) => consultationCase.group === group)
  const upcoming = ofGroup('UPCOMING')
  const needsBooking = ofGroup('NEEDS_BOOKING')
  const past = ofGroup('PAST')
  const cards = (cases: Case[]) => (
    <ul className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
      {cases.map((consultationCase) => (
        <li key={consultationCase.id} className="flex">
          {renderCard(consultationCase)}
        </li>
      ))}
    </ul>
  )
  return (
    <div className="space-y-8">
      {upcoming.length > 0 && (
        <section className="space-y-3">
          {/* Only an Upcoming card shows a time, so the hint sits next to this heading (frontend.md). */}
          <h2 className="text-lg font-medium">
            Upcoming{' '}
            <span className="text-sm font-normal text-muted-foreground">
              (Zurich time)
            </span>
          </h2>
          {cards(upcoming)}
        </section>
      )}
      {needsBooking.length > 0 && (
        <section className="space-y-3">
          <h2 className="text-lg font-medium">Needs booking</h2>
          {cards(needsBooking)}
        </section>
      )}
      {past.length > 0 && (
        <details className="space-y-3">
          <summary className="cursor-pointer text-lg font-medium">
            Past ({past.length})
          </summary>
          <div className="mt-3">{cards(past)}</div>
        </details>
      )}
    </div>
  )
}
