(ns gym-scale.events
  (:require [re-frame.core :refer [reg-event-db reg-event-fx reg-fx dispatch]]
            [react-native :refer [PermissionsAndroid NativeModules NativeEventEmitter] :as react-native]
            [gym-scale.db :as db]
            [react-native-sqlite-storage :as SQLite]
            [react-native-ble-manager :as BleManager]))

(reg-event-fx
 :app/init
 (fn [_ _]
   {:db db/initial-db
    :serial/open-connection nil
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

(reg-event-fx
 :scale/tare
 (fn [cofxs _]
   {:serial/send-string "TARE\n"}))

(reg-event-fx
 :scale/calibrate
 (fn [cofxs [_ grams]]
   {:serial/send-string (str "CALI " grams "\n")}))

(reg-event-db
 :scale/on-weight-change
 (fn [db [_ w]]
   (assoc db :scale/last-weight w)))

(def debug-enable false)
 
(reg-event-db
 :serial/on-message
 (fn [db [_ s]]
   (let [[_ command args-str :as all] (re-find #"([A-Z]+) (.*)" s)
         db' (case command               
               "WEIGHT" (assoc db :scale/last-weight (js/parseFloat args-str))
               (do
                 (js/console.log "Couldn't parse command from " s)
                 db))]
     (cond-> db'
       debug-enable (update :debug-console #(conj % s))))))

(reg-event-db
 :serial/connected
 (fn [db _]
   (assoc db :serial/connected? true)))

(reg-event-db
 :serial/disconnected
 (fn [db [_ _]]
   (assoc db :serial/connected? false)))

(reg-event-db
 :db/opened
 (fn [db _]
   (js/console.log "DB opened")
   db))

(reg-event-db
 :db/error
 (fn [db [_ err]]
   (js/console.error err)
   db))

(reg-event-db
 :db/users-loaded
 (fn [db [_ all-users]]
   (js/console.log "All users" (clj->js all-users))
   db))

(reg-event-fx
 :db/load-all-users
 (fn [_ _]
   {:sqlite/execute-sql {:query "SELECT * from users;"
                         :params-vec []
                         :succ-ev [:db/users-loaded]
                         :err-ev  [:db/error]}}))

;;;;;;;;;
;; FXs ;;
;;;;;;;;;

;; OTG serial connection
;; ---------------------

(def OTGModule (.-OTGModule NativeModules))

(reg-fx
 :serial/open-connection
 (fn [_]
   (let [event-emitter (NativeEventEmitter. OTGModule)]
     (.addListener event-emitter "onSerialData"
                   (fn [serial-msg]
                     (js/console.log ">>>" serial-msg)
                     (dispatch [:serial/on-message (.-data serial-msg)])))
     (.addListener event-emitter "onSerialConnect"
                   (fn [_]
                     (js/console.log "Serial connected")
                     (dispatch [:serial/connected])))
     (.addListener event-emitter "onSerialDisconnect"
                   (fn [error-msg]
                     (js/console.log "Serial disconnected" error-msg)
                     (dispatch [:serial/disconnected error-msg])))
     (js/console.log "Opening connection")
     (.openConnection OTGModule))))

(reg-fx
 :serial/close-connection
 (fn [_]
   (.closeConnection OTGModule)))

(reg-fx
 :serial/send-string
 (fn [s]
   (js/console.log "Sending string :" s)
   (.sendString OTGModule s)))

;; SQLite
;; ------

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

;; Bluetooth LE
;; ------------


(def BleManagerModule (.-BleManager NativeModules))
(def bleManagerEmitter (NativeEventEmitter. BleManagerModule))
(def weight-scale-service-id  "181d")
(def weight-characteristic-id "2a98")


(defn scan-ble-devices []
  (-> (.scan BleManager             
             #js [weight-scale-service-id] ;; services ids we are interested in
             120                           ;; the amount of seconds to scan
             true                          ;; allow duplicates
             #js {}                        ;; options
             )
      (.then (fn []
               (js/console.log "BLE scan started")))
      (.catch (fn [err]
                (js/console.error "[scan-ble-devices]" err)))))

(defn retrieve-service-and-start-notification [peripheral-id]
  (-> (.retrieveServices BleManager peripheral-id)
      (.then (fn [peripheral-info]               
               (js/console.log peripheral-info)
               (js/setTimeout
                (fn []
                  (-> (.startNotification BleManager
                                          peripheral-id
                                          weight-scale-service-id
                                          weight-characteristic-id)
                      (.then (fn []
                               (js/console.log (str "Started notification for " weight-scale-service-id " " weight-characteristic-id))))
                      (.catch (fn [err]
                                (js/console.error "[retrieve-service-and-start-notification]" err)))))
                500)))
      (.catch (fn [err]
                (js/console.error "[retrieve-service-and-start-notification]" err)))))

(defn handle-discover-peripheral [peripheral]
  (js/console.log (str "handle-discover-peripheral" (.-name peripheral)))
  
  (when (= (.-name peripheral) "ESP32")
    (js/console.log peripheral)  
    (if (not (.-connected peripheral))
      ;; if it is not connected connect
      (-> (.connect BleManager (.-id peripheral))
          (.then (fn []
                   (js/console.log "Connected to " (.-id peripheral))
                   (retrieve-service-and-start-notification (.-id peripheral))))
          (.catch (fn [err]
                    (js/console.error "[handle-discover-peripheral]" err))))

      ;; else if it is already connected proceed 
      (retrieve-service-and-start-notification (.-id peripheral)))))

(defn handle-stop-scan []
  (js/console.log "handle-stop-scan"))

(defn handle-disconnect-peripheral [data]  
  (let [peripheral (.-peripheral data)]
    (js/console.log "[handle-disconnect-peripheral] Peripheral disconnected" (.-id peripheral))))

(defn handle-update-value-for-characteristic [data]
  (let [[a b :as d] (js->clj (.-value data))
        w (* (+ (bit-shift-left b 8) a) 5)]
    (dispatch [:scale/on-weight-change w])))

(defn handle-foo [data]
  (js/console.log ">>>>>>>>>>>>>>>>>>" data))

(defn start []
  
  (.addListener bleManagerEmitter "BleManagerDiscoverPeripheral" handle-discover-peripheral)
  (.addListener bleManagerEmitter "BleManagerStopScan" handle-stop-scan)
  (.addListener bleManagerEmitter "BleManagerDisconnectPeripheral" handle-disconnect-peripheral)
  (.addListener bleManagerEmitter "BleManagerDidUpdateValueForCharacteristic" handle-update-value-for-characteristic)

  (.addListener bleManagerEmitter "BleManagerDidUpdateState" handle-foo)
  (.addListener bleManagerEmitter "BleManagerPeripherialDidBond" handle-foo)    
  
  (-> (.start BleManager #js{:showAlert false})
      (.then (fn []
               (js/console.log "BLE module initialized")
               (scan-ble-devices)))
      (.catch (fn [err]
                (js/console.error "[STARTING]" err)))))

(reg-fx
 :ble/start
 (fn [_]
   (-> (.check PermissionsAndroid "android.permission.ACCESS_FINE_LOCATION")
       (.then (fn [has-permission?]
                (if has-permission?
                  (do
                    (js/console.log "Permissions ok, starting...")
                    (start))
                  (do
                    (js/console.log "Not enough permissions")
                    (-> (.request PermissionsAndroid "android.permission.ACCESS_FINE_LOCATION")
                        (.then (fn [granted?]
                                 (if granted?
                                   (do
                                     (js/console.log "Permissions granted, starting...")
                                     (start))
                                   (js/console.log "Permissions rejected"))))))))))))

(comment

  (in-ns 'gym-scale.events)

  

  

  
  
  

  )
