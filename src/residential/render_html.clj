(ns residential.render-html
  "Build-time HTML renderer for `docs/samples/operator-console.html`.

  Closes flagship checklist item 2 (com-junkawasaki/root ADR-2607189300):
  this repo previously had NO demo page and no generator at all. This
  namespace drives the REAL actor stack (`residential.operation` ->
  `residential.governor` -> `residential.store`) through the scenario
  this repo's own `residential.sim` demo driver (`clojure -M:dev:run`)
  already exercises -- confirmed BEFORE writing this file to produce a
  sensible ledger against the real seeded resident ids
  `resident-1`..`resident-4` from `residential.store/demo-data`.

  Nothing on the page is hand-written domain content. Every resident,
  jurisdiction, spec-basis citation, care-plan/incident number, HARD
  hold rule and ledger row is read back out of the store after a real
  graph run. The action-gate and rollout-phase tables are DERIVED by
  calling `residential.phase/gate` and `residential.governor/high-stakes`
  rather than being transcribed into a literal table, so the page
  cannot drift away from the code it documents.

  Deterministic: no timestamps, no random ids, byte-identical across
  reruns against the same seed (verify by diffing two consecutive runs
  into scratch dirs).

  Usage: `clojure -M:dev:render-html [out-file]`
  (default `docs/samples/operator-console.html`)."
  (:require [jp-go-dds.skin]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [residential.store :as store]
            [residential.facts :as facts]
            [residential.phase :as phase]
            [residential.governor :as governor]
            [residential.operation :as op]
            [langgraph.graph :as g]))

(def ^:private operator
  "The same operator context this repo's own `residential.sim` uses."
  {:actor-id "op-1" :actor-role :licensed-care-staff :phase 3})

(defn- exec! [actor tid request]
  (g/run* actor {:request request :context operator} {:thread-id tid}))

(defn- approve! [actor tid]
  (g/run* actor {:approval {:status :approved :by "op-1"}}
          {:thread-id tid :resume? true}))

(defn run-demo!
  "Runs a fresh seeded store through the scenario `residential.sim`
  drives, collecting BOTH the store and the per-run `:audit` channel.

  resident-1 (JPN, safeguarding resolved, background check cleared)
  clears a full lifecycle -- intake (auto-commits at phase 3, the only
  auto-eligible op), a care-plan verification against the real JPN
  spec-basis (phase-gated -> approved), a mandatory-reporting-obligation
  screening (approved), a background-check screening (approved), a
  care-plan finalization (ALWAYS escalates -- `:actuation/finalize-care-
  plan` is permanently high-stakes, never auto at any phase -> approved)
  and an incident-response finalization (ALWAYS escalates, same posture
  -> approved). Then five HARD holds that never reach a human at all:
  resident-2 has no official spec-basis for its (deliberately
  unregistered) ATL jurisdiction; resident-3's own record carries an
  unresolved mandatory-reporting obligation; resident-4's own record
  carries an uncleared background check; and resident-1 is re-submitted
  for BOTH a second care-plan finalization and a second incident-
  response finalization, each refused by its own double-finalization
  guard.

  The `:audit` channel is collected because `approval-granted` facts
  (which carry the approver id) are produced by the `:request-approval`
  node but are NEVER appended to the store ledger by the `:commit`
  node -- measured, not assumed. See `attribution-rows`.

  Returns {:db store :audit [fact ..]}."
  []
  (let [db (store/seed-db)
        actor (op/build db)
        audit (atom [])
        collect! (fn [r] (swap! audit into (get-in r [:state :audit])) r)]

    (collect! (exec! actor "t1" {:op :resident/intake :subject "resident-1"
                                 :patch {:id "resident-1" :resident-name "Sato Kenji"}}))

    (collect! (exec! actor "t2" {:op :careplan/verify :subject "resident-1"}))
    (collect! (approve! actor "t2"))

    (collect! (exec! actor "t3" {:op :safeguarding/screen :subject "resident-1"}))
    (collect! (approve! actor "t3"))

    (collect! (exec! actor "t4" {:op :background-check/screen :subject "resident-1"}))
    (collect! (approve! actor "t4"))

    (collect! (exec! actor "t5" {:op :actuation/finalize-care-plan :subject "resident-1"}))
    (collect! (approve! actor "t5"))

    (collect! (exec! actor "t6" {:op :actuation/finalize-incident-response :subject "resident-1"}))
    (collect! (approve! actor "t6"))

    ;; --- five HARD holds; none of these ever reaches a human ---
    (collect! (exec! actor "t7" {:op :careplan/verify :subject "resident-2" :no-spec? true}))
    (collect! (exec! actor "t8" {:op :safeguarding/screen :subject "resident-3"}))
    (collect! (exec! actor "t9" {:op :background-check/screen :subject "resident-4"}))
    (collect! (exec! actor "t10" {:op :actuation/finalize-care-plan :subject "resident-1"}))
    (collect! (exec! actor "t11" {:op :actuation/finalize-incident-response :subject "resident-1"}))

    {:db db :audit @audit}))

