import { Button } from '@/components/ui/button'
import { ExpertProfileForm } from './ExpertProfileForm'
import { toRequest, useExpertProfile, useSaveExpertProfile } from './queries'

/** UC-14: the expert's own profile, which customers see apart from the Teams link. */
export function ExpertProfilePage() {
  const profile = useExpertProfile()
  const saveProfile = useSaveExpertProfile()

  return (
    <div className="space-y-6">
      <header className="space-y-2">
        <h1 className="text-2xl font-semibold">Profile</h1>
        <p className="text-muted-foreground">
          Customers see your display name and bio in the shop and on their
          cases. Your Teams link is where your consultations take place.
        </p>
      </header>
      {profile.isPending && (
        <p className="text-muted-foreground">Loading your profile…</p>
      )}
      {profile.isError && (
        <div role="alert" className="space-y-3">
          <p>Your profile could not be loaded.</p>
          <Button variant="outline" onClick={() => profile.refetch()}>
            Try again
          </Button>
        </div>
      )}
      {profile.isSuccess && (
        <ExpertProfileForm
          // The form starts from the loaded profile once; a save makes the saved one its starting point.
          defaultValues={toRequest(profile.data)}
          onSave={(changed) => saveProfile.mutateAsync(changed)}
        />
      )}
    </div>
  )
}
