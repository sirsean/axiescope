(ns axiescope.subs
  (:require
   [re-frame.core :as rf]
   [clojure.string :as string]
   [clojure.set :refer [rename-keys]]
   [cljsjs.moment]
   [cljs-web3.core :as web3]
   [axiescope.axie :refer [adjust-axie attach-purity]]
   [axiescope.battle :as battle]
   [axiescope.genes :as genes]))

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
  :axie/loading?
  (fn [db]
    (get-in db [:axie :loading?])))

(rf/reg-sub
  :axie/axie-id
  (fn [db]
    (get-in db [:axie :id])))

(rf/reg-sub
  :my-axies
  (fn [db]
    (get db :my-axies {})))

(rf/reg-sub
  :my-axies/loading?
  (fn [_]
    [(rf/subscribe [:my-axies])])
  (fn [[my-axies]]
    (get-in my-axies [:loading?])))


(rf/reg-sub
  :my-axies/sort-key
  (fn [_]
    [(rf/subscribe [:my-axies])])
  (fn [[my-axies]]
    (get-in my-axies [:sort-key] :id)))

(rf/reg-sub
  :my-axies/sort-order
  (fn [_]
    [(rf/subscribe [:my-axies])])
  (fn [[my-axies]]
    (get-in my-axies [:sort-order] :desc)))

(rf/reg-sub
  :axies/unadjusted
  (fn [_]
    [(rf/subscribe [:my-axies])])
  (fn [[my-axies]]
    (get-in my-axies [:axies] [])))

(rf/reg-sub
  :my-axies/raw-axies
  (fn [_]
    [(rf/subscribe [:axies/unadjusted])])
  (fn [[axies]]
    (map adjust-axie axies)))

(rf/reg-sub
  :my-axies/count
  (fn [_]
    [(rf/subscribe [:my-axies/raw-axies])])
  (fn [[axies]]
    (count axies)))

(rf/reg-sub
  :my-axies/total
  (fn [_]
    [(rf/subscribe [:my-axies])])
  (fn [[my-axies]]
    (get-in my-axies [:total] "?")))

(rf/reg-sub
  :my-axies/offset
  (fn [_]
    [(rf/subscribe [:my-axies])])
  (fn [[my-axies]]
    (get-in my-axies [:offset] 0)))

(rf/reg-sub
  :my-axies/page-size
  (fn [_]
    [(rf/subscribe [:my-axies])])
  (fn [[my-axies]]
    (get-in my-axies [:page-size] 100)))

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
  :breedable
  (fn [db]
    (get db :breedable {})))

(rf/reg-sub
  :breedable/axies
  (fn [_]
    [(rf/subscribe [:my-axies/raw-axies])])
  (fn [[axies]]
    (->> axies
         (filter :breedable))))

(rf/reg-sub
  :breedable/sire
  (fn [_]
    [(rf/subscribe [:breedable])])
  (fn [[bdb]]
    (get-in bdb [:sire])))

(rf/reg-sub
  :breedable/total
  (fn [_]
    [(rf/subscribe [:breedable/axies])])
  (fn [[axies]]
    (count axies)))

(rf/reg-sub
  :breedable/offset
  (fn [_]
    [(rf/subscribe [:breedable])])
  (fn [[bdb]]
    (get-in bdb [:offset] 0)))

(rf/reg-sub
  :breedable/page-size
  (fn [_]
    [(rf/subscribe [:breedable])])
  (fn [[bdb]]
    (get-in bdb [:page-size] 25)))

