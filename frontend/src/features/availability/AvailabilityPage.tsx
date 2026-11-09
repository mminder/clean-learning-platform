import { Button } from '@/components/ui/button'
import { AvailabilityForm } from './AvailabilityForm'
import { toRequest, useAvailability, useSaveAvailability } from './queries'

/** UC-13: the expert's own availability, which customers book within. */
export function AvailabilityPage() {
  const availability = useAvailability()
  const saveAvailability = useSaveAvailability()

  return (
    <div className="space-y-6">
      <header className="space-y-2">
        <h1 className="text-2xl font-semibold">Availability</h1>
        <p className="text-muted-foreground">
          Customers can book you on the selected dates, within your daily time
          ranges (Zurich time).
        </p>
      </header>
      {availability.isPending && (
        <p className="text-muted-foreground">Loading your availability…</p>
      )}
      {availability.isError && (
        <div role="alert" className="space-y-3">
          <p>Your availability could not be loaded.</p>
          <Button variant="outline" onClick={() => availability.refetch()}>
            Try again
          </Button>
        </div>
      )}
      {availability.isSuccess && (
        <AvailabilityForm
          // The form starts from the loaded availability once; a save makes the saved one its starting point.
          defaultValues={toRequest(availability.data)}
          onSave={(changed) => saveAvailability.mutateAsync(changed)}
        />
      )}
    </div>
  )
}
