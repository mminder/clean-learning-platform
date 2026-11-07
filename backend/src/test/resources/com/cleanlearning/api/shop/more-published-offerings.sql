-- Two more published consultation offerings around the seeded Career Coaching. Sorted alphabetically, regardless of
-- case and umlauts, they come out as Agile Coaching, Ärger-Coaching, Career Coaching; a byte-wise sort would put
-- Ärger-Coaching last. delete-extra-offerings.sql removes them.

insert into offering (id, type, name, description, status)
values ('00000000-0000-0000-0000-000000009003', 'CONSULTATION', 'Agile Coaching', 'Coaching for agile teams.',
        'PUBLISHED'),
       ('00000000-0000-0000-0000-000000009004', 'CONSULTATION', 'Ärger-Coaching', 'Dealing with anger at work.',
        'PUBLISHED');

insert into consultation_offering (offering_id, session_duration_minutes, cancellation_deadline_hours)
values ('00000000-0000-0000-0000-000000009003', 90, 48),
       ('00000000-0000-0000-0000-000000009004', 60, 24);

insert into consultation_package (id, consultation_offering_id, session_count, price)
values ('00000000-0000-0000-0000-000000009031', '00000000-0000-0000-0000-000000009003', 1, 250.00),
       ('00000000-0000-0000-0000-000000009032', '00000000-0000-0000-0000-000000009003', 4, 900.00),
       ('00000000-0000-0000-0000-000000009041', '00000000-0000-0000-0000-000000009004', 2, 300.00);
