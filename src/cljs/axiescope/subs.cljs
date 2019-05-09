(ns axiescope.subs
  (:require
   [re-frame.core :as rf]
   [axiescope.battle :as battle]))

(rf/reg-sub
  ::active-panel
  (fn [{:keys [active-panel]}]
    active-panel))

(rf/reg-sub
  ::bs-attacker
  (fn [db]
    (get-in db [:battle-simulator :attacker])))

(rf/reg-sub
  ::bs-defender
  (fn [db]
    (get-in db [:battle-simulator :defender])))

(rf/reg-sub
  ::battle-simulation
  (fn [_]
    [(rf/subscribe [::bs-attacker])
     (rf/subscribe [::bs-defender])])
  (fn [[attacker defender]]
    (when (and attacker defender)
      (battle/simulate attacker defender))))
