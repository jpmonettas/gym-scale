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
 :gym/users-search
 (fn [db _]
   (:gym/users-search db)))

(reg-sub
 :gym/users-search-initials
 :<- [:gym/users-search]
 (fn [users _]
   (->> users
        (map (fn [u]
               (-> u :user/name first str)))
        (into #{}))))

(reg-sub
 :gym/selected-user-data
 (fn [db _]
   (:gym/selected-user-data db)))

(reg-sub
 :gym/checked-in?
 (fn [db _]
   (:gym/checked-in? db)))
