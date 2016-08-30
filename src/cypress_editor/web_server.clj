;; Copyright © 2016, JUXT LTD.

(ns cypress-editor.web-server
  (:require
   [bidi.bidi :refer [tag]]
   [bidi.vhosts :refer [make-handler vhosts-model]]
   [clojure.tools.logging :refer :all]
   [com.stuartsierra.component :refer [Lifecycle using]]
   [clojure.java.io :as io]
   [cypress-editor.sources :refer [source-routes]]
   ;;[cypress-editor.phonebook :refer [phonebook-routes]]
   ;;[cypress-editor.phonebook-app :refer [phonebook-app-routes]]
   [cypress-editor.hello :refer [hello-routes other-hello-routes]]
   [schema.core :as s]
   [selmer.parser :as selmer]
   [yada.resources.webjar-resource :refer [new-webjar-resource]]
   [yada.yada :refer [handler resource] :as yada]))

(defn content-routes []
  ["/"
   [
    ["index.html"
     (yada/resource
      {:id :cypress-editor.resources/index
       :methods
       {:get
        {:produces #{"text/html"}
         :response (fn [ctx]
                     (selmer/render-file "index.html" {:title "Edge Index"
                                                       :ctx ctx}))}}})]

    ["devcards.html"
     (yada/resource
      {:id :cypress-editor.resources/index
       :methods
       {:get
        {:produces #{"text/html"}
         :response (fn [ctx]
                     (selmer/render-file "devcards.html" {:title "Devcards"
                                                          :ctx ctx}))}}})]

    ["" (assoc (yada/redirect :cypress-editor.resources/index) :id :cypress-editor.resources/content)]

    ;; Add some pairs (as vectors) here. First item is the path, second is the handler.
    ;; Here's an example

    [""
     (-> (yada/as-resource (io/file "target"))
         (assoc :id :cypress-editor.resources/static))]]])

(defn routes
  "Create the URI route structure for our application."
  [config]
  [""
   [
    ;; Hello World!
    (hello-routes)
    (other-hello-routes)

    ;;(phonebook-routes db config)
    ;;(phonebook-app-routes db config)

    ["/api" (-> (hello-routes)
                ;; Wrap this route structure in a Swagger
                ;; wrapper. This introspects the data model and
                ;; provides a swagger.json file, used by Swagger UI
                ;; and other tools.
                (yada/swaggered
                 {:info {:title "Hello World!"
                         :version "1.0"
                         :description "An API on the classic example"}
                  :basePath "/api"})
                ;; Tag it so we can create an href to this API
                (tag :cypress-editor.resources/api))]

    ;; Swagger UI
    ["/swagger" (-> (new-webjar-resource "/swagger-ui" {:index-files ["index.html"]})
                    ;; Tag it so we can create an href to the Swagger UI
                    (tag :cypress-editor.resources/swagger))]

    ;; The Edge source code is served for convenience
    (source-routes)

    ;; Our content routes, and potentially other routes.
    (content-routes)

    ;; This is a backstop. Always produce a 404 if we ge there. This
    ;; ensures we never pass nil back to Aleph.
    [true (handler nil)]]])



(s/defrecord WebServer [port :- s/Int
                        listener]
  Lifecycle
  (start [component]
    (if listener
      component                         ; idempotence
      (let [vhosts-model
            (vhosts-model
             [{:scheme :http :host (format "localhost:%d" port)}
              (routes {:port port})])
            listener (yada/listener vhosts-model {:port port})]
        (infof "Started web-server on port %s" (:port listener))
        (assoc component :listener listener))))

  (stop [component]
    (when-let [close (get-in component [:listener :close])]
      (close))
    (assoc component :listener nil)))

(defn new-web-server []
  (using
   (map->WebServer {})
   []))
