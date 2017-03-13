(ns cypress-editor.re-com-views
  (:require
   [cypress-editor.subs :as subs]
   [cypress-editor.utils :refer [regex-formatter]]
   [cypress-editor.communication :as comm]
   [re-frame.core :refer [subscribe dispatch]]
   [re-com.core :as rc]
   [re-frame-datatable.core :as dt]
   [reagent.core :as reagent]))

(defn search-options-box []
  (let [genre-column  (subscribe [:fulltext/genre-column])
        title-column  (subscribe [:fulltext/title-column])
        author-column (subscribe [:fulltext/author-column])
        year-column   (subscribe [:fulltext/year-column])]
    (fn []
      [rc/h-box
       :gap "14px"
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
                                (dispatch [:toggle/fulltext-year-column]))]]])))

(defn fulltext-results-table []
  (let [genre-column  (subscribe [:fulltext/genre-column])
        title-column  (subscribe [:fulltext/title-column])
        author-column (subscribe [:fulltext/author-column])
        year-column   (subscribe [:fulltext/year-column])

        regexp        (subscribe [:fulltext/query])]
    (fn []
      [dt/datatable
       :fulltext/datatable
       [:sentences/fulltext]
       (cond-> [{::dt/column-key   [:id]
                 ::dt/sorting      {::dt/enabled? false}
                 ::dt/render-fn
                 (fn [id]
                   [:i.zmdi.zmdi-file-text
                    {:on-click (fn [_]
                                 (dispatch [:get/sources-by-sentence-id id]))}])
                 ::dt/column-label ""}

                {::dt/column-key   [:before]
                 ::dt/sorting      {::dt/enabled? true}
                 ::dt/column-label "前文"}
                {::dt/column-key   [:key]
                 ::dt/sorting      {::dt/enabled? true}
                 ::dt/column-label "キー"}
                {::dt/column-key   [:after]
                 ::dt/sorting      {::dt/enabled? true}
                 ::dt/column-label "後文"}]

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
       {::dt/table-classes ["ui" "table" "celled" "kwic"]}])))

(defn total-count-message []
  (let [total-count (subscribe [:sentences/fulltext])]
    (when @total-count
      [:p "検索結果：" [:strong (str (count @total-count))] "件"])))

(defn regex-search-box []
  (let [query (subscribe [:fulltext/query])
        genre (subscribe [:user/genre])]
    (fn []
      [rc/input-text
       :model query
       :change-on-blur? true
       :on-change (fn [query-text]
                    (dispatch [:set/fulltext-state nil])
                    (dispatch [:set/fulltext-query query-text]))
       #_:attr #_{:on-key-up (fn [e]
                               (when (== (.-keyCode e) 13)
                                 (dispatch [:get/sentences-fulltext])))}])))

(defn genre-search-box []
  (let [query (subscribe [:fulltext/query])
        genre (subscribe [:user/genre])]
    (fn []
      [rc/input-text
       :model genre
       :change-on-blur? true
       :on-change (fn [genre-text]
                    (dispatch [:set/fulltext-state nil])
                    (dispatch [:set/user-genre genre-text]))
       #_:attr #_{:on-key-up (fn [e]
                               (when (== (.-keyCode e) 13)
                                 (dispatch [:get/sentences-fulltext])))}])))

(defn search-button []
  (let [state (subscribe [:fulltext/state])
        connection-status (subscribe [:sente/connection-status])]
    (fn []
      [rc/h-box
       :children [[rc/button
                   :label "検索"
                   :class (case @state
                            :loading "btn-info"
                            :loaded "btn-success"
                            "btn-primary")
                   :disabled? (or (not @connection-status)
                                  (= :loading @state)
                                  (= :loaded @state))
                   :tooltip (when-not @connection-status
                              "サーバとの通信が途絶えています")
                   :tooltip-position :above-center
                   :on-click (fn [_]
                               (dispatch [:get/sentences-fulltext]))]
                  (when (= :loading @state) [rc/throbber :size :small])]])))

