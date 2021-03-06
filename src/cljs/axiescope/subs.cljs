(ns axiescope.subs
  (:require
   [re-frame.core :as rf]
   [clojure.string :as string]
   [clojure.set :refer [rename-keys]]
   [cljsjs.moment]
   [cljs-web3.core :as web3]
   [axiescope.axie :refer [adjust-axie attach-purity]]
   [axiescope.util :refer [ranking-type->key]]
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

(defn sum-ratings
  [axie id->rating]
  (->
    (->> axie
         :parts
         (mapcat :abilities)
         (map :id)
         (map keyword)
         (map id->rating)
         (reduce +))
    (- 4000)))

(rf/reg-sub
  :my-axies/raw-axies
  (fn [_]
    [(rf/subscribe [:axies/unadjusted])
     (rf/subscribe [:card-rankings/id->rating :all])
     (rf/subscribe [:card-rankings/id->rating :attack])
     (rf/subscribe [:card-rankings/id->rating :defense])])
  (fn [[axies id->all-rating id->attack-rating id->defense-rating]]
    (->> axies
         (map adjust-axie)
         (map (fn [axie]
                (assoc axie
                       :all-rating (sum-ratings axie id->all-rating)
                       :attack-rating (sum-ratings axie id->attack-rating)
                       :defense-rating (sum-ratings axie id->defense-rating)))))))

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

(rf/reg-sub
  :axie/cards
  (fn [[_ axie-id]]
    [(rf/subscribe [:axie/axie axie-id])
     (rf/subscribe [:cards/all])])
  (fn [[axie cards]]
    (->> axie
         :parts
         (mapcat :abilities)
         (map (comp keyword :id))
         (keep (partial get cards)))))

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
  :card-rankings/ranking-type
  (fn [_]
    [(rf/subscribe [:card-rankings])])
  (fn [[cr]]
    (get cr :ranking-type :all)))

(rf/reg-sub
  :card-rankings/loading?
  (fn [_]
    [(rf/subscribe [:card-rankings])])
  (fn [[cr]]
    (get cr :loading? false)))

(rf/reg-sub
  :card-rankings/rankings
  (fn [[_ ranking-type]]
    [(rf/subscribe [:card-rankings])
     (rf/subscribe [:identity ranking-type])])
  (fn [[cr ranking-type]]
    (get cr (ranking-type->key ranking-type) [])))

(rf/reg-sub
  :card-rankings/id->rating
  (fn [[_ ranking-type]]
    [(rf/subscribe [:card-rankings/rankings ranking-type])])
  (fn [[rankings]]
    (->> rankings
         (map (fn [{:keys [id rating]}]
                [(keyword id) rating]))
         (into {}))))

(rf/reg-sub
  :card-rankings/pair
  (fn [_]
    [(rf/subscribe [:card-rankings])])
  (fn [[cr]]
    (get-in cr [:pair])))

(rf/reg-sub
  :combo-rankings
  (fn [db]
    (get db :combo-rankings {})))

(rf/reg-sub
  :combo-rankings/loading?
  (fn [_]
    [(rf/subscribe [:combo-rankings])])
  (fn [[cr]]
    (get cr :loading? false)))

(rf/reg-sub
  :combo-rankings/rankings
  (fn [_]
    [(rf/subscribe [:combo-rankings])])
  (fn [[cr]]
    (get cr :rankings [])))

(rf/reg-sub
  :combo-rankings/id->rating
  (fn [_]
    [(rf/subscribe [:combo-rankings/rankings])])
  (fn [[rankings]]
    (->> rankings
         (map (fn [{:keys [id rating]}]
                [(keyword id) rating]))
         (into {}))))

(rf/reg-sub
  :combo-rankings/pair
  (fn [_]
    [(rf/subscribe [:combo-rankings])])
  (fn [[cr]]
    (get cr :pair)))

(rf/reg-sub
  :combo-rankings/add-selections
  (fn [_]
    [(rf/subscribe [:combo-rankings])])
  (fn [[cr]]
    (get-in cr [:add :selected] {})))

(rf/reg-sub
  :combo-rankings/add-loading?
  (fn [_]
    [(rf/subscribe [:combo-rankings])])
  (fn [[cr]]
    (get-in cr [:add :loading?] false)))

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
