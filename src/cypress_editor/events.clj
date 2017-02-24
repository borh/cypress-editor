(ns cypress-editor.events
  (:require [clojure.string :as str]))

(defmacro sente-bridge
  [pq]
  (let [db1-sym (gensym "db1")
        db-sym (symbol "db") #_(gensym "db2")
        data-sym (gensym "data")
        _-sym (gensym "_")
        path (first pq)
        query (into [] (rest pq))
        get-event (keyword "get"
                           (-> path
                               str
                               (str/replace ":" "")
                               (str/replace "/" "-")))
        set-event (keyword "set"
                           (-> path
                               str
                               (str/replace ":" "")
                               (str/replace "/" "-")))]
    `(do
       ;; (println "Registering: " ~get-event)
       (re-frame.core/reg-event-fx
        ~get-event
        middleware
        (fn [{:keys [~db-sym]} [~_-sym]]
          {:db (assoc ~db-sym ~path :loading)
           :sente {:query ~pq
                   :timeout 60000
                   :update-fx ~set-event}}))
       ;; (println "Registering: " ~set-event)
       (re-frame.core/reg-event-db
        ~set-event
        middleware
        (fn [~db-sym [~data-sym]] (assoc ~db-sym ~path ~data-sym))))))
