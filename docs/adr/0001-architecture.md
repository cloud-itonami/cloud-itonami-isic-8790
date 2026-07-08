# ADR-0001: ResidentialOps-LLM ⊣ Residential Care Governor architecture

## Status

Accepted. `cloud-itonami-isic-8790` promoted from `:blueprint` to
`:implemented` in the `kotoba-lang/industry` registry.

## Context

`cloud-itonami-isic-8790` publishes an OSS business blueprint for
other residential care activities not elsewhere classified: children's
homes, orphanages, residential care for the homeless and similar
vulnerable-population residential settings, run by a qualified,
licensed operator so a community or independent provider never
surrenders resident data and ledgers to a closed SaaS. Like every
prior actor in this fleet, the blueprint alone is not an
implementation: this ADR records the governed-actor architecture that
promotes it to real, tested code, following the same langgraph
StateGraph + independent Governor + Phase 0→3 rollout pattern
established by `cloud-itonami-isic-6511` (life insurance) and applied
across sixty-six prior siblings, most recently `cloud-itonami-isic-
7010` (activities of head offices).

## Decision

### Decision 1: entity and op shape

The primary entity is a `resident`, matching the blueprint's own
Offer language ("resident intake", "care-plan proposal", "incident-
response proposal"). Six ops: `:resident/intake` (directory upsert,
no capital risk), `:careplan/verify` (per-jurisdiction residential-
care/safeguarding evidence checklist, never auto), `:safeguarding/
screen` (mandatory-reporting-obligation screening, unconditional-
evaluation discipline, never auto), `:background-check/screen`
(background-check screening, unconditional-evaluation discipline,
never auto), `:actuation/finalize-care-plan` (POSITIVE, high-stakes
-- finalizing a real care plan), and `:actuation/finalize-incident-
response` (POSITIVE, high-stakes -- finalizing a real incident
response).

### Decision 2: dual-actuation shape on one entity

