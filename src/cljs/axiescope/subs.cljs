(ns axiescope.subs
  (:require
   [re-frame.core :as rf]
   [clojure.string :as string]
   [cljsjs.moment]
   [cljs-web3.core :as web3]
   [axiescope.axie :refer [adjust-axie attach-purity]]
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
  :axiescope/account
  (fn [db]
    (get-in db [:axiescope :account])))

(rf/reg-sub
  :axiescope/logged-in?
  (fn [_]
    [(rf/subscribe [:axiescope/account])])
  (fn [[account]]
    (some? account)))

(rf/reg-sub
  :axiescope.prices.family-tree/loading?
  (fn [db]
    (get-in db [:axiescope :prices :family-tree :loading?] false)))

(rf/reg-sub
  :axiescope.prices.family-tree/tiers
  (fn [db]
    (get-in db [:axiescope :prices :family-tree :tiers] [])))

(rf/reg-sub
  :axiescope.family-tree/views
  (fn [db]
    (get-in db [:axiescope :family-tree :views] [])))

(rf/reg-sub
  :axiescope.family-tree/loading?
  (fn [db]
    (get-in db [:axiescope :family-tree :loading?] false)))

(rf/reg-sub
  :axiescope.family-tree/error
  (fn [db]
    (get-in db [:axiescope :family-tree :error] nil)))

(rf/reg-sub
  :axiescope.family-tree/axie-id
  (fn [db]
    (get-in db [:axiescope :family-tree :axie-id])))

(rf/reg-sub
  :axiescope.family-tree/tree
  (fn [db]
    (get-in db [:axiescope :family-tree :tree])))

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
  :axie/axie-id
  (fn [db]
    (get-in db [:axie :id])))

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
  :axie/db
  (fn [db]
    (get-in db [:axie :db] {})))

(rf/reg-sub
  :axie/axie
  (fn [[_ axie-id]]
    [(rf/subscribe [:axie/db])
     (rf/subscribe [:identity axie-id])])
  (fn [[axie-db axie-id]]
    (-> axie-db
        (get axie-id)
        adjust-axie)))

(defn family-axie
  [axie]
  (-> axie
      attach-purity
      (select-keys [:id :name :image :title :class
                    :sire-id :matron-id :purity])))

(defn generate-family-tree
  [axie-db {:keys [sire-id matron-id] :as axie}]
  (cond-> axie
    (and (some? sire-id) (not (zero? sire-id)))
    (assoc :sire (generate-family-tree
                   axie-db
                   (family-axie (get axie-db (str sire-id)))))
    (and (some? matron-id) (not (zero? matron-id)))
    (assoc :matron (generate-family-tree
                     axie-db
                     (family-axie (get axie-db (str matron-id)))))))

(rf/reg-sub
  :axie/family-tree
  (fn [[_ axie-id]]
    [(rf/subscribe [:axie/db])
     (rf/subscribe [:identity axie-id])])
  (fn [[axie-db axie-id]]
    (generate-family-tree
      axie-db
      (family-axie (get axie-db (str axie-id))))))

(rf/reg-sub
  :axie/family-tree-expansions
  (fn [db]
    (get-in db [:axie :family-tree-expansions] {})))

