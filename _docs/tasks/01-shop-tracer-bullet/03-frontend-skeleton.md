# 03 Frontend skeleton

| | |
|---|---|
| Increment | [01 Shop tracer bullet](../../specs/01-shop-tracer-bullet.md) |
| Type | AFK |
| Blocked by | – |
| Status | Todo |

## Goal

A React SPA with the agreed stack, a layout and routing, and working lint, format, test and build commands. It shows an empty shop page.

## What to build

Read first: [frontend.md](../../architecture/frontend.md), and the plan's [toolchain](README.md#toolchain) and [frontend](README.md#frontend) sections.

- **`frontend/`:** a Vite React TypeScript project, with npm and Node 22 pinned in `.nvmrc`. TypeScript runs in `strict` mode, and `@/` is an alias for `src/`, as shadcn/ui expects.
- **Styling:** Tailwind 4 through its Vite plugin, and shadcn/ui initialized (`components.json`, `src/lib/utils.ts`). Add components only when a page uses them.
- **App shell** in `src/app/`: a `QueryClientProvider`, React Router in declarative mode, and a layout whose header shows the product name and a **Shop** link to `/`. Routes: `/` renders a `ShopPage` placeholder in `features/shop/`, and `*` renders a `NotFoundPage` with a link back to the shop.
- **Dev server:** proxies `/api` to `http://localhost:8080`.
- **Tooling:** ESLint (flat config with typescript-eslint, react-hooks and react-refresh), Prettier, and Vitest with Testing Library and jsdom. Scripts: `dev`, `build` (type-check, then build), `preview`, `lint`, `format`, `format:check` and `test` (`vitest run`).
- **Docs:** frontend commands in [AGENTS.md](../../../AGENTS.md#commands), and `.gitignore` entries for `node_modules/` and `dist/`.

## Acceptance criteria

- [ ] `npm ci && npm run lint && npm run format:check && npm test && npm run build` passes.
- [ ] `npm run dev` serves the layout at `http://localhost:5173`. `/` shows the shop placeholder, and `/nope` shows the not-found page.
- [ ] A request from the dev server to `/api/...` is forwarded to port 8080.
- [ ] The folders match the plan: `app/`, `components/ui/`, `lib/`, `features/shop/`.
- [ ] AGENTS.md has the frontend commands.

## Tests

- One Vitest test that renders the app at an unknown path and finds the not-found page. It proves that the test setup and the router work.

## Out of scope

- The API client, `generate:api` and `check:api` ([06](06-shop-list.md)).
- Money and date formatting: added with the first page that needs them.

## Notes
