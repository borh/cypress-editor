;; Copyright © 2016, JUXT LTD.

;; A complete development environment for websites in Clojure and
;; ClojureScript.

;; Most users will use 'boot dev' from the command-line or via an IDE
;; (e.g. CIDER).

;; See README.md for more details.

(set-env!
 :source-paths #{"sass" "src"}
 :resource-paths #{"resources"}
 :asset-paths #{"assets"}
 :dependencies
 '[[adzerk/boot-cljs "1.7.228-1" :scope "test"]
   [adzerk/boot-cljs-repl "0.3.3" :scope "test"]
   [adzerk/boot-reload "0.4.12" :scope "test"]
   [weasel "0.7.0" :scope "test"] ;; Websocket Server
   [deraen/boot-sass "0.2.1" :scope "test"]
   [reloaded.repl "0.2.3" :scope "test"]

   [org.clojure/clojure "1.9.0-alpha13"]
   [org.clojure/clojurescript "1.9.229"]

   [org.clojure/tools.nrepl "0.2.12"]

   ;; Needed for start-repl in cljs repl
   [com.cemerick/piggieback "0.2.1" :scope "test"]

   ;; Server deps
   [yada "1.1.37" :exclusions [aleph manifold ring-swagger prismatic/schema]]
   [aero "1.0.1"]
   [aleph "0.4.2-alpha8"]
   [bidi "2.0.12"]
   [com.stuartsierra/component "0.3.1"]
   [hiccup "1.0.5"]
   [org.clojure/tools.namespace "0.3.0-alpha3"]
   [prismatic/schema "1.1.3"]
   [selmer "1.0.9" :exclusions [json-html]]
   [metosin/ring-swagger "0.22.11"] ;; Override version in yada?

   ;; App deps
   [reagent "0.6.0"]
   [re-frame "0.8.0"]
   [day8.re-frame/http-fx "0.0.4"]
   [day8.re-frame/async-flow-fx "0.0.6"]
   [re-com "0.9.0"]
   [thi.ng/geom "0.0.1062"]
   [re-frisk "0.2.1" :scope "test"]

   [org.webjars/bootstrap "4.0.0-alpha.3"]
   [org.webjars/material-design-icons "3.0.1"]
   [org.webjars.bower/fontawesome "4.6.3"]

   [devcards "0.2.2" :exclusions [cljsjs/react]]

   [binaryage/devtools      "0.8.2" :scope "test"]
   [binaryage/dirac         "0.6.7" :scope "test"]
   [powerlaces/boot-cljs-devtools   "0.1.1"   :scope "test"]
   [org.clojure/tools.analyzer.jvm  "0.6.10"  :scope "test"]
   [org.clojure/tools.analyzer      "0.6.9"   :scope "test"]
   [org.clojure/data.priority-map   "0.0.7"   :scope "test"]
   [org.clojure/core.memoize        "0.5.9"   :scope "test"]
   [org.clojure/core.cache          "0.6.5"   :scope "test"]
   [org.clojure/core.async          "0.2.391" :scope "test"]
   [com.google.code.findbugs/jsr305 "3.0.1" :scope "test"]
   [http-kit "2.2.0" :scope "test"]

   [com.cognitect/transit-clj "0.8.290"]
   [com.cognitect/transit-cljs "0.8.239"]
   [com.google.javascript/closure-compiler "v20160822" :scope "test"] ;; FIXME (boot show -p)

   ;; Logging
   [clj-logging-config "1.9.12" :scope "test"] ;; dirac?
   [org.clojure/tools.logging "0.3.1"]
   [org.slf4j/jcl-over-slf4j "1.7.21"]
   [org.slf4j/jul-to-slf4j "1.7.21"]
   [org.slf4j/log4j-over-slf4j "1.7.21"]
   [ch.qos.logback/logback-classic "1.1.7"
    :exclusions [org.slf4j/slf4j-api]]])

(require '[adzerk.boot-cljs :refer [cljs]]
         '[adzerk.boot-cljs-repl :refer [cljs-repl start-repl]]
         '[powerlaces.boot-cljs-devtools :refer [cljs-devtools]]
         '[adzerk.boot-reload :refer [reload]]
         '[deraen.boot-sass :refer [sass]]
         '[com.stuartsierra.component :as component]
         'clojure.tools.namespace.repl
         '[cypress-editor.system :refer [new-system]])

(def repl-port 5600)

(task-options!
 repl {:client true
       :port repl-port})

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
   (sass :output-style :expanded)
   (reload :on-jsload 'cypress-editor.app/main)
   ;;(cljs-devtools) ; wait for new version
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
   (sass :output-style :compressed)
   (cljs :ids #{"cypress_editor"} :optimizations :advanced)
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
