(ns cypress-editor.subs
  (:require [re-frame.core :refer [reg-sub subscribe]]))

(reg-sub
 :input-text
 (fn [db _]
   (:input-text db)))

(reg-sub
 :input-text-state
 (fn [db _]
   (:input-text-state db)))

(reg-sub
 :input-text-results
 (fn [db _]
   (:input-text-results db)))

(reg-sub
 :user-text
 (fn [db _]
   (:user-text db)))

(reg-sub
 :error-data
 (fn [db _]
   (:error-data db)))

(reg-sub
 :text-topics
 (fn [db _]
   (:text-topics db)))

(reg-sub
 :selected-topic
 (fn [db _]
   (:selected-topic db)))

(reg-sub
 :connection-status
 (fn [db _]
   (:connection-status db)))
