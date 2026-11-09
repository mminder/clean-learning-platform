-- Appointments (UC-06): one booked session of a case with the case's assigned expert. The first booking sets the
-- assigned expert, which then never changes and stays after the expert stops being eligible (UC-06). It refers to the
-- expert profile, as eligible_expert does.
--
-- The appointment has no expert column: its expert is its case's assigned expert, so busy time joins the case.
-- starts_at and ends_at, because end is reserved in SQL. The check repeats the domain's Appointment (D-36). The
-- status holds only BOOKED until UC-07 adds CANCELLED (spec 04, decision 15); every query of appointments filters by
-- it from the start. CONSUMED is derived from the time, never stored (D-11).

alter table consultation_case
    add column assigned_expert_id uuid references expert_profile (user_id);

create index consultation_case_assigned_expert_id_idx on consultation_case (assigned_expert_id);

create table appointment (
    id                   uuid        primary key,
    consultation_case_id uuid        not null references consultation_case (id),
    starts_at            timestamptz not null,
    ends_at              timestamptz not null,
    status               text        not null check (status in ('BOOKED')),
    created_at           timestamptz not null default now(),
    updated_at           timestamptz not null default now(),
    check (starts_at < ends_at)
);

create index appointment_consultation_case_id_idx on appointment (consultation_case_id);

create trigger appointment_set_updated_at
    before update on appointment
    for each row execute function set_updated_at();
