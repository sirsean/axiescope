(ns axiescope.views.layout
  (:require
    [re-frame.core :as rf]
    [axiescope.subs :as subs]
    [axiescope.views.bar :as bar]
    ))

(defn header
  [{:keys [title bars]}]
  [:div.row
   (let [bars (set bars)]
     [:div.col-xs-12
      (when (:my-axies bars) [bar/my-axies-bar])
      (when (:teams bars) [bar/teams-bar])
      (when (:search bars) [bar/search-bar])
      (when (:items bars) [bar/items-bar])
      (when (:market bars) [bar/market-bar])])
   [:div.col-xs-12.center-xs
    [:h1 title]]])

(defn footer
  []
  (let [panel @(rf/subscribe [::subs/active-panel])]
    [:div.footer.row
     [:div.col-xs-12.center-xs
      [:p [:a {:href "#"
               :on-click (fn [e]
                           (.preventDefault e)
                           (rf/dispatch [:eth/send-eth "0x560EBafD8dB62cbdB44B50539d65b48072b98277" "0.05"]))}
           "donate to sirsean.eth"]]]
     (when (not= :home-panel panel)
       [:div.col-xs-12.center-xs
        [:a {:href "/"} "Back Home"]])]))
