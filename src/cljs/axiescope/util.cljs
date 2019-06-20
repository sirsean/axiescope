(ns axiescope.util)

(defn round
  [d precision]
  (let [factor (Math/pow 10 precision)]
    (/ (Math/round (* d factor)) factor)))
