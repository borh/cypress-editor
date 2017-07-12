(ns cypress-editor.app
  (:require
   ;; Do not remove the following requires needed by re-frame:
   [cypress-editor.subs]
   [cypress-editor.events]
   ;;

   [goog.dom :as dom]

   [cypress-editor.routes :as routes]
   [cypress-editor.views :refer [interface]]
   [cypress-editor.re-com-views :as rcv]
   [cypress-editor.config :refer [debug-enabled?]]

   [clojure.spec.alpha :as s]
   [reagent.core :as r]
   [re-frame.core :refer [dispatch-sync]]
   [re-frisk.core :refer [enable-re-frisk!]]
   [re-learn.core :as re-learn]
   [re-learn.views :as re-learn-views]))

;; # App entry point

(defn ^:export main
  []
  (routes/app-routes)
  (dispatch-sync [:boot])
  (when debug-enabled?
    (enable-console-print!)
    (println "Debug mode enabled...")
    (s/check-asserts true)
    (enable-re-frisk! {:x 50 :y 120}))
  (if (dom/getElement "login")
    (r/render [rcv/login-box]
              (. js/document (getElementById "login")))
    (do
      (r/render [rcv/interface]
                (. js/document (getElementById "app")))
      (r/render [re-learn-views/tutorial-view {:context? true}]
                (. js/document (getElementById "learn"))))))
