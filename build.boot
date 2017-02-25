;; Copyright © 2016, JUXT LTD.

;; A complete development environment for websites in Clojure and
;; ClojureScript.

;; Most users will use 'boot dev' from the command-line or via an IDE
;; (e.g. CIDER).

;; See README.md for more details.

(require '[clojure.java.shell :as sh])

(defn next-version [version]
  (when version
    (let [[a b] (next (re-matches #"(.*?)([\d]+)" version))]
      (when (and a b)
        (str a (inc (Long/parseLong b)))))))

(defn deduce-version-from-git
  "Avoid another decade of pointless, unnecessary and error-prone
  fiddling with version labels in source code."
  []
  (let [[version commits hash dirty?]
        (next (re-matches #"(.*?)-(.*?)-(.*?)(-dirty)?\n"
                          (:out (sh/sh "git" "describe" "--dirty" "--long" "--tags" "--match" "[0-9].*"))))]
    (cond
      dirty? (str (next-version version) "-" hash "-dirty")
      (pos? (Long/parseLong commits)) (str (next-version version) "-" hash)
      :otherwise version)))

(def project "cypress-editor")
(def version (deduce-version-from-git))

(set-env!
 :source-paths #{"src"}
 :resource-paths #{"resources" "assets"}
 ;;:asset-paths #{"assets"}
 :dependencies
 '[[adzerk/boot-cljs "1.7.228-2" :scope "test"]
   [adzerk/boot-cljs-repl "0.3.3" :scope "test"]
   [adzerk/boot-reload "0.5.1" :scope "test"]
   [weasel "0.7.0" :scope "test"] ;; Websocket Server
   [deraen/boot-sass "0.3.0"]
   [reloaded.repl "0.2.3" :scope "test"]

   [tolitius/boot-check "0.1.4" :scope "test"]

   [org.clojure/clojure "1.9.0-alpha14"]
   [org.clojure/clojurescript "1.9.473"]

   [org.clojure/tools.nrepl "0.2.12"]

   ;; Needed for start-repl in cljs repl
   [com.cemerick/piggieback "0.2.1" :scope "test"]

   ;; Server deps
   [yada "1.2.1" :exclusions [aleph manifold ring-swagger prismatic/schema]]
   [aero "1.1.2"]
   [aleph "0.4.2-alpha12"]
   [bidi "2.0.16" :exclusions [ring/ring-core]]
   [com.stuartsierra/component "0.3.2"]
   [hiccup "2.0.0-alpha1"]
   [org.clojure/tools.namespace "0.3.0-alpha3"]
   [prismatic/schema "1.1.3"]
   [selmer "1.10.6" :exclusions [json-html]]
   [metosin/ring-swagger "0.23.0"] ;; Override version in yada?
   [com.taoensso/sente "1.11.0"]

   ;; App deps
   [reagent "0.6.0"]
   [re-frame "0.9.2"]
   [re-frame-datatable "0.5.1"]
   [day8.re-frame/http-fx "0.1.3"]
   [day8.re-frame/async-flow-fx "0.0.6"]
   [re-com "2.0.0"]
   [thi.ng/geom "0.0.1062"]
   [re-frisk "0.3.2" :scope "test"]
   [mount "0.1.11"]
   [funcool/hodgepodge "0.1.4"] ;; LocalStorage

   #_[org.webjars.npm/bulma "0.3.1"]
   #_[org.webjars/bootstrap "4.0.0-alpha.3"]
   #_[org.webjars/material-design-icons "3.0.1"]
   #_[org.webjars.bower/fontawesome "4.6.3"]

   [com.cognitect/transit-clj "0.8.297"]
   [com.cognitect/transit-cljs "0.8.239"]
   ;; [com.google.javascript/closure-compiler "v20170218" :scope "test"] ;; FIXME (boot show -p)
   [com.andrewmcveigh/cljs-time "0.5.0-alpha2"] ;; for advanced compilation

   ;; Development tools
   [devcards "0.2.2" :exclusions [cljsjs/react]]

   [binaryage/devtools      "0.9.1" :scope "test"]
   [binaryage/dirac         "1.1.5" :scope "test"]
   [powerlaces/boot-cljs-devtools   "0.2.0"   :scope "test"]
   [org.clojure/tools.reader        "1.0.0-beta4" :scope "test"]
   [org.clojure/tools.analyzer.jvm  "0.7.0"   :scope "test"]
   [org.clojure/tools.analyzer      "0.6.9"   :scope "test"]
   [org.clojure/data.priority-map   "0.0.7"   :scope "test"]
   [org.clojure/core.memoize        "0.5.9"   :scope "test"]
   [org.clojure/core.cache          "0.6.5"   :scope "test"]
   [org.clojure/core.async          "0.3.441" :scope "test"]
   [com.google.code.findbugs/jsr305 "3.0.1" :scope "test"]
   [http-kit "2.3.0-alpha1" :scope "test"]

   ;; Logging
   [clj-logging-config "1.9.12" :scope "test"] ;; dirac?
   [org.clojure/tools.logging "0.3.1"]
   [org.slf4j/jcl-over-slf4j "1.7.24"]
   [org.slf4j/jul-to-slf4j "1.7.24"]
   [org.slf4j/log4j-over-slf4j "1.7.24"]
   [ch.qos.logback/logback-classic "1.2.1"
    :exclusions [org.slf4j/slf4j-api]]])

(require '[adzerk.boot-cljs :refer [cljs]]
         '[adzerk.boot-cljs-repl :refer [cljs-repl start-repl]]
         '[powerlaces.boot-cljs-devtools :refer [cljs-devtools]]
         '[adzerk.boot-reload :refer [reload]]
         '[deraen.boot-sass :refer [sass]]
         '[com.stuartsierra.component :as component]
         'clojure.tools.namespace.repl
         '[tolitius.boot-check :as check]
         '[cypress-editor.system :refer [new-system]])

(def repl-port 5600)

(task-options!
 repl {:client true
       :port repl-port}
 pom {:project (symbol project)
      :version version
      :description "The Cypress Editor"
      :license {"The MIT License (MIT)" "http://opensource.org/licenses/mit-license.php"}}
 aot {:namespace #{'cypress-editor.main}}
 jar {:main 'cypress-editor.main
      :file (str project "-app.jar")})


(deftask check-sources []
  (set-env! :source-paths #{"src"})
  (comp
   (check/with-yagni)
   (check/with-eastwood)
   (check/with-kibit)
   (check/with-bikeshed)))

(deftask webjar-sass
  ""
  []
  (apply clojure.tools.namespace.repl/set-refresh-dirs (get-env :directories))
  (comp
   (sift :add-jar {'org.webjars.npm/bulma #".*"})
   (sass :output-style :expanded)))

(deftask dev-system
  "Develop the server backend. The system is automatically started in
  the dev profile."
  []
  (require 'reloaded.repl)
  (let [go (resolve 'reloaded.repl/go)]
    (try
      (require 'user)
      (go)
      (catch Exception e
        (boot.util/fail "Exception while starting the system\n")
        (boot.util/print-ex e))))
  identity)

(deftask dev
  "This is the main development entry point."
  []
  (set-env! :dependencies #(vec (concat % '[[reloaded.repl "0.2.3"]])))
  (set-env! :source-paths #(conj % "dev"))

  ;; Needed by tools.namespace to know where the source files are
  (apply clojure.tools.namespace.repl/set-refresh-dirs (get-env :directories))

  (comp
   (watch)
   (speak)
   ;; (webjar-sass)
   (reload :on-jsload 'cypress-editor.app/main)
   ;; (cljs-devtools) ; wait for new version
   (cljs-repl :nrepl-opts {:client false
                           :port repl-port
                           :init-ns 'user}) ; this is also the server repl!
   (cljs :ids #{"cypress_editor"} :optimizations :none
         :compiler-options {:preloads '[devtools.preload]})
   (dev-system)
   (target)))

(deftask build
  "This is used for creating optimized static resources under static"
  []
  (comp
   ;; (sass :output-style :compressed)
   (cljs :ids #{"cypress_editor"}
         :optimizations :advanced
         :compiler-options {:closure-defines {"goog.DEBUG" false}})
   (target :dir #{"static"})))

(defn- run-system [profile]
  (println "Running system with profile" profile)
  (let [system (new-system profile)]
    (component/start system)
    (intern 'user 'system system)
    (with-pre-wrap fileset
      (assoc fileset :system system))))

(deftask run [p profile VAL kw "Profile"]
  (comp
   (repl :server true
         :port (case profile :prod 5601 :beta 5602 5600)
         :init-ns 'user)
   (run-system (or profile :prod))
   (wait)))

(deftask uberjar
  "Build an uberjar"
  []
  (println "Building uberjar")
  (comp
   (build)
   (aot)
   (pom)
   (uber)
   (jar)
   (target)))
