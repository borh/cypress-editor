(ns cypress-editor.app
  (:require
   ;; Do not remove the following requires:
   [cypress-editor.subs]
   [cypress-editor.events]
   ;;

   [cypress-editor.routes :as routes]
   [cypress-editor.views :refer [interface]]
   [cypress-editor.re-com-views :as rcv]

   [clojure.spec :as s]
   [reagent.core :as r]
   [re-frame.core :refer [dispatch-sync]]
   [re-frisk.core :refer [enable-re-frisk!]]))

;; # App entry point

(def ^boolean debug-enabled? "@define {boolean}" ^boolean js/goog.DEBUG)

(defn ^:export main
  []
  #_(routes/app-routes) ;; TODO
  (dispatch-sync [:boot])
  (when debug-enabled?
    (enable-console-print!)
    (println "Debug mode enabled...")
    (s/check-asserts true)
    (enable-re-frisk! {:x 50 :y 120}))
  (r/render [rcv/interface]
            (. js/document (getElementById "app"))))
