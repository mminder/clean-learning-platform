import { Button } from '@/components/ui/button'
import { OfferingCard } from './OfferingCard'
import { useShopOfferings } from './queries'

export function ShopPage() {
  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-semibold">Shop</h1>
      <ShopOfferings />
    </div>
  )
}

function ShopOfferings() {
  const offerings = useShopOfferings()

  if (offerings.isPending) {
    return <p className="text-muted-foreground">Loading offerings…</p>
  }
  if (offerings.isError) {
    return (
      <div role="alert" className="space-y-3">
        <p>The offerings could not be loaded.</p>
        <Button variant="outline" onClick={() => offerings.refetch()}>
          Try again
        </Button>
      </div>
    )
  }
  if (offerings.data.length === 0) {
    return <p className="text-muted-foreground">No offerings yet.</p>
  }
  return (
    <ul className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
      {offerings.data.map((offering) => (
        <li key={offering.id} className="flex">
          <OfferingCard offering={offering} />
        </li>
      ))}
    </ul>
  )
}
