-- A PUBLISHED consultation offering whose last package was removed (UC-16 allows that). It has an eligible expert, so
-- only the missing package keeps it out of the shop. The shared delete-test-data.sql removes it.

insert into offering (id, type, name, description, status)
values ('00000000-0000-0000-0000-000000009005', 'CONSULTATION', 'Empty Coaching', 'No packages left.', 'PUBLISHED');

insert into consultation_offering (offering_id, session_duration_minutes, cancellation_deadline_hours)
values ('00000000-0000-0000-0000-000000009005', 60, 24);

insert into eligible_expert (consultation_offering_id, expert_id)
values ('00000000-0000-0000-0000-000000009005', '00000000-0000-0000-0000-000000000201');
