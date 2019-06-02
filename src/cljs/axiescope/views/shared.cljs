(ns axiescope.views.shared
  (:require
    [re-frame.core :as rf]
    [reagent-table.core :as rt]
    [cuerdas.core :refer [format]]
    [clojure.string :as string]
    [axiescope.moves :as moves]
    ))

(defn round
  [d precision]
  (let [factor (Math/pow 10 precision)]
    (/ (Math/round (* d factor)) factor)))

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
     [:div.row.middle-xs {:key k}
      [:div.col-xs-6.end-xs [:strong title]]
      [:div.col-xs-6 (get axie k)]])])

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
                             ["Mystic" :num-mystic]
                             ["Price" :price]
                             ["EXP" :exp]
                             ["Pending EXP" :pending-exp]
                             ["Next Breed" :next-breed]]]]
      [:div.col-xs-12.col-md-6
       [axie-stat-list axie [["HP" :hp]
                             ["Speed" :speed]
                             ["Skill" :skill]
                             ["Morale" :morale]
                             ["Attack" :attack]
                             ["Defense" :defense]
                             ["Atk+Def" :atk+def]
                             ["Tank" :tank]
                             ["DPS" :dps]]]]]
     (when (= 2 (:stage axie))
       [:div.row {:style {:margin-top "1em"}}
        [:div.col-xs-6.end-xs [:strong "To Petite"]]
        [:div.col-xs-6
         (.fromNow (:morph-to-petite axie))]])
     (when (= 3 (:stage axie))
       [:div.row {:style {:margin-top "1em"}}
        [:div.col-xs-6.end-xs [:strong "To Adult"]]
        [:div.col-xs-6
         (.fromNow (:morph-to-adult axie))]])
     [:div.row {:style {:margin-top "1em"}}
      [:div.col-xs-12
       [:div.row
        [:div.col-xs-2 [:strong "type"]]
        [:div.col-xs-2 [:strong "class"]]
        [:div.col-xs-4 [:strong "name"]]
        [:div.col-xs-1.end-xs [:strong "atk"]]
        [:div.col-xs-1.end-xs [:strong "def"]]
        [:div.col-xs-1.end-xs [:strong "tank"]]
        [:div.col-xs-1.end-xs [:strong "dps"]]]
       (for [p (:parts axie)]
         [:div.row.middle-xs {:key (:id p)
                              :style {:background-color (when (:mystic p) "#54b0e6")
                                      :padding "0.2em 0"
                                      :border-radius "0.25em"}}
          [:div.col-xs-2 (:type p)]
          [:div.col-xs-2 (:class p)]
          [:div.col-xs-4 (:name p)]
          [:div.col-xs-1.end-xs (-> p :moves first :attack)]
          [:div.col-xs-1.end-xs (-> p :moves first :defense)]
          [:div.col-xs-1.end-xs (some-> p :id moves/tank-part-score)]
          [:div.col-xs-1.end-xs (some-> p :id moves/dps-part-score)]
          (when-some [desc (-> p :moves first :effects first :description)]
            [:div.col-xs-8.col-xs-offset-4
             {:style {:font-size "0.7em"}}
             desc])])]]]))

(defn sort-key-button
  [section title sort-key active-sort-key]
  [:button
   {:style {:border-radius "0.3em"
            :padding "0.35em"
            :margin "0 0.1em"}
    :disabled (= sort-key active-sort-key)
    :on-click #(rf/dispatch [(keyword section :set-sort-key) sort-key])}
   title])

(defn sort-order-button
  [section order active-order]
  [:button
   {:style {:border-radius "0.3em"
            :padding "0.35em"
            :margin "0 0.1em"}
    :disabled (= order active-order)
    :on-click #(rf/dispatch [(keyword section :set-sort-order) order])}
   (name order)])

(defn axie-sorter
  [{:keys [section extra-fields]
    :or {section :my-axies
         extra-fields []}}]
  (let [sort-key @(rf/subscribe [(keyword section :sort-key)])
        sort-order @(rf/subscribe [(keyword section :sort-order)])]
    [:div.row
     [:div.col-xs-12.center-xs
      [:p
       [:span {:style {:padding-right "0.3em"}}
        "sort by:"]
       (->> extra-fields
            (concat [["ID" :id]
                     ["Class" :class]
                     ["Purity" :purity]
                     ["Breeds" :breed-count]
                     ["Attack" :attack]
                     ["Defense" :defense]
                     ["Atk+Def" :atk+def]
                     ["Tank" :tank]
                     ["DPS" :dps]])
            (map (fn [[title k]]
                   ^{:key k}
                   [sort-key-button section title k sort-key])))
       [:span {:style {:padding-left "0.6em"
                       :padding-right "0.3em"}}
        "order:"]
       [sort-order-button section :asc sort-order]
       [sort-order-button section :desc sort-order]]]]))

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
      :name [:a {:href (format "/axie/%s" (get row :id))}
             value]
      :image [:img {:style {:width "100%"}
                    :src value}]
      :team-name [:a {:href (format "https://axieinfinity.com/team/%s" (get row :team-id))
                      :target "_blank"}
                  value]
      :gift-button (let [to-addr @(rf/subscribe [:multi-gifter/to-addr])]
                     [:button
                      {:disabled (string/blank? to-addr)
                       :on-click #(rf/dispatch [:multi-gifter/send to-addr (:id row)])}
                      "Gift"])
      value)))

(defn my-axies-table
  [{:keys [section sub column-model extra-sort-fields]
    :or {section :my-axies
         column-model axie-table-column-model
         extra-sort-fields []}}]
  (let [axies (rf/subscribe [sub])]
    [:div.row
     [:div.col-xs-12
      [axie-sorter {:section section
                    :extra-fields extra-sort-fields}]]
     [:div.col-xs-12
      [rt/reagent-table
       axies
       {:table {:class "table table-striped"
                :style {:margin "0 auto"}}
        :column-model column-model
        :render-cell axie-table-render-cell}]]]))
