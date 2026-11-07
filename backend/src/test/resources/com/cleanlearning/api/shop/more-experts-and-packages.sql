-- Two more eligible experts and a third package for the seeded Career Coaching. Sorted alphabetically by display
-- name, regardless of case and umlauts, the experts come out as Älma Muster, anna Beispiel, Expert 1, Expert 2; a
-- byte-wise sort or a sort by id would put them last. The 1-session package comes first, although its id is the
-- largest. delete-more-experts-and-packages.sql removes them.

insert into app_user (id, name, email)
values ('00000000-0000-0000-0000-000000009201', 'Älma Muster', 'alma@example.com'),
       ('00000000-0000-0000-0000-000000009202', 'anna Beispiel', 'anna@example.com');

insert into user_role (user_id, role)
values ('00000000-0000-0000-0000-000000009201', 'EXPERT'),
       ('00000000-0000-0000-0000-000000009202', 'EXPERT');

insert into expert_profile (user_id, display_name, bio)
values ('00000000-0000-0000-0000-000000009201', 'Älma Muster', 'Coach.'),
       ('00000000-0000-0000-0000-000000009202', 'anna Beispiel', 'Coach.');

insert into eligible_expert (consultation_offering_id, expert_id)
values ('00000000-0000-0000-0000-000000001001', '00000000-0000-0000-0000-000000009201'),
       ('00000000-0000-0000-0000-000000001001', '00000000-0000-0000-0000-000000009202');

insert into consultation_package (id, consultation_offering_id, session_count, price)
values ('00000000-0000-0000-0000-000000009051', '00000000-0000-0000-0000-000000001001', 1, 160.00);
