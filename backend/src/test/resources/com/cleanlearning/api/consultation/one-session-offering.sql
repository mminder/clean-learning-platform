-- A published consultation offering with one package of 1 session and both seeded experts eligible: a case bought
-- from it has one credit and no expert, so either expert can take its first booking. The booking races use it. The
-- shared delete-test-data.sql removes it.

insert into offering (id, type, name, description, status)
values ('00000000-0000-0000-0000-000000009007', 'CONSULTATION', 'Mock Interview', 'One practice interview.',
        'PUBLISHED');

insert into consultation_offering (offering_id, session_duration_minutes, cancellation_deadline_hours)
values ('00000000-0000-0000-0000-000000009007', 60, 24);

insert into consultation_package (id, consultation_offering_id, session_count, price)
values ('00000000-0000-0000-0000-000000009071', '00000000-0000-0000-0000-000000009007', 1, 150.00);

insert into eligible_expert (consultation_offering_id, expert_id)
values ('00000000-0000-0000-0000-000000009007', '00000000-0000-0000-0000-000000000201'),
       ('00000000-0000-0000-0000-000000009007', '00000000-0000-0000-0000-000000000202');
