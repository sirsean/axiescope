(ns axiescope.views.card
  (:require
    [cuerdas.core :refer [format]]
    ))

(defn show
  [{:keys [id part-name skill-name
           default-energy default-attack default-defense
           description]}]
  [:div {:style {:position "relative"
                 :margin "auto"
                 :width "300px"
                 :height "400px"
                 :font-size "32px"
                 :font-weight "bold"
                 :color "white"
                 :-webkit-text-stroke "2px black"
                 :background-image (format "url(\"https://storage.googleapis.com/axie-cdn/game/cards/base/%s.png\")" id)
                 :background-repeat "no-repeat"}}
   [:span {:style {:position "absolute"
                   :top "25px"
                   :left "32px"}}
    default-energy]
   [:span {:style {:position "absolute"
                   :font-size "24px"
                   :-webkit-text-stroke "1px white"
                   :top "32px"
                   :left "85px"}}
    skill-name]
   [:span {:style {:position "absolute"
                   :color "#f21835"
                   :top "116px"
                   :left "16px"}}
    default-attack]
   [:span {:style {:position "absolute"
                   :color "#29b304"
                   :top "164px"
                   :left "16px"}}
    default-defense]
   [:span {:style {:position "absolute"
                   :font-size "20px"
                   :font-weight "normal"
                   :-webkit-text-stroke "1px white"
                   :top "300px"
                   :left "44px"
                   :right "36px"}}
    description]
   ])
