(ns axiescope.subs
  (:require
   [re-frame.core :as rf]
   [cljsjs.moment]
   [axiescope.axie :refer [adjust-axie]]
   [axiescope.battle :as battle]))

(rf/reg-sub
  :identity
  (fn [_ [_ x]]
    x))

(rf/reg-sub
  :time/now
  (fn [db]
    (:now db)))

(rf/reg-sub
  ::active-panel
  (fn [{:keys [active-panel]}]
    active-panel))

(rf/reg-sub
  ::eth-addr
  (fn [{:keys [eth-addr]}]
    eth-addr))

(rf/reg-sub
  :battle-simulator/attacker
  (fn [db]
    (get-in db [:battle-simulator :attacker])))

(rf/reg-sub
  :battle-simulator/defender
  (fn [db]
    (get-in db [:battle-simulator :defender])))

(rf/reg-sub
  :battle-simulator/simulation
  (fn [_]
    [(rf/subscribe [:battle-simulator/attacker])
     (rf/subscribe [:battle-simulator/defender])])
  (fn [[attacker defender]]
    (when (and attacker defender)
      (battle/simulate attacker defender))))

(rf/reg-sub
  :axie/loading?
  (fn [db]
    (get-in db [:axie :loading?])))

(rf/reg-sub
  :axie/axie
  (fn [db]
    (adjust-axie (get-in db [:axie :axie]))))

(rf/reg-sub
  :my-axies/loading?
  (fn [db]
    (get-in db [:my-axies :loading?])))


(rf/reg-sub
  :my-axies/sort-key
  (fn [db]
    (get-in db [:my-axies :sort-key] :id)))

(rf/reg-sub
  :my-axies/sort-order
  (fn [db]
    (get-in db [:my-axies :sort-order] :desc)))

(rf/reg-sub
  :my-axies/raw-axies
  (fn [db]
    (map adjust-axie (get-in db [:my-axies :axies]))))

(rf/reg-sub
  :my-axies/count
  (fn [_]
    [(rf/subscribe [:my-axies/raw-axies])])
  (fn [[axies]]
    (count axies)))

(rf/reg-sub
  :my-axies/total
  (fn [db]
    (get-in db [:my-axies :total] "?")))

(rf/reg-sub
  :my-axies/offset
  (fn [db]
    (get-in db [:my-axies :offset] 0)))

(rf/reg-sub
  :my-axies/page-size
  (fn [db]
    (get-in db [:my-axies :page-size] 100)))

(rf/reg-sub
  :my-axies/axies
  (fn [_]
    [(rf/subscribe [:my-axies/raw-axies])
     (rf/subscribe [:my-axies/sort-key])
     (rf/subscribe [:my-axies/sort-order])
     (rf/subscribe [:my-axies/offset])
     (rf/subscribe [:my-axies/page-size])])
  (fn [[axies sort-key sort-order offset page-size]]
    (->> axies
         (sort-by sort-key)
         ((if (= :asc sort-order) identity reverse))
         (drop offset)
         (take page-size))))

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
  :teams/count
  (fn [_]
    [(rf/subscribe [:teams/raw-teams])])
  (fn [[teams]]
    (count teams)))

(rf/reg-sub
  :teams/total
  (fn [db]
    (get-in db [:teams :total] "?")))

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

(rf/reg-sub
  :teams/assigned-axie-ids
  (fn [_]
    [(rf/subscribe [:teams/raw-teams])])
  (fn [[teams]]
    (->> teams
         (mapcat :team-members)
         (map :axie-id)
         set)))

(defn adult?
  [{:keys [stage]}]
  (= stage 4))

(rf/reg-sub
  :teams/unassigned-axies
  (fn [_]
    [(rf/subscribe [:my-axies/raw-axies])
     (rf/subscribe [:teams/assigned-axie-ids])
     (rf/subscribe [:my-axies/sort-key])])
  (fn [[axies assigned? sort-key]]
    (->> axies
         (remove (comp assigned? :id))
         (filter adult?)
         (sort-by sort-key)
         reverse)))

