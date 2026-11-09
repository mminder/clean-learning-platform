-- Two users the expert list must not offer: one with EXPERT but no expert profile (UC-14 comes later), and one with a
-- profile but without EXPERT (UC-19, MVP, lets an expert lose the role). 'Aaa' sorts before Expert 1, so a wrong
-- list would show.

insert into app_user (id, name, email)
values ('00000000-0000-0000-0000-000000009101', 'Aaa Without Profile', 'without-profile@example.com'),
       ('00000000-0000-0000-0000-000000009102', 'Aaa Without Role', 'without-role@example.com');

insert into user_role (user_id, role)
values ('00000000-0000-0000-0000-000000009101', 'EXPERT'),
       ('00000000-0000-0000-0000-000000009102', 'CUSTOMER');

insert into expert_profile (user_id, display_name, bio)
values ('00000000-0000-0000-0000-000000009102', 'Aaa Without Role', 'Lost the role.');
