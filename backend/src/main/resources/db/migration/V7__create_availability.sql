-- An expert's availability (UC-13, D-09): the selected dates, and one set of daily time ranges for all of them, both
-- in Zurich. It belongs to the user, not to the expert profile: managing it needs EXPERT, not a profile. The check
-- repeats the domain's TimeRange (D-36); that ranges neither overlap nor touch is a rule across rows, which the
-- domain's Availability holds.

create table availability_date (
    expert_id    uuid        not null references app_user (id),
    available_on date        not null,
    created_at   timestamptz not null default now(),
    updated_at   timestamptz not null default now(),
    primary key (expert_id, available_on)
);

create table availability_time_range (
    expert_id  uuid        not null references app_user (id),
    start_time time        not null,
    end_time   time        not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    primary key (expert_id, start_time),
    check (start_time < end_time)
);

create trigger availability_date_set_updated_at
    before update on availability_date
    for each row execute function set_updated_at();

create trigger availability_time_range_set_updated_at
    before update on availability_time_range
    for each row execute function set_updated_at();
