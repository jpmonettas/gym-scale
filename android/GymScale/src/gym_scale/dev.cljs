(ns gym-scale.dev
  (:require [re-frame.core :refer [dispatch]]
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
                                  (.executeSql tx "INSERT INTO users (user_SLASH_id,user_SLASH_name) VALUES (38755324,\"Juan\");" [] success-handler error-handler)
                                  (.executeSql tx "INSERT INTO users (user_SLASH_id,user_SLASH_name) VALUES (38755334,\"Jose Pedro\");" [] success-handler error-handler)
                                  (.executeSql tx "INSERT INTO users (user_SLASH_id,user_SLASH_name) VALUES (38755330,\"Cote\");" [] success-handler error-handler)
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

(comment


  @app-db

  ;; connected, on logo screen
  (do
    (reset! app-db {:scale/last-weight 0
                    :scale/connected? true
                    :state/current {:screen/current :logo}
                    :state/prev-stack ()})
    (dispatch [:scale/on-weight-change 76000]))

    (dispatch [:scale/on-weight-change 10])

  ;; advanced to user-select-1
  (reset! app-db
          {:scale/last-weight 76000,
           :scale/connected? true,
           :state/current {:screen/current :user-select-1,
                           :gym/users-search [#:user{:name "Juan", :id 38755324}
                                              #:user{:name "Cote", :id 38755330}
                                              #:user{:name "Jose Pedro", :id 38755334}]},
           :state/prev-stack ()})

  ;; advanced to user-select-2 under letter J
  (reset! app-db
          {:scale/last-weight 76000,
           :scale/connected? true,
           :state/current
           {:screen/current :user-select-2,
            :gym/users-search [#:user{:name "Juan", :id 38755324}
                               #:user{:name "Jose Pedro", :id 38755334}]},
           :state/prev-stack '({:screen/current :user-select-1,
                                :gym/users-search [#:user{:name "Juan", :id 38755324}
                                                   #:user{:name "Cote", :id 38755330}
                                                   #:user{:name "Jose Pedro", :id 38755334}]})})

  ;; advanced to user-check
  (reset! app-db
          {:scale/last-weight 76000,
           :scale/connected? true,
           :state/current {:screen/current :user-check,
                           :gym/users-search [#:user{:name "Juan", :id 38755324}
                                              #:user{:name "Jose Pedro", :id 38755334}],
                           :gym/selected-user-data #:user{:name "Juan", :id 38755324}},
           :state/prev-stack '({:screen/current :user-select-2,
                                :gym/users-search [#:user{:name "Juan", :id 38755324}
                                                   #:user{:name "Jose Pedro", :id 38755334}]}
                               {:screen/current :user-select-1,
                                :gym/users-search [#:user{:name "Juan", :id 38755324}
                                                   #:user{:name "Cote", :id 38755330}
                                                   #:user{:name "Jose Pedro", :id 38755334}]})})


  (query-and-print "select * from users;")
  (query-and-print "select * from checkins;")

  (drop-tables)

  (populate-db)

  )
