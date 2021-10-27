(ns gym-scale.dev
  (:require [re-frame.core :refer [dispatch]]
            [re-frame.db :refer [app-db]]
            [gym-scale.fxs.sqlite :as sqlite]
            [gym-scale.utils :refer [update-keys]]))

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
                      prn))
               (fn on-error [err]
                 (js/console.error "" err))))

(comment

  (dispatch [:scale/on-weight-change 76000])

  @app-db

  ;; connected, on logo screen
  (reset! app-db {:scale/last-weight 0
                  :scale/connected? true
                  :screen/current :logo})

  ;; advanced to user-select-1
  (reset! app-db {:scale/last-weight 76000,
                  :scale/connected? true,
                  :screen/current :user-select-1,
                  :gym/users-search
                  [#:user{:name "Juan", :id 38755324},
                   #:user{:name "Cote", :id 38755330},
                   #:user{:name "Jose Pedro", :id 38755334}]})

  ;; advanced to user-select-2 under letter J
  (reset! app-db {:scale/last-weight 76000,
                  :scale/connected? true,
                  :screen/current :user-select-2,
                  :gym/users-search
                  [#:user{:name "Juan", :id 38755324},
                   #:user{:name "Jose Pedro", :id 38755334}]})

  ;; advanced to user-check
  (reset! app-db {:scale/last-weight 76000,
                  :scale/connected? true,
                  :screen/current :user-check,
                  :gym/users-search
                  [#:user{:name "Juan", :id 38755324},
                   #:user{:name "Jose Pedro", :id 38755334}]
                  :gym/selected-user-data #:user{:name "Juan", :id 38755324}})


  (query-and-print "select * from users;")
  (query-and-print "select * from checkins;")


  ;; Drop all tables
  (.transaction @sqlite/db* drop-all-tables #() #())

  ;; Populate db
  (.transaction @sqlite/db* populate-with-dummy-data
                #(js/console.error "Problem populating db with dummy data" %1)
                #(js/console.info "Dummy data populated successfully"))

  )
