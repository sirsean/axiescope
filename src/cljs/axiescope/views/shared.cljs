(ns axiescope.views.shared
  (:require
    [re-frame.core :as rf]
    [reagent-table.core :as rt]
    [reagent-data-table.core :as rdt]
    [cuerdas.core :refer [format]]
    [clojure.string :as string]
    [axiescope.moves :as moves]
    [axiescope.views.color :as color]
    [axiescope.util :refer [round]]
    ))

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

(defn axie-stat-list
  [axie pairs]
  [:div
   (for [[title k] pairs]
     (let [value (get axie k)]
       [:div.row.middle-xs {:key k
                            :style {:line-height "1.5em"}}
        [:div.col-xs-6.end-xs {:style {:margin-right "-0.6em"}}
         [:strong title]]
        [:div.col-xs-6 (if (string/blank? value)
                         "n/a"
                         value)]]))])

(defn axie-info
  [axie]
  (when (:id axie)
    [:div {:style {:padding "1.5em"}}
     [:div.row
      [:div.col-xs-12.col-md-6
       [axie-stat-list axie [["Class" :class]
                             ["Purity" :purity]
                             ["Breeds" :breed-count]
                             ["Title" :title]
                             ["Mystic" :num-mystic]]]]
      [:div.col-xs-12.col-md-6
       [axie-stat-list axie [["HP" :hp]
                             ["Speed" :speed]
                             ["Skill" :skill]
                             ["Morale" :morale]]]]]
     (when (= 2 (:stage axie))
       [:div.row {:style {:margin-top "1.8em"}}
        [:div.col-xs-6.end-xs [:strong "To Petite"]]
        [:div.col-xs-6
         (.fromNow (:morph-to-petite axie))]])
     (when (= 3 (:stage axie))
       [:div.row {:style {:margin-top "1.8em"}}
        [:div.col-xs-6.end-xs [:strong "To Adult"]]
        [:div.col-xs-6
         (.fromNow (:morph-to-adult axie))]])]))

