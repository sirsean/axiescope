(ns axiescope.db)

(def default-db
  {:active-panel :home-panel
   :web3 (aget js/window "web3")
   :eth (aget js/window "ethereum")})
