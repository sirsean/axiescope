(ns axiescope.events
  (:require
    [clojure.set :refer [rename-keys]]
    [re-frame.core :as rf]
    [district0x.re-frame.interval-fx]
    [cljs-web3.core]
    [cljs-web3.eth]
    [cljs-web3.personal]
    [clojure.string :as string]
    [cuerdas.core :refer [format]]
    [cljs-await.core :refer [await]]
    [cljs.core.async :refer [<!]]
    [camel-snake-kebab.core :refer [->kebab-case-keyword]]
    [camel-snake-kebab.extras :refer [transform-keys]]
    [ajax.core :as ajax]
    [cljsjs.moment]
    [axiescope.config :refer [api-host]]
    [axiescope.genes :as genes]
    [axiescope.db :as db]
    )
  (:require-macros
    [cljs.core.async.macros :refer [go]]
    ))

(rf/reg-fx
  :http-get
  (fn [{:keys [url handler err-handler response-format transform? headers]
        :or {response-format :json
             transform? true}}]
    (println :http-get response-format url)
    (ajax/GET
      url
      {:response-format response-format
       :keywords? true
       :headers headers
       :error-handler (fn [err]
                        (if err-handler
                          (rf/dispatch (conj err-handler err))
                          (println "error" err)))
       :handler (fn [result]
                  (rf/dispatch
                    (conj handler
                          (cond->> result
                            transform? (transform-keys ->kebab-case-keyword)))))})))

(rf/reg-fx
  :http-post
  (fn [{:keys [url handler err-handler response-format body headers]
        :or {response-format :json}}]
    (println :http-post response-format url)
    (ajax/POST
      url
      {:response-format response-format
       :keywords? true
       :headers headers
       :format :json
       :params body
       :error-handler (fn [err]
                        (if err-handler
                          (rf/dispatch (conj err-handler err))
                          (println "error" err)))
       :handler (fn [result]
                  (rf/dispatch
                    (conj handler (transform-keys ->kebab-case-keyword result))))})))

(rf/reg-fx
  :http-delete
  (fn [{:keys [url handler err-handler response-format headers]
        :or {response-format :json}}]
    (println :http-delete response-format url)
    (ajax/DELETE
      url
      {:response-format response-format
       :keywords? true
       :headers headers
       :format :json
       :error-handler (fn [err]
                        (if err-handler
                          (rf/dispatch (conj err-handler err))
                          (println "error" err)))
       :handler (fn [result]
                  (rf/dispatch
                    (conj handler (transform-keys ->kebab-case-keyword result))))})))

(rf/reg-fx
  :blockchain/enable
  (fn [{:keys [eth handlers]}]
    (go
      (let [[err eth-addrs] (<! (await (.enable eth)))]
        (when err
          (println "uhoh, enable failed" err))
        (rf/dispatch [:blockchain/got-addrs eth-addrs handlers])))))

(defn metamask-callback-fn
  [handler err-handler]
  (fn [err res]
    (if (some? err)
      (when (some? err-handler)
        (rf/dispatch [err-handler err]))
      (when (some? handler)
        (if (vector? handler)
          (rf/dispatch (conj handler res))
          (rf/dispatch [handler res]))))))

(rf/reg-fx
  :blockchain/sign
  (fn [{:keys [web3 handler err-handler addr data]}]
    (println "sign" addr data)
    (cljs-web3.personal/sign web3
                             data addr
                             (metamask-callback-fn handler err-handler))))

(rf/reg-fx
  :blockchain/send-eth
  (fn [{:keys [web3 handler err-handler from-addr to-addr value]}]
    (println "send" from-addr to-addr value)
    (cljs-web3.eth/send-transaction! web3
                                     {:from from-addr
                                      :to to-addr
                                      :value value}
                                     (metamask-callback-fn handler err-handler))))

(rf/reg-fx
  :blockchain/contract-call
  (fn [{:keys [contract-instance handler err-handler method args]}]
    (println "contract-call" method args)
    (apply cljs-web3.eth/contract-call
           (concat [contract-instance method]
                   args
                   [(metamask-callback-fn handler err-handler)]))))

(rf/reg-event-fx
  :contract/error
  (fn [_ [_ err]]
    (println "something failed" err)
    {}))

(defmulti set-active-panel
  (fn [cofx [_ panel :as event]]
    panel))

(defmethod set-active-panel :default
  [{:keys [db]} [_ panel]]
  {:db (assoc db :active-panel panel)})

(defmethod set-active-panel :home-panel
  [{:keys [db]} [_ panel]]
  {:db (assoc db :active-panel panel)})

(defmethod set-active-panel :axie-panel
  [{:keys [db]} [_ panel axie-id]]
  {:db (assoc db :active-panel panel)
   :dispatch [:axie/set-id axie-id]})

(defmethod set-active-panel :my-axies-panel
  [{:keys [db]} [_ panel]]
  {:db (assoc db :active-panel panel)
   :blockchain/enable {:eth (:eth db)
                       :handlers [:my-axies/fetch]}})

(defmethod set-active-panel :gallery-panel
  [{:keys [db]} [_ panel]]
  {:db (assoc db :active-panel panel)
   :blockchain/enable {:eth (:eth db)
                       :handlers [:my-axies/fetch]}})

