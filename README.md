# cloud-itonami-isic-8790

Open Business Blueprint for **ISIC Rev.5 8790**: Other residential care activities.

This repository designs a forkable OSS business for other residential care activities not elsewhere classified (e.g. children's homes, orphanages, residential care for the homeless) -- run by a qualified, licensed operator so a community or
independent provider never surrenders patient/resident data and ledgers to a
closed SaaS.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot performs
the physical domain work**. Here a facility-safety monitoring robot supports physical resident safety checks,
under an actor that proposes actions and an independent **Residential Care Governor**
that gates them. The governor never dispatches hardware itself;
`:high`/`:safety-critical` actions require human sign-off.

## Core Contract

```text
intake + identity + care records
        |
        v
ResidentialOps-LLM -> Residential Care Governor -> hold, proceed, or human approval
        |
        v
care ledger + evidence record + audit
```

No automated proposal, by itself, can complete the following without governor
approval and audit evidence: finalizing a care plan or incident response.

## Capability layer

This blueprint resolves its technology stack via
[`kotoba-lang/industry`](https://github.com/kotoba-lang/industry) (ISIC
`8790`). This vertical's care/case records are practice-specific rather
than a shared cross-operator data contract, so it runs on the generic
identity/forms/dmn/bpmn/audit-ledger stack -- no bespoke domain capability lib.

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## Maturity

`:blueprint` -- this repository is the published business/operator design.
The governed actor implementation (`ResidentialOps-LLM` + `Residential Care Governor` as
running code) is a follow-up, same as any other `:blueprint`-tier
`cloud-itonami-*` entry in `kotoba-lang/industry`'s registry.

## License

Code and implementation templates are AGPL-3.0-or-later.
