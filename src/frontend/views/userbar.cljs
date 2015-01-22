(ns frontend.views.userbar
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :as async :refer [chan <!]]
            [reagent.core :as r]
            [frontend.views.util :refer [input button]]))

(defn username-and-password [state class-name & content]
  [:div {:class-name (str "pure-form " (name class-name))}
   [input {:state state, :path [:email]
           :type :email, :class-name :username, :placeholder :email}]
   [input {:state state, :path [:password]
           :type :password :class-name :password, :placeholder :password}]
   content])

(defn signup [app-state]
  (let [form-state (atom {})]
    (fn [_]
      [username-and-password form-state :signin
       [button app-state :signup-send-click
        "Sign up"
        (fn [] @form-state)
        ;; TODO: disable when empty
        ]
       [button app-state :signin-cancel-click "Cancel" nil]])))

(defn signin [app-state]
  (let [form-state (atom {})]
    (fn [_]
      [username-and-password form-state :signin
       [button app-state :signin-send-click
        "Sign in"
        (fn [] @form-state)
        ;; TODO: disable when empty
        ]
       [button app-state :signin-cancel-click "Cancel" nil]])))

(defn signup-or-signin [app-state]
  [:span
   [button app-state :signup-click "Sign up"]
   [button app-state :signin-click "Sign in"]])

(defn user-info [app-state]
  [:div.user-info
   [button app-state :user-menu-click (get-in @app-state [:user :username])]])

(defn user-status-bar [app-state]
  (let [view-state (r/atom :main)
        mounted? (atom false)] 
    (r/create-class
     {:render
      (fn [_]
        [:div.user-status-bar
         (if (:user @app-state)
           [user-info app-state]
           (case @view-state
             :main   [signup-or-signin app-state]
             :signup [signup app-state]
             :signin [signin app-state]))])
      :component-will-mount
      (fn [_]
        (reset! mounted? true)
        (let [events (chan)]
          (async/tap (:read-events @app-state) events)
          (go-loop []
            (let [[event data] (<! events)]
              (condp = event
                :signin-click (reset! view-state :signin)
                :signup-click (reset! view-state :signup)
                :signin-cancel-click (reset! view-state :main)
                :response (when (or (= (:url data) :auth)
                                    (= (:url data) :user))
                            (reset! view-state :main))
                nil))
            (when @mounted? (recur)))))
      :component-will-unmount
      (fn [_]
        (reset! mounted? false))})))
