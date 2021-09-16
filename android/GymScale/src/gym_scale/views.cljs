(ns gym-scale.views  
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.react-native :as rn]
            [reagent.core :as r]))

(defn main-screen []
  (let [calib-grams (r/atom "")]
    (fn []
      (let [last-weight @(subscribe [:scale/last-weight])
            serial-connected? @(subscribe [:serial/connected?])]
        [rn/view {:flex 1}
         [rn/view {:flex 1}
          (if serial-connected?
            [rn/text {} "Connected"]
            [rn/button {:on-press (fn [] (dispatch [:scale/open-connection]))
                        :title "Connect"}])]
         [rn/view {:flex 7}
          [rn/text {:style {:font-size 50}}
           (str (quot last-weight 1000) " Kgs")]]]))))
