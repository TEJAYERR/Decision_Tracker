# Billing team sync: 25 March 2026

Attendees: Arjun (Billing team), Priya (Platform team)
Notes by: Arjun

## Background

Following the 14 March auth session loss, the Billing Service invoice sync failed
and 212 invoices were not sent. The Billing Service depends on the Auth Service for
service tokens, so any Auth Service outage stops invoice sync. The Billing team
owns the Billing Service.

## Discussion

Priya explained that the Platform team is moving session storage to a more durable
setup, but she could not promise that the Auth Service will never be unavailable.
Arjun said Billing cannot rely on that alone and needs invoice sync to survive short
Auth Service outages.

## Decision

Arjun decided that the invoice sync job will retry a failed invoice up to 3 times,
with a 5 minute wait between attempts, before marking it as failed and alerting the
Billing team. This is the retry item from the postmortem action list.

## Follow-ups

Arjun will implement the retry logic in the Billing Service by 10 April.
Priya will tell Billing when the new session storage goes live.