(rf/reg-sub
  :teams/multi-assigned-axies
  (fn [_]
    [(rf/subscribe [:teams/teams])])
  (fn [[teams]]
    (->> teams
         (mapcat (fn [{:keys [team-id name team-members]}]
                   (->> team-members
                        (map (fn [{:keys [axie-id axie]}]
                               [axie-id {:team-id team-id
                                         :team-name name
                                         :axie axie}])))))
         (reduce (fn [a->ts [axie-id t]]
                   (update a->ts axie-id conj t)) {})
         (filter (fn [[_ ts]] (< 1 (count ts))))
         (mapcat (fn [[_ ts]]
                   (->> ts
                        (map (fn [{:keys [axie team-id team-name]}]
                               (assoc axie
                                      :team-id team-id
                                      :team-name team-name)))))))))

(rf/reg-sub
  :my-axies/larva
  (fn [_]
    [(rf/subscribe [:time/now])
     (rf/subscribe [:my-axies/raw-axies])])
  (fn [[now axies]]
    (->> axies
         (filter (fn [{:keys [stage]}] (= stage 2)))
         (filter (fn [{:keys [birth-date]}]
                   (let [bd (js/moment (* birth-date 1000))
                         days (.diff now bd "days")]
                     (<= 3 days)))))))

(rf/reg-sub
  :my-axies/petite
  (fn [_]
    [(rf/subscribe [:time/now])
     (rf/subscribe [:my-axies/raw-axies])])
  (fn [[now axies]]
    (->> axies
         (filter (fn [{:keys [stage]}] (= stage 3)))
         (filter (fn [{:keys [birth-date]}]
                   (let [bd (js/moment (* birth-date 1000))
                         days (.diff now bd "days")]
                     (<= 5 days)))))))

(rf/reg-sub
  :multi-gifter/to-addr
  (fn [db]
    (get db :multi-gifter/to-addr)))

(rf/reg-sub
  :search/loading?
  (fn [db]
    (get-in db [:search :loading?] false)))

(rf/reg-sub
  :search/raw-axies
  (fn [db]
    (map adjust-axie (get-in db [:search :axies]))))

(rf/reg-sub
  :search/count
  (fn [_]
    [(rf/subscribe [:search/raw-axies])])
  (fn [[axies]]
    (count axies)))

(rf/reg-sub
  :search/total
  (fn [db]
    (get-in db [:search :total] "?")))

(rf/reg-sub
  :search/offset
  (fn [db]
    (get-in db [:search :offset] 0)))

(rf/reg-sub
  :search/page-size
  (fn [db]
    (get-in db [:search :page-size] 100)))

(rf/reg-sub
  :search/sort-key
  (fn [db]
    (get-in db [:search :sort-key] :price)))

(rf/reg-sub
  :search/sort-order
  (fn [db]
    (get-in db [:search :sort-order] :asc)))

(rf/reg-sub
  :search/axies
  (fn [_]
    [(rf/subscribe [:search/raw-axies])
     (rf/subscribe [:search/sort-key])
     (rf/subscribe [:search/sort-order])
     (rf/subscribe [:search/offset])
     (rf/subscribe [:search/page-size])])
  (fn [[axies sort-key sort-order offset page-size]]
    (->> axies
         (sort-by sort-key)
         ((if (= :asc sort-order) identity reverse))
         (drop offset)
         (take page-size))))

(rf/reg-sub
  :auto-battle/dollars-per-month
  (fn [_]
    10))

(rf/reg-sub
  :auto-battle/num-months
  (fn [db]
    (get-in db [:auto-battle :num-months] 1)))

(rf/reg-sub
  :auto-battle/token
  (fn [db]
    (get-in db [:auto-battle :token])))

(rf/reg-sub
  :auto-battle/until
  (fn [_]
    [(rf/subscribe [:time/now])
     (rf/subscribe [:auto-battle/num-months])])
  (fn [[now num-months]]
    (.add (js/moment now) num-months "months")))

(rf/reg-sub
  :cryptonator/ticker
  (fn [db [_ ticker]]
    (get-in db [:cryptonator ticker])))
