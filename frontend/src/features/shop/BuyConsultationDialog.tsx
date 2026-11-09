import { useRef, useState, type ReactNode } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router'
import type { components } from '@/api/schema'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog'
import { Label } from '@/components/ui/label'
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group'
import { usePurchaseConsultationPackage } from '@/features/consultation/queries'
import { formatMoney } from '@/lib/money'
import { problemMessage } from '@/lib/problem'
import { isOfferingGone, purchaseFailure } from './purchase-failure'
import { fetchShopOffering, shopOfferingKey, useShopOffering } from './queries'

type ConsultationPackage = components['schemas']['ConsultationPackageResponse']

const NOT_AVAILABLE = 'This offering is no longer available.'
const PACKAGE_CHANGED =
  'This package has changed or is no longer available. Please choose again.'

/**
 * UC-03 steps 2–7: the trigger (`children`) opens a dialog in two steps. **Choose** lists the offering's packages;
 * **Confirm** states the package and its price, and **Buy** buys exactly these. Then the new case opens. Closing the
 * dialog at either step (**Cancel**, Escape, the close button) creates nothing, and it opens at **Choose** again.
 *
 * After a 409 or 404 the shop reloads only when the dialog closes. Reloading at once would take the offering out of
 * the shop's list and turn its detail page into "Offering not found", and the dialog, a child of both, would vanish
 * without its message.
 */
export function BuyConsultationDialog({
  offeringId,
  offeringName,
  children,
}: {
  offeringId: string
  offeringName: string
  children: ReactNode
}) {
  const queryClient = useQueryClient()
  const shopStale = useRef(false)
  return (
    <Dialog
      onOpenChange={(open) => {
        // Stale after a failed purchase, or when the dialog couldn't load the offering any more.
        const offeringGone =
          queryClient.getQueryState(shopOfferingKey(offeringId))?.status ===
          'error'
        if (!open && (shopStale.current || offeringGone)) {
          shopStale.current = false
          void queryClient.invalidateQueries({ queryKey: ['shop'] })
        }
      }}
    >
      <DialogTrigger asChild>{children}</DialogTrigger>
      <DialogContent>
        {/* The content unmounts when the dialog closes, so the steps start again at Choose. */}
        <BuySteps
          offeringId={offeringId}
          offeringName={offeringName}
          onShopStale={() => (shopStale.current = true)}
        />
      </DialogContent>
    </Dialog>
  )
}

