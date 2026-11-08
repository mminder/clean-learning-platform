# UC-14 Manage expert profile

| | |
|---|---|
| Actor | expert |
| Scope | PoC |
| Related | [UC-02](uc-02-browse-shop.md), [UC-06](uc-06-book-consultation-appointment.md), [UC-10](uc-10-join-call.md) |

## Goal

The expert maintains how customers see them and the Teams link for their consultations.

## Preconditions

- The user has `EXPERT`.

## Main flow

1. The expert opens **Profile**.
2. The system shows the profile form:
   - display name (defaults to the user's name);
   - bio (plain text);
   - photo (upload, replace, remove);
   - personal Teams meeting link.
3. The expert edits and saves.
4. The system stores the profile. A replaced photo is deleted and the new one gets a new image id.

## Alternative and error flows

- **The display name is blank:** 400. The bio may be empty.
- **The photo isn't JPEG, PNG or WebP, or is larger than 2 MB:** 400.
- **The Teams link isn't an `https://` URL:** 400.

## Business rules

- The profile appears in the expert picker ([UC-06](uc-06-book-consultation-appointment.md)), on consultation offering details ([UC-02](uc-02-browse-shop.md)), and on case and run details.
- The Teams link is used for every consultation appointment of this expert ([UC-10](uc-10-join-call.md)).
- Images follow D-21.

## Visibility and authorization

- An expert edits only their own profile.
- Display name, bio and photo are public. The Teams link is visible only according to [UC-10](uc-10-join-call.md).

## Out of scope

- Profiles for customers or admins, image cropping or resizing.
