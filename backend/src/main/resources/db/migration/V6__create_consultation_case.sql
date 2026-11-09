-- Cases (UC-03): what a customer gets by buying a package of a consultation offering. The purchase terms are copied
-- from the offering at the purchase and never change (D-08); their checks repeat the ones of the offering's columns
-- they come from (D-36). Name and image are read live from the offering. No reference to the package: an admin may
-- remove a bought package (UC-16). Status, assigned expert and cancellation come with UC-06 and UC-08.

create table consultation_case (
    id                          uuid          primary key,
    customer_id                 uuid          not null references app_user (id),
    -- Keeps an offering with purchases from being deleted.
    consultation_offering_id    uuid          not null references consultation_offering (offering_id),
    price_paid                  numeric(10,2) not null check (price_paid >= 0),
    session_count               int           not null check (session_count > 0),
    session_duration_minutes    int           not null check (session_duration_minutes > 0),
    cancellation_deadline_hours int           not null check (cancellation_deadline_hours >= 0),
    -- The purchase time, from the application's Clock; created_at stays the database's technical time.
    purchased_at                timestamptz   not null,
    created_at                  timestamptz   not null default now(),
    updated_at                  timestamptz   not null default now()
);

create index consultation_case_customer_id_idx on consultation_case (customer_id);

create index consultation_case_consultation_offering_id_idx on consultation_case (consultation_offering_id);

create trigger consultation_case_set_updated_at
    before update on consultation_case
    for each row execute function set_updated_at();
