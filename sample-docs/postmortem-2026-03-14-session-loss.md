# Postmortem: Auth session loss, 14 March 2026

Date written: 18 March 2026
Author: Priya (Platform team, on call during the incident)
Contributors: Meera (Security team), Arjun (Billing team)

## Summary

At 09:12 UTC on 14 March 2026, the Redis cluster behind the Session Store failed over
to a replica that had not received the latest writes. Every active session in the
Auth Service was lost. About 40,000 users were signed out and the Auth Service was
degraded for 25 minutes.

## Impact

Users had to sign in again, which caused a spike of login traffic. The Billing Service
depends on the Auth Service for service tokens, so its invoice sync job failed
during the outage. Arjun reported that 212 invoices were not sent and had to be
re-run by hand the next morning.

Meera checked the logs and found no sign that any session was hijacked or exposed.

## Root cause

The Session Store ran on a single Redis cluster with no durable copy of the sessions.
When the failover happened, the sessions only existed in memory on the old primary.

## Action items

1. Platform will propose durable storage for sessions. Owner: Priya.
2. Billing will add retries to the invoice sync so one failed call does not lose
   an invoice. Owner: Arjun.
3. Security will review session expiry rules once the new storage is chosen. Owner: Meera.