(ns gym-scale.events
  (:require [re-frame.core :refer [reg-event-db reg-event-fx reg-fx dispatch] :as re-frame]
            [gym-scale.db :as db]
            [gym-scale.fxs.ble]
            [gym-scale.fxs.sqlite]
            [expound.alpha :as expound]
            [clojure.spec.alpha :as s]
            [honeysql.core :as sql]
            [honeysql.helpers :as sqlh]))

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
                 :db-size 200000}
   ;; TODO: remove
   :dispatch [:scale/on-weight-change 76000]})

(reg-event-fx :app/init [sc] init)

(defn open-connection [cofxs _]
  {:ble/start nil})

(reg-event-fx :scale/open-connection [sc] open-connection)

(defn close-connection [cofxs _]
  {:serial/close-connection nil})

(reg-event-fx :scale/close-connection [sc] close-connection)

(defn on-weight-change [{:keys [db]} [_ w]]
  (cond-> {:db (assoc db :scale/last-weight w)}

    (and (= :logo (:screen/current db))
         (> w 5000)) ;; we need more than 5kgs on the scale to activate the device
    (assoc :dispatch [:screen/switch-to-user-select-1 nil])

    (< w 5000) ;; if the scale goes under 5kgs asume it is empty so reset UI state
    (assoc :db db/initial-db)))

(reg-event-fx :scale/on-weight-change [] on-weight-change)

(defn switch-to-logo [{:keys [db]} [_]]
  {:db (assoc db :screen/current :logo)})

(defn switch-to-user-select-1 [{:keys [db]} [_]]
  {:db (assoc db :screen/current :user-select-1)
   :dispatch-n [[:sqlite-db/load-all-users nil]]})

(defn switch-to-user-select-2 [{:keys [db]} [_ letter]]
  {:db (assoc db :screen/current :user-select-2)
   :dispatch-n [[:sqlite-db/load-all-users letter]]})

(defn switch-to-user-check [{:keys [db]} [_]]
  {:db (assoc db :screen/current :user-check)})

(reg-event-fx :screen/switch-to-logo [sc] switch-to-logo)
(reg-event-fx :screen/switch-to-user-select-1 [sc] switch-to-user-select-1)
(reg-event-fx :screen/switch-to-user-select-2 [sc] switch-to-user-select-2)
(reg-event-fx :screen/switch-to-user-check [sc] switch-to-user-check)

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
  (assoc db :gym/users-search (->> all-users
                                   (map (fn [u] [(:user/id u) u]))
                                   (into {}))))

(reg-event-db :sqlite-db/users-loaded [sc] users-loaded)

(defn load-users [_ [_ user-name-prefix]]
  {:sqlite/execute-sql {:honey-query (cond-> {:select [:user/id :user/name]
                                              :from [:users]}
                                       user-name-prefix (sqlh/merge-where [:like :user/name (str user-name-prefix "%")]))
                        :succ-ev [:sqlite-db/users-loaded]
                        :err-ev  [:sqlite-db/error]}})

(reg-event-fx :sqlite-db/load-all-users [sc] load-users)
