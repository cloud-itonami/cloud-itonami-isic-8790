(ns residential.registry-test
  (:require [clojure.test :refer [deftest is]]
            [residential.registry :as r]))

;; ----------------------------- register-care-plan-finalization -----------------------------

(deftest careplan-finalization-is-a-draft-not-a-real-finalization
  (let [result (r/register-care-plan-finalization "resident-1" "JPN" 0)]
    (is (nil? (get-in result ["certificate" "proof"])))
    (is (= (get-in result ["certificate" "issued_by_registry"]) false))
    (is (= (get-in result ["certificate" "status"]) "draft-unsigned"))))

(deftest careplan-finalization-assigns-careplan-number
  (let [result (r/register-care-plan-finalization "resident-1" "JPN" 7)]
    (is (= (get result "careplan_number") "JPN-CP-000007"))
    (is (= (get-in result ["record" "resident_id"]) "resident-1"))
    (is (= (get-in result ["record" "kind"]) "care-plan-finalization-draft"))
    (is (= (get-in result ["record" "immutable"]) true))))

(deftest careplan-finalization-validation-rules
  (is (thrown? Exception (r/register-care-plan-finalization "" "JPN" 0)))
  (is (thrown? Exception (r/register-care-plan-finalization "resident-1" "" 0)))
  (is (thrown? Exception (r/register-care-plan-finalization "resident-1" "JPN" -1))))

;; ----------------------------- register-incident-response-finalization -----------------------------

(deftest incident-finalization-is-a-draft-not-a-real-finalization
  (let [result (r/register-incident-response-finalization "resident-1" "JPN" 0)]
    (is (nil? (get-in result ["certificate" "proof"])))
    (is (= (get-in result ["certificate" "issued_by_registry"]) false))
    (is (= (get-in result ["certificate" "status"]) "draft-unsigned"))))

(deftest incident-finalization-assigns-incident-number
  (let [result (r/register-incident-response-finalization "resident-1" "JPN" 7)]
    (is (= (get result "incident_number") "JPN-INC-000007"))
    (is (= (get-in result ["record" "resident_id"]) "resident-1"))
    (is (= (get-in result ["record" "kind"]) "incident-response-finalization-draft"))
    (is (= (get-in result ["record" "immutable"]) true))))

(deftest incident-finalization-validation-rules
  (is (thrown? Exception (r/register-incident-response-finalization "" "JPN" 0)))
  (is (thrown? Exception (r/register-incident-response-finalization "resident-1" "" 0)))
  (is (thrown? Exception (r/register-incident-response-finalization "resident-1" "JPN" -1))))

(deftest history-is-append-only
  (let [c1 (r/register-care-plan-finalization "resident-1" "JPN" 0)
        hist (r/append [] c1)
        c2 (r/register-care-plan-finalization "resident-2" "JPN" 1)
        hist2 (r/append hist c2)]
    (is (= 2 (count hist2)))
    (is (= "JPN-CP-000000" (get-in hist2 [0 "record_id"])))
    (is (= "JPN-CP-000001" (get-in hist2 [1 "record_id"])))))
