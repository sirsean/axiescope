(ns axiescope.events
  (:require
   [re-frame.core :as rf]
   [clojure.string :as string]
   [cuerdas.core :refer [format]]
   [cljs-await.core :refer [await]]
   [cljs.core.async :refer [<!]]
   [camel-snake-kebab.core :refer [->kebab-case-keyword]]
   [camel-snake-kebab.extras :refer [transform-keys]]
   [ajax.core :as ajax]
   [axiescope.db :as db]
   )
  (:require-macros
    [cljs.core.async.macros :refer [go]]
    ))

(rf/reg-fx
  :http-get
  (fn [{:keys [url handler err-handler response-format]
        :or {response-format :json}}]
    (println :http-get response-format url)
    (ajax/GET
      url
      {:response-format response-format
       :keywords? true
       :error-handler (fn [err]
                        (if err-handler
                          (rf/dispatch (conj err-handler err))
                          (println "error" err)))
       :handler #(rf/dispatch (conj handler (transform-keys ->kebab-case-keyword %)))})))

(rf/reg-fx
  :blockchain/enable
  (fn [{:keys [eth handler]}]
    (go
      (let [[err eth-addrs] (<! (await (.enable eth)))]
        (when err
          (println "uhoh, enable failed" err))
        (rf/dispatch [:blockchain/got-addrs eth-addrs handler])))))

(defmulti set-active-panel
  (fn [cofx [_ panel :as event]]
    panel))

(defmethod set-active-panel :default
  [{:keys [db]} [_ panel]]
  {:db (assoc db :active-panel panel)})

(defmethod set-active-panel :home-panel
  [{:keys [db]} [_ panel]]
  {:db (assoc db :active-panel panel)})

(defmethod set-active-panel :my-axies-panel
  [{:keys [db]} [_ panel]]
  {:db (assoc db :active-panel panel)
   :blockchain/enable {:eth (:eth db)
                       :handler ::fetch-my-axies}})

(defmethod set-active-panel :breedable-panel
  [{:keys [db]} [_ panel]]
  {:db (assoc db :active-panel panel)
   :blockchain/enable {:eth (:eth db)
                       :handler ::fetch-my-axies}})

(defmethod set-active-panel :teams-panel
  [{:keys [db]} [_ panel]]
  {:db (assoc db :active-panel panel)
   :blockchain/enable {:eth (:eth db)
                       :handler :teams/fetch-teams}})

(defmethod set-active-panel :unassigned-panel
  [{:keys [db]} [_ panel]]
  {:db (assoc db :active-panel panel)
   :blockchain/enable {:eth (:eth db)
                       :handler :teams/fetch-unassigned}})

(defmethod set-active-panel :multi-assigned-panel
  [{:keys [db]} [_ panel]]
  {:db (assoc db :active-panel panel)
   :blockchain/enable {:eth (:eth db)
                       :handler :teams/fetch-teams}})

(rf/reg-event-fx
  ::set-active-panel
  (fn [cofx [_ active-panel :as event]]
    (set-active-panel cofx event)))

(rf/reg-event-fx
  ::initialize-db
  (fn [_ _]
    {:db db/default-db}))

(rf/reg-event-fx
  :blockchain/got-addrs
  (fn [{:keys [db]} [_ eth-addrs handler]]
    (cond->
      {:db (assoc db :eth-addr (first eth-addrs))}
      (some? handler)
      (merge {:dispatch [handler]}))))

(rf/reg-event-fx
  :teams/fetch-unassigned
  (fn [_ _]
    {:dispatch-n [[::fetch-my-axies]
                  [:teams/fetch-teams]]}))

(rf/reg-event-fx
  ::fetch-my-axies
  (fn [{:keys [db]} _]
    {:db (-> db
             (assoc-in [:my-axies :loading?] true)
             (assoc-in [:my-axies :axies] []))
     :dispatch [::fetch-my-axies-page]}))

(rf/reg-event-fx
  ::fetch-my-axies-page
  (fn [{:keys [db]} [_ total-axies]]
    (let [axies (get-in db [:my-axies :axies])]
      (if (or (nil? total-axies)
              (< (count axies) total-axies))
        {:http-get {:url (format "https://axieinfinity.com/api/v2/addresses/%s/axies?a=1&offset=%s"
                                 (:eth-addr db) (count axies))
                    :handler [::got-my-axies-page]}}

        {:dispatch [::got-my-axies axies]}))))

(rf/reg-event-fx
  ::got-my-axies-page
  (fn [{:keys [db]} [_ {:keys [total-axies axies]}]]
    {:db (update-in db [:my-axies :axies] concat axies)
     :dispatch [::fetch-my-axies-page total-axies]}))

(rf/reg-event-db
  ::got-my-axies
  (fn [db [_ axies]]
    (-> db
        (assoc-in [:my-axies :loading?] false)
        (assoc-in [:my-axies :axies] axies))))

(rf/reg-event-db
  ::set-my-axies-sort-key
  (fn [db [_ sort-key]]
    (assoc-in db [:my-axies :sort-key] sort-key)))

(rf/reg-event-fx
  ::fetch-axie
  (fn [{:keys [db]} [_ axie-id handler]]
    {:db db
     :http-get {:url (format "https://axieinfinity.com/api/v2/axies/%s?lang=en" axie-id)
                :handler [handler]}}))

(rf/reg-event-db
  ::got-axie
  (fn [db [_ axie]]
    db))

(rf/reg-event-fx
  ::simulate-battle
  (fn [{:keys [db]} [_ atk-id def-id]]
    {:db db
     :dispatch-n [[::fetch-axie atk-id ::got-bs-attacker]
                  [::fetch-axie def-id ::got-bs-defender]]}))

(rf/reg-event-db
  ::got-bs-attacker
  (fn [db [_ axie]]
    (assoc-in db [:battle-simulator :attacker] axie)))

(rf/reg-event-db
  ::got-bs-defender
  (fn [db [_ axie]]
    (assoc-in db [:battle-simulator :defender] axie)))

(rf/reg-event-fx
  :teams/fetch-teams
  (fn [{:keys [db]} _]
    {:db (-> db
             (assoc-in [:teams :loading?] true)
             (assoc-in [:teams :teams] []))
     :dispatch [:teams/fetch-teams-page]}))

(rf/reg-event-fx
  :teams/fetch-teams-page
  (fn [{:keys [db]} [_ total]]
    (let [teams (get-in db [:teams :teams])]
      (if (or (nil? total)
              (< (count teams) total))
        {:http-get {:url (format "https://api.axieinfinity.com/v1/battle/teams/?address=%s&offset=%s&count=47&no_limit=1"
                                 (:eth-addr db) (count teams))
                    :handler [:teams/got-teams-page]}}

        {:dispatch [:teams/got-teams teams]}))))

(rf/reg-event-fx
  :teams/got-teams-page
  (fn [{:keys [db]} [_ {:keys [total teams]}]]
    (let [axie-ids (->> teams (mapcat :team-members) (map :axie-id))]
      {:db (update-in db [:teams :teams] concat teams)
       :dispatch-n (-> [[:teams/fetch-teams-page total]
                        [:teams/fetch-activity-points axie-ids]]
                       (concat (map (fn [axie-id]
                                      [::fetch-axie axie-id :teams/got-axie])
                                    axie-ids)))})))
(rf/reg-event-fx
  :teams/got-teams
  (fn [{:keys [db]} [_ teams]]
      {:db (-> db
               (assoc-in [:teams :loading?] false)
               (assoc-in [:teams :teams] teams))}))

(rf/reg-event-fx
  :teams/fetch-activity-points
  (fn [{:keys [db]} [_ axie-ids]]
    {:http-get {:url (format "https://api.axieinfinity.com/v1/battle/battle/activity-point?%s"
                             (->> axie-ids
                                  (filter some?)
                                  (map (partial format "axieId=%s"))
                                  (string/join "&")))
                :handler [:teams/got-activity-points]}}))

(rf/reg-event-db
  :teams/got-activity-points
  (fn [db [_ points]]
    (let [ap-map (reduce
                   (fn [m {:keys [axie-id activity-point]}]
                     (assoc m axie-id activity-point))
                   {}
                   points)]
      (-> db
          (update-in [:teams :axie-id->activity-points]
                     merge
                     ap-map)))))

(rf/reg-event-db
  :teams/got-axie
  (fn [db [_ axie]]
    (assoc-in db [:teams :axie-db (:id axie)] axie)))
