# Northwind Pay: teams and ownership

Last updated: 1 April 2026
Maintained by: Meera (Security team)

## Teams

**Platform team**
Lead: Priya. Members: Priya.
Owns the Auth Service and the Session Store.

**Billing team**
Lead: Arjun. Members: Arjun.
Owns the Billing Service.

**Security team**
Lead: Meera. Members: Meera.
Does not own a production service. Security reviews the design of the Auth Service
and the Session Store, and approves changes to how users sign in.

## How the systems depend on each other

- The Auth Service depends on the Session Store to keep users signed in.
- The Billing Service depends on the Auth Service for service tokens.

## Who to ask

For anything about sign-in or sessions, start with the Platform team.
For invoices and payments, start with the Billing team.
For questions about access rules or token safety, ask the Security team.