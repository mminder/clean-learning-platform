# 07 Offering image

| | |
|---|---|
| Increment | [02 Manage consultation offerings](../../specs/02-manage-consultation-offerings.md) |
| Type | AFK |
| Blocked by | [06](06-offering-lifecycle.md) |
| Status | Done |

## Goal

The admin uploads, replaces and removes an offering's image. Images are stored in Postgres, served publicly with long cache headers, and shown in the admin page and in the shop instead of the placeholder.

## What to build

Read first: [D-21](../../decisions.md#cross-cutting), [UC-16](../../use-cases/uc-16-manage-consultation-offering.md) (image), [UC-14](../../use-cases/uc-14-manage-expert-profile.md) (the same rules, for photos later), and the plan's [schema](README.md#schema), [API](README.md#api) and [writes](README.md#writes) sections.

Work in this order, test first:

1. **A failing REST test:** Admin 1 uploads a small PNG for an offering; `GET /api/images/{imageId}` returns the same bytes as `image/png`.
2. **Schema:** `V5__create_image.sql` as in the plan's [schema](README.md#schema).
3. **Domain**, test first, in `domain.image`:
   - `ImageType` (`JPEG`, `PNG`, `WEBP`) with its content type, detected from the file's first bytes, not from the file name or the declared content type;
   - `Image` (id, type, bytes): not empty, at most 2 MB; anything else is an `IllegalArgumentException` (400);
   - `ConsultationOffering` gets an optional image id; changing it on an archived offering is refused (`WRONG_OFFERING_STATUS`).
4. **Port and adapter:** `ImageStorePort` (`save`, `load`, `delete`) in `application.image`, implemented by `ImageAdapter` with `ImageEntity`.
5. **Use cases:** `UploadConsultationOfferingImageUseCase` and `RemoveConsultationOfferingImageUseCase` lock the offering first, and replacing deletes the old image, as the plan's [writes](README.md#writes) section says. `GetImageUseCase` loads one image (`NotFoundException` for an unknown id). `DeleteConsultationOfferingUseCase` deletes the offering's image too.
6. **API:**
   - the routes from the plan: `PUT` and `DELETE` on `…/{id}/image`, and `ImageController` with `GET /api/images/{id}`;
   - multipart allows 2 MB per file (Spring's default is 1 MB); a larger upload answers 400, like a wrong type ([UC-14](../../use-cases/uc-14-manage-expert-profile.md#alternative-and-error-flows));
   - `GET /api/images/{id}` sends `Cache-Control: public, max-age=31536000, immutable`. Spring Security writes `no-cache` headers by default; make sure the response keeps this one;
   - `GET /api/images/**` is public in `SecurityConfig`, as [security.md](../../architecture/security.md#roles-by-route-prefix) already lists it;
   - `imageId` (`@Nullable`) in `ConsultationOfferingResponse`, `ShopOfferingResponse` and `ShopOfferingDetailResponse`;
   - role matrix rows: the image upload with no body, the removal with an unknown offering id, and `GET /api/images/{id}` with an unknown id.
7. **Cleanup:** the shared test cleanup script removes the images of the offerings it deletes.
8. **Frontend:**
   - `OfferingImage` in `components/`: the image when there's an `imageId`, otherwise `ImagePlaceholder`, in the same frame;
   - `OfferingImageSection` on `ConsultationOfferingPage` (not on the new page): the image, **Upload image** or **Replace image** (a file input that accepts JPEG, PNG and WebP) and **Remove image**; disabled for an archived offering; a 400 shows its `detail`;
   - the upload goes through the generated client with a `FormData` body;
   - the shop's `OfferingCard` and `OfferingDetailPage` use `OfferingImage`. Expert photos stay placeholders (UC-14).
9. **Docs:** [backend.md](../../architecture/backend.md) and [D-21](../../decisions.md#cross-cutting) say `ImageStorePort` and detection from the first bytes since planning; check that they match what you built.

## Acceptance criteria

- [x] On the dev server, demo steps 4 and 5 of the [spec](../../specs/02-manage-consultation-offerings.md#demo) work: the admin page shows the uploaded PNG, and the shop shows it on the card and the detail page (Playwright).
- [x] Replacing an image serves the new one under a new id, and the old id answers 404. After removing it, the shop shows the placeholder.
- [x] A GIF, a text file named `photo.png`, and a JPEG of 2 MB plus one byte each answer 400, and the offering keeps its image. A JPEG of exactly 2 MB is accepted.
- [x] `GET /api/images/{id}` works without a login, with the image's content type and `Cache-Control: public, max-age=31536000, immutable`.
- [x] Changing the image of an archived offering answers 409 `WRONG_OFFERING_STATUS`.
- [x] Deleting a draft with an image deletes the image too.
- [x] `openapi.json` and `schema.d.ts` are regenerated; the role matrix has the three routes; every check passes, including the jar build.

## Tests

- Domain: `ImageType` detection (JPEG, PNG, WebP, other), the size limit
- REST: upload, replace, remove, get; the invalid files; archived; delete with image; `imageId` in the shop
- Authorization: `RoleMatrixTest` rows for the three routes

## Out of scope

- Expert photos ([UC-14](../../use-cases/uc-14-manage-expert-profile.md)): they reuse the table, the port and `GET /api/images/{id}`.
- Resizing, cropping or converting images ([D-21](../../decisions.md#cross-cutting)).

## Notes

- **Domain:** `ImageType.detect` reads the signatures (JPEG `FF D8 FF`, PNG's eight bytes, WebP `RIFF`….`WEBP`); `ImageType.ofContentType` maps a stored row back. `Image` is a record whose bytes are copied in and out; `Image.upload(bytes)` checks the size first ("An image is not empty.", "An image is at most 2 MB."), then the type ("An image is a JPEG, PNG or WebP file."), all `IllegalArgumentException` (400). `ConsultationOffering` holds `Optional<ImageId>` (the domain can't use `@Nullable`, as with `ConsultationPackageEntry`); `changeImage` and `removeImage` refuse an archived offering, and `edit` keeps the image. `ShopOffering` and `ShopConsultationOffering` carry the image id, too.
- **Use cases:** upload locks, builds the `Image`, asks the offering, saves the image, updates the offering and deletes the old image; remove locks, asks, updates, deletes. Delete removes the image through `ImageStorePort` after `DeleteConsultationOfferingPort.delete`. All writes go to one persistence context, and Hibernate flushes inserts, then updates, then deletes in the order of the `remove` calls, so the foreign key always holds. `ImageAdapter.delete` loads the image (up to 2 MB) to remove it: harmless at this scale.
- **Schema:** `offering.image_id` is `unique` (an image belongs to one offering; its index serves the foreign key check on delete). The plan's schema says so. `ImageCheckConstraintsTest` covers the type and size checks, and `UpdatedAtTriggersTest` the `image` trigger.
- **The real upload limit:** MockMvc builds multipart requests itself, so it never meets the servlet container's limit; the domain's size rule gives the 400 there. To test the limit itself, `@IntegrationTest` now also starts the server on a random port (still one shared context), and `ImageUploadLimitTest` logs in through the PoC login over HTTP and uploads 2 MB (200) and 2 MB plus one byte (400, the offering keeps its image). Checked: without `max-file-size` the 2 MB upload answers 400 (Spring's 1 MB default), and without the handler the larger one answers 413. `max-request-size` is 3 MB, room for the multipart framing. A 5 MB file also answered 400 in the browser, not a reset connection.
- **415 for a body that isn't multipart:** the route `consumes` multipart. Without it, a `PUT` without a multipart body ended in Spring's `MultipartException`, a 500. The role matrix row for the upload sends no body, so it gets 415; the plan's route table says so. The overall review lists the 415 in `openapi.json` (`@ProblemResponses.Status415`).
- **`GET /api/images/{id}`** returns a `Resource`, so `openapi.json` says `format: binary`, not `byte` (base64) as for a `byte[]`. `produces` lists the three types. The controller sets `Cache-Control` as a literal string (Spring's `CacheControl` builder writes `max-age` first); Spring Security's header writer leaves a response with its own `Cache-Control` alone, without `Pragma` or `Expires` (`ImageControllerTest`).
- **Worth a look:** a visitor's first request without the `XSRF-TOKEN` cookie gets `Set-Cookie` on the image response too, which is `public`. Browsers don't share it, and a shared cache usually doesn't store a response with `Set-Cookie`; there's no CDN in the PoC. If one comes (MVP hosting), keep the CSRF cookie off `/api/images/**` or tell the CDN not to store `Set-Cookie`.
- **Frontend:** `useUploadConsultationOfferingImage` sends a `FormData` through the generated client; the contract types the part as a string (format binary), so the body is cast once, with a comment. openapi-fetch sets no JSON content type for a `FormData`, so the browser sends `multipart/form-data; boundary=…`, and the CSRF middleware adds `X-XSRF-TOKEN` (a Vitest test in `client.test.ts`; checked in the browser). Both mutations invalidate the offerings and the shop and refetch on a 409. `OfferingImageSection` sits above the form on `ConsultationOfferingPage` only; its buttons and the file input are disabled for an archived offering, and an unsaved edit in the form survives an image change. The shop's detail page now shows the offering's image, or the placeholder, above the name; it showed none before.
- **No repository for `ImageAdapter`:** it only finds, persists and removes by id, so it uses the `EntityManager` like `ConsultationOfferingAdapter`'s writes. The plan's package table says so.
- **Browser check** (Playwright, dev server with the `poc` backend, then the jar with Admin 2 and "Lean Coaching"): demo step 3; the new page has no image section; step 4: a PNG uploaded, shown on the page, the request was `multipart/form-data` with the CSRF header; a visitor's `GET /api/images/{id}` answered `image/png` with `Cache-Control: public, max-age=31536000, immutable` and no `Pragma` or `Expires`. A GIF, a text file named `photo.png`, a JPEG of 2 MB plus one byte and one of 5 MB each showed their 400 `detail` under the buttons, and the page kept its image; a JPEG of exactly 2 MB was accepted. Replacing gave a new id, and the old id answered 404. Step 5: published, the shop's card and the detail page showed the image, the offering before Career Coaching (after it as "Lean Coaching" on the jar), "from CHF 160.00". **Remove image**: the removed id answered 404, and the card and detail page showed the placeholder. Archived: the image's buttons are disabled. A stale page (archived in another tab, then **Upload image**) showed "The offering is archived and can't be changed any more." and reloaded read-only. The only API answers >= 400 were the first `GET /api/me` (401) and the intended 400s and 409. The dev database was cleaned up afterwards: Career Coaching as seeded, no images.
