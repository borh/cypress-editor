(ns cypress-editor.main
  "Entrypoint for production Uberjars"
  (:gen-class)
  (:require [com.stuartsierra.component :as component]
            [cypress-editor.system :refer [new-system]]))

(def system nil)

(defn -main
  [& args]
  (let [system (new-system :prod)]
    (component/start system))
  ;; All threads are daemon, so block forever:
  @(promise))
