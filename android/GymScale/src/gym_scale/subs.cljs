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
   (or (get-in db [:state/current :screen/current]) :logo)))

(reg-sub
 :gym/users-search
 (fn [db _]
   (get-in db [:state/current :gym/users-search])))

(reg-sub
 :gym/users-search-initials
 :<- [:gym/users-search]
 (fn [users _]
   (->> users
        (map (fn [u]
               (-> u :user/last-name first str)))
        (into #{}))))

(reg-sub
 :gym/selected-user-data
 (fn [db _]
   (get-in db [:state/current :gym/selected-user-data])))

(reg-sub
 :gym/checked-in?
 (fn [db _]
   (:gym/checked-in? db)))

(reg-sub
 :clock/date-time-str
 (fn [db _]
   (:clock/date-time-str db)))

(reg-sub
 :gym/all-users
 (fn [db _]
   (get-in db [:state/current :gym/all-users])))
