(ns residential.residentialadvisor
  "ResidentialOps-LLM client -- the *contained intelligence node* for
  the residential-care actor.

  It normalizes resident intake, drafts a per-jurisdiction
  residential-care/safeguarding evidence checklist, screens residents
  for an unresolved mandatory-reporting obligation and for an
  uncleared background check, drafts the care-plan-finalization
  action, and drafts the incident-response-finalization action.
  CRITICAL: it is a smart-but-untrusted advisor. It returns a
  *proposal* (with a rationale + the fields it cited), never a
  committed record or a real care-plan/incident-response
  finalization. Every output is censored downstream by `residential.
  governor` before anything touches the SSoT, and `:actuation/
  finalize-care-plan`/`:actuation/finalize-incident-response`
  proposals NEVER auto-commit at any phase -- see README `Actuation`.

  Like every sibling actor's advisor, this is a deterministic mock so
  the actor graph runs offline and the governor contract is exercised
  end-to-end. In production this calls a real LLM (kotoba-llm or
  equivalent) with the same proposal shape.

  Proposal shape (all kinds):
    {:summary    str            ; human-facing draft / finding
     :rationale  str            ; why -- SCANNED by the spec-basis gate
     :cites      [kw|str ..]    ; facts/sources the LLM used -- SCANNED too
     :effect     kw             ; how a commit would mutate the SSoT
     :stake      kw|nil         ; :actuation/finalize-care-plan | :actuation/finalize-incident-response | nil
     :confidence 0..1}"
  (:require #?(:clj  [clojure.edn :as edn]
               :cljs [cljs.reader :as edn])
            [kotoba.lang.text :as str]
            [residential.facts :as facts]
            [residential.store :as store]
            [langchain.model :as model]))

(defn- normalize-intake
  "Directory upsert -- the LLM only normalizes/validates the patch; it
  does not invent the resident or jurisdiction. High confidence, low
  stakes."
  [_db {:keys [patch]}]
  {:summary    (str "入所者記録更新: " (pr-str (keys patch)))
   :rationale  "入力 patch の正規化のみ。新規事実の生成なし。"
   :cites      (vec (keys patch))
   :effect     :resident/upsert
   :value      patch
   :stake      nil
   :confidence 0.97})

