(ns gym-scale.events
  (:require [re-frame.core :refer [reg-event-db reg-event-fx reg-fx dispatch] :as re-frame]
            [gym-scale.db :as db]
            [gym-scale.fxs.ble]
            [gym-scale.fxs.sqlite]
            [gym-scale.fxs.ticker]
            [expound.alpha :as expound]
            [clojure.spec.alpha :as s]
            [honeysql.core :as sql]
            [honeysql.helpers :as sqlh]
            [goog.string :as gstr]))

(defn check-and-throw
  "Throws an exception if `db` doesn't match the Spec `a-spec`."
  [a-spec db]
  (when-not (s/valid? a-spec db)
    (throw (js/Error. (str "spec check failed: " (expound/expound-str a-spec db))))))

(defn push-to-back-stack [context]
  (let [{:keys [db event]} (:coeffects context)
        new-db (update db :state/prev-stack conj (:state/current db))]
    (assoc-in context [:coeffects :db] new-db)))

(def sc (re-frame/after (partial check-and-throw ::db/db)))
(def backable (re-frame/->interceptor
               :id :push-to-back-stack
               :before push-to-back-stack))

(defn init [_ _]
  {:db db/initial-db
   :ticker/start 5000
   :sqlite/open {:db-name "GymScale"
                 :db-version "1.0"
                 :db-display-name "SQLite gym scale database"
                 :db-size 200000}})

(defn open-connection [cofxs _]
  {:ble/start nil})

(defn close-connection [cofxs _]
  {:serial/close-connection nil})

(defn current-screen [db]
  (get-in db [:state/current :screen/current]))

(defn set-current-screen [db screen]
  (assoc-in db [:state/current :screen/current] screen))

(defn admin-mode? [db]
  (get-in db [:state/current :gym/admin-mode?]))

(defn on-weight-change [{:keys [db]} [_ w]]
  (cond-> {:db (assoc db :scale/last-weight w)}

    (and (= :logo (current-screen db))
         (not (admin-mode? db))
         (> w 5000)) ;; we need more than 5kgs on the scale to activate the device
    (assoc :dispatch [:sqlite-db/load-users nil [:screen/switch-to-user-select-1]])

    (and (< w 5000) (not (admin-mode? db))) ;; if the scale goes under 5kgs asume it is empty so reset UI state unless it is admin mode
    (assoc :db (assoc db/initial-db
                      :scale/connected?  (:scale/connected? db)
                      :scale/last-weight w))))

(defn switch-to-logo [{:keys [db]} [_]]
  {:db (set-current-screen db :logo)})

(defn switch-to-user-select-1 [{:keys [db]} [_ all-users]]
  {:db (-> db
           (set-current-screen :user-select-1)
           (assoc-in [:state/current :gym/users-search] all-users))})

(defn switch-to-user-select-2 [{:keys [db]} [_ users]]
  {:db (-> db
           (set-current-screen :user-select-2)
           (assoc-in [:state/current :gym/users-search] users))})

(defn switch-to-user-check [{:keys [db]} [_ user]]
  {:db (-> db
           (set-current-screen :user-check)
           (assoc-in [:state/current :gym/selected-user-data] user))})

(defn switch-to-admin [{:keys [db]} _]
  {:db (-> db
           (assoc-in [:state/current :gym/admin-mode?] true)
           (set-current-screen :pinpad))})

(defn switch-to-admin-menu [{:keys [db]} _]
  {:db (-> db
           (set-current-screen :admin-menu)
           ;; discard the top of the prev-stack so the back button
           ;; goes back directly to the logo screen instead of the pinpad
           (update :state/prev-stack pop))})

