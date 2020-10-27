(ns axiescope.views.panels.combo-rankings
  (:require
    [clojure.string :as string]
    [re-frame.core :as rf]
    [reagent-data-table.core :as rdt]
    [accountant.core :as accountant]
    [cuerdas.core :refer [format]]
    [axiescope.views.layout :refer [header footer]]
    [axiescope.util :refer [round]]
    ))

(def rankings-headers
  [[:rank "Rank"]
   [:part-name "Parts"]
   [:skill-name "Skills"]
   [:trigger-text "Moves"]
   [:rating "Rating"]
   [:shop "Shop"]])

(defn rankings-render-fn
  [row key]
  (let [value (get row key)]
    (case key
      :part-name
      [:td
       (->> row :cards (map :part-name) (string/join ", "))]

      :skill-name
      [:td
       (->> row :cards (map :skill-name) (string/join ", "))]

      :trigger-text
      [:td
       (->> row :cards (map :trigger-text) (string/join ", "))]

      :rating
      [:td (round value 0)]

      :shop
      [:td
       [:a {:href (format
                    "https://marketplace.axieinfinity.com/axie?%s"
                    (->> row :cards (map :part-id) (map (partial str "part=")) (string/join "&")))
            :target "_blank"}
        "Shop"]]

      [:td value])))

(defn rankings-table
  [rankings]
  [rdt/data-table
   {:headers      rankings-headers
    :rows         rankings
    :td-render-fn rankings-render-fn}])

(defn panel
  []
  (let [loading? @(rf/subscribe [:combo-rankings/loading?])
        rankings @(rf/subscribe [:combo-rankings/rankings])]
    [:div.container
     [header {:title "Combo Rankings"}]
     [:div.row
      [:div.col-xs-12
       [:div.row
        [:div.col-xs-12.center-xs
         [:p "Vote for the card combos you think are better, to help the community come up with a shared ranking."]]
        [:div.col-xs-12.center-xs
         [:button {:on-click (fn [e]
                               (.preventDefault e)
                               (accountant/navigate!
                                 "/combo-rankings/vote"))
                   :style {:padding "0.9em"
                           :margin "0.5em"
                           :font-size "1.1em"
                           :background-color "#2277bb"
                           :color "white"
                           :border "none"
                           :outline "none"
                           :border-radius "1.8em"}}
          "Help By Voting!"]
         [:button {:on-click (fn [e]
                               (.preventDefault e)
                               (accountant/navigate!
                                 "/combo-rankings/add"))
                   :style {:padding "0.9em"
                           :margin "0.5em"
                           :font-size "1.1em"
                           :background-color "#2277bb"
                           :color "white"
                           :border "none"
                           :outline "none"
                           :border-radius "1.8em"}}
          "Add a New Combo!"]]]
       (if loading?
         [:div.row
          [:div.col-xs-12.center-xs
           [:em "loading..."]]]
         [:div.row
          [:div.col-xs-12
           [rankings-table rankings]]])]]
     [footer]]))
