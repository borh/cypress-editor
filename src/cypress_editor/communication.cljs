(ns cypress-editor.communication
  (:require [taoensso.sente :as sente]
            [mount.core :refer [defstate]]
            [cypress-editor.db :refer [ws-url]]))

(defstate socket
  :start (sente/make-channel-socket!
          "/chsk"
          {:type :auto
           :chsk-url-fn #(str ws-url %)}))

(defn send! [& params]
  (apply (:send-fn @socket) params))

(defmulti handle-message first)

(defmulti handle-event :id)

(defmethod handle-event :chsk/state [_])

(defmethod handle-event :chsk/handshake [_])

(defmethod handle-event :chsk/recv [{:keys [?data]}]
  (handle-message ?data))

(defstate router
  :start (sente/start-chsk-router! (:ch-recv @socket) handle-event))
