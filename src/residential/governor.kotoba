(ns residential.governor
  "Residential Care Governor -- the independent compliance layer that
  earns the ResidentialOps-LLM the right to commit. The LLM has no
  notion of jurisdictional child-welfare/safeguarding law, whether a
  resident's own recorded mandatory-reporting obligation has actually
  stayed unresolved, whether a background check has actually stayed
  cleared, or when an act stops being a draft and becomes a real-
  world care-plan or incident-response finalization, so this MUST be
  a separate system able to *reject* a proposal and fall back to HOLD
  -- the residential-care analog of `cloud-itonami-isic-8620`'s
  ClinicGovernor.

  Five checks, in priority order, ALL HARD violations: a human
  approver CANNOT override them (you don't get to approve your way
  past a fabricated jurisdiction spec-basis, incomplete evidence, an
  unresolved mandatory-reporting obligation, an uncleared background
  check, or a double finalization). The confidence/actuation gate is
  SOFT: it asks a human to look (low confidence / actuation), and the
  human may approve -- but see `residential.phase`: for `:stake
  :actuation/finalize-care-plan`/`:actuation/finalize-incident-
  response` (a real care-plan finalization or a real incident-
  response finalization) NO phase ever allows auto-commit either. Two
  independent layers agree that actuation is always a human call.

    1. Spec-basis                  -- did the care-plan proposal cite
                                       an OFFICIAL source (`residential.
                                       facts`), or invent one?
    2. Evidence incomplete         -- for `:actuation/finalize-care-
                                       plan`/`:actuation/finalize-
                                       incident-response`, has the
                                       resident actually been assessed
                                       with a full resident-consent-
                                       record/care-plan-record/
                                       safeguarding-assessment-record/
                                       incident-report-record evidence
                                       checklist on file?
    3. Mandatory-reporting
       obligation unresolved       -- reported by THIS proposal itself
                                       (a `:safeguarding/screen` that
                                       just found an unresolved
                                       reporting obligation), or
                                       already on file for the resident
                                       (`:safeguarding/screen`/either
                                       actuation). Evaluated
                                       UNCONDITIONALLY (not scoped to
                                       a specific op) so the screening
                                       op itself can HARD-hold on its
                                       own finding. A GENUINELY NEW
                                       concept in this fleet (grep-
                                       verified absent -- no dedicated
                                       'mandatory-reporting-obligation'
                                       CHECK FUNCTION exists anywhere
                                       else in this fleet), the 51st
                                       distinct application of the
                                       unconditional-evaluation
                                       discipline overall (`casualty.
                                       governor/sanctions-violations`'s
                                       original fix; most recently
                                       `edsupport.governor/background-
                                       check-not-cleared-violations` at
                                       50th). Grounded in real child-
                                       welfare mandatory-reporting law
                                       (US CAPTA 42 U.S.C. section
                                       5106g, Japan's Child Abuse
                                       Prevention Act Article 6,
                                       Germany's SGB VIII section 8a).
    4. Background check not
       cleared                        -- reported by THIS proposal
                                       itself (a `:background-check/
                                       screen` that just found an
                                       uncleared check), or already on
                                       file for the resident
                                       (`:background-check/screen`/
                                       either actuation). Evaluated
                                       UNCONDITIONALLY, the SAME
                                       discipline as check 3 above --
                                       an HONEST literal reuse of
                                       `school.governor/background-
                                       check-not-cleared-violations`'s
                                       own concept (the FIRST
                                       instance; `sports.governor`
                                       reused it literally as the
                                       SECOND, `personalservice.
                                       governor` as the THIRD,
                                       `edsupport.governor` as the
                                       FOURTH) -- the FIFTH literal
                                       instance of this specific
                                       concept, and the 52nd distinct
                                       application of the
                                       unconditional-evaluation
                                       discipline overall, not claimed
                                       as new.
    5. Confidence floor / actuation
       gate                          -- LLM confidence below threshold,
                                       OR the op is `:actuation/
                                       finalize-care-plan`/`:actuation/
                                       finalize-incident-response`
                                       (REAL residential-care acts) ->
                                       escalate.

  Two more guards, double-finalization prevention, are enforced but
  NOT listed as numbered HARD checks above because they need no
  upstream comparison at all -- `already-care-plan-finalized-
  violations`/`already-incident-finalized-violations` refuse to
  finalize a care plan/incident response for the SAME resident twice,
  off dedicated `:care-plan-finalized?`/`:incident-response-
  finalized?` facts (never a `:status` value) -- the SAME 'check a
  dedicated boolean, not status' discipline every prior sibling
  governor's guards establish, informed by `cloud-itonami-isic-6492`'s
  status-lifecycle bug (ADR-2607071320)."
  (:require [residential.facts :as facts]
            [residential.store :as store]))

(def confidence-floor 0.6)

(def high-stakes
  "Stakes grave enough to always require a human, even when clean.
  Finalizing a real care plan and finalizing a real incident response
  are the two real-world actuation events this actor performs -- a
  two-member set, matching every prior dual-actuation sibling's shape
  (`nursing`/8710, `laundry`/9601, `holdco`/6420). Both are POSITIVE
  actuations (finalizing a real record), matching this fleet's
  majority actuation shape (`3600`/`6190` remain the only negative-
  actuation exceptions)."
  #{:actuation/finalize-care-plan :actuation/finalize-incident-response})

;; ----------------------------- checks -----------------------------

(defn- spec-basis-violations
  "A `:careplan/verify` (or either actuation) proposal with no spec-
  basis citation is a HARD violation -- never invent a jurisdiction's
  residential-care/safeguarding requirements."
  [{:keys [op]} proposal]
  (when (contains? #{:careplan/verify :actuation/finalize-care-plan :actuation/finalize-incident-response} op)
    (let [value (:value proposal)]
      (when (or (empty? (:cites proposal))
                (and (contains? value :spec-basis) (nil? (:spec-basis value))))
        [{:rule :no-spec-basis
          :detail "公式spec-basisの引用が無い提案はケア基準として扱えない"}]))))

(defn- evidence-incomplete-violations
  "For either actuation, the jurisdiction's required resident-
  consent-record/care-plan-record/safeguarding-assessment-record/
  incident-report-record evidence must actually be satisfied -- do
  not trust the advisor's self-reported confidence alone."
  [{:keys [op subject]} st]
  (when (contains? #{:actuation/finalize-care-plan :actuation/finalize-incident-response} op)
    (let [r (store/resident st subject)
          careplan (store/careplan-of st subject)]
      (when-not (and careplan
                     (facts/required-evidence-satisfied?
                      (:jurisdiction r) (:checklist careplan)))
        [{:rule :evidence-incomplete
          :detail "法域の必要書類(入所者同意記録/ケアプラン記録/セーフガーディング評価記録/事故報告記録等)が充足していない状態での提案"}]))))

(defn- mandatory-reporting-obligation-unresolved-violations
  "An unresolved mandatory-reporting obligation -- reported by THIS
  proposal (e.g. a `:safeguarding/screen` that itself just found an
  unresolved obligation), or already on file in the store for the
  resident (`:safeguarding/screen`/either actuation) -- is a HARD,
  un-overridable hold. Evaluated UNCONDITIONALLY (not scoped to a
  specific op) so the screening op itself can HARD-hold on its own
  finding."
  [{:keys [op subject]} proposal st]
  (let [hit-in-proposal? (true? (get-in proposal [:value :mandatory-reporting-obligation-unresolved?]))
        resident-id (when (contains? #{:safeguarding/screen :actuation/finalize-care-plan :actuation/finalize-incident-response} op) subject)
        hit-on-file? (and resident-id (true? (:mandatory-reporting-obligation-unresolved? (store/resident st resident-id))))]
    (when (or hit-in-proposal? hit-on-file?)
      [{:rule :mandatory-reporting-obligation-unresolved
        :detail "義務的通告事項が未解決の状態でのケアプラン/事故対応確定提案は進められない"}])))

(defn- background-check-not-cleared-violations
  "An uncleared background check -- reported by THIS proposal (e.g. a
  `:background-check/screen` that itself just found an uncleared
  check), or already on file in the store for the resident
  (`:background-check/screen`/either actuation) -- is a HARD, un-
  overridable hold. Evaluated UNCONDITIONALLY (not scoped to a
  specific op) so the screening op itself can HARD-hold on its own
  finding."
  [{:keys [op subject]} proposal st]
  (let [hit-in-proposal? (= :not-cleared (get-in proposal [:value :verdict]))
        resident-id (when (contains? #{:background-check/screen :actuation/finalize-care-plan :actuation/finalize-incident-response} op) subject)
        hit-on-file? (and resident-id (= :not-cleared (:verdict (store/background-check-of st resident-id))))]
    (when (or hit-in-proposal? hit-on-file?)
      [{:rule :background-check-not-cleared
        :detail "身元確認が未完了の状態でのケアプラン/事故対応確定提案は進められない"}])))

(defn- already-care-plan-finalized-violations
  "For `:actuation/finalize-care-plan`, refuses to finalize a care
  plan for the SAME resident twice, off a dedicated `:care-plan-
  finalized?` fact (never a `:status` value)."
  [{:keys [op subject]} st]
  (when (= op :actuation/finalize-care-plan)
    (when (store/resident-already-care-plan-finalized? st subject)
      [{:rule :already-care-plan-finalized
        :detail (str subject " は既にケアプラン確定済み")}])))

(defn- already-incident-finalized-violations
  "For `:actuation/finalize-incident-response`, refuses to finalize an
  incident response for the SAME resident twice, off a dedicated
  `:incident-response-finalized?` fact (never a `:status` value)."
  [{:keys [op subject]} st]
  (when (= op :actuation/finalize-incident-response)
    (when (store/resident-already-incident-finalized? st subject)
      [{:rule :already-incident-finalized
        :detail (str subject " は既に事故対応確定済み")}])))

(defn check
  "Censors a ResidentialOps-LLM proposal against the governor rules.
  Returns {:ok? bool :violations [..] :confidence c :escalate? bool
  :high-stakes? bool :hard? bool}."
  [request _context proposal st]
  (let [hard (into []
                   (concat (spec-basis-violations request proposal)
                           (evidence-incomplete-violations request st)
                           (mandatory-reporting-obligation-unresolved-violations request proposal st)
                           (background-check-not-cleared-violations request proposal st)
                           (already-care-plan-finalized-violations request st)
                           (already-incident-finalized-violations request st)))
        conf (:confidence proposal 0.0)
        low? (< conf confidence-floor)
        stakes? (boolean (high-stakes (:stake proposal)))
        hard? (boolean (seq hard))]
    {:ok?          (and (not hard?) (not low?) (not stakes?))
     :violations   hard
     :confidence   conf
     :hard?        hard?
     :escalate?    (and (not hard?) (or low? stakes?))
     :high-stakes? stakes?}))

(defn hold-fact
  "The audit fact written when a proposal is rejected (HOLD)."
  [request context verdict]
  {:t          :governor-hold
   :op         (:op request)
   :actor      (:actor-id context)
   :subject    (:subject request)
   :disposition :hold
   :basis      (mapv :rule (:violations verdict))
   :violations (:violations verdict)
   :confidence (:confidence verdict)})
