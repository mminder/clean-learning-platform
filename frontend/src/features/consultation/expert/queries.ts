import { useMutation, useQuery } from '@tanstack/react-query'
import { client, dataOrThrow, okOrThrow } from '@/api/client'
import type { components } from '@/api/schema'
import {
  expertConsultationCasesKey,
  useReloadCases,
} from '@/features/consultation/case-keys'
import { isNotFound } from '@/lib/api-error'

export type ExpertConsultationCaseSummary =
  components['schemas']['ExpertConsultationCaseSummaryResponse']

export type ExpertNote = components['schemas']['ExpertNoteResponse']

export type NoteRequest = components['schemas']['NoteRequest']

/** The cases the current user is the assigned expert of, in the order of My cases, each with its group (UC-11). */
export function useExpertConsultationCases() {
  return useQuery({
    queryKey: expertConsultationCasesKey,
    queryFn: async () =>
      dataOrThrow(
        await client.GET('/api/expert/consultation-cases'),
        'Loading the cases',
      ),
  })
}

/** One case of the current user as its assigned expert (UC-11 step 4). Another expert's case answers 404, like an unknown one. */
export function useExpertConsultationCase(id: string) {
  return useQuery({
    queryKey: [...expertConsultationCasesKey, id],
    queryFn: async () =>
      dataOrThrow(
        await client.GET('/api/expert/consultation-cases/{id}', {
          params: { path: { id } },
        }),
        'Loading the case',
      ),
  })
}

// After a note's save or delete, and after a 404, which comes from a note deleted meanwhile (spec 06, decision 7), the
// cases of both audiences reload.

/** Writes a note on the case (UC-15 steps 1–3), and answers it. */
export function useAddNote(caseId: string) {
  const reloadCases = useReloadCases()
  return useMutation({
    mutationFn: async (note: NoteRequest) =>
      dataOrThrow(
        await client.POST('/api/expert/consultation-cases/{id}/notes', {
          params: { path: { id: caseId } },
          body: note,
        }),
        'Adding the note',
      ),
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
    }) =>
      dataOrThrow(
        await client.PUT('/api/expert/consultation-cases/{id}/notes/{noteId}', {
          params: { path: { id: caseId, noteId } },
          body: note,
        }),
        'Saving the note',
      ),
    onSuccess: reloadCases,
    onError: (error) => (isNotFound(error) ? reloadCases() : undefined),
  })
}

/** Deletes a note of the case for good (UC-15, "Delete"). */
export function useDeleteNote(caseId: string) {
  const reloadCases = useReloadCases()
  return useMutation({
    mutationFn: async (noteId: string) =>
      okOrThrow(
        await client.DELETE(
          '/api/expert/consultation-cases/{id}/notes/{noteId}',
          { params: { path: { id: caseId, noteId } } },
        ),
        'Deleting the note',
      ),
    onSuccess: reloadCases,
    onError: (error) => (isNotFound(error) ? reloadCases() : undefined),
  })
}
