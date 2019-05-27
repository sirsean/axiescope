(ns axiescope.views.panels.battle-simulator
  (:require
    [re-frame.core :as rf]
    [reagent.core :as r]
    [reagent-table.core :as rt]
    [axiescope.views.layout :refer [header footer]]
    [axiescope.views.shared :refer [show-axie]]
    ))

(defn panel []
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
