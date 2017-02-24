(ns cypress-editor.subs
  (:require [re-frame.core :refer [reg-sub]]))

;; User API

(reg-sub
 :user/text
 (fn [db _]
   (:user/text db)))

(reg-sub
 :user/unit-type
 (fn [db _]
   (:user/unit-type db)))

(reg-sub
 :user/features
 (fn [db _]
   (:user/features db)))

(reg-sub
 :user/token
 (fn [db _]
   (:user/token db)))

(reg-sub
 :user/extra-token
 (fn [db _]
   (:user/extra-token db)))

(reg-sub
 :user/genre
 (fn [db _]
   (:user/genre db)))

(reg-sub
 :user/limit
 (fn [db _]
   (:user/limit db)))

(reg-sub
 :user/html
 (fn [db _]
   (:user/html db)))

(reg-sub
 :user/norm
 (fn [db _]
   (:user/norm db)))

(reg-sub
 :user/collocation
 (fn [db _]
   (:user/collocation db)))

(reg-sub
 :user/collocation-tree
 (fn [db _]
   (:user/collocation-tree db)))

(reg-sub
 :user/selected-topic
 (fn [db _]
   (:user/selected-topic db)))

;; Server status API

(reg-sub
 :sente/connection-status
 (fn [db _]
   (:sente/connection-status db)))

;; Backend API

(reg-sub
 :sources/genre
 (fn [db _]
   (:sources/genre db)))

(reg-sub
 :sentences/collocations
 (fn [db _]
   (:sentences/collocations db)))

(reg-sub
 :sentences/tokens
 (fn [db _]
   (:sentences/tokens db)))

(reg-sub
 :tokens/tree
 (fn [db _]
   (:tokens/tree db)))

(reg-sub
 :tokens/similarity
 (fn [db _]
   (:tokens/similarity db)))

(reg-sub
 :tokens/nearest-tokens
 (fn [db _]
   (:tokens/nearest-tokens db)))

(reg-sub
 :tokens/similarity-with-accuracy
 (fn [db _]
   (:tokens/similarity-with-accuracy db)))

(reg-sub
 :tokens/tsne
 (fn [db _]
   (:tokens/tsne db)))

(reg-sub
 :collocations/collocations
 (fn [db _]
   (:collocations/collocations db)))

(reg-sub
 :collocations/tree
 (fn [db _]
   (:collocations/tree db)))

(reg-sub
 :suggestions/tokens
 (fn [db _]
   (:suggestions/tokens db)))

(reg-sub
 :errors/register
 (fn [db _]
   (:errors/register db)))

(reg-sub
 :topics/infer
 (fn [db _]
   (:topics/infer db)))

;; Fulltext API

(reg-sub
 :fulltext/query
 (fn [db _]
   (:fulltext/query db)))

(reg-sub
 :sentences/fulltext
 (fn [db _]
   (:sentences/fulltext db)))

(reg-sub
 :fulltext/genre-column
 (fn [db _]
   (:fulltext/genre-column db)))

(reg-sub
 :fulltext/title-column
 (fn [db _]
   (:fulltext/title-column db)))

(reg-sub
 :fulltext/author-column
 (fn [db _]
   (:fulltext/author-column db)))

(reg-sub
 :fulltext/year-column
 (fn [db _]
   (:fulltext/year-column db)))

(reg-sub
 :fulltext/state
 (fn [db _]
   (:fulltext/state db)))
