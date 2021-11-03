(ns gym-scale.dev
  (:require [re-frame.core :refer [dispatch subscribe]]
            [re-frame.db :refer [app-db]]
            [gym-scale.fxs.sqlite :as sqlite]
            [gym-scale.utils :refer [update-keys]]
            [clojure.pprint :as pp]))

(defn drop-tables []
  (.transaction @sqlite/db* (fn [tx]
                              (.executeSql tx "DROP TABLE IF EXISTS version;")
                              (.executeSql tx "DROP TABLE IF EXISTS users;")
                              (.executeSql tx "DROP TABLE IF EXISTS checkins;")
                              (js/console.info "All tables dorpped."))
                #() #()))

(defn populate-db []
  ;; Populate db
  (.transaction @sqlite/db* (fn [tx]
                              (let [success-handler (fn [] (js/console.info "Dummy data record added successfully"))
                                    error-handler (fn [err] (js/console.err "Problem inserting dummy data" err))]
                                (try
                                  (.executeSql tx "INSERT INTO users (user_SLASH_id,user_SLASH_first_name,user_SLASH_last_name,user_SLASH_phone,user_SLASH_birthday) VALUES (38755324,\"Juan Pedro\",\"Monetta Sanchez\", \"098164800\", \"1983-10-20\");" [] success-handler error-handler)
                                  (.executeSql tx "INSERT INTO users (user_SLASH_id,user_SLASH_first_name,user_SLASH_last_name,user_SLASH_phone,user_SLASH_birthday) VALUES (38755330,\"Jose Ignacio\",\"Monetta Sanchez\", \"098164800\", \"1986-09-25\");" [] success-handler error-handler)
                                  (catch js/Object e (js/console.error "ERROR populate-with-dummy-data" e))))
                              (js/console.info "Dummy data added"))
                #(js/console.error "Problem populating db with dummy data" %1)
                #(js/console.info "Dummy data populated successfully")))

(defn query-and-print [query]
  (.executeSql @sqlite/db*
               query
               (clj->js [])
               (fn on-success [result-set]
                 (->> (range (-> result-set .-rows .-length))
                      (mapv (fn [i]
                              (let [res-map (-> result-set
                                                .-rows
                                                (.item i)
                                                js->clj)]
                                (update-keys res-map (comp keyword demunge)))))
                      pp/print-table))
               (fn on-error [err]
                 (js/console.error "" err))))

(def connected-logo-state {:scale/last-weight 0
                           :scale/connected? true
                           :state/current {:screen/current :logo}
                           :state/prev-stack ()})

(def user-select-initial-state
  {:scale/last-weight 76000,
   :scale/connected? true,
   :state/current
   {:screen/current :user-select-1,
    :gym/users-search [#:user{:phone "098164800", :last-name "Monetta Sanchez", :first-name "Juan Pedro", :birthday "1983-10-20", :id 38755324}
                       #:user{:phone "098164800", :last-name "Monetta Sanchez", :first-name "Jose Ignacio", :birthday "1986-09-25", :id 38755330}]},
   :state/prev-stack ()})

(def user-select-name-state
  {:scale/last-weight 76000,
   :scale/connected? true,
   :state/current
   {:screen/current :user-select-2,
    :gym/users-search [#:user{:phone "098164800", :last-name "Monetta Sanchez", :first-name "Juan Pedro", :birthday "1983-10-20", :id 38755324}
                       #:user{:phone "098164800", :last-name "Monetta Sanchez", :first-name "Jose Ignacio", :birthday "1986-09-25", :id 38755330}]},
   :state/prev-stack '({:screen/current :user-select-1,
                        :gym/users-search [#:user{:phone "098164800", :last-name "Monetta Sanchez", :first-name "Juan Pedro", :birthday "1983-10-20", :id 38755324}
                                           #:user{:phone "098164800", :last-name "Monetta Sanchez", :first-name "Jose Ignacio", :birthday "1986-09-25", :id 38755330}]})})

(def user-check-state
  {:scale/last-weight 76000,
   :scale/connected? true,
   :state/current
   {:screen/current :user-check, :gym/users-search
    [#:user{:phone "098164800", :last-name "Monetta Sanchez", :first-name "Juan Pedro", :birthday "1983-10-20", :id 38755324}
     #:user{:phone "098164800", :last-name "Monetta Sanchez", :first-name "Jose Ignacio", :birthday "1986-09-25", :id 38755330}],
    :gym/selected-user-data #:user{:phone "098164800", :last-name "Monetta Sanchez", :first-name "Juan Pedro", :birthday "1983-10-20", :id 38755324}},
   :state/prev-stack '({:screen/current :user-select-2,
                        :gym/users-search [#:user{:phone "098164800", :last-name "Monetta Sanchez", :first-name "Juan Pedro", :birthday "1983-10-20", :id 38755324}
                                           #:user{:phone "098164800", :last-name "Monetta Sanchez", :first-name "Jose Ignacio", :birthday "1986-09-25", :id 38755330}]}
                       {:screen/current :user-select-1,
                        :gym/users-search [#:user{:phone "098164800", :last-name "Monetta Sanchez", :first-name "Juan Pedro", :birthday "1983-10-20", :id 38755324}
                                           #:user{:phone "098164800", :last-name "Monetta Sanchez", :first-name "Jose Ignacio", :birthday "1986-09-25", :id 38755330}]})})

(defn con
  "Sumulate connecting to BT scale"
  []
  (reset! app-db connected-logo-state))

(defn sup
  "Simulate jumping up on the scale"
  []
  (dispatch [:scale/on-weight-change 76000]))

(defn sdown
  "Simulate jumping off the scale"
  []
  (dispatch [:scale/on-weight-change 10]))

(comment


  @app-db


  ;; advanced to user-select-1
  (reset! app-db user-select-initial-state)

  ;; advanced to user-select-2 under letter M
  (reset! app-db user-select-name-state)

  ;; advanced to user-check
  (reset! app-db user-check-state)

  (query-and-print "select * from users;")
  (query-and-print "select * from checkins;")
  (query-and-print "SELECT sqlite_version();")

  (drop-tables)

  (populate-db)

  )
