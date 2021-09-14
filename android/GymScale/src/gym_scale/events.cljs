(ns gym-scale.events
  (:require [re-frame.core :refer [reg-event-db reg-event-fx reg-fx dispatch]]
            [react-native :refer [NativeModules NativeEventEmitter]]
            [gym-scale.db :as db]))

(reg-event-fx
 :app/init
 (fn [_ _]
   {:db db/initial-db
    :serial/open-connection nil}))

(reg-event-fx
 :scale/open-connection
 (fn [cofxs _]
   {:serial/open-connection nil}))

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

(def debug-enable false)
 
(reg-event-db
 :serial/on-message
 (fn [db [_ s]]
   (let [[_ command args-str :as all] (re-find #"([A-Z]+) (.*)" s)
         db' (case command
               "STATUS" (assoc db :scale/status (case args-str
                                                  "UNCALIBRATED" :uncalibrated
                                                  "READY"        :ready))
               "WEIGHT" (assoc db :scale/last-weight (js/parseFloat args-str))
               (do
                 (js/console.log "Couldn't parse command from " s)
                 db))]
     (cond-> db'
       debug-enable (update :debug-console #(conj % s))))))

;;;;;;;;;
;; FXs ;;
;;;;;;;;;

(def OTGModule (.-OTGModule NativeModules))

(reg-fx
 :serial/open-connection
 (fn [_]
   (let [event-emitter (NativeEventEmitter. OTGModule)]
     (.addListener event-emitter "onSerialData"
                   (fn [serial-msg]
                     (js/console.log ">>>" serial-msg)
                     (dispatch [:serial/on-message (.-data serial-msg)])))
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
