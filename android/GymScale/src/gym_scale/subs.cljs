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

(reg-sub
 :screen/current
 (fn [db _]
   (or (:screen/current db) :logo)))

(reg-sub
 :gym/users
 (fn [db _]
   (:gym/users db)))

(reg-sub
 :gym/users-initials
 :<- [:gym/users]
 (fn [users _]
   (->> (vals users)
        (map (fn [u]
               (-> u :user/name first str)))
        (into #{}))))
