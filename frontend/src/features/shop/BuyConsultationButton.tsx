import { Button } from '@/components/ui/button'
import { useMe } from '@/features/user/queries'
import { BuyConsultationDialog } from './BuyConsultationDialog'

/**
 * **Buy** for a consultation offering (UC-02, UC-03): a user with `CUSTOMER` opens the buy dialog. While the current
 * user loads, and for a user without `CUSTOMER`, there's no **Buy**.
 */
export function BuyConsultationButton({
  offeringId,
  offeringName,
}: {
  offeringId: string
  offeringName: string
}) {
  const me = useMe()

  // TODO(task 03 of increment 03): a visitor sees Buy, and it opens the login with a redirect to this page.
  if (!(me.data?.roles.includes('CUSTOMER') ?? false)) {
    return null
  }
  return (
    <BuyConsultationDialog offeringId={offeringId} offeringName={offeringName}>
      <Button>Buy</Button>
    </BuyConsultationDialog>
  )
}
