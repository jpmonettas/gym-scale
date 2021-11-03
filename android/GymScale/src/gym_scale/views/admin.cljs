(ns gym-scale.views.admin
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.react-native :as rn]
            [react-native :refer [Alert]]
            [reagent.core :as r]
            [goog.string :as gstr]
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
  (let [all-users @(subscribe [:gym/all-users-vals])]
    [rn/view {:background-color :pink
              :height "100%"
              :justify-content :center
              :align-items :center
              :padding-top 40
              :padding-bottom 40}
     [rn/scroll-view {:width "80%"
                      :background-color :orange}
      [rn/button {:on-press #(dispatch [:screen/switch-to-user-upsert nil])
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
                   :width 90
                   :justify-content :space-between}
          [rn/touchable-highlight {:on-press (fn [] (dispatch [:screen/switch-to-user-upsert (assoc u :editing? true)]))}
           [icon {:name "pencil" :size 30 :color :green}]]
          [rn/touchable-highlight {:on-press (fn []
                                               (.alert Alert
                                                       "Importante "
                                                       (gstr/format "Seguro que desea eliminar a %s, %s ?" (:user/last-name u) (:user/first-name u))
                                                       (clj->js
                                                        [{:text "Aceptar"
                                                          :onPress (fn [] (dispatch [:user/delete u]))}
                                                         {:text "Cancelar"
                                                          :onPress (fn [])}])))}
           [icon {:name "trash-o" :size 30 :color :green}]]]])]]))

(defn user-upsert []
  (let [user @(subscribe [:gym/editing-user])
        user-ref (r/atom user)
        style {:border-width 1
               :font-size 30
               :padding 2}]
    (fn []
      [rn/view {:height "100%"}
       [rn/view {:flex-direction :row
                 :justify-content :space-around
                 :height "40%"}
        [rn/view {:width "45%"
                  :height "100%"
                  :justify-content :space-around}
         [rn/text-input {:placeholder "CI"
                         :editable (not (:editing? user))
                         :style style
                         :keyboard-type :numeric
                         :value (str (:user/id @user-ref))
                         :on-change-text (fn [text] (swap! user-ref assoc :user/id (js/parseInt text)))}]
         [rn/text-input {:placeholder "Nombre"
                         :style style
                         :value (:user/first-name @user-ref)
                         :on-change-text (fn [text] (swap! user-ref assoc :user/first-name text))}]
         [rn/text-input {:placeholder "Apellido"
                         :style style
                         :value (:user/last-name @user-ref)
                         :on-change-text (fn [text] (swap! user-ref assoc :user/last-name text))}]]
        [rn/view {:width "45%"
                  :height "100%"
                  :justify-content :space-around}
         [rn/text-input {:placeholder "Fecha nacimiento aaaa-mm-dd"
                         :style style
                         :value (:user/birthday @user-ref)
                         :on-change-text (fn [text] (swap! user-ref assoc :user/birthday text))}]
         [rn/text-input {:placeholder "Telefono"
                         :style style
                         :value (:user/phone @user-ref)
                         :on-change-text (fn [text] (swap! user-ref assoc :user/phone text))}]
         [rn/view {:width "20%"}
         [rn/button {:title "Guardar"
                     :on-press #(dispatch [:user/upsert @user-ref])}]]]]])))

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
