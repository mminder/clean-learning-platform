import { Button } from '@/components/ui/button'

/**
 * **Join** (UC-10): a link styled as a button that opens the call's Teams link in a new tab, without a referrer. The
 * server sends the link only to whom and when it may (D-19); `TeamsLink`'s rule makes it an `https://` link.
 */
export function JoinButton({ teamsLink }: { teamsLink: string }) {
  return (
    <Button asChild size="sm">
      <a href={teamsLink} target="_blank" rel="noreferrer">
        Join
      </a>
    </Button>
  )
}
