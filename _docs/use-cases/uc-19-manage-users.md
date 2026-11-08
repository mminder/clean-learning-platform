# UC-19 Manage users

| | |
|---|---|
| Actor | admin |
| Scope | MVP |
| Related | [UC-01](uc-01-login.md), [security](../architecture/security.md) |

## Goal

The admin decides who is an expert or an admin.

## Preconditions

- The user has `ADMIN`.
- Real OIDC login is in place ([UC-01](uc-01-login.md), MVP). Users exist after their first login.

## Main flow

1. The admin opens **Users**. The system lists all users with their name, email and roles.
2. The admin grants or revokes `EXPERT` or `ADMIN` on a user.
3. The system stores the change. It takes effect on the user's next request.

## Alternative and error flows

- See the open questions below.

## Business rules

- `CUSTOMER` is granted automatically at the first login (just-in-time provisioning, D-04) and isn't managed here.
- Granting `EXPERT` creates an empty expert profile ([UC-14](uc-14-manage-expert-profile.md)).

## Open questions (decide before implementing)

- May an admin revoke their own `ADMIN` role? Proposal: no, to prevent lock-out.
- What happens when `EXPERT` is revoked while the expert has future appointments or assigned runs? Proposal: block it until those are handed over.
- What happens to the offerings a revoked expert is eligible for? Today the offering form hides the expert but still sends their id, so its save answers 400 ([task 05 of increment 02](../tasks/02-manage-consultation-offerings/05-eligible-experts.md#notes)).
- A session whose user no longer exists: `GET /api/me` answers 404 today; 401 with the session ended fits better.

## Visibility and authorization

- Only admins.

## Out of scope

- Creating users manually, deactivating or deleting users, GDPR export.
