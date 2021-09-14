(ns gym-scale.views  
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.react-native :as rn]
            [reagent.core :as r]))

(defn main-screen []
  (let [calib-grams (r/atom "")]
    (fn []
      (let [last-weight @(subscribe [:scale/last-weight])
            scale-status @(subscribe [:scale/status])]
        [rn/view {:flex 1}
         (case scale-status
           :unknown [rn/view {}
                     [rn/text {:style {:font-size 40}}
                      "Unknown scale status"]
                     [rn/button {:on-press (fn [] (dispatch [:scale/open-connection]))
                                 :title "Connect"}]
                     ]
           :uncalibrated [rn/view {:flex 1}
                          [rn/button {:on-press (fn [] (dispatch [:scale/tare]))
                                      :title "Tare"}]
                          [rn/text-input {:on-change-text (fn [v]
                                                            (reset! calib-grams v))}]
                          [rn/button {:on-press (fn [] (dispatch [:scale/calibrate @calib-grams]))
                                      :title "Calibrate"}]]
           :ready [rn/text {:style {:font-size 50}}
                   (str (quot last-weight 1000) " Kgs")]
           [rn/text {} (str "Weird status" scale-status)])]))))
