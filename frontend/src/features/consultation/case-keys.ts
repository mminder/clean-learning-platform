import { useQueryClient } from '@tanstack/react-query'

// Each list's key is the prefix of its detail keys, so invalidating it invalidates every detail, too. A customer's
// case's experts and slots are under its key, so invalidating the case invalidates them, too.
export const customerConsultationCasesKey = ['customer', 'consultation-cases']
export const expertConsultationCasesKey = ['expert', 'consultation-cases']

/**
 * Reloads the cases of both audiences, the customer's and the expert's, since an expert who is a case's customer has
 * both pages. A mutation that returns it from `onSuccess` stays pending until the queries on the screen have reloaded.
 */
export function useReloadCases() {
  const queryClient = useQueryClient()
  return () =>
    Promise.all([
      queryClient.invalidateQueries({ queryKey: customerConsultationCasesKey }),
      queryClient.invalidateQueries({ queryKey: expertConsultationCasesKey }),
    ])
}
