(ns gym-scale.events
  (:require [re-frame.core :refer [reg-event-db reg-event-fx reg-fx dispatch] :as re-frame]
            [gym-scale.db :as db]
            [gym-scale.fxs.ble]
            [gym-scale.fxs.sqlite]
            [expound.alpha :as expound]
            [clojure.spec.alpha :as s]))

(defn check-and-throw
  "Throws an exception if `db` doesn't match the Spec `a-spec`."
  [a-spec db]
  (when-not (s/valid? a-spec db)
    (throw (js/Error. (str "spec check failed: " (expound/expound-str a-spec db))))))

(def sc (re-frame/after (partial check-and-throw ::db/db)))

(defn init [_ _]
  {:db db/initial-db
   :sqlite/open {:db-name "GymScale"
                 :db-version "1.0"
                 :db-display-name "SQLite gym scale database"
                 :db-size 200000}})

(reg-event-fx :app/init [sc] init)

(defn open-connection [cofxs _]
  {:ble/start nil})

(reg-event-fx :scale/open-connection [sc] open-connection)

(defn close-connection [cofxs _]
  {:serial/close-connection nil})

(reg-event-fx :scale/close-connection [sc] close-connection)

(defn on-weight-change [db [_ w]]
  (assoc db :scale/last-weight w))

(reg-event-db :scale/on-weight-change [] on-weight-change)

(defn connected [db _]
  (assoc db :scale/connected? true))

(reg-event-db :scale/connected [sc] connected)

(defn disconnected [db _]
  (assoc db :scale/connected? false))

(reg-event-db :scale/disconnected [sc] disconnected)

(defn db-opened [db _]
   (js/console.log "DB opened")
  db)

(reg-event-db :sqlite-db/opened [sc] db-opened)

(defn db-error [db [_ err]]
   (js/console.error err)
  db)

(reg-event-db :sqlite-db/error [sc] db-error)

(defn users-loaded [db [_ all-users]]
   (js/console.log "All users" (clj->js all-users))
  db)

(reg-event-db :sqlite-db/users-loaded [sc] users-loaded)

(defn load-users [_ _]
  {:sqlite/execute-sql {:query "SELECT * from users;"
                        :params-vec []
                        :succ-ev [:db/users-loaded]
                        :err-ev  [:db/error]}})

(reg-event-fx :sqlite-db/load-all-users [sc] load-users)
