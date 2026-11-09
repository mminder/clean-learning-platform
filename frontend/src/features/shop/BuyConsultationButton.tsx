import { Link, useLocation } from 'react-router'
import { Button } from '@/components/ui/button'
import { useMe } from '@/features/user/queries'
import { BuyConsultationDialog } from './BuyConsultationDialog'

/**
 * **Buy** for a consultation offering (UC-02, UC-03): a user with `CUSTOMER` opens the buy dialog, and a visitor
 * the login, which returns to this page (UC-01). While the current user loads, and for a user without `CUSTOMER`,
 * there's no **Buy**.
 */
export function BuyConsultationButton({
  offeringId,
  offeringName,
}: {
  offeringId: string
  offeringName: string
}) {
  const me = useMe()
  const location = useLocation()

  if (me.data === null) {
    // The path, search and hash, as RequireRole builds it. The dialog doesn't reopen after the login.
    const redirect = location.pathname + location.search + location.hash
    return (
      <Button asChild>
        <Link to={`/login?redirect=${encodeURIComponent(redirect)}`}>Buy</Link>
      </Button>
    )
  }
  if (!(me.data?.roles.includes('CUSTOMER') ?? false)) {
    return null
  }
  return (
    <BuyConsultationDialog offeringId={offeringId} offeringName={offeringName}>
      <Button>Buy</Button>
    </BuyConsultationDialog>
  )
}
