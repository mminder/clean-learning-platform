import { describe, expect, it } from 'vitest'
import { isLocalPath, landingPath, pathAfterLogin } from './landing'

describe('landingPath', () => {
  it('is the shop for every role, until the landing pages exist', () => {
    expect(landingPath(['CUSTOMER'])).toBe('/')
    expect(landingPath(['CUSTOMER', 'EXPERT'])).toBe('/')
    expect(landingPath(['ADMIN'])).toBe('/')
  })
})

describe('isLocalPath', () => {
  it('accepts a path on this site', () => {
    expect(isLocalPath('/')).toBe(true)
    expect(isLocalPath('/offerings/00000000-0000-0000-0000-000000001001')).toBe(
      true,
    )
    expect(isLocalPath('/admin/consultations?tab=all#top')).toBe(true)
  })

  it('rejects another site', () => {
    expect(isLocalPath('//example.com')).toBe(false)
    expect(isLocalPath('https://example.com/')).toBe(false)
    expect(isLocalPath('/\\example.com')).toBe(false)
    expect(isLocalPath('/\t/example.com')).toBe(false)
    expect(isLocalPath('javascript:alert(1)')).toBe(false)
  })

  it('rejects a path without a leading slash', () => {
    expect(isLocalPath('offerings')).toBe(false)
    expect(isLocalPath('')).toBe(false)
  })
})

describe('pathAfterLogin', () => {
  it('opens the redirect if it is a local path', () => {
    expect(pathAfterLogin('/offerings/1', ['CUSTOMER'])).toBe('/offerings/1')
  })

  it('opens the landing page without a redirect or with another site', () => {
    expect(pathAfterLogin(null, ['ADMIN'])).toBe('/')
    expect(pathAfterLogin('//example.com', ['ADMIN'])).toBe('/')
  })
})
