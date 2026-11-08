import { Link } from 'react-router'
import type { components } from '@/api/schema'
import { ImagePlaceholder } from '@/components/ImagePlaceholder'
import { Badge } from '@/components/ui/badge'
import {
  Card,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { formatMoney } from '@/lib/money'

type ShopOffering = components['schemas']['ShopOfferingResponse']

const typeLabels: Record<ShopOffering['type'], string> = {
  CONSULTATION: 'Consultation',
}

export function OfferingCard({ offering }: { offering: ShopOffering }) {
  return (
    // The title's link stretches over the whole card, so a click anywhere opens the detail page.
    <Card className="relative w-full overflow-hidden pt-0 transition-shadow focus-within:ring-2 focus-within:ring-ring hover:shadow-md">
      <ImagePlaceholder className="aspect-video" />
      <CardHeader>
        <Badge variant="secondary">{typeLabels[offering.type]}</Badge>
        <CardTitle>
          <h2 className="text-lg">
            <Link
              to={`/offerings/${offering.id}`}
              className="outline-none after:absolute after:inset-0"
            >
              {offering.name}
            </Link>
          </h2>
        </CardTitle>
        <CardDescription className="line-clamp-3">
          {offering.description}
        </CardDescription>
      </CardHeader>
      <CardFooter className="mt-auto">
        {/* The price comes from the API: for a consultation, its cheapest package. */}
        <p className="text-sm text-muted-foreground">
          from{' '}
          <span className="text-base font-semibold text-foreground">
            {formatMoney(offering.price)}
          </span>
        </p>
      </CardFooter>
    </Card>
  )
}
