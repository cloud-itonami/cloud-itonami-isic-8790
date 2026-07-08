(ns residential.registry
  "Pure-function care-plan-finalization + incident-response-
  finalization record construction -- an append-only residential-care
  book-of-record draft.

  Like every sibling actor's registry, there is no single
  international check-digit standard for a care-plan or incident-
  response reference number -- every residential-care operator/
  jurisdiction assigns its own reference format. This namespace does
  NOT invent one; it builds a jurisdiction-scoped sequence number and
  validates the record's required fields, the same honest, non-
  fabricating discipline `residential.facts` uses.

  Unlike siblings whose distinctive check is a numeric/temporal
  ground-truth recompute (a registry-level pure predicate), this
  build's TWO distinctive checks (`mandatory-reporting-obligation-
  unresolved?` and `background-check-not-cleared?`) are both BOOLEAN
  flags read directly off the resident's own record by `residential.
  governor` -- the same shape `photo.governor`'s `minor-subject-
  guardian-consent-unresolved-violations` and `personalservice.
  governor`'s/`edsupport.governor`'s `background-check-not-cleared-
  violations` use, neither of which needed a dedicated registry-level
  predicate either. This namespace is therefore intentionally
  'plain': record construction only, no distinctive check function.

  This namespace is pure data + pure functions -- no I/O, no network
  call to any real residential-care-management system. It builds the
  RECORD a residential-care operator would keep, not the act of
  finalizing the care plan or the incident response itself (that is
  `residential.operation`'s `:actuation/finalize-care-plan`/
  `:actuation/finalize-incident-response`, always human-gated -- see
  README `Actuation`)."
  (:require [clojure.string :as str]))

(defn- unsigned-certificate
  "Every certificate this actor produces is UNSIGNED -- signature is the
  residential-care operator's own act, not this actor's. See README
  `Actuation`."
  [kind subject record-id]
  {"@context" ["https://www.w3.org/ns/credentials/v2"]
   "type" ["VerifiableCredential" kind]
   "credentialSubject" {"id" subject "record" record-id}
   "proof" nil
   "issued_by_registry" false
   "status" "draft-unsigned"})

(defn- zero-pad [n w]
  (let [s (str n)]
    (str (apply str (repeat (max 0 (- w (count s))) "0")) s)))

(defn register-care-plan-finalization
  "Validate + construct the CARE-PLAN-FINALIZATION registration DRAFT
  -- the residential-care operator's own act of finalizing a real
  care plan. Pure function -- does not touch any real residential-
  care-management system; it builds the RECORD an operator would
  keep. `residential.governor` independently re-verifies the
  resident's own safeguarding/background-check status, and blocks a
  double-finalization for the same resident, before this is ever
  allowed to commit."
  [resident-id jurisdiction sequence]
  (when-not (and resident-id (not= resident-id ""))
    (throw (ex-info "care-plan-finalization: resident_id required" {})))
  (when-not (and jurisdiction (not= jurisdiction ""))
    (throw (ex-info "care-plan-finalization: jurisdiction required" {})))
  (when (< sequence 0)
    (throw (ex-info "care-plan-finalization: sequence must be >= 0" {})))
  (let [careplan-number (str (str/upper-case jurisdiction) "-CP-" (zero-pad sequence 6))
        record {"record_id" careplan-number
                "kind" "care-plan-finalization-draft"
                "resident_id" resident-id
                "jurisdiction" jurisdiction
                "immutable" true}]
    {"record" record "careplan_number" careplan-number
     "certificate" (unsigned-certificate "CarePlanFinalization" careplan-number careplan-number)}))

(defn register-incident-response-finalization
  "Validate + construct the INCIDENT-RESPONSE-FINALIZATION
  registration DRAFT -- the residential-care operator's own act of
  finalizing a real incident response. Pure function -- does not
  touch any real residential-care-management system; it builds the
  RECORD an operator would keep. `residential.governor` independently
  re-verifies the resident's own safeguarding/background-check
  status, and blocks a double-finalization for the same resident,
  before this is ever allowed to commit."
  [resident-id jurisdiction sequence]
  (when-not (and resident-id (not= resident-id ""))
    (throw (ex-info "incident-response-finalization: resident_id required" {})))
  (when-not (and jurisdiction (not= jurisdiction ""))
    (throw (ex-info "incident-response-finalization: jurisdiction required" {})))
  (when (< sequence 0)
    (throw (ex-info "incident-response-finalization: sequence must be >= 0" {})))
  (let [incident-number (str (str/upper-case jurisdiction) "-INC-" (zero-pad sequence 6))
        record {"record_id" incident-number
                "kind" "incident-response-finalization-draft"
                "resident_id" resident-id
                "jurisdiction" jurisdiction
                "immutable" true}]
    {"record" record "incident_number" incident-number
     "certificate" (unsigned-certificate "IncidentResponseFinalization" incident-number incident-number)}))

(defn append [history result]
  (conj (vec history) (get result "record")))
