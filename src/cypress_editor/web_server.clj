;; Copyright © 2016, JUXT LTD.

(ns cypress-editor.web-server
  (:require
   [bidi.bidi :refer [tag]]
   [bidi.vhosts :refer [make-handler vhosts-model]]
   [clojure.tools.logging :refer :all]
   [com.stuartsierra.component :refer [Lifecycle using]]
   [clojure.java.io :as io]
   [cypress-editor.bulma-ui :as ui]
   [schema.core :as s]
   [yada.resources.webjar-resource :refer [webjars-route-pair]]
   [yada.yada :refer [handler resource] :as yada]
   [clojure.string :as str]))

(defn content-routes []
  ["/"
   [["index.html"
     (yada/resource
      {:id :cypress-editor.resources/index
       :methods
       {:get
        {:produces {:media-type #{"text/html"}
                    :language #{"en"
                                "ja-jp;q=0.9"}}
         :response
         (fn [ctx]
           ;; TODO tempura, session content from login page
           (ui/page {:author "Bor Hodošček/Hinoki Project"
                     :description "Cypress Fulltext Search"
                     :title "Cypress Fulltext Search"
                     :app-name "Cypress Fulltext Search"
                     :lang (case (yada/language ctx)
                             "en" "en"
                             "ja-jp" "ja"
                             "en")}))}}})]

    ["login"
     (yada/resource
      {:id :cypress-editor.resources/login
       :methods
       {:get
        {:produces {:media-type #{"text/html"}
                    :language #{"en"
                                "ja-jp;q=0.9"}}
         :response
         (fn [ctx]
           (ui/page {:author "Bor Hodošček/Hinoki Project"
                     :description "Cypress Fulltext Search"
                     :title "Cypress Fulltext Search Login"
                     :app-name "Cypress Fulltext Search"
                     :lang (case (yada/language ctx)
                             "en" "en"
                             "ja-jp" "ja"
                             "en")
                     :body [:div#login]}))}}})]

    ["" (assoc (yada/redirect :cypress-editor.resources/index)
               :id :cypress-editor.resources/content)]

    ["debug.html"
     (yada/resource
      {:id :cypress-editor.resources/debug
       :methods
       {:get
        {:produces #{"text/plain"}
         :response (fn [ctx]
                     (str/join "\n"
                               (for [path [:webjar/bootstrap
                                           :webjar/bulma
                                           :cypress-editor.resources/index
                                           :cypress-editor.resources/content]]
                                 (str path " => " (yada/path-for ctx path)))))}}})]

    ["assets" (->> (webjars-route-pair)
                   second
                   (map (fn [[wj-name wj-resource]]
                          [wj-name
                           (assoc wj-resource
                                  :id (keyword "webjar" wj-name))]))
                   (into []))]

    [""
     (-> (yada/as-resource (io/file "target"))
         (assoc :id :cypress-editor.resources/static))]]])

(defn routes
  "Create the URI route structure for our application."
  [config]
  [""
   [;; Our content routes, and potentially other routes.
    (content-routes)

    ;; This is a backstop. Always produce a 404 if we get there. This
    ;; ensures we never pass nil back to Aleph.
    [true (handler nil)]]])

(s/defrecord WebServer [host :- s/Str
                        port :- s/Int
                        scheme :- (s/enum :http :https)
                        listener]
  Lifecycle
  (start [component]
    (if listener
      component                         ; idempotence
      (let [vhosts-model
            (vhosts-model
             [{:scheme scheme :host host}
              (routes {:port port})])
            listener (yada/listener vhosts-model {:port port})]
        (infof "Started web-server on %s://%s:%s" (name scheme) host (:port listener))
        (assoc component :listener listener))))

  (stop [component]
    (when-let [close (get-in component [:listener :close])]
      (close))
    (assoc component :listener nil)))

(defn new-web-server []
  (using
   (map->WebServer {})
   []))
