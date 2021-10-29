(ns gym-scale.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.react-native :as rn]
            [reagent.core :as r]))

(defn app-top-bar []
  (let [scale-connected? @(subscribe [:scale/connected?])
        last-weight @(subscribe [:scale/last-weight])
        screen @(subscribe [:screen/current])
        disabled (#{:logo :user-select-1} screen )]
    [rn/view {:flex 1
              :flex-direction :row
              :justify-content :space-between
              :align-items :center
              :background-color :red}

     [rn/touchable-highlight {:on-press (fn []
                                          (dispatch [:screen/back]))
                              :disabled disabled}
      [rn/view {:background-color (if disabled :grey :yellow)
                :padding 10}
       [rn/text {:style {:font-size 30}} "<"]]]

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
                                               (dispatch [:sqlite-db/load-users symb [:screen/switch-to-user-select-2]]))
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
    [rn/view {:background-color :pink
              :height "100%"
              :justify-content :center
              :align-items :center
              :padding-top 40
              :padding-bottom 40}
     [rn/scroll-view {:width "60%"
                      :background-color :orange}
      (for [u users]
        ^{:key (str (:user/id u))}
        [rn/touchable-highlight {:on-press (fn []
                                             (dispatch [:screen/switch-to-user-check u]))}
         [rn/view {:margin 5
                   :background-color :yellow}
          [rn/text {:style {:font-size 30}}
           (str (:user/last-name u) ", " (:user/first-name u))]]])]]))

(defn screen-user-check []
  (let [user @(subscribe [:gym/selected-user-data])
        checked-in? @(subscribe [:gym/checked-in?])]
    [rn/view {:justify-content :center
              :align-items :center
              :height "100%"}

     [rn/text {:style {:font-size 30
                       :margin-bottom 50}}
      (str "Bienvenido " (:user/first-name user))]

     [rn/view {:width "80%"}
      (when-not checked-in?
        [rn/touchable-highlight {:on-press (fn []
                                             (dispatch [:user/check-in (:user/id user)]))}
         [rn/view {:margin 5
                   :background-color :yellow}
          [rn/text {:style {:font-size 30
                            :padding 10
                            :text-align :center}}
           "Check-in"]]])
      [rn/touchable-highlight {:on-press (fn [])}
       [rn/view {:margin 5
                 :background-color :yellow
                 :padding 10}
        [rn/text {:style {:font-size 30
                          :text-align :center}}
         "Mi actividad"]]]]]))


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
