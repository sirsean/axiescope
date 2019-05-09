(ns axiescope.core
  (:require [clojure.string :refer [starts-with?]]
            [ring.middleware.resource :refer [wrap-resource]]))

(defn- wrap-default-index [next-handler]
  (fn [request]
    (next-handler
      (if (or (starts-with? (:uri request) "/css/")  ;; default directory layout after doing
              (starts-with? (:uri request) "/js/"))  ;; `lein new figwheel my-app`
        request
        (assoc request :uri "/index.html")))))  ;; wrap-resource will find index.html for us

(def handler
  (-> (fn [_] {:status 404 :body "static asset not found"})
      (wrap-resource "public")
      wrap-default-index))
