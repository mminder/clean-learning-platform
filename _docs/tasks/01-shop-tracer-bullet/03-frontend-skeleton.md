# 03 Frontend skeleton

| | |
|---|---|
| Increment | [01 Shop tracer bullet](../../specs/01-shop-tracer-bullet.md) |
| Type | AFK |
| Blocked by | – |
| Status | Done |

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

- [x] `npm ci && npm run lint && npm run format:check && npm test && npm run build` passes.
- [x] `npm run dev` serves the layout at `http://localhost:5173`. `/` shows the shop placeholder, and `/nope` shows the not-found page.
- [x] A request from the dev server to `/api/...` is forwarded to port 8080.
- [x] The folders match the plan: `app/`, `components/ui/`, `lib/`, `features/shop/`.
- [x] AGENTS.md has the frontend commands.

## Tests

- One Vitest test that renders the app at an unknown path and finds the not-found page. It proves that the test setup and the router work.

## Out of scope

- The API client, `generate:api` and `check:api` ([06](06-shop-list.md)).
- Money and date formatting: added with the first page that needs them.

## Notes

- **shadcn/ui was initialized by hand.** The `shadcn init` CLI needs `ui.shadcn.com`, which the cloud agent proxy blocks (403 on CONNECT). `components.json` (style `new-york`, base color `neutral`, CSS variables, `@/` aliases), `src/lib/utils.ts` (`cn`) and the neutral theme variables in `src/index.css` follow shadcn's manual installation guide for Tailwind 4. The `dark` variant is bound to a `.dark` class, so components' `dark:` classes stay off (Tailwind's default follows the OS setting); dark mode itself is not set up (YAGNI). No component is added yet. Without access to `ui.shadcn.com`, copy a component from `apps/v4/registry/new-york-v4/ui/<name>.tsx` in the `shadcn-ui/ui` repository (raw.githubusercontent.com) to `src/components/ui/<name>.tsx`. Change `from "cn"` to `from "@/lib/utils"`, `npm install` what it imports (for example `class-variance-authority` and `radix-ui`), and run `npm run format`.
- **Versions.** `create vite` now scaffolds Vite 8, TypeScript 7 and oxlint. We use Vite 8 and ESLint 10 as the plan says, and TypeScript `~6.0`, because typescript-eslint 8 supports TypeScript below 6.1 only. Move to TypeScript 7 when typescript-eslint supports it. React Router is pinned to `^7` (the latest major is 8).
- **Layout of `src/app/`.** `App` holds the routes and the layout; `Providers` holds the `QueryClientProvider`. `main.tsx` wraps them in `BrowserRouter`; the test wraps them in `MemoryRouter`. `NotFoundPage` lives in `app/`, next to the layout.
- **Test setup.** `src/test-setup.ts` loads the `@testing-library/jest-dom` matchers. Vitest is configured in `vite.config.ts`.
- **Checked in a browser** (Chromium, dev server): `/` shows the header (product name, **Shop** link) and the placeholder; `/nope` shows the not-found page, and its link goes back to `/`; `fetch('/api/shop/offerings')` reached a throwaway listener on :8080.
