import { useEffect, useState, type ReactNode } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { ImagePlaceholder } from '@/components/ImagePlaceholder'
import { Button } from '@/components/ui/button'
import { Calendar } from '@/components/ui/calendar'
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
import { isConflict } from '@/lib/api-error'
import { formatDateTime, fromCalendarDay, toCalendarDay } from '@/lib/date'
import { problemMessage } from '@/lib/problem'
import { formatDateAndTimes, formatTimes } from './appointment-times'
import { slotsByDate } from './bookable-slots'
import { bookingFailure } from './booking-failure'
import {
  bookableSlotsKey,
  customerConsultationCaseKey,
  fetchBookableSlots,
  useBookableSlots,
  useBookConsultationAppointment,
  useEligibleExperts,
  type BookableSlot,
  type CustomerConsultationCase,
  type EligibleExpert,
} from './queries'

type Step = 'expert' | 'date' | 'time' | 'confirm'

/** The expert the dialog books: the case's, or the one the customer picked on the first booking. */
type Expert = Pick<EligibleExpert, 'id' | 'displayName'>

const NO_EXPERTS =
  'No experts are available for this offering yet. Please try again later.'
const NO_SLOTS = 'No free times available at the moment.'
const NOT_FOUND = 'This case was not found.'
const BOOKING_FAILED = 'Booking failed. Please try again.'
const TIMES_FAILED = 'The times could not be loaded. Please try again.'
const NOT_FREE_TO_CANCEL =
  "This time is within the cancellation deadline, so it can't be cancelled for free."

/**
 * UC-06 steps 1–6: the trigger (`children`) opens a dialog in steps (spec 04, decision 4). **Expert**, only while the
 * case has no expert, lists the offering's eligible experts; **Date** is a calendar where only dates with bookable
 * slots can be picked; **Time** lists that date's slots; **Confirm** states the expert, the date, the times, the
 * duration and the free-cancellation time, or, for a slot without one, that it can't be cancelled for free (spec 04,
 * decision 7), and **Book** books exactly these. Then the dialog closes, and the case detail shows the appointment.
 * Closing the dialog at any step (**Cancel**, Escape, the close button) books nothing, and it opens at the first step
 * again.
 *
 * Errors follow the plan's error table. The case reloads after a 409 while the dialog stays open: the dialog isn't a
 * child of something the reload removes. Without a trigger, as on a cancelled case, the dialog stays mounted, so a
 * reload that cancels the case doesn't close it while it says so (`CASE_CANCELLED`, UC-08).
 */
export function BookAppointmentDialog({
  consultationCase,
  children,
}: {
  consultationCase: CustomerConsultationCase
  children: ReactNode
}) {
  const [open, setOpen] = useState(false)
  return (
    <Dialog open={open} onOpenChange={setOpen}>
      {children && <DialogTrigger asChild>{children}</DialogTrigger>}
      <DialogContent>
        {/* The content unmounts when the dialog closes, so the steps start again. */}
        <BookingSteps
          consultationCase={consultationCase}
          onBooked={() => setOpen(false)}
        />
      </DialogContent>
    </Dialog>
  )
}

