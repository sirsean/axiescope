(ns axiescope.subs
  (:require
   [re-frame.core :as rf]
   [axiescope.moves :as moves]
   [axiescope.battle :as battle]))

(rf/reg-sub
  :identity
  (fn [_ [_ x]]
    x))

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

(rf/reg-sub
  :my-axies/breedable
  (fn [_]
    [(rf/subscribe [:my-axies/raw-axies])
     (rf/subscribe [:my-axies/sort-key])])
  (fn [[axies sort-key]]
    (->> axies
         (sort-by sort-key)
         reverse
         (filter :breedable))))

(rf/reg-sub
  :teams/loading?
  (fn [db]
    (get-in db [:teams :loading?])))

(rf/reg-sub
  :teams/raw-teams
  (fn [db]
    (get-in db [:teams :teams])))

(rf/reg-sub
  :teams/axie-id->activity-points
  (fn [db]
    (get-in db [:teams :axie-id->activity-points] {})))

(rf/reg-sub
  :teams/axie-db
  (fn [db]
    (get-in db [:teams :axie-db] {})))

(defn team-can-battle?
  [{:keys [team-members]}]
  (and (= 3 (count team-members))
       (every? (comp (partial <= 240) :activity-points) team-members)))

(defn team-ready-in
  [{:keys [team-members]}]
  (->> team-members
       (map :activity-points)
       (apply min)
       (- 240)))

(rf/reg-sub
  :teams/teams
  (fn [_]
    [(rf/subscribe [:teams/raw-teams])
     (rf/subscribe [:teams/axie-id->activity-points])
     (rf/subscribe [:teams/axie-db])])
  (fn [[teams axie-id->activity-points axie-db]]
    (->> teams
         (map (fn [team]
                (-> team
                    (update :team-members
                            (partial map (fn [{:keys [axie-id] :as a}]
                                           (assoc a
                                                  :axie (adjust-axie (get axie-db axie-id))
                                                  :activity-points (axie-id->activity-points axie-id)))))
                    ((fn [t]
                       (assoc t
                              :ready? (team-can-battle? t)
                              :ready-in (team-ready-in t)))))))
         (sort-by :ready-in))))

(rf/reg-sub
  :teams/team-axies
  (fn [[_ team-id]]
    [(rf/subscribe [:identity team-id])
     (rf/subscribe [:teams/raw-teams])
     (rf/subscribe [:teams/axie-db])])
  (fn [[team-id teams axie-db]]
    (->> teams
         (filter (fn [t] (= team-id (:team-id t))))
         first
         :team-members
         (map :axie-id)
         (map axie-db)
         (map adjust-axie))))
