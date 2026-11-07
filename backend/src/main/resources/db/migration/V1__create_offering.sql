-- Offerings, with consultation offerings and their packages. Trainings add their own subtype table later.

create table offering (
    id          uuid        primary key,
    type        text        not null check (type in ('CONSULTATION')),
    name        text        not null,
    description text        not null,
    status      text        not null check (status in ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    created_at  timestamptz not null default now(),
    updated_at  timestamptz not null default now()
);

create table consultation_offering (
    offering_id                 uuid        primary key references offering (id),
    session_duration_minutes    int         not null check (session_duration_minutes > 0),
    cancellation_deadline_hours int         not null check (cancellation_deadline_hours >= 0),
    created_at                  timestamptz not null default now(),
    updated_at                  timestamptz not null default now()
);

create table consultation_package (
    id                       uuid          primary key,
    consultation_offering_id uuid          not null references consultation_offering (offering_id),
    session_count            int           not null check (session_count > 0),
    price                    numeric(10,2) not null check (price >= 0),
    created_at               timestamptz   not null default now(),
    updated_at               timestamptz   not null default now()
);

create index consultation_package_consultation_offering_id_idx on consultation_package (consultation_offering_id);
