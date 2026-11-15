import { describe, expect, it } from 'vitest'
import { toRequest, type ExpertProfile } from './queries'

describe('toRequest', () => {
  const profile: ExpertProfile = {
    displayName: 'Anna Expert',
    bio: 'Coach for careers.',
    photoId: '00000000-0000-0000-0000-000000003001',
    teamsLink: 'https://teams.microsoft.com/l/meetup-join/anna',
  }

  it('keeps the name, the bio and the link, and leaves out the photo', () => {
    expect(toRequest(profile)).toEqual({
      displayName: 'Anna Expert',
      bio: 'Coach for careers.',
      teamsLink: 'https://teams.microsoft.com/l/meetup-join/anna',
    })
  })

  it('makes no link an empty field', () => {
    expect(toRequest({ ...profile, teamsLink: null }).teamsLink).toBe('')
  })
})
