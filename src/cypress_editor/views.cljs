(ns cypress-editor.views
  (:require
   [goog.string :as gstring]
   [goog.string.format]
   [re-frame.core :refer [subscribe dispatch]]))

;; # Components

;; ## Stats

(defn scaled-bar
  [prob scaling-factor]
  [:svg {:width 20 :height 10}
   [:rect {:width 20 :height 10 :style {:fill "#d3d3d3"}}]
   [:rect {:width (* 20 (/ prob scaling-factor)) :height 10}]])

(defn text-bar-graph
  "Displays an ordered list of text and numeric score pairs. The input should be presorted and shortened to desired length."
  ([token-probs width]
   (text-bar-graph token-probs width false))
  ([token-probs width common-scale]
   (let [scaling-factor (or common-scale (apply max (vals token-probs)))]
     [:table.table.table-sm
      [:thead [:tr [:th "prob"] [:th "token"]]]
      [:tbody
       (for [[token prob] token-probs]
         ^{:key (gensym "text-bar-")}
         [:tr
          [:td.bar-width.text-xs-center
           (scaled-bar prob scaling-factor)]
          [:td token]])]])))

;; ## Topic models

(defn topic-model-box
  []
  (let [text-topics (subscribe [:text-topics])
        selected-topic (subscribe [:selected-topic])]
    (fn []
      (if (seq @text-topics)
        [:div.card
         #_[:div.card-header
          [:ul.nav.nav-tabs.card-header-tabs.pull-xs-left
           (mapv
            (fn [[topic-id {:keys [prob]}]] ;; TODO prob
              (println @text-topics @selected-topic topic-id prob)
              ^{:key (str "topic-header-" topic-id)}
              [:li.nav-item
               [:a
                (if (= @selected-topic topic-id)
                  {:class "nav-link active"}
                  {:class "nav-link"
                   :on-click (fn [e] (dispatch [:update-selected-topic topic-id]))})
                (scaled-bar prob 1.0)
                (str topic-id)]])
            @text-topics)]]
         [:div.card-block
          [text-bar-graph (get-in @text-topics [@selected-topic :token-probs]) 100]
          [:a.btn.btn-primary {:on-click (fn [e] (dispatch [:infer-text-topics]))}
           "Update"]]]
        [:div.card
         [:div.card-block
          [:a.btn.btn-primary {:on-click (fn [e] (dispatch [:infer-text-topics]))}
           "Infer topics"]]]))))

;; ## Basic input box

(defn input-box []
  (let [input-text (subscribe [:input-text])
        input-text-state (subscribe [:input-text-state])
        input-text-results (subscribe [:input-text-results])]
    (fn []
      (letfn [(search [query]
                (dispatch [:update-input-text-state :loading])
                (dispatch [:search-input-text query]))]
        [:div.card
         [:div.card-header
          [:ul.nav.nav-tabs.card-header-tabs.pull-xs-left
           [:li.nav-item "Search"]]]
         [:div.card-block
          {:class (str "form-group label-floating "
                       (case input-text-results
                         :success "has-success"
                         :failure "has-error"
                         nil))}
          [:label.control-label
           (case input-text-results
             :success "Search successful"
             :failure "Search failed"
             nil)]
          [:input.form-control
           {:type "text"
            ;;:value @input-text
            :placeholder "Input..."
            :on-change (fn [e]
                         (let [current-text (.. e -target -value)]
                           (dispatch [:set-input-text current-text])))
            :on-key-press (fn [e]
                            (when (== (.-charCode e) 13)
                              (search @input-text)))}]
          [:button.btn.btn-primary.btn-sm
           {:type "button"
            :id "search-button"
            :on-click (fn [e] (search @input-text))}
           "Search"]]
         (if @input-text-results
           [:div
            [:p (pr-str @input-text-results)]])]))))

;; ## Basic textarea box