(rf/reg-sub
  :my-axies/breedable
  (fn [_]
    [(rf/subscribe [:breedable/axies])
     (rf/subscribe [:my-axies/sort-key])
     (rf/subscribe [:my-axies/sort-order])
     (rf/subscribe [:breedable/offset])
     (rf/subscribe [:breedable/page-size])
     (rf/subscribe [:breedable/sire])
     (rf/subscribe [:breed-calc/quick])])
  (fn [[axies sort-key sort-order offset page-size sire quick-db]]
    (->> axies
         (sort-by sort-key)
         ((if (= :asc sort-order) identity reverse))
         (drop offset)
         (take page-size)
         (map (fn [{:keys [id] :as axie}]
                (assoc axie
                       :can-breed-with-sire? (genes/can-breed? sire axie)
                       :quick-calc (get quick-db #{(:id sire) id})
                       :selected-sire? (= id (:id sire))))))))

(rf/reg-sub
  :teams
  (fn [db]
    (get db :teams {})))

(rf/reg-sub
  :teams/loading?
  (fn [_]
    [(rf/subscribe [:teams])])
  (fn [[tdb]]
    (get-in tdb [:loading?])))

(rf/reg-sub
  :teams/raw-teams
  (fn [_]
    [(rf/subscribe [:teams])])
  (fn [[tdb]]
    (get-in tdb [:teams])))

(rf/reg-sub
  :teams/count
  (fn [_]
    [(rf/subscribe [:teams/raw-teams])])
  (fn [[teams]]
    (count teams)))

(rf/reg-sub
  :teams/total
  (fn [_]
    [(rf/subscribe [:teams])])
  (fn [[tdb]]
    (get-in tdb [:total] "?")))

(rf/reg-sub
  :teams/axie-id->activity-points
  (fn [_]
    [(rf/subscribe [:teams])])
  (fn [[tdb]]
    (get-in tdb [::axie-id->activity-points] {})))

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

(rf/reg-sub
  :body-parts
  (fn [db]
    (get-in db [:body-parts] [])))

(rf/reg-sub
  :body-parts/name-map
  (fn [db]
    (get-in db [:name->body-part])))

(rf/reg-sub
  :breed-calc
  (fn [db]
    (get db :breed-calc)))

(rf/reg-sub
  :breed-calc/sire
  (fn [_]
    [(rf/subscribe [:breed-calc])])
  (fn [[breed-calc]]
    (get breed-calc :sire)))

(rf/reg-sub
  :breed-calc/matron
  (fn [_]
    [(rf/subscribe [:breed-calc])])
  (fn [[breed-calc]]
    (get breed-calc :matron)))

(rf/reg-sub
  :breed-calc/can-breed?
  (fn [[_ sire matron]]
    [(rf/subscribe [:identity sire])
     (rf/subscribe [:identity matron])])
  (fn [[sire matron]]
    (genes/can-breed? sire matron)))

(rf/reg-sub
  :breed-calc/prediction
  (fn [[_ sire matron]]
    [(rf/subscribe [:body-parts/name-map])
     (rf/subscribe [:identity sire])
     (rf/subscribe [:identity matron])])
  (fn [[body-parts sire matron]]
    (genes/predict-breed body-parts sire matron)))

(rf/reg-sub
  :breed-calc/predict-score
  (fn [[_ sire matron]]
    [(rf/subscribe [:breed-calc/prediction sire matron])])
  (fn [[prediction]]
    (genes/predict-score prediction)))

(rf/reg-sub
  :breed-calc/quick
  (fn [db]
    (get db :breed-calc/quick)))

(rf/reg-sub
  :breed-calc/quick-calc
  (fn [[_ sire-id matron-id]]
    [(rf/subscribe [:breed-calc/quick])
     (rf/subscribe [:identity sire-id])
     (rf/subscribe [:identity matron-id])])
  (fn [[quick-db sire-id matron-id]]
    (get quick-db #{sire-id matron-id})))

(rf/reg-sub
  :card-rankings
  (fn [db]
    (get db :card-rankings {})))

(rf/reg-sub
  :card-rankings/loading?
  (fn [_]
    [(rf/subscribe [:card-rankings])])
  (fn [[cr]]
    (get cr :loading? false)))

(rf/reg-sub
  :card-rankings/rankings
  (fn [_]
    [(rf/subscribe [:card-rankings])])
  (fn [[cr]]
    (get-in cr [:rankings])))

(rf/reg-sub
  :card-rankings/pair
  (fn [_]
    [(rf/subscribe [:card-rankings])])
  (fn [[cr]]
    (get-in cr [:pair])))

(rf/reg-sub
  :cards
  (fn [db]
    (get db :cards {})))

(rf/reg-sub
  :cards/loading?
  (fn [_]
    [(rf/subscribe [:cards])])
  (fn [[c]]
    (get c :loading? false)))

(rf/reg-sub
  :cards/all
  (fn [_]
    [(rf/subscribe [:cards])])
  (fn [[c]]
    (get c :all {})))

(rf/reg-sub
  :cards/sort-key
  (fn [_]
    [(rf/subscribe [:cards])])
  (fn [[c]]
    (get c :sort-key :id)))

(rf/reg-sub
  :cards/sort-order
  (fn [_]
    [(rf/subscribe [:cards])])
  (fn [[c]]
    (get c :sort-order :desc)))

(rf/reg-sub
  :cards/selector
  (fn [_]
    [(rf/subscribe [:cards])])
  (fn [[c]]
    (get c :selector :all)))

(rf/reg-sub
  :cards/search
  (fn [_]
    [(rf/subscribe [:cards])])
  (fn [[c]]
    (get c :search "")))

(rf/reg-sub
  :cards/list
  (fn [_]
    [(rf/subscribe [:cards/all])
     (rf/subscribe [:cards/sort-key])
     (rf/subscribe [:cards/sort-order])
     (rf/subscribe [:cards/selector])
     (rf/subscribe [:cards/search])])
  (fn [[all sort-key sort-order selector search]]
    (->> all
         vals
         (filter
           (fn [card]
             (or (string/blank? search)
                 (string/includes?
                   (string/lower-case (:skill-name card))
                   (string/lower-case search))
                 (string/includes?
                   (string/lower-case (:part-name card))
                   (string/lower-case search)))))
         (filter
           (fn [card]
             (or (= :all selector)
                 (string/includes? (:id card) (name selector)))))
         (sort-by sort-key)
         ((if (= :asc sort-order) identity reverse)))))
