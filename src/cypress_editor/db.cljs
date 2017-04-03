(ns cypress-editor.db)

(def ^boolean debug-enabled? "@define {boolean}" ^boolean js/goog.DEBUG)

(def sample-text
  ;; TODO add sample documents from Natane
  )

#_"https://hinoki-project.org/p/natsume-server-api"
(def base-url "nlp.lang.osaka-u.ac.jp/natsume-server/api")

(def api-url
  (str "https://" base-url))

(def ws-url
  (str "wss://" base-url))