This blueprint's own README/business-model.md/operator-guide.md
consistently name BOTH acts together ("finalizing a care plan or
incident response"), and the business-model.md's own Offer lists
"care-plan proposal" and "incident-response proposal" as two SEPARATE
items -- distinct enough, and each independently significant enough
(a proactive planning act vs. a reactive incident-handling act), to
warrant two distinct actuations rather than folding them into one
conceptual act (unlike `sports`/8541's genuinely ambiguous either/or
phrasing). Matching `nursing`/8710's, `laundry`/9601's and `holdco`/
6420's dual-actuation-on-one-entity shape, `high-stakes` is the two-
member set `#{:actuation/finalize-care-plan :actuation/finalize-
incident-response}`, each with its own history collection, sequence
counter, and dedicated double-actuation-guard boolean (`:care-plan-
finalized?`/`:incident-response-finalized?`, never a single `:status`
value).

### Decision 3: `mandatory-reporting-obligation-unresolved-violations` -- the 51st unconditional-evaluation screening grounding, a genuinely new concept

Before writing this check, every prior sibling's governor/registry
namespaces were grepped for "mandatory-report", "reporting-
obligation" and "abuse...report" as dedicated CHECK FUNCTIONS -- zero
hits, confirming this is a genuinely new unconditional-evaluation
concept, avoiding the false-precedent-claim risk `leasing`'s ADR-0001
documents. `mandatory-reporting-obligation-unresolved-violations`
reuses the unconditional-evaluation DISCIPLINE (`casualty.governor/
sanctions-violations`'s original fix) for the 51st distinct
application overall, continuing the count established across this
fleet's builds (most recently `edsupport.governor/background-check-
not-cleared-violations` at 50th). Grounded in real child-welfare
mandatory-reporting law: US CAPTA (42 U.S.C. §5106g), Japan's Child
Abuse Prevention Act Article 6, and Germany's SGB VIII §8a. Gates
`:safeguarding/screen` and both actuations.

### Decision 4: `background-check-not-cleared-violations` -- an honest FIFTH literal reuse, not claimed as new

`school.governor` established this concept FIRST; `sports.governor`
reused it literally as the SECOND instance; `personalservice.
governor` as the THIRD; `edsupport.governor` as the FOURTH.
`residential.governor/background-check-not-cleared-violations` is the
FIFTH literal instance of this specific concept, and the 52nd
distinct application of the unconditional-evaluation discipline
overall -- not claimed as new. Evaluated unconditionally so
`:background-check/screen` can HARD-hold on its own finding, not only
when reached via either actuation. Gates `:background-check/screen`
and both actuations.

### Decision 5: TWO dedicated double-actuation-guard booleans

`:care-plan-finalized?` and `:incident-response-finalized?` are each
a dedicated boolean on the `resident` record, never a single
`:status` value -- the same discipline every prior sibling governor's
guards establish, informed by `cloud-itonami-isic-6492`'s real
status-lifecycle bug (ADR-2607071320). Each actuation gets its OWN
guard, history collection and sequence counter, following `nursing`/
8710's precedent exactly.

### Decision 6: Store protocol, MemStore + DatomicStore parity

`residential.store/Store` is implemented by both `MemStore` (atom-
backed, default for dev/tests/demo) and `DatomicStore` (`langchain.
db`-backed), proven to satisfy the same contract in `test/
residential/store_contract_test.clj` -- the same seam every sibling
actor uses so swapping the SSoT backend is a configuration change,
not a rewrite. The protocol's per-entity accessor is named `resident`
directly -- not a Clojure special form, so no `-of` suffix workaround
was needed.

### Decision 7: Phase 0→3 rollout

Phase 3's `:auto` set has exactly one member, `:resident/intake` (no
capital risk). `:careplan/verify`, `:safeguarding/screen` and
`:background-check/screen` are never auto-eligible at any phase
(matching every sibling's screening-op posture), and BOTH actuations
are permanently excluded from every phase's `:auto` set -- a
structural fact, not a rollout milestone, enforced by BOTH
`residential.phase` and `residential.governor`'s `high-stakes` set
independently.

### Decision 8: no bespoke domain capability lib

This blueprint's own `:itonami.blueprint/required-technologies` names
no domain-specific capability beyond the generic robotics/identity/
forms/dmn/bpmn/audit-ledger stack -- there was no capability-lib
decision to make at all.

### Decision 9: mock + LLM advisor pair

`residential.residentialadvisor` provides `mock-advisor`
(deterministic, default everywhere -- the actor graph and governor
contract run offline) and `llm-advisor` (backed by `langchain.model/
ChatModel`, with a defensive EDN-proposal parser so a malformed LLM
response degrades to a safe low-confidence noop rather than ever
auto-finalizing a care plan or incident response).

### Decision 10: no `blueprint.edn` field-sync fixes needed

Matching `photo`/7420's, `personalservice`/9609's, `edsupport`/8550's
and `headoffice`/7010's own experience, this repo's `blueprint.edn`
already had the correct `isic-` prefixed `:id` and correctly
populated `:required-technologies`/`:optional-technologies` matching
the `kotoba-lang/industry` registry's own entry for `"8790"` exactly
-- only the `:maturity` field itself needed adding.

## Alternatives considered

- **Treating "care plan or incident response" as one conceptual act**
  (following `sports`/8541's either/or precedent). Rejected: unlike
  `sports`/8541's genuinely ambiguous phrasing (a single certification
  described loosely two ways), this blueprint's Offer section lists
  "care-plan proposal" and "incident-response proposal" as two
  SEPARATE line items, and the two acts are conceptually distinct (a
  proactive planning act vs. a reactive incident-handling act) --
  matching `nursing`/8710's own resolution of a nearly identical
  phrasing pattern.
- **Reusing `nursing`/8710's `credential-not-current` concept**
  instead of a new mandatory-reporting concept. Rejected: this
  blueprint's population (children/vulnerable residents, not
  medically-licensed-staff-centric) and its own domain concern
  (safeguarding/abuse reporting, not staff credentialing) call for a
  distinct, dedicated check grounded in real child-welfare law, not a
  relabeled credential check.
- **A single combined screening op** covering both the safeguarding
  and background-check concerns. Rejected: the two concerns are
  independently groundable in different real-world regulatory regimes
  (child-welfare mandatory-reporting law vs. staff-safeguarding
  background-check law), so two separate dedicated ops (each gated by
  its own HARD check) more precisely match the "screen the screening
  op directly" discipline this fleet's ADRs already establish.

## Consequences

- Sixty-seventh actor in this fleet (66 implemented before this
  build).
- Establishes a genuinely NEW unconditional-evaluation-screening
  concept (mandatory-reporting-obligation-unresolved), grep-verified
  absent from every prior sibling before the claim was finalized.
- Documents an honest FIFTH literal reuse of the background-check-
  not-cleared concept (school 1st, sports 2nd, personalservice 3rd,
  edsupport 4th, residential 5th), not claimed as new.
- `MemStore` ‖ `DatomicStore` parity is proven by `test/residential/
  store_contract_test.clj`, the same `:db-api`-driven swap pattern
  every sibling actor uses.
- `blueprint.edn` required no field-sync fixes this time (already
  correct) -- only the `:maturity` flip itself.

## References

- `orgs/cloud-itonami/cloud-itonami-isic-8790/README.md`
- `orgs/cloud-itonami/cloud-itonami-isic-8790/docs/business-model.md`
- `orgs/kotoba-lang/industry/resources/kotoba/industry/registry.edn` (entry `"8790"`)
