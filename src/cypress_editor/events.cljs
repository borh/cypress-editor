(ns cypress-editor.events
  (:require
   [re-frame.core :refer [reg-event-db reg-event-fx reg-fx dispatch]]
   [re-frame.std-interceptors :refer [debug trim-v]]
   [ajax.core :as ajax]
   [taoensso.sente :as sente]
   [day8.re-frame.http-fx]
   [day8.re-frame.async-flow-fx]
   [cypress-editor.communication :as comm]
   [cypress-editor.db :refer [api-url]]
   [cypress-editor.utils :refer [kwic-regex-formatter]]
   [clojure.string :as str]
   [clojure.spec :as s])
  (:require-macros [cypress-editor.events :refer [sente-bridge]]))

(def middleware
  [trim-v])

(reg-fx
 :sente
 (fn [{:keys [update-fx query timeout] :or {timeout 30000}}]
   (comm/send! query timeout
               (fn [data]
                 (dispatch [update-fx data])))))

(defn boot-flow
  []
  {:first-dispatch [:sente/authenticate]
   :rules [{:when :seen?
            :events [:sente/auth-success]
            :dispatch-n [[:sente/connect]
                         #_[:get/sources-genre]
                         #_[:get/sentences-collocations]
                         #_[:get/sentences-tokens]
                         #_[:get/tokens-tree]
                         #_[:get/tokens-similarity]
                         #_[:get/tokens-nearest-tokens]
                         #_[:get/tokens-similarity-with-accuracy]
                         #_[:get/tokens-tsne]
                         #_[:get/collocations-collocations]
                         #_[:get/collocations-tree]
                         #_[:get/suggestions-tokens]
                         #_[:get/errors-register]
                         #_[:get/topics-infer]]
            :halt? false}]})

(reg-event-fx
 :sente/connect
 middleware
 (fn [{:keys [db]} [_]]
   {:db (assoc db :sente/connection-status (:open? @(:state @comm/!socket)))
    :dispatch [:sente/started]}))

(def ^boolean debug-enabled? "@define {boolean}" ^boolean js/goog.DEBUG)
(reg-event-fx
 :sente/started
 middleware
 (fn [_ _]
   (when debug-enabled?
     (println "Connected!"))
   {}))

(reg-event-fx
 :sente/authenticate
 (fn [{:keys [db]} _]
   {:http-xhrio {:method          :get
                 :uri             (str api-url "/authenticate")
                 :params {:username (:user/username db)
                          :password (:user/password db)}
                 :timeout         1000
                 :format          (ajax/url-request-format)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success      [:sente/auth-success]
                 :on-failure      [:sente/auth-failure]
                 :with-credentials? false}}))

(reg-event-db
 :sente/auth-success
 (fn [db [_ result]]
   (let [token (:token result)]
     (comm/create-socket! token)
     (assoc db
            :user/auth-token token
            :user/account-valid true))))

(reg-event-fx
 :sente/auth-failure
 (fn [{:keys [db]} [_ result]]
   (when debug-enabled?
     (println "auth-failure" result))
   ;; Rolling timeout with reset?
   {:db (assoc db
               :user/auth-token nil
               :user/account-valid false)}))

;; API

(s/def ::token (s/keys :un-req [:lemma :orth-base :pos]))

(s/def :sources/genre (s/nilable string?))
(s/def :sentences/collocations (s/map-of keyword? string?))
(s/def :sentences/tokens (s/map-of keyword? string?))

(def sources-api
  {:sources/genre nil})

(def tokens-api
  {:sentences/collocations nil
   :sentences/tokens nil
   :tokens/tree nil
   :tokens/similarity nil
   :tokens/nearest-tokens nil
   :tokens/similarity-with-accuracy nil
   :tokens/tsne nil
   :collocations/collocations nil
   :collocations/tree nil
   :suggestions/tokens nil})

(def text-api
  {:errors/register nil
   :topics/infer nil})

(def fulltext-api
  {:fulltext/query (if debug-enabled? "しかしながら" "")
   :fulltext/genre-column true
   :fulltext/title-column true
   :fulltext/author-column false
   :fulltext/year-column false
   :fulltext/document-selected nil
   :fulltext/document-text nil
   :fulltext/document-show false})

(def input-api
  {:user/auth-token nil
   :user/csrf-token nil
   :user/username nil #_(if debug-enabled? "bor" nil)
   :user/password nil #_(if debug-enabled? "test" nil)
   :user/account-valid nil
   :user/id nil

   :user/text (if debug-enabled? "入力テキストを解析する。" nil)
   :user/unit-type :suw
   :user/features [:orth]
   :user/token (if debug-enabled? "花" nil)
   :user/extra-token (if debug-enabled? "菊" nil)
   :user/genre (if debug-enabled? "白書.*" "*")
   :user/limit 5
   :user/html true
   :user/norm :tokens
   :user/collocation {:string-1 "花" :string-1-pos "noun"
                      :string-2 "を" :string-2-pos "particle"
                      :type :noun-particle-verb}
   :user/collocation-tree {:string-1 "花" :string-1-pos "noun"
                           :string-2 "を" :string-2-pos "particle"
                           :string-3 "見る" :string-3-pos "verb"
                           :type :noun-particle-verb}
   :user/selected-topic nil})

(reg-event-fx
 :boot
 (fn [_ _]
   (let [db (merge
             {:sente/connection-status false
              #_{0 {"a" 0.90 "b" 0.05 "c" 0.04 "d" 0.01}
                 1 {"p" 1.0}
                 2 {"k" 9922 "l" 22}}}
             sources-api
             tokens-api
             text-api
             fulltext-api
             input-api)]
     {:db db
      :async-flow (boot-flow)})))

