(ns gym-scale.subs
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub
 :scale/last-weight
 (fn [db _]
   (:scale/last-weight db)))

(reg-sub
 :scale/connected?
 (fn [db _]
   (:scale/connected? db)))
