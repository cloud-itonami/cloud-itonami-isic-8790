# Operator Quickstart: ISIC 8790 Residential Care

## Prerequisites

1. **Java/Clojure runtime** — Clojure 1.11+
   ```bash
   clojure --version
   ```

2. **nbb (optional, for ClojureScript execution)**
   ```bash
   npm install -g nbb
   ```

3. **Monorepo siblings (if forking from the full workspace)**
   - `../../kotoba-lang/langgraph` — the StateGraph actor runtime
   - `../../kotoba-lang/langchain` — underlying LLM framework

   **Standalone fork:** Update `deps.edn` to use git coordinates instead of `:local/root` paths:
   ```clojure
   :deps {io.github.kotoba-lang/langgraph
          {:git/url "https://github.com/kotoba-lang/langgraph.git"
           :git/tag "v0.1.0"}}
   ```

## Run Tests

Verify the Residential Care Governor contract, phase invariants, and store consistency:

```bash
kbb -M:dev:test
```

This runs:
- Governor HARD-check contract (spec-basis, evidence, mandatory-reporting, background-check, double-finalization guards)
- Phase table invariants (phase 0→3, auto-eligible operations)
- Store parity (MemStore ‖ DatomicStore)
- Registry conformance (care-plan/incident-response record shape)
- Facts coverage reporting (per-jurisdiction safeguarding spec citations)

## Run & Demo the Actor

Walk one complete resident lifecycle (clean intake through care-plan finalization) plus four HARD-hold cases:

```bash
kbb -M:dev:run
```

This drives `residential.sim`, which:
1. Seeds a test resident with full evidence and cleared background check
2. Routes through intake → verification → safeguarding screening → care-plan finalization
3. Runs four guard-rail scenarios (fabricated spec-basis, missing evidence, unresolved mandatory-reporting, uncleared background check)
4. Prints the audit ledger and final resident state

## Residential Care Governor

**Location:** `src/residential/governor.kotoba`

The governor is the **independent approval layer** that sits between the ResidentialOps-LLM (in `residentialadvisor.cljc`) and execution. It enforces:

- `:spec-basis/fabricated` — reject if the cited regulation does not exist in this jurisdiction
- `:evidence/incomplete` — reject if required care-plan/incident-response evidence is missing
- `:mandatory-reporting/unresolved` — reject if a mandatory-reporting obligation is active and unresolved
- `:background-check/not-cleared` — reject if the background check is not cleared
- `:actuation/care-plan-finalized` — reject double-finalization of a care plan
- `:actuation/incident-finalized` — reject double-finalization of an incident response

**Never auto-commits** either finalization act (care-plan or incident-response) — both always route to human sign-off, at any phase.

## Static Analysis

Run `clj-kondo` linting (errors fail CI):

```bash
kbb -M:lint
```

## Jurisdiction Configuration

Add new jurisdictions by extending `residential.facts/catalog` in `src/residential/facts.kotoba`. Each entry must cite an official spec-basis (law, regulation, or guidance document):

```clojure
{:jurisdiction/id :usa-ca
 :jurisdiction/name "California (USA)"
 :jurisdiction/safeguarding-basis "California Child Abuse and Neglect Reporting Act (Penal Code §11165.7)"}
```

Currently seeded with 4 jurisdictions (JPN, USA, GBR, DEU). Adding coverage is additive; never fabricate jurisdiction requirements.

## Store Backend

- **Development:** `MemStore` (in-memory; resets on restart)
- **Production:** `DatomicStore` via `langchain.db` (persistent; supports audit ledger and checkpoints)

Both implement the same `Store` protocol (see `src/residential/store.kotoba`).

## Next Steps

1. **Deploy a test operator:** See `docs/operator-guide.md`
2. **Understand the business model:** See `docs/business-model.md`
3. **Architecture deep-dive:** See `docs/adr/0001-architecture.md`
4. **Fork & customize:** This repo is forkable; adapt the governor's decision rules and care-plan/incident-response evidence checklist to your jurisdiction's laws and your facility's risk profile
