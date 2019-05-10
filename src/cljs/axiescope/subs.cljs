(ns axiescope.subs
  (:require
   [re-frame.core :as rf]
   [axiescope.moves :as moves]
   [axiescope.battle :as battle]))

(rf/reg-sub
  ::active-panel
  (fn [{:keys [active-panel]}]
    active-panel))

(rf/reg-sub
  ::bs-attacker
  (fn [db]
    (get-in db [:battle-simulator :attacker])))

(rf/reg-sub
  ::bs-defender
  (fn [db]
    (get-in db [:battle-simulator :defender])))

(rf/reg-sub
  ::battle-simulation
  (fn [_]
    [(rf/subscribe [::bs-attacker])
     (rf/subscribe [::bs-defender])])
  (fn [[attacker defender]]
    (when (and attacker defender)
      (battle/simulate attacker defender))))

(rf/reg-sub
  :my-axies/loading?
  (fn [db]
    (get-in db [:my-axies :loading?])))

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
                        (map :name)
                        (map moves/dps-move-score)
                        (apply +))))

(defn attach-tank-score
  [{:keys [parts] :as axie}]
  (assoc axie :tank (->> parts
                         (map :name)
                         (map moves/tank-move-score)
                         (apply +))))

(defn adjust-axie
  [axie]
  (-> axie
      attach-attack
      attach-defense
      attach-atk+def
      attach-purity
      attach-dps-score
      attach-tank-score))

(rf/reg-sub
  :my-axies/sort-key
  (fn [db]
    (get-in db [:my-axies :sort-key] :id)))

(rf/reg-sub
  :my-axies/raw-axies
  (fn [db]
    (map adjust-axie (get-in db [:my-axies :axies]))))

(rf/reg-sub
  :my-axies/axies
  (fn [_]
    [(rf/subscribe [:my-axies/raw-axies])
     (rf/subscribe [:my-axies/sort-key])])
  (fn [[axies sort-key]]
    (reverse (sort-by sort-key axies))))
