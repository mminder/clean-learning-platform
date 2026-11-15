-- Notes (UC-15): what a case's assigned expert writes on the case, private or public. The checks repeat the domain's
-- NoteTitle, NoteText and NoteVisibility (D-36): the title and the text are never blank, with V3's check, under the
-- ICU collation, without which Postgres's \s misses the no-break spaces that the domain treats as blank. Both
-- visibilities are stored. written_at and edited_at come from the application's Clock, as the purchase time does;
-- created_at and updated_at stay the database's technical times (spec 06, decision 5). edited_at is null until the
-- first edit. No version column: the last save wins (spec 06, decision 7).

create table note (
    id                   uuid        primary key,
    consultation_case_id uuid        not null references consultation_case (id),
    title                text        not null collate "und-x-icu" check (title ~ '\S'),
    text                 text        not null collate "und-x-icu" check (text ~ '\S'),
    visibility           text        not null check (visibility in ('PRIVATE', 'PUBLIC')),
    written_at           timestamptz not null,
    edited_at            timestamptz,
    created_at           timestamptz not null default now(),
    updated_at           timestamptz not null default now()
);

create index note_consultation_case_id_idx on note (consultation_case_id);

create trigger note_set_updated_at
    before update on note
    for each row execute function set_updated_at();
