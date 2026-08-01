# Production hardening, round 2

This patch is designed to be applied after `it-hardening-ui.patch`.

## Required environment variables

Production now requires `JWT_ISSUER`, `JWT_AUDIENCE`, `STRIPE_SECRET_KEY`,
`STRIPE_PUBLISHABLE_KEY`, and `STRIPE_WEBHOOK_SECRET` in addition to the database and
Redis variables. Configure either `JWT_JWK_SET_URI` (recommended for asymmetric keys) or a
`JWT_SECRET` containing at least 32 UTF-8 bytes.

## Database migration

`V2__payment_and_reservation_invariants.sql`:

- snapshots the agreed reservation price and currency;
- derives and persists reservation end times;
- prevents overlapping active reservations with a PostgreSQL exclusion constraint;
- keeps one payment row per reservation;
- adds optimistic-lock versions and atomic webhook event state.

The migration deliberately fails while adding the unique payment constraint if historical
reservations contain duplicate payment rows. Inspect and reconcile those rows manually before
retrying; financial records are never deleted automatically. A useful preflight query is:

```sql
SELECT reservation_id, COUNT(*)
FROM payments
GROUP BY reservation_id
HAVING COUNT(*) > 1;
```

The migration also requires permission to install the `btree_gist` extension.

`baseline-on-migrate` has deliberately been removed from production configuration. For an
existing database that was created before Flyway, perform a controlled one-time baseline
instead of enabling automatic baselining permanently.

## Stripe

The server uses Stripe's official Java SDK, bounded network timeouts, automatic network
retries, and an idempotency key derived from the reservation. The browser completes payment
through `/checkout.html`; webhook events remain the source of truth for the final status.
Webhook claims are committed independently, failed events can be retried, and abandoned
`PROCESSING` claims can be reclaimed after a 15-minute lease. Register every production domain
in Stripe before enabling Link, Apple Pay, Google Pay, or any other method that requires it.

## Observability

Every HTTP response now carries an `X-Request-ID`. The same value is added to the logging MDC
and to API `ProblemDetail` responses, making frontend errors traceable in server logs.

## Remaining structural work

Moving `it-main` to the repository root, extracting the inline frontend into a compiled
application, and changing list APIs to cursor/page-based responses should be separate PRs.
They are intentionally excluded to avoid a breaking and difficult-to-review patch.