(defn sort-key-button
  [section title sort-key active-sort-key]
  (let [active? (= sort-key active-sort-key)]
    [:button
     {:style {:padding "4px 8px"
              :margin "0 0.1em"
              :background-color (if active?
                                  "#2277bb"
                                  "#bcd6ea")
              :color (if active?
                       "white"
                       "black")
              :border "none"
              :outline "none"
              :border-radius "1em"}
      :disabled active?
      :on-click #(rf/dispatch [(keyword section :set-sort-key) sort-key])}
     title]))

(defn sort-order-button
  [section order active-order]
  (let [active? (= order active-order)]
    [:button
     {:style {:padding "4px 8px"
              :margin "0 0.1em"
              :background-color (if active?
                                  "#2277bb"
                                  "#bcd6ea")
              :color (if active?
                       "white"
                       "black")
              :border "none"
              :outline "none"
              :border-radius "1em"}
      :disabled active?
      :on-click #(rf/dispatch [(keyword section :set-sort-order) order])}
     (name order)]))

(defn sorter
  [{:keys [section fields only-field-keys]
    :or {section :my-axies
         fields []
         only-field-keys nil}}]
  (let [sort-key @(rf/subscribe [(keyword section :sort-key)])
        sort-order @(rf/subscribe [(keyword section :sort-order)])]
    [:div.row
     [:div.col-xs-12.center-xs
      {:style {:margin "0.4em"}}
      [:span {:style {:padding-right "0.3em"}}
       "sort by:"]
      (->> fields
           (filter (fn [[_ k]]
                     (or (nil? only-field-keys)
                         (only-field-keys k))))
           (map (fn [[title k]]
                  ^{:key k}
                  [sort-key-button section title k sort-key])))]
     [:div.col-xs-12.center-xs
      {:style {:margin "0.4em"}}
       [:span {:style {:padding-left "0.6em"
                       :padding-right "0.3em"}}
        "order:"]
       [sort-order-button section :asc sort-order]
       [sort-order-button section :desc sort-order]]]))

(defn axie-sorter
  [{:keys [section extra-fields only-field-keys]
    :or {section :my-axies
         extra-fields []
         only-field-keys nil}}]
  [sorter {:section section
           :only-field-keys only-field-keys
           :fields (concat [["ID" :id]
                            ["Name" :name]
                            ["Class" :class]
                            ["Purity" :purity]
                            ["Breeds" :breed-count]
                            ["Attack" :attack]
                            ["Defense" :defense]
                            ["Energy" :energy]
                            ["Atk+Def" :atk+def]
                            ["Overall ELO" :all-rating]
                            ["Atk ELO" :attack-rating]
                            ["Def ELO" :defense-rating]]
                           extra-fields)}])

(defn axies-pager
  [section]
  (let [offset @(rf/subscribe [(keyword section :offset)])
        page-size @(rf/subscribe [(keyword section :page-size)])
        total @(rf/subscribe [(keyword section :total)])
        prev? (>= (- offset page-size) 0)
        next? (<= (+ offset page-size) total)]
    [:div.row {:style {:margin "1.2em 0 0.2em 0"}}
     [:div.col-xs-5.end-xs
      (when prev?
        [:button
         {:on-click #(rf/dispatch [(keyword section :set-offset) (- offset page-size)])}
         "Previous"])]
     [:div.col-xs-2.center-xs
      (when (< page-size total)
        [:span
         (format "%s-%s of %s"
                 (inc offset)
                 (+ page-size offset)
                 total)])]
     [:div.col-xs-5
      (when next?
        [:button
         {:on-click #(rf/dispatch [(keyword section :set-offset) (+ offset page-size)])}
         "Next"])]]))

(def axie-table-headers
  [[:id "ID"]
   [:image ""]
   [:name "Name"]
   [:breed-count "Breeds"]
   [:attack "Attack"]
   [:defense "Defense"]
   [:energy "Energy"]
   [:atk+def "Atk+Def"]
   [:all-rating "Overall ELO"]
   [:attack-rating "Atk ELO"]
   [:defense-rating "Def ELO"]])

(defn parts-row
  [{:keys [parts class]}]
  (when (seq parts)
    (let [c (keyword class)]
      [:div {:style {:width "185px"
                     :display "inline-block"
                     :background-color (color/classes c)
                     :padding "0.2em"
                     :border-radius "0.2em"}}
       (for [p parts
             :let [c (keyword (:class p))
                   m (or (-> p :moves first)
                         (:move p))]]
         [:div {:key (:id p)
                :style {:display "inline-block"
                        :background-color (color/classes c)
                        :vertical-align "top"
                        :width "18px"
                        :margin "0 1px"
                        :padding "0.2em"
                        :border-radius "0.2em"}}
          (if (some? m)
            (let [tank (-> p :id moves/tank-part-score)
                  dps (-> p :id moves/dps-part-score)]
              [:div {:style {:vertical-align "middle"}}
               (when (pos? tank)
                 [:div {:style {:height "30px"
                                :opacity (+ 0.2 (* tank 0.2))}}
                  [:div tank]
                  [:div {:style {:font-size "0.5em"}}
                   "Tank"]])
               (when (pos? dps)
                 [:div {:style {:height "30px"
                                :opacity (+ 0.2 (* dps 0.2))}}
                  [:div dps]
                  [:div {:style {:font-size "0.5em"}}
                   "DPS"]])
               (when (and (zero? tank) (zero? dps))
                 [:div {:style {:height "30px"
                                :opacity 0.2}}
                  "0"])])
            [:div {:style {:height "30px"
                           :opacity 0.2}}
             "0"])])])))

(defn axie-row-render-fn
  [row key]
  (let [value (get row key)]
    (case key
      :id [:td
           [:a {:href (format "https://axieinfinity.com/axie/%s" value)
                :target "_blank"}
            value]]
      :name [:td {:style {:max-width "80px"}}
             [:a {:href (format "/axie/%s" (get row :id))}
              value]]
      :image [:td {:style {:max-width "20px"}}
              [:img {:style {:width "100%"}
                     :src value}]]
      :all-rating [:td
                   [:span (round value 0)]]
      :attack-rating [:td
                      [:span (round value 0)]]
      :defense-rating [:td
                       [:span (round value 0)]]
      :price [:td {:style {:max-width "50px"}}
              [:span (round value 8)]]
      :parts [:td {:style {:max-width "200px"
                           :text-align "center"}}
              [parts-row row]]

      :sire-selector
      (let [axie-id (get row :id)
            selected? (get row :selected-sire?)]
        [:td
         [:button
          {:style {:background-color (if selected?
                                       "#2277bb"
                                       "#bcd6ea")
                   :color (if selected?
                            "white"
                            "black")
                   :padding "4px 8px"
                   :border "none"
                   :outline "none"
                   :border-radius "1em"}
           :on-click #(rf/dispatch [:breedable/select-sire row])}
          "Sire"]])

      :matron-selector
      (let [sire @(rf/subscribe [:breedable/sire])
            axie-id (get row :id)]
        [:td
         (if (some? sire)
           (if (:can-breed-with-sire? row)
             [:div
              [:div
               (if (some? (:quick-calc row))
                 [:span
                  (round (:tank-body (:quick-calc row)) 2)
                  " | "
                  (round (:dps-body (:quick-calc row)) 2)
                  " | "
                  (round (:support-body (:quick-calc row)) 2)]
                 [:a {:on-click #(rf/dispatch [:breed-calc/quick-calc sire row])
                      :href "#"}
                  "Predict"])]
              [:div
               [:a {:style {:font-size "0.8em"}
                    :href (format "/breed/calc/%s/%s" (:id sire) axie-id)}
                "View Prediction"]]]
             [:span "-"])
           [:em "select a sire"])])

      :gift-button [:td
                    (let [to-addr @(rf/subscribe [:multi-gifter/to-addr])]
                      [:button
                       {:disabled (string/blank? to-addr)
                        :on-click #(rf/dispatch [:multi-gifter/send to-addr (:id row)])}
                       "Gift"])]
      [:td value])))

(defn my-axies-table
  [{:keys [section sub headers extra-sort-fields]
    :or {section :my-axies
         headers axie-table-headers
         extra-sort-fields []}}]
  (let [axies @(rf/subscribe [sub])]
    [:div.row
     [:div.col-xs-12
      [axie-sorter {:section section
                    :extra-fields extra-sort-fields}]]
     [:div.col-xs-12
      [rdt/data-table
       {:headers headers
        :rows axies
        :td-render-fn axie-row-render-fn}]]]))
