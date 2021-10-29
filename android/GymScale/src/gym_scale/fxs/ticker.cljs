(ns gym-scale.fxs.ticker
  (:require [re-frame.core :refer [reg-fx dispatch]]))

(def interval-id (atom nil))

(reg-fx
 :start-ticker
 (fn [millis]
   (reset! interval-id
           (js/setInterval (fn []
                             (dispatch [:ticker/tick]))
                           millis))))

(reg-fx
 :stop-ticker
 (fn []
   (js/clearInterval @interval-id)))
