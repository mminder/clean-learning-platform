import { Link, useParams } from 'react-router'
import type { components } from '@/api/schema'
import { ImagePlaceholder } from '@/components/ImagePlaceholder'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { isNotFound } from '@/lib/api-error'
import { formatMoney } from '@/lib/money'
import { useShopOffering } from './queries'

type ConsultationOfferingDetail =
  components['schemas']['ConsultationOfferingDetailResponse']

export function OfferingDetailPage() {
  // The route /offerings/:id always has an id.
  const { id = '' } = useParams()
  const offering = useShopOffering(id)

  if (offering.isPending) {
    return <p className="text-muted-foreground">Loading the offering…</p>
  }
  if (offering.isError) {
    if (isNotFound(offering.error)) {
      return (
        <div className="space-y-4">
          <h1 className="text-2xl font-semibold">Offering not found</h1>
          <BackToShop />
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

  const { name, description, consultation } = offering.data
  return (
    <article className="space-y-8">
      <BackToShop />
      <header className="space-y-3">
        <Badge variant="secondary">Consultation</Badge>
        <h1 className="text-3xl font-semibold">{name}</h1>
        <p className="max-w-prose whitespace-pre-line text-muted-foreground">
          {description}
        </p>
      </header>
      <ConsultationDetail consultation={consultation} />
    </article>
  )
}

function ConsultationDetail({
  consultation,
}: {
  consultation: ConsultationOfferingDetail
}) {
  return (
    <>
      <dl className="grid gap-4 sm:grid-cols-2">
        <div>
          <dt className="text-sm text-muted-foreground">Session duration</dt>
          <dd className="font-medium">
            {consultation.sessionDurationMinutes} min
          </dd>
        </div>
        <div>
          <dt className="text-sm text-muted-foreground">Cancellation</dt>
          <dd className="font-medium">
            Free cancellation up to {consultation.cancellationDeadlineHours} h
            before a session
          </dd>
        </div>
      </dl>

      <section className="space-y-4">
        <h2 className="text-xl font-semibold">Packages</h2>
        <ul className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {consultation.packages.map((consultationPackage) => (
            <li key={consultationPackage.id}>
              <Card>
                <CardContent className="space-y-1">
                  <p className="font-medium">
                    {consultationPackage.sessionCount === 1
                      ? '1 session'
                      : `${consultationPackage.sessionCount} sessions`}
                  </p>
                  <p className="text-lg font-semibold">
                    {formatMoney(consultationPackage.price)}
                  </p>
                </CardContent>
              </Card>
            </li>
          ))}
        </ul>
      </section>

      <section className="space-y-4">
        <h2 className="text-xl font-semibold">Experts</h2>
        <ul className="space-y-6">
          {consultation.eligibleExperts.map((expert) => (
            <li key={expert.id} className="flex gap-4">
              <ImagePlaceholder className="size-20 shrink-0 rounded-full" />
              <div className="space-y-1">
                <h3 className="font-medium">{expert.displayName}</h3>
                <p className="max-w-prose whitespace-pre-line text-muted-foreground">
                  {expert.bio}
                </p>
              </div>
            </li>
          ))}
        </ul>
      </section>
    </>
  )
}

function BackToShop() {
  return (
    <Link to="/" className="inline-block text-sm underline">
      Back to the shop
    </Link>
  )
}
