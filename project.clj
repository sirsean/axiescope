(defproject axiescope "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/clojurescript "1.10.520"]
                 [reagent "0.8.1"]
                 [re-frame "0.10.6"]
                 [cljsjs/web3 "0.19.0-0"]
                 [cljs-web3 "0.19.0-0-11"]
                 [district0x/re-frame-interval-fx "1.0.2"]
                 [camel-snake-kebab "0.4.0"]
                 [cljs-ajax "0.7.4"]
                 [funcool/cuerdas "2.2.0"]
                 [org.clojure/core.async "0.4.490"]
                 [cljs-await "1.0.2"]
                 [cljsjs/moment "2.24.0-0"]
                 [org.clojars.frozenlock/reagent-table "0.1.5"]
                 [secretary "1.2.3"]
                 [venantius/accountant "0.2.4"]]

  :plugins [[lein-cljsbuild "1.1.7"]]

  :min-lein-version "2.5.3"

  :source-paths ["src/clj" "src/cljs"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]

  :figwheel {:css-dirs ["resources/public/css"]
             :ring-handler "axiescope.core/handler"}

  :profiles
  {:dev
   {:dependencies [[binaryage/devtools "0.9.10"]]

    :plugins      [[lein-figwheel "0.5.18"]]}
   :prod { }
   }

  :cljsbuild
  {:builds
   [{:id           "dev"
     :source-paths ["src/cljs"]
     :figwheel     {:on-jsload "axiescope.core/mount-root"
                    :websocket-host :js-client-host}
     :compiler     {:main                 axiescope.core
                    :output-to            "resources/public/js/compiled/app.js"
                    :output-dir           "resources/public/js/compiled/out"
                    :asset-path           "js/compiled/out"
                    :source-map-timestamp true
                    :preloads             [devtools.preload]
                    :external-config      {:devtools/config {:features-to-install :all}}
                    }}

    {:id           "min"
     :source-paths ["src/cljs"]
     :compiler     {:main            axiescope.core
                    :output-to       "resources/public/js/compiled/app.js"
                    :optimizations   :advanced
                    :closure-defines {goog.DEBUG false}
                    :pretty-print    false}}


    ]}
  )
