-- Deletes every note, every appointment, every case and every availability, then every offering that isn't in the
-- poc seed, with its packages and eligible experts, sets the seeded expert profiles back to the seed, and deletes every
-- image that nothing refers to any more, so a test's data doesn't leak into other tests. Tests that buy, book, write
-- notes, save availability, write offerings or save an expert profile or its photo run it after each test method
-- (SeedData.DELETE_TEST_DATA). The seed stays: it has no cases, no notes, no appointments and no availability
-- (spec 04, decision 8). The expert profiles are the only seed rows tests change (spec 06, decision 16).

-- Notes and appointments first: they refer to the cases.
delete from note;

delete from appointment;

-- Then the cases: they refer to the offerings and the users, and their assigned experts to the expert profiles.
delete from consultation_case;

-- Every expert's availability: the seed has none.
delete from availability_date;

delete from availability_time_range;

delete from eligible_expert where consultation_offering_id not in ('00000000-0000-0000-0000-000000001001');

delete from consultation_package where consultation_offering_id not in ('00000000-0000-0000-0000-000000001001');

delete from consultation_offering where offering_id not in ('00000000-0000-0000-0000-000000001001');

delete from offering where id not in ('00000000-0000-0000-0000-000000001001');

-- The seeded profiles as V2_1__seed_user.sql has them, without a Teams link and without a photo.
update expert_profile
set display_name = 'Expert 1',
    bio          = 'Career coach with ten years in tech recruiting. Helps engineers find their next role and prepare '
                       || 'for interviews.',
    teams_link   = null,
    photo_id     = null
where user_id = '00000000-0000-0000-0000-000000000201';

update expert_profile
set display_name = 'Expert 2',
    bio          = 'Leadership coach and former engineering manager. Works with new team leads on their first year '
                       || 'in the role.',
    teams_link   = null,
    photo_id     = null
where user_id = '00000000-0000-0000-0000-000000000202';

-- The images of the deleted offerings and of the profiles' photos: neither an offering nor a profile refers to them any
-- more. The seed has none.
delete from image i
where not exists (select 1 from offering o where o.image_id = i.id)
  and not exists (select 1 from expert_profile p where p.photo_id = i.id);
