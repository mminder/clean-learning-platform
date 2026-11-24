import { useMutation, useQuery } from '@tanstack/react-query'
import { client, dataOrThrow } from '@/api/client'
import type { components } from '@/api/schema'
import {
  customerConsultationCasesKey,
  useReloadCases,
} from '@/features/consultation/case-keys'
import { isConflict } from '@/lib/api-error'

export type PurchaseConsultationPackageRequest =
  components['schemas']['PurchaseConsultationPackageRequest']

export type CustomerConsultationCase =
  components['schemas']['CustomerConsultationCaseResponse']

export type CustomerConsultationCaseSummary =
  components['schemas']['CustomerConsultationCaseSummaryResponse']

export type EligibleExpert = components['schemas']['EligibleExpertResponse']

export type BookableSlot = components['schemas']['BookableSlotResponse']

export type BookConsultationAppointmentRequest =
  components['schemas']['BookConsultationAppointmentRequest']

export type CustomerUpcomingAppointment =
  components['schemas']['CustomerUpcomingAppointmentResponse']

export type CustomerAppointment =
  components['schemas']['CustomerAppointmentResponse']

export type RefundEligibility =
  components['schemas']['RefundEligibilityResponse']

export type CancellationOutcome =
  components['schemas']['CancellationOutcomeResponse']

/** The appointment to cancel, and the outcome the dialog stated: whether the credit is returned (spec 05, decision 4). */
export interface CancelAppointmentVariables {
  appointmentId: string
  creditRefunded: boolean
}

/** One case's key: the prefix of its eligible experts' and its bookable slots' keys. */
export function customerConsultationCaseKey(id: string) {
  return [...customerConsultationCasesKey, id]
}

/** The customer's own cases, in the order of My purchases, each with its group (UC-05's landing page). */
export function useCustomerConsultationCases() {
  return useQuery({
    queryKey: customerConsultationCasesKey,
    queryFn: async () =>
      dataOrThrow(
        await client.GET('/api/customer/consultation-cases'),
        'Loading the cases',
      ),
  })
}

/** One of the customer's cases (UC-05). Another customer's case answers 404, like an unknown one. */
export function useCustomerConsultationCase(id: string) {
  return useQuery({
    queryKey: customerConsultationCaseKey(id),
    queryFn: async () =>
      dataOrThrow(
        await client.GET('/api/customer/consultation-cases/{id}', {
          params: { path: { id } },
        }),
        'Loading the case',
      ),
  })
}

/**
 * Buys a package at the price and session count the dialog's confirmation stated (UC-03). A 409 or 404 means the
 * dialog showed a stale offering; the dialog reloads its packages, and the shop when it closes (`BuyConsultationDialog`).
 */
export function usePurchaseConsultationPackage() {
  const reloadCases = useReloadCases()
  return useMutation({
    mutationFn: async (purchase: PurchaseConsultationPackageRequest) =>
      dataOrThrow(
        await client.POST('/api/customer/consultation-cases', {
          body: purchase,
        }),
        'Buying the package',
      ),
    onSuccess: reloadCases,
  })
}

/**
 * The experts a case's first booking can pick from, by display name (UC-06 step 2): its offering's current eligible
 * experts. Only needed while the case has no expert.
 */
export function useEligibleExperts(caseId: string, enabled: boolean) {
  return useQuery({
    queryKey: [...customerConsultationCaseKey(caseId), 'eligible-experts'],
    queryFn: async () =>
      dataOrThrow(
        await client.GET(
          '/api/customer/consultation-cases/{id}/eligible-experts',
          { params: { path: { id: caseId } } },
        ),
        'Loading the experts',
      ),
    enabled,
  })
}

export function bookableSlotsKey(caseId: string, expertId: string) {
  return [...customerConsultationCaseKey(caseId), 'bookable-slots', expertId]
}

/**
 * The times the case can book with the expert, soonest first (UC-06 steps 3–4). A 409 (`EXPERT_CHANGED`,
 * `EXPERT_NOT_ELIGIBLE`) comes with its problem, as a booking's does.
 */
export async function fetchBookableSlots(caseId: string, expertId: string) {
  return dataOrThrow(
    await client.GET('/api/customer/consultation-cases/{id}/bookable-slots', {
      params: { path: { id: caseId }, query: { expertId } },
    }),
    'Loading the times',
  )
}

/** The bookable slots with the expert, once the expert is known. */
export function useBookableSlots(caseId: string, expertId: string | undefined) {
  return useQuery({
    queryKey: bookableSlotsKey(caseId, expertId ?? ''),
    queryFn: () => fetchBookableSlots(caseId, expertId ?? ''),
    enabled: expertId !== undefined,
  })
}

/**
 * Books a session on the case with the expert and at the start the dialog's confirmation stated (UC-06 steps 6–7).
 * Afterwards, and after a 409, which comes from a stale dialog, it reloads the cases: the list, every case, and their
 * experts and slots. It stays pending until the queries on the screen have reloaded.
 */
export function useBookConsultationAppointment(caseId: string) {
  const reloadCases = useReloadCases()
  return useMutation({
    mutationFn: async (booking: BookConsultationAppointmentRequest) =>
      dataOrThrow(
        await client.POST(
          '/api/customer/consultation-cases/{id}/appointments',
          { params: { path: { id: caseId } }, body: booking },
        ),
        'Booking the appointment',
      ),
    onSuccess: reloadCases,
    onError: (error) => (isConflict(error) ? reloadCases() : undefined),
  })
}

/**
 * Cancels an appointment of the case with the outcome the dialog stated (UC-07 steps 3–5), and answers the case.
 * Afterwards, and after a 409, which comes from a stale dialog, it reloads the cases, as a booking does: the list,
 * every case, and their experts and slots, so the freed time is offered again. It stays pending until the queries on
 * the screen have reloaded.
 */
export function useCancelConsultationAppointment(caseId: string) {
  const reloadCases = useReloadCases()
  return useMutation({
    mutationFn: async ({
      appointmentId,
      creditRefunded,
    }: CancelAppointmentVariables) =>
      dataOrThrow(
        await client.POST(
          '/api/customer/consultation-cases/{id}/appointments/{appointmentId}/cancel',
          {
            params: { path: { id: caseId, appointmentId } },
            body: { creditRefunded },
          },
        ),
        'Cancelling the appointment',
      ),
    onSuccess: reloadCases,
    onError: (error) => (isConflict(error) ? reloadCases() : undefined),
  })
}

/**
 * Cancels the case, and with it its appointments that haven't started, with the outcome the dialog stated: whether the
 * purchase price would be refunded (UC-08 steps 3–5, spec 05 decision 4), and answers the case. Afterwards, and after
 * a 409, which comes from a stale dialog, it reloads the cases, as the appointment's cancellation does: the list, every
 * case, and their experts and slots. It stays pending until the queries on the screen have reloaded.
 */
export function useCancelConsultationCase(caseId: string) {
  const reloadCases = useReloadCases()
  return useMutation({
    mutationFn: async (refundEligible: boolean) =>
      dataOrThrow(
        await client.POST('/api/customer/consultation-cases/{id}/cancel', {
          params: { path: { id: caseId } },
          body: { refundEligible },
        }),
        'Cancelling the case',
      ),
    onSuccess: reloadCases,
    onError: (error) => (isConflict(error) ? reloadCases() : undefined),
  })
}
