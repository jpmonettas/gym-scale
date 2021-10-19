(ns gym-scale.fxs.sqlite
  (:require [react-native-sqlite-storage :as SQLite]
            [re-frame.core :refer [reg-fx dispatch]]))

(def db* (atom nil))

(defn ensure-tables [tx]
  (.executeSql tx
               "CREATE TABLE IF NOT EXISTS version(version_id INTEGER PRIMARY KEY NOT NULL);"
               []
               #(js/console.log "version table created")
               #(js/console.error "Error creating version table" %))
  (.executeSql tx
               "CREATE TABLE IF NOT EXISTS users(user_id INTEGER PRIMARY KEY NOT NULL,
                                                 user_name VARCHAR(30));"
               []
               #(js/console.log "users table created")
               #(js/console.error "Error creating users table" %)))

(defn drop-all-tables [tx]
  (.executeSql tx "DROP TABLE IF EXISTS version;")
  (.executeSql tx "DROP TABLE IF EXISTS users;")
  (js/console.info "All tables dorpped."))

(defn populate-with-dummy-data [tx]
  (try
    (.executeSql tx "INSERT INTO users (user_name) VALUES (\"Juan\");" [])
    (.executeSql tx "INSERT INTO users (user_name) VALUES (\"Pedro\");" [])
    (catch js/Object e (js/console.error "ERROR populate-with-dummy-data" e)))
  (js/console.info "Dummy data added"))

(reg-fx
 :sqlite/open
 (fn [{:keys [db-name db-version db-display-name db-size]}]

   (let [db-error-cb (fn error-callback [err]
                       (dispatch [:db/error err]))
         db-ready-cb (fn [])
         db (.openDatabase SQLite
                           db-name
                           db-version
                           db-display-name
                           db-size
                           (fn open-callback [])
                           db-error-cb)]
     (reset! db* db)

     ;; check if db is already created or if it is first time
     (.executeSql db
                  "SELECT 1 FROM version LIMIT 1"
                  []
                  (fn on-success []
                    (js/console.info "Database OK"))
                  (fn on-error [err]
                    (js/console.error "Database doesn't exist, creating..." err)
                    (.transaction db ensure-tables db-error-cb db-ready-cb))))))

(reg-fx
 :sqlite/execute-sql
 (fn [{:keys [query params-vec succ-ev err-ev]}]
   (let [[succ-ev-key & succ-ev-args] succ-ev
         [err-ev-key & err-ev-args]   err-ev]
     (.transaction @db*
                   (fn [tx]

                     (try
                       (.executeSql tx
                                    query
                                    params-vec
                                    (fn success [_ result-set]
                                      ;; go over the result set and create a vector of rows with clj maps
                                      (let [rows-vec (->> (range (-> result-set .-rows .-length))
                                                          (mapv (fn [i] (-> result-set
                                                                            .-rows
                                                                            (.item i)
                                                                            js->clj))))]
                                        (dispatch (into [succ-ev-key rows-vec] succ-ev-args))))
                                    (fn error [err] (js/console.err "Error executing query")))
                       (catch js/Object e (js/console.error "ERROR :sqlite/execute-sql" e))))))))

(comment

  (.transaction @db* drop-all-tables #() #())
  (.transaction @db* populate-with-dummy-data #() #())

  (dispatch [:db/load-all-users])
  )
