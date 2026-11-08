import createClient from 'openapi-fetch'
import type { paths } from './schema'

// The API is on the SPA's own origin: Vite proxies /api in development, and the jar serves both in production.
export const client = createClient<paths>()
