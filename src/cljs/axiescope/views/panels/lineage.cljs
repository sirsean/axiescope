(ns axiescope.views.panels.lineage
  (:require
    [re-frame.core :as rf]
    [reagent.core :as r]
    [clojure.string :refer [blank?]]
    [accountant.core :as accountant]
    [cuerdas.core :refer [format]]
    [axiescope.views.layout :refer [header footer]]
    ))

(defn count-generations
  [{:keys [sire matron] :as axie}]
  (if-some [parents (seq (filter some? [sire matron]))]
    (inc (apply max (map count-generations parents)))
    0))

(defn count-ancestors
  [{:keys [sire matron] :as axie}]
  (cond-> 0
    (some? sire) (+ 1 (count-ancestors sire))
    (some? matron) (+ 1 (count-ancestors matron))))

(defn descendants-button
  [{:keys [id] :as axie}]
  (let [expanded? @(rf/subscribe [:axie/family-tree-expanded? id])
        num-descendants (count-ancestors axie)]
    (when (pos? num-descendants)
      [:button
       {:style {:margin-left "6px"
                :padding "4px 8px"
                :color (if expanded?
                         "white"
                         "black")
                :background-color (if expanded?
                                    "#2277bb"
                                    "#bcd6ea")
                :border "none"
                :outline "none"
                :border-radius "1em"}
        :on-click #(rf/dispatch [:axie/family-tree-expand id])}
       num-descendants])))

(defn generations-button
  [{:keys [id] :as axie}]
  (let [expanded? @(rf/subscribe [:axie/family-tree-expanded? id])
        gen (count-generations axie)]
    (when-not (zero? gen)
      [:button
       {:style {:margin-left "6px"
                :padding "4px 8px"
                :color (if expanded?
                         "white"
                         "black")
                :background-color (if expanded?
                                    "#2277bb"
                                    "#bcd6ea")
                :font-size "0.7em"
                :vertical-align "middle"
                :border "none"
                :outline "none"
                :border-radius "1em"}
        :disabled (zero? gen)
        :on-click #(rf/dispatch [:axie/family-tree-expand id])}
       (format "gen %s" gen)])))

(defn title-tag
  [{:keys [title]}]
  (when (some? title)
    [:span {:style {:background-color "#c13884"
                    :color "white"
                    :font-size "0.9em"
                    :padding "9px"
                    :border-radius "0.3em"
                    :vertical-align "middle"
                    :margin-left "6px"}}
     (format "%s" title)]))

(defn pure-tag
  [{:keys [purity]}]
  nil)

(defn show-lineage-list
  [{:keys [id name image title sire matron] :as axie}]
  (let [expanded? @(rf/subscribe [:axie/family-tree-expanded? id])]
    [:ul {:style {:list-style "none"
                  :padding-inline-start "60px"}}
     [:li {:style {:margin-bottom "-2em"}}
      [:div
       [:img {:style {:height "100px"
                      :width "133px"
                      :vertical-align "middle"}
              :src image}]
       [:a {:style {:text-decoration "none"
                    :vertical-align "middle"}
            :href (format "/axie/%s" id)}
        name]
       [generations-button axie]
       [pure-tag axie]
       [title-tag axie]]]
     (when (and expanded? (some? sire))
       [:li [show-lineage-list sire]])
     (when (and expanded? (some? matron))
       [:li [show-lineage-list matron]])]))

(defn panel
  []
  (let [loading? @(rf/subscribe [:axie/loading?])
        axie-id @(rf/subscribe [:axie/axie-id])
        axie-id-atom (r/atom axie-id)
        axie @(rf/subscribe [:axie/axie axie-id])
        family-tree @(rf/subscribe [:axie/family-tree axie-id])]
    [:div.container
     [header "Axie Lineage"]
     [:div.row
      [:div.col-xs-12
       [:form {:on-submit (fn [e]
                            (.preventDefault e)
                            (accountant/navigate! (format "/lineage/%s" @axie-id-atom)))}
        [:div.row.middle-xs
         [:div.col-xs-1.end-xs
          [:span "Axie ID"]]
         [:div.col-xs-10.center-xs
          [:input {:type "text"
                   :name "axie-id"
                   :on-change (fn [e]
                                (reset! axie-id-atom (-> e .-target .-value)))
                   :style {:width "100%"
                           :font-size "1.2em"
                           :padding "0.3em"}}]]
         [:div.col-xs-1.end-xs
          [:button {:style {:padding "0.5em"
                            :font-size "1.0em"}}
           "Evaluate"]]]]]]
     (when-not (blank? axie-id)
       (if (and loading? (nil? axie))
         [:div.row
          [:div.col-xs-12.center-xs
           [:p [:em "loading..."]]]]
         [:div.row
          [:div.col-xs-12
           [show-lineage-list family-tree]]]))
     [footer]]))
