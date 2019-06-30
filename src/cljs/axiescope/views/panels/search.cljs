(ns axiescope.views.panels.search
  (:require
    [re-frame.core :as rf]
    [axiescope.views.layout :refer [header footer]]
    [axiescope.views.shared :refer [axies-pager axie-table-headers my-axies-table]]
    ))

(defn search-filter
  [filter-key [min-value max-value]]
  (let [value @(rf/subscribe [(keyword :search filter-key)])]
    [:div.row
     [:div.col-xs-2 [:strong filter-key]]
     [:div.col-xs-2 value]
     [:div.col-xs-2
      [:button {:disabled (>= min-value value)
                :on-click #(rf/dispatch [:search/set-filter filter-key (dec value)])}
       "-"]
      [:button {:disabled (<= max-value value)
                :on-click #(rf/dispatch [:search/set-filter filter-key (inc value)])}
       "+"]]]))

(defn search-filters
  []
  [:div
   [:h3 "Filters"]
   [search-filter :min-purity [0 6]]
   [search-filter :max-breed-count [0 7]]])

(defn panel
  []
  (let [loading? @(rf/subscribe [:search/loading?])
        num-axies @(rf/subscribe [:search/count])
        page-size @(rf/subscribe [:search/page-size])]
    [:div.container
     [header {:title "Search"
              :bars [:search]}]
     (if (and loading?
             (< num-axies page-size))
       [:div.row
        [:div.col-xs-12.center-xs
         [:em "loading..."]]]
       [:div.row
        [:div.col-xs-12
         [search-filters]]
        [:div.col-xs-12
         [my-axies-table {:section :search
                          :sub :search/axies
                          :extra-sort-fields [["Price" :price]]
                          :headers (conj axie-table-headers
                                         [:price "Price"])}]]
        [:div.col-xs-12
         [axies-pager :search]]])
     [footer]]))
