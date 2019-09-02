(ns axiescope.views.panels.team-builder
  (:require
    [clojure.string :refer [blank?]]
    [reagent.core :as r]
    [re-frame.core :as rf]
    [reagent-data-table.core :as rdt]
    [axiescope.team-builder :refer [team-role]]
    [axiescope.views.layout :refer [header footer]]
    [axiescope.views.shared :refer [axie-sorter axie-row-render-fn parts-row]]
    ))

(def axie-table-headers
  [[:id "ID"]
   [:image ""]
   [:name "Name"]
   [:parts ""]
   [:attack "Attack"]
   [:defense "Defense"]
   [:atk+def "Atk+Def"]
   [:tank-body "Tank"]
   [:dps-body "DPS"]
   [:support-body "Support"]
   [:team-builder "Select Axie"]])

(defn layout->title
  [layout]
  (case layout
    :1tank-1dps-1support "1 Tank, 1 DPS, 1 Support"
    :2tank-1dps "2 Tanks, 1 DPS"
    :1tank-2dps "1 Tank, 2 DPS"
    :vertical "Vertical"
    :horizontal "Horizontal"
    "?"))

(defn layout-selector
  [layout selected]
  [:li
   [:a {:style {:font-weight (if (= layout selected) "bold" "normal")}
        :href "#"
        :on-click #(rf/dispatch [:team-builder/set-layout layout])}
    (layout->title layout)]])

(defn selected-axie
  [selected-axies index layout]
  (when-some [axie (get selected-axies index)]
    [:div.col-xs-3
     [:div.row
      [:div.col-xs-12.center-xs
       [:h3 (team-role layout index)]]]
     [:div.row
      [:div.col-xs-12.center-xs
       (:name axie)]]
     [:div.row
      [:div.col-xs-12.center-xs
       [parts-row axie]]]
     [:div.row
      [:div.col-xs-12
       [:div.row
        [:div.col-xs-6]
        [:div.col-xs-2 [:strong "Atk"]]
        [:div.col-xs-2 [:strong "Def"]]]
       (for [part-index (range (count (:move-set axie)))
             :let [part (-> axie :move-set (nth part-index))]]
         ^{:key (:id part)}
         [:div
          [:div.row
           [:div.col-xs-6 [:strong (:name part)]]
           [:div.col-xs-2 (-> part :moves first :attack)]
           [:div.col-xs-2 (-> part :moves first :defense)]
           [:div.col-xs-2
            [:a {:style {:text-decoration "none"}
                 :on-click #(rf/dispatch [:team-builder/move-down index part-index])
                 :href "#"}
             "▼"]
            [:a {:style {:text-decoration "none"}
                 :on-click #(rf/dispatch [:team-builder/move-up index part-index])
                 :href "#"}
             "▲"]]
           ]
          [:div.row
           [:div.col-xs-11.col-xs-offset-1
            {:style {:font-size "0.7em"}}
            (-> part :moves first :effects first :description)]]])
       ]]]))

(defn panel
  []
  (let [axies-loading? @(rf/subscribe [:my-axies/loading?])
        teams-loading? @(rf/subscribe [:teams/loading?])]
    [:div.container
     [header {:title "Team Builder"
              :bars [:my-axies :teams]}]
     (if (or axies-loading? teams-loading?)
       [:div.row
        [:div.col-xs-12.center-xs
         [:em "loading..."]]]
       (let [layout @(rf/subscribe [:team-builder/layout])
             selected-axies @(rf/subscribe [:team-builder/selected-axies])
             team-name @(rf/subscribe [:team-builder/team-name])
             can-create? @(rf/subscribe [:team-builder/can-create?])
             has-token? @(rf/subscribe [:auto-battle/has-token?])]
         [:div
          [:form
           {:on-submit (fn [e]
                         (.preventDefault e)
                         (if has-token?
                           (rf/dispatch [:team-builder/create])
                           (rf/dispatch [:auto-battle/generate-token
                                         {:after-handlers [[:team-builder/create]]}])))}
           [:div.row.middle-xs
            [:div.col-xs-4.col-xs-offset-4.center-xs
             [:input {:type "text"
                      :default-value team-name
                      :on-change (fn [e]
                                   (rf/dispatch [:team-builder/set-name (-> e .-target .-value)]))
                      :style {:width "80%"
                              :padding "0.5em"
                              :font-size "1.2em"}}]]
            [:div.col-xs-4
             [:button
              {:disabled (not can-create?)}
              "Create Team"]]]]
          [:div.row
           [:div.col-xs-3
            [:h3 "Layout"]
            [:ul
             (for [lo [:1tank-1dps-1support
                       :2tank-1dps
                       :1tank-2dps
                       :vertical
                       :horizontal]]
               ^{:key lo}
               [layout-selector lo layout])]]
           [selected-axie selected-axies 0 layout]
           [selected-axie selected-axies 1 layout]
           [selected-axie selected-axies 2 layout]]
          (let [axies @(rf/subscribe [:teams/unassigned-axies])]
            (if (empty? axies)
              [:div.row
               [:div.col-xs-12.center-xs
                [:em "you have no unassigned axies"]]]
              [:div.row
               [:div.col-xs-12
                [axie-sorter {:section :my-axies
                              :only-field-keys #{:id :name
                                                 :attack :defense :atk+def
                                                 :tank-body :dps-body :support-body}}]]
               [:div.col-xs-12
                [rdt/data-table
                 {:rows axies
                  :headers axie-table-headers
                  :td-render-fn axie-row-render-fn}]]]))]))
     [footer]]))
