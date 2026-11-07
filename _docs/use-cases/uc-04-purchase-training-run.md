# UC-04 Purchase training run

| | |
|---|---|
| Actor | customer |
| Scope | PoC without payment. MVP adds payment. |
| Related | [UC-02](uc-02-browse-shop.md), [UC-05](uc-05-view-my-purchases.md), [UC-09](uc-09-cancel-training-enrollment.md) |

## Goal

The customer buys a seat in a specific run and is enrolled in it.

## Preconditions

- The training offering is `PUBLISHED`.
- The user has `CUSTOMER`. A visitor is sent to [UC-01](uc-01-login.md) first and then returns.

## Main flow

1. The customer clicks **Buy** on the training card or the detail page.
2. The system shows a dialog listing the buyable runs (dates, seats left).
3. The customer selects a run.
4. The system asks: "Do you really want to buy *Software Architecture, 4–5 March 2027* for CHF 1,800?"
5. The customer confirms.
6. The system locks the run, re-checks the business rules, and creates an enrollment:
   - status `ENROLLED`;
   - purchase terms: price paid, cancellation deadline;
   - the purchase time.
7. The system opens the enrollment detail ([UC-05](uc-05-view-my-purchases.md)).

## Alternative and error flows

- **The customer cancels the dialog:** nothing is created.
- **The last seat was taken in the meantime:** 409, "This run is sold out."
- **The run has started in the meantime:** 409, "This run has already started."
- **The customer is already enrolled in this run:** 409, "You are already enrolled."

## Business rules

A run is buyable if and only if:

```
offering PUBLISHED
∧ now < start of the run's first time slot
∧ active enrollments < seats
∧ customer has no active enrollment in this run
```

- Enrolling in a different run of the same training is allowed.
- An assigned expert isn't required.

## Visibility and authorization

- The enrollment belongs to the purchasing customer. The run's expert and admins see it in the participant list.

## Out of scope

- Payment (MVP), a waitlist, buying several seats at once.
