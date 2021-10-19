(ns gym-scale.fxs.ble
  (:require [re-frame.core :refer [reg-fx dispatch]]
            [react-native :refer [PermissionsAndroid NativeModules NativeEventEmitter] :as react-native]
            [react-native-ble-manager :as BleManager]))

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
                               (js/console.log (str "Started notification for " weight-scale-service-id " " weight-characteristic-id))
                               (dispatch [:scale/connected])))
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
    (js/console.log "[handle-disconnect-peripheral] Peripheral disconnected" (.-id peripheral))
    (dispatch [:scale/disconnected])))

(defn handle-update-value-for-characteristic [data]
  (let [[a b :as d] (js->clj (.-value data))
        w (* (+ (bit-shift-left b 8) a) 5)]
    (when (<= 0 w 250000) ;; don't dispatch when overflowing, if we don't add this it can send 0/327
      (dispatch [:scale/on-weight-change w]))))

(defn handle-did-update-state [_]
  (js/console.warn "BleManagerDidUpdateState event not implemented"))

(defn handle-peripherial-did-bond [_]
  (js/console.warn "BleManagerPeripherialDidBond event not implemented"))

(defn start []

  (.addListener bleManagerEmitter "BleManagerDiscoverPeripheral" handle-discover-peripheral)
  (.addListener bleManagerEmitter "BleManagerStopScan" handle-stop-scan)
  (.addListener bleManagerEmitter "BleManagerDisconnectPeripheral" handle-disconnect-peripheral)
  (.addListener bleManagerEmitter "BleManagerDidUpdateValueForCharacteristic" handle-update-value-for-characteristic)

  (.addListener bleManagerEmitter "BleManagerDidUpdateState" handle-did-update-state)
  (.addListener bleManagerEmitter "BleManagerPeripherialDidBond" handle-peripherial-did-bond)

  (-> (.start BleManager #js{:showAlert false})
      (.then (fn []
               (scan-ble-devices)
               (js/console.log "BLE module initialized")))
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
