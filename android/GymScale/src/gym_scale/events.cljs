(ns gym-scale.events
  (:require [re-frame.core :refer [reg-event-db reg-event-fx reg-fx dispatch]]
            [gym-scale.db :as db]
            [gym-scale.fxs.ble]
            [gym-scale.fxs.sqlite]))

(reg-event-fx
 :app/init
 (fn [_ _]
   {:db db/initial-db
    :sqlite/open {:db-name "GymScale"
                  :db-version "1.0"
                  :db-display-name "SQLite gym scale database"
                  :db-size 200000}}))

(reg-event-fx
 :scale/open-connection
 (fn [cofxs _]
   {:ble/start nil}))

(reg-event-fx
 :scale/close-connection
 (fn [cofxs _]
   {:serial/close-connection nil}))

(reg-event-db
 :scale/on-weight-change
 (fn [db [_ w]]
   (assoc db :scale/last-weight w)))

(reg-event-db
 :scale/connected
 (fn [db _]
   (assoc db :scale/connected? true)))

(reg-event-db
 :scale/disconnected
 (fn [db _]
   (assoc db :scale/connected? false)))

(reg-event-db
 :sqlite-db/opened
 (fn [db _]
   (js/console.log "DB opened")
   db))

(reg-event-db
 :sqlite-db/error
 (fn [db [_ err]]
   (js/console.error err)
   db))

(reg-event-db
 :sqlite-db/users-loaded
 (fn [db [_ all-users]]
   (js/console.log "All users" (clj->js all-users))
   db))

(reg-event-fx
 :sqlite-db/load-all-users
 (fn [_ _]
   {:sqlite/execute-sql {:query "SELECT * from users;"
                         :params-vec []
                         :succ-ev [:db/users-loaded]
                         :err-ev  [:db/error]}}))
