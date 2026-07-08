# Business Model: Other residential care activities

## Classification

- Repository: `cloud-itonami-isic-8790`
- ISIC Rev.5: `8790`
- Activity: other residential care activities not elsewhere classified (e.g. children's homes, orphanages, residential care for the homeless)
- Social impact: care quality, data sovereignty, transparent audit

## Customer

- independent residential-care operators
- cooperative shelter/group-home networks
- community youth/homeless-services providers

## Offer

- resident intake
- care-plan proposal
- incident-response proposal
- immutable audit ledger

## Revenue

- self-host setup: one-time implementation fee
- managed hosting: monthly subscription per facility
- support: monthly retainer with SLA
- migration: import from an incumbent residential-care system
- per-resident-month fee

## Trust Controls

- no care plan or incident response is finalized without human sign-off
- a fabricated assessment forces a hold, not an override
- every care path is auditable
- resident data stays outside Git
- emergency manual override paths remain outside LLM control
- an unresolved mandatory-reporting obligation, or an uncleared
  background check, forces a hold, not an override
- care-plan and incident-response finalization are each logged and
  escalated, and cannot be finalized twice for the same resident: a
  double-finalization attempt is held off this actor's own resident
  facts alone, with no upstream comparison needed

## Residential Care Governor: decision rule

`blueprint.edn` fixes `:itonami.blueprint/governor` to `:residential-
care-governor` -- this is not a generic "review step," it is the
gate the TWO real-world acts this business performs (finalizing a
real care plan, finalizing a real incident response) must pass. The
governor sits between the ResidentialOps-LLM and execution, per the
README's Core Contract:

```text
ResidentialOps-LLM -> Residential Care Governor -> hold, proceed, or human approval
```

**Approves**: routine residential-care actions proposed against a
resident that already has a consented care plan on file, satisfied
required evidence, a resolved mandatory-reporting-obligation status,
and a cleared background check. These proceed straight to the
resident ledger.

**Rejects or escalates**: the governor refuses to let the advisor
finalize a care plan or incident response on its own authority when
any of the following hold -- a fabricated jurisdiction spec-basis;
incomplete evidence; an unresolved mandatory-reporting obligation; an
uncleared background check; a double-finalization attempt. A clean
finalization proposal still always routes to a human -- neither
`:actuation/finalize-care-plan` nor `:actuation/finalize-incident-
response` is ever auto-committed, at any rollout phase.
