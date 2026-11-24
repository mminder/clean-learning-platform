import type { ReactNode } from 'react'
import type { components } from '@/api/schema'
import { formatDateTime } from '@/lib/date'

/** What every reader of a note sees; the customer's notes have no visibility (spec 06, decision 6). */
export type NoteContent = Pick<
  components['schemas']['ExpertNoteResponse'],
  'title' | 'text' | 'writtenAt' | 'editedAt'
>

/**
 * A note of a case (UC-15): its title, its text with its line breaks, the time it was written, and "edited …" once it
 * was. `children` is what only the expert has: the visibility badge, **Edit** and **Delete**. The page states "(Zurich
 * time)" next to its heading.
 */
export function NoteCard({
  note,
  children,
}: {
  note: NoteContent
  children?: ReactNode
}) {
  return (
    <li className="space-y-2 rounded-lg border p-4">
      <div className="flex flex-wrap items-center gap-3">
        <h3 className="font-semibold">{note.title}</h3>
        {children}
      </div>
      <p className="whitespace-pre-line">{note.text}</p>
      <p className="text-sm text-muted-foreground">
        {formatDateTime(note.writtenAt)}
        {note.editedAt && <>, edited {formatDateTime(note.editedAt)}</>}
      </p>
    </li>
  )
}
