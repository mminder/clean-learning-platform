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
| `/admin/consultations`, `/admin/consultations/:id` | Consultation offerings (UC-16) | admin |
| `/admin/trainings`, `/admin/trainings/:id` | Training offerings and runs (UC-17) | admin |
| `/admin/cases`, `/admin/cases/:id` | All cases (UC-18) | admin |
| `/admin/users` | Users (UC-19, MVP) | admin |

## Navigation and auth

- On start, the SPA calls `GET /api/me`. A 401 means visitor.
- The header shows **Shop** plus one section per role the user holds (union navigation, D-03).
- After login, the SPA redirects to the landing page by priority Expert (`/expert/cases`) → Admin (`/admin/cases`) → Customer (`/my`), or back to the page where login was started (e.g. from **Buy**).
- Route guards only hide pages. **The backend is the authority**; a 401 from any call sends the user to login.
- CSRF: the client reads the `XSRF-TOKEN` cookie and sends `X-XSRF-TOKEN` on mutating requests.

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

## Errors

- A `ProblemDetail` response with status 409 carries a stable `code`, which maps to a user-friendly message (toast or inline). Field errors on 400 are shown next to the form fields.

## Development

- `npm run dev` starts Vite on `:5173` and proxies `/api` to `http://localhost:8080`, giving the same origin as in production.
- The production build (`dist/`) is bundled into the Spring Boot jar by the Maven build ([overview](overview.md#runtime-and-profiles)).
