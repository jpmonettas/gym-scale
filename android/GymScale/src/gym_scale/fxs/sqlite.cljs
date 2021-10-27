(ns gym-scale.fxs.sqlite
  (:require [react-native-sqlite-storage :as SQLite]
            [re-frame.core :refer [reg-fx dispatch]]
            [honeysql.core :as sql]
            [honeysql.format :as sql-format]
            [gym-scale.utils :refer [update-keys]]))

(def db* (atom nil))

(defn ensure-tables [tx]
  (.executeSql tx
               "CREATE TABLE IF NOT EXISTS version(version_id INTEGER PRIMARY KEY NOT NULL);"
               []
               #(js/console.log "version table created")
               #(js/console.error "Error creating version table" %))
  (.executeSql tx
               "CREATE TABLE IF NOT EXISTS users(user_SLASH_id   INTEGER PRIMARY KEY NOT NULL,
                                                 user_SLASH_name VARCHAR(30));"
               []
               #(js/console.log "users table created")
               #(js/console.error "Error creating users table" %))
  (.executeSql tx
               "CREATE TABLE IF NOT EXISTS checkins(user_SLASH_id     INTEGER NOT NULL,
                                                    date              TEXT NOT NULL,
                                                    user_SLASH_weight INTEGER,

                                                    PRIMARY KEY(user_SLASH_id, date)
                                                   );"
               []
               #(js/console.log "users table created")
               #(js/console.error "Error creating users table" %)))

(defn drop-all-tables [tx]
  (.executeSql tx "DROP TABLE IF EXISTS version;")
  (.executeSql tx "DROP TABLE IF EXISTS users;")
  (js/console.info "All tables dorpped."))

(defn populate-with-dummy-data [tx]
  (let [success-handler (fn [] (js/console.info "Dummy data record added successfully"))
        error-handler (fn [err] (js/console.err "Problem inserting dummy data" err))]
    (try
     (.executeSql tx "INSERT INTO users (user_SLASH_id,user_SLASH_name) VALUES (38755324,\"Juan\");" [] success-handler error-handler)
     (.executeSql tx "INSERT INTO users (user_SLASH_id,user_SLASH_name) VALUES (38755334,\"Jose Pedro\");" [] success-handler error-handler)
     (.executeSql tx "INSERT INTO users (user_SLASH_id,user_SLASH_name) VALUES (38755330,\"Cote\");" [] success-handler error-handler)
     (catch js/Object e (js/console.error "ERROR populate-with-dummy-data" e))))
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
                    (js/console.warn "Database doesn't exist, creating..." err)
                    (.transaction db ensure-tables db-error-cb db-ready-cb))))))

(reg-fx
 :sqlite/execute-sql
 (fn [{:keys [honey-query succ-ev err-ev]}]
   (let [[succ-ev-key & succ-ev-args] succ-ev
         [err-ev-key & err-ev-args]   err-ev]
     (.transaction @db*
                   (fn [tx]

                     (try

                       (let [[query & params] (binding [sql-format/*name-transform-fn* munge]
                                                (sql/format honey-query :allow-namespaced-names? true))]
                         (js/console.log "[QUERY]" query
                                         "(VALS)" (clj->js params))
                         (.executeSql tx
                                      query
                                      (clj->js (or params []))
                                      (fn success [_ result-set]
                                        (js/console.log "[QUERY RES] (rows) : " (-> result-set .-rows .-length))
                                        ;; go over the result set and create a vector of rows with clj maps
                                        (let [rows-vec (->> (range (-> result-set .-rows .-length))
                                                            (mapv (fn [i]
                                                                    (let [res-map (-> result-set
                                                                                       .-rows
                                                                                       (.item i)
                                                                                       js->clj)]
                                                                      (update-keys res-map (comp keyword demunge))))))]
                                          (dispatch (into [succ-ev-key rows-vec] succ-ev-args))))
                                      (fn error [err] (js/console.err "Error executing query" err))))

                       (catch js/Object e (js/console.error "ERROR :sqlite/execute-sql" e))))))))
