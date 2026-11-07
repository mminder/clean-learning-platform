# UC-01 Log in / log out

| | |
|---|---|
| Actor | all users |
| Scope | PoC: user picker. MVP: OIDC login with just-in-time provisioning. |
| Related | [security](../architecture/security.md), [UC-19](uc-19-manage-users.md) |

## Goal

Identify the user, start a session, and take them to the right landing page.

## Preconditions

None.

## Main flow (PoC)

1. The visitor clicks **Log in** in the header.
2. The system shows the seeded users with their names and roles.
3. The visitor selects a user.
4. The system starts a session and opens the landing page (see business rules).

## Main flow (MVP)

1. The visitor clicks **Log in** and is redirected to the IdP.
2. After authenticating, the user returns to the platform.
3. The system looks up the user by OIDC subject. If none exists, it creates one with role `CUSTOMER` and the name and email from the ID token.
4. The system starts a session and opens the landing page.

## Log out

1. The user clicks **Log out** in the header.
2. The system ends the session and opens the shop.

## Alternative and error flows

- **Login started from "Buy":** after login, the user returns to the page where they clicked "Buy" ([UC-03](uc-03-purchase-consultation-package.md), [UC-04](uc-04-purchase-training-run.md)).
- **Session expired:** the next API call returns 401 and the frontend shows the login.

## Business rules

- The landing page goes by the first matching role: Expert → "My cases" ([UC-11](uc-11-view-my-consultation-cases.md)), Admin → "Cases" ([UC-18](uc-18-view-all-consultation-cases.md)), Customer → "My purchases" ([UC-05](uc-05-view-my-purchases.md)).
- The header shows a section for every role the user holds (union navigation, no role switcher).
- Seed users (PoC; two per role, as the authorization tests require):

| User | Roles |
|---|---|
| Customer 1 | CUSTOMER |
| Customer 2 | CUSTOMER |
| Expert 1 | EXPERT, CUSTOMER (to try self-booking, D-06) |
| Expert 2 | EXPERT, ADMIN |
| Admin 1 | ADMIN |
| Admin 2 | ADMIN |

## Visibility and authorization

- `POST /api/poc/login` exists only in the `poc` profile.
- `GET /api/me` returns the id, name, email and roles of the current user, or 401.

## Out of scope

- Registration, password reset and MFA inside the app. The IdP handles them.
- Customers editing their own profile.
