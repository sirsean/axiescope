(ns axiescope.team-builder
  (:require
    [cuerdas.core :refer [format]]
    ))

(def layout-role-names
  {:1tank-1dps-1support ["Tank" "DPS" "Support"]
   :2tank-1dps ["Tank 1" "Tank 2" "DPS"]
   :1tank-2dps ["Tank" "DPS 1" "DPS 2"]})

(defn team-role
  [layout index]
  (-> layout-role-names
      (get layout [])
      (nth index (format "Axie %s" (inc index)))))

(def layout-positions
  {:1tank-1dps-1support [2 5 7]
   :2tank-1dps [2 3 5]
   :1tank-2dps [2 4 5]
   :horizontal [1 5 9]
   :vertical [4 5 6]})

(defn team-position
  [layout index]
  (-> layout-positions
      (get layout)
      (nth index)))
