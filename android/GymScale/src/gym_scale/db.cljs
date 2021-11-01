(ns gym-scale.db
  (:require [clojure.spec.alpha :as s]))

(s/def :scale/last-weight number?)
(s/def :scale/connected? boolean?)

(s/def :screen/current #{:logo :user-select-1 :user-select-2 :user-check
                         :pinpad :admin-menu :users-crud})

(s/def :user/id number?)
(s/def :user/name string?)

(s/def :gym/user-data (s/keys :req [:user/id
                                    :user/first-name
                                    :user/last-name
                                    :user/phone
                                    :user/birthday]))

(s/def :gym/users-search (s/coll-of :gym/user-data))
(s/def :gym/all-users (s/coll-of :gym/user-data))

(s/def :gym/selected-user-data :gym/user-data)

(s/def :gym/checked-in? boolean?)

(s/def ::backable-state (s/keys :req [:screen/current]
                                :opt [:gym/users-search
                                      :gym/selected-user-data
                                      :gym/all-users]))

(s/def :state/current ::backable-state)
(s/def :state/prev-stack (s/coll-of ::backable-state))

(s/def :clock/date-time-str string?)

(s/def ::db (s/keys :req [:scale/last-weight
                          :scale/connected?
                          :state/current
                          :state/prev-stack]
                    :opt [:gym/checked-in?
                          :clock/date-time-str]))

(def initial-db
  #_{:scale/last-weight 0 ;; in grams
   :scale/connected? false
   :state/current {:screen/current :logo}
   :state/prev-stack ()
     }
  {:scale/last-weight 0,
   :scale/connected? false,
   :state/current
   {:screen/current :users-crud,
    :gym/all-users
    [#:user{:phone "098164800",
            :last-name "Monetta Sanchez",
            :first-name "Juan Pedro",
            :birthday "1983-10-20",
            :id 38755324}
     #:user{:phone "098164800",
            :last-name "Monetta Sanchez",
            :first-name "Jose Ignacio",
            :birthday "1986-09-25",
            :id 38755330}]},
   :state/prev-stack
   '(#:screen{:current :admin-menu} #:screen{:current :logo})})