;; TODO parameterize state

(reg-event-db
 :set/sente-connection-status
 middleware
 (fn [db [new-state]] (assoc db :sente/connection-status new-state)))

(reg-event-db
 :set/user-account-valid
 middleware
 (fn [db [new-state]] (assoc db :user/account-valid new-state)))

(reg-event-db
 :set/user-auth-token
 middleware
 (fn [db [new-state]] (assoc db :user/auth-token new-state)))

(reg-event-db
 :set/user-username
 middleware
 (fn [db [new-state]] (assoc db :user/username new-state)))

(reg-event-db
 :set/user-password
 middleware
 (fn [db [new-state]] (assoc db :user/password new-state)))

(reg-event-db
 :set/user-text
 middleware
 (fn [db [new-state]] (assoc db :user/text new-state)))

(reg-event-db
 :set/user-unit-type
 middleware
 (fn [db [new-state]] (assoc db :user/unit-type new-state)))

(reg-event-db
 :set/user-features
 middleware
 (fn [db [new-state]] (assoc db :user/features new-state)))

(reg-event-db
 :set/user-token
 middleware
 (fn [db [new-state]] (assoc db :user/token new-state)))

(reg-event-db
 :set/user-extra-token
 middleware
 (fn [db [new-state]] (assoc db :user/extra-token new-state)))

(reg-event-db
 :set/user-genre
 middleware
 (fn [db [new-state]] (assoc db :user/genre new-state)))

(reg-event-db
 :set/user-limit
 middleware
 (fn [db [new-state]] (assoc db :user/limit new-state)))

(reg-event-db
 :set/user-html
 middleware
 (fn [db [new-state]] (assoc db :user/html new-state)))

(reg-event-db
 :set/user-norm
 middleware
 (fn [db [new-state]] (assoc db :user/norm new-state)))

(reg-event-db
 :set/user-collocation
 middleware
 (fn [db [new-state]] (assoc db :user/collocation new-state)))

(reg-event-db
 :set/user-collocation-tree
 middleware
 (fn [db [new-state]] (assoc db :user/collocation-tree new-state)))

(reg-event-db
 :set/user-selected-topic
 middleware
 (fn [db [new-state]] (assoc db :user/selected-topic new-state)))

(reg-event-db
 :set/fulltext-query
 middleware
 (fn [db [new-state]] (assoc db :fulltext/query new-state)))

(reg-event-db
 :set/fulltext-state
 middleware
 (fn [db [new-state]] (assoc db :fulltext/state new-state)))

;; Toggle columns
(reg-event-db
 :toggle/fulltext-genre-column
 middleware
 (fn [db [_]] (update db :fulltext/genre-column not)))

