(ns axiescope.db
  (:require
   [cljsjs.moment]
   ))

(def default-db
  {:active-panel :home-panel
   :now (js/moment)
   :web3 (aget js/window "web3")
   :eth (aget js/window "ethereum")
   :my-axies {:page-size 100}})
