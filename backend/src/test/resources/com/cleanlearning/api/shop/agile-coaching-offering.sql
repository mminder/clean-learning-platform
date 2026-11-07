-- A second published consultation offering, whose name sorts before the seeded Career Coaching.
-- delete-extra-offerings.sql removes it.

insert into offering (id, type, name, description, status)
values ('00000000-0000-0000-0000-000000009003', 'CONSULTATION', 'Agile Coaching', 'Coaching for agile teams.',
        'PUBLISHED');

insert into consultation_offering (offering_id, session_duration_minutes, cancellation_deadline_hours)
values ('00000000-0000-0000-0000-000000009003', 90, 48);

insert into consultation_package (id, consultation_offering_id, session_count, price)
values ('00000000-0000-0000-0000-000000009031', '00000000-0000-0000-0000-000000009003', 1, 250.00),
       ('00000000-0000-0000-0000-000000009032', '00000000-0000-0000-0000-000000009003', 4, 900.00);
