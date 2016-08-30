(ns cypress-editor.devcards
  (:require
   [cypress-editor.events]
   [cypress-editor.subs]

   [cypress-editor.views :as views]

   [re-frame.core :refer [subscribe dispatch-sync]]
   [devcards.core :as dc :include-macros true]
   [cljs.test :as t :include-macros true :refer-macros [testing is]])
  (:require-macros
   [devcards.core :refer [defcard-rg deftest]]))

(def input-box-db
  (subscribe [:input-text]))

(defcard-rg input-box-test
  "Input box"
  views/input-box
  {:input-text @input-box-db}
  {:inspect-data true})

(defcard-rg textarea-box-test
  "Textarea box"
  views/textarea-box
  []
  {:inspect-data true})

(deftest more-test
  "Tests"
  (testing "More test"
    (is (= 1 1)))
  (testing "One more"
    (is (= 1 2))))

(defn main []
  (enable-console-print!)
  (dispatch-sync [:boot])
  (dc/start-devcard-ui!))
