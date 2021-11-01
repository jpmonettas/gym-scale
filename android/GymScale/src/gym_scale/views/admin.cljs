(ns gym-scale.views.admin
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.react-native :as rn]
            [reagent.core :as r]
            ["react-native-vector-icons/FontAwesome$default" :as Icon]))

(def icon (r/adapt-react-class Icon))

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

(defn users-crud []
  (let [all-users @(subscribe [:gym/all-users])]
    [rn/view {:background-color :pink
              :height "100%"
              :justify-content :center
              :align-items :center
              :padding-top 40
              :padding-bottom 40}
     [rn/scroll-view {:width "80%"
                      :background-color :orange}
      [rn/button {:on-press #()
                  :title "Nuevo"} ]
      (for [u all-users]
        ^{:key (str (:user/id u))}
        [rn/view {:margin 5
                  :flex-direction :row
                  :justify-content :space-between
                  :background-color :yellow}
         [rn/text {:style {:font-size 30}}
          (str (:user/last-name u) ", " (:user/first-name u))]
         [rn/view {:flex-direction :row
                   :width 60
                   :justify-content :space-between}
          [rn/touchable-highlight {:on-press (fn [] (js/console.log "Editing user" (:user/first-name u)))}
           [icon {:name "pencil" :size 30 :color :green}]]
          [rn/touchable-highlight {:on-press (fn [] (js/console.log "Deleting user" (:user/first-name u)))}
           [icon {:name "trash-o" :size 30 :color :green}]]]])]]))

(defn menu-button [{:keys [text on-click]}]
  [rn/touchable-highlight {:on-press on-click}
   [rn/view {:margin 5
             :background-color :yellow}
    [rn/text {:style {:font-size 30
                      :padding 10
                      :text-align :center}}
     text]]])

(defn menu []
  [rn/view {}
   [rn/view {:justify-content :center
             :align-items :center
             :height "100%"}

    [rn/view {:width "80%"}
     [menu-button {:text "Mantenimiento usuarios"
                   :on-click (fn [] (dispatch [:sqlite-db/load-users nil [:screen/switch-to-users-crud]]))}]
     [menu-button {:text "Pagos"}]
     [menu-button {:text "Cumpleaños"}]]]])
