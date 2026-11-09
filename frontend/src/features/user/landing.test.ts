import { describe, expect, it } from 'vitest'
import { isLocalPath, landingPath, pathAfterLogin } from './landing'

describe('landingPath', () => {
  it('is My purchases for a user with CUSTOMER, until the expert has a landing page', () => {
    expect(landingPath(['CUSTOMER'])).toBe('/my')
    expect(landingPath(['CUSTOMER', 'EXPERT'])).toBe('/my')
  })

  it('is the shop for a user with neither ADMIN nor CUSTOMER', () => {
    expect(landingPath(['EXPERT'])).toBe('/')
    expect(landingPath([])).toBe('/')
  })

  it('is the consultation offerings for a user with ADMIN, before CUSTOMER', () => {
    expect(landingPath(['ADMIN'])).toBe('/admin/consultations')
    expect(landingPath(['EXPERT', 'ADMIN'])).toBe('/admin/consultations')
    expect(landingPath(['CUSTOMER', 'ADMIN'])).toBe('/admin/consultations')
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
    expect(pathAfterLogin(null, ['ADMIN'])).toBe('/admin/consultations')
    expect(pathAfterLogin('//example.com', ['CUSTOMER'])).toBe('/my')
  })
})
