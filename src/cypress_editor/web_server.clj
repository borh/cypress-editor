;; Copyright © 2016, JUXT LTD.

(ns cypress-editor.web-server
  (:require
   [bidi.bidi :refer [tag]]
   [bidi.vhosts :refer [make-handler vhosts-model]]
   [clojure.tools.logging :refer :all]
   [com.stuartsierra.component :refer [Lifecycle using]]
   [clojure.java.io :as io]
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
                     (selmer/render-file "index.html" {:title "Cypress"
                                                       :ctx ctx}))}}})]

    ["devcards.html"
     (yada/resource
      {:id :cypress-editor.resources/devcards
       :methods
       {:get
        {:produces #{"text/html"}
         :response (fn [ctx]
                     (selmer/render-file "devcards.html" {:title "Devcards"
                                                          :ctx ctx}))}}})]

    ["" (assoc (yada/redirect :cypress-editor.resources/index) :id :cypress-editor.resources/content)]

    ;; Add some pairs (as vectors) here. First item is the path, second is the handler.
    ;; Here's an example

    ["assets"
     [["bootstrap"
       (new-webjar-resource "bootstrap")]
      ["material-design-iconic-font"
       (new-webjar-resource "material-design-iconic-font")]
      ["font-awesome"
       (new-webjar-resource "font-awesome")]]]


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
