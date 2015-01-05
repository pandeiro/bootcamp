(ns backend
  (:require [backend.api :as api]
            [ring.middleware.cors :as cors]
            [ring.middleware.defaults :as defaults]
            [ring.middleware.edn :as edn]))

(def system
  (-> api/routes
    edn/wrap-edn-params
    (cors/wrap-cors
     :access-control-allow-origin  #".+"
     :access-control-allow-headers [:content-type]
     :access-control-allow-methods [:get :put :post :delete])
    (defaults/wrap-defaults
      (assoc defaults/site-defaults
             :security {:anti-forgery false}))))
