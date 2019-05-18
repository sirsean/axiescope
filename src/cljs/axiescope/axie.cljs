(ns axiescope.axie
  (:require
    [cljs-web3.core :as web3]
    [axiescope.moves :as moves]
    ))

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
  (-> axie
      merge-stats
      attach-num-mystic
      attach-attack
      attach-defense
      attach-atk+def
      attach-price
      attach-purity
      attach-dps-score
      attach-tank-score))
