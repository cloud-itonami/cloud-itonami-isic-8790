# cloud-itonami-isic-8790

Open Business Blueprint for **ISIC Rev.5 8790**: Other residential
care activities.

This repository publishes a residential-care actor -- resident
intake, per-jurisdiction residential-care/safeguarding regulatory
assessment, mandatory-reporting-obligation screening, background-
check screening, care-plan finalization and incident-response
finalization -- as an OSS business that any qualified, licensed
operator can fork, deploy, run, improve and sell, so a community or
independent provider never surrenders resident data and ledgers to a
closed SaaS.

Built on this workspace's
[`langgraph`](https://github.com/kotoba-lang/langgraph)
StateGraph runtime (portable `.cljc`, supervised superstep loop,
interrupts, Datomic/in-mem checkpoints) -- the same actor pattern as
every prior actor in this fleet
([`cloud-itonami-isic-6511`](https://github.com/cloud-itonami/cloud-itonami-isic-6511),
[`6512`](https://github.com/cloud-itonami/cloud-itonami-isic-6512),
[`6621`](https://github.com/cloud-itonami/cloud-itonami-isic-6621),
[`6622`](https://github.com/cloud-itonami/cloud-itonami-isic-6622),
[`6629`](https://github.com/cloud-itonami/cloud-itonami-isic-6629),
[`6520`](https://github.com/cloud-itonami/cloud-itonami-isic-6520),
[`6530`](https://github.com/cloud-itonami/cloud-itonami-isic-6530),
[`6820`](https://github.com/cloud-itonami/cloud-itonami-isic-6820),
[`6612`](https://github.com/cloud-itonami/cloud-itonami-isic-6612),
[`6492`](https://github.com/cloud-itonami/cloud-itonami-isic-6492),
[`6920`](https://github.com/cloud-itonami/cloud-itonami-isic-6920),
[`6611`](https://github.com/cloud-itonami/cloud-itonami-isic-6611),
[`7120`](https://github.com/cloud-itonami/cloud-itonami-isic-7120),
[`8620`](https://github.com/cloud-itonami/cloud-itonami-isic-8620),
[`8530`](https://github.com/cloud-itonami/cloud-itonami-isic-8530),
[`9200`](https://github.com/cloud-itonami/cloud-itonami-isic-9200),
[`7500`](https://github.com/cloud-itonami/cloud-itonami-isic-7500),
[`9603`](https://github.com/cloud-itonami/cloud-itonami-isic-9603),
[`9521`](https://github.com/cloud-itonami/cloud-itonami-isic-9521),
[`9321`](https://github.com/cloud-itonami/cloud-itonami-isic-9321),
[`8730`](https://github.com/cloud-itonami/cloud-itonami-isic-8730),
[`9102`](https://github.com/cloud-itonami/cloud-itonami-isic-9102),
[`9103`](https://github.com/cloud-itonami/cloud-itonami-isic-9103),
[`9602`](https://github.com/cloud-itonami/cloud-itonami-isic-9602),
[`9000`](https://github.com/cloud-itonami/cloud-itonami-isic-9000),
[`8890`](https://github.com/cloud-itonami/cloud-itonami-isic-8890),
[`8610`](https://github.com/cloud-itonami/cloud-itonami-isic-8610),
[`9311`](https://github.com/cloud-itonami/cloud-itonami-isic-9311),
[`8510`](https://github.com/cloud-itonami/cloud-itonami-isic-8510),
[`9412`](https://github.com/cloud-itonami/cloud-itonami-isic-9412),
[`6491`](https://github.com/cloud-itonami/cloud-itonami-isic-6491),
[`8720`](https://github.com/cloud-itonami/cloud-itonami-isic-8720),
[`8521`](https://github.com/cloud-itonami/cloud-itonami-isic-8521),
[`6619`](https://github.com/cloud-itonami/cloud-itonami-isic-6619),
[`3600`](https://github.com/cloud-itonami/cloud-itonami-isic-3600),
[`6190`](https://github.com/cloud-itonami/cloud-itonami-isic-6190),
[`3030`](https://github.com/cloud-itonami/cloud-itonami-isic-3030),
[`3830`](https://github.com/cloud-itonami/cloud-itonami-isic-3830),
[`7020`](https://github.com/cloud-itonami/cloud-itonami-isic-7020),
[`9420`](https://github.com/cloud-itonami/cloud-itonami-isic-9420),
[`9491`](https://github.com/cloud-itonami/cloud-itonami-isic-9491),
[`2610`](https://github.com/cloud-itonami/cloud-itonami-isic-2610),
[`3512`](https://github.com/cloud-itonami/cloud-itonami-isic-3512),
[`8810`](https://github.com/cloud-itonami/cloud-itonami-isic-8810),
[`8691`](https://github.com/cloud-itonami/cloud-itonami-isic-8691),
[`8569`](https://github.com/cloud-itonami/cloud-itonami-isic-8569),
[`6419`](https://github.com/cloud-itonami/cloud-itonami-isic-6419),
[`7310`](https://github.com/cloud-itonami/cloud-itonami-isic-7310),
[`7320`](https://github.com/cloud-itonami/cloud-itonami-isic-7320),
[`7210`](https://github.com/cloud-itonami/cloud-itonami-isic-7210),
[`7410`](https://github.com/cloud-itonami/cloud-itonami-isic-7410),
[`8710`](https://github.com/cloud-itonami/cloud-itonami-isic-8710),
[`8541`](https://github.com/cloud-itonami/cloud-itonami-isic-8541),
[`8690`](https://github.com/cloud-itonami/cloud-itonami-isic-8690),
[`9601`](https://github.com/cloud-itonami/cloud-itonami-isic-9601),
[`6420`](https://github.com/cloud-itonami/cloud-itonami-isic-6420),
[`7420`](https://github.com/cloud-itonami/cloud-itonami-isic-7420),
[`9609`](https://github.com/cloud-itonami/cloud-itonami-isic-9609),
[`8550`](https://github.com/cloud-itonami/cloud-itonami-isic-8550),
[`7010`](https://github.com/cloud-itonami/cloud-itonami-isic-7010)) --
here it is **ResidentialOps-LLM ⊣ Residential Care Governor**.

> **Why an actor layer at all?** An LLM is great at drafting a
> resident-intake summary, normalizing records, and checking whether
> a jurisdiction's own required residential-care/safeguarding
> evidence checklist has been satisfied -- but it has **no notion of
> which jurisdiction's child-welfare/safeguarding law is official, no
> license to finalize a real care plan or incident response, and no
> way to know on its own whether a mandatory-reporting obligation has
> actually stayed resolved**. Letting it finalize a care plan or
> incident response directly invites fabricated regulatory citations,
> a mandatory-reporting obligation being quietly overlooked, and an
> uncleared background check being waved through -- and liability,
> and child-welfare/safeguarding risk, for whoever runs it. This
> project seals the ResidentialOps-LLM into a single node and wraps
> it with an independent **Residential Care Governor**, a human
> **approval workflow**, and an immutable **audit ledger**.

## Scope: what this actor does and does not do

This actor covers resident intake through residential-care/
safeguarding regulatory assessment, mandatory-reporting-obligation
screening, background-check screening, care-plan finalization and
incident-response finalization. It does **not**, by itself, hold any
license required to operate as a residential-care facility in a given
jurisdiction, and it does not claim to. It also does not perform the
actual day-to-day care work itself, or judge its quality --
`residential.governor`'s checks read the resident's own recorded
boolean fields directly, not a care-quality review. Whoever deploys
and operates a live instance (a licensed residential-care operator)
supplies any jurisdiction-specific license, the real care workforce
and the real case-management-system integrations, and bears that
jurisdiction's liability -- the software supplies the governed,
spec-cited, audited execution scaffold so that operator does not have
to build the compliance layer from scratch.

### Actuation

**Finalizing a real care plan or finalizing a real incident response
is never autonomous, at any phase, by construction.** Two independent
layers enforce this (`residential.governor`'s `:actuation/finalize-
care-plan`/`:actuation/finalize-incident-response` high-stakes gate
and `residential.phase`'s phase table, which never puts either op in
any phase's `:auto` set) -- see `residential.phase`'s docstring and
`test/residential/phase_test.kotoba`'s `finalize-care-plan-never-auto-
at-any-phase`/`finalize-incident-response-never-auto-at-any-phase`.
The actor may draft, check and recommend; a human licensed care-staff
member is always the one who actually finalizes a care plan or
incident response. Following `nursing`/8710's, `laundry`/9601's and
`holdco`/6420's dual-actuation-on-one-entity precedent -- the
blueprint's own Offer explicitly lists "care-plan proposal" and
"incident-response proposal" as two separate items, and the README/
business-model.md/operator-guide.md consistently name BOTH acts
together ("finalizing a care plan or incident response") -- this
build treats them as TWO distinct real-world acts, each with its own
history collection, sequence counter and dedicated double-actuation-
guard boolean. Both are POSITIVE actuations (finalizing a real
record), matching this fleet's majority actuation shape (`3600`/
`6190` are the fleet's two NEGATIVE-actuation exceptions).

## The core contract

```
resident intake + jurisdiction facts (residential.facts, spec-cited)
        |
        v
   ┌───────────────────────┐   proposal      ┌───────────────────────┐
   │ ResidentialOps-LLM    │ ─────────────▶ │ Residential Care              │  (independent system)
   │ (sealed)              │  + citations    │ Governor:                    │
   └───────────────────────┘                 │ spec-basis · evidence-       │
          │                 commit ◀┼ incomplete · mandatory-           │
          │                         │ reporting-obligation-unresolved     │
    record + ledger        escalate ┼ (unconditional, NEW) · background-    │
          │              (ALWAYS for│ check-not-cleared (unconditional,      │
          │               both      │ honest reuse) · already-care-plan-       │
          │               actuations│ -finalized · already-incident-             │
          ▼                         │ finalized                                    │
      human approval                └───────────────────────┘
```

**The ResidentialOps-LLM never finalizes a care plan or incident
response the Residential Care Governor would reject, and never does
so without a human sign-off.** Hard violations (fabricated regulatory
requirements; unsupported evidence; an unresolved mandatory-reporting
obligation; an uncleared background check; a double finalization)
force **hold** and *cannot* be approved past; a clean finalization
proposal still always routes to a human.

## Run

```bash
kbb -M:dev:run     # walk one clean dual-actuation lifecycle + four HARD-hold cases through the actor
kbb -M:dev:test    # governor contract · phase invariants · store parity · registry conformance · facts coverage
kbb -M:lint        # clj-kondo (errors fail; CI mirrors this)
```

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot
performs the physical domain work**. Here a facility-safety
monitoring robot supports physical resident safety checks, under the
actor, gated by the independent **Residential Care Governor**. The
governor never dispatches hardware itself; `:high`/`:safety-critical`
actions require human sign-off.

## Open business

This repository is not only source code. It is a public, forkable
business model:

| Layer | What is open |
|---|---|
| OSS core | Actor runtime, Residential Care Governor, care-plan/incident-response draft records, audit ledger |
| Business blueprint | Customer, offer, pricing, unit economics, sales motion |
| Operator playbook | How to fork, license, deploy and support the service in a jurisdiction |
| Trust controls | Governance, security reporting, actuation invariant, audit requirements |

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md) to start this as an
open business on itonami.cloud, and
[`docs/adr/0001-architecture.md`](docs/adr/0001-architecture.md) for the
full architecture and decision record.

## Capability layer

This blueprint resolves its technology stack via
[`kotoba-lang/industry`](https://github.com/kotoba-lang/industry) (ISIC
`8790`). This vertical's resident/case records are practice-specific
rather than a shared cross-operator data contract, so `residential.*`
runs on the generic robotics/identity/forms/dmn/bpmn/audit-ledger
stack only -- no bespoke domain capability lib to reference at all.

## Layout

| File | Role |
|---|---|
| `src/residential/store.kotoba` | **Store** protocol -- `MemStore` ‖ `DatomicStore` (`langchain.db`) + append-only audit ledger + SEPARATE care-plan-finalization and incident-response-finalization histories/sequence counters. No dynamically-filed sub-record -- each actuation op acts directly on a pre-seeded resident, and the double-actuation guards check dedicated `:care-plan-finalized?`/`:incident-response-finalized?` booleans rather than a `:status` value |
| `src/residential/registry.kotoba` | Care-plan- and incident-response-finalization draft records. Intentionally 'plain': this build's two distinctive checks are both boolean flags evaluated directly by the governor, not registry-level numeric/temporal predicates |
| `src/residential/facts.kotoba` | Per-jurisdiction residential-care/safeguarding catalog with an official spec-basis citation per entry, honest coverage reporting |
| `src/residential/residentialadvisor.kotoba` | **ResidentialOps-LLM** -- `mock-advisor` ‖ `llm-advisor`; intake/careplan-verification/safeguarding-screening/background-check-screening/care-plan-finalization/incident-response-finalization proposals |
| `src/residential/governor.kotoba` | **Residential Care Governor** -- 6 HARD checks (spec-basis · evidence-incomplete · mandatory-reporting-obligation-unresolved, unconditional evaluation, GENUINELY NEW, the 51st grounding of this discipline · background-check-not-cleared, unconditional evaluation, the FIFTH literal instance of `school`'s/`sports`'s/`personalservice`'s/`edsupport`'s concept, the 52nd grounding overall, not claimed as new · already-care-plan-finalized guard · already-incident-finalized guard) + 1 soft (confidence/actuation gate) |
| `src/residential/phase.kotoba` | **Phase 0→3** -- read-only → assisted intake → assisted verify → supervised (both finalizations always human; resident intake is the ONLY auto-eligible op, no direct capital risk) |
| `src/residential/operation.kotoba` | **OperationActor** -- langgraph-clj StateGraph |
| `src/residential/sim.kotoba` | demo driver |
| `test/residential/*_test.clj` | governor contract · phase invariants · store parity · registry conformance · facts coverage |

## Business-process coverage (honest)

This actor covers resident intake through residential-care/
safeguarding regulatory assessment, mandatory-reporting-obligation
screening, background-check screening, care-plan finalization and
incident-response finalization -- the core governed lifecycle this
blueprint's own `docs/business-model.md` names as its Offer:

| Covered | Not covered (out of scope for this R0) |
|---|---|
| Resident intake + per-jurisdiction evidence checklisting, HARD-gated on an official spec-basis citation (`:resident/intake`/`:careplan/verify`) | Real case-management-system integration, real day-to-day residential-care work itself (see `residential.facts`'s docstring) |
| Mandatory-reporting-obligation screening, evaluated unconditionally so the screening op itself can HARD-hold on its own finding (`:safeguarding/screen`) | Any care-quality judgment itself -- deliberately outside this actor's competence |
| Background-check screening, evaluated unconditionally (`:background-check/screen`) | |
| Care-plan finalization, HARD-gated on full evidence, a resolved safeguarding status and a cleared background check, plus a double-finalization guard (`:actuation/finalize-care-plan`) | |
| Incident-response finalization, HARD-gated identically, plus its OWN dedicated double-finalization guard (`:actuation/finalize-incident-response`) | |
| Immutable audit ledger for every intake/verification/screening/finalization decision | |

Extending coverage is additive: add the next gate (e.g. a placement-
stability-review check) as its own governed op with its own HARD
checks and tests, following the SAME "an independent governor
re-verifies against the actor's own records before any real-world
act" pattern this repo's flagship ops already establish.

## Jurisdiction coverage (honest)

`residential.facts/coverage` reports how many requested jurisdictions
actually have an official spec-basis in `residential.facts/catalog`
-- currently 4 seeded (JPN, USA, GBR, DEU) out of ~194 jurisdictions
worldwide. This is a starting catalog to prove the governor contract
end-to-end, not a claim of global coverage. Adding a jurisdiction is
additive: one map entry in `residential.facts/catalog`, citing a real
official source -- never fabricate a jurisdiction's requirements to
make coverage look bigger.

## Maturity

`:implemented` -- `ResidentialOps-LLM` + `Residential Care Governor`
run as real, tested code (see `Run` above), promoted from the
originally-published `:blueprint`-tier scaffold, modeled closely on
the sixty-six prior actors' architecture. See `docs/adr/0001-
architecture.md` for the history and design.

## License

Code and implementation templates are AGPL-3.0-or-later.
