-- Removes what more-experts-and-packages.sql adds, so it doesn't leak into other tests. The seed stays.

delete from consultation_package where id = '00000000-0000-0000-0000-000000009051';

delete from eligible_expert where expert_id in (
    '00000000-0000-0000-0000-000000009201',
    '00000000-0000-0000-0000-000000009202');

delete from expert_profile where user_id in (
    '00000000-0000-0000-0000-000000009201',
    '00000000-0000-0000-0000-000000009202');

delete from user_role where user_id in (
    '00000000-0000-0000-0000-000000009201',
    '00000000-0000-0000-0000-000000009202');

delete from app_user where id in (
    '00000000-0000-0000-0000-000000009201',
    '00000000-0000-0000-0000-000000009202');
