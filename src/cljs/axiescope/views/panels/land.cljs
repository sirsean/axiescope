(ns axiescope.views.panels.land
  (:require
    [re-frame.core :as rf]
    [reagent-table.core :as rt]
    [cuerdas.core :refer [format]]
    [axiescope.views.layout :refer [header footer]]
    [axiescope.views.shared :refer [axies-pager sort-key-button sort-order-button]]
    ))

(defn land-panel
  []
  [:div.container
   [header "Land"]
   (if (not @(rf/subscribe [:land/logged-in?]))
     [:div.row
      [:div.col-xs-12
       [:p "In order to use the Land section, you need to log in."]
       [:p [:a {:href "#"
                :on-click (fn [e]
                            (.preventDefault e)
                            (rf/dispatch [:land/login]))}
            "Login"]]]]
     (let [profile @(rf/subscribe [:land/profile])]
       [:div
        [:div.row
         [:div.col-xs-2.end-xs "Email"]
         [:div.col-xs-6 (:email profile)]]
        [:div.row
         [:div.col-xs-2.end-xs "Account ID"]
         [:div.col-xs-6 (:account-id profile)]]
        [:div.row
         [:div.col-xs-2.end-xs "LOOM address"]
         [:div.col-xs-6 (:loom-address profile)]]
        [:div.row
         [:div.col-xs-12
          [:ul.tools-list
           [:li [:a {:href "/land/items"}
                 "My Items"]]
           [:li [:a {:href "/land/market"}
                 "Marketplace"]]
           [:li [:a {:href "/land/valuation"}
                 "Item Valuation"]]]]]]))
   [footer]])

(defn items-sorter
  [{:keys [section extra-fields]
    :or {section :land-items
         extra-fields []}}]
  (let [sort-key @(rf/subscribe [(keyword section :sort-key)])
        sort-order @(rf/subscribe [(keyword section :sort-order)])]
    [:div.row
     [:div.col-xs-12.center-xs
      [:p
       [:span {:style {:padding-right "0.3em"}}
        "sort by:"]
       (->> extra-fields
            (concat [["Name" :name]
                     ["Count" :num-items]
                     ["Rarity" :rarity-value]])
            (map (fn [[title k]]
                   ^{:key k}
                   [sort-key-button section title k sort-key])))
       [:span {:style {:padding-left "0.6em"
                       :padding-right "0.3em"}}
        "order:"]
       [sort-order-button section :asc sort-order]
       [sort-order-button section :desc sort-order]]]]))

(defn land-items-panel
  []
  [:div.container
   [header "Items" [:items]]
   (let [loading? @(rf/subscribe [:land/items-loading?])
         items (rf/subscribe [:land/items])]
     (if loading?
       [:div.row
        [:div.col-xs-12.center-xs
         [:em "loading..."]]]
       [:div.row
        [:div.col-xs-12
         [items-sorter]
         [rt/reagent-table
          items
          {:table {:class "table table-big table-striped table-wrap"
                   :style {:margin "0 auto"}}
           :column-model [{:header ""
                           :key :image}
                          {:header "Name"
                           :key :name}
                          {:header "Count"
                           :key :num-items}
                          {:header "Rarity"
                           :key :rarity}
                          {:header "Effects"
                           :key :effects}
                          {:headers "IDs"
                           :key :item-ids
                           :attrs {:style {:width "200px"}}}]
           :render-cell (fn [{:keys [key attrs]
                              :or {attrs {}}} row _ _]
                          [:span attrs
                          (case key
                            :image (let [item-alias (get row :item-alias)]
                                     [:img {:style {:max-width "100px"
                                                    :max-height "100px"}
                                            :src (format "https://cdn.axieinfinity.com/terrarium-items/%s.png" item-alias)}])
                            :item-ids (for [{:keys [item-alias item-id]} (get row :all-items)]
                                        [:span {:key item-id}
                                         [:a {:href (format "https://land.axieinfinity.com/item/%s/%s"
                                                            item-alias item-id)
                                              :target "_blank"}
                                          item-id]
                                         " "])
                            (get row key))])}]]]))
   [footer]])

(defn market-sorter
  [{:keys [section extra-fields]
    :or {section :land-market
         extra-fields []}}]
  (let [sort-key @(rf/subscribe [(keyword section :sort-key)])
        sort-order @(rf/subscribe [(keyword section :sort-order)])]
    [:div.row
     [:div.col-xs-12.center-xs
      [:p
       [:span {:style {:padding-right "0.3em"}}
        "sort by:"]
       (->> extra-fields
            (concat [["Name" :name]
                     ["Rarity" :rarity-value]
                     ["Price" :current-price]])
            (map (fn [[title k]]
                   ^{:key k}
                   [sort-key-button section title k sort-key])))
       [:span {:style {:padding-left "0.6em"
                       :padding-right "0.3em"}}
        "order:"]
       [sort-order-button section :asc sort-order]
       [sort-order-button section :desc sort-order]]]]))

