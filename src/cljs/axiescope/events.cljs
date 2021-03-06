(ns axiescope.events
  (:require
    [clojure.set :refer [rename-keys]]
    [re-frame.core :as rf]
    [re-graph.core :as re-graph]
    [district0x.re-frame.interval-fx]
    [cljs-web3.core]
    [cljs-web3.eth]
    [cljs-web3.personal]
    [clojure.string :as string]
    [cuerdas.core :refer [format]]
    [cljs-await.core :refer [await]]
    [cljs.core.async :refer [<!]]
    [camel-snake-kebab.core :refer [->kebab-case-keyword ->camelCaseKeyword]]
    [camel-snake-kebab.extras :refer [transform-keys]]
    [ajax.core :as ajax]
    [cljsjs.moment]
    [axiescope.config :refer [api-host]]
    [axiescope.genes :as genes]
    [axiescope.db :as db]
    [axiescope.query :as query]
    [axiescope.util :refer [ranking-type->key]]
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
   :dispatch-n [[:axie/set-id axie-id]
                [:cards/fetch]]})

(defmethod set-active-panel :my-axies-panel
  [{:keys [db]} [_ panel]]
  {:db (assoc db :active-panel panel)
   :blockchain/enable {:eth (:eth db)
                       :handlers [:my-axies/fetch
                                  [:card-rankings/fetch :all]
                                  [:card-rankings/fetch :attack]
                                  [:card-rankings/fetch :defense]]}})

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

(defmethod set-active-panel :card-rankings-panel
  [{:keys [db]} [_ panel ranking-type]]
  {:db (-> db
           (assoc :active-panel panel)
           (assoc-in [:card-rankings :ranking-type] ranking-type))
   :dispatch [:card-rankings/fetch ranking-type]})

(defmethod set-active-panel :card-rankings-vote-panel
  [{:keys [db]} [_ panel ranking-type]]
  {:db (assoc db :active-panel panel)
   :dispatch [:card-rankings/fetch
              ranking-type
              [[:card-rankings/next-pair ranking-type]]]})

(defmethod set-active-panel :combo-rankings-panel
  [{:keys [db]} [_ panel]]
  {:db (assoc db :active-panel panel)
   :dispatch [:combo-rankings/fetch]})

(defmethod set-active-panel :combo-rankings-vote-panel
  [{:keys [db]} [_ panel]]
  {:db (assoc db :active-panel panel)
   :dispatch [:combo-rankings/fetch [[:combo-rankings/next-pair]]]})

(defmethod set-active-panel :combo-rankings-add-panel
  [{:keys [db]} [_ panel]]
  {:db (-> db
           (assoc :active-panel panel)
           (update :cards dissoc :search)
           (update :cards dissoc :selector)
           (update :cards dissoc :sort-key)
           (update :cards dissoc :sort-order))
   :dispatch [:cards/fetch]})

(defmethod set-active-panel :cards-panel
  [{:keys [db]} [_ panel]]
  {:db (-> db
           (assoc :active-panel panel)
           (update :cards dissoc :search)
           (update :cards dissoc :selector)
           (update :cards dissoc :sort-key)
           (update :cards dissoc :sort-order))
   :dispatch [:cards/fetch]})

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
             (assoc-in [:axie :id] (str axie-id)))
     :dispatch-n [[::fetch-axie axie-id {:force? true
                                         :handler handler}]]}))

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
        {:dispatch [::re-graph/query
                    query/fetch-my-axies-query
                    {:owner (:eth-addr db)
                     :sort  "IdDesc"
                     :size  100
                     :from  (count axies)}
                    [:my-axies/got-page]]}
        {:dispatch [:my-axies/got axies]}))))

(rf/reg-event-fx
  :my-axies/got-page
  (fn [{:keys [db]} [_ {{{:keys [total results]} :axies} :data}]]
    (let [axies (->> results
                     (map (partial transform-keys ->kebab-case-keyword))
                     (map (fn [a]
                            (-> a
                                (update :id long)))))]
      {:db (-> db
               (update-in [:axie :db]
                          merge
                          (->> axies
                               (map (fn [{:keys [id] :as axie}]
                                      [(str id) axie]))
                               (into {})))
               (assoc-in [:my-axies :total] total)
               (update-in [:my-axies :axies] concat axies))
       :dispatch [:my-axies/fetch-page total]})))

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
        {:dispatch [::re-graph/query
                    query/fetch-axie-query
                    {:axieId axie-id}
                    [:axie/got handler]]}

        (and (some? axie) (some? handler))
        {:dispatch [handler axie]}

        :else
        {}))))

(rf/reg-event-fx
  :axie/got
  (fn [{:keys [db]} [_ handler {:keys [data]}]]
    (let [axie (transform-keys ->kebab-case-keyword (:axie data))]
      (cond->
        {:db (-> db
                 (assoc-in [:axie :loading?] false)
                 (assoc-in [:axie :db (str (:id axie))] axie))}
        (some? handler)
        (merge {:dispatch [handler axie]})))))

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
  :cryptonator/fetch-ticker
  (fn [_ [_ ticker]]
    {:http-get {:url (format "https://api.cryptonator.com/api/ticker/%s"
                             (string/lower-case ticker))
                :handler [:cryptonator/got-ticker ticker]}}))

(rf/reg-event-db
  :cryptonator/got-ticker
  (fn [db [_ ticker result]]
    (assoc-in db [:cryptonator ticker] (:ticker result))))

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

