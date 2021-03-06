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
 :source-paths #{"src" "sass" "test"}
 :resource-paths #{"resources"}
 :dependencies
 '[[adzerk/boot-cljs "LATEST" :scope "test"]
   [powerlaces/boot-figreload "LATEST" :scope "test"]
   [pandeiro/boot-http "0.8.3" :scope "test"]
   ;; Needed for start-repl in cljs repl
   [com.cemerick/piggieback "0.2.2" :scope "test"]
   [weasel "0.7.0" :scope "test"] ;; Websocket Server

   [adzerk/boot-cljs-repl "0.3.3" :scope "test"]
   [adzerk/boot-reload "0.6.0" :scope "test"]
   [deraen/boot-sass "0.3.1"]
   [reloaded.repl "0.2.4" :scope "test"]

   [adzerk/boot-test "1.2.0" :scope "test"]
   [tolitius/boot-check "0.1.11" :scope "test"]
   [org.clojure/test.check "0.10.0-alpha3" :scope "test"]

   [org.clojure/clojure "1.10.0-alpha6"]
   [org.clojure/clojurescript "1.10.339"]

   [org.clojure/tools.nrepl "0.2.13"]

   ;; Backend (server) deps
   [yada "1.2.4"]
   [aero "1.1.3"]
   [aleph "0.4.6" :exclusions [io.netty/netty-transport
                               io.netty/netty-transport-native-epoll
                               io.netty/netty-codec
                               io.netty/netty-codec-http
                               io.netty/netty-handler
                               io.netty/netty-handler-proxy
                               io.netty/netty-resolver
                               io.netty/netty-resolver-dns]]
   [io.netty/netty-all "4.1.29.Final"]
   [crypto-random "1.2.0" :exclusions [commons-codec]]
   [prismatic/schema "1.1.9"]
   [bidi "2.1.3" :exclusions [ring/ring-core]]
   [com.stuartsierra/component "0.3.2"]
   [hiccup "2.0.0-alpha1"]
   [org.clojure/tools.namespace "0.3.0-alpha4"]

   [com.taoensso/sente "1.13.1"]
   [com.taoensso/tempura "1.2.1"]
   [com.taoensso/encore "2.97.0"]

   [orchestra "2018.08.19-1" :scope "test"]

   ;; Frontend deps
   [reagent "LATEST"]
   [re-frame "0.10.6"]
   [day8.re-frame/test "0.1.5" :scope "test"]
   [re-frame-datatable "0.6.0"]
   [day8.re-frame/http-fx "0.1.6"]
   [day8.re-frame/async-flow-fx "0.0.11"]
   #_[day8.re-frame/undo "0.3.2"] ; TODO
   #_[com.smxemail/re-frame-cookie-fx "0.0.2"] ; TODO
   [akiroz.re-frame/storage "0.1.2"]
   [re-learn "0.1.1"] ; TODO
   [rid3 "0.2.1"]
   #_[thi.ng/geom "0.0.1062"]
   [re-frisk "0.5.4" :scope "test"]
   #_[day8.re-frame/trace "0.1.22-react16" :scope "test"]
   [day8.re-frame/re-frame-10x "0.3.3-react16" :scope "test"]
   [day8.re-frame/tracing "0.5.1" :scope "test"]
   [secretary "1.2.3"]
   ;; [funcool/hodgepodge "0.1.4"] ;; TODO: LocalStorage

   [org.webjars.npm/bulma "0.7.1"]
   #_[org.webjars/material-design-icons "3.0.1"]
   [org.webjars/font-awesome "5.3.1"]
   #_[org.webjars.bower/material-design-iconic-font "2.2.0"]
   [org.webjars.npm/balloon-css "0.5.0"]

   [com.cognitect/transit-clj "0.8.313"]
   [com.cognitect/transit-cljs "0.8.256"]
   [com.andrewmcveigh/cljs-time "0.5.2"] ;; for advanced compilation

   ;; Development tools
   [binaryage/devtools              "0.9.10"  :scope "test"]
   [binaryage/dirac                 "1.2.37"  :scope "test"]
   [powerlaces/boot-cljs-devtools   "0.2.0"   :scope "test"]
   [org.clojure/tools.reader        "1.3.0"   :scope "test"]
   [org.clojure/tools.analyzer.jvm  "0.7.2"   :scope "test"]
   [org.clojure/tools.analyzer      "0.6.9"   :scope "test"]
   [org.clojure/data.priority-map   "0.0.10"  :scope "test"]
   [org.clojure/core.memoize        "0.7.1"   :scope "test"]
   [org.clojure/core.cache          "0.7.1"   :scope "test"]
   [org.clojure/core.async          "0.4.474" :scope "test"]
   [com.google.code.findbugs/jsr305 "3.0.2"   :scope "test"]
   [http-kit "2.3.0" :scope "test"]

   ;; Logging
   [clj-logging-config "1.9.12" :scope "test"] ;; dirac?
   [org.clojure/tools.logging "0.5.0-alpha"]
   [org.slf4j/jcl-over-slf4j "1.8.0-beta2"]
   [org.slf4j/jul-to-slf4j "1.8.0-beta2"]
   [org.slf4j/log4j-over-slf4j "1.8.0-beta2"]
   [ch.qos.logback/logback-classic "1.3.0-alpha4"
    :exclusions [org.slf4j/slf4j-api]]])

