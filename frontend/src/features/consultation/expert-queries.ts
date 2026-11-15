import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { client } from '@/api/client'
import type { components } from '@/api/schema'
import { ApiError, isNotFound } from '@/lib/api-error'
import { problemOf } from '@/lib/problem'

export type ExpertConsultationCaseSummary =
  components['schemas']['ExpertConsultationCaseSummaryResponse']

export type ExpertNote = components['schemas']['ExpertNoteResponse']

export type NoteRequest = components['schemas']['NoteRequest']

// The list's key is the prefix of the detail keys, as the customer's is.
const expertConsultationCasesKey = ['expert', 'consultation-cases']

/** The cases the current user is the assigned expert of, in the order of My cases, each with its group (UC-11). */
export function useExpertConsultationCases() {
  return useQuery({
    queryKey: expertConsultationCasesKey,
    queryFn: async () => {
      const { data, response } = await client.GET(
        '/api/expert/consultation-cases',
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

/** One case of the current user as its assigned expert (UC-11 step 4). Another expert's case answers 404, like an unknown one. */
export function useExpertConsultationCase(id: string) {
  return useQuery({
    queryKey: [...expertConsultationCasesKey, id],
    queryFn: async () => {
      const { data, response } = await client.GET(
        '/api/expert/consultation-cases/{id}',
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
 * After a note's save or delete, and after a 404, which comes from a note deleted meanwhile (spec 06, decision 7), the
 * cases reload: the expert's, and the customer's, since an expert who is the case's customer has both pages. A
 * mutation stays pending until the queries on the screen have reloaded.
 */
function useReloadCases() {
  const queryClient = useQueryClient()
  return () =>
    Promise.all([
      queryClient.invalidateQueries({ queryKey: expertConsultationCasesKey }),
      queryClient.invalidateQueries({
        queryKey: ['customer', 'consultation-cases'],
      }),
    ])
}

/** Writes a note on the case (UC-15 steps 1–3), and answers it. */
export function useAddNote(caseId: string) {
  const reloadCases = useReloadCases()
  return useMutation({
    mutationFn: async (note: NoteRequest) => {
      const { data, error, response } = await client.POST(
        '/api/expert/consultation-cases/{id}/notes',
        { params: { path: { id: caseId } }, body: note },
      )
      if (data === undefined) {
        throw new ApiError(
          `Adding the note failed: ${response.status}`,
          response.status,
          problemOf(response.status, error),
        )
      }
      return data
    },
    onSuccess: reloadCases,
    onError: (error) => (isNotFound(error) ? reloadCases() : undefined),
  })
}

/** Saves a note of the case (UC-15, "Edit"), and answers it. The last save wins (spec 06, decision 7). */
export function useEditNote(caseId: string) {
  const reloadCases = useReloadCases()
  return useMutation({
    mutationFn: async ({
      noteId,
      note,
    }: {
      noteId: string
      note: NoteRequest
    }) => {
      const { data, error, response } = await client.PUT(
        '/api/expert/consultation-cases/{id}/notes/{noteId}',
        { params: { path: { id: caseId, noteId } }, body: note },
      )
      if (data === undefined) {
        throw new ApiError(
          `Saving the note failed: ${response.status}`,
          response.status,
          problemOf(response.status, error),
        )
      }
      return data
    },
    onSuccess: reloadCases,
    onError: (error) => (isNotFound(error) ? reloadCases() : undefined),
  })
}

/** Deletes a note of the case for good (UC-15, "Delete"). */
export function useDeleteNote(caseId: string) {
  const reloadCases = useReloadCases()
  return useMutation({
    mutationFn: async (noteId: string) => {
      const { error, response } = await client.DELETE(
        '/api/expert/consultation-cases/{id}/notes/{noteId}',
        { params: { path: { id: caseId, noteId } } },
      )
      if (!response.ok) {
        throw new ApiError(
          `Deleting the note failed: ${response.status}`,
          response.status,
          problemOf(response.status, error),
        )
      }
    },
    onSuccess: reloadCases,
    onError: (error) => (isNotFound(error) ? reloadCases() : undefined),
  })
}
