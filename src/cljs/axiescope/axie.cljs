(ns axiescope.axie
  (:require
    [cljs-web3.core :as web3]
    [cljsjs.moment]
    [axiescope.moves :as moves]
    [axiescope.stats :as stats]
    [axiescope.util :refer [round]]
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

(defn attach-dps-tiers
  [{:keys [parts] :as axie}]
  (assoc axie :dps-tiers (->> parts
                              (map :id)
                              (map moves/dps-part-score)
                              (apply +))))

(defn attach-tank-tiers
  [{:keys [parts] :as axie}]
  (assoc axie :tank-tiers (->> parts
                               (map :id)
                               (map moves/tank-part-score)
                               (apply +))))

(defn support-tiers
  [{:keys [parts]}]
  (->> parts
       (map :id)
       (map moves/support-part-score)
       (apply +)))

(defn attach-support-tiers
  [axie]
  (assoc axie :support-tiers (support-tiers axie)))

(defn average
  [nums]
  (if (empty? nums)
    0
    (/ (reduce + nums) (count nums))))

(defn weighted-average
  "Calculates a total score from individual weighted results.
  Expects pairs of <weight>, <score> whose weights add to 1.0."
  [& args]
  (apply + (map #(apply * %)
                (partition 2 args))))

(defn tank-body
  [{:keys [stats parts] :as axie}]
  (if (moves/shortcake? axie)
    0
    (apply weighted-average
           [0.60 (stats/hp-score (:hp stats))
            #_#_0.20 (stats/speed-score (:speed stats))
            0.40 (->> parts
                      (mapcat :moves)
                      (map :defense)
                      (map stats/defense-score)
                      average)])))

(defn attach-tank-body
  [axie]
  (assoc axie :tank-body (round (tank-body axie) 1)))

(defn dps-body
  [{:keys [class stats parts]}]
  (let [atk (->> parts
                 (mapcat :moves)
                 (map :attack)
                 (map stats/attack-score)
                 average)
        acc (->> parts
                 (mapcat :moves)
                 (map :accuracy)
                 (map stats/accuracy-score)
                 average)
        score (/ (* atk acc) 4)
        stat-score (case class
                     ("aquatic" "bird") (+ score (-> stats :speed stats/speed-score (/ 5)))
                     ("beast" "bug") (+ score (-> stats :morale stats/morale-score (/ 5)))
                     ("reptile" "plant") (+ score (-> stats :skill stats/skill-score (/ 5)))
                     0)]
    (* stat-score 0.85)))

(defn attach-dps-body
  [axie]
  (assoc axie :dps-body (round (dps-body axie) 1)))

(defn support-body
  [{:keys [stats] :as axie}]
  (weighted-average
    0.75 (* (support-tiers axie) (/ 5 16))
    0.10 (stats/hp-score (:hp stats))
    0.15 (stats/speed-score (:speed stats))))

(defn attach-support-body
  [axie]
  (assoc axie :support-body (round (support-body axie) 1)))

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

(defn update-birth-date
  [axie]
  (update axie :birth-date (comp js/moment (partial * 1000))))

(defn attach-morphable
  [{:keys [birth-date] :as axie}]
  (assoc axie
         :morph-to-petite (.add (js/moment birth-date) 3 "days")
         :morph-to-adult (.add (js/moment birth-date) 5 "days")))

(defn merge-stats
  [{:keys [stats] :as axie}]
  (merge stats axie))

(defn adjust-axie
  [axie]
  (some-> axie
          merge-stats
          update-birth-date
          attach-morphable
          attach-num-mystic
          attach-attack
          attach-defense
          attach-atk+def
          attach-price
          attach-purity
          attach-next-breed
          attach-pending-exp
          attach-tank-body
          attach-tank-tiers
          attach-dps-body
          attach-dps-tiers
          attach-support-body
          attach-support-tiers))
