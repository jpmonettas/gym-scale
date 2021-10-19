(ns gym-scale.fxs.otg-serial
  (:require [react-native :refer [PermissionsAndroid NativeModules NativeEventEmitter] :as react-native]
            [re-frame.core :refer [reg-fx dispatch]]))

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
