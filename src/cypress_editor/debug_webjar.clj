(ns cypress-editor.debug-webjar
  (:import [org.webjars WebJarAssetLocator])
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [yada.resource :refer [resource as-resource]]
   [yada.resources.classpath-resource :refer [new-classpath-resource]]
   [yada.test :as test]))

(def ^:private webjars-pattern
  #"META-INF/resources/webjars/([^/]+)/([^/]+)/(.*)")

(defn- asset-path [resource]
  (let [[_ name version path] (re-matches webjars-pattern resource)]
    path))

(defn- asset-map [^WebJarAssetLocator locator webjar]
  (->> (.listAssets locator (str webjar "/"))
       (map (juxt asset-path identity))
       (into {})))

(defn new-webjar-resource
  "Create a new webjar resource that resolves requests with
   path info in the specified webjar name in the active classpath.

   Optionally takes an options map with the following keys:

   * index-files - a collection of files to try if the path info
                   ends with /

   Example:

      (new-webjar-resource \"swagger-ui\")

   would resolve index.html to META-INF/resources/webjars/swagger-ui//index.html inside
   the active classpath (e.g. of the JAR that serves the resource).

   If used with bidi, the following route

     [\"\" (yada (new-webjar-resource
                \"swagger-ui\" {:index-files [\"index.html\"]}))]

   can be used to serve all files for the swagger-ui webjar and fall back to
   index.html for URL paths like / or foo/."
  ([webjar]
   (new-webjar-resource webjar nil))
  ([webjar {:keys [index-files]}]
   (resource
    {:path-info?   true
     :methods      {}
     :sub-resource (let [assets (asset-map (WebJarAssetLocator.) webjar)]
                     ;; (println "assets" assets)
                     (fn [ctx]
                       (let [path-info (-> ctx :request :path-info)
                             ;;_ (println "path-info" path-info)
                             path (str/replace path-info #"^/" "")
                             ;;_ (println "path" path)
                             files (if (= (last path-info) \/)
                                     (map #(get assets (str path %)) index-files)
                                     (list (get assets path)))
                             ;;_ (println "files" files)
                             res (first (sequence (comp (drop-while nil?)
                                                        (map io/resource))
                                                  files))]
                         (as-resource res))))})))

(defn webjars-route-pair
  ""
  ([] (webjars-route-pair nil))
  ([options]
   (let [webjars (->> (WebJarAssetLocator.)
                      .getWebJars
                      keys
                      (map (juxt (fn [webjar-name]
                                   (str "/" webjar-name "/"))
                                 #(new-webjar-resource % options))))]
     ["" webjars])))