function BookingSteps({
  consultationCase,
  onBooked,
}: {
  consultationCase: CustomerConsultationCase
  onBooked: () => void
}) {
  const caseId = consultationCase.id
  const queryClient = useQueryClient()
  // The expert as the dialog opened, not as the case reloads meanwhile: the booking names the expert the customer saw.
  const [expert, setExpert] = useState<Expert | undefined>(
    consultationCase.expert ?? undefined,
  )
  const [hasExpertStep, setHasExpertStep] = useState(!consultationCase.expert)
  const [step, setStep] = useState<Step>(hasExpertStep ? 'expert' : 'date')
  const [chosenExpertId, setChosenExpertId] = useState<string>()
  const [chosenDate, setChosenDate] = useState<string>()
  const [chosenStart, setChosenStart] = useState<string>()
  // The slot as the confirmation states it: the request sends it, whatever reloads meanwhile.
  const [confirming, setConfirming] = useState<BookableSlot>()
  const [error, setError] = useState<string>()
  const [closeOnly, setCloseOnly] = useState(false)
  // Loading the times after the expert step, or the booking.
  const [busy, setBusy] = useState(false)

  const experts = useEligibleExperts(caseId, hasExpertStep)
  const slots = useBookableSlots(
    caseId,
    step === 'expert' ? undefined : expert?.id,
  )
  const book = useBookConsultationAppointment(caseId)
  const byDate = slotsByDate(slots.data ?? [])
  // The times of a case cancelled in another tab (UC-06's error flow): only Close is left, and the case reloads, so the
  // page behind shows the cancelled case. Only the case: its slots would answer the same again.
  const slotsOfACancelledCase =
    slots.isError && bookingFailure(slots.error) === 'caseCancelled'
  useEffect(() => {
    if (slotsOfACancelledCase) {
      void queryClient.invalidateQueries({
        queryKey: customerConsultationCaseKey(caseId),
        exact: true,
      })
    }
  }, [slotsOfACancelledCase, queryClient, caseId])

  function resetDateAndTime() {
    setChosenDate(undefined)
    setChosenStart(undefined)
    setConfirming(undefined)
  }

  /** After EXPERT_CHANGED the reloaded case has its expert: the dialog goes on with them, to the dates. */
  function continueWithTheCasesExpert() {
    const reloaded = queryClient.getQueryData<CustomerConsultationCase>(
      customerConsultationCaseKey(caseId),
    )
    if (reloaded?.expert) {
      setExpert(reloaded.expert)
      setHasExpertStep(false)
      setStep('date')
    } else {
      // The case couldn't be reloaded: the customer picks again.
      setExpert(undefined)
      setChosenExpertId(undefined)
      setStep('expert')
    }
  }

  /** The plan's error table. `fallback` is the message for any other failure, which stays where it is. */
  function showFailure(failure: unknown, fallback: string) {
    switch (bookingFailure(failure)) {
      case 'slotTaken':
        setError(problemMessage(failure, fallback))
        resetDateAndTime()
        setStep('date')
        return
      case 'expertNotEligible':
        setError(problemMessage(failure, fallback))
        resetDateAndTime()
        setExpert(undefined)
        setChosenExpertId(undefined)
        setStep('expert')
        return
      case 'expertChanged':
        setError(problemMessage(failure, fallback))
        resetDateAndTime()
        continueWithTheCasesExpert()
        return
      case 'noOpenCredits':
      case 'caseCancelled':
        setError(problemMessage(failure, fallback))
        setCloseOnly(true)
        return
      case 'caseNotFound':
        setError(NOT_FOUND)
        setCloseOnly(true)
        return
      case 'failed':
        setError(fallback)
    }
  }

  /**
   * The first booking's expert: their times load before the dates show, so that an expert who stopped being eligible,
   * or a case that got its expert in another tab, answers here as the booking would.
   */
  async function continueWith(picked: EligibleExpert) {
    setError(undefined)
    setBusy(true)
    try {
      await queryClient.fetchQuery({
        queryKey: bookableSlotsKey(caseId, picked.id),
        queryFn: () => fetchBookableSlots(caseId, picked.id),
      })
    } catch (failure) {
      if (isConflict(failure)) {
        // The dialog is stale: reload the case, its experts and its slots.
        await queryClient.invalidateQueries({
          queryKey: customerConsultationCaseKey(caseId),
        })
      }
      showFailure(failure, TIMES_FAILED)
      setBusy(false)
      return
    }
    setBusy(false)
    setExpert(picked)
    resetDateAndTime()
    setStep('date')
  }

  async function bookSlot(slot: BookableSlot, bookedExpert: Expert) {
    setError(undefined)
    setBusy(true)
    try {
      // The mutation reloads the case before it settles: after a 409 the steps below show what's current.
      await book.mutateAsync({ expertId: bookedExpert.id, start: slot.start })
    } catch (failure) {
      showFailure(failure, BOOKING_FAILED)
      setBusy(false)
      return
    }
    onBooked()
  }

  const title = (
    <DialogHeader>
      <DialogTitle>Book an appointment</DialogTitle>
    </DialogHeader>
  )
  const alert = error !== undefined && (
    <p role="alert" className="text-destructive">
      {error}
    </p>
  )

  if (closeOnly || slotsOfACancelledCase) {
    return (
      <>
        {title}
        <DialogDescription role="alert" className="text-destructive">
          {closeOnly ? error : problemMessage(slots.error, TIMES_FAILED)}
        </DialogDescription>
        <DialogFooter>
          <CloseButton label="Close" />
        </DialogFooter>
      </>
    )
  }

  if (step === 'expert' || expert === undefined) {
    const eligible = experts.data ?? []
    const chosen = eligible.find(({ id }) => id === chosenExpertId)
    return (
      <>
        {title}
        <DialogDescription>Choose your expert.</DialogDescription>
        {alert}
        {experts.isPending && (
          <p className="text-muted-foreground">Loading the experts…</p>
        )}
        {experts.isError && (
          <p role="alert" className="text-destructive">
            The experts could not be loaded. Close the dialog and try again.
          </p>
        )}
        {experts.isSuccess && eligible.length === 0 && <p>{NO_EXPERTS}</p>}
        {eligible.length > 0 && (
          <RadioGroup
            aria-label="Expert"
            value={chosen?.id ?? ''}
            onValueChange={setChosenExpertId}
            className="max-h-96 overflow-y-auto"
          >
            {eligible.map((eligibleExpert) => (
              <Label
                key={eligibleExpert.id}
                htmlFor={`expert-${eligibleExpert.id}`}
                className="cursor-pointer items-start gap-3 rounded-md border p-3 leading-normal font-normal has-[[data-state=checked]]:border-primary"
              >
                <RadioGroupItem
                  id={`expert-${eligibleExpert.id}`}
                  value={eligibleExpert.id}
                  className="mt-1"
                />
                <ImagePlaceholder className="size-12 shrink-0 rounded-full [&_svg]:size-6" />
                <span className="space-y-1">
                  <span className="block font-medium">
                    {eligibleExpert.displayName}
                  </span>
                  <span className="block whitespace-pre-line text-muted-foreground">
                    {eligibleExpert.bio}
                  </span>
                </span>
              </Label>
            ))}
          </RadioGroup>
        )}
        <DialogFooter>
          <CloseButton label="Cancel" />
          <Button
            disabled={chosen === undefined || busy}
            onClick={() => {
              if (chosen !== undefined) {
                void continueWith(chosen)
              }
            }}
          >
            Continue
          </Button>
        </DialogFooter>
      </>
    )
  }

  const backTo = (previous: Step) => (
    <Button
      variant="outline"
      disabled={busy}
      onClick={() => {
        setError(undefined)
        setStep(previous)
      }}
    >
      Back
    </Button>
  )

  if (step === 'confirm' && confirming !== undefined) {
    return (
      <>
        {title}
        <DialogDescription>
          Book {expert.displayName} on{' '}
          {formatDateAndTimes(confirming.start, confirming.end)} (
          {consultationCase.purchaseTerms.sessionDurationMinutes} min, Zurich
          time)?
        </DialogDescription>
        {/* No time from the server: it has passed, with a deadline longer than the notice. */}
        {confirming.freeCancellationUntil ? (
          <p>
            Free cancellation until{' '}
            {formatDateTime(confirming.freeCancellationUntil)}.
          </p>
        ) : (
          <p>{NOT_FREE_TO_CANCEL}</p>
        )}
        {alert}
        <DialogFooter>
          {backTo('time')}
          <Button
            disabled={busy}
            onClick={() => void bookSlot(confirming, expert)}
          >
            Book
          </Button>
        </DialogFooter>
      </>
    )
  }

  const dateSlots =
    chosenDate === undefined ? [] : (byDate.get(chosenDate) ?? [])

  if (step === 'time' && dateSlots.length > 0) {
    const chosen = dateSlots.find(({ start }) => start === chosenStart)
    return (
      <>
        {title}
        <DialogDescription>Choose a time (Zurich time).</DialogDescription>
        {alert}
        <RadioGroup
          aria-label="Time"
          value={chosen?.start ?? ''}
          onValueChange={setChosenStart}
          className="max-h-96 grid-cols-2 overflow-y-auto"
        >
          {dateSlots.map((slot) => (
            <Label
              key={slot.start}
              htmlFor={`slot-${slot.start}`}
              className="cursor-pointer rounded-md border p-3 has-[[data-state=checked]]:border-primary"
            >
              <RadioGroupItem id={`slot-${slot.start}`} value={slot.start} />
              {formatTimes(slot.start, slot.end)}
            </Label>
          ))}
        </RadioGroup>
        <DialogFooter>
          {backTo('date')}
          <Button
            disabled={chosen === undefined}
            onClick={() => {
              setError(undefined)
              setConfirming(chosen)
              setStep('confirm')
            }}
          >
            Continue
          </Button>
        </DialogFooter>
      </>
    )
  }

  const dates = [...byDate.keys()]
  return (
    <>
      {title}
      <DialogDescription>Choose a date.</DialogDescription>
      {alert}
      {slots.isPending && (
        <p className="text-muted-foreground">Loading the times…</p>
      )}
      {slots.isError && (
        <p role="alert" className="text-destructive">
          The times could not be loaded. Close the dialog and try again.
        </p>
      )}
      {slots.isSuccess && dates.length === 0 && <p>{NO_SLOTS}</p>}
      {dates.length > 0 && (
        <Calendar
          mode="single"
          timeZone="Europe/Zurich"
          className="mx-auto rounded-md border"
          defaultMonth={toCalendarDay(dates[0])}
          selected={
            chosenDate === undefined ? undefined : toCalendarDay(chosenDate)
          }
          onSelect={(day) => {
            setChosenDate(day === undefined ? undefined : fromCalendarDay(day))
            setChosenStart(undefined)
          }}
          // Only dates with bookable slots can be picked (spec 04, decision 4).
          disabled={(day) => !byDate.has(fromCalendarDay(day))}
        />
      )}
      <DialogFooter>
        {hasExpertStep ? backTo('expert') : <CloseButton label="Cancel" />}
        <Button
          disabled={dateSlots.length === 0}
          onClick={() => {
            setError(undefined)
            setStep('time')
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