(defn switch-to-users-crud [{:keys [db]} [_ users]]
  (let [db' (-> db
            (set-current-screen :users-crud)
            (assoc-in [:state/current :gym/all-users] (->> users
                                                           (map (fn [u] [(:user/id u) u]))
                                                           (into {}))))]

    {:db db'}))

(defn switch-to-user-upsert [{:keys [db]} [_ user]]
  {:db (-> db
           (set-current-screen :user-upsert)
           (assoc-in [:state/current :gym/editing-user] user))})

(defn screen-back [{:keys [db]} _]
  (let [{:keys [state/current state/prev-stack]} db]
    {:db (-> db
             (assoc :state/current (peek prev-stack))
             (update :state/prev-stack pop))}))

(defn connected [db _]
  (assoc db :scale/connected? true))

(defn disconnected [db _]
  (assoc db :scale/connected? false))

(defn db-opened [db _]
   (js/console.log "DB opened")
  db)

(defn db-error [db [_ err]]
   (js/console.error err)
  db)

(defn load-users [_ [_ user-name-prefix success-ev]]
  {:sqlite/execute-sql {:honey-query (cond-> {:select [:*]
                                              :from [:users]}
                                       user-name-prefix (sqlh/merge-where [:like :user/last-name (str user-name-prefix "%")]))
                        :succ-ev success-ev
                        :err-ev  [:sqlite-db/error]}})

(defn check-in-success [{:keys [db]} _]
  {:db (assoc db :gym/checked-in? true)})

(defn now-as-data-str []
  (let  [d (js/Date.)]
    (str (.getFullYear d) "-" (inc (.getMonth d)) "-" (.getDate d))))

(defn user-check-in [{:keys [db]} [_ user-id]]
  {:sqlite/execute-sql {:honey-query {:insert-into :checkins
                                      :values [{:user/id user-id
                                                :date (now-as-data-str)
                                                :user/weight (:scale/last-weight db)}]}
                        :succ-ev [:sqlite-db/check-in-success]
                        :err-ev [:sqlite-db/error]}})

(defn user-upsert-success [cofxs [_ _ user]]
  ;; @HACKY: go back without dispatching so we skip the `backable` interceptor
  (let [{:keys [db]} (screen-back cofxs nil)]
    {:db (assoc-in db [:state/current :gym/all-users (:user/id user)] user)}))

(defn user-upsert [{:keys [db]} [_ user]]
  (let [user-map {:user/id (:user/id user)
                  :user/first-name (:user/first-name user)
                  :user/last-name (:user/last-name user)
                  :user/phone (:user/phone user)
                  :user/birthday (:user/birthday user)}]
    {:sqlite/execute-sql {:honey-query (if (:editing? user)
                                         {:update :users
                                          :set user-map
                                          :where [:= :user/id (:user/id user)]}

                                         {:insert-into :users
                                          :values [user-map]})
                          :succ-ev [:sqlite-db/user-upsert-success user]
                          :err-ev [:sqlite-db/error]}}))

(defn user-delete-success [{:keys [db]} [_ _ user]]
  {:db (update-in db [:state/current :gym/all-users] dissoc (:user/id user))})

(defn user-delete [{:keys [db]} [_ user]]
  {:sqlite/execute-sql {:honey-query {:delete-from :users
                                      :where [:= :user/id (:user/id user)]}
                        :succ-ev [:sqlite-db/user-delete-success user]
                        :err-ev [:sqlite-db/error]}})

(defn clock-tick [db [_]]
  (let [d (js/Date.)]
    (assoc db :clock/date-time-str
           (str (.getDate d) "/" (inc (.getMonth d)) " " (.getHours d) ":" (.getMinutes d)))))

(reg-event-fx :app/init                       [sc]          init)
(reg-event-fx :scale/open-connection          [sc]          open-connection)
(reg-event-fx :scale/close-connection         [sc]          close-connection)
(reg-event-fx :scale/on-weight-change         []            on-weight-change)
(reg-event-fx :screen/switch-to-logo          [sc]          switch-to-logo)
(reg-event-fx :screen/switch-to-user-select-1 [sc]          switch-to-user-select-1)
(reg-event-fx :screen/switch-to-user-select-2 [backable sc] switch-to-user-select-2)
(reg-event-fx :screen/switch-to-user-check    [backable sc] switch-to-user-check)
(reg-event-fx :screen/switch-to-admin         [backable sc] switch-to-admin)
(reg-event-fx :screen/switch-to-admin-menu    [backable sc] switch-to-admin-menu)
(reg-event-fx :screen/switch-to-users-crud    [backable sc] switch-to-users-crud)
(reg-event-fx :screen/switch-to-user-upsert   [backable sc] switch-to-user-upsert)
(reg-event-fx :screen/back                    [sc]          screen-back)
(reg-event-db :scale/connected                [sc]          connected)
(reg-event-db :scale/disconnected             [sc]          disconnected)
(reg-event-db :sqlite-db/opened               [sc]          db-opened)
(reg-event-db :sqlite-db/error                [sc]          db-error)
(reg-event-fx :sqlite-db/load-users           [sc]          load-users)
(reg-event-fx :sqlite-db/check-in-success     [sc]          check-in-success)
(reg-event-fx :sqlite-db/user-upsert-success  [sc]          user-upsert-success)
(reg-event-fx :sqlite-db/user-delete-success  [sc]          user-delete-success)
(reg-event-fx :user/check-in                  [sc]          user-check-in)
(reg-event-fx :user/upsert                    [sc]          user-upsert)
(reg-event-fx :user/delete                    [sc]          user-delete)
(reg-event-db :ticker/tick                    []            clock-tick)
