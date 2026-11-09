-- Deletes every case, every availability, then every offering that isn't in the poc seed, with its packages, eligible
-- experts and image, so a test's data doesn't leak into other tests. Tests that buy, save availability or write
-- offerings run it after each test method (SeedData.DELETE_TEST_DATA). The seed stays: it has no cases and no
-- availability (spec 04, decision 8), and tests never change seed rows.

-- Cases first: they refer to the offerings and the users.
delete from consultation_case;

-- Every expert's availability: the seed has none.
delete from availability_date;

delete from availability_time_range;

delete from eligible_expert where consultation_offering_id not in ('00000000-0000-0000-0000-000000001001');

delete from consultation_package where consultation_offering_id not in ('00000000-0000-0000-0000-000000001001');

delete from consultation_offering where offering_id not in ('00000000-0000-0000-0000-000000001001');

delete from offering where id not in ('00000000-0000-0000-0000-000000001001');

-- The images of the deleted offerings: no offering refers to them any more. The seed has none. Once expert photos
-- exist (UC-14), the ones that an expert profile refers to stay, too.
delete from image i where not exists (select 1 from offering o where o.image_id = i.id);