(defmethod set-active-panel :breedable-panel
  [{:keys [db]} [_ panel]]
  {:db (assoc db :active-panel panel)
   :dispatch [:body-parts/fetch]
   :blockchain/enable {:eth (:eth db)
                       :handlers [:my-axies/fetch]}})

(defmethod set-active-panel :breed-calc-panel
  [{:keys [db]} [_ panel sire-id matron-id]]
  {:db (assoc db :active-panel panel)
   :dispatch-n [[:body-parts/fetch]
                [::fetch-axie sire-id {:handler :breed-calc/set-sire}]
                [::fetch-axie matron-id {:handler :breed-calc/set-matron}]]})

(defmethod set-active-panel :teams-panel
  [{:keys [db]} [_ panel]]
  {:db (assoc db :active-panel panel)
   :blockchain/enable {:eth (:eth db)
                       :handlers [:teams/fetch-teams]}})

(defmethod set-active-panel :unassigned-panel
  [{:keys [db]} [_ panel]]
  {:db (assoc db :active-panel panel)
   :blockchain/enable {:eth (:eth db)
                       :handlers [:my-axies/fetch
                                  :teams/fetch-teams]}})

(defmethod set-active-panel :multi-assigned-panel
  [{:keys [db]} [_ panel]]
  {:db (assoc db :active-panel panel)
   :blockchain/enable {:eth (:eth db)
                       :handlers [:my-axies/fetch
                                  :teams/fetch-teams]}})

(defmethod set-active-panel :morph-to-petite-panel
  [{:keys [db]} [_ panel]]
  {:db (assoc db :active-panel panel)
   :blockchain/enable {:eth (:eth db)
                       :handlers [:my-axies/fetch]}})

(defmethod set-active-panel :morph-to-adult-panel
  [{:keys [db]} [_ panel]]
  {:db (assoc db :active-panel panel)
   :blockchain/enable {:eth (:eth db)
                       :handlers [:my-axies/fetch]}})

(defmethod set-active-panel :multi-gifter-panel
  [{:keys [db]} [_ panel]]
  {:db (assoc db :active-panel panel)
   :blockchain/enable {:eth (:eth db)
                       :handlers [:my-axies/fetch]}})

(defmethod set-active-panel :search-panel
  [{:keys [db]} [_ panel]]
  {:db (assoc db :active-panel panel)
   :blockchain/enable {:eth (:eth db)
                       :handlers [:search/fetch]}})

(defmethod set-active-panel :auto-battle-panel
  [{:keys [db]} [_ panel]]
  {:db (assoc db :active-panel panel)
   :blockchain/enable {:eth (:eth db)}
   :dispatch [:axiescope.prices.auto-battle/fetch]})

(defmethod set-active-panel :land-panel
  [{:keys [db]} [_ panel]]
  {:db (assoc db :active-panel panel)
   :dispatch [:land/login]})

(defmethod set-active-panel :land-items-panel
  [{:keys [db]} [_ panel]]
  {:db (assoc db :active-panel panel)
   :dispatch [:land/login {:after-handlers [[:land/fetch-items]]}]})

(defmethod set-active-panel :land-market-panel
  [{:keys [db]} [_ panel]]
  {:db (assoc db :active-panel panel)
   :dispatch [:land/fetch-market]})

(defmethod set-active-panel :land-valuation-panel
  [{:keys [db]} [_ panel]]
  {:db (assoc db :active-panel panel)
   :dispatch [:land/login {:after-handlers [[:land/fetch-items]
                                            [:land/fetch-market]]}]})

(defmethod set-active-panel :lineage-panel
  [{:keys [db]} [_ panel]]
  {:db (assoc db :active-panel panel)
   :blockchain/enable {:eth (:eth db)
                       :handlers [:axiescope.prices.family-tree/fetch]}})

(rf/reg-event-fx
  ::set-active-panel
  (fn [cofx [_ active-panel :as event]]
    (set-active-panel cofx event)))

(rf/reg-event-fx
  ::initialize-db
  (fn [_ _]
    {:db db/default-db
     :dispatch-interval {:id :ticker
                         :dispatch [:tick/second]
                         :ms 1000}}))

(rf/reg-event-fx
  :tick/second
  (fn [_ _]
    {:dispatch-n [[:time/now]
                  [:contract/load-axie-abi]]}))

(rf/reg-event-db
  :time/now
  (fn [db]
    (assoc db :now (js/moment))))

