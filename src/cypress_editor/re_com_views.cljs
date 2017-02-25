(ns cypress-editor.re-com-views
  (:require
   [cypress-editor.subs :as subs]
   [re-frame.core :refer [subscribe dispatch]]
   [re-com.core :as rc]
   [re-frame-datatable.core :as dt]
   [reagent.core :as reagent]))

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

(defn regex-formatter [text rx]
  ;; (println text (re-pos rx text))
  (loop [r []
         last-end 0
         m (re-pos rx text)]
    (if (seq m)
      (let [[begin end] (first m)
            before-string (subs text last-end begin)
            after-string (subs text end (inc (count text)))
            hl-string (subs text begin end)]
        (recur (into r [before-string [:strong hl-string]])
               end
               (next m)))
      (if (empty? r) [text] (into r [(subs text last-end (inc (count text)))])))))

(defn toggle-column [s column]
  (if (column s)
    (disj s column)
    (conj s column)))

(defn column-selection []
  (let [genre-column  (subscribe [:fulltext/genre-column])
        title-column  (subscribe [:fulltext/title-column])
        author-column (subscribe [:fulltext/author-column])
        year-column   (subscribe [:fulltext/year-column])]
    (fn []
      [rc/h-box
       :gap "14px"
       ;;:align :center
       :align-self :end
       :children [[rc/checkbox
                   :label     "Genre"
                   :model     genre-column
                   :on-change (fn [_]
                                (dispatch [:toggle/fulltext-genre-column]))]
                  [rc/checkbox
                   :label     "Title"
                   :model     title-column
                   :on-change (fn [_]
                                (dispatch [:toggle/fulltext-title-column]))]
                  [rc/checkbox
                   :label     "Author"
                   :model     author-column
                   :on-change (fn [_]
                                (dispatch [:toggle/fulltext-author-column]))]
                  [rc/checkbox
                   :label     "Year"
                   :model     year-column
                   :on-change (fn [_]
                                (dispatch [:toggle/fulltext-year-column]))]]])))

(defn fulltext-results-table []
  (let [genre-column  (subscribe [:fulltext/genre-column])
        title-column  (subscribe [:fulltext/title-column])
        author-column (subscribe [:fulltext/author-column])
        year-column   (subscribe [:fulltext/year-column])]
    (fn []
      [dt/datatable
       :fulltext/datatable
       [:sentences/fulltext]
       (cond->
           [{::dt/column-key   [:text]
             ::dt/sorting      {::dt/enabled? true}
             ;;::dt/render-fn    regex-formatter
             ::dt/column-label "Text"}]

         @genre-column
         (conj
          {::dt/column-key   [:genre]
           ::dt/sorting      {::dt/enabled? true}
           ::dt/column-label "Genre"})

         @title-column
         (conj
          {::dt/column-key   [:title]
           ::dt/sorting      {::dt/enabled? true}
           ::dt/column-label "Title"})

         @author-column
         (conj
          {::dt/column-key   [:author]
           ::dt/sorting      {::dt/enabled? true}
           ::dt/column-label "Author"})

         @year-column
         (conj
          {::dt/column-key   [:year]
           ::dt/sorting      {::dt/enabled? true}
           ::dt/column-label "Year"}))
       {;; ::dt/pagination    {::dt/enabled? true
        ;;                     ::dt/per-page 20}
        ::dt/table-classes ["ui" "table" "celled"]}])))

(defn total-count-message []
  (let [total-count (subscribe [:sentences/fulltext])]
    (when @total-count
      [:p [:strong (str (count @total-count))] " matches"])))

(defn regex-search-box []
  (let [m (subscribe [:fulltext/query])]
    (fn []
      [rc/input-text
       :model m
       :on-change (fn [query-text]
                    (dispatch [:set/fulltext-state nil])
                    (dispatch [:set/fulltext-query query-text]))])))

(defn genre-search-box []
  (let [m (subscribe [:user/genre])]
    (fn []
      [rc/input-text
       :model m
       :on-change (fn [genre-text]
                    (dispatch [:set/fulltext-state nil])
                    (dispatch [:set/user-genre genre-text]))])))

(defn search-button []
  (let [query (subscribe [:fulltext/query])
        genre (subscribe [:user/genre])

        state (subscribe [:fulltext/state])]
    (fn []
      [rc/h-box
       ;; :height   "50px"
       ;; :gap      "50px"
       ;; :align    :baseline
       :children [[rc/button
                   :label (case @state
                            :loading "Searching"
                            :loaded "Finished"
                            "Search")
                   ;; :tooltip          "I'm a tooltip on the left"
                   ;; :tooltip-position :left-center
                   ;;:align-self :center
                   :on-click (fn [_]
                               (dispatch [:get/sentences-fulltext {:query @query :genre @genre}]))]
                  (when (= :loading @state) [rc/throbber :size :small])]])))

(defn interface []
  [rc/v-box
   :size "100px"
   :gap "24px"
   :align :center
   :children [[rc/h-box
               ;;:align :center
               ;;:align-self :center
               :gap "28px"
               :children [[rc/box :align-self :center
                           :child [rc/title :label "Natsume DB fulltext search"]]
                          [rc/gap :size "1"]
                          [column-selection]]]
              [rc/h-box
               :gap "28px"
               :align :end
               :children [[rc/v-box
                           :children [[rc/h-box
                                       :gap      "4px"
                                       :children [[:span.field-label "Regular expression search"]
                                                  [rc/info-button
                                                   :info [:p [:a {:href "https://www.postgresql.org/docs/9.3/static/functions-matching.html"} "Documentation"]]]]]
                                      [regex-search-box]]]

                          [rc/v-box
                           :children [[rc/h-box
                                       :gap      "4px"
                                       :children [[:span.field-label "Genre filter"]
                                                  [rc/info-button
                                                   :info [:p "lquery"]]]]
                                      [genre-search-box]]]

                          [search-button]]]
              [rc/box :child [total-count-message]]
              [fulltext-results-table]]])
