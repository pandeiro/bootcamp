(ns frontend.views.update
  (:require [clojure.string :as s]
            [reagent.core :as r]
            [frontend.views.util :refer [record-in button]]))

(defn post-update [app-state]
  (let [form-state (r/atom {})
        disable-button? (fn []
                          (let [{:keys [post]} @form-state]
                            (or (not post) (empty? (s/trim post)))))]
    (fn [_]
      [:div.post-update
       [:textarea {:placeholder "Post update here..."
                   :on-change #(record-in form-state [:post] %)}]
       (if-not (:user @app-state)
         [button app-state :signup-click
          "Signup to post" nil disable-button?]
         [button app-state :post-send-click
          "Post" (fn [] @form-state) disable-button?])])))