(reg-event-db
 :toggle/fulltext-title-column
 middleware
 (fn [db [_]] (update db :fulltext/title-column not)))

(reg-event-db
 :toggle/fulltext-author-column
 middleware
 (fn [db [_]] (update db :fulltext/author-column not)))

(reg-event-db
 :toggle/fulltext-year-column
 middleware
 (fn [db [_]] (update db :fulltext/year-column not)))

;;

(comment
  (reg-event-db
   :set/user-misc
   middleware
   (fn [db [new-state]] (assoc db :user/misc new-state))))


(comment
  (reg-event-db
   :update-text-topics
   middleware
   (fn [db [results]]
     (let [topics
           (->> results
                :results
                (reduce
                 (fn [a {:keys [id prob tokens]}]
                   (assoc a id {:prob prob :token-probs (zipmap tokens (repeat 1.0))}))
                 {}))
           first-topic (-> results :results first :id)]
       (assoc db :text-topics topics :selected-topic first-topic)))))

;;

(sente-bridge [:sources/genre :sources])

(sente-bridge [:sentences/collocations
               (merge {:limit (:user/limit input-api)
                       :html  (:user/html input-api)}
                      (:user/collocation-tree input-api))])

(sente-bridge [:sentences/tokens {:lemma (:user/token input-api) :limit (:user/limit input-api) :html (:user/html input-api)}])

(sente-bridge [:tokens/tree {:lemma (:user/token input-api) :norm (:user/norm input-api)}])

(sente-bridge [:tokens/similarity [:suw [:orth] (:user/token input-api) (:user/extra-token input-api)]])

(sente-bridge [:tokens/nearest-tokens [:suw [:orth] (:user/token input-api) 5]])

(sente-bridge [:tokens/similarity-with-accuracy [:suw [:orth] (:user/token input-api) 0.8]])

(sente-bridge [:collocations/collocations (merge {:limit (:user/limit input-api)}
                                                 (:user/collocation input-api))])

(sente-bridge [:collocations/tree (:user/collocation-tree input-api)])

(sente-bridge [:tokens/tsne [:suw [:orth] (:user/token input-api) 5 2]])

(sente-bridge [:suggestions/tokens {:lemma (:user/token input-api)}])

(sente-bridge [:errors/register (:user/text input-api)])

(sente-bridge [:topics/infer {:unit-type :suw :features [:orth] :text (:user/text input-api)}])

;;

(reg-event-fx
 :get/sources-by-sentence-id
 middleware
 (fn [_ [query]]
   {:sente {:query [:sources/sentence-id query]
            :timeout 5000
            :update-fx :set/fulltext-document-text}
    :dispatch [:toggle/fulltext-document-show]}))

(reg-event-db
 :toggle/fulltext-document-show
 middleware
 (fn [db [_]]
   (update db :fulltext/document-show not)))

(reg-event-db
 :set/fulltext-document-text
 middleware
 (fn [db [text]]
   (assoc db
          :fulltext/document-text
          (->> text
               str/split-lines
               (map (fn [s] ^{:key (gensym "p-")} [:p s]))))))

(reg-event-fx
 :get/sentences-fulltext
 middleware
 (fn [{:keys [db]} [_]]
   {:db (assoc db
               :sentences/fulltext nil
               :fulltext/state :loading)
    :sente {:query [:sentences/fulltext
                    {:query (:fulltext/query db)
                     :genre (:user/genre db)}]
            :timeout 600000
            :update-fx :set/sentences-fulltext}}))

(reg-event-db
 :set/sentences-fulltext
 middleware
 (fn [db [data]]
   (let [transform-fn
         (fn [{:keys [id title author year genre before_text key_text after_text]}]
           (for [match (kwic-regex-formatter
                        (re-pattern (:fulltext/query db))
                        key_text)]
             {:id id
              :title title
              :author author
              :year year
              :genre genre
              :before (str before_text (:before match))
              :key (:key match)
              :after (str (:after match) after_text)}))]
     (assoc db
            :sentences/fulltext (mapcat transform-fn data)
            :fulltext/state :loaded))))
