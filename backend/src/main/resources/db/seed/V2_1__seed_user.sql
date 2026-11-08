-- poc seed: two users per role (UC-01), the profiles of both experts, both eligible for Career Coaching.
-- Tests refer to these ids through SeedData.

insert into app_user (id, name, email)
values ('00000000-0000-0000-0000-000000000101', 'Customer 1', 'customer1@example.com'),
       ('00000000-0000-0000-0000-000000000102', 'Customer 2', 'customer2@example.com'),
       ('00000000-0000-0000-0000-000000000201', 'Expert 1', 'expert1@example.com'),
       ('00000000-0000-0000-0000-000000000202', 'Expert 2', 'expert2@example.com'),
       ('00000000-0000-0000-0000-000000000301', 'Admin 1', 'admin1@example.com'),
       ('00000000-0000-0000-0000-000000000302', 'Admin 2', 'admin2@example.com');

-- Expert 1 is also a customer, to try self-booking (D-06). Expert 2 is also an admin.
insert into user_role (user_id, role)
values ('00000000-0000-0000-0000-000000000101', 'CUSTOMER'),
       ('00000000-0000-0000-0000-000000000102', 'CUSTOMER'),
       ('00000000-0000-0000-0000-000000000201', 'EXPERT'),
       ('00000000-0000-0000-0000-000000000201', 'CUSTOMER'),
       ('00000000-0000-0000-0000-000000000202', 'EXPERT'),
       ('00000000-0000-0000-0000-000000000202', 'ADMIN'),
       ('00000000-0000-0000-0000-000000000301', 'ADMIN'),
       ('00000000-0000-0000-0000-000000000302', 'ADMIN');

insert into expert_profile (user_id, display_name, bio)
values ('00000000-0000-0000-0000-000000000201', 'Expert 1',
        'Career coach with ten years in tech recruiting. Helps engineers find their next role and prepare for '
            || 'interviews.'),
       ('00000000-0000-0000-0000-000000000202', 'Expert 2',
        'Leadership coach and former engineering manager. Works with new team leads on their first year in the '
            || 'role.');

insert into eligible_expert (consultation_offering_id, expert_id)
values ('00000000-0000-0000-0000-000000001001', '00000000-0000-0000-0000-000000000201'),
       ('00000000-0000-0000-0000-000000001001', '00000000-0000-0000-0000-000000000202');
