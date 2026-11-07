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
    <Card className="w-full overflow-hidden pt-0">
      <ImagePlaceholder className="aspect-video" />
      <CardHeader>
        <Badge variant="secondary">{typeLabels[offering.type]}</Badge>
        <CardTitle>
          <h2 className="text-lg">{offering.name}</h2>
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
