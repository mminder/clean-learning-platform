-- Cancelling an appointment (UC-07): the status gains CANCELLED, and credit_refunded says whether the cancellation
-- returned the credit, in time, or forfeited it, late (glossary). Only what exists is stored (spec 05, decision 11):
-- credit_refunded is set exactly when the appointment is cancelled, and the check says so (D-36). No cancellation time:
-- nothing shows one. CONSUMED stays derived, never stored (D-11).

-- V8's inline check on status, which Postgres named appointment_status_check, is replaced, not added to.
alter table appointment
    drop constraint appointment_status_check;

alter table appointment
    add constraint appointment_status_check check (status in ('BOOKED', 'CANCELLED'));

alter table appointment
    add column credit_refunded boolean;

alter table appointment
    add constraint appointment_credit_refunded_check check ((status = 'CANCELLED') = (credit_refunded is not null));
