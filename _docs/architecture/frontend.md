# Frontend architecture

## Stack

| Concern | Choice |
|---|---|
| Build | Vite, TypeScript (strict), npm |
| UI | React, Tailwind, shadcn/ui (Radix-based components copied into the repo) |
| Routing | React Router (declarative mode) |
| Server state | TanStack Query |
| Forms | React Hook Form (`useFieldArray` for packages, days, time slots, runs) |
| Dates | date-fns + `@date-fns/tz`, always `Europe/Zurich` |
| API client | generated: openapi-typescript (types) + openapi-fetch (client) |
| Tests | Vitest + Testing Library where there's logic; Playwright E2E in the MVP |
| Lint/format | ESLint + Prettier |

## Structure

```
frontend/src
├─ app/                 router, providers (QueryClient), layout, header navigation
├─ api/                 generated schema types + configured openapi-fetch client
├─ components/          shared components, e.g. ImagePlaceholder
│  └─ ui/               shadcn/ui components
├─ lib/                 date formatting (Zurich), money formatting, error mapping
└─ features/            mirrors the backend features
   ├─ shop/             UC-02, buy dialogs (UC-03, UC-04)
   ├─ consultation/     UC-05 (cases), UC-06, UC-07, UC-08, UC-11, UC-15, UC-18
   ├─ training/         UC-05 (enrollments), UC-09, UC-12
   ├─ availability/     UC-13
   ├─ offering/         UC-16, UC-17 (admin)
   └─ user/             UC-01, UC-14, UC-19
```

A feature folder contains its pages, components, query and mutation hooks, and its tests.

A shadcn/ui component is added when a page first uses it: run `npx shadcn@latest add <component>` in `frontend/`, then `npm run format`. The components import `cn` from shadcn's npm package `cn` (since 2026-11-08, through `npx shadcn@latest migrate cn`), and so does our own code; there's no `lib/utils.ts`. `components.json` still has the `utils` alias (`@/lib/utils`): the CLI's schema requires it, and the CLI rejects the config without it.

## Routes

| Path | Page | Access |
|---|---|---|
| `/` | Shop (UC-02) | public |
| `/offerings/:id` | Offering detail (UC-02) | public |
| `/login` | PoC user picker (UC-01) | public |
| `/my` | My purchases (UC-05) | customer |
| `/my/cases/:id` | Case detail (UC-05–08, 10) | customer |
| `/my/enrollments/:id` | Enrollment detail (UC-05, 09, 10) | customer |
| `/expert/cases`, `/expert/cases/:id` | My cases, notes (UC-11, 15) | expert |
| `/expert/trainings`, `/expert/trainings/:id` | My trainings (UC-12) | expert |
| `/expert/availability` | Availability (UC-13) | expert |
| `/expert/profile` | Profile (UC-14) | expert |
| `/admin/consultations`, `/admin/consultations/new`, `/admin/consultations/:id` | Consultation offerings (UC-16) | admin |
| `/admin/trainings`, `/admin/trainings/:id` | Training offerings and runs (UC-17) | admin |
| `/admin/cases`, `/admin/cases/:id` | All cases (UC-18) | admin |
| `/admin/users` | Users (UC-19, MVP) | admin |

## Navigation and auth

