(ns axiescope.views
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [reagent-table.core :as rt]
   [cuerdas.core :refer [format]]
   [axiescope.subs :as subs]
   [axiescope.events :as events]
   ))

(defn footer
  []
  (let [panel @(rf/subscribe [::subs/active-panel])]
    [:div.footer.row
     [:div.col-xs-12.center-xs
      [:p "Donate to 0x560EBafD8dB62cbdB44B50539d65b48072b98277"]]
     (when (not= :home-panel panel)
       [:div.col-xs-12.center-xs
        [:a {:href "/"} "Back Home"]])]))

(defn home-panel []
  [:div.container
   [:div.row
    [:div.col-xs-12
     [:h1 "axiescope"]]]
   [:div.row
    [:div.col-xs-12
     [:p "These are a collection of tools to help you with Axie Infinity."]]]
   [:div.row
    [:div.col-xs-12
     [:ul
      [:li [:a {:href "/battle-simulator"}
            "Battle Simulator"]]
      [:li [:a {:href "/my-axies"}
            "My Axies"]]]]]
   [footer]])

(defn show-axie
  [{:keys [id name image] :as axie}]
  (when axie
    [:div.row
     [:div.col-xs-12.center-xs
      [:h2
       [:a {:href (format "https://axieinfinity.com/axie/%s" id)
            :target "_blank"}
        name]]]
     [:div.col-xs-12.center-xs
      [:img {:style {:width "100%"}
             :src image}]]]))

(defn battle-simulator-panel []
  [:div.container
   [:div.row
    [:div.col-xs-12.center-xs
     [:h1 "Battle Simulator"]]]
   (let [attacker @(rf/subscribe [::subs/bs-attacker])
         defender @(rf/subscribe [::subs/bs-defender])
         simulation (rf/subscribe [::subs/battle-simulation])
         atk-id (r/atom (:id attacker))
         def-id (r/atom (:id defender))]
     [:div
      [:form {:on-submit (fn [e]
                           (.preventDefault e)
                           (rf/dispatch [::events/simulate-battle @atk-id @def-id]))}
       [:div.row {:style {:background-color "#EEEEEE"
                          :padding "0.6em"
                          :border-radius "0.5em"}}
        [:div.col-xs-6.center-xs
         [:label {:for "attacker"
                  :style {:padding "0.5em"}}
          "Attacker"]
         [:input {:type "text"
                  :name "attacker"
                  :style {:max-width "40%"}
                  :on-change (fn [e]
                               (reset! atk-id (-> e .-target .-value)))}]]
        [:div.col-xs-6.center-xs
         [:label {:for "defender"
                  :style {:padding "0.5em"}}
          "Defender"]
         [:input {:type "text"
                  :name "defender"
                  :style {:max-width "40%"}
                  :on-change (fn [e]
                               (reset! def-id (-> e .-target .-value)))}]]]
       [:div.row {:style {:margin "0.6em"
                          :padding "0.6em"}}
        [:div.col-xs-12.center-xs
         [:button {:style {:padding "1em"
                           :font-size "1em"}}
          "Simulate"]]]]
      [:div.row
       [:div.col-xs-6
        [show-axie attacker]]
       [:div.col-xs-6
        [show-axie defender]]]
      (when @simulation
        [:div.row
         [:div.col-xs-12
          [rt/reagent-table
           simulation
           {:table {:style {:margin "0 auto"}}
            :column-model [{:header "Attack"
                            :key :attack}
                           {:header "Defense"
                            :key :defense}
                           {:header "Damage"
                            :key :dmg}]
            :render-cell (fn [{:keys [key]} row _ _]
                           (get row key))}]]])])
   [footer]])

(defn my-axies-sort-button
  [title sort-key active-sort-key]
  [:button
   {:style {:border-radius "0.3em"
            :padding "0.35em"
            :margin "0 0.1em"}
    :disabled (= sort-key active-sort-key)
    :on-click #(rf/dispatch [::events/set-my-axies-sort-key sort-key])}
   title])

(defn my-axies-panel
  []
  (let [loading? @(rf/subscribe [:my-axies/loading?])
        sort-key @(rf/subscribe [:my-axies/sort-key])
        axies (rf/subscribe [:my-axies/axies])]
    [:div.container
     [:div.row
      [:div.col-xs-12.center-xs
       [:h1 "My Axies"]]]
     (if loading?
       [:div.row
        [:div.col-xs-12.center-xs
         [:em "loading..."]]]
       [:div.row
        [:div.col-xs-12.center-xs
         [:p
          [:span {:style {:padding-right "0.3em"}}
           "sort by:"]
          [my-axies-sort-button "ID" :id sort-key]
          [my-axies-sort-button "Class" :class sort-key]
          [my-axies-sort-button "Purity" :purity sort-key]
          [my-axies-sort-button "Breeds" :breed-count sort-key]
          [my-axies-sort-button "Attack" :attack sort-key]
          [my-axies-sort-button "Defense" :defense sort-key]
          [my-axies-sort-button "Atk+Def" :atk+def sort-key]
          [my-axies-sort-button "Tank" :tank sort-key]
          [my-axies-sort-button "DPS" :dps sort-key]]]
        [:div.col-xs-12
         [rt/reagent-table
          axies
          {:table {:class "table table-striped"
                   :style {:margin "0 auto"}}
           :column-model [{:header "ID"
                           :key :id}
                          {:key :image}
                          {:header "Name"
                           :key :name}
                          {:header "Class"
                           :key :class}
                          {:header "Purity"
                           :key :purity}
                          {:header "Breeds"
                           :key :breed-count}
                          {:header "Attack"
                           :key :attack}
                          {:header "Defense"
                           :key :defense}
                          {:header "Atk+Def"
                           :key :atk+def}
                          {:header "Tank"
                           :key :tank}
                          {:header "DPS"
                           :key :dps}]
           :render-cell (fn [{:keys [key]} row _ _]
                          (let [value (get row key)]
                            (case key
                              :id [:a {:href (format "https://axieinfinity.com/axie/%s" value)
                                       :target "_blank"}
                                   value]
                              :image [:img {:style {:width "100%"}
                                            :src value}]
                              value)))}]]])
     [footer]]))

(defn panels
  [panel]
  (case panel
    :home-panel [home-panel]
    :battle-simulator-panel [battle-simulator-panel]
    :my-axies-panel [my-axies-panel]
    [home-panel]))

(defn show-panel
  [panel]
  [panels panel])

(defn main-panel []
  (let [active-panel @(rf/subscribe [::subs/active-panel])]
    [show-panel active-panel]))
