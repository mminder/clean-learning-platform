-- updated_at is the time of the last change (D-20). The database sets it, like created_at's default: a before update
-- trigger on every table sets it to the transaction's now(). A table created later adds the trigger in its create
-- migration (_docs/architecture/backend.md, persistence).

create function set_updated_at() returns trigger
    language plpgsql
as
$$
begin
    new.updated_at = now();
    return new;
end;
$$;

create trigger offering_set_updated_at
    before update on offering
    for each row execute function set_updated_at();

create trigger consultation_offering_set_updated_at
    before update on consultation_offering
    for each row execute function set_updated_at();

create trigger consultation_package_set_updated_at
    before update on consultation_package
    for each row execute function set_updated_at();

create trigger app_user_set_updated_at
    before update on app_user
    for each row execute function set_updated_at();

create trigger user_role_set_updated_at
    before update on user_role
    for each row execute function set_updated_at();

create trigger expert_profile_set_updated_at
    before update on expert_profile
    for each row execute function set_updated_at();

create trigger eligible_expert_set_updated_at
    before update on eligible_expert
    for each row execute function set_updated_at();
