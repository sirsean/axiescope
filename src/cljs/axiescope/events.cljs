(ns axiescope.events
  (:require
   [re-frame.core :as rf]
   [cuerdas.core :refer [format]]
   [ajax.core :as ajax]
   [axiescope.db :as db]
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
       :handler #(rf/dispatch (conj handler %))})))

(defmulti set-active-panel
  (fn [cofx [_ panel :as event]]
    panel))

(defmethod set-active-panel :default
  [{:keys [db]} [_ panel]]
  {:db (assoc db :active-panel panel)})

(defmethod set-active-panel :home-panel
  [{:keys [db]} [_ panel]]
  {:db (assoc db :active-panel panel)})

(rf/reg-event-fx
  ::set-active-panel
  (fn [cofx [_ active-panel :as event]]
    (set-active-panel cofx event)))

(rf/reg-event-db
 ::initialize-db
 (fn [_ _]
   db/default-db))

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
