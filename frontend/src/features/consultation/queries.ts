import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { client } from '@/api/client'
import type { components } from '@/api/schema'
import { ApiError, isConflict } from '@/lib/api-error'
import { problemOf } from '@/lib/problem'

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

/** The appointment to cancel, and the outcome the dialog stated: whether the credit is returned (spec 05, decision 4). */
export interface AppointmentCancellation {
  appointmentId: string
  creditRefunded: boolean
}

// The list's key is the detail keys' prefix, so invalidating it invalidates every detail, too. A case's experts and
// slots are under its key, so invalidating the case invalidates them, too.
const customerConsultationCasesKey = ['customer', 'consultation-cases']

/** One case's key: the prefix of its eligible experts' and its bookable slots' keys. */
export function customerConsultationCaseKey(id: string) {
  return [...customerConsultationCasesKey, id]
}

/** The customer's own cases, in the order of My purchases, each with its group (UC-05's landing page). */
export function useCustomerConsultationCases() {
  return useQuery({
    queryKey: customerConsultationCasesKey,
    queryFn: async () => {
      const { data, response } = await client.GET(
        '/api/customer/consultation-cases',
      )
      // not `error`: a 401 has an empty body, and openapi-fetch then returns `error: undefined`
      if (data === undefined) {
        throw new ApiError(
          `Loading the cases failed: ${response.status}`,
          response.status,
        )
      }
      return data
    },
  })
}

/** One of the customer's cases (UC-05). Another customer's case answers 404, like an unknown one. */
export function useCustomerConsultationCase(id: string) {
  return useQuery({
    queryKey: customerConsultationCaseKey(id),
    queryFn: async () => {
      const { data, response } = await client.GET(
        '/api/customer/consultation-cases/{id}',
        { params: { path: { id } } },
      )
      // not `error`: a 401 has an empty body, and openapi-fetch then returns `error: undefined`
      if (data === undefined) {
        throw new ApiError(
          `Loading the case failed: ${response.status}`,
          response.status,
        )
      }
      return data
    },
  })
}

/**
 * Buys a package at the price and session count the dialog's confirmation stated (UC-03). A 409 or 404 means the
 * dialog showed a stale offering; the dialog reloads its packages, and the shop when it closes (`BuyConsultationDialog`).
 */
export function usePurchaseConsultationPackage() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async (purchase: PurchaseConsultationPackageRequest) => {
      const { data, error, response } = await client.POST(
        '/api/customer/consultation-cases',
        { body: purchase },
      )
      if (data === undefined) {
        throw new ApiError(
          `Buying the package failed: ${response.status}`,
          response.status,
          problemOf(response.status, error),
        )
      }
      return data
    },
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: customerConsultationCasesKey }),
  })
}

/**
 * The experts a case's first booking can pick from, by display name (UC-06 step 2): its offering's current eligible
 * experts. Only needed while the case has no expert.
 */
export function useEligibleExperts(caseId: string, enabled: boolean) {
  return useQuery({
    queryKey: [...customerConsultationCaseKey(caseId), 'eligible-experts'],
    queryFn: async () => {
      const { data, response } = await client.GET(
        '/api/customer/consultation-cases/{id}/eligible-experts',
        { params: { path: { id: caseId } } },
      )
      // not `error`: a 401 has an empty body, and openapi-fetch then returns `error: undefined`
      if (data === undefined) {
        throw new ApiError(
          `Loading the experts failed: ${response.status}`,
          response.status,
        )
      }
      return data
    },
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
  const { data, error, response } = await client.GET(
    '/api/customer/consultation-cases/{id}/bookable-slots',
    { params: { path: { id: caseId }, query: { expertId } } },
  )
  if (data === undefined) {
    throw new ApiError(
      `Loading the times failed: ${response.status}`,
      response.status,
      problemOf(response.status, error),
    )
  }
  return data
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
  const queryClient = useQueryClient()
  const reloadCases = () =>
    queryClient.invalidateQueries({ queryKey: customerConsultationCasesKey })
  return useMutation({
    mutationFn: async (booking: BookConsultationAppointmentRequest) => {
      const { data, error, response } = await client.POST(
        '/api/customer/consultation-cases/{id}/appointments',
        { params: { path: { id: caseId } }, body: booking },
      )
      if (data === undefined) {
        throw new ApiError(
          `Booking the appointment failed: ${response.status}`,
          response.status,
          problemOf(response.status, error),
        )
      }
      return data
    },
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
  const queryClient = useQueryClient()
  const reloadCases = () =>
    queryClient.invalidateQueries({ queryKey: customerConsultationCasesKey })
  return useMutation({
    mutationFn: async ({
      appointmentId,
      creditRefunded,
    }: AppointmentCancellation) => {
      const { data, error, response } = await client.POST(
        '/api/customer/consultation-cases/{id}/appointments/{appointmentId}/cancel',
        {
          params: { path: { id: caseId, appointmentId } },
          body: { creditRefunded },
        },
      )
      if (data === undefined) {
        throw new ApiError(
          `Cancelling the appointment failed: ${response.status}`,
          response.status,
          problemOf(response.status, error),
        )
      }
      return data
    },
    onSuccess: reloadCases,
    onError: (error) => (isConflict(error) ? reloadCases() : undefined),
  })
}
