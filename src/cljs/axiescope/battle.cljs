(ns axiescope.battle)

(defn class-advantage
  [a b]
  (case [a b]
    ["plant" "aquatic"]   3
    ["plant" "bird"]      3
    ["reptile" "aquatic"] 3
    ["reptile" "bird"]    3
    ["aquatic" "bug"]     3
    ["aquatic" "beast"]   3
    ["bird" "bug"]        3
    ["bird" "beast"]      3
    ["beast" "plant"]     3
    ["beast" "reptile"]   3
    ["bug" "plant"]       3
    ["bug" "reptile"]     3
    ["aquatic" "plant"]   -3
    ["bird" "plant"]      -3
    ["aquatic" "reptile"] -3
    ["bird" "reptile"]    -3
    ["bug" "aquatic"]     -3
    ["beast" "aquatic"]   -3
    ["bug" "bird"]        -3
    ["beast" "bird"]      -3
    ["plant" "beast"]     -3
    ["reptile" "beast"]   -3
    ["plant" "bug"]       -3
    ["reptile" "bug"]     -3
    0))

(defn simulate
  [attacker defender]
  (let [defender-class (:class defender)
        attacks (->> attacker
                     :parts
                     (filter (comp seq :moves))
                     (map (fn [{:keys [name class moves]}]
                            {:name name
                             :class class
                             :attack (-> moves first :attack)})))
        defenses (->> defender
                      :parts
                      (filter (comp seq :moves))
                      (map (fn [{:keys [name class moves]}]
                             {:name name
                              :class class
                              :defense (-> moves first :defense)})))]
    (for [a attacks
          d defenses]
      {:attack (:name a)
       :defense (:name d)
       :dmg (+ (- (:attack a) (:defense d))
               (class-advantage (:class a) defender-class)
               (class-advantage (:class a) (:class d)))})))
