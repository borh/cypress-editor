(ns cypress-editor.app
  (:require
   ;; Do not remove the following requires:
   [cypress-editor.subs]
   [cypress-editor.events]
   ;;

   [cypress-editor.views :refer [interface]]
   [cypress-editor.re-com-views :as rcv]

   [reagent.core :as r]
   [re-frame.core :refer [dispatch-sync]]
   [re-frisk.core :refer [enable-re-frisk!]]))

;; # App entry point

(enable-console-print!)

(def ^boolean debug-enabled? "@define {boolean}" ^boolean js/goog.DEBUG)

(defn ^:export main
  []
  (dispatch-sync [:boot])
  (when debug-enabled?
    (enable-console-print!)
    (println "Debug mode enabled...")
    (enable-re-frisk! {:x 50 :y 120}))
  (r/render [rcv/interface]
            (. js/document (getElementById "app"))))
