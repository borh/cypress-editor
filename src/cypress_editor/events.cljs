(ns cypress-editor.events
  (:require
   [re-frame.core :refer [reg-event-db reg-event-fx]]
   [re-frame.std-interceptors :refer [debug trim-v]]
   [ajax.core :as ajax]
   [day8.re-frame.http-fx]
   [day8.re-frame.async-flow-fx]))

(defn boot-flow
  []
  {:first-dispatch [:connect-server]
   :rules [{:when :seen?
            :events [:server-connection-success]
            ;;:dispatch [:enable-ui]
            :halt? true}
           {:when :seen-any-of?
            :events [:server-connection-failure]
            :dispatch-n [[:try-server-reconnect 5000] [:set-disabled-state]]
            :halt? true}]})

(reg-event-fx
 :boot
 (fn [_ _]
   {:db (-> {:connection-status :offline
             ;;:user-text "この文章はサンプルです。"
             :error-data nil
             :text-topics nil #_{0 {"a" 0.90 "b" 0.05 "c" 0.04 "d" 0.01}
                           1 {"p" 1.0}
                           2 {"k" 9922 "l" 22}}
             :selected-topic nil
             :input-text ""
             :input-text-state nil
             :input-text-results nil
             :input-text-results-state nil
             :state :loading})
    :async-flow (boot-flow)}))

;; TODO parameterize state
(reg-event-db
 :server-connection-success
 [trim-v debug]
 (fn [db _]
   (assoc db :connection-status :online)))

(reg-event-db
 :server-connection-failure
 [trim-v debug]
 (fn [db _]
   (assoc db :connection-status :offline)))

(reg-event-db
 :server-connection-loading
 [trim-v debug]
 (fn [db _]
   (assoc db :connection-status :loading)))

(reg-event-db
 :update-input-text-state
 [trim-v debug]
 (fn [db [new-state]]
   (assoc db :input-text-state new-state)))

(reg-event-db
 :set-input-text
 [trim-v debug]
 (fn [db [new-input-text]]
   (assoc db :input-text new-input-text)))

(reg-event-db
 :set-input-text-results
 [trim-v debug]
 (fn [db [results]]
   (assoc db :input-text-results results)))

(reg-event-db
 :set-input-text-results-state
 [trim-v debug]
 (fn [db [state]]
   (assoc db :input-text-results-state state)))

(reg-event-db
 :update-selected-topic
 [trim-v debug]
 (fn [db [topic-id]]
   (assoc db :update-selected-topic topic-id)))

(reg-event-db
 :update-text-topics
 [trim-v debug]
 (fn [db [results]]
   (let [topics
         (->> results
              :results
              (reduce
               (fn [a {:keys [id prob tokens]}]
                 (assoc a id {:prob prob :token-probs (zipmap tokens (repeat 1.0))}))
               {}))
         first-topic (-> results :results first :id)]
     (assoc db :text-topics topics :selected-topic first-topic))))

(reg-event-db
 :show-infer-failure
 [trim-v debug]
 (fn [db [topics]]
   (assoc db :update-text-topics {:failure topics})))

(reg-event-fx
 :connect-server
 (fn [{:keys [db]} [_ a]]
   {:http-xhrio
    {:method :get
     :uri    "https://nlp.lang.osaka-u.ac.jp/natsume-server/api/sources/genre"
     :response-format (ajax/json-response-format {:keywords? true})
     :on-success [:server-connection-success]
     :on-failure [:server-connection-failure]}
    :db db}))

(reg-event-fx
 :search-input-text
 (fn [{:keys [db]} [_ query]]
   {:http-xhrio
    {:method :get
     :uri    "https://nlp.lang.osaka-u.ac.jp/natsume-server/api/tokens"
     :params {:lemma query}
     :response-format (ajax/json-response-format {:keywords? true})
     :on-success [:set-input-text-results]
     :on-failure [:set-input-text-results-state :failure]}
    :db (assoc db :input-text-results-state :loading)}))

(reg-event-fx
 :analyze-errors
 (fn [{:keys [db]} [_]]
   {:http-xhrio
    {:method :post
     :uri    "https://nlp.lang.osaka-u.ac.jp/natsume-server/api/errors/register"
     :format :text/plain
     :headers {"Content-Type" "text/plain"}
     :body   (:user-text db) ;; FIXME
     :response-format (ajax/json-response-format {:keywords? true})
     :on-success [:update-analysis-results]
     :on-failure [:show-analysis-failure]}
    :db (assoc db :input-text-results-state :loading)}))

(reg-event-fx
 :infer-text-topics
 (fn [{:keys [db]} [_]]
   {:http-xhrio
    {:method :post
     :uri    "https://nlp.lang.osaka-u.ac.jp/natsume-server/api/topics/infer"
     :format :text/plain
     :headers {"Content-Type" "text/plain"}
     :body (:user-text db) ;; FIXME
     :response-format (ajax/json-response-format {:keywords? true})
     :on-success [:update-text-topics]
     :on-failure [:show-infer-failure]}
    :db db}))

(reg-event-db
 :update-user-text
 [trim-v debug]
 (fn [db [text]]
   (assoc db :user-text text)))

(reg-event-db
 :update-analysis-results
 [trim-v debug]
 (fn [db [results]]
   (assoc db :error-data results)))

(reg-event-db
 :show-analysis-failure
 [trim-v debug]
 (fn [db [results]]
   (assoc db :error-data [:failed results])))