(defn connection-status-box []
  (let [status (subscribe [:sente/connection-status])]
    (fn []
      [rc/h-box
       :gap "12px"
       :align :stretch
       :children
       [(if @status
          [:i.zmdi.zmdi-cloud-circle]
          [:i.zmdi.zmdi-cloud-off])
        [rc/button
         :label "ログアウト"
         :on-click (fn [_]
                     (dispatch [:set/user-account-valid nil])
                     (dispatch [:set/user-auth-token nil]))]]])))

(defn login-box []
  (let [user-name          (subscribe [:user/username])
        user-password      (subscribe [:user/password])
        user-account-valid (subscribe [:user/account-valid])]
    (fn []
      [rc/v-box
       :gap "24px"
       :align-self :center
       :align :center
       :children
       [[rc/gap :size "24px"]
        [rc/h-box
         :gap "5px"
         :align-self :center
         :children
         [[:span.field-label "ユーザ　　"]
          [rc/input-text
           :model user-name
           :attr {:name "username"}
           :status (when @user-account-valid :success)
           :on-change (fn [s] (dispatch [:set/user-username s]))]]]

        [rc/h-box
         :gap "5px"
         :align-self :center
         :children
         [[:span.field-label "パスワード"]
          [rc/input-password
           :model user-password
           :attr {:name "password"}
           :status (when @user-account-valid :success)
           :on-change (fn [s] (dispatch [:set/user-password s]))]]]

        [rc/button
         :label (if @user-account-valid "ログアウト" "ログイン")
         :on-click (fn [_] (dispatch [:sente/authenticate]))]

        (when (false? @user-account-valid)
          [rc/alert-box
           :alert-type :danger
           :heading "ログインに失敗しました"
           :body "ユーザ名とパスワードをもう一度ご確認の上、再度ログインしてください。"])]])))

(defn header-bar []
  [rc/h-box
   :gap "24px"
   :size "stretch"
   :align :center
   :align-self :center
   :children
   [[rc/box :align-self :center
     :child [rc/title :label "Natsume DB全文検索"]]
    [rc/gap :size "1"]
    [search-options-box]
    [connection-status-box]]])

(defn search-box []
  [rc/h-box
   :gap "28px"
   :align :end
   :children
   [[rc/v-box
     :children
     [[rc/h-box
       :gap "4px"
       :children
       [[:span.field-label "正規表現検索"]
        [rc/info-button
         :info [:p "任意の正規表現を入れてください。"
                [:a {:href "https://www.postgresql.jp/document/9.6/html/functions-matching.html"
                     #_"https://www.postgresql.org/docs/9.3/static/functions-matching.html"}
                 "（マニュアル「9.7.3.1. 正規表現」の詳細をご参照ください）"]]]]]
      [regex-search-box]]]

    [rc/v-box
     :children
     [[rc/h-box
       :gap "4px"
       :children
       [[:span.field-label "ジャンルで絞り込検索"]
        [rc/info-button
         :info [:p "例：「*」は全ジャンルを検索します。特定のジャンルは「書籍.*」のように指定ください。複数のジャンルは「白書|書籍.*」のように指定できます。"]]]]
      [genre-search-box]]]

    [search-button]]])

(defn debug-box []
  (let [auth-token (subscribe [:user/auth-token])
        csrf-token (subscribe [:user/csrf-token])]
    (fn []
      [rc/v-box
       :gap "5px"
       :children [[:span "auth-token: " @auth-token]
                  [:span "csrf-token: " @csrf-token]]])))

(defn interface []
  (let [user-account-valid (subscribe [:user/account-valid])

        document-text  (subscribe [:fulltext/document-text])
        show-document? (subscribe [:fulltext/document-show])]
    (fn []
      [rc/v-box
       :size "auto"
       :gap "24px"
       :align :center
       :children
       (cond-> []
         ^boolean js/goog.DEBUG
         (conj [debug-box])

         (not @user-account-valid)
         (conj [login-box])

         @user-account-valid
         (into
          [[header-bar]
           [rc/line :size "2px"]
           [search-box]
           [rc/box :child [total-count-message]]
           [fulltext-results-table]
           (when @show-document?
             [rc/modal-panel
              :backdrop-on-click
              (fn [] (dispatch [:toggle/fulltext-document-show]))
              :child
              [rc/scroller
               :v-scroll :auto
               :max-width "800px"
               :max-height "400px"
               :child [:div @document-text]]])]))])))
