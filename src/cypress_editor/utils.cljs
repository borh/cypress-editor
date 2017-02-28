(ns cypress-editor.utils)

;; http://stackoverflow.com/questions/18735665/how-can-i-get-the-positions-of-regex-matches-in-clojurescript
(defn regex-modifiers
  "Returns the modifiers of a regex, concatenated as a string."
  [re]
  (str (if (.-multiline re) "m")
       (if (.-ignoreCase re) "i")))

(defn re-pos
  "Returns a vector of vectors, each subvector containing in order:
   the position of the match, the matched string, and any groups
   extracted from the match."
  [re s]
  (let [re (js/RegExp. (.-source re) (str "g" (regex-modifiers re)))]
    (loop [res []]
      (if-let [m (.exec re s)]
        (let [begin (.-index m)
              end (.-lastIndex re)]
          (recur (conj res [begin end])))
        res))))

(defn regex-formatter
  "Returns a hiccup :span vector with matched regular expression
  wrapped in :strong objects."
  [rx text]
  (loop [r [:span]
         last-end 0
         m (re-pos rx text)]
    (if (seq m)
      (let [[begin end] (first m)
            before-string (subs text last-end begin)
            hl-string (subs text begin end)]
        (recur (into r [before-string [:strong hl-string]])
               end
               (next m)))
      (cond (empty? r) [text]
            (= last-end (count text)) r
            :else (into r [(subs text last-end (inc (count text)))])))))

(defn regex-formatter-multiple
  "Returns a hiccup :span vector with matched regular expression
  wrapped in :strong objects, one for each match.
  FIXME should probably check is no matches are found."
  [rx text before-span after-span]
  (let [matches (re-pos rx text)]
    (if-not (seq matches)
      (println "Server-client regular expression error: " rx text))
    (map
     (fn [[begin end]]
       (let [before-string (subs text 0 begin)
             after-string (subs text end (inc (count text)))
             hl-string (subs text begin end)]
         [:span before-string [:strong hl-string] after-string]))
     matches)))
