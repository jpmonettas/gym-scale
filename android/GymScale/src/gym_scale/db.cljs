(ns gym-scale.db
  (:require [clojure.spec.alpha :as s]))

(s/def :scale/last-weight number?)
(s/def :scale/connected? boolean?)

(s/def :screen/current #{:logo :user-select-1 :user-select-2 :user-check})

(s/def :user/id number?)
(s/def :user/name string?)

(s/def :gym/user-data (s/keys :req [:user/id
                                    :user/name]))

(s/def :gym/users-search (s/coll-of :gym/user-data))

(s/def :gym/selected-user-data :gym/user-data)

(s/def :gym/checked-in? boolean?)

(s/def ::db (s/keys :req [:scale/last-weight
                          :scale/connected?]
                    :opt [:gym/users-search
                          :gym/selected-user-data
                          :gym/checked-in?]))

(def initial-db
  ;; initial
  #_{:scale/last-weight 0 ;; in grams
     :scale/connected? false
     :screen/current :logo
     }

  ;; connected, on logo screen
  {:scale/last-weight 0
   :scale/connected? true
   :screen/current :logo}

  ;; advanced to user-select-1
  #_{:scale/last-weight 76000,
     :scale/connected? true,
     :screen/current :user-select-1,
     :gym/users-search
     [#:user{:name "Juan", :id 38755324},
      #:user{:name "Cote", :id 38755330},
      #:user{:name "Jose Pedro", :id 38755334}]}

  ;; advanced to user-select-2 under letter J
  #_{:scale/last-weight 76000,
   :scale/connected? true,
   :screen/current :user-select-2,
   :gym/users-search
   [#:user{:name "Juan", :id 38755324},
    #:user{:name "Jose Pedro", :id 38755334}]}

  #_{:scale/last-weight 76000,
   :scale/connected? true,
   :screen/current :user-check,
   :gym/users-search
   [#:user{:name "Juan", :id 38755324},
    #:user{:name "Jose Pedro", :id 38755334}]
   :gym/selected-user-data #:user{:name "Juan", :id 38755324}})

(comment
  (re-frame.core/dispatch [:scale/on-weight-change 76000])
  @re-frame.db/app-db
  )
