(ns axiescope.views.panels.auto-battle
  (:require
    [re-frame.core :as rf]
    [cuerdas.core :refer [format]]
    [axiescope.subs :as subs]
    [axiescope.views.layout :refer [header footer]]
    ))

(defn panel
  []
  [:div.container
   [header "Auto-Battle"]
   (let [eth-addr @(rf/subscribe [::subs/eth-addr])
         token @(rf/subscribe [:auto-battle/token])
         eth-usd @(rf/subscribe [:cryptonator/ticker "eth-usd"])
         dollars-per-month @(rf/subscribe [:auto-battle/dollars-per-month])
         num-months @(rf/subscribe [:auto-battle/num-months])
         until @(rf/subscribe [:auto-battle/until])]
     [:div
      [:p "Each team can battle once every four hours, up to 3 times per 12 hours. That means that in order to maximize your exp, you need to log in multiple times per day and send each team to battle over and over."]
      [:p (format "That is a lot of work! I can do it automatically for you, so you can maximize your exp with no work at all. In exchange, you pay $%s/month in ETH." dollars-per-month)]
      (if (some? token)
        (let [json (.stringify js/JSON (clj->js {:eth-addr eth-addr
                                                 :token token
                                                 :until (.format until "YYYY-MM-DD")}) nil 4)]
          [:div
           [:textarea {:rows 5
                       :style {:width "100%"
                               :border "none"}
                       :value json
                       :read-only true
                       :id "my-json-code"}]
           [:p [:button {:on-click (fn [e]
                                     (let [elem (.getElementById js/document "my-json-code")]
                                       (.setSelectionRange elem 0 (.. elem -value -length))
                                       (.focus elem)
                                       (.execCommand js/document "copy")
                                       (.blur elem)
                                       (.preventDefault e)))}
                "Copy to Clipboard"]]
           [:p "Send me that JSON in Discord and show proof of payment, and I'll get you set up."]])
        [:div
         [:p "In order for the auto-battle program to work, I need your ETH address and your Axie Infinity bearer token. Please click the Generate Token button to fill in the token."]
         [:button {:on-click (fn [e]
                               (rf/dispatch [:auto-battle/generate-token]))}
          "Generate Token"]])
      [:div.row
       [:div.col-xs-12.col-xs-offset-1
        [:h3 "Price List"]]]
      [:div.row.middle-xs {:style {:margin-bottom "1em"}}
       [:div.col-xs-1
        (format "%s month%s" num-months (when (not= 1 num-months) "s"))]
       [:div.col-xs
        [:button {:disabled (>= 1 num-months)
                  :on-click #(rf/dispatch [:auto-battle/set-num-months (dec num-months)])}
         "-1"]
        [:button {:on-click #(rf/dispatch [:auto-battle/set-num-months (inc num-months)])}
         "+1"]]]
      [:div.row
       [:div.col-xs-1.end-xs [:strong "USD"]]
       [:div.col-xs-6
        (format "%s/month" dollars-per-month)]]
      (let [price-per-month (->> eth-usd :price (/ dollars-per-month))
            price (* price-per-month num-months)]
        [:div.row.middle-xs
         [:div.col-xs-1.end-xs [:strong "ETH"]]
         [:div.col-xs-3
          (format "%s/month" price-per-month)]
         [:div.col-xs-3
          [:button {:style {:padding "0.5em"
                            :border-radius "0.6em"}
                    :on-click (fn [e]
                                (rf/dispatch [:eth/send-eth
                                              "0x560EBafD8dB62cbdB44B50539d65b48072b98277"
                                              price]))}
           (format "Pay %s" price)]]])
      [:p "Prices are negotiable, if you think this is more than you want to pay ... or you'd prefer to pay with DAI or LOOM or some other token. Let's talk about it."]])
   [footer]])
