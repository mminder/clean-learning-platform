-- Removes the offerings that tests add, so they don't leak into other tests. The seed stays.

delete from consultation_package where consultation_offering_id in (
    '00000000-0000-0000-0000-000000009001',
    '00000000-0000-0000-0000-000000009002',
    '00000000-0000-0000-0000-000000009003');

delete from consultation_offering where offering_id in (
    '00000000-0000-0000-0000-000000009001',
    '00000000-0000-0000-0000-000000009002',
    '00000000-0000-0000-0000-000000009003');

delete from offering where id in (
    '00000000-0000-0000-0000-000000009001',
    '00000000-0000-0000-0000-000000009002',
    '00000000-0000-0000-0000-000000009003');
