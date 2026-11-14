# 06 The expert's side of consultations: technical plan

Spec: [06 The expert's side of consultations](../../specs/06-expert-side-of-consultations.md)

This plan holds the decisions that several tasks rely on. The tasks link here instead of repeating them. If a task has to change one of them, it updates this file in the same change. Everything from the plans of increments [01](../01-shop-tracer-bullet/README.md) to [05](../05-cancel-consultation-appointment-or-case/README.md) still holds: toolchain, test setup and test clock, error conventions, writes, locks, forms, mutations, uploads, customer-owned and expert-owned data, and `lib/date.ts`. So does [D-39](../../decisions.md#architecture): `CaseCredits` and `CaseInList` hold the rules every view of a case shares.

## Tasks

| # | Task | Type | Blocked by |
|---|---|---|---|
| 01 | [My cases](01-my-cases.md) | AFK | – |
| 02 | [The expert's case detail](02-expert-case-detail.md) | AFK | 01 |
| 03 | [Notes](03-notes.md) | AFK | 02 |
| 04 | [Public notes on the customer's case](04-public-notes-for-the-customer.md) | AFK | 03 |
| 05 | [The expert profile](05-expert-profile.md) | AFK | – |
| 06 | [The expert photo](06-expert-photo.md) | AFK | 05 |
| 07 | [Join a call](07-join-call.md) | AFK | 02, 05 |

```
01 My cases ──► 02 The expert's ──┬─► 03 Notes ──► 04 Public notes on the customer's case
                case detail       │
                                  └──────────────┐
                                                 ├─► 07 Join a call
05 The expert profile ──┬────────────────────────┘
                        └─► 06 The expert photo
```

01 and 02 give the expert their cases, with the first queries scoped to the assigned expert: they set the pattern for a second audience of one resource, so review 02 with extra care, as task 03 of increment 04. 03 and 04 are the notes, for the expert and then for the customer. 05 and 06 are the profile and its photo, and 07 brings the Teams link of 05 onto the appointments of both case details.

**The tasks run one after the other, in the order of their numbers** (spec decision 20). The graph shows what a task needs, not what may run at the same time: every task changes `openapi.json` and `schema.d.ts` and is checked by hand on the dev server, which two worktrees can't share (one database, ports 8080 and 5173). The migrations are numbered in this order. 05 and 06 need nothing of 01 to 04, so the increment can be cut after 04 if it gets too large ([spec](../../specs/06-expert-side-of-consultations.md#risks)).

## Backend

### Packages and names

| Package | Classes | Task |
|---|---|---|
| `domain.consultation.view` | `ExpertConsultationCaseSummary` (a case on **My cases**), `ExpertAppointment` (a booked appointment as its expert sees it) | 01 |
| `domain.consultation.view` | `ExpertConsultationCase` (the case as its assigned expert sees it) | 02 |
| `domain.consultation` | `Note` (a note of a case, with the rules of writing and editing) | 03 |
| `domain.consultation.value` | `NoteId`, `NoteTitle`, `NoteText` (never blank), `NoteVisibility` (`PRIVATE`, `PUBLIC`, both stored) | 03 |
| `domain.consultation.view` | `ExpertConsultationCase` gains the notes (03) and the expert's Teams link (07); `CustomerConsultationCase` gains the public notes (04) and the assigned expert's Teams link (07); `CustomerUpcomingAppointment` gains the link window (07) | 03, 04, 07 |
| `domain.user.value` | `TeamsLink` (an `https://` link) | 05 |
| `domain.user` | `OwnExpertProfile` (the profile as its expert edits it: the public `ExpertProfile` plus the Teams link) | 05 |
| `domain.user` | `ExpertProfile` gains `Optional<ImageId> photoId`; `OwnExpertProfile` gains `changePhoto` and `removePhoto` | 06 |
| `domain.shared` | `LinkWindow` (when a call's link is shown to a customer) | 07 |
| `application.consultation` | `ListExpertConsultationCasesUseCase` | 01 |
| `application.consultation` | `GetExpertConsultationCaseUseCase` | 02 |
| `application.consultation` | `AddNoteUseCase` with `AddNoteCommand`, `EditNoteUseCase` with `EditNoteCommand`, `DeleteNoteUseCase` | 03 |
| `application.consultation.port` | `LoadExpertConsultationCasesPort` (`loadAll(UserId expertId, Instant asOf)`) | 01 |
| `application.consultation.port` | `LoadExpertConsultationCasePort` (`load(ConsultationCaseId, UserId expertId, Instant asOf)`) | 02 |
| `application.consultation.port` | `CheckAssignedExpertPort` (`isAssignedExpert(ConsultationCaseId, UserId expertId)`), `LoadNotePort` (`load(NoteId, ConsultationCaseId, UserId expertId)`), `SaveNotePort` (`add(Note)`, `update(Note)`), `DeleteNotePort` (`delete(NoteId, ConsultationCaseId, UserId expertId)`, which answers whether it deleted one) | 03 |
| `application.user` | `GetExpertProfileUseCase`, `SaveExpertProfileUseCase` with `SaveExpertProfileCommand` | 05 |
| `application.user` | `UploadExpertPhotoUseCase`, `RemoveExpertPhotoUseCase` | 06 |
| `application.user.port` | `LoadOwnExpertProfilePort` (`load(UserId expertId)`), `SaveOwnExpertProfilePort` (`save(OwnExpertProfile)`) | 05 |
| `adapters.database.consultation` | `ExpertConsultationCaseAdapter`; `row`: `ExpertConsultationCaseSummaryRow` (01), `ExpertConsultationCaseRow` (02) | 01, 02 |
| `adapters.database.consultation` | `NoteAdapter`, `NoteRepository`; `entity`: `NoteEntity` | 03 |
| `adapters.database.user` | `ExpertProfileAdapter`; `entity`: `ExpertProfileEntity` maps `teamsLink` (05) and `photoId` (06) | 05, 06 |
| `api.consultation` | `ExpertConsultationCaseController`; `response`: `ExpertConsultationCaseSummaryResponse`, `ExpertAppointmentResponse` | 01 |
| `api.consultation` | `response`: `ExpertConsultationCaseResponse`, `ExpertCancelledAppointmentResponse` | 02 |
| `api.consultation` | the notes' routes; `request`: `NoteRequest`; `response`: `ExpertNoteResponse` | 03 |
| `api.consultation` | `response`: `CustomerNoteResponse` | 04 |
| `api.user` | `ExpertProfileController`; `request`: `ExpertProfileRequest`; `response`: `ExpertProfileResponse` | 05 |

- **Names** follow the [glossary](../../glossary.md): case, assigned expert, note, expert profile, Teams link, link window. The expert's views and responses carry the audience, `Expert…`, as the customer's carry `Customer…`.
- **`ExpertProfile` stays the public profile** (display name, bio and, from 06, photo): what the shop, the expert picker and the customer's case show. It never has the Teams link, so no query for those pages loads one. **`OwnExpertProfile`** is the whole profile, with the Teams link: only the profile's own use cases load it (spec decision 15).
- **`PurchaseGroup`** names the groups of **My cases**, too: they're the groups of **My purchases**, from `CaseInList`.
- **The use cases take the `CurrentUser`**, whose id is the expert's: every case query is scoped to `assigned_expert_id`, and the profile's routes have no id.
- **A note is a top-level model, not a view**: the expert's page shows exactly what the write decides with ([D-37](../../decisions.md#architecture)).

### Schema

| Migration | Task | Content |
|---|---|---|
| `V11__create_note.sql` | 03 | the `note` table |
| `V12__expert_teams_link.sql` | 05 | the profile's `teams_link` |
| `V13__expert_photo.sql` | 06 | the profile's `photo_id` |

```
note            id                   uuid primary key
                consultation_case_id uuid not null references consultation_case (id)
                title                text not null, check (title ~ '\S')
                text                 text not null, check (text ~ '\S')
                visibility           text not null, check (visibility in ('PRIVATE', 'PUBLIC'))
                written_at           timestamptz not null, from the application's Clock
                edited_at            timestamptz, null until the first edit, from the application's Clock
                created_at, updated_at, with the set_updated_at trigger
                index note_consultation_case_id_idx on (consultation_case_id)

expert_profile  + teams_link text, null without a link, check (teams_link ~ '^https://[!-~]+$')
                + photo_id   uuid unique references image (id)
```

- **The note's times** come from the `Clock`, as the purchase's does: `created_at` and `updated_at` stay the database's technical times (spec decision 5).
- **The checks repeat the value objects' rules** ([D-36](../../decisions.md#architecture)): `NoteTitle` and `NoteText` are never blank, with `OfferingName`'s meaning of blank and V3's check; `TeamsLink` is `https://` followed by at least one printable ASCII character without a space, the same expression in Java and in Postgres.
- **The seed doesn't change** (spec decision 16): the seeded profiles have neither a Teams link nor a photo. Tests and the demo set them through the API.
- **`photo_id` is unique**, like `offering.image_id`: an image belongs to one owner at most, and the index serves the foreign key check when an image is deleted.
- **The migrations are numbered in the tasks' order.** If the order changes, renumber before merging: a migration below one that already ran fails on a database that has it.
- **The test cleanup** (`delete-test-data.sql`) deletes every note before the cases (03), sets the two seeded profiles back to the seed's display name and bio, without a Teams link (05) and without a photo (06), and then deletes every image that neither an offering nor a profile refers to (06). Until now tests never changed seed rows; the profiles are the first, and the cleanup restores them.

### API

| Route | Answer | Task |
|---|---|---|
| `GET /api/expert/consultation-cases` | 200 `ExpertConsultationCaseSummaryResponse[]`, in the order of **My cases** | 01 |
| `GET /api/expert/consultation-cases/{id}` | 200 `ExpertConsultationCaseResponse`; 400; 404 | 02 |
| `POST /api/expert/consultation-cases/{id}/notes` | `NoteRequest` → 201 `ExpertNoteResponse`; 400; 404; 415 | 03 |
| `PUT /api/expert/consultation-cases/{id}/notes/{noteId}` | `NoteRequest` → 200 `ExpertNoteResponse`; 400; 404; 415 | 03 |
| `DELETE /api/expert/consultation-cases/{id}/notes/{noteId}` | 204; 400; 404 | 03 |
| `GET /api/customer/consultation-cases/{id}` | as before, plus `notes` (04), the experts' `photoId` (06), and the link window on each upcoming appointment (07) | 04, 06, 07 |
| `GET /api/expert/profile` | 200 `ExpertProfileResponse` | 05 |
| `PUT /api/expert/profile` | `ExpertProfileRequest` → 200 `ExpertProfileResponse`; 400; 415 | 05 |
| `PUT /api/expert/profile/photo` | multipart `file` → 200 `ExpertProfileResponse`; 400; 415 | 06 |
| `DELETE /api/expert/profile/photo` | 200 `ExpertProfileResponse` | 06 |
| `GET /api/shop/offerings/{id}`, `GET /api/customer/consultation-cases/{id}/eligible-experts` | as before, each expert with `photoId` | 06 |

401 and 403 come from security, as for every `/api/expert/**` route. Each method declares its errors with `ProblemResponses` and its success with `@ResponseStatus`. operationIds: `listExpertConsultationCases`, `getExpertConsultationCase`, `addNote`, `editNote`, `deleteNote`, `getExpertProfile`, `saveExpertProfile`, `uploadExpertPhoto`, `removeExpertPhoto`.

```jsonc
// ExpertAppointmentResponse                                                                            01
{ "id": "…", "start": "2026-11-17T09:00:00Z", "end": "2026-11-17T10:00:00Z", "status": "BOOKED" }  // or CONSUMED

// ExpertConsultationCaseSummaryResponse                                                                01
{
  "id": "…",
  "customerName": "Customer 1",
  "offeringName": "Career Coaching",
  "purchasedAt": "2026-11-09T14:00:00Z",
  "openCredits": 2,
  "totalCredits": 3,
  "nextAppointment": { /* ExpertAppointmentResponse; @Nullable */ },
  "group": "UPCOMING",          // or NEEDS_BOOKING, PAST: the server decides (spec 04, decision 18)
  "cancelledAt": null           // @Nullable: an active case
}

// ExpertConsultationCaseResponse                                                                       02, 03, 07
{
  "id": "…",
  "customerName": "Customer 1",
  "customerEmail": "customer1@example.com",
  "offeringName": "Career Coaching",
  "sessionCount": 3,                    // the terms the expert sees: no price paid (spec decision 4)
  "sessionDurationMinutes": 60,
  "cancellationDeadlineHours": 24,
  "purchasedAt": "2026-11-09T14:00:00Z",
  "openCredits": 2,
  "totalCredits": 3,
  "cancelledAt": null,                  // @Nullable: an active case; no refund outcome (spec decision 4)
  "upcomingAppointments": [ /* ExpertAppointmentResponse, soonest first; a running one is CONSUMED */ ],
  "pastAppointments": [ /* ExpertAppointmentResponse, newest first */ ],
  "cancelledAppointments": [            // ExpertCancelledAppointmentResponse, newest start first (spec decision 14)
    { "id": "…", "start": "2026-11-12T09:00:00Z", "end": "2026-11-12T10:00:00Z", "creditRefunded": false }
  ],
  "notes": [ /* ExpertNoteResponse, newest first (03) */ ],
  "teamsLink": "https://example.com/meet/expert-2"   // @Nullable: the expert's own link, or none yet (07)
}

// NoteRequest                                                                                          03
{ "title": "First session", "text": "Line one\nLine two", "visibility": "PRIVATE" }
// @NotBlank title and text; @NotNull visibility

// ExpertNoteResponse                                                                                   03
{
  "id": "…", "title": "First session", "text": "Line one\nLine two", "visibility": "PRIVATE",
  "writtenAt": "2026-11-09T14:00:00Z",
  "editedAt": null                      // @Nullable: never edited
}

// CustomerNoteResponse: no visibility, every one is public                                             04
{ "id": "…", "title": "Homework", "text": "…", "writtenAt": "…", "editedAt": null }

// CustomerConsultationCaseResponse: increment 05's fields, plus                                        04
{ "notes": [ /* CustomerNoteResponse, newest first */ ] }

// CustomerUpcomingAppointmentResponse: increment 05's fields, plus                                     07
{
  "linkAvailableFrom": "2026-11-17T08:00:00Z",       // start − 1 h
  "linkWindowOpen": true,                            // linkAvailableFrom ≤ now < end
  "teamsLink": "https://example.com/meet/expert-2"   // @Nullable: only while the window is open, and the expert has one
}

// ExpertProfileRequest                                                                                 05
{ "displayName": "Expert Two", "bio": "…", "teamsLink": "https://example.com/meet/expert-2" }
// @NotBlank displayName; @NotNull bio, which may be empty;
// teamsLink: null or "" for none, otherwise @Pattern with TeamsLink's rule and the message "must be an https:// link"

// ExpertProfileResponse                                                                                05, 06
{ "displayName": "Expert Two", "bio": "…", "teamsLink": null, "photoId": null }   // both @Nullable; photoId with 06

// EligibleExpertResponse, AssignedExpertResponse: as before, plus                                      06
{ "photoId": null }   // @Nullable
```

- **A new note answers 201 without a `Location`**: it has no route to read it by, as the booking's appointment ([backend.md](../../architecture/backend.md#rest-conventions)). The page reloads the case anyway.
- **No route lists notes**: they come with the case, the expert's with every note, the customer's with the public ones.
- **The profile's routes have no id**: the profile is the current user's, as the availability is.
- **`group`, the lists of appointments, `linkWindowOpen`, `linkAvailableFrom` and `teamsLink`** are computed by the server. The frontend never compares a time with a window.
- **The Teams link is in three responses only**: the expert's own profile, the expert's own case, and the customer's upcoming appointment while its window is open. The shop's, the expert picker's and the assigned expert's responses have no such field, and their queries don't select the column.

### The case as its expert sees it

01 for the list, 02 for the detail, 03 for the notes, 07 for the link.

- **`ExpertConsultationCaseSummary`** (01): a `CaseInList`, the customer's name, the offering's name and `asOf`. `nextAppointment()` is the credits' next appointment as an `ExpertAppointment`; `group()` is `inList.groupAt(asOf)`. `ListExpertConsultationCasesUseCase` sorts with `CaseInList.orderAt(now)`, as the customer's list does: one rule for both lists ([D-39](../../decisions.md#architecture)). So Needs booking is sorted by purchase time (spec decision 3), and a completed case whose credits were all forfeited sorts under Past by its purchase time, as on **My purchases**.
- **`ExpertAppointment`** (01): id, start, end and the `AppointmentStatus` at `asOf`, built with `ExpertAppointment.of(appointment, asOf)`, as `CustomerAppointment` is.
- **`ExpertConsultationCase`** (02): the case's id, the customer's name and email, the offering's name, the session duration and the cancellation deadline (not `PurchaseTerms`, which has the price paid), the purchase time, the `CaseCredits`, `Optional<Instant> cancelledAt` and `asOf`. `upcomingAppointments()` and `pastAppointments()` come from `CaseCredits`, as the customer's do; `cancelledAppointments()` are the credits' `CancelledAppointment`s, newest start first, then by id as text. The response maps a `CancelledAppointment` as it is: no view of its own.
- **It gains** `List<Note> notes`, newest first (03), and `Optional<TeamsLink> teamsLink`, the expert's own (07).
- **The customer's name and email** are `app_user.name` and `app_user.email` of the case's customer, `String`s as on `User`.
- **The adapter** (`ExpertConsultationCaseAdapter`) reads the cases with `where c.assignedExpertId = :expertId`, joined with the offering and the customer's user row, and every appointment of those cases, booked and cancelled, in one query for the list; it builds the credits with `ConsultationCaseMapper.toCaseCredits`. The queries go into `ConsultationCaseRepository`, next to the customer's. `consultation_case_assigned_expert_id_idx` serves them. A case without an assigned expert matches no expert.
- **A cancelled case** is `PAST` on the list, and its card says "Cancelled", as the customer's card does. Its detail has `cancelledAt` and lists the appointments cancelled with it among the cancelled ones.

### Notes

03 for the expert, 04 for the customer, following UC-15 and spec decisions 5 to 7.

- **`Note`**: `NoteId`, `ConsultationCaseId`, `NoteTitle`, `NoteText`, `NoteVisibility`, `Instant writtenAt`, `Optional<Instant> editedAt`.

  | Method | Rule |
  |---|---|
  | `Note.write(consultationCaseId, title, text, visibility, now)` | a new id, `writtenAt = now`, never edited |
  | `edit(title, text, visibility, now)` | the same id, case and `writtenAt`; `editedAt = now`. Every save is an edit, also one that changes only the visibility, or nothing |
  | `isPublic()` | `visibility == PUBLIC` |

- **`AddNoteUseCase.invoke(CurrentUser, AddNoteCommand)`**: `CheckAssignedExpertPort.isAssignedExpert(caseId, user.id())`, otherwise `NotFoundException` ("Case <id> not found."); `Note.write(…, clock.instant())`; `SaveNotePort.add`; answer the note.
- **`EditNoteUseCase.invoke(CurrentUser, EditNoteCommand)`**: `LoadNotePort.load(noteId, caseId, user.id())`, otherwise `NotFoundException` ("Note <id> not found."); `edit(…, clock.instant())`; `SaveNotePort.update`; answer the note.
- **`DeleteNoteUseCase.invoke(CurrentUser, ConsultationCaseId, NoteId)`**: `DeleteNotePort.delete(noteId, caseId, user.id())`; `NotFoundException` when it deleted none.
- **No lock and no version** (spec decision 7): the last save wins, and a note that's gone is a 404.
- **In any status of the case** (UC-15): no use case reads the case's status.
- **Scoped in SQL**: `LoadNotePort` and `DeleteNotePort` find a note only by its id, its case's id and the case's `assigned_expert_id` together. So a note of another case under this case's path, a note of another expert's case, and an unknown one are all the same 404.
- **The order** is `written_at` descending, then `id` descending, in the query: an edit doesn't move a note.
- **The customer's notes** (04): `CustomerConsultationCase` gains `List<Note> notes`, which the adapter loads with `visibility = PUBLIC` in SQL; the record refuses a private one (`IllegalArgumentException`: a programming error). `CustomerNoteResponse` has no visibility.

### The expert profile

05 for the form's fields, 06 for the photo, following UC-14 and spec decisions 8, 9, 12 and 13.

- **`TeamsLink`** (05): a value object, `https://` followed by at least one printable ASCII character, without spaces; anything else is an `InvalidValueException` ("A Teams link is an https:// link."). Any host, not only Microsoft's (spec decision 8). Because of the rule, the frontend can put it into an `href` as it is.
- **`OwnExpertProfile`** (05): the `ExpertProfile` and `Optional<TeamsLink> teamsLink`.

  | Method | Rule | Task |
  |---|---|---|
  | `edit(DisplayName, String bio, Optional<TeamsLink>)` | replaces the three values; the id and the photo stay | 05 |
  | `changePhoto(ImageId)` | points at the new image | 06 |
  | `removePhoto()` | no photo | 06 |

- **`SaveExpertProfileUseCase.invoke(CurrentUser, SaveExpertProfileCommand)`**: lock the expert with `LockExpertPort.lock(user.id())`, load, `edit`, `SaveOwnExpertProfilePort.save`, answer the profile. The controller builds the `DisplayName` and the `TeamsLink`; a `teamsLink` of `null` or `""` is no link.
- **`UploadExpertPhotoUseCase.invoke(CurrentUser, byte[] file)`** and **`RemoveExpertPhotoUseCase.invoke(CurrentUser)`** (06) follow `UploadConsultationOfferingImageUseCase` and `RemoveConsultationOfferingImageUseCase`: lock the expert, load the profile, `Image.upload(file)`, save the image, point the profile at it, delete the old image, in one transaction.
- **Every profile write locks the expert's user row first** (spec decision 13, [D-27](../../decisions.md#architecture)), the lock that saving availability takes. No concurrency test.
- **A missing profile is a programming error** (spec decision 12): `LoadOwnExpertProfilePort.load` throws `IllegalStateException`, a 500. Every user with `EXPERT` has a seeded profile.
- **`ExpertProfile.photoId`** (06) is read wherever an `ExpertProfile` is built: the shop's offering detail (`ShopOfferingAdapter`), the expert picker (`ConsultationBookingAdapter`) and the customer's case (`CustomerConsultationCaseAdapter`). The admin's list of experts (`Expert`) doesn't change.

### The link window

07, following UC-10, [D-19](../../decisions.md#views-and-calls) and spec decisions 2, 10 and 11.

- **`LinkWindow`** (`domain.shared`, so the training days can use it): the constant `OPENS_BEFORE_START`, one hour; `opensAt(Instant start)`; `isOpenAt(Instant start, Instant end, Instant asOf)`: from `opensAt` on, and before `end`.
- **The customer**: `CustomerConsultationCase` gains `Optional<TeamsLink> expertTeamsLink`, which its query reads from the assigned expert's profile. `CustomerUpcomingAppointment.of` takes it and derives `linkAvailableFrom` (`LinkWindow.opensAt`), `linkWindowOpen` (`LinkWindow.isOpenAt` at `asOf`) and `teamsLink`: the expert's link while the window is open, otherwise empty. The record refuses a link with a closed window. A past appointment has no link field at all.
- **The expert**: `ExpertConsultationCase.teamsLink` is the expert's own link, whenever the case is shown. The page puts **Join** on each upcoming appointment, which the server lists, so a past appointment has none.
- **The link is read when the case is shown**, never copied to an appointment: a changed link reaches every upcoming appointment.

### Ownership

- **Scoped queries**: every query of the expert's cases and notes filters by the case's `assigned_expert_id` in SQL. Another expert's case is empty, like an unknown id: 404 on every route under `/api/expert/consultation-cases/{id}`, and nothing changes ([security.md](../../architecture/security.md#ownership-in-use-cases)).
- **The customer's view is the customer's, whoever holds it**: an expert who booked themself sees the case on **My cases** through the expert's routes, with every note, and on **My purchases** through the customer's routes, with the public notes and the link only inside the window.
- **The profile** is always the current user's.

## Frontend

```
frontend/src
├─ app/               App: /expert/cases (01), /expert/cases/:id (02), /expert/profile (05) under RequireRole role="EXPERT";
│                     Layout: My cases (01) before Availability, Profile (05) after it
├─ components/        ExpertPhoto (06)
└─ features/
   ├─ user/           landing.ts: EXPERT lands on /expert/cases (01);
   │                  queries.ts: useExpertProfile, useSaveExpertProfile (05), useUploadExpertPhoto, useRemoveExpertPhoto (06);
   │                  ExpertProfilePage, ExpertProfileForm (05), ExpertPhotoSection (06)
   └─ consultation/   expert-queries.ts: useExpertConsultationCases (01), useExpertConsultationCase (02),
                      useAddNote, useEditNote, useDeleteNote (03);
                      CaseGroups (01, from MyPurchasesPage), MyCasesPage, ExpertConsultationCaseCard (01);
                      ExpertConsultationCasePage (02); NoteCard, NoteDialog (03);
                      ConsultationCasePage: "Notes from your expert" (04), the expert's photo (06), Join (07);
                      link-window-reload.ts (07)
```

No new shadcn component: the note's dialog uses `dialog`, `textarea` and `radio-group`, which exist.

### Header and landing page

01 and 05, following spec decision 1.

- **The header**: **Shop**, **My purchases** (`CUSTOMER`), **My cases**, **Availability**, **Profile** (`EXPERT`), **Consultations** (`ADMIN`). 01 adds **My cases**, 05 adds **Profile**.
- **`landingPath`** (01): a user with `EXPERT` lands on `/expert/cases`; otherwise as before, `ADMIN` on `/admin/consultations`, then `CUSTOMER` on `/my`, then the shop.

### My cases

01. `MyCasesPage` at `/expert/cases`.

- **`CaseGroups`**: the three groups of `MyPurchasesPage` (Upcoming with "(Zurich time)", Needs booking, Past collapsed with its count, an empty group hidden) move into one component that takes the cases, each with an `id` and a `group`, and a function that renders a card. Both pages use it, so the groups can't drift apart.
- **`ExpertConsultationCaseCard`**: the customer's name as the title, which links to `/expert/cases/:id`; the offering's name; "Next: 17 Nov 2026, 10:00–11:00", or "Now: …" while it runs; "2 of 3 credits open". A cancelled case says "Cancelled" in place of the next appointment and the credits. No image.
- **Without cases**: "No cases yet. A case appears here once a customer books you."

### The expert's case detail

`ExpertConsultationCasePage` at `/expert/cases/:id`, following `ConsultationCasePage`.

| Part | Content | Task |
|---|---|---|
| Heading | a link back to **My cases**; the customer's name as the title; the email as a `mailto:` link; the offering's name | 02 |
| Terms | "3 sessions", "60 minutes per session", "Free cancellation up to 24 h before a session", "Bought 9 Nov 2026, 15:00 (Zurich time)", worded as on the customer's case; no price | 02 |
| Credits | "2 of 3 credits open"; a cancelled case says instead "Cancelled on 15 Nov 2026, 12:00 (Zurich time)", without a refund outcome | 02 |
| Upcoming appointments | soonest first; a running one says "Now" | 02 |
| | each with **Join**, or "Add your Teams link in your profile." with a link to **Profile** | 07 |
| Past appointments | newest first, "Consumed" | 02 |
| Cancelled appointments | newest first, "Cancelled, credit refunded" or "Cancelled, credit forfeited" (spec decision 14) | 02 |
| Notes | **Add note**, and the notes, newest first | 03 |
| A 404 | "Case not found", as on the customer's case | 02 |

Each list of appointments is hidden while it's empty and has "(Zurich time)" once, next to its heading.

### Notes on the pages

03 for the expert, 04 for the customer.

- **`NoteCard`**: a note's title, its text with its line breaks (`whitespace-pre-line`), "9 Nov 2026, 15:00" from `writtenAt`, and "edited 10 Nov 2026, 09:00" once it has an `editedAt`; a slot for what only the expert has. Both pages use it.
- **The expert's notes** (03): on each card the badge "Private" or "Public", **Edit** and **Delete**. **Add note** and **Edit** open `NoteDialog`: a form with **Title**, **Text** (a `textarea`) and the visibility as two radio buttons, **Private** (the default of a new note) and **Public**; React Hook Form with `NoteRequest` as its values, no rules in the form, a 400's messages next to the fields (`showSaveError`). It closes after the save. **Delete** asks with `ConfirmDialog`: "Delete the note "First session"?", "This can't be undone.", **Cancel** and **Delete**.
- **A note that's gone** (a 404 of a save or a delete): the dialog says "This note no longer exists." with only **Close**, and the case reloads.
- **The customer's notes** (04): a section "Notes from your expert" below the appointments, with a `NoteCard` each and "(Zurich time)" next to the heading; hidden while there's none (spec decision 6).

### Profile

`ExpertProfilePage` at `/expert/profile`.

- **`ExpertProfileForm`** (05): **Display name**, **Bio** (a `textarea`) and **Teams link**, following `ConsultationOfferingForm`: `ExpertProfileRequest` as the form's values, no rules in the form, a 400's messages next to the fields, "Saved" until the next change, and the saved profile's values as the new defaults. An empty Teams link is sent as `""`.
- **`ExpertPhotoSection`** (06), following `OfferingImageSection`: the photo or the placeholder, **Upload photo** or **Replace photo**, and **Remove photo**, each taking effect at once, apart from the form's **Save**; a 400's `detail` shows in the section.
- **`ExpertPhoto`** (06, `components/`): a round picture from `/api/images/{photoId}`, cropped by the browser (`object-cover`), or the round placeholder; `className` sizes both. It's built on `OfferingImage`. It replaces the round `ImagePlaceholder` on the shop's offering detail, in the booking dialog's expert picker and on the customer's case.

### Join

07, on both case details.

| Who | The upcoming appointment shows | When |
|---|---|---|
| Customer | "The link will be available from 09:00 (Zurich time)." (`formatTime(linkAvailableFrom)`) | `linkWindowOpen` is false, with or without a link |
| Customer | **Join** | `linkWindowOpen` and a `teamsLink` |
| Customer | "The meeting link has not been provided yet." | `linkWindowOpen` without a `teamsLink` |
| Expert | **Join** | the case has a `teamsLink` |
| Expert | "Add your Teams link in your profile.", "your profile" linking to `/expert/profile` | the case has none |

- **Join** is a link styled as a button, `target="_blank"` with `rel="noreferrer"`.
- **The window opens while the page is open** (spec decision 11): `msUntilLinkWindowReload(upcomingAppointments, now)` in `link-window-reload.ts` answers how long to wait until the soonest `linkAvailableFrom` of an appointment whose window isn't open, and `ConsultationCasePage` then refetches the case. It answers nothing when there's no such appointment or the time is more than a day away (a browser's timer can't wait longer than 24.8 days), and 30 seconds when the time has passed on the browser's clock but the server still says closed, so that a browser clock running ahead doesn't reload in a loop.

### Queries

- **`expert-queries.ts`**: the keys are `['expert', 'consultation-cases']` for the list and `[…, id]` for a case, as the customer's. A 401 has an empty body, so the hooks check `data === undefined`, as the customer's do.
- **The notes' mutations** (03) invalidate `['expert', 'consultation-cases']` and `['customer', 'consultation-cases']`, after a save, a delete and a 404: an expert who is the case's customer has both pages.
- **The profile's mutations** (05, 06) invalidate every query (`queryClient.invalidateQueries()`): the profile shows in the shop, in the expert picker, on cases and in the admin's list of experts.

## Tests

| What | How |
|---|---|
| Times | The `TestClock`. The scenarios' dates: now is Mon 9 Nov 2026, 15:00 (Zurich); Expert 2 is available on 17 Nov; Customer 1 buys Career Coaching and books Expert 2 on 17 Nov, 10:00–11:00, all through the API. |
| Test data | A test sets an expert's Teams link and photo through the API as that expert. `delete-test-data.sql` deletes the notes and restores the seeded profiles ([schema](#schema)). |
| Role matrix | A row per new route, `EXPERT`: the case routes on an unknown case (404), the notes' with a valid body; the profile's `PUT` with a body the domain refuses, such as a display name of no-break spaces (400), so an allowed caller changes no data; the photo's `PUT` with `Route.withImage` as the offering's. |
| Ownership | Expert 1 on Customer 1's case, which has Expert 2: the detail, and adding, editing and deleting a note, each 404, and nothing changes (02, 03). A note of another case of the same expert under this case's path: 404 (03). Each expert's list has only their own cases, and a case without an expert is in none (01). Each expert reads and saves only their own profile (05). |
| Security | The customer's case has the public note and not the private one: the body contains neither its id nor its title (04, [security.md](../../architecture/security.md#testing)). The customer's case has no Teams link at 08:59 and at 11:00, and has it at 09:00 and at 10:30; with the link set, the bodies of the shop's offering detail, the eligible experts and the customer's case outside the window don't contain it (07). |
| REST | Per route, the happy path. Besides: the groups and the order of **My cases**, an expert who booked themself, an expert removed from the eligible ones (01); the detail with a consumed appointment, one cancelled in time and one cancelled late, and of a cancelled case (02); a note on an active, a completed and a cancelled case, the 400s, an edit's `editedAt` and place, a second delete (03); the profile's 400s, an empty bio and an empty link (05); upload, replace and remove with the old image's 404, a file of another type and one over 2 MB (06); the link after the expert changed it, and without one (07). |
| Concurrency | None (spec decisions 7 and 13). |
| Domain | `ExpertConsultationCaseSummary`: the next appointment and the group at a time (01). `ExpertConsultationCase`: the three lists at a time, a running appointment, the cancelled ones' order (02). `Note`: `write`, `edit` with each change and with none; `NoteTitle` and `NoteText` blank (03). `CustomerConsultationCase` refuses a private note (04). `TeamsLink`: `https://` only, no spaces, not empty; `OwnExpertProfile.edit` (05), `changePhoto` and `removePhoto` (06). `LinkWindow`: just before, at and after both ends; `CustomerUpcomingAppointment`: the link only in the window, a running appointment, no link at the expert (07). |
| Frontend | Vitest for logic only ([D-29](../../decisions.md#architecture)): `landingPath` (01), `msUntilLinkWindowReload` (07). The pages and dialogs are checked by hand with Playwright. |

## Commands

No new commands.
