-- Deletes every offering that isn't in the poc seed, with its packages, eligible experts and image, so a test's
-- offerings don't leak into other tests. Writing tests run it after each test method (SeedData.DELETE_TEST_OFFERINGS).
-- The seed stays: tests never change seed rows.

delete from eligible_expert where consultation_offering_id not in ('00000000-0000-0000-0000-000000001001');

delete from consultation_package where consultation_offering_id not in ('00000000-0000-0000-0000-000000001001');

delete from consultation_offering where offering_id not in ('00000000-0000-0000-0000-000000001001');

delete from offering where id not in ('00000000-0000-0000-0000-000000001001');

-- The images of the deleted offerings: no offering refers to them any more. The seed has none. Once expert photos
-- exist (UC-14), the ones that an expert profile refers to stay, too.
delete from image i where not exists (select 1 from offering o where o.image_id = i.id);