(defn- verify-careplan
  "Per-jurisdiction residential-care/safeguarding evidence checklist
  draft. `:no-spec?` injects the failure mode we must defend against:
  proposing a checklist for a jurisdiction with NO official spec-basis
  in `residential.facts` -- the Residential Care Governor must reject
  this (never invent a jurisdiction's requirements)."
  [db {:keys [subject no-spec?]}]
  (let [r (store/resident db subject)
        iso3 (if no-spec? "ATL" (:jurisdiction r))
        sb (facts/spec-basis iso3)]
    (if (nil? sb)
      {:summary    (str iso3 " の公式spec-basisが見つかりません")
       :rationale  "residential.facts に未登録の法域。要件を推測で作らない。"
       :cites      []
       :effect     :careplan/set
       :value      {:jurisdiction iso3 :checklist [] :spec-basis nil}
       :stake      nil
       :confidence 0.9}
      {:summary    (str iso3 " (" (:owner-authority sb) ") 向け必要書類 "
                        (count (:required-evidence sb)) " 件を提案")
       :rationale  (str "公式ソース: " (:provenance sb) " / 法的根拠: " (:legal-basis sb))
       :cites      [(:legal-basis sb) (:provenance sb)]
       :effect     :careplan/set
       :value      {:jurisdiction iso3
                    :checklist (:required-evidence sb)
                    :spec-basis (:provenance sb)
                    :legal-basis (:legal-basis sb)}
       :stake      nil
       :confidence 0.9})))

(defn- screen-safeguarding
  "Mandatory-reporting-obligation screening draft.
  `:mandatory-reporting-obligation-unresolved?` on the resident record
  injects the failure mode: the Residential Care Governor must HOLD,
  un-overridably, on any unresolved obligation."
  [db {:keys [subject]}]
  (let [r (store/resident db subject)]
    (cond
      (nil? r)
      {:summary "対象入所者記録が見つかりません" :rationale "no resident record"
       :cites [] :effect :safeguarding/set :value {:resident-id subject :mandatory-reporting-obligation-unresolved? nil}
       :stake nil :confidence 0.0}

      (true? (:mandatory-reporting-obligation-unresolved? r))
      {:summary    (str (:resident-name r) ": 義務的通告事項が未解決")
       :rationale  "スクリーニングが未解決状態を検出。人手確認とホールドが必須。"
       :cites      [:safeguarding-check]
       :effect     :safeguarding/set
       :value      {:resident-id subject :mandatory-reporting-obligation-unresolved? true}
       :stake      nil
       :confidence 0.95}

      :else
      {:summary    (str (:resident-name r) ": 義務的通告事項は解決済み")
       :rationale  "セーフガーディング・スクリーニング完了。"
       :cites      [:safeguarding-check]
       :effect     :safeguarding/set
       :value      {:resident-id subject :mandatory-reporting-obligation-unresolved? false}
       :stake      nil
       :confidence 0.9})))

(defn- screen-background-check
  "Resident-facing background-check screening draft -- the SAME
  literal concept `school.schooladvisor`'s advisor established first,
  and `sports.sportsadvisor`/`personalservice.personalserviceadvisor`/
  `edsupport.edsupportadvisor` already reused literally (see
  `residential.governor`'s ns docstring for the full ordinal
  accounting). `:background-check-not-cleared?` on the resident
  record injects the failure mode the Residential Care Governor must
  HOLD, un-overridably, on."
  [db {:keys [subject]}]
  (let [r (store/resident db subject)]
    (cond
      (nil? r)
      {:summary "対象入所者記録が見つかりません" :rationale "no resident record"
       :cites [] :effect :background-check/set :value {:resident-id subject :verdict :unknown}
       :stake nil :confidence 0.0}

      (true? (:background-check-not-cleared? r))
      {:summary    (str (:resident-name r) ": 身元確認が未完了")
       :rationale  "スクリーニングが身元確認未完了を検出。人手確認とホールドが必須。"
       :cites      [:background-check]
       :effect     :background-check/set
       :value      {:resident-id subject :verdict :not-cleared}
       :stake      nil
       :confidence 0.95}

      :else
      {:summary    (str (:resident-name r) ": 身元確認は完了")
       :rationale  "身元確認スクリーニング完了。"
       :cites      [:background-check]
       :effect     :background-check/set
       :value      {:resident-id subject :verdict :cleared}
       :stake      nil
       :confidence 0.9})))

(defn- propose-care-plan-finalization
  "Draft the actual CARE-PLAN-FINALIZATION action -- finalizing a
  real care plan for a resident. ALWAYS `:stake :actuation/finalize-
  care-plan` -- this is a REAL-WORLD act, never a draft the actor may
  auto-run. See README `Actuation`: no phase ever adds this op to a
  phase's `:auto` set (`residential.phase`); the governor also always
  escalates on `:actuation/finalize-care-plan`. Two independent
  layers agree, deliberately."
  [db {:keys [subject]}]
  (let [r (store/resident db subject)
        safe? (and r (not (:mandatory-reporting-obligation-unresolved? r))
                   (not (:background-check-not-cleared? r)))]
    {:summary    (str subject " 向けケアプラン確定提案"
                      (when r (str " (resident=" (:resident-name r) ")")))
     :rationale  (if r
                   (str "mandatory-reporting-obligation-unresolved?=" (:mandatory-reporting-obligation-unresolved? r)
                        " background-check-not-cleared?=" (:background-check-not-cleared? r))
                   "入所者記録が見つかりません")
     :cites      (if r [subject] [])
     :effect     :resident/mark-care-plan-finalized
     :value      {:resident-id subject}
     :stake      :actuation/finalize-care-plan
     :confidence (if safe? 0.9 0.3)}))

(defn- propose-incident-response-finalization
  "Draft the actual INCIDENT-RESPONSE-FINALIZATION action -- finalizing
  a real incident response for a resident. ALWAYS `:stake :actuation/
  finalize-incident-response` -- this is a REAL-WORLD act, never a
  draft the actor may auto-run. See README `Actuation`: no phase ever
  adds this op to a phase's `:auto` set (`residential.phase`); the
  governor also always escalates on `:actuation/finalize-incident-
  response`. Two independent layers agree, deliberately."
  [db {:keys [subject]}]
  (let [r (store/resident db subject)
        safe? (and r (not (:mandatory-reporting-obligation-unresolved? r))
                   (not (:background-check-not-cleared? r)))]
    {:summary    (str subject " 向け事故対応確定提案"
                      (when r (str " (resident=" (:resident-name r) ")")))
     :rationale  (if r
                   (str "mandatory-reporting-obligation-unresolved?=" (:mandatory-reporting-obligation-unresolved? r)
                        " background-check-not-cleared?=" (:background-check-not-cleared? r))
                   "入所者記録が見つかりません")
     :cites      (if r [subject] [])
     :effect     :resident/mark-incident-finalized
     :value      {:resident-id subject}
     :stake      :actuation/finalize-incident-response
     :confidence (if safe? 0.9 0.3)}))

(defn infer
  "Route a request to the right proposal generator.
  request: {:op kw :subject id ...op-specific...}"
  [db {:keys [op] :as request}]
  (case op
    :resident/intake                          (normalize-intake db request)
    :careplan/verify                          (verify-careplan db request)
    :safeguarding/screen                      (screen-safeguarding db request)
    :background-check/screen                  (screen-background-check db request)
    :actuation/finalize-care-plan              (propose-care-plan-finalization db request)
    :actuation/finalize-incident-response     (propose-incident-response-finalization db request)
    {:summary "未対応の操作" :rationale (str op) :cites []
     :effect :noop :stake nil :confidence 0.0}))

;; ----------------------------- Advisor protocol -----------------------------

(defprotocol Advisor
  (-advise [advisor store request] "store + request -> proposal map"))

(defn mock-advisor
  "The deterministic advisor (the `infer` logic above). Default everywhere."
  [] (reify Advisor (-advise [_ st req] (infer st req))))

(def ^:private system-prompt
  (str "あなたは居住型ケア事業(児童福祉施設等)のケアプラン・事故対応確定"
       "エージェントの助言者です。与えられた事実のみに基づき、提案を1つだけ"
       "EDNマップで返します。説明や前置きは一切書かず、EDNだけを出力します。\n"
       "キー: :summary(人向けドラフト) :rationale(根拠/必ず事実から) "
       ":cites(使った事実キーのベクタ) "
       ":effect(:resident/upsert|:careplan/set|:safeguarding/set|"
       ":background-check/set|:resident/mark-care-plan-finalized|"
       ":resident/mark-incident-finalized) "
       ":stake(:actuation/finalize-care-plan か :actuation/finalize-incident-response か nil) "
       ":confidence(0..1)。\n"
       "重要: 登録されていない法域の要件を絶対に創作してはいけません。"
       "spec-basisが無い場合は :cites を空にし confidence を上げないこと。"))

(defn- facts-for [st {:keys [op subject]}]
  (case op
    :careplan/verify                          {:resident (store/resident st subject)}
    :safeguarding/screen                      {:resident (store/resident st subject)}
    :background-check/screen                  {:resident (store/resident st subject)}
    :actuation/finalize-care-plan               {:resident (store/resident st subject)}
    :actuation/finalize-incident-response      {:resident (store/resident st subject)}
    {:resident (store/resident st subject)}))

(defn- parse-proposal
  "Parse the model's EDN proposal defensively. Any parse/shape failure
  yields a safe low-confidence noop so the Residential Care Governor
  escalates/holds -- an LLM hiccup can never auto-finalize a care plan
  or incident response."
  [content]
  (let [p (try (edn/read-string (str/trim (str content)))
               (catch #?(:clj Exception :cljs :default) _ nil))]
    (if (map? p)
      (-> p
          (update :cites #(vec (or % [])))
          (update :confidence #(if (number? %) (double %) 0.0))
          (update :effect #(or % :noop)))
      {:summary "LLM応答を解釈できませんでした" :rationale (str content)
       :cites [] :effect :noop :stake nil :confidence 0.0})))

(defn llm-advisor
  "An advisor backed by a `langchain.model/ChatModel` (real inference)."
  ([chat-model] (llm-advisor chat-model {}))
  ([chat-model gen-opts]
   (reify Advisor
     (-advise [_ st req]
       (let [msgs [{:role :system :content system-prompt}
                   {:role :user :content (str "操作: " (:op req)
                                              "\n対象: " (:subject req)
                                              "\n事実: " (pr-str (facts-for st req)))}]
             resp (model/-generate chat-model msgs gen-opts)]
         (parse-proposal (:content resp)))))))

(defn trace
  "Decision-grounded audit record -- persisted to the :audit channel."
  [request proposal]
  {:t          :residentialadvisor-proposal
   :op         (:op request)
   :subject    (:subject request)
   :summary    (:summary proposal)
   :rationale  (:rationale proposal)
   :cites      (:cites proposal)
   :confidence (:confidence proposal)})
