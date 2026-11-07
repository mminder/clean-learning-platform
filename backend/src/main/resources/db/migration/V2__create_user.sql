-- Users with their roles, expert profiles, and the experts eligible for a consultation offering.

-- "user" is reserved in PostgreSQL.
create table app_user (
    id         uuid        primary key,
    name       text        not null,
    email      text        not null unique,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table user_role (
    user_id    uuid        not null references app_user (id),
    role       text        not null check (role in ('CUSTOMER', 'EXPERT', 'ADMIN')),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    primary key (user_id, role)
);

create table expert_profile (
    user_id      uuid        primary key references app_user (id),
    -- The shop sorts experts by display name: alphabetically, regardless of case and umlauts (ICU root collation).
    display_name text        not null collate "und-x-icu",
    bio          text        not null,
    created_at   timestamptz not null default now(),
    updated_at   timestamptz not null default now()
);

create table eligible_expert (
    consultation_offering_id uuid        not null references consultation_offering (offering_id),
    expert_id                uuid        not null references expert_profile (user_id),
    created_at               timestamptz not null default now(),
    updated_at               timestamptz not null default now(),
    primary key (consultation_offering_id, expert_id)
);

create index eligible_expert_expert_id_idx on eligible_expert (expert_id);