(defn land-market-panel
  []
  [:div.container
   [header "Market" [:market]]
   (let [loading? @(rf/subscribe [:land/market-loading?])
         num-items @(rf/subscribe [:land/market-count])
         page-size @(rf/subscribe [:land-market/page-size])
         items (rf/subscribe [:land/market])]
     (if (and loading?
              (< num-items page-size))
       [:div.row
        [:div.col-xs-12.center-xs
         [:em "loading..."]]]
       [:div.row
        [:div.col-xs-12
         [market-sorter]
         [rt/reagent-table
          items
          {:table {:class "table table-big table-striped table-wrap"
                   :style {:margin "0 auto"}}
           :column-model [{:header ""
                           :key :image}
                          {:header "ID"
                           :key :token-id}
                          {:header "Name"
                           :key :name}
                          {:header "Rarity"
                           :key :rarity}
                          {:header "Effects"
                           :key :effects}
                          {:header "Price"
                           :key :current-price}]
           :render-cell (fn [{:keys [key]} row _ _]
                          (case key
                            :token-id [:a {:href (format "https://land.axieinfinity.com/item/%s/%s"
                                                         (get row :alias)
                                                         (get row :token-id))
                                           :target "_blank"}
                                       (get row :token-id)]
                            :image [:img {:style {:max-width "50px"
                                                  :max-height "50px"}
                                          :src (get row :figure-url)}]
                            (get row key)))}]]
        [:div.col-xs-12
         [axies-pager :land-market]]]))
   [footer]])

(defn valuation-sorter
  [{:keys [section extra-fields]
    :or {section :valuation
         extra-fields []}}]
  (let [sort-key @(rf/subscribe [(keyword section :sort-key)])
        sort-order @(rf/subscribe [(keyword section :sort-order)])]
    [:div.row
     [:div.col-xs-12.center-xs
      [:p
       [:span {:style {:padding-right "0.3em"}}
        "sort by:"]
       (->> extra-fields
            (concat [["Name" :name]
                     ["Count" :num-items]
                     ["Rarity" :rarity-value]
                     ["Min Price" :min-price]
                     ["Max Price" :max-price]
                     ["Avg Price" :avg-price]])
            (map (fn [[title k]]
                   ^{:key k}
                   [sort-key-button section title k sort-key])))
       [:span {:style {:padding-left "0.6em"
                       :padding-right "0.3em"}}
        "order:"]
       [sort-order-button section :asc sort-order]
       [sort-order-button section :desc sort-order]]]]))

(defn land-valuation-panel
  []
  [:div.container
   [header "Item Valuation" [:items :market]]
   (let [loading? @(rf/subscribe [:land/items-loading?])
         items (rf/subscribe [:valuation/items])]
     (if loading?
       [:div.row
        [:div.col-xs-12.center-xs
         [:em "loading..."]]]
       [:div.row
        [:div.col-xs-12
         [valuation-sorter]
         [rt/reagent-table
          items
          {:table {:class "table table-big table-striped table-wrap"
                   :style {:margin "0 auto"}}
           :column-model [{:header ""
                           :key :image}
                          {:header "Name"
                           :key :name}
                          {:header "Count"
                           :key :num-items}
                          {:header "Rarity"
                           :key :rarity}
                          {:header "Min Price"
                           :key :min-price}
                          {:header "Max Price"
                           :key :max-price}
                          {:header "Avg Price"
                           :key :avg-price}
                          {:headers "IDs"
                           :key :item-ids
                           :attrs {:style {:width "200px"}}}]
           :render-cell (fn [{:keys [key attrs]
                              :or {attrs {}}} row _ _]
                          [:span attrs
                          (case key
                            :image (let [item-alias (get row :item-alias)]
                                     [:img {:style {:max-width "100px"
                                                    :max-height "100px"}
                                            :src (format "https://cdn.axieinfinity.com/terrarium-items/%s.png" item-alias)}])
                            :item-ids (for [{:keys [item-alias item-id]} (get row :all-items)]
                                        [:span {:key item-id}
                                         [:a {:href (format "https://land.axieinfinity.com/item/%s/%s"
                                                            item-alias item-id)
                                              :target "_blank"}
                                          item-id]
                                         " "])
                            (get row key))])}]]]))
   [footer]])
