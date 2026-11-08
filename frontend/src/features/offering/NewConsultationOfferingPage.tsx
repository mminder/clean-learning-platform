import { useNavigate } from 'react-router'
import { BackToConsultations } from './BackToConsultations'
import { ConsultationOfferingForm } from './ConsultationOfferingForm'
import {
  useCreateConsultationOffering,
  type ConsultationOfferingRequest,
} from './queries'

/** A new offering's form starts empty; the backend rejects the empty number fields until they're filled in. */
const newConsultationOffering: ConsultationOfferingRequest = {
  name: '',
  description: '',
  sessionDurationMinutes: NaN,
  cancellationDeadlineHours: NaN,
  packages: [],
  eligibleExpertIds: [],
}

/** UC-16 steps 2–4: a new offering. Saving creates it as a draft and opens its page. */
export function NewConsultationOfferingPage() {
  const createOffering = useCreateConsultationOffering()
  const navigate = useNavigate()

  return (
    <div className="space-y-6">
      <BackToConsultations />
      <h1 className="text-2xl font-semibold">New consultation offering</h1>
      <ConsultationOfferingForm
        defaultValues={newConsultationOffering}
        onSave={async (offering) => {
          const created = await createOffering.mutateAsync(offering)
          navigate(`/admin/consultations/${created.id}`)
          return created
        }}
      />
    </div>
  )
}
