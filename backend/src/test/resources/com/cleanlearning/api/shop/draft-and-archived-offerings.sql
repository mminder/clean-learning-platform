-- A DRAFT and an ARCHIVED consultation offering besides the seed. delete-extra-offerings.sql removes them.

insert into offering (id, type, name, description, status)
values ('00000000-0000-0000-0000-000000009001', 'CONSULTATION', 'Draft Coaching', 'Not published yet.', 'DRAFT'),
       ('00000000-0000-0000-0000-000000009002', 'CONSULTATION', 'Archived Coaching', 'No longer sold.', 'ARCHIVED');

insert into consultation_offering (offering_id, session_duration_minutes, cancellation_deadline_hours)
values ('00000000-0000-0000-0000-000000009001', 60, 24),
       ('00000000-0000-0000-0000-000000009002', 60, 24);

insert into consultation_package (id, consultation_offering_id, session_count, price)
values ('00000000-0000-0000-0000-000000009011', '00000000-0000-0000-0000-000000009001', 1, 100.00),
       ('00000000-0000-0000-0000-000000009021', '00000000-0000-0000-0000-000000009002', 1, 100.00);