;; ----------------------------- rendering helpers -----------------------------

(defn- esc [v]
  (-> (str v)
      (str/replace "&" "&amp;")
      (str/replace "<" "&lt;")
      (str/replace ">" "&gt;")))

(defn- kw-str [k] (if (keyword? k) (subs (str k) 1) (str k)))

(defn- yes-no [b] (if b "<span class=\"critical\">yes</span>" "<span class=\"ok\">no</span>"))

(defn- last-fact-for [ledger subject]
  (last (filter #(= (:subject %) subject) ledger)))

(defn- status-cell [ledger subject]
  (let [f (last-fact-for ledger subject)]
    (cond
      (nil? f) "<span class=\"muted\">no activity</span>"
      (= :governor-hold (:t f))
      (str "<span class=\"critical\">HARD hold &middot; "
           (esc (kw-str (or (-> f :violations first :rule) :unknown))) "</span>")
      (= :committed (:t f)) "<span class=\"ok\">committed</span>"
      :else "<span class=\"muted\">in progress</span>")))

;; ----------------------------- residents -----------------------------

(defn- resident-row
  [ledger {:keys [id resident-name jurisdiction
                  mandatory-reporting-obligation-unresolved?
                  background-check-not-cleared?
                  care-plan-finalized? incident-response-finalized?
                  careplan-number incident-number]}]
  (format (str "        <tr><td><code>%s</code></td><td>%s</td><td>%s</td><td>%s</td>"
               "<td>%s</td><td>%s</td><td>%s</td><td>%s</td></tr>")
          (esc id) (esc resident-name) (esc jurisdiction)
          (yes-no mandatory-reporting-obligation-unresolved?)
          (yes-no background-check-not-cleared?)
          (if care-plan-finalized?
            (str "<span class=\"ok\">finalized &middot; <code>" (esc careplan-number) "</code></span>")
            "<span class=\"muted\">not finalized</span>")
          (if incident-response-finalized?
            (str "<span class=\"ok\">finalized &middot; <code>" (esc incident-number) "</code></span>")
            "<span class=\"muted\">not finalized</span>")
          (status-cell ledger id)))

;; ----------------------------- action gate (derived) -----------------------------

(def ^:private op-order
  "The closed op contract, in lifecycle order. Membership is asserted
  against `residential.phase/write-ops` at render time, so an op added
  to the actor without being added here fails the build rather than
  quietly vanishing from the page."
  [:resident/intake :careplan/verify :safeguarding/screen
   :background-check/screen
   :actuation/finalize-care-plan :actuation/finalize-incident-response])

(defn- gate-cell
  "Derives the phase-3 gate for `o` by actually calling
  `residential.phase/gate` and `residential.governor/high-stakes` --
  never a transcribed description."
  [o]
  (let [high? (boolean (governor/high-stakes o))
        {:keys [disposition reason]} (phase/gate phase/default-phase {:op o} :commit)]
    (cond
      high?
      (str "<span class=\"warn\">ALWAYS human approval &middot; never auto at any phase</span>"
           " <span class=\"muted\">(governor high-stakes + absent from every <code>:auto</code> set)</span>")

      (= :commit disposition)
      "<span class=\"ok\">phase-3 auto-commit when governor-clean</span>"

      :else
      (str "<span class=\"warn\">phase-3: human approval</span> <span class=\"muted\">("
           (esc (kw-str reason)) ")</span>"))))

(defn- gate-row [o]
  (format "        <tr><td><code>%s</code></td><td>%s</td></tr>"
          (esc (str o)) (gate-cell o)))

(defn- phase-row [[n {:keys [label writes auto]}]]
  (format "        <tr><td class=\"num\">%s</td><td><code>%s</code></td><td class=\"num\">%s</td><td>%s</td></tr>"
          n (esc label) (count writes)
          (if (seq auto)
            (str/join ", " (map #(str "<code>" (esc (str %)) "</code>") (sort-by str auto)))
            "<span class=\"muted\">none</span>")))

(defn- actuation-invariant-rows
  "Proves, by evaluation rather than by assertion in prose, that no
  phase ever lists a high-stakes actuation op in its `:auto` set."
  []
  (for [[n {:keys [auto]}] (sort-by key phase/phases)
        :let [leaked (filter governor/high-stakes auto)]]
    (format "        <tr><td class=\"num\">%s</td><td>%s</td></tr>"
            n
            (if (seq leaked)
              (str "<span class=\"critical\">LEAKED: " (esc (str/join ", " (map str leaked))) "</span>")
              "<span class=\"ok\">no actuation op is auto-eligible</span>"))))

;; ----------------------------- governor holds -----------------------------

(defn- hold-rows [ledger]
  (for [{:keys [op subject violations confidence]} (filter #(= :governor-hold (:t %)) ledger)
        :let [v (first violations)]]
    (format (str "        <tr><td><code>%s</code></td><td><code>%s</code></td>"
                 "<td><span class=\"critical\">%s</span></td><td>%s</td><td class=\"num\">%s</td></tr>")
            (esc (str op)) (esc subject)
            (esc (kw-str (:rule v))) (esc (:detail v)) confidence)))

;; ----------------------------- jurisdictions -----------------------------

(defn- jurisdiction-row [[iso3 {:keys [name owner-authority legal-basis provenance required-evidence]}]]
  (format (str "        <tr><td><code>%s</code></td><td>%s</td><td>%s</td><td>%s</td>"
               "<td class=\"num\">%s</td><td><a href=\"%s\">source</a></td></tr>")
          (esc iso3) (esc name) (esc owner-authority) (esc legal-basis)
          (count required-evidence) (esc provenance)))

(defn- evidence-rows
  "The required-evidence checklist actually stored for the one resident
  whose care-plan verification committed -- read back out of the store,
  not out of `facts/catalog`."
  [db]
  (for [r (store/all-residents db)
        :let [cp (store/careplan-of db (:id r))]
        :when cp
        item (:checklist cp)]
    (format "        <tr><td><code>%s</code></td><td><code>%s</code></td><td>%s</td></tr>"
            (esc (:id r)) (esc (:jurisdiction cp)) (esc item))))

;; ----------------------------- finalization records -----------------------------

(defn- record-row [rec]
  (format (str "        <tr><td><code>%s</code></td><td>%s</td><td><code>%s</code></td>"
               "<td><code>%s</code></td><td>%s</td></tr>")
          (esc (get rec "record_id")) (esc (get rec "kind"))
          (esc (get rec "resident_id")) (esc (get rec "jurisdiction"))
          (if (get rec "immutable")
            "<span class=\"ok\">immutable</span>"
            "<span class=\"warn\">mutable</span>")))

;; ----------------------------- approver attribution (derived) -----------------------------

(def ^:private approver-keys
  "Keys a store backend might use to retain the approver. Checked by
  presence so this table self-corrects if a backend starts retaining
  attribution it currently drops."
  [:approved-by :approver "approved_by" "approver"])

(defn- retained-approver [m]
  (when (map? m) (some #(get m %) approver-keys)))

(defn- audit-approver
  "The approver recorded in the in-run `:audit` channel for this
  subject+op, which is where `approval-granted` facts live."
  [audit subject o]
  (->> audit
       (filter #(and (= :approval-granted (:t %)) (= subject (:subject %)) (= o (:op %))))
       (map :by)
       first))

(defn- attribution-row [label subject o stored audit]
  (let [kept (retained-approver stored)
        seen (audit-approver audit subject o)]
    (format "        <tr><td>%s</td><td><code>%s</code></td><td>%s</td><td>%s</td></tr>"
            (esc label) (esc subject)
            (cond kept (str "<code>" (esc kept) "</code>")
                  seen (str "<code>" (esc seen) "</code>")
                  :else "<span class=\"muted\">&mdash;</span>")
            (cond
              kept "<span class=\"ok\">retained in record</span>"
              seen (str "<span class=\"warn\">audit only &mdash; not retained in record</span>")
              :else (str "<span class=\"muted\">auto-committed under phase-3 policy &mdash; "
                         "no human approver required</span>")))))

(defn- attribution-rows
  "Walks the SSoT registers and the actuation histories and asks each
  one whether it actually kept the approver, rather than hardcoding a
  verdict. Where a register dropped it, the approver is joined from the
  audit fact and labelled as such -- silently omitting it would leave a
  reader unable to distinguish 'nobody approved' from 'the store did
  not keep it'."
  [db audit]
  (concat
   (for [r (store/all-residents db)
         [label o stored]
         [["resident directory (:resident/upsert)" :resident/intake (store/resident db (:id r))]
          ["care-plan register (:careplan/set)" :careplan/verify (store/careplan-of db (:id r))]
          ["safeguarding register (:safeguarding/set)" :safeguarding/screen (store/safeguarding-of db (:id r))]
          ["background-check register (:background-check/set)" :background-check/screen (store/background-check-of db (:id r))]]
         :when stored]
     (attribution-row label (:id r) o stored audit))
   (for [rec (store/careplan-history db)]
     (attribution-row "care-plan finalization record" (get rec "resident_id")
                      :actuation/finalize-care-plan rec audit))
   (for [rec (store/incident-history db)]
     (attribution-row "incident-response finalization record" (get rec "resident_id")
                      :actuation/finalize-incident-response rec audit))))

;; ----------------------------- ledger -----------------------------

(defn- ledger-row [{:keys [t op subject disposition basis]}]
  (format "        <tr><td>%s</td><td><code>%s</code></td><td><code>%s</code></td><td>%s</td><td>%s</td></tr>"
          (if (= :governor-hold t)
            (str "<span class=\"critical\">" (esc (kw-str t)) "</span>")
            (str "<span class=\"ok\">" (esc (kw-str t)) "</span>"))
          (esc (str (or op :n-a))) (esc subject)
          (esc (kw-str (or disposition "")))
          (esc (or (some->> basis (map kw-str) (str/join ", ")) ""))))

;; ----------------------------- document -----------------------------

(defn render
  "Renders the full operator-console.html document from the result of
  `run-demo!` (or any other real scenario)."
  [{:keys [db audit]}]
  (let [ledger (vec (store/ledger db))
        residents (store/all-residents db)
        holds (filter #(= :governor-hold (:t %)) ledger)
        cov (facts/coverage)]
    (str
     "<html lang=\"ja\"><head><meta charset=\"utf-8\">"
     "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
     "<title>cloud-itonami-isic-8790 &middot; residential care</title><style>"
     (jp-go-dds.skin/dds+skin)
     "</style></head><body>\n"
     "<header class=\"bar\">\n"
     "  <h1>Residential care activities (ISIC 8790) — Operator Console</h1>\n"
     "  <span class=\"badge\">read-only sample · governor-gated · care-plan &amp; incident-response finalization always human-approved</span>\n"
     "</header>\n"
     "<main>\n"

     ;; --- residents ---
     "  <section class=\"card\">\n"
     "    <h2>Residents</h2>\n"
     "    <p class=\"muted\">Demo snapshot — build-time-generated from <code>residential.store</code> via <code>residential.render-html</code> (<code>clojure -M:dev:render-html</code>). Every row is read back out of the SSoT after a real actor run.</p>\n"
     "    <table>\n"
     "      <thead><tr><th>Resident</th><th>Name</th><th>Jurisdiction</th><th>Mandatory-reporting obligation unresolved</th><th>Background check not cleared</th><th>Care plan</th><th>Incident response</th><th>Last op status</th></tr></thead>\n"
     "      <tbody>\n"
     (str/join "\n" (map (partial resident-row ledger) residents)) "\n"
     "      </tbody>\n"
     "    </table>\n"
     "  </section>\n"

     ;; --- action gate ---
     "  <section class=\"card\">\n"
     "    <h2>Action gate (Residential Care Governor)</h2>\n"
     "    <p class=\"muted\">Derived at build time by calling <code>residential.phase/gate</code> and <code>residential.governor/high-stakes</code> — not transcribed. HARD violations cannot be overridden by an approver.</p>\n"
     "    <table>\n"
     "      <thead><tr><th>Op</th><th>Gate at phase " phase/default-phase "</th></tr></thead>\n"
     "      <tbody>\n"
     (str/join "\n" (map gate-row op-order)) "\n"
     "      </tbody>\n"
     "    </table>\n"
     "  </section>\n"

     ;; --- rollout ladder ---
     "  <section class=\"card\">\n"
     "    <h2>Rollout phases</h2>\n"
     "    <p class=\"muted\">Read straight out of <code>residential.phase/phases</code>. The console runs at phase <code>" phase/default-phase "</code>.</p>\n"
     "    <table>\n"
     "      <thead><tr><th>Phase</th><th>Label</th><th>Write ops</th><th>Auto-commit ops</th></tr></thead>\n"
     "      <tbody>\n"
     (str/join "\n" (map phase-row (sort-by key phase/phases))) "\n"
     "      </tbody>\n"
     "    </table>\n"
     "    <h3>Two-layer actuation invariant</h3>\n"
     "    <p class=\"muted\">Evaluated, not asserted: for every phase, is any <code>governor/high-stakes</code> op present in that phase's <code>:auto</code> set?</p>\n"
     "    <table>\n"
     "      <thead><tr><th>Phase</th><th>Check</th></tr></thead>\n"
     "      <tbody>\n"
     (str/join "\n" (actuation-invariant-rows)) "\n"
     "      </tbody>\n"
     "    </table>\n"
     "  </section>\n"

     ;; --- hard holds ---
     "  <section class=\"card\">\n"
     "    <h2>HARD governor holds this run (" (count holds) ")</h2>\n"
     "    <p class=\"muted\">None of these reached a human approver. A HARD violation short-circuits before the approval node — there is no override path.</p>\n"
     "    <table>\n"
     "      <thead><tr><th>Op</th><th>Resident</th><th>Rule</th><th>Detail</th><th>Advisor confidence</th></tr></thead>\n"
     "      <tbody>\n"
     (str/join "\n" (hold-rows ledger)) "\n"
     "      </tbody>\n"
     "    </table>\n"
     "  </section>\n"

     ;; --- jurisdictions ---
     "  <section class=\"card\">\n"
     "    <h2>Jurisdiction spec-basis catalog</h2>\n"
     "    <p class=\"muted\">From <code>residential.facts/catalog</code>. A jurisdiction absent from this table has NO spec-basis — the advisor must not invent one, and the governor holds if it tries (see <code>resident-2</code>/<code>ATL</code> above).</p>\n"
     "    <table>\n"
     "      <thead><tr><th>ISO3</th><th>Jurisdiction</th><th>Owner authority</th><th>Legal basis</th><th>Required evidence</th><th>Provenance</th></tr></thead>\n"
     "      <tbody>\n"
     (str/join "\n" (map jurisdiction-row (sort-by key facts/catalog))) "\n"
     "      </tbody>\n"
     "    </table>\n"
     "    <p class=\"muted\">Coverage: <span class=\"num\">" (:covered cov) "</span> of <span class=\"num\">"
     (:requested cov) "</span> requested jurisdictions seeded ("
     (esc (str/join ", " (:covered-jurisdictions cov))) "). " (esc (:note cov)) "</p>\n"
     "    <h3>Evidence checklist actually on file</h3>\n"
     "    <p class=\"muted\">Read back from the committed care-plan register, not from the catalog — this is what the governor's <code>evidence-incomplete</code> check reads.</p>\n"
     "    <table>\n"
     "      <thead><tr><th>Resident</th><th>Jurisdiction</th><th>Required evidence item</th></tr></thead>\n"
     "      <tbody>\n"
     (str/join "\n" (evidence-rows db)) "\n"
     "      </tbody>\n"
     "    </table>\n"
     "  </section>\n"

     ;; --- finalization records ---
     "  <section class=\"card\">\n"
     "    <h2>Finalization records (drafts)</h2>\n"
     "    <p class=\"muted\">Built by <code>residential.registry</code> — an unsigned draft record a residential-care operator would keep. Signing is the operator's own act, not this actor's.</p>\n"
     "    <table>\n"
     "      <thead><tr><th>Record id</th><th>Kind</th><th>Resident</th><th>Jurisdiction</th><th>Status</th></tr></thead>\n"
     "      <tbody>\n"
     (str/join "\n" (map record-row (concat (store/careplan-history db)
                                            (store/incident-history db)))) "\n"
     "      </tbody>\n"
     "    </table>\n"
     "  </section>\n"

     ;; --- approver attribution ---
     "  <section class=\"card\">\n"
     "    <h2>Approver attribution</h2>\n"
     "    <p class=\"muted\">Derived by walking each SSoT register and asking whether it actually kept the approver, rather than hardcoding a verdict — so this table self-corrects if a backend starts retaining attribution it currently drops. Where the record did not keep it, the approver is joined from the in-run audit fact and labelled explicitly: omitting it silently would leave you unable to tell &ldquo;nobody approved&rdquo; from &ldquo;the store did not keep it&rdquo;.</p>\n"
     "    <table>\n"
     "      <thead><tr><th>Register / record</th><th>Resident</th><th>Approver</th><th>Provenance</th></tr></thead>\n"
     "      <tbody>\n"
     (str/join "\n" (attribution-rows db audit)) "\n"
     "      </tbody>\n"
     "    </table>\n"
     "  </section>\n"

     ;; --- ledger ---
     "  <section class=\"card\">\n"
     "    <h2>Audit ledger (this run)</h2>\n"
     "    <p class=\"muted\">Append-only decision-fact log — every commit and every hold this scenario produced, in order.</p>\n"
     "    <table>\n"
     "      <thead><tr><th>Fact</th><th>Op</th><th>Resident</th><th>Disposition</th><th>Basis</th></tr></thead>\n"
     "      <tbody>\n"
     (str/join "\n" (map ledger-row ledger)) "\n"
     "      </tbody>\n"
     "    </table>\n"
     "  </section>\n"
     "</main>\n"
     "<footer>Generated by <code>residential.render-html</code> from a real <code>residential.operation</code> run — "
     (count ledger) " ledger facts, " (count holds) " HARD holds, "
     (count residents) " residents. No hand-written domain content.</footer>\n"
     "</body></html>\n")))

(defn -main [& args]
  (let [out (or (first args) "docs/samples/operator-console.html")
        {:keys [db] :as result} (run-demo!)
        ledger (vec (store/ledger db))
        holds (filterv #(= :governor-hold (:t %)) ledger)]

    ;; Build-time invariant, not a convention: a console that shows no
    ;; HARD hold has not demonstrated that the governor can refuse
    ;; anything, which is the whole claim this page makes. Fail the
    ;; build rather than publish a page that only shows happy paths.
    (when (zero? (count holds))
      (throw (ex-info (str "render-html: scenario produced 0 :governor-hold records -- "
                           "refusing to emit a console that cannot demonstrate a refusal")
                      {:ledger-facts (count ledger)
                       :dispositions (frequencies (map :t ledger))})))

    ;; The op contract on the page must match the actor's real op set.
    (when-not (= (set op-order) phase/write-ops)
      (throw (ex-info "render-html: op-order drifted from residential.phase/write-ops"
                      {:page (set op-order) :actor phase/write-ops})))

    (let [html (render result)]
      (io/make-parents out)
      (spit out html)
      (println "wrote" out
               (str "(" (count ledger) " ledger facts, "
                    (count holds) " HARD holds ["
                    (str/join ", " (map #(kw-str (-> % :violations first :rule)) holds))
                    "], "
                    (count (store/all-residents db)) " residents, "
                    (count (store/careplan-history db)) " care-plan records, "
                    (count (store/incident-history db)) " incident records)")))))
