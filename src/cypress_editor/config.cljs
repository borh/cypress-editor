(ns cypress-editor.config)

(def ^boolean debug-enabled? "@define {boolean}" ^boolean js/goog.DEBUG)

(def base-url "nlp.lang.osaka-u.ac.jp/natsume-server/api")

(def api-url
  (str "https://" base-url))

(def ws-url
  (str "wss://" base-url))
