-- A draft with a case of Customer 1, which the API can't make: a draft isn't for sale, and unpublishing needs no
-- purchases. The purchase guard's safety net on delete is tested with it. The shared delete-test-data.sql removes it.

insert into offering (id, type, name, description, status)
values ('00000000-0000-0000-0000-000000009006', 'CONSULTATION', 'Draft Coaching', 'Bought, then a draft.', 'DRAFT');

insert into consultation_offering (offering_id, session_duration_minutes, cancellation_deadline_hours)
values ('00000000-0000-0000-0000-000000009006', 60, 24);

insert into consultation_package (id, consultation_offering_id, session_count, price)
values ('00000000-0000-0000-0000-000000009061', '00000000-0000-0000-0000-000000009006', 1, 100.00);

insert into consultation_case (id, customer_id, consultation_offering_id, price_paid, session_count,
                               session_duration_minutes, cancellation_deadline_hours, purchased_at)
values ('00000000-0000-0000-0000-000000009601', '00000000-0000-0000-0000-000000000101',
        '00000000-0000-0000-0000-000000009006', 100.00, 1, 60, 24, '2026-11-09T14:00:00Z');