(defn ranking-type->url-path
  [ranking-type]
  (case ranking-type
    :attack "/attack"
    :defense "/defense"
    :all "/all"))

(rf/reg-event-fx
  :card-rankings/fetch
  (fn [{:keys [db]} [_ ranking-type after-handlers]]
    {:db (-> db
             (assoc-in [:card-rankings :loading?] true))
     :http-get {:url (format "%s/api/card-rankings%s"
                             api-host
                             (ranking-type->url-path ranking-type))
                :handler [:card-rankings/got ranking-type after-handlers]}}))

(rf/reg-event-fx
  :card-rankings/got
  (fn [{:keys [db]} [_ ranking-type after-handlers rankings]]
    {:db (-> db
             (assoc-in [:card-rankings :loading?] false)
             (assoc-in [:card-rankings (ranking-type->key ranking-type)] rankings))
     :dispatch-n (or after-handlers [])}))

(rf/reg-event-db
  :card-rankings/next-pair
  (fn [db [_ ranking-type]]
    (-> db
        (assoc-in [:card-rankings :pair]
                  (->> (get-in db [:card-rankings (ranking-type->key ranking-type)] [])
                       shuffle
                       (take 2))))))

(rf/reg-event-fx
  :card-rankings/vote
  (fn [{:keys [db]} [_ ranking-type winner loser]]
    {:http-post {:url (format "%s/api/card-rankings%s/%s/%s"
                              api-host
                              (ranking-type->url-path ranking-type)
                              winner loser)
                 :handler [:card-rankings/voted ranking-type]}}))

(rf/reg-event-fx
  :card-rankings/voted
  (fn [_ [_ ranking-type]]
    {:dispatch [:card-rankings/next-pair ranking-type]}))

(rf/reg-event-fx
  :combo-rankings/fetch
  (fn [{:keys [db]} [_ after-handlers]]
    {:db (-> db
             (assoc-in [:combo-rankings :loading?] true))
     :http-get {:url (format "%s/api/combo-rankings" api-host)
                :handler [:combo-rankings/got after-handlers]}}))

(rf/reg-event-fx
  :combo-rankings/got
  (fn [{:keys [db]} [_ after-handlers rankings]]
    {:db (-> db
             (assoc-in [:combo-rankings :loading?] false)
             (assoc-in [:combo-rankings :rankings] rankings))
     :dispatch-n (or after-handlers [])}))

(rf/reg-event-db
  :combo-rankings/next-pair
  (fn [db [_]]
    (-> db
        (assoc-in [:combo-rankings :pair]
                  (->> (get-in db [:combo-rankings :rankings] [])
                       shuffle
                       (take 2))))))

(rf/reg-event-fx
  :combo-rankings/vote
  (fn [{:keys [db]} [_ winner loser]]
    {:http-post {:url (format "%s/api/combo-rankings/%s/%s"
                              api-host winner loser)
                 :handler [:combo-rankings/voted]}}))

(rf/reg-event-fx
  :combo-rankings/voted
  (fn [_ [_]]
    {:dispatch [:combo-rankings/next-pair]}))

(rf/reg-event-db
  :combo-rankings/add-select
  (fn [db [_ card]]
    (-> db
        (assoc-in [:combo-rankings :add :selected (keyword (:type card))] card))))

(rf/reg-event-db
  :combo-rankings/add-deselect
  (fn [db [_ card]]
    (-> db
        (update-in [:combo-rankings :add :selected] dissoc (keyword (:type card))))))

(defn format-combo-key
  [ids]
  (->> ids
       (map name)
       sort
       (string/join ".")))

(rf/reg-event-fx
  :combo-rankings/add-submit
  (fn [{:keys [db]} _]
    (let [selections (get-in db [:combo-rankings :add :selected])]
      {:db (-> db
               (assoc-in [:combo-rankings :add :loading?] true))
       :http-post {:url (format "%s/api/combo-rankings/%s"
                                api-host
                                (->> selections
                                     vals
                                     (map :id)
                                     format-combo-key))
                   :handler [:combo-rankings/added]}})))

(rf/reg-event-fx
  :combo-rankings/added
  (fn [{:keys [db]} _]
    {:db (-> db
             (assoc-in [:combo-rankings :add :loading?] false)
             (update-in [:combo-rankings :add] dissoc :selected))
     :dispatch [::set-active-panel :combo-rankings-panel]}))

(rf/reg-event-fx
  :cards/fetch
  (fn [{:keys [db]} [_ after-handlers]]
    {:db (-> db
             (assoc-in [:cards :loading?] true))
     :http-get {:url (format "%s/api/cards" api-host)
                :handler [:cards/got after-handlers]}}))

(rf/reg-event-fx
  :cards/got
  (fn [{:keys [db]} [_ {:keys [after-handlers]} cards]]
    {:db (-> db
             (assoc-in [:cards :loading?] false)
             (assoc-in [:cards :all] cards))
     :dispatch-n (or after-handlers [])}))

(rf/reg-event-db
  :cards/set-sort-key
  (fn [db [_ sort-key]]
    (-> db
        (assoc-in [:cards :sort-key] sort-key))))

(rf/reg-event-db
  :cards/set-sort-order
  (fn [db [_ sort-order]]
    (-> db
        (assoc-in [:cards :sort-order] sort-order))))

(rf/reg-event-db
  :cards/set-selector
  (fn [db [_ selector]]
    (-> db
        (assoc-in [:cards :selector] selector))))

(rf/reg-event-db
  :cards/set-search
  (fn [db [_ search]]
    (-> db
        (assoc-in [:cards :search] search))))
