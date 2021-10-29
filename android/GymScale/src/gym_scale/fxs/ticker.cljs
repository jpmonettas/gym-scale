(ns gym-scale.fxs.ticker
  (:require [re-frame.core :refer [reg-fx dispatch]]))

(def interval-id (atom nil))

(reg-fx
 :ticker/start
 (fn [millis]
   (reset! interval-id
           (js/setInterval (fn []
                             (dispatch [:ticker/tick]))
                           millis))))

(reg-fx
 :ticker/stop
 (fn []
   (js/clearInterval @interval-id)))
