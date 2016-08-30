(ns cypress-editor.app
  (:require
   ;; Do not remove the following requires:
   [cypress-editor.events]
   [cypress-editor.subs]
   ;;

   [cypress-editor.views :refer [interface]]

   [reagent.core :as r]
   [re-frame.core :refer [dispatch-sync]]))

;; # App entry point

(def ^boolean debug-enabled? "@define {boolean}" ^boolean js/goog.DEBUG)

(defn ^:export main
  []
  (enable-console-print!)
  (dispatch-sync [:boot])
  (r/render [interface]
            (. js/document (getElementById "app"))))
