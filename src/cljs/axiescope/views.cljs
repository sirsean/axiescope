(ns axiescope.views
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [reagent-table.core :as rt]
   [cuerdas.core :refer [format]]
   [axiescope.subs :as subs]
   [axiescope.events :as events]
   ))

(defn loading-bar
  [numer denom color]
  (let [percent (max 7 (if (and numer denom (not= "?" denom))
                         (* 100 (/ numer denom))
                         0))]
      [:div {:style {:background-color color
                     :color "white"
                     :border-radius "0.15em"
                     :padding "0.2em"
                     :width (format "%s%" percent)
                     :height "100%"}}
       [:span (format "%s/%s" numer denom)]]))

(defn my-axies-bar
  []
  (let [loading? @(rf/subscribe [:my-axies/loading?])
        num-axies @(rf/subscribe [:my-axies/count])
        total-axies @(rf/subscribe [:my-axies/total])]
    [:div.row.middle-xs {:style {:margin-bottom "0.1em"}}
     [:div.col-xs-1.end-xs
      [:span "Axies"]]
     [:div.col-xs-10
      [loading-bar num-axies total-axies "#00b8ce"]]
     [:div.col-xs-1.end-xs
      [:button
       {:disabled loading?
        :on-click #(rf/dispatch [:my-axies/fetch true])}
       "Reload"]]]))

(defn teams-bar
  []
  (let [loading? @(rf/subscribe [:teams/loading?])
        num-teams @(rf/subscribe [:teams/count])
        total-teams @(rf/subscribe [:teams/total])]
    [:div.row.middle-xs {:style {:margin-bottom "0.1em"}}
     [:div.col-xs-1.end-xs
      [:span "Teams"]]
     [:div.col-xs-10
      [loading-bar num-teams total-teams "#6cc000"]]
     [:div.col-xs-1.end-xs
      [:button
       {:disabled loading?
        :on-click #(rf/dispatch [:teams/fetch-teams true])}
       "Reload"]]]))

(defn header
  [title bars]
  [:div.row
   (let [bars (set bars)]
     [:div.col-xs-12
      (when (:my-axies bars) [my-axies-bar])
      (when (:teams bars) [teams-bar])])
   [:div.col-xs-12.center-xs
    [:h1 title]]])

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
     [:ul.tools-list
      [:li [:a {:href "/battle-simulator"}
            "Battle Simulator"]]
      [:li [:a {:href "/my-axies"}
            "My Axies"]]
      [:li [:a {:href "/breedable"}
            "Breedable"]]
      [:li [:a {:href "/teams"}
            "Teams"]]
      [:li [:a {:href "/unassigned"}
            "Unassigned Axies"]]
      [:li [:a {:href "/multi-assigned"}
            "Multi-Assigned Axies"]]
      [:li [:a {:href "/morph-to-petite"}
            "Morph to Petite"]]
      [:li [:a {:href "/morph-to-adult"}
            "Morph to Adult"]]
      [:li [:a {:href "/multi-gifter"}
            "Multi-Gifter"]]]]]
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
   [header "Battle Simulator"]
   (let [attacker @(rf/subscribe [:battle-simulator/attacker])
         defender @(rf/subscribe [:battle-simulator/defender])
         simulation (rf/subscribe [:battle-simulator/simulation])
         atk-id (r/atom (:id attacker))
         def-id (r/atom (:id defender))]
     [:div
      [:form {:on-submit (fn [e]
                           (.preventDefault e)
                           (rf/dispatch [:battle-simulator/simulate @atk-id @def-id]))}
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

(def axie-table-column-model
  [{:header "ID"
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
    :key :dps}])

(defn axie-table-render-cell
  [{:keys [key]} row _ _]
  (let [value (get row key)]
    (case key
      :id [:a {:href (format "https://axieinfinity.com/axie/%s" value)
               :target "_blank"}
           value]
      :image [:img {:style {:width "100%"}
                    :src value}]
      :team-name [:a {:href (format "https://axieinfinity.com/team/%s" (get row :team-id))
                      :target "_blank"}
                  value]
      :gift-button (let [to-addr @(rf/subscribe [:multi-gifter/to-addr])]
                     [:button
                      {:disabled (nil? to-addr)
                       :on-click #(rf/dispatch [:multi-gifter/send to-addr (:id row)])}
                      "Gift"])
      value)))

(defn my-axies-sort-button
  [title sort-key active-sort-key]
  [:button
   {:style {:border-radius "0.3em"
            :padding "0.35em"
            :margin "0 0.1em"}
    :disabled (= sort-key active-sort-key)
    :on-click #(rf/dispatch [::events/set-my-axies-sort-key sort-key])}
   title])

(defn axie-sorter
  []
  (let [sort-key @(rf/subscribe [:my-axies/sort-key])]
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
       [my-axies-sort-button "DPS" :dps sort-key]]]]))

(defn my-axies-table
  [axies-sub]
  (let [axies (rf/subscribe [axies-sub])]
    [:div.row
     [:div.col-xs-12
      [axie-sorter]]
     [:div.col-xs-12
      [rt/reagent-table
       axies
       {:table {:class "table table-striped"
                :style {:margin "0 auto"}}
        :column-model axie-table-column-model
        :render-cell axie-table-render-cell}]]]))

(defn my-axies-panel
  []
  (let [loading? @(rf/subscribe [:my-axies/loading?])]
    [:div.container
     [header "My Axies" [:my-axies]]
     (if loading?
       [:div.row
        [:div.col-xs-12.center-xs
         [:em "loading..."]]]
       [my-axies-table :my-axies/axies])
     [footer]]))

(defn breedable-panel
  []
  (let [loading? @(rf/subscribe [:my-axies/loading?])]
    [:div.container
     [header "Breedable" [:my-axies]]
     (if loading?
       [:div.row
        [:div.col-xs-12.center-xs
         [:em "loading..."]]]
       [my-axies-table :my-axies/breedable])
     [footer]]))

(defn teams-panel
  []
  (let [loading? @(rf/subscribe [:teams/loading?])
        teams @(rf/subscribe [:teams/teams])]
    [:div.container
     [header "Teams" [:teams]]
     (if loading?
       [:div.row
        [:div.col-xs-12.center-xs
         [:em "loading teams..."]]]
       [:div.row
        [:div.col-xs-12
         (for [t teams]
           [:div.row {:key (:team-id t)}
            [:div.col-xs-12
             [:div.row {:style {:background-color "#DDDDDD"
                                :padding "0.4em 0"}}
              [:div.col-xs-6
               [:strong (:name t)]]
              [:div.col-xs-6.end-xs
               (if (:ready? t)
                 [:em "ready"]
                 [:span (format "ready in %sm" (:ready-in t))])]]
             [:div.row
              [:div.col-xs-12
               (let [axies (rf/subscribe [:teams/team-axies (:team-id t)])]
                 [rt/reagent-table
                  axies
                  {:column-model axie-table-column-model
                   :render-cell axie-table-render-cell}])]]]])]
        ])
     [footer]]))

(defn unassigned-panel
  []
  (let [axies-loading? @(rf/subscribe [:my-axies/loading?])
        teams-loading? @(rf/subscribe [:teams/loading?])]
    [:div.container
     [header "Unassigned Axies" [:my-axies :teams]]
     (if (or axies-loading? teams-loading?)
       [:div.row
        [:div.col-xs-12.center-xs
         [:em "loading..."]]]
       (let [axies (rf/subscribe [:teams/unassigned-axies])]
         [:div.row
          [:div.col-xs-12
           [axie-sorter]]
          [:div.col-xs-12
           [rt/reagent-table
            axies
            {:table {:class "table table-striped"
                     :style {:margin "0 auto"}}
             :column-model axie-table-column-model
             :render-cell axie-table-render-cell}]]]))
     [footer]]))

(defn multi-assigned-panel
  []
  (let [loading? @(rf/subscribe [:teams/loading?])
        axies (rf/subscribe [:teams/multi-assigned-axies])]
    [:div.container
     [header "Multi-Assigned Axies" [:my-axies :teams]]
     [:div.row
      [:div.col-xs-12.center-xs
       [:p "(These axies are on more than one team right now.)"]]]
     (if loading?
       [:div.row
        [:div.col-xs-12.center-xs
         [:em "loading..."]]]
       (if (empty? @axies)
         [:div.row
          [:div.col-xs-12.center-xs
           [:em "you have no axies assigned to multiple teams"]]]
         [:div.row
          [:div.col-xs-12
           [rt/reagent-table
            axies
            {:table {:class "table table-striped"
                     :style {:margin "0 auto"}}
             :column-model (conj axie-table-column-model {:header "Team"
                                                          :key :team-name})
             :render-cell axie-table-render-cell}]]]))
     [footer]]))

(defn morph-to-petite-panel
  []
  (let [loading? @(rf/subscribe [:my-axies/loading?])]
    [:div.container
     [header "Morph to Petite" [:my-axies]]
     (if loading?
       [:div.row
        [:div.col-xs-12.center-xs
         [:em "loading..."]]]
       (if (empty? @(rf/subscribe [:my-axies/larva]))
         [:div.row
          [:div.col-xs-12.center-xs
           [:em "you have no axies that are ready to morph to petite"]]]
         [:div.row
          [:div.col-xs-12
           [:div.row
            [:div.col-xs-12
             [my-axies-table :my-axies/larva]]]
           [:div.row
            [:div.col-xs-12.center-xs
             [:p
              [:a {:href "https://dappsuniverse.com/axie/mass-morph"
                   :target "_blank"}
               "go Mass Morph them"]]]]]]))
     [footer]]))

(defn morph-to-adult-panel
  []
  (let [loading? @(rf/subscribe [:my-axies/loading?])]
    [:div.container
     [header "Morph to Adult" [:my-axies]]
     (if loading?
       [:div.row
        [:div.col-xs-12.center-xs
         [:em "loading..."]]]
       (if (empty? @(rf/subscribe [:my-axies/petite]))
         [:div.row
          [:div.col-xs-12.center-xs
           [:em "you have no axies that are ready to morph to adult"]]]
         [:div.row
          [:div.col-xs-12
           [:div.row
            [:div.col-xs-12
             [my-axies-table :my-axies/petite]]]
           [:div.row
            [:div.col-xs-12.center-xs
             [:p
              [:a {:href "https://dappsuniverse.com/axie/mass-morph"
                   :target "_blank"}
               "go Mass Morph them"]]]]]]))
     [footer]]))

(defn multi-gifter-panel
  []
  (let [loading? @(rf/subscribe [:my-axies/loading?])
        to-addr @(rf/subscribe [:multi-gifter/to-addr])]
    [:div.container
     [header "Multi-Gifter" [:my-axies]]
     [:div.row
      [:div.col-xs-12.center-xs
       [:p "You can send gifts faster this way."]]]
     (if loading?
       [:div.row
        [:div.col-xs-12.center-xs
         [:em "loading..."]]]
       [:div.row
        [:div.col-xs-12
         [:div.row
          [:div.col-xs-12.center-xs
           [:h2 "The first thing you have to do is set the recipient's address:"]
           [:p
            [:input {:type "text"
                     :style {:padding "0.4em"
                             :font-size "1.2em"
                             :text-align "center"
                             :width "60%"}
                     :default-value to-addr
                     :on-change (fn [e]
                                  (rf/dispatch [:multi-gifter/set-to-addr
                                                (-> e .-target .-value)]))}]]]]
         [:div.row
          [:div.col-xs-12.center-xs
           [:h2 "and then you can send any axies as gifts just by clicking the Gift button..."]
           (let [axies (rf/subscribe [:my-axies/axies])]
             [:div.row
              [:div.col-xs-12
               [axie-sorter]]
              [:div.col-xs-12
               [rt/reagent-table
                axies
                {:table {:class "table table-striped"
                         :style {:margin "0 auto"}}
                 :column-model (conj axie-table-column-model {:header ""
                                                              :key :gift-button})
                 :render-cell axie-table-render-cell}]]])]]]])
     [footer]]))

(defn panels
  [panel]
  (case panel
    :home-panel [home-panel]
    :battle-simulator-panel [battle-simulator-panel]
    :my-axies-panel [my-axies-panel]
    :breedable-panel [breedable-panel]
    :teams-panel [teams-panel]
    :unassigned-panel [unassigned-panel]
    :multi-assigned-panel [multi-assigned-panel]
    :morph-to-petite-panel [morph-to-petite-panel]
    :morph-to-adult-panel [morph-to-adult-panel]
    :multi-gifter-panel [multi-gifter-panel]
    [home-panel]))

(defn show-panel
  [panel]
  [panels panel])

(defn main-panel []
  (let [active-panel @(rf/subscribe [::subs/active-panel])]
    [show-panel active-panel]))
