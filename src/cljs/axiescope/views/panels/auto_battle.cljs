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
   (let [logged-in? @(rf/subscribe [:axiescope/logged-in?])
         account @(rf/subscribe [:axiescope.auto-battle/account])
         eth-addr @(rf/subscribe [::subs/eth-addr])
         token @(rf/subscribe [:auto-battle/token])
         eth-usd @(rf/subscribe [:cryptonator/ticker "eth-usd"])
         price-tiers @(rf/subscribe [:axiescope.prices.auto-battle/tiers])
         current-tier @(rf/subscribe [:auto-battle/current-tier])
         current-tier-index @(rf/subscribe [:auto-battle/current-tier-index])
         dollars-per-month @(rf/subscribe [:auto-battle/dollars-per-month])
         num-months @(rf/subscribe [:auto-battle/num-months])
         until @(rf/subscribe [:auto-battle/until])
         payload {:eth-addr eth-addr
                  :token token
                  :max-teams (:max-teams current-tier)
                  :until (.format until "YYYY-MM-DD")}]
     [:div.row
      [:div.col-xs-6
       (when-not logged-in?
        [:div.row
         [:div.col-xs-12.center-xs
          [:p "You need to log in, so we know who you are."]
          [:button {:on-click #(rf/dispatch [:axiescope/auth {:after-handlers [[:axiescope.auto-battle.account/fetch]]}])
                    :style {:padding "0.9em"
                            :background-color "#2277bb"
                            :color "white"
                            :border "none"
                            :outline "none"
                            :border-radius "1.8em"}}
           "Login"]
          [:p "Please sign this message, and the result is a secret token between us."]]])
       (when (and logged-in? (some? account))
         [:div.row
          [:div.col-xs-12
           [:div.row.middle-xs
            [:div.col-xs-8.col-xs-offset-2.center-xs
             [:h4 "Your Account"]]
            [:div.col-xs-2.end-xs
             [:button {:on-click #(rf/dispatch [:axiescope.auto-battle.account/fetch])
                       :style {:padding "0.6em"
                               :font-size "0.8em"
                               :background-color "#2277bb"
                               :color "white"
                               :border "none"
                               :outline "none"
                               :border-radius "1.8em"}}
              "Refresh"]]]
           [:div.row {:style {:margin "0.45em 0"
                              :padding-bottom "0.2em"
                              :border-bottom "1px solid #2277bb"
                              :font-weight "bold"}}
            [:div.col-xs-3.end-xs "Max Teams"]
            [:div.col-xs-9.end-xs "Until"]]
           (let [{:keys [max-teams until]} account]
             [:div.row
              [:div.col-xs-3.end-xs
               (if (some? max-teams)
                 max-teams
                 "Unlimited")]
              [:div.col-xs-9.end-xs
               (if (and (some? until)
                        (not= 3 max-teams))
                 until
                 "Forever")]])]])
       (when (and logged-in? (nil? account))
         [:div.row
          [:div.col-xs-12.center-xs
           [:p "Looks like you need to sign up for the free tier!"]]
            [:div.col-xs-12.center-xs
             [:button {:on-click #(rf/dispatch [:auto-battle/generate-token {:after-handlers [[:axiescope.auto-battle/signup]]}])
                       :style {:padding "0.9em"
                               :background-color "#2277bb"
                               :color "white"
                               :border "none"
                               :outline "none"
                               :border-radius "1.8em"}}
              "Signup"]]
          [:div.col-xs-12.center-xs
           [:p "After you've signed up, then you can upgrade if you want."]]])
       [:div.row {:style {:margin-top "6em"}}
        [:div.col-xs-12.center-xs
         [:div.row {:style {:margin "0.45em 0"
                            :padding-bottom "0.2em"
                            :border-bottom "1px solid #2277bb"
                            :font-weight "bold"}}
          [:div.col-xs-3.end-xs "Max Teams"]
          [:div.col-xs-3.end-xs "USD Price"]
          [:div.col-xs-4.end-xs "ETH Price"]]
         (for [{:keys [max-teams usd eth]} price-tiers]
           [:div.row.middle-xs {:key (format "teams-%s" max-teams)
                                :style {:margin "0.45em 0"
                                        :padding-bottom "0.4em"
                                        :border-bottom "1px solid #2277bb"}}
            [:div.col-xs-3.end-xs (if (some? max-teams)
                                    max-teams
                                    "Unlimited")]
            [:div.col-xs-3.end-xs (format "$%s/mo" usd)]
            [:div.col-xs-4.end-xs (format "%s ETH/mo" (round eth 5))]
            [:div.col-xs-2
             (when (pos? eth)
               [:button {:disabled (or (not logged-in?) (nil? account))
                         :on-click #(rf/dispatch [:axiescope.pay/auto-battle max-teams eth])
                         :style {:padding "0.6em"
                                 :background-color (if (or (not logged-in?) (nil? account))
                                                     "#bcd6ea"
                                                     "#2277bb")
                                 :color "white"
                                 :border "none"
                                 :outline "none"
                                 :border-radius "1em"}}
                "Pay"])]])]]
       ]
      [:div.col-xs-6
       [:p "Each team can battle once every four hours, up to 3 times per 12 hours. That means that in order to maximize your exp, you need to log in multiple times per day and send each team to battle over and over."]
      [:p (format "That is a lot of work! I can do it automatically for you, so you can maximize your exp with no work at all.")]
      [:p "If you're new to the game, I want to help you out. That's why there's a free tier! I will auto-battle your first few teams for free. All you have to do is wait for the exp to roll in."]
      [:p "Once you've got more teams, that tells me one of two things has happened: you've spent some money on axies, or you've gained enough exp to breed a bunch of axies for yourself. I think that means you can afford to maintain your investment. That's why it costs more to auto-battle more teams (also my computer has to do more work). You should be able to afford it by selling off some of your cheaper axies."]
      [:p "The auto-battles cost less than the value of the exp you get ... not to mention the value of your time."]
      [:p "Note! I can't take money from your ETH account, so you can sign up for one month, but I can't automatically renew. You'll have to come in and renew manually in a month. If you forget, you will automatically fall back to the free tier and get your first three teams auto-battled."]]])
   [footer]])
