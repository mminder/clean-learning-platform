-- Deletes every offering that isn't in the poc seed, with its packages and eligible experts, so a test's offerings
-- don't leak into other tests. Writing tests run it after each test method (SeedData.DELETE_TEST_OFFERINGS). The seed
-- stays: tests never change seed rows.

delete from eligible_expert where consultation_offering_id not in ('00000000-0000-0000-0000-000000001001');

delete from consultation_package where consultation_offering_id not in ('00000000-0000-0000-0000-000000001001');

delete from consultation_offering where offering_id not in ('00000000-0000-0000-0000-000000001001');

delete from offering where id not in ('00000000-0000-0000-0000-000000001001');
