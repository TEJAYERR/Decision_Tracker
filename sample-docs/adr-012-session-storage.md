# ADR-012: Move session storage from Redis to Postgres

Date: 12 April 2026
Author: Priya (Platform team)
Reviewers: Meera (Security team)
Status: Accepted

## Context

On 14 March 2026, a Redis failover caused every active session in the Auth Service
to be lost. Around 40,000 users were logged out at once and had to sign in again.
The outage lasted 25 minutes. Our Session Store, which was running on a single Redis
cluster, had no durable copy of the sessions.

## Decision

We will move session storage from Redis to Postgres. Sessions will be written to
Postgres first, and Redis may be kept only as a read cache later if we need it.

## Consequences

The Auth Service needs a new session table and a cleanup job for expired sessions.
Latency will be slightly higher than Redis, which we accept in exchange for durability.
The Platform team owns the migration and will finish it by the end of May.