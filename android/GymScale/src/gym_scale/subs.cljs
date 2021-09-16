(ns gym-scale.subs
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub
 :scale/last-weight
 (fn [db _]
   (:scale/last-weight db)))

(reg-sub
 :scale/status
 (fn [db _]
   (:scale/status db)))

(reg-sub
 :serial/connected?
 (fn [db _]
   (:serial/connected? db)))

