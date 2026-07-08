(ns residential.store-contract-test
  "The Store contract, run against BOTH backends. Proving MemStore and
  the Datomic-backed (langchain.db) store satisfy the same contract is
  what makes 'swap the SSoT for Datomic / kotoba-server' a
  configuration change, not a rewrite -- see `cloud-itonami-isic-6511`'s
  `underwriting.store-contract-test` for the same pattern on the
  sibling actor."
  (:require [clojure.test :refer [deftest is testing]]
            [residential.store :as store]))

(defn- backends []
  [["MemStore" (store/seed-db)] ["DatomicStore" (store/datomic-seed-db)]])

(deftest read-parity
  (doseq [[label s] (backends)]
    (testing label
      (is (= "Sato Kenji" (:resident-name (store/resident s "resident-1"))))
      (is (= "JPN" (:jurisdiction (store/resident s "resident-1"))))
      (is (false? (:mandatory-reporting-obligation-unresolved? (store/resident s "resident-1"))))
      (is (false? (:background-check-not-cleared? (store/resident s "resident-1"))))
      (is (true? (:mandatory-reporting-obligation-unresolved? (store/resident s "resident-3"))))
      (is (true? (:background-check-not-cleared? (store/resident s "resident-4"))))
      (is (false? (:care-plan-finalized? (store/resident s "resident-1"))))
      (is (false? (:incident-response-finalized? (store/resident s "resident-1"))))
      (is (= ["resident-1" "resident-2" "resident-3" "resident-4"]
             (mapv :id (store/all-residents s))))
      (is (nil? (store/safeguarding-of s "resident-1")))
      (is (nil? (store/background-check-of s "resident-1")))
      (is (nil? (store/careplan-of s "resident-1")))
      (is (= [] (store/ledger s)))
      (is (= [] (store/careplan-history s)))
      (is (= [] (store/incident-history s)))
      (is (zero? (store/next-careplan-sequence s "JPN")))
      (is (zero? (store/next-incident-sequence s "JPN")))
      (is (false? (store/resident-already-care-plan-finalized? s "resident-1")))
      (is (false? (store/resident-already-incident-finalized? s "resident-1"))))))

(deftest write-and-ledger-parity
  (doseq [[label s] (backends)]
    (testing label
      (testing "partial upsert merges, preserving untouched fields"
        (store/commit-record! s {:effect :resident/upsert
                                 :value {:id "resident-1" :resident-name "Sato Kenji"}})
        (is (= "Sato Kenji" (:resident-name (store/resident s "resident-1"))))
        (is (false? (:background-check-not-cleared? (store/resident s "resident-1"))) "unrelated field preserved"))
      (testing "careplan / safeguarding / background-check payloads commit and read back"
        (store/commit-record! s {:effect :careplan/set :path ["resident-1"]
                                 :payload {:jurisdiction "JPN" :checklist ["a" "b"]}})
        (is (= {:jurisdiction "JPN" :checklist ["a" "b"]} (store/careplan-of s "resident-1")))
        (store/commit-record! s {:effect :safeguarding/set :path ["resident-1"]
                                 :payload {:resident-id "resident-1" :mandatory-reporting-obligation-unresolved? false}})
        (is (= {:resident-id "resident-1" :mandatory-reporting-obligation-unresolved? false} (store/safeguarding-of s "resident-1")))
        (store/commit-record! s {:effect :background-check/set :path ["resident-1"]
                                 :payload {:resident-id "resident-1" :verdict :cleared}})
        (is (= {:resident-id "resident-1" :verdict :cleared} (store/background-check-of s "resident-1"))))
      (testing "care-plan finalization drafts a record and advances the sequence"
        (store/commit-record! s {:effect :resident/mark-care-plan-finalized :path ["resident-1"]})
        (is (= "JPN-CP-000000" (get (first (store/careplan-history s)) "record_id")))
        (is (= "care-plan-finalization-draft" (get (first (store/careplan-history s)) "kind")))
        (is (true? (:care-plan-finalized? (store/resident s "resident-1"))))
        (is (= 1 (count (store/careplan-history s))))
        (is (= 1 (store/next-careplan-sequence s "JPN")))
        (is (true? (store/resident-already-care-plan-finalized? s "resident-1")))
        (is (false? (store/resident-already-care-plan-finalized? s "resident-2"))))
      (testing "incident-response finalization drafts a record and advances the sequence"
        (store/commit-record! s {:effect :resident/mark-incident-finalized :path ["resident-1"]})
        (is (= "JPN-INC-000000" (get (first (store/incident-history s)) "record_id")))
        (is (= "incident-response-finalization-draft" (get (first (store/incident-history s)) "kind")))
        (is (true? (:incident-response-finalized? (store/resident s "resident-1"))))
        (is (= 1 (count (store/incident-history s))))
        (is (= 1 (store/next-incident-sequence s "JPN")))
        (is (true? (store/resident-already-incident-finalized? s "resident-1")))
        (is (false? (store/resident-already-incident-finalized? s "resident-2"))))
      (testing "ledger is append-only and order-preserving"
        (store/append-ledger! s {:op :a :disposition :commit})
        (store/append-ledger! s {:op :b :disposition :hold})
        (is (= [:commit :hold] (mapv :disposition (store/ledger s))))))))

(deftest datomic-empty-store-is-usable
  (let [s (store/datomic-store)]
    (is (nil? (store/resident s "nope")))
    (is (= [] (store/all-residents s)))
    (is (= [] (store/ledger s)))
    (is (= [] (store/careplan-history s)))
    (is (= [] (store/incident-history s)))
    (is (zero? (store/next-careplan-sequence s "JPN")))
    (is (zero? (store/next-incident-sequence s "JPN")))
    (store/with-residents s {"x" {:id "x" :resident-name "n"
                                  :mandatory-reporting-obligation-unresolved? false
                                  :background-check-not-cleared? false
                                  :care-plan-finalized? false :incident-response-finalized? false
                                  :jurisdiction "JPN" :status :intake}})
    (is (= "n" (:resident-name (store/resident s "x"))))))
