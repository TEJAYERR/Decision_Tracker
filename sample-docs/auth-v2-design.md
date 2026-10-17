# Auth v2 design

Date: 20 April 2026
Author: Meera (Security team)
Reviewer: Priya (Platform team)
Status: Accepted

## Why we are changing this

In October 2025, ADR-004 decided that services would authenticate to the Auth Service
using static service tokens that never expire. That was quick to build, but a leaked
token stays valid forever. After the 14 March session loss, Security reviewed the
Auth Service end to end and flagged the static tokens as the biggest remaining risk.

## Decision

Auth v2 replaces the static service tokens from ADR-004 with short-lived tokens that
expire after 15 minutes and are renewed automatically. ADR-004 is superseded and
should no longer be followed for new work.

## Impact

The Auth Service must issue and renew the new tokens. The Billing Service uses
service tokens for invoice sync, so the Billing team must update its token handling
before the old tokens are switched off on 30 June 2026. The Platform team owns the
Auth Service and will build the token renewal.