(rf/reg-sub
  :axie/family-tree-expanded?
  (fn [[_ axie-id]]
    [(rf/subscribe [:axie/family-tree-expansions])
     (rf/subscribe [:identity axie-id])])
  (fn [[expansions axie-id]]
    (get expansions (str axie-id))))

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
     (rf/subscribe [:axie/db])])
  (fn [[teams axie-id->activity-points axie-db]]
    (->> teams
         (map (fn [team]
                (-> team
                    (update :team-members
                            (partial map (fn [{:keys [axie-id] :as a}]
                                           (assoc a
                                                  :axie (adjust-axie (get axie-db (str axie-id)))
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
     (rf/subscribe [:axie/db])])
  (fn [[team-id teams axie-db]]
    (->> teams
         (filter (fn [t] (= team-id (:team-id t))))
         first
         :team-members
         (map (comp str :axie-id))
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
         (filter (fn [{:keys [morph-to-petite]}]
                   (<= 0 (.diff now morph-to-petite "seconds")))))))

(rf/reg-sub
  :my-axies/petite
  (fn [_]
    [(rf/subscribe [:time/now])
     (rf/subscribe [:my-axies/raw-axies])])
  (fn [[now axies]]
    (->> axies
         (filter (fn [{:keys [stage]}] (= stage 3)))
         (filter (fn [{:keys [morph-to-adult]}]
                   (<= 0 (.diff now morph-to-adult "seconds")))))))

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
  :search/filtered-axies
  (fn [_]
    [(rf/subscribe [:search/raw-axies])
     (rf/subscribe [:search/filters])])
  (fn [[axies filters]]
    (->> axies
         (filter (fn [a]
                   (and (<= (:min-purity filters) (:purity a))
                        (>= (:max-breed-count filters) (:breed-count a))))))))

(rf/reg-sub
  :search/count
  (fn [_]
    [(rf/subscribe [:search/raw-axies])])
  (fn [[axies]]
    (count axies)))

(rf/reg-sub
  :search/filtered-count
  (fn [_]
    [(rf/subscribe [:search/filtered-axies])])
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
  :search/min-purity
  (fn [db]
    (get-in db [:search :filter :min-purity] 0)))

(rf/reg-sub
  :search/max-breed-count
  (fn [db]
    (get-in db [:search :filter :max-breed-count] 7)))

(rf/reg-sub
  :search/filters
  (fn [_]
    [(rf/subscribe [:search/min-purity])
     (rf/subscribe [:search/max-breed-count])])
  (fn [[min-purity max-breed-count]]
    {:min-purity min-purity
     :max-breed-count max-breed-count}))

(rf/reg-sub
  :search/axies
  (fn [_]
    [(rf/subscribe [:search/filtered-axies])
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
  :auto-battle/price-tiers
  (fn [_]
    [{:max-teams 3 :dollars-per-month 0}
     {:max-teams 10 :dollars-per-month 3}
     {:max-teams 30 :dollars-per-month 6}
     {:max-teams 100 :dollars-per-month 10}
     {:max-teams nil :dollars-per-month 20}]))

(rf/reg-sub
  :auto-battle/current-tier-index
  (fn [db]
    (get-in db [:auto-battle :current-tier-index] 0)))

(rf/reg-sub
  :auto-battle/current-tier
  (fn [_]
    [(rf/subscribe [:auto-battle/price-tiers])
     (rf/subscribe [:auto-battle/current-tier-index])])
  (fn [[tiers index]]
    (nth tiers index)))

(rf/reg-sub
  :auto-battle/dollars-per-month
  (fn [_]
    [(rf/subscribe [:auto-battle/current-tier])])
  (fn [[tier]]
    (:dollars-per-month tier)))

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

(rf/reg-sub
  :land/profile
  (fn [db]
    (get-in db [:land :profile])))

(rf/reg-sub
  :land/logged-in?
  (fn [_]
    [(rf/subscribe [:land/profile])])
  (fn [[profile]]
    (some? profile)))

(rf/reg-sub
  :land/items-loading?
  (fn [db]
    (get-in db [:land :items :loading?] false)))

(defn item-rarity-value
  [rarity]
  (case rarity
    "common" 1
    "rare" 2
    "epic" 3
    "mystic" 4
    0))

(rf/reg-sub
  :land/total-items
  (fn [db]
    (get-in db [:land :items :total] "?")))

(rf/reg-sub
  :land/raw-items
  (fn [db]
    (get-in db [:land :items :items] [])))

(rf/reg-sub
  :land/items-count
  (fn [_]
    [(rf/subscribe [:land/raw-items])])
  (fn [[items]]
    (count items)))

(rf/reg-sub
  :land/deduped-items
  (fn [_]
    [(rf/subscribe [:land/raw-items])])
  (fn [[items]]
    (->> items
         (group-by :item-alias)
         (map (fn [[item-alias same-items]]
                (let [item (first same-items)]
                  (-> item
                      (update :name string/trim)
                      (assoc :all-items same-items
                             :num-items (count same-items)
                             :rarity-value (item-rarity-value (:rarity item))))))))))

(rf/reg-sub
  :land-items/sort-key
  (fn [db]
    (get-in db [:land :items :sort-key] :name)))

(rf/reg-sub
  :land-items/sort-order
  (fn [db]
    (get-in db [:land :items :sort-order] :asc)))

(rf/reg-sub
  :land/items
  (fn [_]
    [(rf/subscribe [:land/deduped-items])
     (rf/subscribe [:land-items/sort-key])
     (rf/subscribe [:land-items/sort-order])])
  (fn [[items sort-key sort-order]]
    (->> items
         (sort-by sort-key)
         ((if (= :asc sort-order) identity reverse)))))

(rf/reg-sub
  :land/market-loading?
  (fn [db]
    (get-in db [:land :market :loading?] false)))

(rf/reg-sub
  :land-market/total
  (fn [db]
    (get-in db [:land :market :total] "?")))

(rf/reg-sub
  :land/raw-market
  (fn [db]
    (map (fn [item]
           (-> item
               (assoc :rarity-value (-> item :rarity item-rarity-value))
               (update :current-price (comp (partial * 1e-18M) web3/to-decimal))))
         (get-in db [:land :market :items]))))

(rf/reg-sub
  :land/market-count
  (fn [_]
    [(rf/subscribe [:land/raw-market])])
  (fn [[items]]
    (count items)))

(rf/reg-sub
  :land-market/sort-key
  (fn [db]
    (get-in db [:land :market :sort-key] :current-price)))

(rf/reg-sub
  :land-market/sort-order
  (fn [db]
    (get-in db [:land :market :sort-order] :asc)))

(rf/reg-sub
  :land-market/offset
  (fn [db]
    (get-in db [:land :market :offset] 0)))

(rf/reg-sub
  :land-market/page-size
  (fn [db]
    (get-in db [:land :market :page-size] 100)))

(rf/reg-sub
  :land/market
  (fn [_]
    [(rf/subscribe [:land/raw-market])
     (rf/subscribe [:land-market/sort-key])
     (rf/subscribe [:land-market/sort-order])
     (rf/subscribe [:land-market/offset])
     (rf/subscribe [:land-market/page-size])])
  (fn [[items sort-key sort-order offset page-size]]
    (->> items
         (sort-by sort-key)
         ((if (= :asc sort-order) identity reverse))
         (drop offset)
         (take page-size))))

(rf/reg-sub
  :valuation/sort-key
  (fn [db]
    (get-in db [:valuation :sort-key] :name)))

(rf/reg-sub
  :valuation/sort-order
  (fn [db]
    (get-in db [:valuation :sort-order] :asc)))

(rf/reg-sub
  :valuation/items
  (fn [_]
    [(rf/subscribe [:land/deduped-items])
     (rf/subscribe [:land/raw-market])
     (rf/subscribe [:valuation/sort-key])
     (rf/subscribe [:valuation/sort-order])])
  (fn [[items market sort-key sort-order]]
    (let [market-values (->> market
                             (group-by :alias)
                             (map (fn [[k v]]
                                    [k (map :current-price v)]))
                             (into {}))]
      (->> items
           (map (fn [item]
                  (let [mv (get market-values (:item-alias item))]
                    (assoc item
                           :min-price (apply min mv)
                           :max-price (apply max mv)
                           :avg-price (/ (apply + mv) (count mv))))))
           (sort-by sort-key)
           ((if (= :asc sort-order) identity reverse))))))
