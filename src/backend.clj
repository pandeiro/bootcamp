(ns backend
  (:require [backend.api :as api]
            [backend.stores :refer [redis]]
            [ring.middleware.cors :as cors]
            [ring.middleware.defaults :as defaults]
            [ring.middleware.edn :as edn]
            [ring.middleware.reload :as reload]
            [taoensso.carmine.ring :refer [carmine-store]]))

(def system
  (-> api/routes
    edn/wrap-edn-params
    reload/wrap-reload
    (cors/wrap-cors
     :access-control-allow-origin  #".+"
     :access-control-allow-headers [:content-type]
     :access-control-allow-methods [:get :put :post :delete])
    (defaults/wrap-defaults
      (assoc defaults/site-defaults
             :security {:anti-forgery false}
             :session  {:store (carmine-store redis)}))))