function BuySteps({
  offeringId,
  offeringName,
  onShopStale,
}: {
  offeringId: string
  offeringName: string
  onShopStale: () => void
}) {
  // On the detail page the packages are cached already.
  const offering = useShopOffering(offeringId)
  const purchase = usePurchaseConsultationPackage()
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const [chosenId, setChosenId] = useState<string>()
  // The package as the confirmation states it: the request sends these values, whatever reloads meanwhile.
  const [confirming, setConfirming] = useState<ConsultationPackage>()
  const [unavailable, setUnavailable] = useState(false)
  const [error, setError] = useState<string>()
  // The purchase, and after a PACKAGE_CHANGED the reload of the packages.
  const [busy, setBusy] = useState(false)

  async function buy(chosen: ConsultationPackage) {
    setError(undefined)
    setBusy(true)
    let consultationCase
    try {
      consultationCase = await purchase.mutateAsync({
        offeringId,
        packageId: chosen.id,
        price: chosen.price.amount,
        sessionCount: chosen.sessionCount,
      })
    } catch (failure) {
      await showPurchaseError(failure)
      setBusy(false)
      return
    }
    navigate(`/my/cases/${consultationCase.id}`)
  }

  async function showPurchaseError(failure: unknown) {
    const kind = purchaseFailure(failure)
    if (kind === 'failed') {
      setError('Buying failed. Please try again.')
      return
    }
    onShopStale()
    if (kind === 'offeringGone') {
      setError(problemMessage(failure, NOT_AVAILABLE))
      setUnavailable(true)
      return
    }
    // PACKAGE_CHANGED: the current packages, fetched past the cache, so that an offering that left the shop (its
    // last package removed) doesn't turn the detail page, and this dialog with it, into "Offering not found".
    try {
      const current = await fetchShopOffering(offeringId)
      queryClient.setQueryData(shopOfferingKey(offeringId), current)
    } catch (reloadFailure) {
      if (isOfferingGone(reloadFailure)) {
        setError(NOT_AVAILABLE)
        setUnavailable(true)
        return
      }
      // Otherwise Choose keeps the packages it has; buying again tells.
    }
    setError(problemMessage(failure, PACKAGE_CHANGED))
    setChosenId(undefined)
    setConfirming(undefined)
  }

  const title = (
    <DialogHeader>
      <DialogTitle>Buy {offeringName}</DialogTitle>
    </DialogHeader>
  )

  // An offering that left the shop (a 404 or a malformed id) can't be bought: only Close is left.
  const gone =
    unavailable || (offering.isError && isOfferingGone(offering.error))
  if (gone) {
    return (
      <>
        {title}
        <DialogDescription role="alert" className="text-destructive">
          {error ?? NOT_AVAILABLE}
        </DialogDescription>
        <DialogFooter>
          <CloseButton label="Close" />
        </DialogFooter>
      </>
    )
  }

  if (confirming !== undefined) {
    return (
      <>
        {title}
        <DialogDescription>
          Do you really want to buy {offeringName},{' '}
          {sessions(confirming.sessionCount)} for{' '}
          {formatMoney(confirming.price)}?
        </DialogDescription>
        {error !== undefined && (
          <p role="alert" className="text-destructive">
            {error}
          </p>
        )}
        <DialogFooter>
          <Button
            variant="outline"
            disabled={busy}
            onClick={() => {
              setError(undefined)
              setConfirming(undefined)
            }}
          >
            Back
          </Button>
          <Button disabled={busy} onClick={() => void buy(confirming)}>
            Buy
          </Button>
        </DialogFooter>
      </>
    )
  }

  const packages = offering.data?.consultation.packages ?? []
  const chosen = packages.find(({ id }) => id === chosenId)
  return (
    <>
      {title}
      <DialogDescription>Choose a package.</DialogDescription>
      {error !== undefined && (
        <p role="alert" className="text-destructive">
          {error}
        </p>
      )}
      {offering.isPending && (
        <p className="text-muted-foreground">Loading the packages…</p>
      )}
      {offering.isError && (
        <p role="alert" className="text-destructive">
          The packages could not be loaded. Close the dialog and try again.
        </p>
      )}
      {packages.length > 0 && (
        <RadioGroup
          aria-label="Package"
          value={chosen?.id ?? ''}
          onValueChange={setChosenId}
        >
          {packages.map((consultationPackage) => (
            <Label
              key={consultationPackage.id}
              htmlFor={`package-${consultationPackage.id}`}
              className="cursor-pointer rounded-md border p-3 has-[[data-state=checked]]:border-primary"
            >
              <RadioGroupItem
                id={`package-${consultationPackage.id}`}
                value={consultationPackage.id}
              />
              <span className="flex-1">
                {sessions(consultationPackage.sessionCount)}
              </span>
              <span className="font-semibold">
                {formatMoney(consultationPackage.price)}
              </span>
            </Label>
          ))}
        </RadioGroup>
      )}
      <DialogFooter>
        <CloseButton label="Cancel" />
        <Button
          disabled={chosen === undefined}
          onClick={() => {
            setError(undefined)
            setConfirming(chosen)
          }}
        >
          Continue
        </Button>
      </DialogFooter>
    </>
  )
}

function CloseButton({ label }: { label: string }) {
  return (
    <DialogClose asChild>
      <Button variant="outline">{label}</Button>
    </DialogClose>
  )
}

/** "1 session" in the singular, as the detail page writes it. */
function sessions(count: number): string {
  return count === 1 ? '1 session' : `${count} sessions`
}
