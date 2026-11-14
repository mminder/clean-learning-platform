import { describe, expect, it } from 'vitest'
import { isLocalPath, landingPath, pathAfterLogin } from './landing'

describe('landingPath', () => {
  it('is My cases for a user with EXPERT, before ADMIN and CUSTOMER', () => {
    expect(landingPath(['EXPERT'])).toBe('/expert/cases')
    expect(landingPath(['EXPERT', 'ADMIN'])).toBe('/expert/cases')
    expect(landingPath(['CUSTOMER', 'EXPERT'])).toBe('/expert/cases')
  })

  it('is the consultation offerings for a user with ADMIN and without EXPERT, before CUSTOMER', () => {
    expect(landingPath(['ADMIN'])).toBe('/admin/consultations')
    expect(landingPath(['CUSTOMER', 'ADMIN'])).toBe('/admin/consultations')
  })

  it('is My purchases for a user with only CUSTOMER', () => {
    expect(landingPath(['CUSTOMER'])).toBe('/my')
  })

  it('is the shop for a user without any of the roles', () => {
    expect(landingPath([])).toBe('/')
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
    expect(pathAfterLogin(null, ['EXPERT', 'ADMIN'])).toBe('/expert/cases')
    expect(pathAfterLogin('//example.com', ['CUSTOMER'])).toBe('/my')
  })
})
