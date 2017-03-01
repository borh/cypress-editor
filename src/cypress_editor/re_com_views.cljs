(ns cypress-editor.re-com-views
  (:require
   [cypress-editor.subs :as subs]
   [cypress-editor.utils :refer [regex-formatter]]
   [re-frame.core :refer [subscribe dispatch]]
   [re-com.core :as rc]
   [re-frame-datatable.core :as dt]
   [reagent.core :as reagent]))


(defn search-options-box []
  (let [genre-column  (subscribe [:fulltext/genre-column])
        title-column  (subscribe [:fulltext/title-column])
        author-column (subscribe [:fulltext/author-column])
        year-column   (subscribe [:fulltext/year-column])

        kwic-toggle (subscribe [:fulltext/kwic])
        kwic-before (subscribe [:fulltext/kwic-before])
        kwic-after  (subscribe [:fulltext/kwic-after])]
    (fn []
      [rc/h-box
       :gap "14px"
       ;;:align :center
       :align-self :end
       :children [[rc/checkbox
                   :label     "ジャンル"
                   :model     genre-column
                   :on-change (fn [_]
                                (dispatch [:toggle/fulltext-genre-column]))]
                  [rc/checkbox
                   :label     "タイトル"
                   :model     title-column
                   :on-change (fn [_]
                                (dispatch [:toggle/fulltext-title-column]))]
                  [rc/checkbox
                   :label     "著者"
                   :model     author-column
                   :on-change (fn [_]
                                (dispatch [:toggle/fulltext-author-column]))]
                  [rc/checkbox
                   :label     "出版年"
                   :model     year-column
                   :on-change (fn [_]
                                (dispatch [:toggle/fulltext-year-column]))]

                  [rc/checkbox
                   :model kwic-toggle
                   :label "KWIC検索"
                   :on-change (fn [_]
                                (dispatch [:toggle/fulltext-kwic]))]

                  ;; TODO These don't really make sense when using ellipsis in table...
                  #_[rc/h-box
                     :children [[:span "前文脈"]
                                [rc/input-text
                                 :width "3em"
                                 :model kwic-before
                                 :validation-regex #"\d*"
                                 :disabled? (not @kwic-toggle)
                                 :on-change (fn [span]
                                              (dispatch [:set/fulltext-kwic-before span]))]
                                [:span "字"]]]

                  #_[rc/h-box
                     :children [[:span "後文脈"]
                                [rc/input-text
                                 :width "3em"
                                 :model kwic-after
                                 :validation-regex #"\d*"
                                 :disabled? (not @kwic-toggle)
                                 :on-change (fn [span]
                                              (dispatch [:set/fulltext-kwic-after span]))]
                                [:span "字"]]]]])))

(defn fulltext-results-table []
  (let [genre-column  (subscribe [:fulltext/genre-column])
        title-column  (subscribe [:fulltext/title-column])
        author-column (subscribe [:fulltext/author-column])
        year-column   (subscribe [:fulltext/year-column])

        kwic-toggle   (subscribe [:fulltext/kwic])

        regexp        (subscribe [:fulltext/query])]
    (fn []
      [dt/datatable
       :fulltext/datatable
       [:sentences/fulltext]
       (cond-> []

         (not @kwic-toggle)
         (conj
          {::dt/column-key   [:text]
           ::dt/sorting      {::dt/enabled? true}
           ;; ::dt/render-fn    (partial regex-formatter (re-pattern @regexp))
           ::dt/column-label "テキスト"})

         @kwic-toggle
         (into [{::dt/column-key   [:before]
                 ::dt/sorting      {::dt/enabled? true}
                 ::dt/column-label "前文"}
                {::dt/column-key   [:key]
                 ::dt/sorting      {::dt/enabled? true}
                 ::dt/column-label "キー"}
                {::dt/column-key   [:after]
                 ::dt/sorting      {::dt/enabled? true}
                 ::dt/column-label "後文"}])

         @genre-column
         (conj
          {::dt/column-key   [:genre]
           ::dt/sorting      {::dt/enabled? true}
           ::dt/column-label "ジャンル"})

         @title-column
         (conj
          {::dt/column-key   [:title]
           ::dt/sorting      {::dt/enabled? true}
           ::dt/column-label "タイトル"})

         @author-column
         (conj
          {::dt/column-key   [:author]
           ::dt/sorting      {::dt/enabled? true}
           ::dt/column-label "著者"})

         @year-column
         (conj
          {::dt/column-key   [:year]
           ::dt/sorting      {::dt/enabled? true}
           ::dt/column-label "出版年"}))
       {;; ::dt/pagination    {::dt/enabled? true
        ;;                     ::dt/per-page 20}
        ::dt/table-classes (if @kwic-toggle
                             ["ui" "table" "celled" "kwic"]
                             ["ui" "table" "celled"])}])))

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
       :children [[rc/button
                   :label (case @state
                            :loading "検索中"
                            :loaded "検索完了"
                            "検索")
                   :on-click (fn [_]
                               (dispatch [:get/sentences-fulltext {:query @query :genre @genre}]))]
                  (when (= :loading @state) [rc/throbber :size :small])]])))

(defn interface []
  [rc/v-box
   :size "100px"
   :gap "24px"
   :align :center
   :children [[rc/h-box
               :gap "28px"
               :children [[rc/box :align-self :center
                           :child [rc/title :label "Natsume DB全文検索"]]
                          [rc/gap :size "1"]
                          [search-options-box]]]
              [rc/h-box
               :gap "28px"
               :align :end
               :children [[rc/v-box
                           :children [[rc/h-box
                                       :gap      "4px"
                                       :children [[:span.field-label "正規表現検索"]
                                                  [rc/info-button
                                                   :info [:p [:a {:href "https://www.postgresql.jp/document/9.6/html/functions-matching.html" #_"https://www.postgresql.org/docs/9.3/static/functions-matching.html"} "説明"]]]]]
                                      [regex-search-box]]]

                          [rc/v-box
                           :children [[rc/h-box
                                       :gap      "4px"
                                       :children [[:span.field-label "ジャンルで絞り込検索"]
                                                  [rc/info-button
                                                   :info [:p "lquery"]]]]
                                      [genre-search-box]]]

                          [search-button]]]
              [rc/box :child [total-count-message]]
              [fulltext-results-table]]])
