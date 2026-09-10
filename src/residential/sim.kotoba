(ns residential.sim
  "Demo driver -- `clojure -M:dev:run`. Walks a clean resident through
  intake -> care-plan verification -> safeguarding screening ->
  background-check screening -> care-plan-finalization proposal
  (always escalates) -> human approval -> commit, then through
  incident-response-finalization proposal (always escalates) -> human
  approval -> commit, then shows four HARD holds (a jurisdiction with
  no spec-basis, a resident whose own recorded mandatory-reporting
  obligation has NOT been resolved [screened directly via
  `:safeguarding/screen` -- never via an actuation op against an
  unscreened resident -- see this actor's own governor ns docstring /
  the lesson `parksafety`'s ADR-2607071922 Decision 5, `eldercare`'s,
  `museum`'s, `conservation`'s, `salon`'s, `entertainment`'s,
  `casework`'s, `hospital`'s, `facility`'s, `school`'s, `association`'s,
  `leasing`'s, `behavioral`'s, `secondary`'s, `card`'s, `water`'s,
  `telecom`'s, `aerospace`'s, `recovery`'s, `consulting`'s, `union`'s,
  `congregation`'s, `fab`'s, `energy`'s, `care`'s, `navigator`'s,
  `learning`'s, `banking`'s, `advertising`'s, `polling`'s, `research`'s,
  `design`'s, `nursing`'s, `sports`'s, `alliedhealth`'s, `laundry`'s,
  `holdco`'s, `photo`'s, `personalservice`'s, `edsupport`'s and
  `headoffice`'s ADR-0001s already recorded], an uncleared background
  check screened directly, and a double care-plan-finalization/
  incident-response-finalization of an already-processed resident)
  that never reach a human at all, and prints the audit ledger + the
  draft care-plan-finalization and incident-response-finalization
  records."
  (:require [langgraph.graph :as g]
            [residential.store :as store]
            [residential.operation :as op]))

(def operator {:actor-id "op-1" :actor-role :licensed-care-staff :phase 3})

(defn- exec! [actor tid request context]
  (g/run* actor {:request request :context context} {:thread-id tid}))

(defn- approve! [actor tid]
  (g/run* actor {:approval {:status :approved :by "op-1"}} {:thread-id tid :resume? true}))

(defn -main [& _]
  (let [db (store/seed-db)
        actor (op/build db)]
    (println "== resident/intake resident-1 (JPN, clean; safeguarding resolved, background check cleared) ==")
    (println (exec! actor "t1" {:op :resident/intake :subject "resident-1"
                                :patch {:id "resident-1" :resident-name "Sato Kenji"}} operator))

    (println "== careplan/verify resident-1 (escalates -- human approves) ==")
    (println (exec! actor "t2" {:op :careplan/verify :subject "resident-1"} operator))
    (println (approve! actor "t2"))

    (println "== safeguarding/screen resident-1 (clean; escalates -- human approves) ==")
    (println (exec! actor "t3" {:op :safeguarding/screen :subject "resident-1"} operator))
    (println (approve! actor "t3"))

    (println "== background-check/screen resident-1 (clean; escalates -- human approves) ==")
    (println (exec! actor "t4" {:op :background-check/screen :subject "resident-1"} operator))
    (println (approve! actor "t4"))

    (println "== actuation/finalize-care-plan resident-1 (always escalates -- actuation/finalize-care-plan) ==")
    (let [r (exec! actor "t5" {:op :actuation/finalize-care-plan :subject "resident-1"} operator)]
      (println r)
      (println "-- human licensed care staff approves --")
      (println (approve! actor "t5")))

    (println "== actuation/finalize-incident-response resident-1 (always escalates -- actuation/finalize-incident-response) ==")
    (let [r (exec! actor "t6" {:op :actuation/finalize-incident-response :subject "resident-1"} operator)]
      (println r)
      (println "-- human licensed care staff approves --")
      (println (approve! actor "t6")))

    (println "== careplan/verify resident-2 (no spec-basis -> HARD hold) ==")
    (println (exec! actor "t7" {:op :careplan/verify :subject "resident-2" :no-spec? true} operator))

    (println "== safeguarding/screen resident-3 (unresolved -> HARD hold, never reaches a human) ==")
    (println (exec! actor "t8" {:op :safeguarding/screen :subject "resident-3"} operator))

    (println "== background-check/screen resident-4 (not cleared -> HARD hold, never reaches a human) ==")
    (println (exec! actor "t9" {:op :background-check/screen :subject "resident-4"} operator))

    (println "== actuation/finalize-care-plan resident-1 AGAIN (double-finalization -> HARD hold) ==")
    (println (exec! actor "t10" {:op :actuation/finalize-care-plan :subject "resident-1"} operator))

    (println "== actuation/finalize-incident-response resident-1 AGAIN (double-finalization -> HARD hold) ==")
    (println (exec! actor "t11" {:op :actuation/finalize-incident-response :subject "resident-1"} operator))

    (println "== audit ledger ==")
    (doseq [f (store/ledger db)] (println f))

    (println "== draft care-plan-finalization records ==")
    (doseq [r (store/careplan-history db)] (println r))

    (println "== draft incident-response-finalization records ==")
    (doseq [r (store/incident-history db)] (println r))))
