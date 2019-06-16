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
       {:style {:margin-left "12px"
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
                    :margin-left "12px"}}
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
                      :margin-right "-14px"
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

(defn landing
  []
  (let [logged-in? @(rf/subscribe [:axiescope/logged-in?])
        axie-id-atom (r/atom nil)
        account @(rf/subscribe [:axiescope/account])
        views-available (- (:family-tree-paid account) (:family-tree-views account))
        price-tiers @(rf/subscribe [:axiescope.prices.family-tree/tiers])
        previously-viewed @(rf/subscribe [:axiescope.family-tree/views])]
    [:div.row
     [:div.col-xs-6
      (when-not logged-in?
        [:div.row
         [:div.col-xs-12.center-xs
          [:p "You need to log in, so we know who you are."]
          [:button {:on-click #(rf/dispatch [:axiescope/auth {:after-handlers [[:axiescope.family-tree.views/fetch]]}])
                    :style {:padding "0.9em"
                            :background-color "#2277bb"
                            :color "white"
                            :border "none"
                            :outline "none"
                            :border-radius "1.8em"}}
           "Login"]
          [:p "Please sign this message, and the result is a secret token between us."]]])
      (when logged-in?
        [:div.row.middle-xs
         [:div.col-xs-8.col-xs-offset-2.center-xs
          [:p (format "You have %s view%s available."
                      views-available
                      (when-not (= 1 views-available) "s"))]]
         [:div.col-xs-2.end-xs
          [:button {:on-click #(rf/dispatch [:axiescope.account/fetch])
                    :style {:padding "0.6em"
                            :font-size "0.8em"
                            :background-color "#2277bb"
                            :color "white"
                            :border "none"
                            :outline "none"
                            :border-radius "1.8em"}}
           "Refresh"]]])
      [:div.row {:style {:margin-top "1em"}}
       [:div.col-xs-12.center-xs
        [:div.row {:style {:margin "0.45em 0"
                           :padding-bottom "0.2em"
                           :border-bottom "1px solid #2277bb"
                           :font-weight "bold"}}
         [:div.col-xs-2.end-xs "Views"]
         [:div.col-xs-4.end-xs "USD Price"]
         [:div.col-xs-4.end-xs "ETH Price"]]
        (for [{:keys [views usd eth]} price-tiers]
          [:div.row.middle-xs {:key views
                               :style {:margin "0.45em 0"
                                       :padding-bottom "0.4em"
                                       :border-bottom "1px solid #2277bb"}}
           [:div.col-xs-2.end-xs views]
           [:div.col-xs-4.end-xs (format "$%s" usd)]
           [:div.col-xs-4.end-xs (format "%s ETH" eth)]
           [:div.col-xs-2
            [:button {:disabled (not logged-in?)
                      :on-click #(rf/dispatch [:axiescope.pay/family-tree eth])
                      :style {:padding "0.6em"
                              :background-color (if logged-in?
                                                  "#2277bb"
                                                  "#bcd6ea")
                              :color "white"
                              :border "none"
                              :outline "none"
                              :border-radius "1em"}}
             "Pay"]]])]]
      (when logged-in?
        [:div.row {:style {:margin-top "1em"}}
         [:div.col-xs-12.center-xs
          [:form {:on-submit (fn [e]
                               (when (pos? views-available)
                                 (when-some [id @axie-id-atom]
                                   (rf/dispatch [:axiescope.family-tree/fetch id])))
                               (.preventDefault e))}
           [:div.row.middle-xs
            [:div.col-xs-2.end-xs
             [:span "Axie ID"]]
            [:div.col-xs-8.center-xs
             [:input {:type "text"
                      :on-change (fn [e]
                                   (reset! axie-id-atom (-> e .-target .-value)))
                      :style {:width "100%"
                              :font-size "1.2em"
                              :padding "0.3em"}}]]
            [:div.col-xs-2.center-xs
             [:button {:disabled (zero? views-available)
                       :style {:padding "0.6em"
                               :font-size "1.0em"
                               :font-weight "bold"
                               :background-color (if (pos? views-available)
                                                   "#2277bb"
                                                  "#bcd6ea")
                               :color "white"
                               :border "none"
                               :outline "none"
                               :border-radius "1em"}}
              "Go"]]]]]])
      (when (and logged-in?
                 (seq previously-viewed))
        [:div.row {:style {:margin-top "1em"}}
         [:div.col-xs-12
          [:h4 "Previously Viewed"]
          [:p "You have already paid for these, so you can view them again and it won't count against your views."]
          [:ul
           (for [{:keys [id axie-id]} previously-viewed]
             [:li {:key id}
              [:a {:href "#"
                   :on-click (fn [e]
                               (rf/dispatch [:axiescope.family-tree/fetch axie-id])
                               (.preventDefault e))}
               axie-id]])]]])]
     [:div.col-xs-6
      [:img {:src "/img/lineage-hero.png"
             :style {:width "100%"
                     :margin-left "4em"
                     :-webkit-mask-image "-webkit-gradient(linear, left top, right top, from(rgba(0,0,0,0.5)), to(rgba(0,0,0,0.01)))"}}]]]))

(defn show-family-tree
  []
  (let [loading? @(rf/subscribe [:axiescope.family-tree/loading?])
        error @(rf/subscribe [:axiescope.family-tree/error])
        tree @(rf/subscribe [:axiescope.family-tree/tree])]
    [:div.row
     [:div.col-xs-12
      [:button {:on-click #(rf/dispatch [:axiescope.family-tree/clear])
                :style {:padding "0.6em"
                        :font-size "0.8em"
                        :background-color "#2277bb"
                        :color "white"
                        :border "none"
                        :outline "none"
                        :border-radius "1em"}}
       "< Close Axie"]]
     [:div.col-xs-12
      (cond
        loading?
        [:div.row
         [:div.col-xs-12.center-xs
          [:p "loading..."]]]

        (some? error)
        [:div.row
         [:div.col-xs-12.center-xs
          [:p (format "Error: %s" (:status-text error))]]]

        (some? tree)
        [:div.row
         [:div.col-xs-12
          [show-lineage-list tree]]])]]))

(defn panel
  []
  (let [axie-id @(rf/subscribe [:axiescope.family-tree/axie-id])]
    [:div.container
     [header {:title "Axie Lineage"}]
     (if (some? axie-id)
       [show-family-tree]
       [landing])
     [footer]]))
