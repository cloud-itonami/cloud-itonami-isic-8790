(ns residential.facts
  "Per-jurisdiction residential-care/child-welfare/safeguarding
  regulatory catalog -- the G2-style spec-basis table the Residential
  Care Governor checks every `:careplan/verify` proposal against
  ('did the advisor cite an OFFICIAL public source for this
  jurisdiction's residential-care/safeguarding framework, or did it
  invent one?').

  Coverage is reported HONESTLY (see `coverage`), the same discipline
  every sibling actor's `facts` namespace uses: a jurisdiction not in
  this table has NO spec-basis, full stop -- the advisor must not
  fabricate one, and the governor holds if it tries.

  Seed values are drawn from each jurisdiction's official child-
  welfare/residential-care authority (see `:provenance`); they are a
  STARTING catalog, not a from-scratch survey of all ~194
  jurisdictions. Extending coverage is additive: add one map to
  `catalog`, cite a real source, done -- never invent a jurisdiction's
  requirements to make coverage look bigger.")

(def catalog
  "iso3 -> requirement map. `:required-evidence` mirrors the resident-
  consent/care-plan/safeguarding-assessment/incident-report evidence
  set this blueprint's own Offer names; `:legal-basis` /
  `:owner-authority` / `:provenance` are the G2 citation the governor
  requires before any `:actuation/finalize-care-plan`/`:actuation/
  finalize-incident-response` proposal can commit."
  {"JPN" {:name "Japan"
          :owner-authority "厚生労働省 (Ministry of Health, Labour and Welfare)"
          :legal-basis "児童福祉法 (Child Welfare Act) -- 児童虐待の防止等に関する法律第6条 (Child Abuse Prevention Act Art. 6, mandatory reporting)"
          :national-spec "児童福祉施設等における入所者ケアプランおよび虐待通告義務"
          :provenance "https://www.mhlw.go.jp/stf/seisakunitsuite/bunya/kodomo/kodomo_kosodate/dv/index.html"
          :required-evidence ["入所者同意記録 (resident-consent-record)"
                              "ケアプラン記録 (care-plan-record)"
                              "セーフガーディング評価記録 (safeguarding-assessment-record)"
                              "事故報告記録 (incident-report-record)"]}
   "USA" {:name "United States"
          :owner-authority "U.S. Department of Health & Human Services, Administration for Children and Families"
          :legal-basis "Child Abuse Prevention and Treatment Act (CAPTA), 42 U.S.C. §5106g -- mandatory reporting requirement"
          :national-spec "Residential child-welfare facility care-plan and mandatory-reporting documentation requirements"
          :provenance "https://www.acf.hhs.gov/cb/law-regulation/child-abuse-prevention-and-treatment-act"
          :required-evidence ["Resident consent record"
                              "Care-plan record"
                              "Safeguarding-assessment record"
                              "Incident-report record"]}
   "GBR" {:name "United Kingdom"
          :owner-authority "Department for Education (DfE)"
          :legal-basis "Children Act 1989/2004 -- Working Together to Safeguard Children (statutory guidance)"
          :national-spec "Residential care-plan and safeguarding-referral documentation for looked-after children/vulnerable residents"
          :provenance "https://www.gov.uk/government/publications/working-together-to-safeguard-children--2"
          :required-evidence ["Resident consent record"
                              "Care-plan record"
                              "Safeguarding-assessment record"
                              "Incident-report record"]}
   "DEU" {:name "Germany"
          :owner-authority "Bundesministerium für Familie, Senioren, Frauen und Jugend (BMFSFJ)"
          :legal-basis "Kinder- und Jugendhilfegesetz (SGB VIII) §8a -- Schutzauftrag bei Kindeswohlgefährdung (mandatory reporting duty)"
          :national-spec "Hilfeplan (care-plan) und Meldepflicht bei Gefährdung des Kindeswohls in stationären Einrichtungen"
          :provenance "https://www.gesetze-im-internet.de/sgb_8/__8a.html"
          :required-evidence ["Einwilligungsprotokoll (resident-consent-record)"
                              "Hilfeplanprotokoll (care-plan-record)"
                              "Kinderschutz-Bewertungsprotokoll (safeguarding-assessment-record)"
                              "Vorfallberichtsprotokoll (incident-report-record)"]}})

(defn spec-basis
  "The jurisdiction's requirement map, or nil -- nil means NO spec-basis,
  and the governor must hold any proposal that tries to finalize a
  care plan or incident response on it."
  [iso3]
  (get catalog iso3))

(defn coverage
  "Honest coverage report: how many of the requested jurisdictions actually
  have a spec-basis entry. Never report a missing jurisdiction as covered."
  ([] (coverage (keys catalog)))
  ([iso3s]
   (let [have (filter catalog iso3s)
         missing (remove catalog iso3s)]
     {:requested (count iso3s)
      :covered (count have)
      :covered-jurisdictions (vec (sort have))
      :missing-jurisdictions (vec (sort missing))
      :note (str "cloud-itonami-isic-8790 R0: " (count catalog)
                 " jurisdictions seeded with an official spec-basis. "
                 "This is a starting catalog, not a survey of all ~194 "
                 "jurisdictions -- extend `residential.facts/catalog`, "
                 "never fabricate a jurisdiction's requirements.")})))

(defn required-evidence-satisfied?
  "Does `submitted` (a set/coll of evidence keywords or strings) satisfy
  every evidence item listed for `iso3`? Missing spec-basis -> never
  satisfied."
  [iso3 submitted]
  (when-let [{:keys [required-evidence]} (spec-basis iso3)]
    (let [need (count required-evidence)
          have (count (filter (set submitted) required-evidence))]
      (= need have))))

(defn evidence-checklist [iso3]
  (:required-evidence (spec-basis iso3) []))
