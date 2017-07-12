(ns cypress-editor.communication
  (:require [taoensso.sente :as sente]
            [taoensso.sente.packers.transit :as sente-transit]
            [re-frame.core :refer [dispatch]]
            [cypress-editor.config :refer [ws-url api-url]]))

;; https://github.com/ptaoussanis/sente/issues/118#issuecomment-87378277

(def !socket (atom nil))

(defn send! [& params]
  (apply (:send-fn @!socket) params))

(defmulti handle-message first)

(defmulti handle-event :id)

(defmethod handle-event :chsk/state [{:keys [state]}]
  (dispatch [:set/sente-connection-status (:open? @state)]))

(defmethod handle-event :chsk/handshake [_])

(defmethod handle-event :chsk/recv [{:keys [?data ?csrf-token]}]
  (handle-message ?data))

(defmethod handle-message :chsk/ws-ping [_]
  (dispatch [:set/sente-connection-status (:open? @(:state @!socket))]))

(defn create-socket! [token]
  (reset! !socket
          (sente/make-channel-socket!
           "/chsk"
           {:type :auto
            :packer (sente-transit/get-transit-packer)
            :client-id token
            :params {:uid "uuid"}
            :chsk-url-fn #(str ws-url %)}))
  (sente/start-chsk-router! (:ch-recv @!socket) handle-event))
