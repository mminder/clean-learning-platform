-- Images (D-21): the bytes and their content type, at most 2 MB. An offering may have one. Expert photos (UC-14) will
-- use the same table. The checks repeat the rules of the domain's Image and ImageType (D-36); the type itself is
-- detected from the file's first bytes, which the database doesn't check.

create table image (
    id           uuid        primary key,
    content_type text        not null check (content_type in ('image/jpeg', 'image/png', 'image/webp')),
    -- 1 byte to 2 MB (2 × 1024 × 1024 bytes)
    data         bytea       not null check (octet_length(data) between 1 and 2097152),
    created_at   timestamptz not null default now(),
    updated_at   timestamptz not null default now()
);

create trigger image_set_updated_at
    before update on image
    for each row execute function set_updated_at();

-- An image belongs to one offering at most, so replacing or removing it may delete it. The unique constraint's index
-- also serves the foreign key check when an image is deleted.
alter table offering
    add column image_id uuid unique references image (id);
