import { useState, type ReactNode } from 'react'
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
import { ApiError, isBadRequest, isNotFound } from '@/lib/api-error'
import { formatMoney } from '@/lib/money'
import { problemMessage } from '@/lib/problem'
import { useShopOffering } from './queries'

type ConsultationPackage = components['schemas']['ConsultationPackageResponse']

const NOT_AVAILABLE = 'This offering is no longer available.'

/**
 * UC-03 steps 2–7: the trigger (`children`) opens a dialog in two steps. **Choose** lists the offering's packages;
 * **Confirm** states the package and its price, and **Buy** buys exactly these. Then the new case opens. Closing the
 * dialog at either step (**Cancel**, Escape, the close button) creates nothing, and it opens at **Choose** again.
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
  return (
    <Dialog>
      <DialogTrigger asChild>{children}</DialogTrigger>
      <DialogContent>
        {/* The content unmounts when the dialog closes, so the steps start again at Choose. */}
        <BuySteps offeringId={offeringId} offeringName={offeringName} />
      </DialogContent>
    </Dialog>
  )
}

function BuySteps({
  offeringId,
  offeringName,
}: {
  offeringId: string
  offeringName: string
}) {
  // On the detail page the packages are cached already.
  const offering = useShopOffering(offeringId)
  const purchase = usePurchaseConsultationPackage()
  const navigate = useNavigate()
  const [chosenId, setChosenId] = useState<string>()
  // The package as the confirmation states it: the request sends these values, whatever reloads meanwhile.
  const [confirming, setConfirming] = useState<ConsultationPackage>()
  const [unavailable, setUnavailable] = useState(false)
  const [error, setError] = useState<string>()

  async function buy(chosen: ConsultationPackage) {
    setError(undefined)
    let consultationCase
    try {
      consultationCase = await purchase.mutateAsync({
        offeringId,
        packageId: chosen.id,
        price: chosen.price.amount,
        sessionCount: chosen.sessionCount,
      })
    } catch (failure) {
      showPurchaseError(failure)
      return
    }
    navigate(`/my/cases/${consultationCase.id}`)
  }

  /** The mutation has reloaded the shop before it failed, so the packages are the current ones. */
  function showPurchaseError(failure: unknown) {
    const code = failure instanceof ApiError ? failure.problem?.code : undefined
    if (code === 'PACKAGE_CHANGED') {
      setError(problemMessage(failure, NOT_AVAILABLE))
      setChosenId(undefined)
      setConfirming(undefined)
    } else if (code === 'OFFERING_NOT_AVAILABLE') {
      setError(problemMessage(failure, NOT_AVAILABLE))
      setUnavailable(true)
    } else if (isNotFound(failure)) {
      setError(NOT_AVAILABLE)
      setUnavailable(true)
    } else {
      setError('Buying failed. Please try again.')
    }
  }

  const title = (
    <DialogHeader>
      <DialogTitle>Buy {offeringName}</DialogTitle>
    </DialogHeader>
  )

  // An offering that left the shop (a 404 or a malformed id) can't be bought: only Close is left.
  const gone =
    unavailable ||
    (offering.isError &&
      (isNotFound(offering.error) || isBadRequest(offering.error)))
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
            disabled={purchase.isPending}
            onClick={() => {
              setError(undefined)
              setConfirming(undefined)
            }}
          >
            Back
          </Button>
          <Button
            disabled={purchase.isPending}
            onClick={() => void buy(confirming)}
          >
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