(require '[adzerk.boot-test :refer :all]
         '[adzerk.boot-cljs :refer [cljs]]
         '[powerlaces.boot-figreload :refer [reload]]
         '[adzerk.boot-cljs-repl :refer [cljs-repl start-repl]]
         '[powerlaces.boot-cljs-devtools :refer [cljs-devtools dirac]]
         ;;'[adzerk.boot-reload :as b]
         ;;'[pandeiro.boot-http            :refer [serve]]
         '[deraen.boot-sass :refer [sass]]
         '[com.stuartsierra.component :as component]
         'clojure.tools.namespace.repl
         '[tolitius.boot-check :as check]
         '[cypress-editor.system :refer [new-system]])

(def repl-port 5610)

(task-options!
 repl {:client true
       :port repl-port}
 pom {:project (symbol project)
      :version version
      :description "The Cypress Editor"
      :license {"The MIT License (MIT)" "http://opensource.org/licenses/mit-license.php"}}
 aot {:namespace #{'cypress-editor.main}}
 jar {:main 'cypress-editor.main
      :file (str project "-app.jar")}
 target {:dir #{"target"}})


(deftask check-sources []
  (set-env! :source-paths #{"src"})
  (comp
   (check/with-yagni)
   (check/with-eastwood)
   (check/with-kibit)
   (check/with-bikeshed)))

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
  (set-env! :source-paths #(conj % "dev"))

  ;; Needed by tools.namespace to know where the source files are
  (apply clojure.tools.namespace.repl/set-refresh-dirs (get-env :directories))

  (comp
   (repl :server true)
   #_(serve)
   (watch)
   (speak)
   (sass)
   (cljs-devtools)
   (reload #_:on-jsload #_'cypress-editor.app/main)
   (dirac :nrepl-opts {:port 8230
                       :nrepl-middleware ['dirac.nrepl/middleware]
                       :client false})
   (cljs :ids #{"cypress_editor"}
         :optimizations :none
         :source-map true
         :compiler-options {:external-config
                            {:devtools/config {:features-to-install [:formatters :hints]
                                               :fn-symbol "λ"
                                               :print-config-overrides true}}
                            :closure-defines {"goog.DEBUG" true
                                              "day8.re_frame.tracing.trace_enabled_QMARK_" true
                                              "re_frame.trace.trace_enabled_QMARK_" true}
                            :preloads ['day8.re-frame-10x.preload
                                       'devtools.preload
                                       'dirac.runtime.preload]
                            :main "cypress-editor.app"
                            :parallel-build true})
   (dev-system)
   (target)))

(deftask build
  "This is used for creating optimized static resources under static"
  []
  (set-env! :dependencies (fn [deps] (conj deps '[day8.re-frame/tracing-stubs "0.5.1"])))
  (comp
   (sass :output-style :compressed)
   (cljs :ids #{"cypress_editor"}
         :optimizations :advanced
         :source-map true
         :compiler-options {:closure-defines {"goog.DEBUG" false}
                            :parallel-build true})))

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
         :port (case profile :prod 5611 :beta 5612 5610)
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
