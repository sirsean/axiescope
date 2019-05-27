(ns axiescope.axie
  (:require
    [cljs-web3.core :as web3]
    [axiescope.moves :as moves]
    ))

(def breed-count->next-exp
  {0 700
   1 900
   2 900
   3 1500
   4 2400
   5 3000
   6 3000})

(defn calc-next-breed
  [axie]
  (-> axie
      :breed-count
      breed-count->next-exp))

(defn attach-next-breed
  [axie]
  (assoc axie :next-breed (calc-next-breed axie)))

(defn calc-pending-exp
  [{:keys [exp pending-exp breed-count]}]
  (let [used-exp (->> breed-count->next-exp
                      (filter (fn [[bc _]]
                                (< bc breed-count)))
                      (map second)
                      (apply +))]
    (- pending-exp
       used-exp
       -400 ; because axies start with 400 exp
       exp)))

(defn attach-pending-exp
  [axie]
  (assoc axie :pending-exp (calc-pending-exp axie)))

(defn calc-num-mystic
  [axie]
  (->> axie
       :parts
       (map :mystic)
       (filter true?)
       count))

(defn attach-num-mystic
  [axie]
  (assoc axie :num-mystic (calc-num-mystic axie)))

(defn calc-purity
  [{:keys [class] :as axie}]
  (->> axie
       :parts
       (map :class)
       (filter (partial = class))
       count))

(defn attach-purity
  [axie]
  (assoc axie :purity (calc-purity axie)))

(defn calc-attack
  [axie]
  (->> axie
       :parts
       (mapcat :moves)
       (map :attack)
       (apply +)))

(defn attach-attack
  [axie]
  (assoc axie :attack (calc-attack axie)))

(defn calc-defense
  [axie]
  (->> axie
       :parts
       (mapcat :moves)
       (map :defense)
       (apply +)))

(defn attach-defense
  [axie]
  (assoc axie :defense (calc-defense axie)))

(defn attach-atk+def
  [{:keys [attack defense] :as axie}]
  (assoc axie :atk+def (+ (or attack 0) (or defense 0))))

(defn attach-dps-score
  [{:keys [parts] :as axie}]
  (assoc axie :dps (->> parts
                        (map :id)
                        (map moves/dps-part-score)
                        (apply +))))

(defn attach-tank-score
  [{:keys [parts] :as axie}]
  (assoc axie :tank (->> parts
                         (map :id)
                         (map moves/tank-part-score)
                         (apply +))))

(defn calc-price
  [axie]
  (some-> axie
          :auction
          :buy-now-price
          web3/to-decimal
          (* 1e-18M)))

(defn attach-price
  [axie]
  (assoc axie :price (calc-price axie)))

(defn merge-stats
  [{:keys [stats] :as axie}]
  (merge stats axie))

(defn adjust-axie
  [axie]
  (some-> axie
          merge-stats
          attach-num-mystic
          attach-attack
          attach-defense
          attach-atk+def
          attach-price
          attach-purity
          attach-next-breed
          attach-pending-exp
          attach-dps-score
          attach-tank-score))
