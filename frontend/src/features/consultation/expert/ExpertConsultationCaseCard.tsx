import { Link } from 'react-router'
import {
  Card,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { CaseCardState } from '@/features/consultation/CaseParts'
import type { ExpertConsultationCaseSummary } from './queries'

/**
 * A case on My cases (UC-11 step 3): the customer's name, the offering, the next appointment in Zurich time, "Next"
 * before it starts and "Now" while it runs (spec 06, decision 2), and the credits. A cancelled case says "Cancelled" in
 * place of the next appointment and the credits (decision 18); the API puts it under Past. No image.
 */
export function ExpertConsultationCaseCard({
  consultationCase,
}: {
  consultationCase: ExpertConsultationCaseSummary
}) {
  return (
    // The title's link stretches over the whole card, so a click anywhere opens the case.
    <Card className="relative w-full transition-shadow focus-within:ring-2 focus-within:ring-ring hover:shadow-md">
      <CardHeader>
        <CardTitle>
          <h3 className="text-lg">
            <Link
              to={`/expert/cases/${consultationCase.id}`}
              className="outline-none after:absolute after:inset-0"
            >
              {consultationCase.customerName}
            </Link>
          </h3>
        </CardTitle>
        <CardDescription>{consultationCase.offeringName}</CardDescription>
      </CardHeader>
      <CardFooter className="mt-auto flex-col items-start gap-1 text-sm">
        <CaseCardState consultationCase={consultationCase} />
      </CardFooter>
    </Card>
  )
}
