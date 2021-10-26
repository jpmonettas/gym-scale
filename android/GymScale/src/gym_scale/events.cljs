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
    (= :logo (:screen/current db)) (assoc :dispatch [:screen/switch :user-select-1])))

(reg-event-fx :scale/on-weight-change [] on-weight-change)

(defn screen-switch [{:keys [db]} [_ screen]]
  (let [db' (assoc db :screen/current screen)]
    (case screen
     :logo          {:db db'}
     :user-select-1 {:db db'
                     :dispatch-n [[:sqlite-db/load-all-users]]}
     :user-select-2 {:db db'}
     :user-check    {:db db'})))

(reg-event-fx :screen/switch [sc] screen-switch)

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
  (assoc db :gym/users (->> all-users
                            (map (fn [u] [(:user/id u) u]))
                            (into {}))))

(reg-event-db :sqlite-db/users-loaded [sc] users-loaded)

(defn load-users [_ _]
  {:sqlite/execute-sql {:honey-query {:select [:user/id :user/name] :from [:users]}
                        :succ-ev [:sqlite-db/users-loaded]
                        :err-ev  [:sqlite-db/error]}})

(reg-event-fx :sqlite-db/load-all-users [sc] load-users)
