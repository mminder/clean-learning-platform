-- Cancelling a case (UC-08): the case gets a stored status, ACTIVE or CANCELLED (D-12), and when it was cancelled and
-- whether it's refund-eligible. Only what exists is stored (spec 05, decision 11): cancelled_at and refund_eligible are
-- set exactly when the case is cancelled, and the checks say so (D-36). COMPLETED stays derived, never stored (D-11).

-- The default only fills the existing rows; the purchase writes ACTIVE.
alter table consultation_case
    add column status text not null default 'ACTIVE' check (status in ('ACTIVE', 'CANCELLED'));

alter table consultation_case
    alter column status drop default;

-- From the application's Clock.
alter table consultation_case
    add column cancelled_at timestamptz;

alter table consultation_case
    add column refund_eligible boolean;

alter table consultation_case
    add constraint consultation_case_cancelled_at_check
        check ((status = 'CANCELLED') = (cancelled_at is not null));

alter table consultation_case
    add constraint consultation_case_refund_eligible_check
        check ((status = 'CANCELLED') = (refund_eligible is not null));
