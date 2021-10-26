(ns gym-scale.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.react-native :as rn]
            [reagent.core :as r]))

(defn app-top-bar []
  (let [scale-connected? @(subscribe [:scale/connected?])
        last-weight @(subscribe [:scale/last-weight])]
    [rn/view {:flex 1
              :flex-direction :row
              :justify-content :space-between
              :align-items :center
              :background-color :red}

     [rn/button {:title "<<<"}]

     (if scale-connected?
       [rn/text {:style {:font-size 40}}
        (str (quot last-weight 1000) " Kgs")]
       [rn/button {:on-press (fn [] (dispatch [:scale/open-connection]))
                   :title "Conectar"}])

     [rn/text {:style {:font-size 30}} "26/10 08:40"]]))


(defn screen-logo []
  [rn/view {:height "100%"
            :justify-content :center
            :align-items :center}
   [rn/text {} "LOGO"]])

(defn screen-user-select-1 []
  (let [alphabet "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        users-initials @(subscribe [:gym/users-search-initials])]
    [rn/view {:height "100%"
              :justify-content :center
              :align-items :center}
     [rn/view {:flex-direction   :row
               :flex-wrap :wrap
               :width 590}
      (for [symb alphabet]
        (let [symb-enabled? (contains? users-initials symb)]
         ^{:key symb}
          [rn/touchable-highlight {:on-press (fn []
                                               (dispatch [:screen/switch-to-user-select-2 symb]))
                                   :disabled (not symb-enabled?)}
          [rn/view {:border-width 1
                    :border-color :black
                    :padding 30
                    :margin 2}
           [rn/text {:style {:font-size 30
                             :color (if symb-enabled? :black "#ccc")
                             :font-family "monospace"}} symb]]]))]]))

(defn screen-user-select-2 []
  (let [users @(subscribe [:gym/users-search])]
    [rn/view {}
     [rn/text {} (str users)]]))

(defn screen-user-check []
  [rn/view {}
   [rn/text {} "screen-user-check"]])


(defn main-screen []
  (let [current-screen @(subscribe [:screen/current])]
    [rn/view {:flex 1}
     [app-top-bar]
     [rn/view {:flex 9}
      (case current-screen
        :logo          [screen-logo]
        :user-select-1 [screen-user-select-1]
        :user-select-2 [screen-user-select-2]
        :user-check    [screen-user-check])]]))
