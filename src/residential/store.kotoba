(ns residential.store
  "SSoT for the residential-care actor, behind a `Store` protocol so
  the backend is a swap, not a rewrite -- the same seam every prior
  `cloud-itonami-isic-*` actor in this fleet uses:

    - `MemStore`     -- atom of EDN. The deterministic default for
                        dev/tests/demo (no deps).
    - `DatomicStore` -- backed by `langchain.db`, a Datomic-API-compatible
                        EAV store (datalog q / pull / upsert). Pure `.cljc`,
                        so it runs offline AND can be pointed at a real
                        Datomic Local or a kotoba-server pod by swapping
                        `langchain.db`'s `:db-api` (see langchain.kotoba-db).

  Both implement the same protocol and pass the same contract
  (test/residential/store_contract_test.clj), which is the whole
  point: the actor, the Residential Care Governor and the audit
  ledger never know which SSoT they run on.

  Like every prior dual-actuation sibling (`nursing`/8710, `laundry`/
  9601, `holdco`/6420), this actor has TWO actuation events
  (finalizing a care plan, finalizing an incident response) acting on
  the SAME entity (a `resident`), each with its OWN history
  collection, sequence counter and dedicated double-actuation-guard
  boolean (`:care-plan-finalized?`/`:incident-response-finalized?`,
  never a `:status` value) -- the same discipline every prior
  sibling governor's guards establish, informed by `cloud-itonami-
  isic-6492`'s status-lifecycle bug (ADR-2607071320).

  NOTE on naming: the protocol's per-entity accessor is `resident`
  directly -- not a Clojure special form, so no `-of` suffix
  workaround was needed.

  The ledger stays append-only on every backend: 'which resident was
  screened for an unresolved mandatory-reporting obligation, which
  resident was screened for a cleared background check, which care
  plan/incident response was finalized, on what jurisdictional basis,
  approved by whom' is always a query over an immutable log -- the
  audit trail a family/community trusting a residential-care operator
  needs, and the evidence an operator needs if a finalization decision
  is later disputed."
  (:require [residential.registry :as registry]
            [langchain.db :as d]
            [langchain-store.core :as ls]))

(defprotocol Store
  (resident [s id])
  (all-residents [s])
  (safeguarding-of [s resident-id] "committed mandatory-reporting-obligation screening verdict for a resident, or nil")
  (background-check-of [s resident-id] "committed background-check screening verdict for a resident, or nil")
  (careplan-of [s resident-id] "committed care-plan evidence assessment, or nil")
  (ledger [s])
  (careplan-history [s] "the append-only care-plan-finalization history (residential.registry drafts)")
  (incident-history [s] "the append-only incident-response-finalization history (residential.registry drafts)")
  (next-careplan-sequence [s jurisdiction] "next care-plan-number sequence for a jurisdiction")
  (next-incident-sequence [s jurisdiction] "next incident-number sequence for a jurisdiction")
  (resident-already-care-plan-finalized? [s resident-id] "has this resident's care plan already been finalized?")
  (resident-already-incident-finalized? [s resident-id] "has this resident's incident response already been finalized?")
  (commit-record! [s record] "apply a committed op's record to the SSoT")
  (append-ledger! [s fact]   "append one immutable decision fact")
  (with-residents [s residents] "replace/seed the resident directory (map id->resident)"))

;; ----------------------------- demo data -----------------------------

(defn demo-data
  "A small, self-contained resident set so the actor + tests run
  offline."
  []
  {:residents
   {"resident-1" {:id "resident-1" :resident-name "Sato Kenji"
                 :mandatory-reporting-obligation-unresolved? false
                 :background-check-not-cleared? false
                 :care-plan-finalized? false :incident-response-finalized? false
                 :jurisdiction "JPN" :status :intake}
    "resident-2" {:id "resident-2" :resident-name "Atlantis Doe"
                 :mandatory-reporting-obligation-unresolved? false
                 :background-check-not-cleared? false
                 :care-plan-finalized? false :incident-response-finalized? false
                 :jurisdiction "ATL" :status :intake}
    "resident-3" {:id "resident-3" :resident-name "鈴木花子"
                 :mandatory-reporting-obligation-unresolved? true
                 :background-check-not-cleared? false
                 :care-plan-finalized? false :incident-response-finalized? false
                 :jurisdiction "JPN" :status :intake}
    "resident-4" {:id "resident-4" :resident-name "田中一郎"
                 :mandatory-reporting-obligation-unresolved? false
                 :background-check-not-cleared? true
                 :care-plan-finalized? false :incident-response-finalized? false
                 :jurisdiction "JPN" :status :intake}}})

;; ----------------------------- shared commit logic -----------------------------

(defn- finalize-care-plan!
  "Backend-agnostic `:resident/mark-care-plan-finalized` -- looks up
  the resident via the protocol and drafts the care-plan-finalization
  record, and returns {:result .. :resident-patch ..} for the caller
  to persist."
  [s resident-id]
  (let [r (resident s resident-id)
        seq-n (next-careplan-sequence s (:jurisdiction r))
        result (registry/register-care-plan-finalization resident-id (:jurisdiction r) seq-n)]
    {:result result
     :resident-patch {:care-plan-finalized? true
                      :careplan-number (get result "careplan_number")}}))

(defn- finalize-incident-response!
  "Backend-agnostic `:resident/mark-incident-finalized` -- looks up
  the resident via the protocol and drafts the incident-response-
  finalization record, and returns {:result .. :resident-patch ..}
  for the caller to persist."
  [s resident-id]
  (let [r (resident s resident-id)
        seq-n (next-incident-sequence s (:jurisdiction r))
        result (registry/register-incident-response-finalization resident-id (:jurisdiction r) seq-n)]
    {:result result
     :resident-patch {:incident-response-finalized? true
                      :incident-number (get result "incident_number")}}))

;; ----------------------------- MemStore (default) -----------------------------

(defrecord MemStore [a]
  Store
  (resident [_ id] (get-in @a [:residents id]))
  (all-residents [_] (sort-by :id (vals (:residents @a))))
  (safeguarding-of [_ id] (get-in @a [:safeguarding-screens id]))
  (background-check-of [_ id] (get-in @a [:background-checks id]))
  (careplan-of [_ resident-id] (get-in @a [:careplans resident-id]))
  (ledger [_] (:ledger @a))
  (careplan-history [_] (:careplan-finalizations @a))
  (incident-history [_] (:incident-finalizations @a))
  (next-careplan-sequence [_ jurisdiction] (get-in @a [:careplan-sequences jurisdiction] 0))
  (next-incident-sequence [_ jurisdiction] (get-in @a [:incident-sequences jurisdiction] 0))
  (resident-already-care-plan-finalized? [_ resident-id] (boolean (get-in @a [:residents resident-id :care-plan-finalized?])))
  (resident-already-incident-finalized? [_ resident-id] (boolean (get-in @a [:residents resident-id :incident-response-finalized?])))
  (commit-record! [s {:keys [effect path value payload]}]
    (case effect
      :resident/upsert
      (swap! a update-in [:residents (:id value)] merge value)

      :careplan/set
      (swap! a assoc-in [:careplans (first path)] payload)

      :safeguarding/set
      (swap! a assoc-in [:safeguarding-screens (first path)] payload)

      :background-check/set
      (swap! a assoc-in [:background-checks (first path)] payload)

      :resident/mark-care-plan-finalized
      (let [resident-id (first path)
            {:keys [result resident-patch]} (finalize-care-plan! s resident-id)
            jurisdiction (:jurisdiction (resident s resident-id))]
        (swap! a (fn [state]
                   (-> state
                       (update-in [:careplan-sequences jurisdiction] (fnil inc 0))
                       (update-in [:residents resident-id] merge resident-patch)
                       (update :careplan-finalizations registry/append result))))
        result)

      :resident/mark-incident-finalized
      (let [resident-id (first path)
            {:keys [result resident-patch]} (finalize-incident-response! s resident-id)
            jurisdiction (:jurisdiction (resident s resident-id))]
        (swap! a (fn [state]
                   (-> state
                       (update-in [:incident-sequences jurisdiction] (fnil inc 0))
                       (update-in [:residents resident-id] merge resident-patch)
                       (update :incident-finalizations registry/append result))))
        result)
      nil)
    s)
  (append-ledger! [_ fact] (swap! a update :ledger conj fact) fact)
  (with-residents [s residents] (when (seq residents) (swap! a assoc :residents residents)) s))

(defn seed-db
  "A MemStore seeded with the demo resident set. The deterministic
  default."
  []
  (->MemStore (atom (assoc (demo-data)
                           :careplans {} :safeguarding-screens {} :background-checks {} :ledger []
                           :careplan-sequences {} :careplan-finalizations []
                           :incident-sequences {} :incident-finalizations []))))

;; ----------------------------- DatomicStore (langchain.db) -----------------------------

(def ^:private schema
  "DataScript/Datomic-style schema: only constraint attrs are declared.
  Compound values (careplan/safeguarding/background-check payloads,
  ledger facts, care-plan/incident-response records) are stored as
  EDN strings so `langchain.db` doesn't expand them into sub-entities
  -- the same convention every sibling actor's store uses."
  {:resident/id                          {:db/unique :db.unique/identity}
   :careplan/resident-id                  {:db/unique :db.unique/identity}
   :safeguarding/resident-id                {:db/unique :db.unique/identity}
   :background-check/resident-id             {:db/unique :db.unique/identity}
   :ledger/seq                                {:db/unique :db.unique/identity}
   :careplan-finalization/seq                  {:db/unique :db.unique/identity}
   :incident-finalization/seq                   {:db/unique :db.unique/identity}
   :careplan-sequence/jurisdiction                {:db/unique :db.unique/identity}
   :incident-sequence/jurisdiction                 {:db/unique :db.unique/identity}})

(defn- enc [v] (ls/enc v))
(defn- dec* [s] (ls/dec* s))

(defn- resident->tx [{:keys [id resident-name mandatory-reporting-obligation-unresolved?
                            background-check-not-cleared? care-plan-finalized?
                            incident-response-finalized? jurisdiction status
                            careplan-number incident-number]}]
  (cond-> {:resident/id id}
    resident-name                                       (assoc :resident/resident-name resident-name)
    (some? mandatory-reporting-obligation-unresolved?)    (assoc :resident/mandatory-reporting-obligation-unresolved? mandatory-reporting-obligation-unresolved?)
    (some? background-check-not-cleared?)                  (assoc :resident/background-check-not-cleared? background-check-not-cleared?)
    (some? care-plan-finalized?)                             (assoc :resident/care-plan-finalized? care-plan-finalized?)
    (some? incident-response-finalized?)                      (assoc :resident/incident-response-finalized? incident-response-finalized?)
    jurisdiction                                                (assoc :resident/jurisdiction jurisdiction)
    status                                                        (assoc :resident/status status)
    careplan-number                                               (assoc :resident/careplan-number careplan-number)
    incident-number                                                (assoc :resident/incident-number incident-number)))

(def ^:private resident-pull
  [:resident/id :resident/resident-name :resident/mandatory-reporting-obligation-unresolved?
   :resident/background-check-not-cleared? :resident/care-plan-finalized?
   :resident/incident-response-finalized? :resident/jurisdiction :resident/status
   :resident/careplan-number :resident/incident-number])

(defn- pull->resident [m]
  (when (:resident/id m)
    {:id (:resident/id m) :resident-name (:resident/resident-name m)
     :mandatory-reporting-obligation-unresolved? (boolean (:resident/mandatory-reporting-obligation-unresolved? m))
     :background-check-not-cleared? (boolean (:resident/background-check-not-cleared? m))
     :care-plan-finalized? (boolean (:resident/care-plan-finalized? m))
     :incident-response-finalized? (boolean (:resident/incident-response-finalized? m))
     :jurisdiction (:resident/jurisdiction m) :status (:resident/status m)
     :careplan-number (:resident/careplan-number m) :incident-number (:resident/incident-number m)}))

(defrecord DatomicStore [conn]
  Store
  (resident [_ id]
    (pull->resident (d/pull (d/db conn) resident-pull [:resident/id id])))
  (all-residents [_]
    (->> (d/q '[:find [?id ...] :where [?e :resident/id ?id]] (d/db conn))
         (map #(pull->resident (d/pull (d/db conn) resident-pull [:resident/id %])))
         (sort-by :id)))
  (safeguarding-of [_ id]
    (dec* (d/q '[:find ?p . :in $ ?rid
                :where [?k :safeguarding/resident-id ?rid] [?k :safeguarding/payload ?p]]
              (d/db conn) id)))
  (background-check-of [_ id]
    (dec* (d/q '[:find ?p . :in $ ?rid
                :where [?k :background-check/resident-id ?rid] [?k :background-check/payload ?p]]
              (d/db conn) id)))
  (careplan-of [_ resident-id]
    (dec* (d/q '[:find ?p . :in $ ?rid
                :where [?a :careplan/resident-id ?rid] [?a :careplan/payload ?p]]
              (d/db conn) resident-id)))
  (ledger [_]
    (->> (d/q '[:find ?s ?f :where [?e :ledger/seq ?s] [?e :ledger/fact ?f]] (d/db conn))
         (sort-by first)
         (mapv (comp dec* second))))
  (careplan-history [_]
    (->> (d/q '[:find ?s ?r :where [?e :careplan-finalization/seq ?s] [?e :careplan-finalization/record ?r]] (d/db conn))
         (sort-by first)
         (mapv (comp dec* second))))
  (incident-history [_]
    (->> (d/q '[:find ?s ?r :where [?e :incident-finalization/seq ?s] [?e :incident-finalization/record ?r]] (d/db conn))
         (sort-by first)
         (mapv (comp dec* second))))
  (next-careplan-sequence [_ jurisdiction]
    (or (d/q '[:find ?n . :in $ ?j
              :where [?e :careplan-sequence/jurisdiction ?j] [?e :careplan-sequence/next ?n]]
            (d/db conn) jurisdiction)
        0))
  (next-incident-sequence [_ jurisdiction]
    (or (d/q '[:find ?n . :in $ ?j
              :where [?e :incident-sequence/jurisdiction ?j] [?e :incident-sequence/next ?n]]
            (d/db conn) jurisdiction)
        0))
  (resident-already-care-plan-finalized? [s resident-id]
    (boolean (:care-plan-finalized? (resident s resident-id))))
  (resident-already-incident-finalized? [s resident-id]
    (boolean (:incident-response-finalized? (resident s resident-id))))
  (commit-record! [s {:keys [effect path value payload]}]
    (case effect
      :resident/upsert
      (d/transact! conn [(resident->tx value)])

      :careplan/set
      (d/transact! conn [{:careplan/resident-id (first path) :careplan/payload (enc payload)}])

      :safeguarding/set
      (d/transact! conn [{:safeguarding/resident-id (first path) :safeguarding/payload (enc payload)}])

      :background-check/set
      (d/transact! conn [{:background-check/resident-id (first path) :background-check/payload (enc payload)}])

      :resident/mark-care-plan-finalized
      (let [resident-id (first path)
            {:keys [result resident-patch]} (finalize-care-plan! s resident-id)
            jurisdiction (:jurisdiction (resident s resident-id))
            next-n (inc (next-careplan-sequence s jurisdiction))]
        (d/transact! conn
                     [(resident->tx (assoc resident-patch :id resident-id))
                      {:careplan-sequence/jurisdiction jurisdiction :careplan-sequence/next next-n}
                      {:careplan-finalization/seq (count (careplan-history s)) :careplan-finalization/record (enc (get result "record"))}])
        result)

      :resident/mark-incident-finalized
      (let [resident-id (first path)
            {:keys [result resident-patch]} (finalize-incident-response! s resident-id)
            jurisdiction (:jurisdiction (resident s resident-id))
            next-n (inc (next-incident-sequence s jurisdiction))]
        (d/transact! conn
                     [(resident->tx (assoc resident-patch :id resident-id))
                      {:incident-sequence/jurisdiction jurisdiction :incident-sequence/next next-n}
                      {:incident-finalization/seq (count (incident-history s)) :incident-finalization/record (enc (get result "record"))}])
        result)
      nil)
    s)
  (append-ledger! [s fact]
    (d/transact! conn [{:ledger/seq (count (ledger s)) :ledger/fact (enc fact)}])
    fact)
  (with-residents [s residents]
    (when (seq residents) (d/transact! conn (mapv resident->tx (vals residents)))) s))

(defn datomic-store
  "A DatomicStore (langchain.db backend) seeded from `data`
  ({:residents ..}); empty when omitted."
  ([] (datomic-store {}))
  ([{:keys [residents]}]
   (let [s (->DatomicStore (d/create-conn schema))]
     (with-residents s residents))))

(defn datomic-seed-db
  "A DatomicStore seeded with the demo resident set -- the Datomic-
  backed analog of `seed-db`, used to prove protocol parity."
  []
  (datomic-store (demo-data)))
