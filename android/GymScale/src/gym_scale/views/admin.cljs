(ns gym-scale.views.admin
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.react-native :as rn]
            [reagent.core :as r]))


(def default-pin "2468")

(defn pinpad []
  (let [pin-ref (r/atom "")]
    (fn []
      (let [pd (fn pinpad-digit [d]
                 [rn/touchable-highlight {:on-press (fn []
                                                      (swap! pin-ref #(str % d))
                                                      (when (= (count @pin-ref) 4)
                                                        (when (= @pin-ref default-pin)
                                                          (dispatch [:screen/switch-to-admin-menu]))
                                                        (reset! pin-ref "")))}
                  [rn/view {:border-width 1
                            :border-color :black
                            :padding-top    15
                            :padding-left   30
                            :padding-right  30
                            :padding-bottom 15
                            :margin 2}
                   [rn/text {:style {:font-size 50
                                     :font-family "monospace"}}
                    d]]])]
        [rn/view {:justify-content :center
                  :align-items :center
                  :height "100%"}
         [rn/text-input {:value @pin-ref
                         :secure-text-entry true
                         :style {:font-size 20}}]
         [rn/view {:flex-direction :row
                   :background-color "#eee"
                   :flex-wrap :wrap
                   :width 288}
          [rn/view {:flex-direction :row} [pd "1"] [pd "2"] [pd "3"]]
          [rn/view {:flex-direction :row} [pd "4"] [pd "5"] [pd "6"]]
          [rn/view {:flex-direction :row} [pd "7"] [pd "8"] [pd "9"]]
          [rn/view {:flex-direction :row
                    :width "100%"
                    :justify-content :center}      [pd "0"]]]]))))

(defn menu []
  [rn/view {}
   [rn/text {}
    "Admin menu"]])