(rf/reg-event-fx
  :blockchain/got-addrs
  (fn [{:keys [db]} [_ eth-addrs handlers]]
    (cond->
      {:db (assoc db :eth-addr (first eth-addrs))}
      (some? handlers)
      (merge {:dispatch-n (mapv #(if (vector? %) % (vector %)) handlers)}))))

(rf/reg-event-fx
  :contract/load-axie-abi
  (fn [{:keys [db]} _]
    (if (nil? (:contract/axie-instance db))
      {:http-get {:url "/js/axie-abi.json"
                  :transform? false
                  :handler [:contract/got-axie-abi]}}
      {})))

(rf/reg-event-db
  :contract/got-axie-abi
  (fn [db [_ abi]]
    (if (cljs-web3.eth/eth (aget js/window "web3"))
      (assoc db
             :contract/axie-instance (cljs-web3.eth/contract-at
                                       (aget js/window "web3")
                                       abi
                                       "0xF5b0A3eFB8e8E4c201e2A935F110eAaF3FFEcb8d"))
      db)))

(rf/reg-event-fx
  :eth/send-eth
  (fn [{:keys [db]} [_ to-addr eth-value]]
    {:blockchain/send-eth {:web3 (:web3 db)
                           :from-addr (:eth-addr db)
                           :to-addr to-addr
                           :value (cljs-web3.core/to-wei eth-value :ether)
                           :err-handler :contract/error
                           :handler :eth/sent}}))

(rf/reg-event-fx
  :eth/sent
  (fn [_ [_ resp]]
    (println "sent" resp)
    {}))

(rf/reg-event-fx
  :axiescope/auth
  (fn [{:keys [db]} [_ {:keys [after-handlers]}]]
    (if (some? (get-in db [:axiescope :token]))
      {:dispatch [:axiescope.account/register]}
      {:blockchain/sign {:web3 (:web3 db)
                         :addr (:eth-addr db)
                         :data "axiescope"
                         :handler [:axiescope/got-token
                                   {:after-handler [:axiescope.account/register
                                                    {:after-handlers after-handlers}]}]
                         :err-handler :contract/error}})))

(rf/reg-event-fx
  :axiescope/got-token
  (fn [{:keys [db]} [_ {:keys [after-handler]} token]]
    (cond-> {:db (assoc-in db [:axiescope :token] token)}
      (some? after-handler)
      (assoc :dispatch after-handler))))

(rf/reg-event-fx
  :axiescope.account/fetch
  (fn [{:keys [db]} _]
    {:http-get {:url (format "%s/api/account" api-host)
                :headers {"Authorization" (format "Bearer %s" (get-in db [:axiescope :token]))}
                :handler [:axiescope.account/got nil]
                :err-handler [:axiescope.account/error]}}))

(rf/reg-event-fx
  :axiescope.account/register
  (fn [{:keys [db]} [_ {:keys [after-handlers]}]]
    {:http-post {:url (format "%s/api/register" api-host)
                 :headers {"Authorization" (format "Bearer %s" (get-in db [:axiescope :token]))}
                 :handler [:axiescope.account/got {:after-handlers (conj after-handlers [:axiescope.account/fetch])}]
                 :err-handler [:axiescope.account/error]}}))

(rf/reg-event-fx
  :axiescope.account/got
  (fn [{:keys [db]} [_ {:keys [after-handlers]} account]]
    (cond-> {:db (assoc-in db [:axiescope :account] account)}
      (seq after-handlers)
      (assoc :dispatch-n after-handlers))))

(rf/reg-event-db
  :axiescope.account/error
  (fn [db [_ err]]
    (println "failed to get account" err)
    db))

(rf/reg-event-fx
  :axiescope.pay/family-tree
  (fn [{:keys [db]} [_ eth-value]]
    {:blockchain/send-eth {:web3 (:web3 db)
                           :from-addr (:eth-addr db)
                           :to-addr "0x560ebafd8db62cbdb44b50539d65b48072b98277"
                           :value (cljs-web3.core/to-wei eth-value :ether)
                           :err-handler :contract/error
                           :handler :axiescope.paid/family-tree}}))

(rf/reg-event-fx
  :axiescope.paid/family-tree
  (fn [{:keys [db]} [_ txid]]
    (println txid)
    {:http-post {:url (format "%s/api/pay" api-host)
                 :headers {"Authorization" (format "Bearer %s" (get-in db [:axiescope :token]))
                           "Content-Type" "application/json"}
                 :body {:product :family-tree
                        :txid txid}
                 :handler [:axiescope.payment-submitted/family-tree]}}))

(rf/reg-event-db
  :axiescope.payment-submitted/family-tree
  (fn [db _]
    (println "payment submitted")
    db))

(rf/reg-event-fx
  :axiescope.pay/auto-battle
  (fn [{:keys [db]} [_ max-teams eth-value]]
    {:blockchain/send-eth {:web3 (:web3 db)
                           :from-addr (:eth-addr db)
                           :to-addr "0x560ebafd8db62cbdb44b50539d65b48072b98277"
                           :value (cljs-web3.core/to-wei eth-value :ether)
                           :err-handler :contract/error
                           :handler [:axiescope.paid/auto-battle max-teams]}}))

(rf/reg-event-fx
  :axiescope.paid/auto-battle
  (fn [{:keys [db]} [_ max-teams txid]]
    (println txid)
    {:http-post {:url (format "%s/api/pay" api-host)
                 :headers {"Authorization" (format "Bearer %s" (get-in db [:axiescope :token]))
                           "Content-Type" "application/json"}
                 :body {:product :auto-battle
                        :txid txid
                        :max-teams max-teams}
                 :handler [:axiescope.payment-submitted/auto-battle]}}))

(rf/reg-event-db
  :axiescope.payment-submitted/auto-battle
  (fn [db _]
    (println "payment submitted")
    db))

(rf/reg-event-fx
  :axiescope.prices.family-tree/fetch
  (fn [{:keys [db]} _]
    {:db (-> db
             (assoc-in [:axiescope :prices :family-tree :loading?] true))
     :http-get {:url (format "%s/api/prices/family-tree" api-host)
                :handler [:axiescope.prices.family-tree/got]}}))

(rf/reg-event-db
  :axiescope.prices.family-tree/got
  (fn [db [_ tiers]]
    (-> db
        (assoc-in [:axiescope :prices :family-tree :loading?] false)
        (assoc-in [:axiescope :prices :family-tree :tiers] tiers))))

(rf/reg-event-fx
  :axiescope.prices.auto-battle/fetch
  (fn [{:keys [db]} _]
    {:db (-> db
             (assoc-in [:axiescpoe :prices :auto-battle :loading?] false))
     :http-get {:url (format "%s/api/prices/auto-battle" api-host)
                :handler [:axiescope.prices.auto-battle/got]}}))

(rf/reg-event-db
  :axiescope.prices.auto-battle/got
  (fn [db [_ tiers]]
    (-> db
        (assoc-in [:axiescope :prices :auto-battle :loading?] false)
        (assoc-in [:axiescope :prices :auto-battle :tiers] tiers))))

(rf/reg-event-fx
  :axiescope.family-tree.views/fetch
  (fn [{:keys [db]} _]
    {:http-get {:url (format "%s/api/family-tree/views" api-host)
                :headers {"Authorization" (format "Bearer %s" (get-in db [:axiescope :token]))}
                :handler [:axiescope.family-tree.views/got]}}))

(rf/reg-event-db
  :axiescope.family-tree.views/got
  (fn [db [_ views]]
    (assoc-in db [:axiescope :family-tree :views] views)))

(rf/reg-event-fx
  :axiescope.family-tree/fetch
  (fn [{:keys [db]} [_ axie-id]]
    {:db (-> db
             (assoc-in [:axiescope :family-tree :axie-id] axie-id)
             (assoc-in [:axiescope :family-tree :error] nil)
             (assoc-in [:axiescope :family-tree :loading?] true)
             (assoc-in [:axiescope :family-tree :tree] nil))
     :http-get {:url (format "%s/api/family-tree/%s" api-host axie-id)
                :headers {"Authorization" (format "Bearer %s" (get-in db [:axiescope :token]))}
                :handler [:axiescope.family-tree/got]
                :err-handler [:axiescope.family-tree/error]}}))

(rf/reg-event-fx
  :axiescope.family-tree/got
  (fn [{:keys [db]} [_ tree]]
    {:db (-> db
             (assoc-in [:axiescope :family-tree :loading?] false)
             (assoc-in [:axiescope :family-tree :tree] tree))
     :dispatch-n [[:axiescope.account/fetch]
                  [:axiescope.family-tree.views/fetch]]}))

(rf/reg-event-fx
  :axiescope.family-tree/error
  (fn [{:keys [db]} [_ err]]
    (println err)
    (println (:status err) (:status-text err))
    {:db (-> db
             (assoc-in [:axiescope :family-tree :loading?] false)
             (assoc-in [:axiescope :family-tree :error] err))}))

(rf/reg-event-db
  :axiescope.family-tree/clear
  (fn [db _]
    (-> db
        (assoc-in [:axiescope :family-tree :axie-id] nil)
        (assoc-in [:axiescope :family-tree :loading?] false)
        (assoc-in [:axiescope :family-tree :tree] nil))))

(rf/reg-event-fx
  :axiescope.auto-battle.account/fetch
  (fn [{:keys [db]} _]
    {:http-get {:url (format "%s/api/account/auto-battle" api-host)
                :headers {"Authorization" (format "Bearer %s" (get-in db [:axiescope :token]))}
                :handler [:axiescope.auto-battle.account/got]
                :err-handler [:axiescope.auto-battle.account/error]}}))

(rf/reg-event-db
  :axiescope.auto-battle.account/got
  (fn [db [_ account]]
    (-> db
        (assoc-in [:axiescope :auto-battle :account] account))))

(rf/reg-event-db
  :axiescope.auto-battle.account/error
  (fn [db _]
    db))

(rf/reg-event-fx
  :axiescope.auto-battle/signup
  (fn [{:keys [db]} _]
    (let [token (get-in db [:auto-battle :token])]
      {:http-post {:url (format "%s/api/auto-battle/signup" api-host)
                   :headers {"Authorization" (format "Bearer %s" (get-in db [:axiescope :token]))}
                   :body {:token token}
                   :handler [:axiescope.auto-battle.account/got]}})))

(rf/reg-event-fx
  :axiescope.auto-battle/delete
  (fn [{:keys [db]} _]
    {:http-delete {:url (format "%s/api/auto-battle/deactivate" api-host)
                   :headers {"Authorization" (format "Bearer %s" (get-in db [:axiescope :token]))}
                   :handler [:axiescope.auto-battle/deleted]}}))

(rf/reg-event-fx
  :axiescope.auto-battle/deleted
  (fn [{:keys [db]} _]
    {:db (-> db
             (assoc-in [:axiescope :auto-battle :account] nil))
     :dispatch [:axiescope.auto-battle.account/fetch]}))

(rf/reg-event-fx
  :body-parts/fetch
  (fn [{:keys [db]} _]
    (cond-> {}
      (nil? (:body-parts db))
      (merge
        {:http-get {:url  "https://axieinfinity.com/api/v2/body-parts?withMoveDetails=true"
                    :handler [:body-parts/got]}}))))

(rf/reg-event-db
  :body-parts/got
  (fn [db [_ body-parts]]
    (-> db
        (assoc :body-parts body-parts)
        (assoc :name->body-part
               (->> body-parts
                    (group-by :type)
                    (map (fn [[type parts]]
                           [(keyword type)
                            (->> parts
                                 (map (fn [{:keys [name] :as part}]
                                        [name (rename-keys part {:part-id :id})]))
                                 (into {}))]))
                    (into {}))))))

(rf/reg-event-fx
  :axie/set-id
  (fn [{:keys [db]} [_ axie-id {:keys [handler]}]]
    {:db (-> db
             (assoc-in [:axie :loading?] true)
             (assoc-in [:axie :family-tree-expansions] {(str axie-id) true})
             (assoc-in [:axie :id] (str axie-id)))
     :dispatch [::fetch-axie axie-id {:force? true
                                      :handler handler}]}))

(rf/reg-event-db
  :search/set-sort-key
  (fn [db [_ sort-key]]
    (-> db
        (assoc-in [:search :sort-key] sort-key)
        (assoc-in [:search :offset] 0))))

(rf/reg-event-db
  :search/set-sort-order
  (fn [db [_ sort-order]]
    (-> db
        (assoc-in [:search :sort-order] sort-order)
        (assoc-in [:search :offset] 0))))

(rf/reg-event-db
  :search/set-offset
  (fn [db [_ offset]]
    (assoc-in db [:search :offset] offset)))

(rf/reg-event-db
  :search/set-filter
  (fn [db [_ k v]]
    (assoc-in db [:search :filter k] v)))

(rf/reg-event-fx
  :search/fetch
  (fn [{:keys [db]} [_ force?]]
    (if (or (-> db :search :axies nil?)
            force?)
      {:db (-> db
               (assoc-in [:search :loading?] true)
               (assoc-in [:search :offset] 0)
               (assoc-in [:search :axies] []))
       :dispatch [:search/fetch-page]}
      {})))

(rf/reg-event-fx
  :search/fetch-page
  (fn [{:keys [db]} [_ total-axies]]
    (let [axies (get-in db [:search :axies])]
      (if (or (nil? total-axies)
              (< (count axies) total-axies))
        {:http-get {:url (format "https://axieinfinity.com/api/v2/axies?breedable&lang=en&offset=%s&sale=1&sorting=lowest_price"
                                 (count axies))
                    :handler [:search/got-page]}}

        {:dispatch [:search/got axies]}))))


(rf/reg-event-fx
  :search/got-page
  (fn [{:keys [db]} [_ {:keys [total-axies axies]}]]
    {:db (-> db
             (assoc-in [:search :total] total-axies)
             (update-in [:search :axies] concat axies))
     :dispatch [:search/fetch-page total-axies]}))

(rf/reg-event-db
  :search/got
  (fn [db [_ axies]]
    (-> db
        (assoc-in [:search :loading?] false)
        (assoc-in [:search :axie] axies))))

(rf/reg-event-fx
  :my-axies/fetch
  (fn [{:keys [db]} [_ force?]]
    (if (or (-> db :my-axies :axies nil?)
            force?)
      {:db (-> db
               (assoc-in [:my-axies :loading?] true)
               (assoc-in [:my-axies :offset] 0)
               (assoc-in [:my-axies :axies] []))
       :dispatch [:my-axies/fetch-page]}
      {})))

(rf/reg-event-fx
  :my-axies/fetch-page
  (fn [{:keys [db]} [_ total-axies]]
    (let [axies (get-in db [:my-axies :axies])]
      (if (or (nil? total-axies)
              (< (count axies) total-axies))
        {:http-get {:url (format "https://axieinfinity.com/api/v2/addresses/%s/axies?a=1&offset=%s"
                                 (:eth-addr db) (count axies))
                    :handler [:my-axies/got-page]}}

        {:dispatch [:my-axies/got axies]}))))

(rf/reg-event-fx
  :my-axies/got-page
  (fn [{:keys [db]} [_ {:keys [total-axies axies]}]]
    {:db (-> db
             (update-in [:axie :db]
                        merge
                        (->> axies
                             (map (fn [{:keys [id] :as axie}]
                                    [(str id) axie]))
                             (into {})))
             (assoc-in [:my-axies :total] total-axies)
             (update-in [:my-axies :axies] concat axies))
     :dispatch [:my-axies/fetch-page total-axies]}))

(rf/reg-event-db
  :my-axies/got
  (fn [db [_ axies]]
    (-> db
        (assoc-in [:my-axies :loading?] false)
        (assoc-in [:my-axies :axies] axies))))

(rf/reg-event-db
  :my-axies/set-sort-key
  (fn [db [_ sort-key]]
    (-> db
        (assoc-in [:my-axies :sort-key] sort-key)
        (assoc-in [:my-axies :offset] 0))))

(rf/reg-event-db
  :my-axies/set-sort-order
  (fn [db [_ sort-order]]
    (-> db
        (assoc-in [:my-axies :sort-order] sort-order)
        (assoc-in [:my-axies :offset] 0))))

(rf/reg-event-db
  :my-axies/set-offset
  (fn [db [_ offset]]
    (assoc-in db [:my-axies :offset] offset)))

(rf/reg-event-fx
  ::fetch-axie
  (fn [{:keys [db]} [_ axie-id {:keys [force? handler]}]]
    (let [axie (get-in db [:axie :db (str axie-id)])]
      (cond
        (or (nil? axie)
            force?)
        {:http-get {:url (format "https://axieinfinity.com/api/v2/axies/%s?lang=en" axie-id)
                    :handler [:axie/got handler]}}

        (and (some? axie) (some? handler))
        {:dispatch [handler axie]}

        :else
        {}))))

(rf/reg-event-fx
  :axie/got
  (fn [{:keys [db]} [_ handler axie]]
    (cond->
      {:db (-> db
               (assoc-in [:axie :loading?] false)
               (assoc-in [:axie :db (str (:id axie))] axie))}
      (some? handler)
      (merge {:dispatch [handler axie]}))))

(rf/reg-event-fx
  :axie/fetch-parents
  (fn [{:keys [db]} [_ {:keys [sire-id matron-id]}]]
    {:dispatch-n
     (cond-> []
       (and (some? sire-id) (not (zero? sire-id)))
       (conj [::fetch-axie sire-id {:handler :axie/fetch-parents}])

       (and (some? matron-id) (not (zero? matron-id)))
       (conj [::fetch-axie matron-id {:handler :axie/fetch-parents}]))}))

(rf/reg-event-db
  :axie/family-tree-expand
  (fn [db [_ axie-id]]
    (update-in db [:axie :family-tree-expansions (str axie-id)] not)))

(rf/reg-event-fx
  :battle-simulator/simulate
  (fn [{:keys [db]} [_ atk-id def-id]]
    {:db db
     :dispatch-n [[::fetch-axie atk-id {:handler :battle-simulator/got-attacker}]
                  [::fetch-axie def-id {:handler :battle-simulator/got-defender}]]}))

(rf/reg-event-db
  :battle-simulator/got-attacker
  (fn [db [_ axie]]
    (assoc-in db [:battle-simulator :attacker] axie)))

(rf/reg-event-db
  :battle-simulator/got-defender
  (fn [db [_ axie]]
    (assoc-in db [:battle-simulator :defender] axie)))

(rf/reg-event-fx
  :teams/fetch-teams
  (fn [{:keys [db]} [_ force?]]
    (if (or (-> db :teams :teams nil?)
            force?)
      {:db (-> db
               (assoc-in [:teams :loading?] true)
               (assoc-in [:teams :teams] []))
       :dispatch [:teams/fetch-teams-page]}
      {})))

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
      {:db (-> db
               (assoc-in [:teams :total] total)
               (update-in [:teams :teams] concat teams))
       :dispatch-n (-> [[:teams/fetch-teams-page total]
                        [:teams/fetch-activity-points axie-ids]]
                       (concat (map (fn [axie-id]
                                      [::fetch-axie axie-id])
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
  :multi-gifter/set-to-addr
  (fn [db [_ to-addr]]
    (assoc db :multi-gifter/to-addr to-addr)))

(rf/reg-event-fx
  :multi-gifter/send
  (fn [{:keys [db]} [_ to-addr axie-id]]
    {:blockchain/contract-call {:contract-instance (:contract/axie-instance db)
                                :method :safeTransferFrom
                                :args [(:eth-addr db) to-addr axie-id]
                                :handler :multi-gifter/sent
                                :err-handler :contract/error}}))

(rf/reg-event-db
  :multi-gifter/sent
  (fn [db [_ result]]
    (println "sent" result)
    db))

(rf/reg-event-fx
  :auto-battle/generate-token
  (fn [{:keys [db]} [_ {:keys [after-handlers]}]]
    {:blockchain/sign {:web3 (:web3 db)
                       :addr (:eth-addr db)
                       :data "0x4178696520496e66696e697479"
                       :handler [:auto-battle/got-token {:after-handlers after-handlers}]
                       :err-handler :contract/error}}))

(rf/reg-event-fx
  :auto-battle/got-token
  (fn [{:keys [db]} [_ {:keys [after-handlers]} token]]
    (cond-> {:db (assoc-in db [:auto-battle :token] token)}
      (seq after-handlers)
      (assoc :dispatch-n after-handlers))))

(rf/reg-event-db
  :auto-battle/set-num-months
  (fn [db [_ num-months]]
    (assoc-in db [:auto-battle :num-months] num-months)))

(rf/reg-event-db
  :auto-battle/set-current-tier-index
  (fn [db [_ index]]
    (assoc-in db [:auto-battle :current-tier-index] index)))

(rf/reg-event-fx
  :cryptonator/fetch-ticker
  (fn [_ [_ ticker]]
    {:http-get {:url (format "https://api.cryptonator.com/api/ticker/%s"
                             (string/lower-case ticker))
                :handler [:cryptonator/got-ticker ticker]}}))

(rf/reg-event-db
  :cryptonator/got-ticker
  (fn [db [_ ticker result]]
    (assoc-in db [:cryptonator ticker] (:ticker result))))

(rf/reg-event-fx
  :land/login
  (fn [{:keys [db]} [ _ {:keys [after-handlers]}]]
    (if (-> db :land :profile some?)
      {:dispatch-n (or after-handlers [])}
      {:blockchain/enable {:eth (:eth db)
                           :handlers [[:land/fetch-random-message after-handlers]]}})))

(rf/reg-event-fx
  :land/fetch-random-message
  (fn [_ [_ after-handlers]]
    {:http-get {:url "https://axieinfinity.com/account-api/account/random-message"
                :handler [:land/got-random-message after-handlers]}}))

(rf/reg-event-fx
  :land/got-random-message
  (fn [{:keys [db]} [_ after-handlers {:keys [data]}]]
    {:db (assoc-in db [:land :random-message] data)
     :blockchain/sign {:web3 (:web3 db)
                       :addr (:eth-addr db)
                       :data data
                       :handler [:land/got-signature after-handlers]
                       :err-handler :contract/error}}))

(rf/reg-event-fx
  :land/got-signature
  (fn [{:keys [db]} [_ after-handlers signature]]
    {:http-post {:url "https://axieinfinity.com/account-api/account/login-signature/ethereum"
                 :body {:message (get-in db [:land :random-message])
                        :signature signature
                        :signer (:eth-addr db)}
                 :headers {"Content-Type" "application/json"}
                 :handler [:land/got-token after-handlers]}}))

(rf/reg-event-fx
  :land/got-token
  (fn [{:keys [db]} [_ after-handlers {:keys [data]}]]
    {:db (assoc-in db [:land :token] data)
     :dispatch [:land/fetch-profile after-handlers]}))

(rf/reg-event-fx
  :land/fetch-profile
  (fn [{:keys [db]} [_ after-handlers]]
    {:http-get {:url "https://axieinfinity.com/account-api/account/profile"
                :headers {"Authorization" (format "Bearer %s" (get-in db [:land :token]))}
                :handler [:land/got-profile after-handlers]}}))

(rf/reg-event-fx
  :land/got-profile
  (fn [{:keys [db]} [_ after-handlers {:keys [data]}]]
    {:db (assoc-in db [:land :profile] data)
     :dispatch-n (or after-handlers [])}))

(rf/reg-event-fx
  :land/fetch-items
  (fn [{:keys [db]} [_ force?]]
    (if (or (-> db :land :items nil?)
            force?)
      {:db (-> db
               (assoc-in [:land :items :loading?] true)
               (assoc-in [:land :items :items] []))
       :dispatch [:land/fetch-items-page]}
      {})))

(rf/reg-event-fx
  :land/fetch-items-page
  (fn [{:keys [db]} [_ total-items]]
    (let [items (get-in db [:land :items :items])]
      (if (or (nil? total-items)
              (< (count items) total-items))
        {:http-get {:url (format "https://axieinfinity.com/land-api/profile/inventory/%s/?limit=50&offset=%s"
                                 (get-in db [:land :profile :account-id]) (count items))
                    :handler [:land/got-items-page]}}

        {:dispatch [:land/got-items items]}))))

(rf/reg-event-fx
  :land/got-items-page
  (fn [{:keys [db]} [_ {:keys [total-items items]}]]
    {:db (-> db
             (assoc-in [:land :items :total] total-items)
             (update-in [:land :items :items] concat items))
     :dispatch [:land/fetch-items-page total-items]}))

(rf/reg-event-db
  :land/got-items
  (fn [db [_ items]]
    (-> db
        (assoc-in [:land :items :loading?] false)
        (assoc-in [:land :items :items] items))))

(rf/reg-event-db
  :land-items/set-sort-key
  (fn [db [_ sort-key]]
    (assoc-in db [:land :items :sort-key] sort-key)))

(rf/reg-event-db
  :land-items/set-sort-order
  (fn [db [_ sort-order]]
    (assoc-in db [:land :items :sort-order] sort-order)))

(rf/reg-event-fx
  :land/fetch-market
  (fn [{:keys [db]} [_ force?]]
    (if (or (-> db :land :market nil?)
            force?)
      {:db (-> db
               (assoc-in [:land :market :loading?] true)
               (assoc-in [:land :market :items] []))
       :dispatch [:land/fetch-market-page]}
      {})))

(rf/reg-event-fx
  :land/fetch-market-page
  (fn [{:keys [db]} [_ total]]
    (let [items (get-in db [:land :market :items])]
      (if (or (nil? total)
              (< (count items) total))
        {:http-get {:url (format "https://axieinfinity.com/marketplace-api/query-assets?sorting=lowest_price&offset=%s&count=100"
                                 (count items))
                    :handler [:land/got-market-page]}}
        {:dispatch [:land/got-market items]}))))

(rf/reg-event-fx
  :land/got-market-page
  (fn [{:keys [db]} [_ {:keys [total results]}]]
    {:db (-> db
             (assoc-in [:land :market :total] total)
             (update-in [:land :market :items] concat results))
     :dispatch [:land/fetch-market-page total]}))

(rf/reg-event-db
  :land/got-market
  (fn [db [_ items]]
    (-> db
        (assoc-in [:land :market :loading?] false)
        (assoc-in [:land :market :items] items))))

(rf/reg-event-db
  :land-market/set-sort-key
  (fn [db [_ sort-key]]
    (assoc-in db [:land :market :sort-key] sort-key)))

(rf/reg-event-db
  :land-market/set-sort-order
  (fn [db [_ sort-order]]
    (assoc-in db [:land :market :sort-order] sort-order)))

(rf/reg-event-db
  :land-market/set-offset
  (fn [db [_ offset]]
    (assoc-in db [:land :market :offset] offset)))

(rf/reg-event-db
  :valuation/set-sort-key
  (fn [db [_ sort-key]]
    (assoc-in db [:valuation :sort-key] sort-key)))

(rf/reg-event-db
  :valuation/set-sort-order
  (fn [db [_ sort-order]]
    (assoc-in db [:valuation :sort-order] sort-order)))

(rf/reg-event-db
  :breedable/select-sire
  (fn [db [_ sire]]
    (assoc-in db [:breedable :sire] sire)))

(rf/reg-event-db
  :breedable/set-offset
  (fn [db [_ offset]]
    (assoc-in db [:breedable :offset] offset)))

(rf/reg-event-db
  :breed-calc/set-sire
  (fn [db [_ axie]]
    (assoc-in db [:breed-calc :sire] axie)))

(rf/reg-event-db
  :breed-calc/set-matron
  (fn [db [_ axie]]
    (assoc-in db [:breed-calc :matron] axie)))

(rf/reg-event-db
  :breed-calc/quick-calc
  (fn [db [_ sire matron]]
    (cond-> db
      (nil? (get-in db [:breed-calc/quick #{(:id sire) (:id matron)}]))
      (assoc-in
        [:breed-calc/quick #{(:id sire) (:id matron)}]
        (-> (genes/predict-breed
              (:name->body-part db)
              sire
              matron)
            genes/predict-score)))))

;; TODO create teams
;; POST https://api.axieinfinity.com/v1/battle/teams/create

;; How do I decide who goes where?
;; How do I choose the move order?
;; Can I make those manual for now, and just use this tool to make it easier to choose which axies are on the team?
;; Also it would be nice to be able to edit existing teams more easily.

;; Request URL: https://api.axieinfinity.com/v1/battle/teams/create
;; 
;; payload:
;; {"from":"0x560ebafd8db62cbdb44b50539d65b48072b98277",
;;  "name":"gong fu cha",
;;  "fighters":[{"id":63910,
;;               "position":5,
;;               "moveSet":[{"index":1,"part":"horn"},
;;                          {"index":2,"part":"mouth"},
;;                          {"index":3,"part":"back"},
;;                          {"index":4,"part":"tail"}]},
;;              {"id":10140,
;;               "position":8,
;;               "moveSet":[{"index":1,"part":"horn"},
;;                          {"index":2,"part":"mouth"},
;;                          {"index":3,"part":"tail"},
;;                          {"index":4,"part":"back"}]},
;;              {"id":13977,
;;               "position":1,
;;               "moveSet":[{"index":1,"part":"tail"},
;;                          {"index":2,"part":"back"},
;;                          {"index":3,"part":"mouth"},
;;                          {"index":4,"part":"horn"}]}]}
;; 
;; returns:
;; {teamId: "e4cc1d1e-ca33-443d-ba4d-e649d65d08b0"}
;; 
;; POST https://api.axieinfinity.com/v1/battle/teams/update/e4cc1d1e-ca33-443d-ba4d-e649d65d08b0
;; 
;; {"from":"0x560ebafd8db62cbdb44b50539d65b48072b98277",
;;  "name":"gong fu cha",
;;  "fighters":[{"id":63910,
;;               "position":5,
;;               "moveSet":[{"index":1,"part":"horn"},
;;                          {"index":2,"part":"mouth"},
;;                          {"index":3,"part":"back"},
;;                          {"index":4,"part":"tail"}]},
;;              {"id":10140,
;;               "position":7,
;;               "moveSet":[{"index":1,"part":"horn"},
;;                          {"index":2,"part":"mouth"},
;;                          {"index":3,"part":"tail"},
;;                          {"index":4,"part":"back"}]},
;;              {"id":13977,
;;               "position":1,
;;               "moveSet":[{"index":1,"part":"tail"},
;;                          {"index":2,"part":"back"},
;;                          {"index":3,"part":"mouth"},
;;                          {"index":4,"part":"horn"}]}]}
;; 
;; returns:
;; {teamId: "e4cc1d1e-ca33-443d-ba4d-e649d65d08b0"}