(defn textarea-box []
  [:div.card
   [:div.card-header
    "作文欄"
    [:button.btn.btn-primary.btn-sm.pull-xs-right
     {:on-click
      (fn [e]
        (dispatch [:analyze-errors]))}
     [:i.material-icons "spellcheck"] #_" Error check"]]
   [:div.card-body
    [:textarea.form-control
     {:rows 5
      :placeholder "作文を入力してください..."
      :on-change (fn [e]
                   (let [text (.. e -target -value)]
                     (dispatch [:update-user-text text])))}]]])

;; ## Textual analysis

(defn text-analysis-box
  []
  (let [user-text (subscribe [:user-text])
        error-data (subscribe [:error-data])]
    (fn []
      (let [errors (:results @error-data)
            tokens (:parsed_tokens @error-data)]
        [:div.card
         [:div.card-header "分析結果"]
         [:div.card-body
          (into [:ruby]
                (reduce
                 (fn [a {:keys [orth pos_1]}]
                   (conj a orth [:rt pos_1] " "))
                 []
                 tokens))
          (pr-str errors)]]))))

;; ## Draft.js editor integration

(comment
  (defn draftjs []
    (Draft/editor
     {:editorState @editor-state-atom
      :onChange    (fn [new-state]
                     (reset! editor-state-atom new-state)
                     (.forceUpdate @wrapper-state))})))

;; ## Collocation list w/ bar graph

(defn navbar []
  (let [connection-status (subscribe [:connection-status])]
    (fn []
      [:nav {:class (case @connection-status
                      :online  "navbar navbar-fixed-top navbar-light bg-faded navbar-default"
                      :offline "navbar navbar-fixed-top navbar-light bg-faded navbar-danger"
                      nil)
             :role "navigation"}
       #_[:button.navbar-toggle {:type "button" :data-toggle "collapse"
                               :data-target "#navbar-collapse"}
        [:span.sr-only "Toggle navigation"]
        [:span.icon-bar] [:span.icon-bar] [:span.icon-bar]]
       [:a.navbar-brand {:href "#"} "Cypress Editor"]

       [:ul.nav.navbar-nav
        [:li.nav-item.divider.pull-xs-right]

        [:li.nav-item.pull-xs-right
         [:a.nav-link
          [:i.material-icons
           (case @connection-status ;; TODO move to websocket status
             :online  "cloud"
             :loading "cloud_download"
             :offline "cloud_off"
             nil)]]]

        [:li.nav-item.pull-xs-right [:a.nav-link [:i.fa.fa-repeat] #_" Redo"]]
        [:li.nav-item.pull-xs-right [:a.nav-link [:i.fa.fa-undo] #_" Undo"]]

        #_[:li.active [:a {:href "#"} "Link"]]
        #_[:li.dropdown
         [:a.dropdown-toggle {:href "#" :data-toggle "dropdown"} "Profile" [:b.caret]]
         [:ul.dropdown-menu
          [:li [:a {:href "#"} "Action"]]
          [:li.divider]]]]
       #_[:div.collapse.navbar-toggleable-xs {:id "navbar-collapse"}
        ]])))

(defn footer []
  (let [connection-status (subscribe [:connection-status])]
    (fn []
      [:nav {:class (case @connection-status
                      :online  "navbar bg-faded navbar-fixed-bottom navbar-default"
                      :offline "navbar bg-faded navbar-fixed-bottom navbar-danger"
                      nil)}
       [:div.navbar-header
        [:a.navbar-brand {:href "#"} "Stats"]
        [:span.navbar-brand (gstring/format "Words: %d, Sentences: %d, Paragraphs: %d"
                                            0 0 0)]]])))

(defn interface []
  [:div.container-fluid
   [navbar]
   [:div.row.editor-interface
    [:div.col-xs-8.group
     [textarea-box]
     [text-analysis-box]]
    [:div.col-xs-4
     [topic-model-box]
     [input-box]]]
   [footer]])
