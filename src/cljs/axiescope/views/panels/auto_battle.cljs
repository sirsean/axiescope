(ns axiescope.views.panels.auto-battle
  (:require
    [re-frame.core :as rf]
    [cuerdas.core :refer [format]]
    [axiescope.subs :as subs]
    [axiescope.views.layout :refer [header footer]]
    [axiescope.util :refer [round]]
    ))

(defn panel
  []
  [:div.container
   [header {:title "Auto-Battle"}]
   (let [eth-addr @(rf/subscribe [::subs/eth-addr])
         token @(rf/subscribe [:auto-battle/token])
         eth-usd @(rf/subscribe [:cryptonator/ticker "eth-usd"])
         price-tiers @(rf/subscribe [:auto-battle/price-tiers])
         current-tier @(rf/subscribe [:auto-battle/current-tier])
         current-tier-index @(rf/subscribe [:auto-battle/current-tier-index])
         dollars-per-month @(rf/subscribe [:auto-battle/dollars-per-month])
         num-months @(rf/subscribe [:auto-battle/num-months])
         until @(rf/subscribe [:auto-battle/until])
         payload {:eth-addr eth-addr
                  :token token
                  :max-teams (:max-teams current-tier)
                  :until (.format until "YYYY-MM-DD")}]
     [:div
      [:p "Each team can battle once every four hours, up to 3 times per 12 hours. That means that in order to maximize your exp, you need to log in multiple times per day and send each team to battle over and over."]
      [:p (format "That is a lot of work! I can do it automatically for you, so you can maximize your exp with no work at all.")]
      [:p "If you're new to the game, I want to help you out. That's why there's a free tier! I will auto-battle your first few teams for free. All you have to do is wait for the exp to roll in."]
      [:p "Once you've got more teams, that tells me one of two things has happened: you've spent some money on axies, or you've gained enough exp to breed a bunch of axies for yourself. I think that means you can afford to maintain your investment. That's why it costs more to auto-battle more teams (also my computer has to do more work). You should be able to afford it by selling off some of your cheaper axies."]
      (if (some? token)
        (let [json (.stringify js/JSON (clj->js payload) nil 4)]
          [:div
           [:textarea {:rows 6
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
           [:p "Send me that JSON in Discord and show proof of payment (a link to the transaction on Etherscan), and I'll get you set up."]])
        [:div
         [:p "In order for the auto-battle program to work, I need your ETH address and your Axie Infinity bearer token. Please click the Generate Token button to fill in the token."]
         [:button {:on-click (fn [e]
                               (rf/dispatch [:auto-battle/generate-token]))}
          "Generate Token"]])
      [:div.row
       [:div.col-xs-12.col-xs-offset-1
        [:h3 "Price Tiers"]]]
      [:div.row
       [:div.col-xs-1 [:strong "Max Teams"]]
       [:div.col-xs-1 [:strong "USD/month"]]
       [:div.col-xs-1 [:strong "ETH/month"]]]
      (map-indexed (fn [i tier]
                     [:div.row.middle-xs {:key i}
                      [:div.col-xs-1
                       (if-some [max-teams (:max-teams tier)]
                         max-teams
                         "unlimited")]
                      [:div.col-xs-1 (:dollars-per-month tier)]
                      [:div.col-xs-1
                       (round (->> eth-usd :price (/ (:dollars-per-month tier))) 8)]
                      [:div.col-xs-1
                       [:button {:style {:margin "0.2em"}
                                 :disabled (= i current-tier-index)
                                 :on-click #(rf/dispatch [:auto-battle/set-current-tier-index i])}
                        "Choose"]]])
                   price-tiers)
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
                    :disabled (not (pos? price))
                    :on-click (fn [e]
                                (rf/dispatch [:eth/send-eth
                                              "0x560EBafD8dB62cbdB44B50539d65b48072b98277"
                                              price]))}
           (format "Pay %s" price)]]])
      [:p "Prices are negotiable, if you think this is more than you want to pay ... or you'd prefer to pay with DAI or LOOM or some other token. Let's talk about it."]
      [:p "When your subscription expires, you fall back to the free tier and get three teams auto-battled until you renew. And if you were already on the free tier, there's basically no difference between expired and not-expired."]])
   [footer]])
