(ns axiescope.util)

(defn round
  [d precision]
  (let [factor (Math/pow 10 precision)]
    (/ (Math/round (* d factor)) factor)))

(defn ranking-type->key
  [ranking-type]
  (case ranking-type
    :attack :attack-rankings
    :defense :defense-rankings
    :all :rankings))