- On start, the SPA calls `GET /api/me`. A 401 means visitor.
- The header shows **Shop** plus one section per role the user holds (union navigation, D-03).
- After login, the SPA redirects to the landing page by priority Expert (`/expert/cases`) → Admin (`/admin/cases`) → Customer (`/my`), or back to the page where login was started (e.g. from **Buy**).
- Route guards only hide pages. **The backend is the authority**. `RequireRole` (`app/`) wraps a section's routes: a visitor goes to `/login?redirect=<path>`, a user without the role sees the not-found page.
- A 401 from any query or mutation other than `useMe` refetches `['me']` (the `QueryClient` in `app/queryClient.ts`), so `RequireRole` then sends the user to the login.
- After login, the SPA resets the query cache, so no data of the visitor or the previous user remains, and the queries on the screen load again. After logout, it loads the shop as a new page (`window.location.assign('/')`), so nothing of the user stays in memory. Not a reset and a navigation inside the SPA: the reset empties `['me']`, and `RequireRole` on the page being left would send the visitor to the login instead of the shop.
- CSRF: a middleware of the API client (`api/client.ts`) reads the `XSRF-TOKEN` cookie on each request and sends it as `X-XSRF-TOKEN` on `POST`, `PUT`, `PATCH` and `DELETE` ([security](security.md#authentication)).

## Data fetching

- Every API call goes through the generated `openapi-fetch` client, wrapped in TanStack Query hooks per feature.
- After a mutation, the hook invalidates the affected queries (e.g. a booking invalidates the case detail and "my purchases"). There's no manual state syncing.
- No global client state library. Server state lives in TanStack Query, UI state in components.

## API contract

- `backend/openapi.json` is the contract (D-29). `npm run generate:api` regenerates `src/api/schema.d.ts` from it.
- API DTOs are never hand-written. When the backend changes the contract, the frontend types change in the same commit.

## Dates, times and money

- All formatting goes through `lib/` helpers, which always use `Europe/Zurich` and show a "(Zurich time)" hint where times appear.
- The frontend never decides a business rule (deadline passed, seats left, link window). It shows what the API returns, for example `canCancel`, `refundIfCancelledNow` and `teamsLink`.
- Money is formatted as `CHF 1,800.00` from the API's `{ amount, currency }`.

## Forms

UC-16's `ConsultationOfferingForm` is the pattern ([increment 02](../tasks/02-manage-consultation-offerings/README.md#forms-and-mutations)):

- **React Hook Form**, with the generated `*Request` type as the form's values. One form serves the new and the edit page.
- **No rules in the form.** Inputs have their types (text, number), but no `required` or `min`: the backend's 400 is the one source of the rules, and its messages show next to the fields.
- **Number inputs** use `valueAsNumber`. An empty one is `NaN`, which JSON sends as `null`, so the backend answers "must not be null" next to the field. A new form starts with `NaN` for an empty number field.
- **Saving** calls the mutation's `mutateAsync`. When it fails, `showSaveError` (`lib/problem.ts`) puts a 400's field errors on the fields with `setError`, or returns the message to show above the form. After a save, the saved offering's values become the form's defaults (`reset(toRequest(saved))`), and "Saved" shows until the next change. The saved offering, not the submitted values, because it carries the ids the server gave to new rows.
- **Lists of rows** (the packages) use `useFieldArray`: one row per entry, **Remove** on each, **Add package** below. A row's server `id` stays in the form's values, unseen, so a save names the rows it keeps; `fields[i].id` is only React's key. A new row starts empty (`NaN` for a number, `''` for a text), not at 0. Field errors land on the row (`packages.1.price`).
- **Mutations** invalidate what they change and return the invalidation, so they stay pending until the queries on the screen have reloaded.

## Errors

- A 409's `detail` is the message for the user, and the page shows it (inline or as a toast). Its stable `code` lets a page react to one conflict, for example by reloading stale data ([D-34](../decisions.md#architecture)). Field errors on 400 are shown next to the form fields; a 400 without a field list shows its `detail`.
- The frontend reads an error body only through the generated `ProblemDetail` type (`components['schemas']['ProblemDetail']`, [D-34](../decisions.md#architecture)), and only a 400's or a 409's: `problemOf(status, error)` in `lib/problem.ts` returns the body for those and `undefined` otherwise. openapi-fetch types every error body as `ProblemDetail`, because `openapi.json` lists it on every route, but a 403's body is Boot's error JSON. For 401, 403 and 500 the frontend decides on the status alone.
- A hook throws `ApiError` for every non-2xx answer, with the status and, for a 400 or 409, the problem (`ApiError.problem`). Field paths become form paths with `toFormPath` (`packages[1].price` → `packages.1.price`).

## Development

- `npm run dev` starts Vite on `:5173` and proxies `/api` to `http://localhost:8080`, giving the same origin as in production.
- The production build (`dist/`) is bundled into the Spring Boot jar by the Maven build ([overview](overview.md#runtime-and-profiles)).
