-- Removes the users of users-that-are-not-offered-as-experts.sql. Tests that choose them as eligible experts also
-- run the shared delete-test-data.sql, which comes first, so no foreign key points at them.

delete from expert_profile where user_id in ('00000000-0000-0000-0000-000000009102');

delete from user_role where user_id in ('00000000-0000-0000-0000-000000009101', '00000000-0000-0000-0000-000000009102');

delete from app_user where id in ('00000000-0000-0000-0000-000000009101', '00000000-0000-0000-0000-000000009102');
