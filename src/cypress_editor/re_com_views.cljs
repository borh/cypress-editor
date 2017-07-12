(ns cypress-editor.re-com-views
  (:require
   [clojure.string :as string]
   [cypress-editor.bulma-ui :as ui]
   [cypress-editor.viz :as viz]
   [cypress-editor.config :refer [debug-enabled?]]
   [cypress-editor.utils :as utils]
   [reagent.core :as reagent]
   [re-frame.core :refer [subscribe dispatch]]
   [re-frame-datatable.core :as dt]
   [re-learn.core :as re-learn]))

(defn search-options-box []
  (re-learn/with-lesson
    {:id          :first-lesson
     :description "検索結果でどの列を表示するかをこちらで選択できます。"
     :position    :bottom                  ;; optional, defaults to :right. values are :left, :right, :bottom, :top, :unattached, :bottom-left etc
     :version     1                        ;; optional, defaults to 1
     :attach      [:div.nav-center]}
    (let [genre-column  (subscribe [:fulltext/genre-column])
          title-column  (subscribe [:fulltext/title-column])
          author-column (subscribe [:fulltext/author-column])
          year-column   (subscribe [:fulltext/year-column])

          speech-tag    (subscribe [:fulltext/speech-tag])
          quotation-tag     (subscribe [:fulltext/quotation-tag])]
      (fn []
        [:div.nav-center ;; FIXME this element is duplicated in navbar code
         [:span.nav-item [:label "結果表示のオプション："]]
         [:span.nav-item (ui/checkbox {:label "会話文"
                                       :model speech-tag
                                       :on-change #(dispatch [:toggle/fulltext-speech-tag])})]
         [:span.nav-item (ui/checkbox {:label "引用文"
                                       :model quotation-tag
                                       :on-change #(dispatch [:toggle/fulltext-quotation-tag])})]
         [:span.nav-item (ui/checkbox {:label "ジャンル"
                                       :model genre-column
                                       :on-change #(dispatch [:toggle/fulltext-genre-column])})]
         [:span.nav-item (ui/checkbox {:label "タイトル"
                                       :model title-column
                                       :on-change #(dispatch [:toggle/fulltext-title-column])})]
         [:span.nav-item (ui/checkbox {:label "著者"
                                       :model author-column
                                       :on-change #(dispatch [:toggle/fulltext-author-column])})]
         [:span.nav-item (ui/checkbox {:label "出版年"
                                       :model year-column
                                       :on-change #(dispatch [:toggle/fulltext-year-column])})]]))))

(defn fulltext-results-table []
  (let [genre-column  (subscribe [:fulltext/genre-column])
        title-column  (subscribe [:fulltext/title-column])
        author-column (subscribe [:fulltext/author-column])
        year-column   (subscribe [:fulltext/year-column])

        regexp        (subscribe [:fulltext/query])]
    (fn []
      [dt/datatable
       :fulltext/datatable
       [:fulltext/matches]
       (cond-> [{::dt/column-key [:id]
                 ::dt/sorting {::dt/enabled? false}
                 ::dt/render-fn
                 (fn [id]
                   (ui/fa-icon
                    :file-text
                    {:size :is-small
                     :on-click (fn [_]
                                 (dispatch [:get/sources-by-sentence-id id]))}))
                 ::dt/column-label ""}

                {::dt/column-key [:tags]
                 ::dt/sorting {::dt/enabled? true}
                 ::dt/render-fn
                 (fn [tags]
                   [:div
                    (if (:speech tags)
                      [:span.tag.is-danger "話"])
                    (if (:quotation tags)
                      [:span.tag.is-warning "引"])])
                 ::dt/column-label "タグ"}

                {::dt/column-key [:before]
                 ::dt/sorting {::dt/enabled? true}
                 ;; TODO Not ideal, but this reverse transformation works...
                 ;; FIXME render-fn should really be a reagent/create-class?
                 ::dt/render-fn (fn [s] [:span (string/reverse s)])
                 ::dt/column-label "前文"}

                {::dt/column-key [:key]
                 ::dt/sorting {::dt/enabled? true}
                 ::dt/column-label "キー"}

                {::dt/column-key [:after]
                 ::dt/sorting {::dt/enabled? true}
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
       {::dt/table-classes ["ui" "table" "celled" "kwic" "is-narrow"]}])))

(defn total-count-message []
  (let [total-count (subscribe [:fulltext/total-count])]
    (when @total-count
      [:div.level-item.has-text-centered
       [:div
        [:p.heading "検索結果"]
        [:p.title @total-count "件"]]])))

(defn patterns-message []
  (let [patterns (subscribe [:fulltext/patterns])]
    (when @patterns
      [:div.level-item.has-text-centered

       [viz/barchart patterns]

       #_[:div
          [:p.heading "正規表現パターン"]
          (into [:p.title]
                (interleave
                 (for [[pattern pattern-freq] @patterns]
                   [:span pattern " ⇒ " pattern-freq])
                 (repeat ", ")))]])))

(defn regex-search-box []
  (let [query (subscribe [:fulltext/query])
        state (subscribe [:fulltext/state])
        composition-state (reagent/atom false)]
    (fn []
      (ui/form {:model query
                :label "正規表現検索"
                :label-tooltip "任意の正規表現を入れてください。\n文頭の指定は「^」マークでできます。例えば「^それ(故|ゆえ)に?」で「それゆえに」などの表現が文頭で現れる文のみが検索されます。"
                #_[:p [:a {:href "https://www.postgresql.jp/document/9.6/html/functions-matching.html" #_"https://www.postgresql.org/docs/9.6/static/functions-matching.html"} "（マニュアル「9.7.3.1. 正規表現」の詳細をご参照ください）"]]
                :placeholder "正規表現を入れてください"
                :is-horizontal true
                :load-state state
                :attrs {:on-blur
                        (fn [e]
                          (let [query-text (-> e .-target .-value)]
                            (dispatch [:set/fulltext-state nil])
                            (dispatch [:set/fulltext-query query-text])))
                        ;; TODO IME integration in React, https://w3c.github.io/input-events/
                        :on-composition-start
                        (fn [e] (reset! composition-state true))
                        :on-composition-update
                        (fn [e] (reset! composition-state false))
                        :on-composition-end
                        (fn [e]
                          (reset! composition-state false)
                          (dispatch [:set/fulltext-state nil])
                          (dispatch [:set/fulltext-query (-> e .-target .-value)]))
                        #_:on-key-up #_(fn [e]
                                         (when (and (not @composition-state)
                                                    (== (.-keyCode e) 13))
                                           (dispatch [:get/fulltext-matches])))}
                #_:on-change #_(fn [e]
                                 (let [query-text (-> e .-target .-value)]
                                   (when (not @composition-state)
                                     (dispatch [:set/fulltext-state nil])
                                     (dispatch [:set/fulltext-query query-text]))))}))))

(defn genre-search-box []
  (let [genre (subscribe [:user/genre])
        state (subscribe [:fulltext/state])
        composition-state (reagent/atom false)]
    (fn []
      (ui/form {:model genre
                :label "ジャンル絞り込み検索"
                :label-tooltip "例：「*」は全ジャンルを検索します。特定のジャンルは「書籍.*」のように指定ください。複数のジャンルは「白書|書籍.*」のように指定できます。"
                :placeholder "ジャンルのクエリ"
                :is-horizontal true
                :load-state state
                :attrs {:on-composition-start #(reset! composition-state true)
                        :on-composition-update #(reset! composition-state false)
                        :on-composition-end #(reset! composition-state false)
                        :on-blur (fn [e]
                                   (let [genre-text (-> e .-target .-value)]
                                     (dispatch [:set/fulltext-state nil])
                                     (dispatch [:set/user-genre genre-text])))}}))))

(defn search-button []
  (let [state (subscribe [:fulltext/state])
        connection-state (subscribe [:sente/connection-status])]
    (fn []
      [:div.field.is-horizontal
       [:div.field-label]
       [:div.field-body
        [:div.field
         [:p.control
          (ui/button
           {:label "検索"
            :load-state state
            :disabled? (or (not @connection-state) (= :loading @state))
            :on-click #(dispatch [:get/fulltext-matches])})]]]])))

(defn search-box []
  [:div.columns.is-padded
   [:div.column
    [regex-search-box]
    [genre-search-box]
    [search-button]]])

(defn logout-box []
  [:a.nav-item
   {:on-click (fn [_]
                (utils/redirect "login")
                #_(dispatch [:set/user-account-valid nil])
                #_(dispatch [:set/user-auth-token nil])
                #_(dispatch [:set/active-page :login]))}
   #_(ui/fa-icon :sign-in)
   (ui/fa-icon :sign-out)
   "ログアウト"])

(defn user-box []
  (let [username (subscribe [:user/username])]
    (fn []
      [:span.nav-item @username])))

(defn connection-status-box []
  (let [status (subscribe [:sente/connection-status])]
    (fn []
      [:a
       (if (not @status)
         {:class "nav-item is-active is-tab is-danger"
          :data-balloon "サーバとの通信が途絶えています"
          :data-balloon-length "large"
          :data-balloon-pos "down"
          :on-click #(dispatch [:sente/connect])}
         {:class "nav-item"
          :data-balloon "サーバとの通信状態は通常です"
          :data-balloon-length "large"
          :data-balloon-pos "down"})
       (if (not @status)
         (ui/fa-icon :chain-broken)
         (ui/fa-icon :link))])))

(defn login-box []
  (let [active-page        (subscribe [:page/active])
        user-name          (subscribe [:user/username])
        user-password      (subscribe [:user/password])
        user-account-valid (subscribe [:user/account-valid])
        connection-status  (subscribe [:sente/connection-status])]
    (fn []
      [:div.columns.is-centered.is-padded
       [:div.column.is-5-desktop.is-8-tablet
        (ui/form {:model user-name
                  :form-type "username"
                  :attrs {:name "username"}
                  :is-horizontal true
                  :label   "ユーザ"
                  :placeholder "ユーザ名"
                  :load-state (if @user-account-valid (atom :success) (atom nil))
                  :icon :user
                  :on-change (fn [e] (dispatch [:set/user-username
                                                (-> e .-target .-value)]))})
        (ui/form {:model user-password
                  :form-type "password"
                  :attrs {:name "password"}
                  :is-horizontal true
                  :label   "パスワード"
                  :placeholder "パスワード"
                  :load-state (if @user-account-valid (atom :success) (atom nil))
                  :icon :lock
                  :on-change (fn [e] (dispatch [:set/user-password
                                                (-> e .-target .-value)]))})
        [:div.field.is-horizontal
         [:div.field-label]
         [:div.field-body
          [:div.field
           [:div.control
            (ui/button {:label (if @user-account-valid "ログアウト" "ログイン")
                        ;; :disabled? (not @connection-status)
                        ;; :tooltip (if (not @connection-status)
                        ;;            "サーバとの通信が途絶えています"
                        ;;            nil)
                        ;; :tooltip-pos "right"
                        :on-click (fn [_]
                                    #_(if (= :login @active-page))
                                    (dispatch [:sente/authenticate]))})]]]]
        (when (false? @user-account-valid)
          (ui/notification
           {:state-level "is-danger"
            :heading "ログインに失敗しました"
            :body "ユーザ名とパスワードをもう一度ご確認の上、再度ログインしてください。"}))]])))

(defn header-bar []
  (ui/navbar {:app-name "Cypress Fulltext Search"
              :nav-items {;;:left []
                          :center [[search-options-box]
                                   [connection-status-box]]
                          :right [[user-box]
                                  [logout-box]]}}))

(defn debug-box []
  (let [auth-token (subscribe [:user/auth-token])
        csrf-token (subscribe [:user/csrf-token])]
    (fn []
      [:div
       [:p "auth-token: " @auth-token]
       [:p "csrf-token: " @csrf-token]])))

(defn interface []
  (let [active-page (subscribe [:page/active])

        sente-connection-status (subscribe [:sente/connection-status])

        user-account-valid (subscribe [:user/account-valid])

        document-data (subscribe [:fulltext/document-data])
        show-document? (subscribe [:fulltext/document-show])]
    (fn []
      (cond-> [:div.container.is-fluid]

        debug-enabled?
        (conj [debug-box])

        (or (not @sente-connection-status)
            (= :login @active-page))
        (into [(ui/navbar {:app-name "Cypress Fulltext Search"
                           :nav-items {:center [[connection-status-box]]}})
               [login-box]])

        (and @sente-connection-status ;;@user-account-valid
             (= :app @active-page))
        (into
         [[header-bar]
          [search-box]
          [:nav.level
           [total-count-message]
           [patterns-message]]
          [fulltext-results-table]
          (when @show-document?
            [:div.modal.is-active
             [:div.modal-background]
             [:div.modal-content
              [:div.card
               [:div.card-header [:p.card-header-title
                                  (:fulltext/document-title @document-data) "，"
                                  (:fulltext/document-author @document-data) "，"
                                  (:fulltext/document-year @document-data) "（"
                                  (:fulltext/document-genre @document-data) "）"]]
               [:div.card-content (:fulltext/document-text @document-data)]]]
             [:button.modal-close
              {:on-click #(dispatch [:toggle/fulltext-document-show])}]])])))))
