(ns edge.devcards
  (:require
   [devcards.core :as dc :include-macros true])
  (:require-macros
   [devcards.core :refer [defcard]]))

(enable-console-print!)

(defcard trying-test
  #_(carder/dashboard-item {:type :dashboard/graphic :title "Wut." :image "image.png"}))

(defn main []
  (dc/start-devcard-ui!))
