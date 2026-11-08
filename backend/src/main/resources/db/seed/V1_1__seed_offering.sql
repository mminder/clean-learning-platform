-- poc seed: Career Coaching with its two packages. Tests refer to these ids through SeedData.

insert into offering (id, type, name, description, status)
values ('00000000-0000-0000-0000-000000001001', 'CONSULTATION', 'Career Coaching',
        'One-to-one sessions with an experienced coach for your next career step. '
            || 'Together you take stock of your strengths, sharpen your goals and plan concrete actions, '
            || 'whether you are aiming for a new role, a promotion or a change of field. '
            || 'Between sessions you try things out, and the next session builds on what you learned.',
        'PUBLISHED');

insert into consultation_offering (offering_id, session_duration_minutes, cancellation_deadline_hours)
values ('00000000-0000-0000-0000-000000001001', 60, 24);

insert into consultation_package (id, consultation_offering_id, session_count, price)
values ('00000000-0000-0000-0000-000000001011', '00000000-0000-0000-0000-000000001001', 3, 450.00),
       ('00000000-0000-0000-0000-000000001012', '00000000-0000-0000-0000-000000001001', 5, 700.00);
